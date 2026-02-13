/**
 * CommentPermissionEnforcer 内置插件
 *
 * CKEditor Cloud Services 的 `comment:write` 权限只允许用户编辑/删除自己的评论，
 * 但前端 UI 默认为所有评论都显示 Edit/Remove 按钮。
 * 本插件通过 MutationObserver 监听侧栏 DOM 变化，
 * 对非当前用户的评论隐藏 Edit/Remove 菜单下拉按钮（.ck-dropdown），
 * 使 UI 与权限模型保持一致。
 *
 * 使用方式：在 Java 端调用 setCommentPermissionEnforcerEnabled(true)，
 * 或在 annotationSidebarEnabled=true 时自动启用。
 *
 * DOM 结构说明：
 * - 评论元素同时拥有 .ck-comment 和 .ck-annotation 两个 class（在同一元素上，非嵌套）
 * - 按钮使用 aria-labelledby（非 aria-label），需通过 CSS class 定位
 * - CKEditor CSS 会覆盖普通 style 设置，需使用 !important
 */
import { Plugin } from 'ckeditor5';

export default class CommentPermissionEnforcer extends Plugin {
    static get pluginName() {
        return 'CommentPermissionEnforcer' as const;
    }

    private _observer: MutationObserver | null = null;
    private _enforcing = false;

    init(): void {
        const editor = this.editor;

        // 等待 editor ready 后启动观察
        editor.on('ready', () => {
            this._startObserving();
        });
    }

    override destroy(): void {
        this._observer?.disconnect();
        this._observer = null;
        super.destroy();
    }

    private _startObserving(): void {
        const editor = this.editor;

        // 获取当前用户 ID
        let currentUserId: string | undefined;
        try {
            const usersPlugin = editor.plugins.get('Users') as { me?: { id?: string } };
            currentUserId = usersPlugin.me?.id;
        } catch {
            return;
        }
        if (!currentUserId) return;

        // 获取侧栏容器（兼容多种 DOM 结构）
        const domRoot = editor.editing.view.getDomRoot();
        const container = domRoot?.closest('vaadin-ckeditor');
        if (!container) return;

        const sidebar = container.querySelector('.annotation-sidebar-container')
            || container.querySelector('#annotation-sidebar');
        if (!sidebar) return;

        // 执行一次初始检查
        this._enforcePermissions(sidebar as HTMLElement, currentUserId);

        // 监听侧栏 DOM 变化（评论渲染、菜单展开等）
        // 使用 _enforcing 标志避免 MutationObserver 与 style 修改之间的无限循环
        this._observer = new MutationObserver(() => {
            if (this._enforcing) return;
            this._enforcePermissions(sidebar as HTMLElement, currentUserId!);
        });

        this._observer.observe(sidebar, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['class', 'aria-expanded']
        });
    }

    private _enforcePermissions(
        sidebar: HTMLElement,
        currentUserId: string
    ): void {
        this._enforcing = true;
        try {
            this._doEnforce(sidebar, currentUserId);
        } finally {
            this._enforcing = false;
        }
    }

    private _doEnforce(
        sidebar: HTMLElement,
        currentUserId: string
    ): void {
        // .ck-comment 和 .ck-annotation 在同一元素上（非嵌套关系）
        const commentAnnotations = sidebar.querySelectorAll(
            '.ck-comment.ck-annotation'
        );

        for (const commentEl of commentAnnotations) {
            // 使用 data-author-id 属性直接判断评论归属，
            // 比按名称匹配更可靠（多个用户可能共享同一显示名，如 "Anonymous User"）
            const authorId = commentEl.getAttribute('data-author-id');
            if (!authorId) continue;

            const isOwnComment = authorId === currentUserId;

            // 定位 .ck-dropdown（在 .ck-annotation__actions 内），
            // CKEditor 按钮不使用 aria-label，需通过 CSS class 定位
            const actionsContainer = commentEl.querySelector('.ck-annotation__actions');
            if (!actionsContainer) continue;

            const dropdown = actionsContainer.querySelector('.ck-dropdown') as HTMLElement | null;

            if (dropdown) {
                if (isOwnComment) {
                    dropdown.style.removeProperty('display');
                } else {
                    // 使用 !important 覆盖 CKEditor 的 CSS 规则
                    dropdown.style.setProperty('display', 'none', 'important');
                }
            }
        }
    }
}
