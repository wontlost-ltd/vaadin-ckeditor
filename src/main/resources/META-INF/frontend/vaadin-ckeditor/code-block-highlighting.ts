/**
 * 代码块语法高亮（编辑时）。
 *
 * CKEditor 5 的 CodeBlock 只在 `<code>` 上输出 `language-*` 类，编辑区不着色
 * （ckeditor/ckeditor5#5769）。外部高亮器不能直接改编辑区 **DOM**——CKEditor 的渲染器会把 DOM
 * 改回视图状态（ckeditor/ckeditor5#6309）。这里改的是编辑 **视图**（view），渲染器据此生成 DOM：
 *
 * - 用 highlight.js 对代码块的纯文本分词，按行得到「文本片段 + 类名」；
 * - 在 `editing.view.change()` 中把代码块视图逐行对齐到期望的分段：文本包进带自定义属性的
 *   `<span class="hljs-*">` 属性元素（AttributeElement，与加粗等格式同类，Mapper 天然支持），
 *   与期望一致的行不动，所以输入时通常只重建光标所在的一行；
 * - 模型（model）一字不改：不产生 operation、不进撤销栈、不触发 `change:data`；
 *   数据管线（`editor.getData()`，即 Java 端 `getValue()`）从模型生成，不含任何高亮 span。
 *
 * 选过 marker + `markerToHighlight` 方案：数据同样干净，但在大代码块开头每输入一个字符，
 * 其后所有 marker 都要随之移动并重新转换，1000 行代码块单次按键在 jsdom 中超过 6 秒；
 * 逐行对齐的方案按键开销与代码块长度成线性（分词）且只重建变化的行。
 *
 * 含其他功能高亮（评论、修订建议、查找替换等的属性元素）的行保持原样，避免破坏它们的视图结构。
 *
 * 本模块是 add-on 的**自定义插件**（见 addon-plugins.ts）：Java 端 `setCodeBlockHighlighting(true)` 把
 * `CustomPlugin("VaadinCodeBlockHighlighting")` 及其配置 `codeBlockHighlighting: { enabled, theme }`
 * 加入插件列表，连接器按需加载本模块；之后的开关与换配色经 `vaadin:pluginConfig` 事件到达，无需重建编辑器。
 */
import { Plugin, type Editor, type ViewDowncastWriter as DowncastWriter, type ViewElement } from 'ckeditor5';
import { PLUGIN_CONFIG_EVENT } from './plugin-resolver';
import './code-block-highlighting.css';

/** highlight.js 中本模块用到的最小接口（便于测试注入）。 */
export interface HljsLike {
    highlight(code: string, options: { language: string; ignoreIllegals?: boolean }): { value: string };
    getLanguage(name: string): unknown;
}

/** 分词结果：代码块纯文本中的 [start, end) 区间及其 CSS 类（空格分隔）。 */
export interface HighlightToken {
    start: number;
    end: number;
    classes: string;
}

/** 内置配色；`none` 只输出 `hljs-*` 类，由应用自带样式。 */
export const CODE_HIGHLIGHT_THEMES = ['auto', 'github', 'github-dark', 'atom-one-light', 'atom-one-dark', 'none'] as const;
export type CodeHighlightTheme = typeof CODE_HIGHLIGHT_THEMES[number];

/** 超过该长度（字符）的代码块不着色，避免超大块拖慢输入；见 USER_GUIDE 的性能数据。 */
export const DEFAULT_MAX_BLOCK_LENGTH = 100_000;
/** 插件在编辑器配置中的键：`{ enabled?: boolean; theme?: CodeHighlightTheme }`。 */
export const CONFIG_KEY = 'codeBlockHighlighting';

interface HighlightingConfig {
    enabled?: boolean;
    theme?: string;
}

/** 输入后的去抖延迟（毫秒）。 */
export const DEFAULT_DEBOUNCE_MS = 30;

/** 标记本插件创建的 span（区分评论、修订建议等其他功能的属性元素）。 */
const SPAN_PROPERTY = 'vaadinCodeHighlight';
/** 低于评论 / 修订建议高亮，使它们包在外层。 */
const SPAN_PRIORITY = 5;
const ROOT_CLASS = 'ck-code-hl';
const THEME_CLASS_PREFIX = 'ck-code-hl_theme-';
const DARK_CLASS = 'ck-code-hl_dark';

/**
 * CKEditor 默认 `codeBlock.languages` 中与 highlight.js 名称不同的条目。
 * 其余语言名（以及 `js`、`ts`、`py`、`sh` 等别名）由 `hljs.getLanguage()` 直接识别。
 */
const LANGUAGE_ALIASES: Record<string, string | null> = {
    plaintext: null,
    plain: null,
    text: null,
    cs: 'csharp',
    html: 'xml',
};

/** CKEditor 代码块语言 → highlight.js 语言；不认识或纯文本时返回 null（不着色）。 */
export function resolveHljsLanguage(hljs: HljsLike, language: string | undefined | null): string | null {
    if (!language) {
        return null;
    }
    const key = language.toLowerCase();
    if (key in LANGUAGE_ALIASES) {
        return LANGUAGE_ALIASES[key];
    }
    return hljs.getLanguage(key) ? key : null;
}

/**
 * 对一段代码分词。highlight.js 输出转义后的 HTML，其文本拼接起来与输入逐字相同；
 * 这里把嵌套的 span 压平：每段文本取其所有祖先 span 的类，相邻同类合并。
 */
export function tokenize(hljs: HljsLike, code: string, language: string): HighlightToken[] {
    if (!code) {
        return [];
    }
    const html = hljs.highlight(code, { language, ignoreIllegals: true }).value;
    // <template> 的内容是惰性的（不执行脚本、不加载资源）；highlight.js 已转义代码文本，这里只读取结构
    const template = document.createElement('template');
    template.innerHTML = html;

    const tokens: HighlightToken[] = [];
    let offset = 0;
    const walk = (node: Node, classes: string[]): void => {
        for (const child of Array.from(node.childNodes)) {
            if (child.nodeType === Node.TEXT_NODE) {
                const length = (child as Text).data.length;
                pushToken(tokens, offset, offset + length, classes.join(' '));
                offset += length;
            } else if (child.nodeType === Node.ELEMENT_NODE) {
                const own = (child as Element).getAttribute('class');
                walk(child, own ? [...classes, ...own.split(/\s+/).filter(Boolean)] : classes);
            }
        }
    };
    walk(template.content, []);
    return tokens;
}

function pushToken(tokens: HighlightToken[], start: number, end: number, classes: string): void {
    if (!classes || end <= start) {
        return;
    }
    const last = tokens[tokens.length - 1];
    if (last && last.end === start && last.classes === classes) {
        last.end = end;
        return;
    }
    tokens.push({ start, end, classes });
}

type ModelElement = ReturnType<Editor['model']['document']['getRoot']> & {
    name: string;
    getAttribute(key: string): unknown;
    getChildren(): Iterable<ModelNode>;
    is(type: string, name?: string): boolean;
    root: { rootName: string };
    parent: unknown;
    maxOffset: number;
};
interface ModelNode {
    is(type: string, name?: string): boolean;
    data?: string;
    parent?: ModelNode | null;
}

/** 代码块的纯文本：softBreak 计为一个 `\n`，与模型偏移一一对应。 */
export function codeBlockText(block: { getChildren(): Iterable<ModelNode> }): string {
    let text = '';
    for (const child of block.getChildren()) {
        text += child.is('$text') || child.is('$textProxy') ? child.data ?? '' : '\n';
    }
    return text;
}

export interface CodeBlockHighlightingOptions {
    /** 按需加载 highlight.js；只在首次启用时调用。 */
    loadHighlighter: () => Promise<HljsLike>;
    maxBlockLength?: number;
    debounceMs?: number;
    onError?: (error: unknown) => void;
}

/**
 * 编辑时代码块高亮插件。插件随 CodeBlock 一起注入，但默认**不工作**：只有 `setEnabled(true)` 后才加载
 * highlight.js、监听变更并着色；`setEnabled(false)` 移除全部高亮 span。
 * 选项经 {@link createCodeBlockHighlightingPlugin} 生成的子类的静态 `options` 传入。
 */
export class CodeBlockHighlighting extends Plugin {
    static get pluginName() {
        return 'VaadinCodeBlockHighlighting' as const;
    }

    /** 由工厂子类覆盖。 */
    static options: CodeBlockHighlightingOptions = {
        loadHighlighter: () => Promise.reject(new Error('highlight.js loader not configured')),
    };

    private get options(): CodeBlockHighlightingOptions {
        return (this.constructor as typeof CodeBlockHighlighting).options;
    }

    private hljs: HljsLike | null = null;
    private enabled = false;
    private theme: CodeHighlightTheme = 'auto';
    private dark = false;
    private dirty = new Set<ModelElement>();
    private timer: ReturnType<typeof setTimeout> | undefined;
    private loading: Promise<void> | null = null;
    private themeObserver: MutationObserver | null = null;
    /** 最近一次着色耗时（毫秒），供性能测量读取。 */
    lastHighlightMs = 0;

    init(): void {
        const editor = this.editor;
        // Java 端经 CustomPlugin 配置传入的开关与配色；运行中的变化经 PLUGIN_CONFIG_EVENT 到达
        const initial = (editor.config.get(CONFIG_KEY) ?? {}) as HighlightingConfig;
        this.listenTo(editor, PLUGIN_CONFIG_EVENT, (_evt, key: unknown, config: unknown) => {
            if (key === CONFIG_KEY) {
                void this.applyConfig((config ?? {}) as HighlightingConfig);
            }
        });
        editor.on('ready', () => {
            this.watchEditorTheme();
            void this.applyConfig(initial);
        });

        // 文本、软换行、language 属性的任何改动都会经转换器重建部分视图，涉及的块需重新对齐
        this.listenTo(editor.model.document, 'change:data', () => {
            if (!this.enabled || !this.hljs) {
                return;
            }
            for (const block of this.changedCodeBlocks()) {
                this.dirty.add(block);
            }
            if (this.dirty.size > 0) {
                this.schedule();
            }
        });

        this.listenTo(editor.editing.view.document, 'compositionend', () => {
            if (this.dirty.size > 0) {
                this.schedule();
            }
        });

        this.applyRootClasses();
    }

    override destroy(): void {
        this.themeObserver?.disconnect();
        this.themeObserver = null;
        clearTimeout(this.timer);
        this.dirty.clear();
        super.destroy();
    }

    /** 应用 `{ enabled, theme }` 配置。 */
    applyConfig(config: HighlightingConfig): Promise<void> {
        this.setTheme(config.theme ?? 'auto');
        return this.setEnabled(config.enabled === true);
    }

    /**
     * 跟随连接器的明暗主题：宿主 `<vaadin-ckeditor>` 的 `data-ck-theme` 属性（theme-manager 写入）。
     * 在编辑器就绪时定位宿主（全屏会把编辑区移出宿主，之后仍观察同一个宿主）。
     */
    private watchEditorTheme(): void {
        const editor = this.editor as unknown as {
            sourceElement?: HTMLElement;
            ui: { element?: HTMLElement | null; getEditableElement?: () => HTMLElement | undefined };
        };
        const start = editor.ui.getEditableElement?.() ?? editor.ui.element ?? editor.sourceElement;
        const host = start?.closest?.('[data-ck-theme]');
        if (!host) {
            return;
        }
        const sync = () => this.setDark(host.getAttribute('data-ck-theme') === 'dark');
        sync();
        this.themeObserver = new MutationObserver(sync);
        this.themeObserver.observe(host, { attributes: true, attributeFilter: ['data-ck-theme'] });
    }

    get isHighlighting(): boolean {
        return this.enabled;
    }

    /** 启用或停用着色；启用时按需加载 highlight.js，返回的 Promise 在首次着色完成后 resolve。 */
    setEnabled(enabled: boolean): Promise<void> {
        if (enabled === this.enabled) {
            return this.loading ?? Promise.resolve();
        }
        this.enabled = enabled;
        this.applyRootClasses();
        clearTimeout(this.timer);
        if (!enabled) {
            // 停用：所有代码块对齐到「无高亮」，即移除本插件的 span
            this.markAllCodeBlocks();
            this.render();
            return Promise.resolve();
        }
        this.loading = (this.hljs ? Promise.resolve(this.hljs) : this.options.loadHighlighter())
            .then((hljs) => {
                this.hljs = hljs;
                if (this.enabled) {
                    this.markAllCodeBlocks();
                    this.render();
                }
            })
            .catch((error) => {
                this.enabled = false;
                this.applyRootClasses();
                this.options.onError?.(error);
            })
            .finally(() => {
                this.loading = null;
            });
        return this.loading;
    }

    setTheme(theme: string): void {
        this.theme = (CODE_HIGHLIGHT_THEMES as readonly string[]).includes(theme)
            ? theme as CodeHighlightTheme
            : 'auto';
        this.applyRootClasses();
    }

    /** 编辑器当前明暗（只影响 `auto` 配色）。 */
    setDark(dark: boolean): void {
        this.dark = dark;
        this.applyRootClasses();
    }

    /** 立即处理待着色的块（测试与性能测量用）。 */
    flush(): void {
        clearTimeout(this.timer);
        this.timer = undefined;
        this.render();
    }

    private schedule(): void {
        clearTimeout(this.timer);
        this.timer = setTimeout(() => {
            this.timer = undefined;
            // 输入法组合中改动视图会打断组合（Safari 尤甚），等 compositionend 再着色
            if (this.editor.editing.view.document.isComposing) {
                return;
            }
            this.render();
        }, this.options.debounceMs ?? DEFAULT_DEBOUNCE_MS);
    }

    private markAllCodeBlocks(): void {
        const model = this.editor.model;
        for (const rootName of model.document.getRootNames()) {
            const root = model.document.getRoot(rootName);
            if (!root) {
                continue;
            }
            for (const { item } of model.createRangeIn(root)) {
                if ((item as unknown as ModelNode).is('element', 'codeBlock')) {
                    this.dirty.add(item as unknown as ModelElement);
                }
            }
        }
    }

    /** 本次变更涉及的代码块：块内文本改动、块的插入，以及 language 属性改动。 */
    private changedCodeBlocks(): Set<ModelElement> {
        const blocks = new Set<ModelElement>();
        const changes = this.editor.model.document.differ.getChanges() as Array<Record<string, any>>;
        for (const change of changes) {
            if (change.type === 'attribute') {
                for (const { item } of change.range.getWalker({ shallow: true })) {
                    addCodeBlock(blocks, item);
                }
                addCodeBlock(blocks, change.range.start.parent);
                continue;
            }
            const position = change.position;
            if (!position) {
                continue;
            }
            addCodeBlock(blocks, position.parent);
            if (change.type === 'insert') {
                addCodeBlock(blocks, position.nodeAfter);
            }
        }
        return blocks;
    }

    /** 把待处理的代码块视图逐行对齐到期望的高亮分段。 */
    private render(): void {
        if (this.dirty.size === 0) {
            return;
        }
        const blocks = [...this.dirty];
        this.dirty.clear();
        const started = performance.now();
        const editing = this.editor.editing;
        const model = this.editor.model;
        editing.view.change((writer) => {
            for (const block of blocks) {
                if (!block.parent || block.root.rootName === '$graveyard') {
                    continue;
                }
                const code = editing.mapper.toViewElement(block as never);
                if (!code) {
                    continue;
                }
                const text = codeBlockText(block);
                const tokens = this.enabled && this.hljs ? this.tokensFor(block, text) : [];
                reconcileCodeView(writer, code, toLines(text, tokens));
            }
            // 被替换的文本节点可能正承载视图选区：按模型选区重新生成，避免渲染器指向已移除的节点
            editing.downcastDispatcher.convertSelection(model.document.selection, model.markers, writer);
        });
        this.lastHighlightMs = performance.now() - started;
    }

    private tokensFor(block: ModelElement, text: string): HighlightToken[] {
        const language = resolveHljsLanguage(this.hljs!, block.getAttribute('language') as string | undefined);
        if (!language || text.length > (this.options.maxBlockLength ?? DEFAULT_MAX_BLOCK_LENGTH)) {
            return [];
        }
        try {
            return tokenize(this.hljs!, text, language);
        } catch (error) {
            this.options.onError?.(error);
            return [];
        }
    }

    /** 配色以编辑根上的类表达（全屏时编辑区会被移出组件，宿主上的属性选择器够不着）。 */
    private applyRootClasses(): void {
        const view = this.editor.editing.view;
        view.change((writer) => {
            for (const root of view.document.roots) {
                for (const cls of Array.from(root.getClassNames())) {
                    if (cls === ROOT_CLASS || cls === DARK_CLASS || cls.startsWith(THEME_CLASS_PREFIX)) {
                        writer.removeClass(cls, root);
                    }
                }
                if (this.enabled) {
                    writer.addClass([ROOT_CLASS, THEME_CLASS_PREFIX + this.theme], root);
                    if (this.dark) {
                        writer.addClass(DARK_CLASS, root);
                    }
                }
            }
        });
    }
}

/** 一行内的片段：文本及其类名（null 表示不着色）。 */
export interface LineSegment {
    text: string;
    classes: string | null;
}

/** 把分词结果切成逐行片段（`\n` 对应视图中的 `<br>`）。 */
export function toLines(text: string, tokens: HighlightToken[]): LineSegment[][] {
    const lines: LineSegment[][] = [];
    let lineStart = 0;
    let tokenIndex = 0;
    for (const lineText of text.split('\n')) {
        const lineEnd = lineStart + lineText.length;
        const segments: LineSegment[] = [];
        let cursor = lineStart;
        while (tokenIndex < tokens.length && tokens[tokenIndex].start < lineEnd) {
            const token = tokens[tokenIndex];
            const start = Math.max(token.start, lineStart);
            const end = Math.min(token.end, lineEnd);
            if (start > cursor) {
                pushSegment(segments, text.slice(cursor, start), null);
            }
            if (end > start) {
                pushSegment(segments, text.slice(start, end), token.classes);
            }
            cursor = Math.max(cursor, end);
            if (token.end > lineEnd) {
                break; // 跨行的 token（如多行注释）在下一行继续
            }
            tokenIndex++;
        }
        if (lineEnd > cursor) {
            pushSegment(segments, text.slice(cursor, lineEnd), null);
        }
        lines.push(segments);
        lineStart = lineEnd + 1;
    }
    return lines;
}

function pushSegment(segments: LineSegment[], text: string, classes: string | null): void {
    const last = segments[segments.length - 1];
    if (last && last.classes === classes) {
        last.text += text;
        return;
    }
    segments.push({ text, classes });
}

/**
 * 读取视图中 [from, to) 子节点的分段；含非本插件的元素（评论、修订建议等的高亮）时返回 null。
 */
function readSegments(code: ViewElement, from: number, to: number): LineSegment[] | null {
    const segments: LineSegment[] = [];
    for (let i = from; i < to; i++) {
        const node = code.getChild(i)!;
        if (node.is('$text')) {
            pushSegment(segments, node.data, null);
            continue;
        }
        const child = node.is('attributeElement') && node.getCustomProperty(SPAN_PROPERTY) && node.childCount === 1
            ? node.getChild(0)
            : null;
        if (!child || !child.is('$text')) {
            return null;
        }
        pushSegment(segments, child.data, (node as ViewElement).getAttribute('class') ?? null);
    }
    return segments;
}

function sameSegments(a: LineSegment[], b: LineSegment[]): boolean {
    return a.length === b.length && a.every((s, i) => s.text === b[i].text && s.classes === b[i].classes);
}

function joinText(segments: LineSegment[]): string {
    return segments.map((s) => s.text).join('');
}

/**
 * 把代码块视图（`<code>` 元素：文本与 `<br>` 交替）逐行对齐到期望分段。
 * 自后向前处理，前面各行的子节点下标保持不变。
 */
export function reconcileCodeView(writer: DowncastWriter, code: ViewElement, lines: LineSegment[][]): void {
    const bounds: Array<[number, number]> = [];
    let from = 0;
    for (let i = 0; i < code.childCount; i++) {
        if (code.getChild(i)!.is('element', 'br')) {
            bounds.push([from, i]);
            from = i + 1;
        }
    }
    bounds.push([from, code.childCount]);
    if (bounds.length !== lines.length) {
        return; // 视图与模型暂不一致（转换未完成），下次变更再处理
    }
    for (let line = bounds.length - 1; line >= 0; line--) {
        const [start, end] = bounds[line];
        const actual = readSegments(code, start, end);
        const wanted = lines[line];
        if (!actual || sameSegments(actual, wanted) || joinText(actual) !== joinText(wanted)) {
            continue;
        }
        if (end > start) {
            writer.remove(writer.createRange(writer.createPositionAt(code, start), writer.createPositionAt(code, end)));
        }
        const lineText = joinText(wanted);
        if (!lineText) {
            continue;
        }
        writer.insert(writer.createPositionAt(code, start), writer.createText(lineText));
        // 自右向左包裹：左侧剩余部分始终是第 start 个子节点
        let offset = lineText.length;
        for (let i = wanted.length - 1; i >= 0; i--) {
            const segment = wanted[i];
            offset -= segment.text.length;
            if (!segment.classes) {
                continue;
            }
            const textNode = code.getChild(start)!;
            const span = writer.createAttributeElement('span', { class: segment.classes }, { priority: SPAN_PRIORITY });
            writer.setCustomProperty(SPAN_PROPERTY, true, span);
            writer.wrap(writer.createRange(
                writer.createPositionAt(textNode, offset),
                writer.createPositionAt(textNode, offset + segment.text.length),
            ), span);
        }
    }
}

/** 以给定选项生成插件类（连接器在模块级创建一次，所有编辑器实例共用）。 */
export function createCodeBlockHighlightingPlugin(options: CodeBlockHighlightingOptions): typeof CodeBlockHighlighting {
    return class extends CodeBlockHighlighting {
        static override options = options;
    };
}

function addCodeBlock(blocks: Set<ModelElement>, node: unknown): void {
    let current = node as (ModelNode & { name?: string }) | null | undefined;
    while (current) {
        if (current.is('element', 'codeBlock')) {
            blocks.add(current as unknown as ModelElement);
            return;
        }
        current = current.parent as typeof current;
    }
}

/** add-on 使用的插件类：首次启用时按需加载 highlight.js 的 common 语言包（约 36 种语言）。 */
const VaadinCodeBlockHighlighting = createCodeBlockHighlightingPlugin({
    loadHighlighter: async () => (await import('highlight.js/lib/common')).default as unknown as HljsLike,
    onError: (error) => console.warn('[VaadinCKEditor] Code block highlighting failed:', error),
});

export default VaadinCodeBlockHighlighting;
