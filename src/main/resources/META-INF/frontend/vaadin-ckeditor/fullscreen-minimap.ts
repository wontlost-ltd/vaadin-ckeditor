/**
 * 全屏模式下的 minimap（issue #156）。
 *
 * CKEditor 49.1.0 的 Fullscreen 插件会把大纲（DocumentOutlineUI）、在线用户、批注、AI 面板挪进全屏布局，
 * 但不处理 Minimap：minimap 留在原容器里，而全屏会把 <body> 下其它元素都设为 display:none，于是 minimap 不可见；
 * Minimap 还在 ui.ready 时缓存了编辑区的滚动祖先，拖动 / 点击 / 同步的闭包都绑在旧滚动容器上，
 * 编辑区挪进全屏的 .ck-fullscreen__editable-wrapper 后不再跟随滚动。
 *
 * 本模块：
 * - 进入全屏时在编辑列右侧（右侧栏之后）放一个 `data-ck-fullscreen="vaadin-minimap"` 槽，
 *   用 Fullscreen 处理器的公开方法 moveToFullscreen 把 add-on 的 minimap 容器挪进去——
 *   退出时由 CKEditor 自己按占位符放回原处，退出流程与之前完全一致；
 * - 进入后与退出后各重建一次 minimap 视图，让它绑定当前的滚动祖先。重建依赖 Minimap 插件的
 *   内部成员（CKEditor 49.1.0 固定版本），缺失时只告警、不影响全屏本身。
 */
import { findClosestScrollableAncestor } from 'ckeditor5';

export const MINIMAP_SLOT = 'vaadin-minimap';

/** Fullscreen 处理器中本模块用到的公开方法 */
export interface FullscreenHandlerLike {
    getWrapper(): HTMLElement;
    moveToFullscreen(element: HTMLElement, placeholderName: string): void;
}

/** Minimap 插件的内部成员（ckeditor5-minimap 49.1.0） */
interface MinimapInternals {
    _minimapView?: { destroy(): void; element: HTMLElement } | null;
    _editingRootElement?: HTMLElement;
    _scrollableRootAncestor?: HTMLElement | null;
    _initializeMinimapView?: () => void;
    _syncMinimapToEditingRootScrollPosition?: () => void;
}

/**
 * 在编辑列右侧建 minimap 槽，并把 minimap 容器挪进去。已挪过或缺少容器时返回 false。
 * `minimapWidth` 为宿主上算出的 --ck-minimap-width（全屏容器挂在 body 下，继承不到宿主变量）。
 */
export function moveMinimapIntoFullscreen(
    handler: FullscreenHandlerLike,
    minimapContainer: HTMLElement | null,
    minimapWidth: string,
): boolean {
    const wrapper = handler.getWrapper();
    const editableWrapper = wrapper.querySelector('.ck-fullscreen__editable-wrapper');
    if (!minimapContainer || !editableWrapper || wrapper.querySelector(`[data-ck-fullscreen="${MINIMAP_SLOT}"]`)) {
        return false;
    }
    const slot = wrapper.ownerDocument.createElement('div');
    slot.className = 'ck ck-fullscreen__minimap';
    slot.setAttribute('data-ck-fullscreen', MINIMAP_SLOT);
    if (minimapWidth) {
        slot.style.setProperty('--ck-minimap-width', minimapWidth);
    }
    // 右侧栏之后、AI 面板（right-edge）之前
    editableWrapper.insertBefore(slot, editableWrapper.querySelector('.ck-fullscreen__right-edge'));
    handler.moveToFullscreen(minimapContainer, MINIMAP_SLOT);
    return true;
}

/**
 * 重建 minimap 视图，使其跟随编辑区当前的滚动祖先（并按当前页宽重算缩放）。
 * 内部成员缺失（CKEditor 升级后结构变化）时返回 false，由调用方告警。
 */
export function rebindMinimap(minimap: unknown): boolean {
    const m = minimap as MinimapInternals | null;
    if (!m?._minimapView || !m._editingRootElement || typeof m._initializeMinimapView !== 'function') {
        return false;
    }
    m._minimapView.destroy();
    m._minimapView.element.remove();
    m._scrollableRootAncestor = findClosestScrollableAncestor(m._editingRootElement) as HTMLElement | null;
    m._initializeMinimapView();
    m._syncMinimapToEditingRootScrollPosition?.();
    return true;
}
