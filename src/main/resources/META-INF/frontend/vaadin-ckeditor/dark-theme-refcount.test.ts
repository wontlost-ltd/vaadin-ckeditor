import { describe, it, expect } from 'vitest';
import { createRefcount } from './dark-theme-refcount';

describe('dark-theme-refcount (review: multi-instance ref counting)', () => {
    it('starts at 0 with no apply/remove signal', () => {
        const rc = createRefcount();
        expect(rc.count).toBe(0);
        expect(rc.justApplied).toBe(false);
        expect(rc.justRemoved).toBe(false);
    });

    it('first acquire (0→1) signals justApplied', () => {
        const rc = createRefcount().acquire();
        expect(rc.count).toBe(1);
        expect(rc.justApplied).toBe(true);
        expect(rc.justRemoved).toBe(false);
    });

    it('subsequent acquires do NOT re-apply (only the 0→1 transition does)', () => {
        const rc = createRefcount().acquire().acquire().acquire();
        expect(rc.count).toBe(3);
        expect(rc.justApplied).toBe(false);
    });

    it('last release (1→0) signals justRemoved', () => {
        const rc = createRefcount().acquire().release();
        expect(rc.count).toBe(0);
        expect(rc.justRemoved).toBe(true);
    });

    it('non-final release does NOT remove (global style stays for remaining instances)', () => {
        let rc = createRefcount().acquire().acquire(); // count=2
        rc = rc.release(); // 2→1
        expect(rc.count).toBe(1);
        expect(rc.justRemoved).toBe(false);
        rc = rc.release(); // 1→0
        expect(rc.justRemoved).toBe(true);
    });

    it('balanced acquire/release across 3 instances applies once, removes once', () => {
        let rc = createRefcount();
        let applies = 0, removes = 0;
        // 3 instances acquire
        for (let i = 0; i < 3; i++) { rc = rc.acquire(); if (rc.justApplied) applies++; }
        // 3 instances release
        for (let i = 0; i < 3; i++) { rc = rc.release(); if (rc.justRemoved) removes++; }
        expect(applies).toBe(1);
        expect(removes).toBe(1);
        expect(rc.count).toBe(0);
    });

    it('release never goes below 0 and an over-release does not falsely signal removal', () => {
        const rc = createRefcount().release();
        expect(rc.count).toBe(0);
        expect(rc.justRemoved).toBe(false); // releasing at 0 must not trigger style removal
    });

    it('is immutable — acquire/release return new values without mutating the original', () => {
        const base = createRefcount().acquire(); // count=1
        const a = base.acquire();
        const b = base.release();
        expect(base.count).toBe(1);
        expect(a.count).toBe(2);
        expect(b.count).toBe(0);
    });
});
