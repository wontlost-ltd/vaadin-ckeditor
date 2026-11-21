import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 评论 / 修订建议持久化桥的 JS↔Java 往返（`/collab-bridge` 夹具）。
 *
 * 真实的连接器与真实的 VaadinCollaborationBridge 插件，对接 premium 包的**替身**（sample 以
 * CKEDITOR_PREMIUM_STUB=1 构建时注入，按 49.x 公开 API 形状仿写 Users / Permissions / CommentsRepository /
 * Comments / TrackChanges）。测试像 CKEditor 一样调用已安装的 adapter，断言请求经 $server 到达 Java
 * CollaborationBridge、写入内存 store、作者与时间由服务端决定、越权被拒绝，以及重新加载后数据回到编辑器。
 * 真实 premium 插件何时调用 adapter、UI 与 marker 转换不在此验证（需要 CKEditor license）。
 */
test.skip(process.env.CKEDITOR_PREMIUM_STUB !== '1',
    'needs the sample built with CKEDITOR_PREMIUM_STUB=1 (premium plugin stand-ins); see e2e/README.md');

type StubState = {
    me: string | null;
    users: string[];
    permissions: string[];
    threads: Array<{ threadId: string; isFromAdapter: boolean; comments: Array<{ commentId: string; authorId: string; createdAtIsDate: boolean }> }>;
    suggestions: Array<{ id: string; authorId: string; createdAtIsDate: boolean }>;
    hasCommentsAdapter: boolean;
    hasTrackChangesAdapter: boolean;
};

async function open(page: Page, query: string): Promise<void> {
    await page.goto(`/collab-bridge?${query}`);
    await waitForCKEditorReady(page);
    await page.waitForFunction(() => {
        const stub = (window as unknown as { __ckPremiumStub?: { editors: Array<{ state: string }> } }).__ckPremiumStub;
        return stub?.editors.some((e) => e.state === 'ready');
    });
}

/** 读取当前编辑器（最后一个 ready 的）上替身插件的状态。 */
async function stubState(page: Page): Promise<StubState> {
    return page.evaluate(() => {
        type Ed = { state: string; plugins: { get(n: string): any } };
        const editors = (window as unknown as { __ckPremiumStub: { editors: Ed[] } }).__ckPremiumStub.editors;
        const editor = editors.filter((e) => e.state === 'ready').pop()!;
        const users = editor.plugins.get('Users');
        const repository = editor.plugins.get('CommentsRepository');
        const trackChanges = editor.plugins.get('TrackChanges');
        return {
            me: users.me?.id ?? null,
            users: users.users.map((u: { id: string }) => u.id),
            permissions: editor.plugins.get('Permissions').permissions,
            threads: repository.threads.map((t: any) => ({
                threadId: t.threadId,
                isFromAdapter: t.isFromAdapter,
                comments: t.comments.map((c: any) => ({ commentId: c.commentId, authorId: c.authorId, createdAtIsDate: c.createdAt instanceof Date })),
            })),
            suggestions: trackChanges.suggestions.map((s: any) => ({ id: s.id, authorId: s.authorId, createdAtIsDate: s.createdAt instanceof Date })),
            hasCommentsAdapter: !!repository.adapter,
            hasTrackChangesAdapter: !!trackChanges.adapter,
        };
    });
}

/** 像 CKEditor 一样调用 adapter 方法；返回 { ok, value } 或 { ok: false, error }。 */
async function callAdapter(page: Page, plugin: 'CommentsRepository' | 'TrackChanges', method: string, ...args: unknown[]) {
    return page.evaluate(async ({ plugin, method, args }) => {
        type Ed = { state: string; plugins: { get(n: string): any } };
        const editors = (window as unknown as { __ckPremiumStub: { editors: Ed[] } }).__ckPremiumStub.editors;
        const editor = editors.filter((e) => e.state === 'ready').pop()!;
        try {
            const value = await editor.plugins.get(plugin).adapter[method](...args);
            return { ok: true, value: JSON.parse(JSON.stringify(value ?? null)), createdAtIsDate: value?.createdAt instanceof Date };
        } catch (error) {
            return { ok: false, error: (error as Error).message };
        }
    }, { plugin, method, args });
}

/** 让服务端把 store 投影到 #store-state 后读取（点击是一次服务端往返，用 expect.poll 等待结果）。 */
async function storeState(page: Page): Promise<string> {
    await page.locator('#btn-refresh').click();
    return page.locator('#store-state').innerText();
}

test.describe('评论 / 修订建议持久化桥', () => {
    test('启动加载：用户、当前用户、权限、线程与建议从 Java store 进入编辑器', async ({ page }) => {
        await open(page, 'reset=1&user=bob');
        const state = await stubState(page);
        expect(state.me).toBe('bob');
        expect(state.users).toEqual(['alice', 'bob']);
        expect(state.permissions).toEqual(['document:write', 'comment:write', 'comment:admin']);
        expect(state.hasCommentsAdapter).toBe(true);
        expect(state.hasTrackChangesAdapter).toBe(true);
        expect(state.threads).toEqual([{
            threadId: 't1', isFromAdapter: true,
            comments: [{ commentId: 'c1', authorId: 'alice', createdAtIsDate: true }],
        }]);
        expect(state.suggestions).toEqual([{ id: 's1', authorId: 'alice', createdAtIsDate: true }]);
    });

    test('添加评论：作者是服务端的当前用户（忽略伪造的 authorId），重新加载后回到编辑器', async ({ page }) => {
        await open(page, 'reset=1&user=bob');
        const added = await callAdapter(page, 'CommentsRepository', 'addComment',
            { channelId: 'x', threadId: 't1', commentId: 'c-bob', content: '<p>Bob replies</p>', attributes: {}, authorId: 'alice' });
        expect(added.ok).toBe(true);
        expect(added.createdAtIsDate).toBe(true);
        await expect.poll(() => storeState(page)).toContain('comment t1/c-bob by bob "<p>Bob replies</p>"');

        await open(page, 'user=alice');
        const state = await stubState(page);
        expect(state.me).toBe('alice');
        expect(state.threads[0].comments.map((c) => `${c.commentId}:${c.authorId}`)).toEqual(['c1:alice', 'c-bob:bob']);
    });

    test('越权：不能编辑他人的评论；评论者不能提出修订建议', async ({ page }) => {
        await open(page, 'reset=1&user=bob');
        const edit = await callAdapter(page, 'CommentsRepository', 'updateComment',
            { threadId: 't1', commentId: 'c1', content: '<p>hacked</p>' });
        expect(edit).toMatchObject({ ok: false });
        expect(edit.error).toContain('Permission denied');
        await expect.poll(() => storeState(page)).toContain(`comment t1/c1 by alice "<p>Alice's comment</p>"`);

        await open(page, 'user=bob&perm=comment:write');
        expect((await stubState(page)).permissions).toEqual(['comment:write']);
        const suggestion = await callAdapter(page, 'TrackChanges', 'addSuggestion', { id: 's-bob', type: 'insertion', data: null, attributes: {} });
        expect(suggestion).toMatchObject({ ok: false });
        expect(suggestion.error).toContain('requires document:write');
        await expect.poll(() => storeState(page)).toContain('suggestion s1 insertion by alice');
        expect(await storeState(page)).not.toContain('s-bob');
    });

    test('修订建议：新增建议的作者由服务端决定；拆分他人的建议沿用原作者；接受建议更新状态', async ({ page }) => {
        await open(page, 'reset=1&user=bob');
        const own = await callAdapter(page, 'TrackChanges', 'addSuggestion',
            { id: 's-own', type: 'insertion', data: null, attributes: {}, authorId: 'alice' });
        expect(own).toMatchObject({ ok: true, value: { id: 's-own', authorId: 'bob' }, createdAtIsDate: true });

        const split = await callAdapter(page, 'TrackChanges', 'addSuggestion',
            { id: 's-split', type: 'insertion', data: null, attributes: {}, originalSuggestionId: 's1' });
        expect(split).toMatchObject({ ok: true, value: { id: 's-split', authorId: 'alice', originalSuggestionId: 's1' } });

        expect((await callAdapter(page, 'TrackChanges', 'updateSuggestion', 's-own', { state: 'accepted' })).ok).toBe(true);
        await expect.poll(() => storeState(page)).toContain('suggestion s-own insertion by bob accepted');
        expect(await storeState(page)).toContain('suggestion s-split insertion by alice open');
    });

    test('线程：新建（作者为当前用户）、解决（resolvedBy 由服务端决定）、获取', async ({ page }) => {
        await open(page, 'reset=1&user=bob');
        const thread = await callAdapter(page, 'CommentsRepository', 'addCommentThread', {
            threadId: 't-bob', context: { type: 'text', value: 'Document' }, attributes: {},
            comments: [{ commentId: 'c-t', content: '<p>New thread</p>', attributes: {}, authorId: 'alice' }],
        });
        expect(thread).toMatchObject({ ok: true, value: { threadId: 't-bob' } });

        const resolved = await callAdapter(page, 'CommentsRepository', 'resolveCommentThread', { threadId: 't-bob' });
        expect(resolved).toMatchObject({ ok: true, value: { threadId: 't-bob', resolvedBy: 'bob' } });

        const fetched = await callAdapter(page, 'CommentsRepository', 'getCommentThread', { threadId: 't-bob' });
        expect(fetched).toMatchObject({ ok: true, value: { threadId: 't-bob', resolvedBy: 'bob', comments: [{ authorId: 'bob' }] } });
        await expect.poll(() => storeState(page)).toContain('comment t-bob/c-t by bob "<p>New thread</p>" resolved by bob');
    });
});
