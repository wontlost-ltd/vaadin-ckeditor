/**
 * 评论 / 修订建议持久化桥（VaadinCollaborationBridge）的 mock 测试。
 *
 * 没有 CKEditor 商业 license，真实的 Comments / TrackChanges 无法运行，因此这里用**真实的 CKEditor 内核**
 * （ClassicEditor，jsdom）加载按 49.x 文档形状仿写的 `Users` / `Permissions` / `CommentsRepository` /
 * `TrackChanges` 插件，验证：
 * - 插件经 add-on 的自定义插件机制（registerCKEditorPlugin + PluginResolver）加载；
 * - afterInit 的启动加载：用户、当前用户、权限、线程（isFromAdapter: true、Date 类型）、建议；
 * - adapter 各方法发往服务端的操作名与数据（剔除 authorId / createdAt / channelId），以及返回值映射；
 * - 服务端拒绝、超时、宿主断开时 adapter 的 Promise reject。
 * 真实 premium 插件的行为（何时调用 adapter、UI、marker 转换）只能用 license 验证，见 PREMIUM_INTEGRATION.md。
 */
import { describe, it, expect, beforeAll, afterEach, vi } from 'vitest';
import { ClassicEditor, Essentials, Paragraph, Plugin } from 'ckeditor5';
import {
    CollaborationRpc,
    RESPONSE_EVENT,
    VaadinCollaborationBridge,
    createCommentsAdapter,
    createTrackChangesAdapter,
    type CollaborationHost,
} from './collaboration-bridge';
import { PluginResolver, getGlobalPluginRegistry } from './plugin-resolver';

beforeAll(() => {
    const g = globalThis as { ResizeObserver?: unknown };
    g.ResizeObserver ??= class { observe(): void {} unobserve(): void {} disconnect(): void {} };
});

// ---------------------------------------------------------------- 按文档形状仿写的 premium 插件

type Json = Record<string, any>;

/** collaboration-core/users `Users`：users、me、addUser、getUser、defineMe（只能设置一次）。 */
class FakeUsers extends Plugin {
    static get pluginName() { return 'Users' as const; }
    users: Json[] = [];
    private _me: Json | null = null;
    get me() { return this._me; }
    addUser(data: Json) { const user = { ...data }; this.users.push(user); return user; }
    getUser(id: string) { return this.users.find((u) => u.id === id) ?? null; }
    defineMe(id: string) {
        if (this._me) throw new Error('me already defined');
        this._me = this.getUser(id);
        if (!this._me) throw new Error(`unknown user ${id}`);
    }
}

/** collaboration-core/permissions `Permissions#setPermissions(permissions, channelId?)`。 */
class FakePermissions extends Plugin {
    static get pluginName() { return 'Permissions' as const; }
    permissions: string[] | null = null;
    setPermissions(permissions: string[]) { this.permissions = permissions; }
}

/** comments/commentsrepository `CommentsRepository`：adapter 读写属性、addCommentThread(data)。 */
class FakeCommentsRepository extends Plugin {
    static get pluginName() { return 'CommentsRepository' as const; }
    static get requires() { return [FakeUsers]; }
    adapter: Json | null = null;
    threads: Json[] = [];
    initialized = false;
    init() { this.initialized = true; }
    addCommentThread(data: Json) {
        if (!this.initialized) throw new Error('used before init');
        this.threads.push(data);
        return data;
    }
}

/** track-changes/trackchanges `TrackChanges`：adapter 读写属性、addSuggestion(data)。 */
class FakeTrackChanges extends Plugin {
    static get pluginName() { return 'TrackChanges' as const; }
    static get requires() { return [FakeCommentsRepository, FakeUsers, FakePermissions]; }
    adapter: Json | null = null;
    suggestions: Json[] = [];
    addSuggestion(data: Json) { this.suggestions.push(data); return data; }
}

// ---------------------------------------------------------------- 假服务端（模拟 Vaadin $server 与应答事件）

interface Call { operation: string; payload: Json }

function createHost(handler: (operation: string, payload: Json) => unknown | Promise<unknown>) {
    const host = document.createElement('vaadin-ckeditor') as unknown as HTMLElement & CollaborationHost;
    const calls: Call[] = [];
    host.$server = {
        collaborationRequest(requestId, operation, payloadJson) {
            const payload = JSON.parse(payloadJson) as Json;
            calls.push({ operation, payload });
            // 与 Vaadin 一致：应答异步到达
            Promise.resolve().then(() => handler(operation, payload)).then(
                (result) => host.dispatchEvent(new CustomEvent(RESPONSE_EVENT, {
                    detail: { requestId, result: JSON.stringify(result ?? null), error: null },
                })),
                (error: Error) => host.dispatchEvent(new CustomEvent(RESPONSE_EVENT, {
                    detail: { requestId, result: null, error: error.message },
                })),
            );
        },
    };
    document.body.appendChild(host);
    return { host, calls };
}

const LOAD = {
    me: { id: 'bob', name: 'Bob' },
    users: [{ id: 'alice', name: 'Alice', avatar: 'https://example.test/a.png' }, { id: 'bob', name: 'Bob' }],
    permissions: ['document:write', 'comment:write'],
    threads: [{
        threadId: 't1', context: { type: 'text', value: 'hello' }, resolvedAt: null, resolvedBy: null,
        unlinkedAt: null, attributes: {},
        comments: [{ commentId: 'c1', authorId: 'alice', createdAt: '2026-11-01T10:00:00Z', content: '<p>Hi</p>', attributes: {} }],
    }],
    suggestions: [{
        id: 's1', type: 'insertion', authorId: 'alice', createdAt: '2026-11-01T11:00:00Z', hasComments: false,
        data: null, attributes: {}, originalSuggestionId: null,
    }],
};

let editors: ClassicEditor[] = [];
afterEach(async () => {
    await Promise.all(editors.map((e) => e.destroy()));
    editors = [];
    document.body.innerHTML = '';
});

async function createEditor(handler: (operation: string, payload: Json) => unknown, extraPlugins: unknown[] = [FakeTrackChanges]) {
    const { host, calls } = createHost(handler);
    const element = document.createElement('div');
    host.appendChild(element);
    // 与连接器相同：插件类经自定义插件机制解析
    const resolved = await new PluginResolver({ debug() {}, warn() {}, error() {} }).resolvePlugins([
        { name: 'VaadinCollaborationBridge', premium: false, importPath: './vaadin-ckeditor/collaboration-bridge' },
    ]);
    const editor = await ClassicEditor.create(element, {
        licenseKey: 'GPL',
        plugins: [Essentials, Paragraph, ...extraPlugins, ...resolved],
        initialData: '<p>text</p>',
    } as never);
    editors.push(editor);
    return { editor, host, calls };
}

describe('经自定义插件机制加载', () => {
    it('模块加载即登记到 add-on 的插件登记表，PluginResolver 取到同一个类', async () => {
        expect(getGlobalPluginRegistry()['VaadinCollaborationBridge']).toBe(VaadinCollaborationBridge);
        const resolver = new PluginResolver({ debug() {}, warn() {}, error() {} });
        const plugins = await resolver.resolvePlugins([
            { name: 'VaadinCollaborationBridge', premium: false, importPath: './vaadin-ckeditor/collaboration-bridge' },
        ]);
        expect(plugins).toEqual([VaadinCollaborationBridge]);
        expect(resolver.loadErrors).toEqual([]);
    });
});

describe('启动加载（afterInit）', () => {
    it('注册用户与当前用户、设置权限、预载线程与建议（日期为 Date，线程标记 isFromAdapter）', async () => {
        const { editor, calls } = await createEditor(() => LOAD);
        expect(calls.map((c) => c.operation)).toEqual(['load']);

        const users = editor.plugins.get('Users') as unknown as FakeUsers;
        expect(users.users.map((u) => u.id)).toEqual(['alice', 'bob']);
        expect(users.getUser('alice')).toEqual({ id: 'alice', name: 'Alice', avatar: 'https://example.test/a.png' });
        expect(users.me?.id).toBe('bob');

        expect((editor.plugins.get('Permissions') as unknown as FakePermissions).permissions)
            .toEqual(['document:write', 'comment:write']);

        const repository = editor.plugins.get('CommentsRepository') as unknown as FakeCommentsRepository;
        expect(repository.adapter).not.toBeNull();
        expect(repository.threads).toHaveLength(1);
        const thread = repository.threads[0];
        expect(thread).toMatchObject({ threadId: 't1', context: { type: 'text', value: 'hello' }, isFromAdapter: true, resolvedAt: null });
        expect(thread.comments[0].createdAt).toBeInstanceOf(Date);
        expect(thread.comments[0].createdAt.toISOString()).toBe('2026-11-01T10:00:00.000Z');
        expect(thread.comments[0].authorId).toBe('alice');

        const trackChanges = editor.plugins.get('TrackChanges') as unknown as FakeTrackChanges;
        expect(trackChanges.adapter).not.toBeNull();
        expect(trackChanges.suggestions[0]).toMatchObject({ id: 's1', type: 'insertion', authorId: 'alice', hasComments: false });
        expect(trackChanges.suggestions[0].createdAt).toBeInstanceOf(Date);

        expect((editor.plugins.get('VaadinCollaborationBridge') as unknown as VaadinCollaborationBridge).canModifyAllComments).toBe(false);
    });

    it('只加载了 Comments（无 TrackChanges）时只装评论 adapter', async () => {
        const { editor } = await createEditor(() => LOAD, [FakeCommentsRepository]);
        expect((editor.plugins.get('CommentsRepository') as unknown as FakeCommentsRepository).threads).toHaveLength(1);
        expect(editor.plugins.has('TrackChanges')).toBe(false);
    });

    it('服务端拒绝启动加载时编辑器创建失败（不在无持久化的情况下静默运行）', async () => {
        await expect(createEditor(() => { throw new Error('Permission denied'); })).rejects.toThrow('Permission denied');
    });
});

describe('CommentsAdapter 映射', () => {
    async function adapter(handler: (operation: string, payload: Json) => unknown) {
        const { editor, calls } = await createEditor((operation, payload) => operation === 'load' ? LOAD : handler(operation, payload));
        calls.length = 0;
        const repository = editor.plugins.get('CommentsRepository') as unknown as FakeCommentsRepository;
        return { adapter: repository.adapter as Json, calls };
    }

    it('addCommentThread：只发送线程与评论内容，不发送 authorId / createdAt / channelId；返回 Date', async () => {
        const { adapter: a, calls } = await adapter(() => ({ threadId: 't2', comments: [{ commentId: 'c9', createdAt: '2026-11-22T00:00:00Z' }] }));
        const result = await a.addCommentThread({
            channelId: Symbol('channel'), threadId: 't2', context: { type: 'text', value: 'x' }, attributes: { a: 1 },
            resolvedAt: new Date(), resolvedBy: 'mallory',
            comments: [{ commentId: 'c9', content: '<p>New</p>', attributes: {}, authorId: 'mallory', createdAt: new Date(0) }],
        });
        expect(calls).toEqual([{ operation: 'addCommentThread', payload: {
            threadId: 't2', context: { type: 'text', value: 'x' }, attributes: { a: 1 }, unlinkedAt: null,
            comments: [{ commentId: 'c9', content: '<p>New</p>', attributes: {} }],
        } }]);
        expect(result.threadId).toBe('t2');
        expect(result.comments[0].createdAt).toEqual(new Date('2026-11-22T00:00:00Z'));
    });

    it('getCommentThread：返回 CKEditor 形状的线程，不存在时为 null', async () => {
        const { adapter: a, calls } = await adapter((_op, payload) => payload.threadId === 't1' ? LOAD.threads[0] : null);
        const thread = await a.getCommentThread({ channelId: 'ch', threadId: 't1' });
        expect(thread.comments[0].createdAt).toBeInstanceOf(Date);
        expect(thread).not.toHaveProperty('isFromAdapter');
        expect(await a.getCommentThread({ channelId: 'ch', threadId: 'nope' })).toBeNull();
        expect(calls[0]).toEqual({ operation: 'getCommentThread', payload: { threadId: 't1' } });
    });

    it('updateCommentThread / resolve / reopen / remove 与评论增改删的操作名与数据', async () => {
        const { adapter: a, calls } = await adapter((operation, payload) => {
            if (operation === 'resolveCommentThread') return { threadId: payload.threadId, resolvedAt: '2026-11-22T01:00:00Z', resolvedBy: 'bob' };
            if (operation === 'addComment') return { commentId: payload.commentId, createdAt: '2026-11-22T02:00:00Z' };
            return null;
        });
        await a.updateCommentThread({ channelId: 'ch', threadId: 't1', attributes: { b: 2 }, unlinkedAt: new Date('2026-11-22T03:00:00Z') });
        const resolved = await a.resolveCommentThread({ channelId: 'ch', threadId: 't1' });
        await a.reopenCommentThread({ channelId: 'ch', threadId: 't1' });
        const added = await a.addComment({ channelId: 'ch', threadId: 't1', commentId: 'c2', content: 'x', attributes: {}, authorId: 'mallory' });
        await a.updateComment({ channelId: 'ch', threadId: 't1', commentId: 'c2', content: 'y' });
        await a.removeComment({ channelId: 'ch', threadId: 't1', commentId: 'c2' });
        await a.removeCommentThread({ channelId: 'ch', threadId: 't1' });

        expect(calls).toEqual([
            { operation: 'updateCommentThread', payload: { threadId: 't1', attributes: { b: 2 }, unlinkedAt: '2026-11-22T03:00:00.000Z' } },
            { operation: 'resolveCommentThread', payload: { threadId: 't1' } },
            { operation: 'reopenCommentThread', payload: { threadId: 't1' } },
            { operation: 'addComment', payload: { threadId: 't1', commentId: 'c2', content: 'x', attributes: {} } },
            { operation: 'updateComment', payload: { threadId: 't1', commentId: 'c2', content: 'y' } },
            { operation: 'removeComment', payload: { threadId: 't1', commentId: 'c2' } },
            { operation: 'removeCommentThread', payload: { threadId: 't1' } },
        ]);
        expect(resolved).toEqual({ threadId: 't1', resolvedAt: new Date('2026-11-22T01:00:00Z'), resolvedBy: 'bob' });
        expect(added).toEqual({ commentId: 'c2', createdAt: new Date('2026-11-22T02:00:00Z') });
    });

    it('服务端拒绝时 adapter 的 Promise 以服务端消息 reject', async () => {
        const { adapter: a } = await adapter(() => { throw new Error('Permission denied: only the author can edit this comment'); });
        await expect(a.updateComment({ threadId: 't1', commentId: 'c1', content: 'hack' }))
            .rejects.toThrow('only the author can edit this comment');
    });
});

describe('TrackChangesAdapter 映射', () => {
    it('getSuggestion / addSuggestion / updateSuggestion', async () => {
        const { editor, calls } = await createEditor((operation, payload) => {
            if (operation === 'load') return LOAD;
            if (operation === 'getSuggestion') return LOAD.suggestions[0];
            if (operation === 'addSuggestion') return { ...payload, authorId: 'bob', createdAt: '2026-11-22T04:00:00Z' };
            return null;
        });
        calls.length = 0;
        const a = (editor.plugins.get('TrackChanges') as unknown as FakeTrackChanges).adapter as Json;

        const got = await a.getSuggestion('s1');
        expect(got.createdAt).toBeInstanceOf(Date);
        const added = await a.addSuggestion({
            id: 's2', type: 'deletion', data: null, attributes: { groupId: 'g' }, originalSuggestionId: 's1',
            authorId: 'mallory', createdAt: new Date(0),
        });
        await a.updateSuggestion('s2', { state: 'accepted', hasComments: true });

        expect(calls).toEqual([
            { operation: 'getSuggestion', payload: { id: 's1' } },
            { operation: 'addSuggestion', payload: { id: 's2', type: 'deletion', data: null, attributes: { groupId: 'g' }, originalSuggestionId: 's1' } },
            { operation: 'updateSuggestion', payload: { id: 's2', state: 'accepted', hasComments: true } },
        ]);
        expect(added).toMatchObject({ id: 's2', authorId: 'bob', originalSuggestionId: 's1' });
        expect(added.createdAt).toEqual(new Date('2026-11-22T04:00:00Z'));
    });
});

describe('CollaborationRpc', () => {
    it('超时 reject 并丢弃迟到的应答', async () => {
        vi.useFakeTimers();
        try {
            const host = document.createElement('div') as unknown as HTMLElement & CollaborationHost;
            let lastId = '';
            host.$server = { collaborationRequest: (id) => { lastId = id; } };
            const rpc = new CollaborationRpc(host, 1000);
            const promise = rpc.call('addComment', {});
            const assertion = expect(promise).rejects.toThrow('timed out after 1000 ms');
            vi.advanceTimersByTime(1001);
            await assertion;
            expect(rpc.pendingCount).toBe(0);
            rpc.resolve(lastId, '{}', null); // 迟到的应答被忽略
        } finally {
            vi.useRealTimers();
        }
    });

    it('无服务端连接时立即 reject；dispose 拒绝在途请求', async () => {
        const offline = document.createElement('div') as unknown as HTMLElement & CollaborationHost;
        await expect(new CollaborationRpc(offline).call('load', {})).rejects.toThrow('server connection not available');

        const host = document.createElement('div') as unknown as HTMLElement & CollaborationHost;
        host.$server = { collaborationRequest: () => undefined };
        const rpc = new CollaborationRpc(host);
        const pending = rpc.call('load', {});
        rpc.dispose();
        await expect(pending).rejects.toThrow('Editor destroyed');
    });

    it('非法 JSON 应答 reject；其他通道的应答不影响本通道', async () => {
        const host = document.createElement('div') as unknown as HTMLElement & CollaborationHost;
        let id = '';
        host.$server = { collaborationRequest: (requestId) => { id = requestId; } };
        const rpc = new CollaborationRpc(host);
        const promise = rpc.call('load', {});
        host.dispatchEvent(new CustomEvent(RESPONSE_EVENT, { detail: { requestId: 'other', result: '{}', error: null } }));
        expect(rpc.pendingCount).toBe(1);
        host.dispatchEvent(new CustomEvent(RESPONSE_EVENT, { detail: { requestId: id, result: '{oops', error: null } }));
        await expect(promise).rejects.toThrow('invalid JSON');
    });

    it('adapter 工厂可独立使用（不依赖编辑器）', async () => {
        const host = document.createElement('div') as unknown as HTMLElement & CollaborationHost;
        host.$server = {
            collaborationRequest: (requestId) => queueMicrotask(() => host.dispatchEvent(new CustomEvent(RESPONSE_EVENT, {
                detail: { requestId, result: JSON.stringify({ commentId: 'c', createdAt: '2026-01-01T00:00:00Z' }), error: null },
            }))),
        };
        const rpc = new CollaborationRpc(host);
        expect(await createCommentsAdapter(rpc).addComment({ threadId: 't', commentId: 'c', content: '', attributes: {} }))
            .toEqual({ commentId: 'c', createdAt: new Date('2026-01-01T00:00:00Z') });
        expect(Object.keys(createTrackChangesAdapter(rpc)).sort()).toEqual(['addSuggestion', 'getSuggestion', 'updateSuggestion']);
    });
});
