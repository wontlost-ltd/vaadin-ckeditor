/**
 * fullscreen-page 单测（issue #156）：属性归一化、写入元素、全屏回调包装。
 */
import { describe, it, expect, vi } from 'vitest';
import {
    DEFAULT_FULLSCREEN_PAGE,
    FULLSCREEN_PAGE_ATTR,
    FULLSCREEN_PAGE_MIN_WIDTH_VAR,
    FULLSCREEN_PAGE_WIDTH_VAR,
    applyFullscreenPage,
    pageMinWidth,
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

describe('pageMinWidth', () => {
    it.each([
        ['1200px', '1200px'],
        ['90vw', '90vw'],
        ['calc(1200px + 2rem)', 'calc(1200px + 2rem)'],
        ['80%', '0px'],
        ['50cqw', '0px'],
        ['62.5cqi', '0px'],
        ['calc(100% - 40px)', '0px'],
        // 减去相对项：页面随编辑列变窄，不设下限，避免不必要的横向滚动
        ['calc(1200px - 10%)', '0px'],
        ['max(800px, calc(1200px - 5cqw))', '0px'],
        ['calc(1200px - min(10%, 20px))', '0px'],
        ['calc(1200px - 40px)', 'calc(1200px - 40px)'],
        ['calc(100% + 120px)', 'calc(0px + 120px)'],
        ['min(1400px, 90%)', 'min(1400px, 0px)'],
        ['max(50cqw, 1200px)', 'max(0px, 1200px)'],
        ['clamp(600px, 80%, 1400px)', 'clamp(600px, 0px, 1400px)'],
        ['calc(100% * 0.5)', 'calc(0px * 0.5)'],
    ])('%s → %s (column-relative terms count as 0)', (width, expected) => {
        expect(pageMinWidth(width)).toBe(expected);
    });
});

describe('applyFullscreenPage', () => {
    it('sets the column min-width variable for custom widths only (issue #156 overlap)', () => {
        const el = document.createElement('div');
        applyFullscreenPage(el, { mode: 'custom', width: '1200px' });
        expect(el.style.getPropertyValue(FULLSCREEN_PAGE_MIN_WIDTH_VAR)).toBe('1200px');
        applyFullscreenPage(el, { mode: 'custom', width: 'max(50cqw, 1200px)' });
        expect(el.style.getPropertyValue(FULLSCREEN_PAGE_MIN_WIDTH_VAR)).toBe('max(0px, 1200px)');
        applyFullscreenPage(el, { mode: 'custom', width: '1200px' });
        applyFullscreenPage(el, { mode: 'a4-fit', width: null });
        expect(el.style.getPropertyValue(FULLSCREEN_PAGE_MIN_WIDTH_VAR)).toBe('');
    });

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
