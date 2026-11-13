/**
 * overlay-host 单测（issue #143 / #144）
 *
 * Vaadin 25 的 Dialog/overlay 以 popover 进入浏览器顶层；CKEditor 挂在 document.body 上的
 * 浮层容器（.ck-body-wrapper）与全屏容器无论 z-index 多大都压不过顶层。
 * CKEditor 49 起由 ui.overlayContainer 把浮层容器挂进宿主；本模块负责：识别宿主是否位于顶层、
 * 把 CKEditor 自建的 wrapper 提升到顶层、交互时重新置顶、销毁后清理标记。
 */
import { describe, it, expect, afterEach, beforeEach, vi } from 'vitest';
import {
    OVERLAY_HOST_ATTR,
    findTopLayerAncestor,
    configureTopLayerHost,
    promoteHostWrapper,
    releaseOverlayHost,
    promoteToTopLayer,
    demoteFromTopLayer,
    raiseHostLayers,
} from './overlay-host';

/**
 * jsdom 没有 Popover API：用最小桩模拟 showPopover/hidePopover 与 :popover-open，
 * 并记录调用顺序，便于断言「谁最后被 show 谁在最上」。
 */
const popoverLog: string[] = [];
beforeEach(() => {
    popoverLog.length = 0;
    const proto = HTMLElement.prototype as unknown as Record<string, unknown>;
    proto.showPopover = vi.fn(function (this: HTMLElement) {
        this.dataset.popoverOpen = '1';
        popoverLog.push('show:' + this.className);
    });
    proto.hidePopover = vi.fn(function (this: HTMLElement) {
        delete this.dataset.popoverOpen;
        popoverLog.push('hide:' + this.className);
    });
    const originalMatches = Element.prototype.matches;
    vi.spyOn(Element.prototype, 'matches').mockImplementation(function (this: Element, selector: string) {
        if (selector === ':popover-open') {
            return (this as HTMLElement).dataset.popoverOpen === '1';
        }
        return originalMatches.call(this, selector);
    });
});

/** 模拟 Vaadin 25.3 结构：<vaadin-dialog> 的 shadow 内有 <vaadin-dialog-overlay popover> 再套 slot */
function mountDialogLike(): { dialog: HTMLElement; host: HTMLElement } {
    const dialog = document.createElement('vaadin-dialog');
    const shadow = dialog.attachShadow({ mode: 'open' });
    const overlay = document.createElement('vaadin-dialog-overlay');
    overlay.setAttribute('popover', 'manual');
    const overlayShadow = overlay.attachShadow({ mode: 'open' });
    const content = document.createElement('div');
    content.setAttribute('part', 'content');
    content.appendChild(document.createElement('slot'));
    overlayShadow.appendChild(content);
    overlay.appendChild(document.createElement('slot'));
    shadow.appendChild(overlay);

    const host = document.createElement('vaadin-ckeditor');
    dialog.appendChild(host);
    document.body.appendChild(dialog);
    return { dialog, host };
}

afterEach(() => {
    document.body.innerHTML = '';
    vi.restoreAllMocks();
    const proto = HTMLElement.prototype as unknown as Record<string, unknown>;
    delete proto.showPopover;
    delete proto.hidePopover;
});

describe('findTopLayerAncestor', () => {
    it('returns null for an element in the plain document', () => {
        const host = document.createElement('vaadin-ckeditor');
        document.body.appendChild(host);
        expect(findTopLayerAncestor(host)).toBeNull();
    });

    it('finds a popover ancestor reached through slots and shadow roots', () => {
        const { host } = mountDialogLike();
        const found = findTopLayerAncestor(host);
        expect(found?.tagName.toLowerCase()).toBe('vaadin-dialog-overlay');
    });

    it('finds a light-DOM popover ancestor', () => {
        const pop = document.createElement('div');
        pop.setAttribute('popover', 'auto');
        const host = document.createElement('vaadin-ckeditor');
        pop.appendChild(host);
        document.body.appendChild(pop);
        expect(findTopLayerAncestor(host)).toBe(pop);
    });
});

/** 模拟 CKEditor 49 的 BodyCollection：在 ui.overlayContainer（宿主）内自建 .ck-body-wrapper 并放入 .ck-body */
function mountCkWrapper(host: HTMLElement): HTMLElement {
    const wrapper = document.createElement('div');
    wrapper.className = 'ck-body-wrapper';
    const body = document.createElement('div');
    body.className = 'ck ck-body';
    wrapper.appendChild(body);
    host.appendChild(wrapper);
    return wrapper;
}

describe('promoteHostWrapper', () => {
    it('does nothing when the host is not inside the top layer', () => {
        const host = document.createElement('vaadin-ckeditor');
        document.body.appendChild(host);
        const wrapper = mountCkWrapper(host);

        expect(promoteHostWrapper(host)).toBe(false);
        expect(wrapper.hasAttribute('popover')).toBe(false);
        expect(host.hasAttribute(OVERLAY_HOST_ATTR)).toBe(false);
    });

    it('returns false while CKEditor has not mounted its wrapper yet', () => {
        const { host } = mountDialogLike();
        expect(promoteHostWrapper(host)).toBe(false);
        expect(host.hasAttribute(OVERLAY_HOST_ATTR)).toBe(false);
    });

    it('turns the CKEditor-owned wrapper into an open manual popover and marks the host', () => {
        const { host } = mountDialogLike();
        const wrapper = mountCkWrapper(host);

        expect(promoteHostWrapper(host)).toBe(true);
        expect(wrapper.getAttribute('popover')).toBe('manual');
        expect(wrapper.matches(':popover-open')).toBe(true);
        expect(host.hasAttribute(OVERLAY_HOST_ATTR)).toBe(true);
        expect(popoverLog).toEqual(['show:ck-body-wrapper']);
    });

    it('is idempotent across repeated remount notifications', () => {
        const { host } = mountDialogLike();
        mountCkWrapper(host);
        promoteHostWrapper(host);
        promoteHostWrapper(host);
        expect(popoverLog).toEqual(['show:ck-body-wrapper']);
    });

    it('promotes a fresh wrapper after CKEditor replaced the previous one', () => {
        const { host } = mountDialogLike();
        const first = mountCkWrapper(host);
        promoteHostWrapper(host);
        first.remove(); // CKEditor 在容器为空时移除 wrapper
        const second = mountCkWrapper(host);

        expect(promoteHostWrapper(host)).toBe(true);
        expect(second.matches(':popover-open')).toBe(true);
    });
});

describe('releaseOverlayHost', () => {
    it('clears the marker once CKEditor has removed its wrapper', () => {
        const { host } = mountDialogLike();
        const wrapper = mountCkWrapper(host);
        promoteHostWrapper(host);
        wrapper.remove();

        releaseOverlayHost(host);
        expect(host.hasAttribute(OVERLAY_HOST_ATTR)).toBe(false);
    });

    it('keeps the marker while a wrapper is still mounted in the host', () => {
        const { host } = mountDialogLike();
        mountCkWrapper(host);
        promoteHostWrapper(host);

        releaseOverlayHost(host);
        expect(host.hasAttribute(OVERLAY_HOST_ATTR)).toBe(true);
    });

    it('is a no-op for a host that was never marked', () => {
        const host = document.createElement('vaadin-ckeditor');
        document.body.appendChild(host);
        expect(() => releaseOverlayHost(host)).not.toThrow();
    });
});

describe('top layer promotion', () => {
    it('promoteToTopLayer is idempotent for an already open popover', () => {
        const el = document.createElement('div');
        el.className = 'fs';
        document.body.appendChild(el);
        promoteToTopLayer(el);
        promoteToTopLayer(el);
        expect(popoverLog).toEqual(['show:fs']);
    });

    it('demoteFromTopLayer hides an open popover and tolerates a closed one', () => {
        const el = document.createElement('div');
        el.className = 'fs';
        document.body.appendChild(el);
        promoteToTopLayer(el);
        demoteFromTopLayer(el);
        demoteFromTopLayer(el);
        expect(popoverLog).toEqual(['show:fs', 'hide:fs']);
    });

    it('raiseHostLayers re-shows the fullscreen wrapper first and the body wrapper last', () => {
        const { host } = mountDialogLike();
        mountCkWrapper(host);
        promoteHostWrapper(host);
        const fullscreen = document.createElement('div');
        fullscreen.className = 'ck ck-fullscreen__main-wrapper';
        host.appendChild(fullscreen);
        promoteToTopLayer(fullscreen);
        popoverLog.length = 0;

        raiseHostLayers(host);

        expect(popoverLog).toEqual([
            'hide:ck ck-fullscreen__main-wrapper', 'show:ck ck-fullscreen__main-wrapper',
            'hide:ck-body-wrapper', 'show:ck-body-wrapper',
        ]);
    });

    it('raiseHostLayers skips closed popovers', () => {
        const { host } = mountDialogLike();
        mountCkWrapper(host);
        promoteHostWrapper(host);
        const fullscreen = document.createElement('div');
        fullscreen.className = 'ck ck-fullscreen__main-wrapper';
        host.appendChild(fullscreen); // 未 promote：全屏尚未进入
        popoverLog.length = 0;

        raiseHostLayers(host);

        expect(popoverLog).toEqual(['hide:ck-body-wrapper', 'show:ck-body-wrapper']);
    });

    it('works without a Popover API (older browsers): no throw, host still marked', () => {
        const proto = HTMLElement.prototype as unknown as Record<string, unknown>;
        delete proto.showPopover;
        delete proto.hidePopover;
        const { host } = mountDialogLike();
        mountCkWrapper(host);
        expect(() => promoteHostWrapper(host)).not.toThrow();
        expect(host.hasAttribute(OVERLAY_HOST_ATTR)).toBe(true);
        expect(() => raiseHostLayers(host)).not.toThrow();
    });
});

describe('configureTopLayerHost', () => {
    it('leaves the config untouched when the host is not inside the top layer', () => {
        const host = document.createElement('vaadin-ckeditor');
        document.body.appendChild(host);
        const config: Record<string, unknown> = { fullscreen: { menuBar: { isVisible: true } } };

        configureTopLayerHost(host, config);

        expect(config).toEqual({ fullscreen: { menuBar: { isVisible: true } } });
    });

    it('points ui.overlayContainer at the host and wraps the fullscreen callbacks', () => {
        const { host } = mountDialogLike();
        const config: Record<string, unknown> = { ui: { poweredBy: { position: 'inside' } }, fullscreen: { menuBar: { isVisible: true } } };

        configureTopLayerHost(host, config);

        expect((config.ui as Record<string, unknown>).overlayContainer).toBe(host);
        expect((config.ui as Record<string, unknown>).poweredBy).toEqual({ position: 'inside' });
        const fullscreen = config.fullscreen as Record<string, unknown>;
        expect(fullscreen.menuBar).toEqual({ isVisible: true });
        expect(fullscreen.container).toBeUndefined();
        expect(typeof fullscreen.onEnterCallback).toBe('function');
        expect(typeof fullscreen.onLeaveCallback).toBe('function');
    });

    it('wrapped callbacks promote the fullscreen wrapper, re-raise the body wrapper and still call user callbacks', () => {
        const { host } = mountDialogLike();
        mountCkWrapper(host);
        promoteHostWrapper(host);
        const calls: string[] = [];
        const config: Record<string, unknown> = {
            fullscreen: {
                onEnterCallback: () => calls.push('user-enter'),
                onLeaveCallback: () => calls.push('user-leave'),
            },
        };
        configureTopLayerHost(host, config);
        const fullscreen = config.fullscreen as { onEnterCallback(w: HTMLElement): void; onLeaveCallback(w: HTMLElement): void };
        const wrapper = document.createElement('div');
        wrapper.className = 'ck ck-fullscreen__main-wrapper';
        host.appendChild(wrapper);
        popoverLog.length = 0;

        fullscreen.onEnterCallback(wrapper);
        fullscreen.onLeaveCallback(wrapper);

        expect(calls).toEqual(['user-enter', 'user-leave']);
        expect(popoverLog).toEqual([
            'show:ck ck-fullscreen__main-wrapper',
            'hide:ck ck-fullscreen__main-wrapper', 'show:ck ck-fullscreen__main-wrapper',
            'hide:ck-body-wrapper', 'show:ck-body-wrapper',
            'hide:ck ck-fullscreen__main-wrapper',
        ]);
    });

    it('does not intervene at all when the caller configured ui.overlayContainer', () => {
        const { host } = mountDialogLike();
        const custom = document.createElement('div');
        const onEnterCallback = (): void => {};
        const config: Record<string, unknown> = { ui: { overlayContainer: custom }, fullscreen: { onEnterCallback } };

        configureTopLayerHost(host, config);

        expect((config.ui as Record<string, unknown>).overlayContainer).toBe(custom);
        expect((config.fullscreen as Record<string, unknown>).onEnterCallback).toBe(onEnterCallback);
    });

    it('keeps a caller-configured fullscreen.container but still hosts the floating UI', () => {
        const { host } = mountDialogLike();
        const custom = document.createElement('div');
        const config: Record<string, unknown> = { fullscreen: { container: custom } };

        configureTopLayerHost(host, config);

        expect((config.ui as Record<string, unknown>).overlayContainer).toBe(host);
        expect(config.fullscreen).toEqual({ container: custom });
    });
});
