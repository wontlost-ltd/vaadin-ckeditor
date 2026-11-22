/**
 * word-count-reporter 单测（#70）：节流到每 500 ms 至多一次、首个立即发送、窗口末尾补发最后一次、
 * 数值不变不发送、reset 后强制重发、cancel 丢弃待发；以及超限判定。
 */
import { describe, it, expect, beforeEach } from 'vitest';
import {
    createWordCountReporter,
    exceedsLimit,
    type ReporterTimers,
    type WordCountStats,
} from './word-count-reporter';

/** 可手动推进的假时钟 */
function fakeTimers() {
    let now = 0;
    let tasks: { at: number; fn: () => void; id: number }[] = [];
    let nextId = 1;
    const timers: ReporterTimers = {
        now: () => now,
        setTimeout: (fn, ms) => {
            const id = nextId++;
            tasks.push({ at: now + ms, fn, id });
            return id;
        },
        clearTimeout: (id) => { tasks = tasks.filter((t) => t.id !== id); },
    };
    const advance = (ms: number) => {
        now += ms;
        const due = tasks.filter((t) => t.at <= now);
        tasks = tasks.filter((t) => t.at > now);
        due.forEach((t) => t.fn());
    };
    return { timers, advance, pending: () => tasks.length };
}

const s = (words: number, characters: number): WordCountStats => ({ words, characters });

describe('createWordCountReporter', () => {
    let sent: WordCountStats[];
    let clock: ReturnType<typeof fakeTimers>;
    let reporter: ReturnType<typeof createWordCountReporter>;

    beforeEach(() => {
        sent = [];
        clock = fakeTimers();
        reporter = createWordCountReporter((stats) => sent.push(stats), 500, clock.timers);
    });

    it('sends the first update immediately', () => {
        reporter.update(s(1, 5));
        expect(sent).toEqual([s(1, 5)]);
    });

    it('coalesces updates within the interval and sends the last one when it ends', () => {
        reporter.update(s(1, 1));
        clock.advance(100);
        reporter.update(s(1, 2));
        clock.advance(100);
        reporter.update(s(2, 4));
        expect(sent).toEqual([s(1, 1)]);
        clock.advance(299);
        expect(sent).toEqual([s(1, 1)]);
        clock.advance(1);
        expect(sent).toEqual([s(1, 1), s(2, 4)]);
    });

    it('never sends more than once per interval while typing continuously', () => {
        for (let i = 1; i <= 40; i++) {           // 10 s，每 250 ms 一次（WordCount 插件的节流频率）
            reporter.update(s(i, i * 5));
            clock.advance(250);
        }
        clock.advance(500);
        expect(sent.length).toBeLessThanOrEqual(21);
        expect(sent.at(-1)).toEqual(s(40, 200));    // 最终值一定到达
    });

    it('skips unchanged counts', () => {
        reporter.update(s(3, 10));
        clock.advance(1000);
        reporter.update(s(3, 10));
        expect(sent).toEqual([s(3, 10)]);
    });

    it('does not send a trailing update that reverted to the last sent value', () => {
        reporter.update(s(3, 10));
        reporter.update(s(4, 12));
        reporter.update(s(3, 10));
        clock.advance(500);
        expect(sent).toEqual([s(3, 10)]);
    });

    it('reset forces the next update to be sent', () => {
        reporter.update(s(3, 10));
        clock.advance(1000);
        reporter.reset();
        reporter.update(s(3, 10));
        expect(sent).toEqual([s(3, 10), s(3, 10)]);
    });

    it('cancel drops the pending update', () => {
        reporter.update(s(1, 1));
        reporter.update(s(2, 2));
        reporter.cancel();
        clock.advance(1000);
        expect(sent).toEqual([s(1, 1)]);
        expect(clock.pending()).toBe(0);
    });

    it('sends copies, not the caller objects', () => {
        const stats = s(1, 1);
        reporter.update(stats);
        stats.words = 99;
        expect(sent[0]).toEqual(s(1, 1));
    });
});

describe('exceedsLimit', () => {
    it.each([
        [s(10, 50), 0, 0, false],
        [s(10, 50), 10, 0, false],
        [s(11, 50), 10, 0, true],
        [s(10, 51), 0, 50, true],
        [s(10, 50), 10, 50, false],
        [s(10, 50), 100, 49, true],
    ])('%o with maxWords=%i maxCharacters=%i → %s', (stats, maxWords, maxCharacters, expected) => {
        expect(exceedsLimit(stats, maxWords, maxCharacters)).toBe(expected);
    });

    it('is false before the first count', () => {
        expect(exceedsLimit(null, 1, 1)).toBe(false);
    });
});
