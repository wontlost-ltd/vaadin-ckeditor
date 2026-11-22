/**
 * VaadinWordCount 插件单测（#70）：监听 WordCount 的 update、就绪时上报初始计数、节流上报、
 * 计数元素放入容器、超限类名、上限更新、resend、销毁清理。
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

vi.mock('ckeditor5', () => {
    type Listener = (...args: unknown[]) => void;
    class Emitter {
        private listeners: Record<string, Listener[]> = {};
        on(event: string, cb: Listener) { (this.listeners[event] ??= []).push(cb); }
        fire(event: string, ...args: unknown[]) { (this.listeners[event] ?? []).forEach((cb) => cb({}, ...args)); }
    }
    class MockPlugin extends Emitter {
        editor: any;
        constructor(editor: any) { super(); this.editor = editor; }
        listenTo(emitter: Emitter, event: string, cb: Listener) { emitter.on(event, cb); }
        destroy() {}
    }
    class WordCount extends Emitter {
        words = 0;
        characters = 0;
        wordCountContainer = document.createElement('div');
    }
    return { Plugin: MockPlugin, WordCount };
});

import { WordCount } from 'ckeditor5';
import VaadinWordCount from './word-count-plugin';
import { WORD_COUNT_OVER_LIMIT_CLASS } from './word-count-reporter';

type Stats = { words: number; characters: number };

function setup(config: Record<string, unknown>) {
    const wordCount = new (WordCount as any)();
    const editorListeners: Record<string, () => void> = {};
    const editor = {
        config: { get: (key: string) => (key === 'vaadinWordCount' ? config : undefined) },
        plugins: { get: () => wordCount },
        on: (event: string, cb: () => void) => { editorListeners[event] = cb; },
    };
    const plugin = new VaadinWordCount(editor as any);
    plugin.init();
    return { plugin, wordCount, ready: () => editorListeners.ready?.() };
}

describe('VaadinWordCount', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('reports the initial count when the editor is ready and each later update, throttled', () => {
        const reports: Stats[] = [];
        const { wordCount, ready } = setup({ onReport: (s: Stats) => reports.push(s) });
        wordCount.words = 2;
        wordCount.characters = 9;
        ready();
        expect(reports).toEqual([{ words: 2, characters: 9 }]);

        wordCount.fire('update', { words: 3, characters: 12 });
        wordCount.fire('update', { words: 4, characters: 15 });
        expect(reports).toHaveLength(1);
        vi.advanceTimersByTime(500);
        expect(reports).toEqual([{ words: 2, characters: 9 }, { words: 4, characters: 15 }]);
    });

    it('puts the WordCount element into the container and marks it over the limit', () => {
        const container = document.createElement('div');
        const { plugin, wordCount } = setup({ container, maxWords: 3 });
        expect(container.firstChild).toBe(wordCount.wordCountContainer);

        wordCount.fire('update', { words: 4, characters: 10 });
        expect(plugin.overLimit).toBe(true);
        expect(container.classList.contains(WORD_COUNT_OVER_LIMIT_CLASS)).toBe(true);

        plugin.setLimits(5, 0);
        expect(container.classList.contains(WORD_COUNT_OVER_LIMIT_CLASS)).toBe(false);
        plugin.setLimits(0, 9);
        expect(container.classList.contains(WORD_COUNT_OVER_LIMIT_CLASS)).toBe(true);
    });

    it('resend reports the current count again even if unchanged', () => {
        const reports: Stats[] = [];
        const { plugin, wordCount } = setup({ onReport: (s: Stats) => reports.push(s) });
        wordCount.fire('update', { words: 1, characters: 1 });
        vi.advanceTimersByTime(1000);
        plugin.resend();
        expect(reports).toEqual([{ words: 1, characters: 1 }, { words: 1, characters: 1 }]);
    });

    it('works without onReport or container', () => {
        const { plugin, wordCount } = setup({});
        wordCount.fire('update', { words: 1, characters: 1 });
        expect(plugin.stats).toEqual({ words: 1, characters: 1 });
        expect(() => plugin.resend()).not.toThrow();
    });

    it('destroy cancels the pending report and detaches the counter', () => {
        const reports: Stats[] = [];
        const container = document.createElement('div');
        const { plugin, wordCount } = setup({ container, maxWords: 1, onReport: (s: Stats) => reports.push(s) });
        wordCount.fire('update', { words: 2, characters: 2 });
        wordCount.fire('update', { words: 3, characters: 3 });
        plugin.destroy();
        vi.advanceTimersByTime(1000);
        expect(reports).toEqual([{ words: 2, characters: 2 }]);
        expect(container.childElementCount).toBe(0);
        expect(container.classList.contains(WORD_COUNT_OVER_LIMIT_CLASS)).toBe(false);
    });
});
