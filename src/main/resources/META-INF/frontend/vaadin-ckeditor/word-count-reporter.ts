/**
 * 实时字数统计（add-on issue #70，上游 ckeditor5#2573）。
 *
 * 计数来自 CKEditor 的 WordCount 插件：其 `wordCount.onUpdate` 在 `change:data` 后以 250 ms 节流触发
 * （@ckeditor/ckeditor5-word-count 49.1.0 `init()`：`throttle(this._refreshStats, 250)`）。
 * 转发到服务端时再节流到每 500 ms 至多一次（首个立即发出，窗口内的最后一次在窗口结束时补发），
 * 且只在数值变化时发送——连续输入时每秒最多 2 次 RPC，停止输入后最终数值一定到达服务端。
 */
export interface WordCountStats {
    words: number;
    characters: number;
}

/** 服务端上报的最小间隔（毫秒） */
export const WORD_COUNT_REPORT_INTERVAL_MS = 500;

/** 计数容器的类名与超限修饰类名（word-count.css） */
export const WORD_COUNT_CONTAINER_CLASS = 'word-count-container';
export const WORD_COUNT_OVER_LIMIT_CLASS = 'word-count-container--over-limit';

export interface ReporterTimers {
    now(): number;
    setTimeout(fn: () => void, ms: number): unknown;
    clearTimeout(handle: unknown): void;
}

const DEFAULT_TIMERS: ReporterTimers = {
    now: () => Date.now(),
    setTimeout: (fn, ms) => setTimeout(fn, ms),
    clearTimeout: (handle) => clearTimeout(handle as ReturnType<typeof setTimeout>),
};

export interface WordCountReporter {
    /** 记录最新计数；按节流规则发送 */
    update(stats: WordCountStats): void;
    /** 忘记上次已发送的值：下一次 update 无论是否变化都会发送（监听器新注册时用） */
    reset(): void;
    /** 取消待发送的补发（编辑器销毁时） */
    cancel(): void;
}

function same(a: WordCountStats | null, b: WordCountStats): boolean {
    return a !== null && a.words === b.words && a.characters === b.characters;
}

export function createWordCountReporter(
    send: (stats: WordCountStats) => void,
    intervalMs: number = WORD_COUNT_REPORT_INTERVAL_MS,
    timers: ReporterTimers = DEFAULT_TIMERS,
): WordCountReporter {
    let lastSent: WordCountStats | null = null;
    let lastSendTime = Number.NEGATIVE_INFINITY;
    let pending: WordCountStats | null = null;
    let timer: unknown = null;

    const flush = (): void => {
        timer = null;
        const next = pending;
        pending = null;
        if (next && !same(lastSent, next)) {
            emit(next);
        }
    };

    const emit = (stats: WordCountStats): void => {
        lastSent = { ...stats };
        lastSendTime = timers.now();
        send(lastSent);
    };

    return {
        update(stats) {
            if (timer !== null) {
                pending = { ...stats };
                return;
            }
            if (same(lastSent, stats)) {
                return;
            }
            const wait = lastSendTime + intervalMs - timers.now();
            if (wait <= 0) {
                emit(stats);
                return;
            }
            pending = { ...stats };
            timer = timers.setTimeout(flush, wait);
        },
        reset() {
            lastSent = null;
        },
        cancel() {
            if (timer !== null) {
                timers.clearTimeout(timer);
            }
            timer = null;
            pending = null;
        },
    };
}

/** 是否超过任一上限（上限 ≤ 0 表示不限） */
export function exceedsLimit(stats: WordCountStats | null, maxWords: number, maxCharacters: number): boolean {
    if (!stats) {
        return false;
    }
    return (maxWords > 0 && stats.words > maxWords) || (maxCharacters > 0 && stats.characters > maxCharacters);
}
