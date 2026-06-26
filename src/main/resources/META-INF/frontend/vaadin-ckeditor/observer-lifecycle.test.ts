import { describe, it, expect, vi } from 'vitest';
import { replaceObserver, disposeObserver, type Disconnectable } from './observer-lifecycle';

function makeObserver(): Disconnectable & { disconnect: ReturnType<typeof vi.fn<() => void>> } {
    return { disconnect: vi.fn<() => void>() };
}

describe('replaceObserver (review: MutationObserver leak / stacking)', () => {
    it('creates and starts a new observer when none exists', () => {
        const obs = makeObserver();
        const start = vi.fn<(o: Disconnectable) => void>();
        const result = replaceObserver(undefined, () => obs, start);
        expect(result).toBe(obs);
        expect(start).toHaveBeenCalledWith(obs);
    });

    it('disconnects the existing observer before creating a new one (no stacking)', () => {
        const old = makeObserver();
        const fresh = makeObserver();
        const result = replaceObserver(old, () => fresh, () => {});
        expect(old.disconnect).toHaveBeenCalledTimes(1);
        expect(result).toBe(fresh);
    });

    it('does not call create/start before disconnecting the old one', () => {
        const old = makeObserver();
        const order: string[] = [];
        old.disconnect.mockImplementation(() => order.push('disconnect-old'));
        replaceObserver(
            old,
            () => { order.push('create-new'); return makeObserver(); },
            () => { order.push('start-new'); },
        );
        expect(order).toEqual(['disconnect-old', 'create-new', 'start-new']);
    });

    it('repeated setup never leaves more than one live observer', () => {
        let live = 0;
        const make = () => {
            live++;
            return { disconnect: vi.fn(() => { live--; }) } as Disconnectable;
        };
        let held: Disconnectable | undefined;
        for (let i = 0; i < 5; i++) {
            held = replaceObserver(held, make, () => {});
            expect(live).toBe(1); // exactly one observer alive after each setup
        }
        disposeObserver(held);
        expect(live).toBe(0);
    });
});

describe('disposeObserver', () => {
    it('disconnects and returns undefined', () => {
        const obs = makeObserver();
        const result = disposeObserver(obs);
        expect(obs.disconnect).toHaveBeenCalledTimes(1);
        expect(result).toBeUndefined();
    });

    it('is a no-op when given undefined', () => {
        expect(disposeObserver(undefined)).toBeUndefined();
    });

    it('is safe to call twice (idempotent teardown)', () => {
        let held: Disconnectable | undefined = makeObserver();
        held = disposeObserver(held);
        expect(disposeObserver(held)).toBeUndefined();
    });
});
