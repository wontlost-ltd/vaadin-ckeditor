/**
 * CommentPermissionEnforcer 插件单元测试
 *
 * 覆盖 MutationObserver 注册/清理、权限执行逻辑（基于 data-author-id）、re-entrant 保护
 *
 * Run with: npx vitest run comment-permission-enforcer.test.ts
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock ckeditor5 Plugin
vi.mock('ckeditor5', () => {
    class MockPlugin {
        editor: any;
        constructor(editor: any) {
            this.editor = editor;
        }
        destroy() {}
    }
    return { Plugin: MockPlugin };
});

import CommentPermissionEnforcer from './comment-permission-enforcer';

// --- 辅助工具 ---

/** 创建评论 DOM 元素，使用 data-author-id 标识作者 */
const createCommentElement = (authorName: string, authorId: string) => {
    const comment = document.createElement('div');
    comment.className = 'ck-comment ck-annotation';
    comment.setAttribute('data-author-id', authorId);

    const name = document.createElement('span');
    name.className = 'ck-annotation__info-name';
    name.textContent = authorName;

    const actions = document.createElement('div');
    actions.className = 'ck-annotation__actions';

    const dropdown = document.createElement('div');
    dropdown.className = 'ck-dropdown';

    actions.appendChild(dropdown);
    comment.appendChild(name);
    comment.appendChild(actions);

    return { comment, dropdown };
};

/** 创建侧栏 DOM 结构 */
const setupCommentDom = () => {
    const host = document.createElement('vaadin-ckeditor');
    const domRoot = document.createElement('div');
    const sidebar = document.createElement('div');
    sidebar.className = 'annotation-sidebar-container';
    host.appendChild(domRoot);
    host.appendChild(sidebar);
    document.body.appendChild(host);
    return { host, domRoot, sidebar };
};

/** 创建 mock editor */
const createMockEditor = (
    domRoot: HTMLElement,
    currentUserId: string
) => {
    const readyCallbacks: Array<() => void> = [];

    const editor = {
        on: (event: string, callback: () => void) => {
            if (event === 'ready') readyCallbacks.push(callback);
        },
        plugins: {
            get: (name: string) => {
                if (name === 'Users') return { me: { id: currentUserId } };
                throw new Error(`Unknown plugin: ${name}`);
            },
        },
        editing: {
            view: {
                getDomRoot: () => domRoot,
            },
        },
    };

    return {
        editor,
        triggerReady: () => readyCallbacks.forEach(cb => cb()),
    };
};

// --- MutationObserver mock ---
let observerCallback: MutationCallback | null = null;
let observeSpy: ReturnType<typeof vi.fn>;
let disconnectSpy: ReturnType<typeof vi.fn>;

beforeEach(() => {
    observeSpy = vi.fn();
    disconnectSpy = vi.fn();
    observerCallback = null;

    class MockMutationObserver {
        constructor(callback: MutationCallback) {
            observerCallback = callback;
        }
        observe(target: Node, options?: MutationObserverInit) {
            observeSpy(target, options);
        }
        disconnect() {
            disconnectSpy();
        }
    }

    vi.stubGlobal('MutationObserver', MockMutationObserver as unknown as typeof MutationObserver);
});

afterEach(() => {
    document.body.innerHTML = '';
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
});

// --- 测试 ---

describe('CommentPermissionEnforcer', () => {

    describe('pluginName', () => {
        it('应返回 CommentPermissionEnforcer', () => {
            expect(CommentPermissionEnforcer.pluginName).toBe('CommentPermissionEnforcer');
        });
    });

    describe('MutationObserver 生命周期', () => {
        it('init + ready 后应注册 MutationObserver 监听侧栏', () => {
            const { domRoot, sidebar } = setupCommentDom();
            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();

            expect(observeSpy).toHaveBeenCalledWith(sidebar, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ['class', 'aria-expanded'],
            });
        });

        it('destroy 应断开 MutationObserver', () => {
            const { domRoot } = setupCommentDom();
            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();
            plugin.destroy();

            expect(disconnectSpy).toHaveBeenCalledTimes(1);
        });

        it('Users 插件不可用时不应注册 observer', () => {
            const { domRoot } = setupCommentDom();
            const editor = {
                on: (_: string, callback: () => void) => callback(),
                plugins: {
                    get: (name: string) => {
                        if (name === 'Users') throw new Error('not available');
                        throw new Error(`Unknown plugin: ${name}`);
                    },
                },
                editing: { view: { getDomRoot: () => domRoot } },
            };
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();

            expect(observeSpy).not.toHaveBeenCalled();
        });

        it('Users.me 为空时不应注册 observer', () => {
            const { domRoot } = setupCommentDom();
            const editor = {
                on: (_: string, callback: () => void) => callback(),
                plugins: {
                    get: (name: string) => {
                        if (name === 'Users') return { me: undefined };
                        throw new Error(`Unknown plugin: ${name}`);
                    },
                },
                editing: { view: { getDomRoot: () => domRoot } },
            };
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();

            expect(observeSpy).not.toHaveBeenCalled();
        });
    });

    describe('re-entrant 保护', () => {
        it('_enforcing 为 true 时 observer 回调不应执行 _doEnforce', () => {
            const { domRoot } = setupCommentDom();
            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);
            const doEnforceSpy = vi.spyOn(plugin as any, '_doEnforce');

            plugin.init();
            triggerReady();
            doEnforceSpy.mockClear();

            // 模拟 re-entrant 场景
            (plugin as any)._enforcing = true;
            observerCallback?.([] as MutationRecord[], {} as MutationObserver);

            expect(doEnforceSpy).not.toHaveBeenCalled();
        });

        it('_enforcing 为 false 时 observer 回调应执行权限检查', () => {
            const { domRoot } = setupCommentDom();
            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);
            const doEnforceSpy = vi.spyOn(plugin as any, '_doEnforce');

            plugin.init();
            triggerReady();
            doEnforceSpy.mockClear();

            observerCallback?.([] as MutationRecord[], {} as MutationObserver);

            expect(doEnforceSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('权限执行（基于 data-author-id）', () => {
        it('应隐藏非当前用户评论的 Edit/Remove 菜单', () => {
            const { domRoot, sidebar } = setupCommentDom();

            const ownComment = createCommentElement('Alice', 'user-1');
            const otherComment = createCommentElement('Bob', 'user-2');

            sidebar.appendChild(ownComment.comment);
            sidebar.appendChild(otherComment.comment);

            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();

            // 自己的评论菜单应保持可见
            expect(ownComment.dropdown.style.getPropertyValue('display')).toBe('');
            // 他人评论菜单应隐藏，使用 !important
            expect(otherComment.dropdown.style.getPropertyValue('display')).toBe('none');
            expect(otherComment.dropdown.style.getPropertyPriority('display')).toBe('important');
        });

        it('应显示当前用户评论的 Edit/Remove 菜单', () => {
            const { domRoot, sidebar } = setupCommentDom();

            const ownComment = createCommentElement('Alice', 'user-1');
            sidebar.appendChild(ownComment.comment);

            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();

            expect(ownComment.dropdown.style.getPropertyValue('display')).toBe('');
        });

        it('多个用户共享同一显示名时应正确区分（data-author-id 不同）', () => {
            const { domRoot, sidebar } = setupCommentDom();

            // 两个 "Anonymous User"，不同 author ID
            const ownComment = createCommentElement('Anonymous User', 'user-anonymous-user');
            const otherComment = createCommentElement('Anonymous User', 'user-cabbc949');

            sidebar.appendChild(ownComment.comment);
            sidebar.appendChild(otherComment.comment);

            const { editor, triggerReady } = createMockEditor(domRoot, 'user-anonymous-user');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();

            // 自己的评论菜单应可见
            expect(ownComment.dropdown.style.getPropertyValue('display')).toBe('');
            // 他人同名评论菜单应隐藏
            expect(otherComment.dropdown.style.getPropertyValue('display')).toBe('none');
            expect(otherComment.dropdown.style.getPropertyPriority('display')).toBe('important');
        });

        it('没有 data-author-id 的评论元素应被跳过', () => {
            const { domRoot, sidebar } = setupCommentDom();

            // 创建缺少 data-author-id 的评论
            const comment = document.createElement('div');
            comment.className = 'ck-comment ck-annotation';
            const actions = document.createElement('div');
            actions.className = 'ck-annotation__actions';
            const dropdown = document.createElement('div');
            dropdown.className = 'ck-dropdown';
            actions.appendChild(dropdown);
            comment.appendChild(actions);
            sidebar.appendChild(comment);

            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();

            // 缺少 data-author-id 的评论不应被处理
            expect(dropdown.style.getPropertyValue('display')).toBe('');
        });

        it('之前被隐藏的自己评论在重新执行时应恢复可见', () => {
            const { domRoot, sidebar } = setupCommentDom();

            const comment = createCommentElement('Alice', 'user-1');
            // 模拟之前被错误隐藏
            comment.dropdown.style.setProperty('display', 'none', 'important');
            sidebar.appendChild(comment.comment);

            const { editor, triggerReady } = createMockEditor(domRoot, 'user-1');
            const plugin = new CommentPermissionEnforcer(editor as any);

            plugin.init();
            triggerReady();

            // 应恢复可见
            expect(comment.dropdown.style.getPropertyValue('display')).toBe('');
        });
    });
});
