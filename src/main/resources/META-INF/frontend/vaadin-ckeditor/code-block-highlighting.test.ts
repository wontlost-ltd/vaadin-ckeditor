/**
 * 代码块语法高亮插件：用真实的 CKEditor（jsdom）与真实的 highlight.js 验证
 * 编辑视图着色、数据输出干净、撤销 / 重做不受影响、切换语言重新着色、停用移除全部高亮。
 */
import { describe, it, expect, beforeAll, afterEach, vi } from 'vitest';
import { ClassicEditor, Essentials, Paragraph, CodeBlock, Bold } from 'ckeditor5';
import hljs from 'highlight.js/lib/common';
import { PLUGIN_CONFIG_EVENT } from './plugin-resolver';
import VaadinCodeBlockHighlighting, {
    createCodeBlockHighlightingPlugin,
    codeBlockText,
    resolveHljsLanguage,
    tokenize,
    type CodeBlockHighlighting,
    type HljsLike,
} from './code-block-highlighting';

// jsdom 没有 ResizeObserver，CKEditor 工具栏分组需要它
beforeAll(() => {
    const g = globalThis as { ResizeObserver?: unknown };
    g.ResizeObserver ??= class { observe(): void {} unobserve(): void {} disconnect(): void {} };
});

const HL = hljs as unknown as HljsLike;
const JS_CODE = 'const answer = 42;\n// note\nreturn "x";';
const JS_HTML = `<pre><code class="language-javascript">${JS_CODE}</code></pre>`;

let editors: ClassicEditor[] = [];
afterEach(async () => {
    await Promise.all(editors.map((e) => e.destroy()));
    editors = [];
    document.body.innerHTML = '';
});

async function createEditor(initialData: string, loads = { count: 0 }) {
    const Plugin = createCodeBlockHighlightingPlugin({
        loadHighlighter: async () => { loads.count++; return HL; },
        debounceMs: 0,
    });
    const element = document.createElement('div');
    document.body.appendChild(element);
    const editor = await ClassicEditor.create(element, {
        licenseKey: 'GPL',
        plugins: [Essentials, Paragraph, Bold, CodeBlock, Plugin],
        initialData,
    } as never);
    editors.push(editor);
    const highlighter = editor.plugins.get('VaadinCodeBlockHighlighting') as unknown as CodeBlockHighlighting;
    return { editor, highlighter, loads };
}

function editingHtml(editor: ClassicEditor): string {
    return (editor.editing.view.getDomRoot() as HTMLElement).innerHTML;
}

function highlightSpans(editor: ClassicEditor): HTMLElement[] {
    return Array.from((editor.editing.view.getDomRoot() as HTMLElement).querySelectorAll('pre span[class*="hljs-"]'));
}

/** 本插件在编辑视图中的 span 数（替代原 marker 计数）。 */
function markerCount(editor: ClassicEditor): number {
    return highlightSpans(editor).length;
}

describe('tokenize / resolveHljsLanguage', () => {
    it('压平嵌套 span，区间覆盖原文且只含有类的片段', () => {
        const code = '<div class="a">x</div>';
        const tokens = tokenize(HL, code, 'xml');
        expect(tokens.length).toBeGreaterThan(0);
        for (const t of tokens) {
            expect(t.end).toBeGreaterThan(t.start);
            expect(t.classes).toMatch(/hljs-/);
        }
        // 属性名带上祖先 tag 的类
        const attr = tokens.find((t) => code.slice(t.start, t.end) === 'class');
        expect(attr?.classes).toBe('hljs-tag hljs-attr');
        // 区间升序且不重叠
        for (let i = 1; i < tokens.length; i++) {
            expect(tokens[i].start).toBeGreaterThanOrEqual(tokens[i - 1].end);
        }
    });

    it('转义字符不改变偏移', () => {
        const code = 'if (a < b && c > "d") {}';
        const tokens = tokenize(HL, code, 'javascript');
        const str = tokens.find((t) => t.classes === 'hljs-string');
        expect(code.slice(str!.start, str!.end)).toBe('"d"');
    });

    it('空代码不分词', () => {
        expect(tokenize(HL, '', 'javascript')).toEqual([]);
    });

    it('CKEditor 默认语言映射到 highlight.js，纯文本与未知语言不着色', () => {
        expect(resolveHljsLanguage(HL, 'cs')).toBe('csharp');
        expect(resolveHljsLanguage(HL, 'html')).toBe('xml');
        expect(resolveHljsLanguage(HL, 'plaintext')).toBeNull();
        expect(resolveHljsLanguage(HL, 'no-such-language')).toBeNull();
        expect(resolveHljsLanguage(HL, undefined)).toBeNull();
        // CKEditor 默认 codeBlock.languages 全部可着色（plaintext 除外）
        for (const lang of ['c', 'cs', 'cpp', 'css', 'diff', 'html', 'java', 'javascript', 'php', 'python', 'ruby', 'typescript', 'xml']) {
            expect(resolveHljsLanguage(HL, lang), lang).not.toBeNull();
        }
        // 自定义 codeBlock.languages 中 highlight.js 认识的名称与别名
        expect(resolveHljsLanguage(HL, 'kotlin')).toBe('kotlin');
        expect(resolveHljsLanguage(HL, 'SQL')).toBe('sql');
    });
});

describe('CodeBlockHighlighting 插件', () => {
    it('默认不工作：不加载 highlight.js、不产生 marker、不加根类', async () => {
        const { editor, loads } = await createEditor(JS_HTML);
        expect(loads.count).toBe(0);
        expect(markerCount(editor)).toBe(0);
        expect(highlightSpans(editor)).toHaveLength(0);
        expect((editor.editing.view.getDomRoot() as HTMLElement).classList.contains('ck-code-hl')).toBe(false);
    });

    it('启用后编辑视图出现 hljs span，getData() 与启用前逐字相同', async () => {
        const { editor, highlighter, loads } = await createEditor(JS_HTML);
        const before = editor.getData();
        await highlighter.setEnabled(true);

        expect(loads.count).toBe(1);
        const spans = highlightSpans(editor);
        expect(spans.map((s) => s.textContent)).toEqual(expect.arrayContaining(['const', '42', '// note', 'return', '"x"']));
        expect(spans.find((s) => s.textContent === 'const')!.className).toBe('hljs-keyword');
        expect(editingHtml(editor)).toContain('hljs-comment');

        expect(editor.getData()).toBe(before);
        expect(editor.getData()).not.toMatch(/hljs|<span/);
    });

    it('着色不进撤销栈，也不触发 change:data，模型无 marker', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        let dataChanges = 0;
        editor.model.document.on('change:data', () => { dataChanges++; });
        await highlighter.setEnabled(true);
        expect(markerCount(editor)).toBeGreaterThan(0);
        expect(Array.from(editor.model.markers)).toHaveLength(0);
        expect(dataChanges).toBe(0);
        expect(editor.commands.get('undo')!.isEnabled).toBe(false);
    });

    it('输入时重新着色：新关键字被着色，数据仍干净', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        await highlighter.setEnabled(true);

        const block = editor.model.document.getRoot()!.getChild(0)!;
        editor.model.change((writer) => {
            writer.insertText('let ', editor.model.createPositionAt(block as never, 0));
        });
        highlighter.flush();

        const keywords = highlightSpans(editor).filter((s) => s.className === 'hljs-keyword').map((s) => s.textContent);
        expect(keywords).toContain('let');
        expect(editor.getData()).toBe(`<pre><code class="language-javascript">let ${JS_CODE}</code></pre>`);
    });

    it('撤销 / 重做只作用于文本，高亮随之更新', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        await highlighter.setEnabled(true);
        const original = editor.getData();

        const block = editor.model.document.getRoot()!.getChild(0)!;
        editor.model.change((writer) => {
            // 代码块内的换行是 softBreak 元素（与按 Enter 一致），不是文本里的 \n
            writer.insertElement('softBreak', editor.model.createPositionAt(block as never, 0));
            writer.insertText('var x;', editor.model.createPositionAt(block as never, 0));
        });
        highlighter.flush();
        const typed = editor.getData();
        expect(typed).not.toBe(original);

        editor.execute('undo');
        highlighter.flush();
        expect(editor.getData()).toBe(original);
        expect(highlightSpans(editor).map((s) => s.textContent)).not.toContain('var');
        expect(editor.commands.get('undo')!.isEnabled).toBe(false);

        editor.execute('redo');
        highlighter.flush();
        expect(editor.getData()).toBe(typed);
        expect(highlightSpans(editor).map((s) => s.textContent)).toContain('var');
    });

    it('输入只重建变化的行，其他行的 DOM 节点保持不变', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        await highlighter.setEnabled(true);
        const before = highlightSpans(editor);
        const lastLineSpan = before[before.length - 1]; // 第三行的 "x" 字符串

        const block = editor.model.document.getRoot()!.getChild(0)!;
        editor.model.change((writer) => {
            writer.insertText('let ', editor.model.createPositionAt(block as never, 0));
        });
        highlighter.flush();
        expect(highlightSpans(editor)).toContain(lastLineSpan);
    });

    it('着色后模型选区不变，视图选区仍映射到同一位置', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        const block = editor.model.document.getRoot()!.getChild(0)!;
        editor.model.change((writer) => {
            writer.setSelection(editor.model.createPositionAt(block as never, 8)); // "const an|swer"
        });
        await highlighter.setEnabled(true);
        const modelPos = editor.model.document.selection.getFirstPosition()!;
        expect(modelPos.offset).toBe(8);
        const viewPos = editor.editing.view.document.selection.getFirstPosition()!;
        expect(editor.editing.mapper.toModelPosition(viewPos).isEqual(modelPos)).toBe(true);
    });

    it('含其他功能高亮（如评论 marker）的行保持原样，其余行照常着色', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        editor.conversion.for('editingDowncast').markerToHighlight({
            model: 'comment', view: { classes: 'ck-comment-marker' },
        });
        const block = editor.model.document.getRoot()!.getChild(0)!;
        editor.model.change((writer) => {
            writer.addMarker('comment:t1', {
                range: editor.model.createRange(
                    editor.model.createPositionAt(block as never, 6),
                    editor.model.createPositionAt(block as never, 12),
                ),
                usingOperation: false, affectsData: true,
            });
        });
        await highlighter.setEnabled(true);
        const root = editor.editing.view.getDomRoot() as HTMLElement;
        expect(root.querySelector('.ck-comment-marker')?.textContent).toBe('answer');
        // 第二、三行照常着色
        expect(highlightSpans(editor).map((s) => s.textContent)).toEqual(expect.arrayContaining(['// note', 'return']));
        expect(editor.getData()).not.toMatch(/hljs/);
    });

    it('切换代码块语言会重新着色', async () => {
        const { editor, highlighter } = await createEditor('<pre><code class="language-plaintext">def f(): return None</code></pre>');
        await highlighter.setEnabled(true);
        expect(highlightSpans(editor)).toHaveLength(0);

        editor.execute('codeBlock', { language: 'python', forceValue: true });
        highlighter.flush();
        const texts = highlightSpans(editor).map((s) => s.textContent);
        expect(texts).toEqual(expect.arrayContaining(['def', 'return', 'None']));
        expect(editor.getData()).toBe('<pre><code class="language-python">def f(): return None</code></pre>');

        editor.execute('codeBlock', { language: 'plaintext', forceValue: true });
        highlighter.flush();
        expect(highlightSpans(editor)).toHaveLength(0);
        expect(markerCount(editor)).toBe(0);
    });

    it('新插入的代码块与删除的代码块都被处理', async () => {
        const { editor, highlighter } = await createEditor('<p>text</p>');
        await highlighter.setEnabled(true);
        editor.setData(JS_HTML + '<p>after</p>');
        highlighter.flush();
        expect(highlightSpans(editor).length).toBeGreaterThan(0);

        editor.setData('<p>no code</p>');
        highlighter.flush();
        expect(markerCount(editor)).toBe(0);
    });

    it('软换行跨行的注释也按模型偏移着色', async () => {
        const code = '/* a\nb */ let x;';
        const { editor, highlighter } = await createEditor(`<pre><code class="language-javascript">${code}</code></pre>`);
        await highlighter.setEnabled(true);
        const block = editor.model.document.getRoot()!.getChild(0)!;
        expect(codeBlockText(block as never)).toBe(code);
        const comment = highlightSpans(editor).filter((s) => s.className === 'hljs-comment');
        expect(comment.map((s) => s.textContent).join('|')).toBe('/* a|b */');
    });

    it('停用移除全部高亮与根类，再启用不重复加载 highlight.js', async () => {
        const { editor, highlighter, loads } = await createEditor(JS_HTML);
        await highlighter.setEnabled(true);
        await highlighter.setEnabled(false);
        expect(markerCount(editor)).toBe(0);
        expect(highlightSpans(editor)).toHaveLength(0);
        expect((editor.editing.view.getDomRoot() as HTMLElement).classList.contains('ck-code-hl')).toBe(false);

        await highlighter.setEnabled(true);
        expect(loads.count).toBe(1);
        expect(highlightSpans(editor).length).toBeGreaterThan(0);
    });

    it('配色与明暗以编辑根上的类表达，不进入数据', async () => {
        const { editor, highlighter } = await createEditor(JS_HTML);
        await highlighter.setEnabled(true);
        const root = editor.editing.view.getDomRoot() as HTMLElement;
        expect(root.classList.contains('ck-code-hl')).toBe(true);
        expect(root.classList.contains('ck-code-hl_theme-auto')).toBe(true);
        expect(root.classList.contains('ck-editor__editable')).toBe(true);

        highlighter.setDark(true);
        expect(root.classList.contains('ck-code-hl_dark')).toBe(true);

        highlighter.setTheme('atom-one-dark');
        expect(root.classList.contains('ck-code-hl_theme-atom-one-dark')).toBe(true);
        expect(root.classList.contains('ck-code-hl_theme-auto')).toBe(false);

        highlighter.setTheme('bogus');
        expect(root.classList.contains('ck-code-hl_theme-auto')).toBe(true);
        expect(editor.getData()).not.toMatch(/ck-code-hl/);
    });

    it('按编辑器配置 codeBlockHighlighting 启用（Java CustomPlugin 配置的路径），运行中经 vaadin:pluginConfig 切换', async () => {
        const Plugin = createCodeBlockHighlightingPlugin({ loadHighlighter: async () => HL, debounceMs: 0 });
        const host = document.createElement('div');
        host.setAttribute('data-ck-theme', 'dark');
        const element = document.createElement('div');
        host.appendChild(element);
        document.body.appendChild(host);
        const editor = await ClassicEditor.create(element, {
            licenseKey: 'GPL', plugins: [Essentials, Paragraph, CodeBlock, Plugin], initialData: JS_HTML,
            codeBlockHighlighting: { enabled: true, theme: 'github' },
        } as never);
        editors.push(editor);
        const highlighter = editor.plugins.get('VaadinCodeBlockHighlighting') as unknown as CodeBlockHighlighting;
        await vi.waitFor(() => expect(highlightSpans(editor).length).toBeGreaterThan(0));
        const root = editor.editing.view.getDomRoot() as HTMLElement;
        expect(root.classList.contains('ck-code-hl_theme-github')).toBe(true);
        // 宿主的 data-ck-theme（theme-manager 写入）决定明暗
        expect(root.classList.contains('ck-code-hl_dark')).toBe(true);
        host.setAttribute('data-ck-theme', 'light');
        await vi.waitFor(() => expect(root.classList.contains('ck-code-hl_dark')).toBe(false));

        editor.fire(PLUGIN_CONFIG_EVENT, 'codeBlockHighlighting', { enabled: true, theme: 'atom-one-dark' });
        expect(root.classList.contains('ck-code-hl_theme-atom-one-dark')).toBe(true);
        editor.fire(PLUGIN_CONFIG_EVENT, 'otherPlugin', { enabled: false });
        expect(highlighter.isHighlighting).toBe(true);
        editor.fire(PLUGIN_CONFIG_EVENT, 'codeBlockHighlighting', { enabled: false });
        await vi.waitFor(() => expect(highlightSpans(editor)).toHaveLength(0));
    });

    it('默认导出是 add-on 注册的插件类（按需加载 highlight.js）', () => {
        expect((VaadinCodeBlockHighlighting as unknown as { pluginName: string }).pluginName).toBe('VaadinCodeBlockHighlighting');
    });

    it('超过长度上限的代码块不着色', async () => {
        const Plugin = createCodeBlockHighlightingPlugin({ loadHighlighter: async () => HL, maxBlockLength: 10, debounceMs: 0 });
        const element = document.createElement('div');
        document.body.appendChild(element);
        const editor = await ClassicEditor.create(element, {
            licenseKey: 'GPL', plugins: [Essentials, Paragraph, CodeBlock, Plugin], initialData: JS_HTML,
        } as never);
        editors.push(editor);
        await (editor.plugins.get('VaadinCodeBlockHighlighting') as unknown as CodeBlockHighlighting).setEnabled(true);
        expect(markerCount(editor)).toBe(0);
    });

    it('highlight.js 加载失败时回到停用状态并报告错误', async () => {
        const errors: unknown[] = [];
        const Plugin = createCodeBlockHighlightingPlugin({
            loadHighlighter: () => Promise.reject(new Error('chunk 404')),
            onError: (e) => errors.push(e),
        });
        const element = document.createElement('div');
        document.body.appendChild(element);
        const editor = await ClassicEditor.create(element, {
            licenseKey: 'GPL', plugins: [Essentials, Paragraph, CodeBlock, Plugin], initialData: JS_HTML,
        } as never);
        editors.push(editor);
        const highlighter = editor.plugins.get('VaadinCodeBlockHighlighting') as unknown as CodeBlockHighlighting;
        await highlighter.setEnabled(true);
        expect(highlighter.isHighlighting).toBe(false);
        expect(errors).toHaveLength(1);
        expect(editor.getData()).toBe(JS_HTML.replace(JS_CODE, JS_CODE));
    });

    it.each([200])('性能（jsdom，真实浏览器数据见 Playwright）：%i 行代码块首次着色与单次按键增量着色的耗时', async (lines) => {
        const line = 'function f(a, b) { return a + b * 2; } // comment "s"';
        const big = Array.from({ length: lines }, () => line).join('\n');
        const { editor, highlighter } = await createEditor(`<pre><code class="language-javascript">${big}</code></pre>`);
        let start = performance.now();
        await highlighter.setEnabled(true);
        const first = performance.now() - start;
        expect(markerCount(editor)).toBeGreaterThan(lines * 5);

        const block = editor.model.document.getRoot()!.getChild(0)!;
        editor.model.change((writer) => {
            writer.insertText('x', editor.model.createPositionAt(block as never, 0));
        });
        start = performance.now();
        highlighter.flush();
        const keystroke = performance.now() - start;
        // 记录到输出，供 USER_GUIDE 的性能表引用（jsdom 中的数值，偏保守）
        console.info(`[perf] ${lines} lines / ${big.length} chars: first ${first.toFixed(0)}ms, keystroke ${keystroke.toFixed(0)}ms, markers ${markerCount(editor)}`);
        expect(editor.getData()).not.toMatch(/hljs/);
    }, 30_000);
});
