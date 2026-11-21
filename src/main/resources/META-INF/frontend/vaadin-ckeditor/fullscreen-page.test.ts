/**
 * fullscreen-page 单测（issue #156）：属性归一化、写入元素、全屏回调包装。
 */
import { describe, it, expect, vi } from 'vitest';
import {
    DEFAULT_FULLSCREEN_PAGE,
    FULLSCREEN_PAGE_ATTR,
    FULLSCREEN_PAGE_WIDTH_VAR,
    applyFullscreenPage,
    resolveFullscreenPage,
    wrapFullscreenPageCallbacks,
} from './fullscreen-page';

const allSupported = () => true;

describe('resolveFullscreenPage', () => {
    it('defaults to a4-fit when the mode is missing or unknown', () => {
        const warn = vi.fn();
        expect(resolveFullscreenPage(undefined, undefined, allSupported, warn)).toEqual(DEFAULT_FULLSCREEN_PAGE);
        expect(resolveFullscreenPage('', '', allSupported, warn)).toEqual(DEFAULT_FULLSCREEN_PAGE);
        expect(warn).not.toHaveBeenCalled();
        expect(resolveFullscreenPage('zoom', '', allSupported, warn)).toEqual(DEFAULT_FULLSCREEN_PAGE);
        expect(warn).toHaveBeenCalledOnce();
    });

    it('keeps predefined modes and ignores any width for them', () => {
        for (const mode of ['a4-fit', 'a4', 'full-width'] as const) {
            expect(resolveFullscreenPage(mode, '1200px', allSupported)).toEqual({ mode, width: null });
        }
    });

    it('accepts a custom width the browser supports, normalised to lower case', () => {
        expect(resolveFullscreenPage('custom', ' MIN(1400PX, 90%) ', allSupported))
            .toEqual({ mode: 'custom', width: 'min(1400px, 90%)' });
    });

    it.each([
        '', '1200px; color: red', '1200px}', 'var(--x)', 'url("x")', '"1200px"', 'a'.repeat(101), '1200foo', 'env(x)',
    ])('falls back to a4-fit for an unsafe custom width %j', (width) => {
        const warn = vi.fn();
        expect(resolveFullscreenPage('custom', width, allSupported, warn)).toEqual(DEFAULT_FULLSCREEN_PAGE);
        expect(warn).toHaveBeenCalledOnce();
    });

    it('falls back to a4-fit when the browser rejects the width', () => {
        const supports = vi.fn(() => false);
        expect(resolveFullscreenPage('custom', 'calc(1px +)', supports)).toEqual(DEFAULT_FULLSCREEN_PAGE);
        expect(supports).toHaveBeenCalledWith('calc(1px +)');
    });
});

describe('applyFullscreenPage', () => {
    it('writes the mode attribute and sets / removes the width variable', () => {
        const el = document.createElement('div');
        applyFullscreenPage(el, { mode: 'custom', width: '1200px' });
        expect(el.getAttribute(FULLSCREEN_PAGE_ATTR)).toBe('custom');
        expect(el.style.getPropertyValue(FULLSCREEN_PAGE_WIDTH_VAR)).toBe('1200px');

        applyFullscreenPage(el, { mode: 'a4', width: null });
        expect(el.getAttribute(FULLSCREEN_PAGE_ATTR)).toBe('a4');
        expect(el.style.getPropertyValue(FULLSCREEN_PAGE_WIDTH_VAR)).toBe('');
    });
});

describe('wrapFullscreenPageCallbacks', () => {
    it('applies the current page to the wrapper on enter and keeps earlier callbacks', () => {
        const order: string[] = [];
        const config: Record<string, unknown> = {
            fullscreen: {
                menuBar: { isVisible: false },
                onEnterCallback: () => order.push('previous-enter'),
                onLeaveCallback: () => order.push('previous-leave'),
            },
        };
        let page = { mode: 'full-width' as const, width: null };
        const onWrapper = vi.fn();
        wrapFullscreenPageCallbacks(config, () => page, onWrapper);

        const fullscreen = config.fullscreen as Record<string, (w: HTMLElement) => void> & { menuBar: unknown };
        expect(fullscreen.menuBar).toEqual({ isVisible: false });
        const wrapper = document.createElement('div');
        fullscreen.onEnterCallback(wrapper);
        expect(wrapper.getAttribute(FULLSCREEN_PAGE_ATTR)).toBe('full-width');
        expect(onWrapper).toHaveBeenLastCalledWith(wrapper);

        fullscreen.onLeaveCallback(wrapper);
        expect(onWrapper).toHaveBeenLastCalledWith(null);
        expect(order).toEqual(['previous-enter', 'previous-leave']);

        // 设置在回调时求值：改设置后再次进入使用新值
        page = { mode: 'a4', width: null } as never;
        fullscreen.onEnterCallback(wrapper);
        expect(wrapper.getAttribute(FULLSCREEN_PAGE_ATTR)).toBe('a4');
    });

    it('works without existing fullscreen config', () => {
        const config: Record<string, unknown> = {};
        wrapFullscreenPageCallbacks(config, () => DEFAULT_FULLSCREEN_PAGE, () => {});
        const wrapper = document.createElement('div');
        (config.fullscreen as Record<string, (w: HTMLElement) => void>).onEnterCallback(wrapper);
        expect(wrapper.getAttribute(FULLSCREEN_PAGE_ATTR)).toBe('a4-fit');
    });
});
