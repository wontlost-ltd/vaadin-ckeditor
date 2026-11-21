/**
 * 评论 / 修订建议持久化桥（CKEditor 的 "adapter integration"，不依赖 CKEditor Cloud）。
 *
 * 这是 add-on 自带的**自定义插件** `VaadinCollaborationBridge`：Java 端 `CollaborationBridge` 的
 * `@JsModule` 加载本模块，本模块经 `registerCKEditorPlugin` 登记插件类；`VaadinCKEditor#setCollaborationBridge`
 * 把 `CustomPlugin("VaadinCollaborationBridge")` 加入插件列表，连接器从登记表取出本插件。
 *
 * 插件在 `afterInit()`（所有插件 init 之后、编辑器载入数据之前）：
 * 1. 经 `$server.collaborationRequest('load')` 取得 me、users、permissions、comment threads、suggestions；
 * 2. `Users.addUser()` / `Users.defineMe()`、`Permissions.setPermissions()`；
 * 3. 设置 `CommentsRepository#adapter` 并以 `isFromAdapter: true` 预载线程；
 * 4. 设置 `TrackChanges#adapter` 并 `addSuggestion()` 预载建议。
 * 之后 CKEditor 对 adapter 的每次调用都转为一次服务端请求，应答经宿主元素上的
 * `vaadin-ckeditor-collaboration` 事件返回；超时或服务端拒绝时 adapter 的 Promise reject。
 *
 * API 依据（CKEditor 49.x 的 .d.ts）：comments/commentsrepository `CommentsAdapter`、
 * track-changes/trackchanges `TrackChangesAdapter`、collaboration-core/users `Users`、
 * collaboration-core/permissions `Permissions`。作者、时间一律由服务端决定，这里发送前会剔除。
 */
import { Plugin } from 'ckeditor5';
import { registerCKEditorPlugin } from './plugin-resolver';

export const PLUGIN_NAME = 'VaadinCollaborationBridge';
/** 服务端应答事件（Java VaadinCKEditor#respondToCollaborationRequest）。 */
export const RESPONSE_EVENT = 'vaadin-ckeditor-collaboration';
export const DEFAULT_TIMEOUT_MS = 30_000;

/** 宿主元素上由 Vaadin 注入的服务端调用。 */
export interface CollaborationServer {
    collaborationRequest(requestId: string, operation: string, payloadJson: string): void;
}

export interface CollaborationHost extends EventTarget {
    $server?: CollaborationServer;
    /** Java 端 CollaborationBridge.Builder#requestTimeout。 */
    collaborationTimeoutMs?: number;
}

interface Pending {
    resolve: (value: unknown) => void;
    reject: (error: Error) => void;
    timer: ReturnType<typeof setTimeout>;
    operation: string;
}

let requestCounter = 0;

/**
 * 请求 / 应答通道：每个请求带唯一 id，服务端经宿主事件应答；超时、宿主断开时 reject。
 */
export class CollaborationRpc {
    private readonly pending = new Map<string, Pending>();
    private readonly onResponse = (event: Event) => {
        const detail = (event as CustomEvent).detail as { requestId?: string; result?: string | null; error?: string | null } | undefined;
        if (detail?.requestId) {
            this.resolve(detail.requestId, detail.result ?? null, detail.error ?? null);
        }
    };

    constructor(private readonly host: CollaborationHost, private readonly timeoutMs: number = DEFAULT_TIMEOUT_MS) {
        host.addEventListener(RESPONSE_EVENT, this.onResponse);
    }

    call<T = unknown>(operation: string, payload: unknown): Promise<T> {
        const server = this.host.$server;
        if (!server?.collaborationRequest) {
            return Promise.reject(new Error(`Collaboration request ${operation} failed: server connection not available`));
        }
        const requestId = `collab-${++requestCounter}`;
        return new Promise<T>((resolve, reject) => {
            const timer = setTimeout(() => {
                this.pending.delete(requestId);
                reject(new Error(`Collaboration request ${operation} timed out after ${this.timeoutMs} ms`));
            }, this.timeoutMs);
            this.pending.set(requestId, { resolve: resolve as (v: unknown) => void, reject, timer, operation });
            try {
                server.collaborationRequest(requestId, operation, JSON.stringify(payload ?? null));
            } catch (error) {
                clearTimeout(timer);
                this.pending.delete(requestId);
                reject(error instanceof Error ? error : new Error(String(error)));
            }
        });
    }

    resolve(requestId: string, resultJson: string | null, error: string | null): void {
        const entry = this.pending.get(requestId);
        if (!entry) {
            return; // 已超时或不是本通道的请求
        }
        this.pending.delete(requestId);
        clearTimeout(entry.timer);
        if (error) {
            entry.reject(new Error(error));
            return;
        }
        try {
            entry.resolve(resultJson == null ? null : JSON.parse(resultJson));
        } catch {
            entry.reject(new Error(`Collaboration request ${entry.operation} returned invalid JSON`));
        }
    }

    get pendingCount(): number {
        return this.pending.size;
    }

    /** 编辑器销毁：拒绝所有在途请求并停止监听。 */
    dispose(): void {
        this.host.removeEventListener(RESPONSE_EVENT, this.onResponse);
        for (const entry of this.pending.values()) {
            clearTimeout(entry.timer);
            entry.reject(new Error('Editor destroyed'));
        }
        this.pending.clear();
    }
}

// ---------------------------------------------------------------- 数据映射

type Json = Record<string, unknown>;

function toDate(value: unknown): Date | null {
    return typeof value === 'string' ? new Date(value) : null;
}

function iso(value: unknown): string | null {
    return value instanceof Date ? value.toISOString() : null;
}

/** 服务端线程 JSON → `CommentsRepository#addCommentThread` 的参数（isFromAdapter: true，不回调 adapter）。 */
export function toCommentThread(thread: Json): Json {
    return {
        threadId: thread.threadId,
        context: thread.context ?? null,
        resolvedAt: toDate(thread.resolvedAt),
        resolvedBy: thread.resolvedBy ?? null,
        unlinkedAt: toDate(thread.unlinkedAt),
        attributes: thread.attributes ?? {},
        comments: ((thread.comments as Json[]) ?? []).map(toComment),
        isFromAdapter: true,
    };
}

function toComment(comment: Json): Json {
    return {
        commentId: comment.commentId,
        authorId: comment.authorId,
        createdAt: toDate(comment.createdAt),
        content: comment.content,
        attributes: comment.attributes ?? {},
    };
}

/** 服务端建议 JSON → CKEditor `SuggestionData`。 */
export function toSuggestion(suggestion: Json): Json {
    return {
        id: suggestion.id,
        type: suggestion.type,
        authorId: suggestion.authorId,
        createdAt: toDate(suggestion.createdAt),
        hasComments: suggestion.hasComments === true,
        data: suggestion.data ?? null,
        attributes: suggestion.attributes ?? {},
        originalSuggestionId: suggestion.originalSuggestionId ?? null,
    };
}

/** 只保留列出的字段（剔除 channelId（可能是 symbol）、authorId、createdAt、isFromAdapter 等）。 */
function pick(data: Json | undefined, keys: string[]): Json {
    const out: Json = {};
    for (const key of keys) {
        if (data && data[key] !== undefined) {
            out[key] = data[key];
        }
    }
    return out;
}

/**
 * `CommentsAdapter`（ckeditor5-comments 49.x `comments/commentsrepository`）。
 * 方法签名与返回值形状与官方文档一致；authorId / createdAt / resolvedBy / resolvedAt 由服务端决定。
 */
export function createCommentsAdapter(rpc: CollaborationRpc) {
    return {
        async addCommentThread(data: Json) {
            const comments = ((data.comments as Json[]) ?? []).map((c) => pick(c, ['commentId', 'content', 'attributes']));
            const payload = { ...pick(data, ['threadId', 'context', 'attributes']), unlinkedAt: iso(data.unlinkedAt), comments };
            const result = await rpc.call<Json>('addCommentThread', payload);
            return {
                threadId: result.threadId,
                comments: ((result.comments as Json[]) ?? []).map((c) => ({ commentId: c.commentId, createdAt: toDate(c.createdAt) })),
            };
        },
        async getCommentThread(data: Json) {
            const thread = await rpc.call<Json | null>('getCommentThread', pick(data, ['threadId']));
            if (!thread) {
                return null;
            }
            const { isFromAdapter: _ignored, ...rest } = toCommentThread(thread);
            return rest;
        },
        async updateCommentThread(data: Json) {
            const payload: Json = pick(data, ['threadId', 'context', 'attributes']);
            if ('unlinkedAt' in data) {
                payload.unlinkedAt = iso(data.unlinkedAt);
            }
            await rpc.call('updateCommentThread', payload);
        },
        async resolveCommentThread(data: Json) {
            const result = await rpc.call<Json>('resolveCommentThread', pick(data, ['threadId']));
            return { threadId: result.threadId, resolvedAt: toDate(result.resolvedAt), resolvedBy: result.resolvedBy };
        },
        async reopenCommentThread(data: Json) {
            await rpc.call('reopenCommentThread', pick(data, ['threadId']));
        },
        async removeCommentThread(data: Json) {
            await rpc.call('removeCommentThread', pick(data, ['threadId']));
        },
        async addComment(data: Json) {
            const result = await rpc.call<Json>('addComment', pick(data, ['threadId', 'commentId', 'content', 'attributes']));
            return { commentId: result.commentId, createdAt: toDate(result.createdAt) };
        },
        async updateComment(data: Json) {
            await rpc.call('updateComment', pick(data, ['threadId', 'commentId', 'content', 'attributes']));
        },
        async removeComment(data: Json) {
            await rpc.call('removeComment', pick(data, ['threadId', 'commentId']));
        },
    };
}

/** `TrackChangesAdapter`（ckeditor5-track-changes 49.x `trackchanges`）。 */
export function createTrackChangesAdapter(rpc: CollaborationRpc) {
    return {
        async getSuggestion(id: string) {
            return toSuggestion(await rpc.call<Json>('getSuggestion', { id }));
        },
        async addSuggestion(data: Json) {
            const payload = pick(data, ['id', 'type', 'hasComments', 'data', 'attributes', 'originalSuggestionId']);
            return toSuggestion(await rpc.call<Json>('addSuggestion', payload));
        },
        async updateSuggestion(id: string, data: Json) {
            await rpc.call('updateSuggestion', { id, ...pick(data, ['hasComments', 'attributes', 'state']) });
        },
    };
}

// ---------------------------------------------------------------- 插件

interface UsersPlugin {
    getUser(id: string): unknown;
    addUser(data: Json): unknown;
    defineMe(id: string): void;
    me: { id: string } | null;
}

interface LoadResult {
    me: Json;
    users: Json[];
    permissions: string[];
    threads: Json[];
    suggestions: Json[];
}

/** 定位宿主 `<vaadin-ckeditor>`：连接器以 attachTo（classic）或 root.element 创建编辑器。 */
export function findHost(config: { get(key: string): unknown }): CollaborationHost | null {
    const attachTo = config.get('attachTo') as HTMLElement | undefined;
    const root = config.get('root') as { element?: HTMLElement } | undefined;
    const element = attachTo ?? root?.element;
    return (element?.closest?.('vaadin-ckeditor') as unknown as CollaborationHost) ?? null;
}

export class VaadinCollaborationBridge extends Plugin {
    static get pluginName() {
        return PLUGIN_NAME;
    }

    rpc: CollaborationRpc | null = null;
    /** 服务端下发的当前用户权限（comment-permission-enforcer 据此决定是否隐藏他人评论的编辑菜单）。 */
    permissions: string[] = [];

    /** 当前用户能否编辑 / 删除他人的评论（comment:modify_all）。 */
    get canModifyAllComments(): boolean {
        return this.permissions.includes('comment:modify_all');
    }

    async afterInit(): Promise<void> {
        const editor = this.editor;
        const host = findHost(editor.config);
        if (!host) {
            console.warn('[VaadinCKEditor] VaadinCollaborationBridge: host element not found, comments are not persisted');
            return;
        }
        this.rpc = new CollaborationRpc(host, host.collaborationTimeoutMs ?? DEFAULT_TIMEOUT_MS);
        const data = await this.rpc.call<LoadResult>('load', {});
        const plugins = editor.plugins;

        if (plugins.has('Users')) {
            const users = plugins.get('Users') as unknown as UsersPlugin;
            for (const user of [...(data.users ?? []), data.me]) {
                if (user && !users.getUser(user.id as string)) {
                    users.addUser({ id: user.id, name: user.name, ...(user.avatar ? { avatar: user.avatar } : {}) });
                }
            }
            if (!users.me) {
                users.defineMe(data.me.id as string);
            }
        }
        this.permissions = data.permissions ?? [];
        if (plugins.has('Permissions')) {
            (plugins.get('Permissions') as unknown as { setPermissions(p: string[]): void }).setPermissions(this.permissions);
        }

        // 顺序与官方集成指南一致：用户 → 评论线程 → 建议
        if (plugins.has('CommentsRepository')) {
            const repository = plugins.get('CommentsRepository') as unknown as {
                adapter: unknown;
                addCommentThread(data: Json): unknown;
            };
            repository.adapter = createCommentsAdapter(this.rpc);
            for (const thread of data.threads ?? []) {
                repository.addCommentThread(toCommentThread(thread));
            }
        }
        if (plugins.has('TrackChanges')) {
            const trackChanges = plugins.get('TrackChanges') as unknown as {
                adapter: unknown;
                addSuggestion(data: Json): unknown;
            };
            trackChanges.adapter = createTrackChangesAdapter(this.rpc);
            for (const suggestion of data.suggestions ?? []) {
                trackChanges.addSuggestion(toSuggestion(suggestion));
            }
        }
    }

    override destroy(): void {
        this.rpc?.dispose();
        this.rpc = null;
        super.destroy();
    }
}

registerCKEditorPlugin(PLUGIN_NAME, VaadinCollaborationBridge);

export default VaadinCollaborationBridge;
