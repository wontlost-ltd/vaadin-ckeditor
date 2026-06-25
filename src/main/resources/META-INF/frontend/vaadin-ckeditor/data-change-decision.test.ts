import { describe, it, expect } from 'vitest';
import { decideDataChange, type DataChangeState } from './data-change-decision';

function state(overrides: Partial<DataChangeState> = {}): DataChangeState {
    return {
        newContent: '<p>new</p>',
        lastKnownContent: '<p>old</p>',
        sync: true,
        apiChangeDepth: 0,
        changeSource: 'USER_INPUT',
        ...overrides,
    };
}

describe('decideDataChange', () => {
    describe('issue #38: server-originated changes must not round-trip to the server', () => {
        it('does NOT sync to server when apiChangeDepth > 0 (Binder.readBean echo)', () => {
            const d = decideDataChange(state({ apiChangeDepth: 1, sync: true }));
            expect(d.syncToServer).toBe(false);
        });

        it('still syncs to server for genuine user input (apiChangeDepth === 0)', () => {
            const d = decideDataChange(state({ apiChangeDepth: 0, sync: true }));
            expect(d.syncToServer).toBe(true);
        });

        it('labels content-change source as API when server-originated', () => {
            const d = decideDataChange(state({ apiChangeDepth: 1, changeSource: 'USER_INPUT' }));
            expect(d.contentChangeSource).toBe('API');
        });

        it('nested API changes (apiChangeDepth > 1) are still treated as API-originated', () => {
            const d = decideDataChange(state({ apiChangeDepth: 2 }));
            expect(d.syncToServer).toBe(false);
            expect(d.contentChangeSource).toBe('API');
        });
    });

    describe('sync flag', () => {
        it('does not sync to server when sync is false, even for user input', () => {
            const d = decideDataChange(state({ sync: false, apiChangeDepth: 0 }));
            expect(d.syncToServer).toBe(false);
        });

        it('syncs to server when sync is true and change is user-originated', () => {
            const d = decideDataChange(state({ sync: true, apiChangeDepth: 0 }));
            expect(d.syncToServer).toBe(true);
        });
    });

    describe('content-change firing', () => {
        it('fires content change when content actually changed', () => {
            const d = decideDataChange(state({ newContent: '<p>a</p>', lastKnownContent: '<p>b</p>' }));
            expect(d.fireContentChange).toBe(true);
            expect(d.nextLastKnownContent).toBe('<p>a</p>');
            expect(d.resetChangeSource).toBe(true);
        });

        it('does NOT fire content change when content is unchanged', () => {
            const d = decideDataChange(state({ newContent: '<p>same</p>', lastKnownContent: '<p>same</p>' }));
            expect(d.fireContentChange).toBe(false);
            expect(d.nextLastKnownContent).toBeNull();
            expect(d.resetChangeSource).toBe(false);
        });
    });

    describe('change source labeling for user-originated changes', () => {
        it('preserves UNDO_REDO source when not API-originated', () => {
            const d = decideDataChange(state({ apiChangeDepth: 0, changeSource: 'UNDO_REDO' }));
            expect(d.contentChangeSource).toBe('UNDO_REDO');
        });

        it('preserves PASTE source when not API-originated', () => {
            const d = decideDataChange(state({ apiChangeDepth: 0, changeSource: 'PASTE' }));
            expect(d.contentChangeSource).toBe('PASTE');
        });
    });
});
