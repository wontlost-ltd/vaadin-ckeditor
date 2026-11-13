/**
 * 顶层（top layer）宿主支持 —— issue #143 / #144
 *
 * Vaadin 25 的 Dialog 与各类 overlay 通过 `popover="manual"` 进入浏览器顶层。
 * CKEditor 把浮层（对话框、气泡、提示）统一渲染到 document.body 上的
 * `.ck-body-wrapper`，全屏模式也默认把编辑器搬到 document.body；顶层之外的元素
 * 无论 z-index 多大都显示在 Dialog 之下，并且模态 Dialog 会给 body 加
 * `pointer-events: none`，这些浮层也就无法交互。
 *
 * 解决思路不是抬 z-index，而是让这些容器留在宿主元素 `<vaadin-ckeditor>` 内：
 * 宿主本身被 slot 进 Dialog，位于顶层子树里，Vaadin 的外部点击判定、焦点陷阱、
 * 模态指针屏蔽也都把它视为 Dialog 内部内容。CKEditor 49 起提供 `ui.overlayContainer`：
 * 连接器把它指向宿主，CKEditor 便在宿主内自建 `.ck-body-wrapper` 挂浮层容器，
 * 编辑根连上后懒挂载、销毁时自行移除；全屏插件默认也以该容器为挂载点并铺满视口。
 * 本模块不再搬动任何 CKEditor 节点，只负责顶层判定与 popover 提升。
 *
 * 仅挂进宿主还不够：Vaadin Dialog 的内容区带 `clip-path`，会把越出 Dialog 边框的
 * 浮层裁掉。因此这些容器自身也以 `popover="manual"` 进入顶层——顶层元素不受祖先
 * 裁剪与变换影响，同时仍留在 Dialog 子树内。wrapper 由 CKEditor 懒挂载，
 * 连接器在 BodyCollection 的 `remount` 事件上再次提升（见 promoteHostWrapper）。顶层按 show 的先后排序，
 * Vaadin 重排自身 overlay（hide+show）后可能压到我们上面，故在用户与编辑器交互时
 * 重新 show 一次（见 raiseHostLayers）。hide+show 在同一任务内同步完成，浏览器不会在
 * 其间做渲染更新，因此容器内已聚焦的元素不会失焦；外部监听者在 beforetoggle 上会看到
 * 成对的 open>closed、closed>open，而异步的 toggle 按规范在同一任务内合并，
 * Chromium / Firefox 实测只收到 open>open（e2e/tests/dialog-overlay.spec.ts 有断言）。
 *
 * 边界：Popover API 缺失时只挂不提升（Vaadin 25 自身的 overlay 已依赖 Popover API，
 * 能运行 Vaadin 25 的浏览器必然支持）；调用方显式配置 ui.overlayContainer 或
 * fullscreen.container 时连接器尊重其选择，不介入对应部分。
 *
 * 顶层判定只看祖先是否带 `popover` 属性，不看它是否已打开：Flow 的 Dialog/Popover
 * 只在打开期间把子组件挂进 DOM，关闭即卸载并销毁编辑器，所以宿主被创建时其
 * popover 祖先必然处于打开状态。若有自定义容器在关闭的 popover 内长期保留编辑器，
 * 提升后的空 wrapper 为 0×0 且无内容，也不会产生可见影响。
 */

/** 宿主位于顶层并已提升浮层容器时打在宿主上的标记，供 CSS 重置 popover 的 UA 样式。 */
export const OVERLAY_HOST_ATTR = 'data-ck-overlay-host';

/**
 * 沿扁平树（slot → shadow host → 父元素）向上查找带 `popover` 属性的祖先。
 * 返回第一个命中的元素；不在顶层子树内返回 null。
 */
export function findTopLayerAncestor(element: Element): Element | null {
    let node: Element | null = element;
    while (node) {
        if (node !== element && node.hasAttribute('popover')) {
            return node;
        }
        node = nextFlatTreeParent(node);
    }
    return null;
}

function nextFlatTreeParent(node: Element): Element | null {
    if (node.assignedSlot) {
        return node.assignedSlot;
    }
    if (node.parentElement) {
        return node.parentElement;
    }
    const root = node.getRootNode();
    return root instanceof ShadowRoot ? root.host : null;
}

type FullscreenCallback = (wrapper: HTMLElement) => void;

/**
 * 宿主位于顶层时改写编辑器配置：把 `ui.overlayContainer` 指向宿主，让 CKEditor 把浮层容器
 * 与全屏容器都建在宿主内；并包装 fullscreen 的 onEnter / onLeave，在进入全屏时把全屏容器
 * 提升为顶层 popover、退出时撤销。宿主不在顶层时不改任何东西。
 *
 * 调用方已显式配置 `ui.overlayContainer` 时整体不介入：浮层与全屏都会挂到那个容器，
 * 不在宿主内，本模块的提升与 CSS 复位都不适用。只显式配置 `fullscreen.container`
 * 时仍接管浮层，但不碰全屏。
 */
export function configureTopLayerHost(host: HTMLElement, config: Record<string, unknown>): void {
    if (!findTopLayerAncestor(host)) {
        return;
    }
    const ui = (config.ui ?? {}) as Record<string, unknown>;
    if (ui.overlayContainer) {
        return;
    }
    config.ui = { ...ui, overlayContainer: host };

    const fullscreen = (config.fullscreen ?? {}) as Record<string, unknown>;
    if (fullscreen.container) {
        return;
    }
    const userEnter = fullscreen.onEnterCallback as FullscreenCallback | undefined;
    const userLeave = fullscreen.onLeaveCallback as FullscreenCallback | undefined;
    config.fullscreen = {
        ...fullscreen,
        onEnterCallback: (wrapper: HTMLElement) => {
            promoteToTopLayer(wrapper);
            // 浮层容器必须排在全屏之上，否则全屏里打开的 CKEditor 对话框会被盖住
            raiseHostLayers(host);
            userEnter?.(wrapper);
        },
        onLeaveCallback: (wrapper: HTMLElement) => {
            userLeave?.(wrapper);
            demoteFromTopLayer(wrapper);
        },
    };
}

/**
 * 把 CKEditor 挂在宿主内的 `.ck-body-wrapper` 提升到顶层并给宿主打标记。
 * wrapper 由 CKEditor 在编辑根连上后懒挂载、空了即移除，因此本函数设计为可重复调用：
 * 创建后立即调一次，之后在 BodyCollection 的 `remount` 事件上再调。
 * 宿主不在顶层或 wrapper 尚未出现时不做任何事并返回 false。
 */
export function promoteHostWrapper(host: HTMLElement): boolean {
    const wrapper = hostWrapper(host);
    if (!wrapper || !findTopLayerAncestor(host)) {
        return false;
    }
    host.setAttribute(OVERLAY_HOST_ATTR, '');
    promoteToTopLayer(wrapper);
    return true;
}

/** 让元素以 manual popover 进入顶层；已在顶层或浏览器不支持 Popover API 时不做事。 */
export function promoteToTopLayer(element: HTMLElement): void {
    if (typeof element.showPopover !== 'function' || isPopoverOpen(element)) {
        return;
    }
    element.setAttribute('popover', 'manual');
    element.showPopover();
}

/** 将元素移出顶层；未在顶层时不做事。 */
export function demoteFromTopLayer(element: HTMLElement): void {
    if (typeof element.hidePopover === 'function' && isPopoverOpen(element)) {
        element.hidePopover();
    }
}

/**
 * 把宿主拥有的顶层容器重新排到最上：先全屏容器，再浮层容器，
 * 保证全屏期间 CKEditor 对话框/气泡仍压在全屏之上。只处理当前已打开的 popover。
 */
export function raiseHostLayers(host: HTMLElement): void {
    const fullscreen = host.querySelector<HTMLElement>(':scope > .ck-fullscreen__main-wrapper');
    for (const layer of [fullscreen, hostWrapper(host)]) {
        if (layer && isPopoverOpen(layer)) {
            layer.hidePopover();
            layer.showPopover();
        }
    }
}

function isPopoverOpen(element: Element): boolean {
    try {
        return element.matches(':popover-open');
    } catch {
        return false;
    }
}

/**
 * 编辑器销毁后清理宿主标记。wrapper 由 CKEditor 自己在容器为空时移除；
 * 只有它确已不在宿主内才清标记，以免误清同一宿主上仍存活的编辑器。
 */
export function releaseOverlayHost(host: HTMLElement): void {
    if (!hostWrapper(host)) {
        host.removeAttribute(OVERLAY_HOST_ATTR);
    }
}

function hostWrapper(host: HTMLElement): HTMLElement | null {
    return host.querySelector<HTMLElement>(':scope > .ck-body-wrapper');
}
