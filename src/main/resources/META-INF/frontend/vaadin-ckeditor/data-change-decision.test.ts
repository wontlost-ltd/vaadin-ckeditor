import { describe, it, expect } from 'vitest';
import { decideDataChange, needsContent, type DataChangeState } from './data-change-decision';

function state(overrides: Partial<DataChangeState> = {}): DataChangeState {
    return {
        newContent: '<p>new</p>',
        lastKnownContent: '<p>old</p>',
        sync: true,
        apiChangeDepth: 0,
        changeSource: 'USER_INPUT',
        contentChangeEvents: true,
        serverKnowsLastContent: true,
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

    describe('issue #137: contentChange 只在有监听器时上报，且旧内容按需携带', () => {
        it('无监听器时不上报，但仍推进 lastKnownContent 并标记服务端镜像失配', () => {
            const d = decideDataChange(state({ contentChangeEvents: false }));
            expect(d.fireContentChange).toBe(false);
            expect(d.nextLastKnownContent).toBe('<p>new</p>');
            expect(d.nextServerKnowsLastContent).toBe(false);
            expect(d.resetChangeSource).toBe(true);
        });

        it('无监听器不影响用户输入同步', () => {
            const d = decideDataChange(state({ contentChangeEvents: false, sync: true }));
            expect(d.syncToServer).toBe(true);
        });

        it('服务端镜像一致时不携带旧内容', () => {
            const d = decideDataChange(state({ serverKnowsLastContent: true }));
            expect(d.fireContentChange).toBe(true);
            expect(d.sendOldContent).toBe(false);
            expect(d.nextServerKnowsLastContent).toBe(true);
        });

        it('服务端镜像失配时携带旧内容，上报后恢复一致', () => {
            const d = decideDataChange(state({ serverKnowsLastContent: false }));
            expect(d.sendOldContent).toBe(true);
            expect(d.nextServerKnowsLastContent).toBe(true);
        });

        it('内容未变时不改变镜像状态', () => {
            for (const known of [true, false]) {
                const d = decideDataChange(state({ newContent: '<p>x</p>', lastKnownContent: '<p>x</p>', serverKnowsLastContent: known }));
                expect(d.sendOldContent).toBe(false);
                expect(d.nextServerKnowsLastContent).toBe(known);
            }
        });
    });

    describe('needsContent', () => {
        it('有监听器时总是需要内容', () => {
            expect(needsContent({ contentChangeEvents: true, sync: false, apiChangeDepth: 1 })).toBe(true);
        });

        it('无监听器时，只有需同步的用户输入才需要内容', () => {
            expect(needsContent({ contentChangeEvents: false, sync: true, apiChangeDepth: 0 })).toBe(true);
            expect(needsContent({ contentChangeEvents: false, sync: true, apiChangeDepth: 1 })).toBe(false);
            expect(needsContent({ contentChangeEvents: false, sync: false, apiChangeDepth: 0 })).toBe(false);
        });
    });

    describe('issue #137 审查：可能被服务端丢弃的上报之后不信任镜像', () => {
        it('API 回填的上报之后，下次必须携带旧内容', () => {
            const d = decideDataChange(state({ apiChangeDepth: 1, serverKnowsLastContent: true }));
            expect(d.fireContentChange).toBe(true);
            expect(d.sendOldContent).toBe(false);
            expect(d.nextServerKnowsLastContent).toBe(false);
        });

        it('协作变更的上报之后，下次必须携带旧内容', () => {
            const d = decideDataChange(state({ changeSource: 'COLLABORATION' }));
            expect(d.nextServerKnowsLastContent).toBe(false);
        });

        it('本地用户输入的上报之后信任镜像', () => {
            for (const source of ['USER_INPUT', 'PASTE', 'UNDO_REDO']) {
                const d = decideDataChange(state({ changeSource: source, serverKnowsLastContent: false }));
                expect(d.sendOldContent).toBe(true);
                expect(d.nextServerKnowsLastContent).toBe(true);
            }
        });
    });
});
