/**
 * 测试用的 `ckeditor5-premium-features` 替身（只在 CKEDITOR_PREMIUM_STUB=1 构建 sample 时经 vite.config.ts 的别名注入）。
 *
 * 没有 CKEditor 商业 license，真实的 Comments / TrackChanges 会锁定编辑器。这里按 49.x 公开 API 的形状
 * 仿写 `Users`、`Permissions`、`CommentsRepository`、`Comments`、`TrackChanges`，让 e2e 能跑通
 * 「真实连接器 + 真实 VaadinCollaborationBridge 插件 ↔ Java CollaborationBridge」的往返。
 * 它不实现 UI、marker 转换或 CKEditor 何时调用 adapter——这些只能用 license 验证。
 * 测试经 `window.__ckPremiumStub` 读取插件状态，并像 CKEditor 一样调用已安装的 adapter。
 */
import { Plugin, type Editor } from 'ckeditor5';

type Json = Record<string, any>;

interface StubRegistry {
    editors: Editor[];
}

const registry: StubRegistry = ((window as unknown as { __ckPremiumStub?: StubRegistry }).__ckPremiumStub ??= { editors: [] });

/** collaboration-core/users：users、me、addUser、getUser、defineMe（只能设置一次）。 */
export class Users extends Plugin {
    static get pluginName() { return 'Users' as const; }
    users: Json[] = [];
    private _me: Json | null = null;
    get me() { return this._me; }
    addUser(data: Json) { const user = { ...data }; this.users.push(user); return user; }
    getUser(id: string) { return this.users.find((u) => u.id === id) ?? null; }
    defineMe(id: string) {
        if (this._me) throw new Error('Users#defineMe: the local user can be set only once');
        this._me = this.getUser(id);
    }
}

/** collaboration-core/permissions：setPermissions(permissions, channelId?)。 */
export class Permissions extends Plugin {
    static get pluginName() { return 'Permissions' as const; }
    permissions: string[] = ['document:write', 'comment:write', 'comment:admin'];
    setPermissions(permissions: string[]) { this.permissions = [...permissions]; }
}

/** comments/commentsrepository：adapter 属性、addCommentThread、getCommentThreads。 */
export class CommentsRepository extends Plugin {
    static get pluginName() { return 'CommentsRepository' as const; }
    static get requires() { return [Users]; }
    adapter: Json | null = null;
    threads: Json[] = [];
    init() { registry.editors.push(this.editor); }
    addCommentThread(data: Json) { this.threads.push(data); return data; }
    getCommentThreads() { return this.threads; }
}

export class Comments extends Plugin {
    static get pluginName() { return 'Comments' as const; }
    static get requires() { return [CommentsRepository, Users, Permissions]; }
}

/** track-changes/trackchanges：adapter 属性、addSuggestion、getSuggestions。 */
export class TrackChanges extends Plugin {
    static get pluginName() { return 'TrackChanges' as const; }
    static get requires() { return [Comments]; }
    adapter: Json | null = null;
    suggestions: Json[] = [];
    addSuggestion(data: Json) { this.suggestions.push(data); return data; }
    getSuggestions() { return this.suggestions; }
}
