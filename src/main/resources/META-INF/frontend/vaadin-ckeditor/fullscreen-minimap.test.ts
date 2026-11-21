/**
 * fullscreen-minimap 单测（issue #156）：minimap 容器挪进全屏布局，以及按当前滚动祖先重建 minimap 视图。
 */
import { describe, it, expect, vi } from 'vitest';

vi.mock('ckeditor5', () => ({
    findClosestScrollableAncestor: vi.fn((el: HTMLElement) => el.closest('[data-scroller]')),
}));

import { MINIMAP_SLOT, moveMinimapIntoFullscreen, rebindMinimap, type FullscreenHandlerLike } from './fullscreen-minimap';

/** 与 CKEditor 49.1.0 的全屏容器结构一致的最小桩；moveToFullscreen 记录占位符 */
function fakeHandler() {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = `
        <div class="ck-fullscreen__editable-wrapper">
            <div class="ck-fullscreen__left-sidebar"></div>
            <div class="ck-fullscreen__editable"></div>
            <div class="ck-fullscreen__right-sidebar"></div>
            <div class="ck-fullscreen__right-edge"></div>
        </div>`;
    const moved: string[] = [];
    const handler: FullscreenHandlerLike = {
        getWrapper: () => wrapper,
        moveToFullscreen: (element, name) => {
            element.replaceWith(Object.assign(document.createElement('div'), { className: 'placeholder' }));
            wrapper.querySelector(`[data-ck-fullscreen="${name}"]`)!.append(element);
            moved.push(name);
        },
    };
    return { wrapper, handler, moved };
}

describe('moveMinimapIntoFullscreen', () => {
    it('adds a slot after the right sidebar and moves the container through moveToFullscreen', () => {
        const { wrapper, handler, moved } = fakeHandler();
        const host = document.createElement('div');
        const container = Object.assign(document.createElement('div'), { className: 'minimap-container' });
        host.append(container);

        expect(moveMinimapIntoFullscreen(handler, container, '200px')).toBe(true);
        const children = [...wrapper.querySelector('.ck-fullscreen__editable-wrapper')!.children].map((c) => c.className);
        expect(children).toEqual(['ck-fullscreen__left-sidebar', 'ck-fullscreen__editable', 'ck-fullscreen__right-sidebar',
            'ck ck-fullscreen__minimap', 'ck-fullscreen__right-edge']);
        const slot = wrapper.querySelector<HTMLElement>(`[data-ck-fullscreen="${MINIMAP_SLOT}"]`)!;
        expect(slot.firstElementChild).toBe(container);
        expect(slot.style.getPropertyValue('--ck-minimap-width')).toBe('200px');
        expect(moved).toEqual([MINIMAP_SLOT]);
        // 原处留下占位符，退出时由 CKEditor 放回
        expect(host.querySelector('.placeholder')).not.toBeNull();
    });

    it('does nothing without a container or when already moved', () => {
        const { handler, moved } = fakeHandler();
        expect(moveMinimapIntoFullscreen(handler, null, '')).toBe(false);
        const container = document.createElement('div');
        document.createElement('div').append(container);
        expect(moveMinimapIntoFullscreen(handler, container, '')).toBe(true);
        expect(moveMinimapIntoFullscreen(handler, container, '')).toBe(false);
        expect(moved).toHaveLength(1);
    });
});

describe('rebindMinimap', () => {
    it('destroys the old view and re-initialises against the current scrollable ancestor', () => {
        const scroller = document.createElement('div');
        scroller.setAttribute('data-scroller', '');
        const root = document.createElement('div');
        scroller.append(root);
        const oldElement = document.createElement('div');
        document.body.append(oldElement);
        const destroy = vi.fn();
        const minimap = {
            _minimapView: { destroy, element: oldElement },
            _editingRootElement: root,
            _scrollableRootAncestor: document.body as HTMLElement | null,
            _initializeMinimapView: vi.fn(),
            _syncMinimapToEditingRootScrollPosition: vi.fn(),
        };

        expect(rebindMinimap(minimap)).toBe(true);
        expect(destroy).toHaveBeenCalledOnce();
        expect(oldElement.isConnected).toBe(false);
        expect(minimap._scrollableRootAncestor).toBe(scroller);
        expect(minimap._initializeMinimapView).toHaveBeenCalledOnce();
        expect(minimap._syncMinimapToEditingRootScrollPosition).toHaveBeenCalledOnce();
    });

    it('returns false when the Minimap internals are missing', () => {
        expect(rebindMinimap(null)).toBe(false);
        expect(rebindMinimap({})).toBe(false);
        expect(rebindMinimap({ _minimapView: { destroy() {}, element: document.createElement('div') } })).toBe(false);
    });
});
