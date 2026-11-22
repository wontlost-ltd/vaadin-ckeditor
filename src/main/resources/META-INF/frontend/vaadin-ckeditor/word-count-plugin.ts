/**
 * VaadinWordCount：把 CKEditor WordCount 插件接到 Vaadin 的字数 API 上（#70）。
 *
 * 以 add-on 既有的自定义插件机制提供：本模块在加载时用 {@link registerCKEditorPlugin} 注册到全局插件注册表，
 * Java 端在需要时（addWordCountChangeListener / setWordCountVisible）把它作为 CustomPlugin
 * （`CustomPlugin.fromRegistry("VaadinWordCount")`）加入插件列表。应用可以继承本类并以同名重新注册来扩展或替换它
 * （注册表后写覆盖先写）。
 *
 * 职责：
 * - 依赖 WordCount（`requires`），监听其 `update`（250 ms 节流）并在就绪时补一次初始计数；
 * - 按 `vaadinWordCount.onReport` 回调上报，节流到每 500 ms 至多一次、只在变化时（word-count-reporter.ts）；
 * - 把 WordCount 的计数元素放进 `vaadinWordCount.container`，超过上限时给容器加超限类名。
 *
 * 配置（由连接器注入，函数与 DOM 元素无法从 Java 传入）：
 * `vaadinWordCount: { onReport?(stats), container?: HTMLElement, maxWords?: number, maxCharacters?: number }`
 */
import { Plugin, WordCount } from 'ckeditor5';
import {
    createWordCountReporter,
    exceedsLimit,
    WORD_COUNT_OVER_LIMIT_CLASS,
    type WordCountReporter,
    type WordCountStats,
} from './word-count-reporter';

export interface VaadinWordCountConfig {
    onReport?: (stats: WordCountStats) => void;
    container?: HTMLElement | null;
    maxWords?: number;
    maxCharacters?: number;
}

export const VAADIN_WORD_COUNT_PLUGIN = 'VaadinWordCount';

export default class VaadinWordCount extends Plugin {
    static get pluginName() {
        return 'VaadinWordCount' as const;
    }

    static get requires() {
        return [WordCount] as const;
    }

    /** 最近一次计数（就绪前为 null） */
    stats: WordCountStats | null = null;

    private reporter: WordCountReporter | null = null;
    private container: HTMLElement | null = null;
    private maxWords = 0;
    private maxCharacters = 0;

    init(): void {
        const config = (this.editor.config.get('vaadinWordCount') ?? {}) as VaadinWordCountConfig;
        const onReport = config.onReport;
        this.reporter = onReport ? createWordCountReporter((stats) => onReport(stats)) : null;
        this.maxWords = config.maxWords ?? 0;
        this.maxCharacters = config.maxCharacters ?? 0;
        this.container = config.container ?? null;

        const wordCount = this.editor.plugins.get(WordCount);
        this.container?.appendChild(wordCount.wordCountContainer);
        this.listenTo(wordCount, 'update', (_evt, stats: WordCountStats) => this.apply(stats));
        // WordCount 只在 change:data 后更新：就绪时按需读取一次初始计数
        this.editor.on('ready', () => this.apply({ words: wordCount.words, characters: wordCount.characters }));
    }

    /** 更新上限（连接器在服务端修改 setMaxWords / setMaxCharacters 后调用） */
    setLimits(maxWords: number, maxCharacters: number): void {
        this.maxWords = maxWords;
        this.maxCharacters = maxCharacters;
        this.updateLimitState();
    }

    /** 无论数值是否变化，重新上报当前计数（服务端新注册监听器时） */
    resend(): void {
        if (this.stats && this.reporter) {
            this.reporter.reset();
            this.reporter.update(this.stats);
        }
    }

    /** 当前计数是否超过任一上限 */
    get overLimit(): boolean {
        return exceedsLimit(this.stats, this.maxWords, this.maxCharacters);
    }

    protected apply(stats: WordCountStats): void {
        this.stats = { words: stats.words, characters: stats.characters };
        this.updateLimitState();
        this.reporter?.update(this.stats);
    }

    private updateLimitState(): void {
        this.container?.classList.toggle(WORD_COUNT_OVER_LIMIT_CLASS, this.overLimit);
    }

    override destroy(): void {
        this.reporter?.cancel();
        this.container?.classList.remove(WORD_COUNT_OVER_LIMIT_CLASS);
        if (this.container) {
            const wordCount = this.editor.plugins.get(WordCount);
            wordCount.wordCountContainer.remove();
        }
        super.destroy();
    }
}
