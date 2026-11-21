import { expect, test, type Page } from '@playwright/test';

/**
 * 全屏页面宽度与侧栏（issue #156）。夹具 /fullscreen-doc：文档编辑器（DECOUPLED）可选左侧大纲、右侧 minimap。
 *
 * CKEditor 49.1.0 的 Fullscreen 插件把大纲移进全屏左侧栏，但不处理 minimap（留在被隐藏的原容器里、
 * 不跟随全屏滚动）；add-on 把 minimap 挪进编辑列右侧并重新绑定滚动。
 * 规则：编辑列 = 全屏宽度 − 可见侧栏（含外边距）；A4_FIT 页宽 = 编辑列 − 2 × 28px，FULL_WIDTH = 编辑列，
 * A4 = 795.701px；页面不与侧栏重叠，侧栏不被压缩。
 *
 * 大纲（DocumentOutline）是 CKEditor 付费插件：sample 默认不安装 ckeditor5-premium-features，
 * 含大纲的用例仅在 E2E_PREMIUM=1（sample 以安装了该包的方式构建）时运行；以 GPL 许可证加载付费插件时
 * CKEditor 把编辑器锁为只读并清空内容，此时只能验证布局，大纲跳转另需 CKEDITOR_LICENSE_KEY（夹具从环境变量读取）。
 */

const A4_WIDTH = 795.701;
const GUTTER = 28;
const MINIMAP_WIDTH = 180;
const PREMIUM = process.env.E2E_PREMIUM === '1';
const LICENSED = !!process.env.CKEDITOR_LICENSE_KEY;

type Sidebars = { outline: boolean; minimap: boolean };
const SIDEBAR_CASES: Sidebars[] = [
    { outline: false, minimap: false },
    { outline: false, minimap: true },
    { outline: true, minimap: false },
    { outline: true, minimap: true },
];
const label = (s: Sidebars) => (s.outline && s.minimap ? 'outline + minimap' : s.outline ? 'outline' : s.minimap ? 'minimap' : 'no sidebars');

interface Box { left: number; right: number; width: number }
interface Layout {
    wrapperWidth: number;
    column: number;
    page: Box;
    sidebars: { name: string; box: Box; outer: number }[];
}

async function open(page: Page, query: Record<string, string>): Promise<void> {
    await page.goto(`/fullscreen-doc?${new URLSearchParams(query)}`);
    await page.waitForFunction(() => (document.querySelector('vaadin-ckeditor#ckeditor') as unknown as
        { editor?: { state: string } })?.editor?.state === 'ready', null, { timeout: 30_000 });
}

async function toggleFullscreen(page: Page, inFullscreen: boolean): Promise<void> {
    const scope = inFullscreen ? '.ck-fullscreen__main-wrapper' : 'vaadin-ckeditor#ckeditor';
    await page.locator(`${scope} .ck-toolbar button[data-cke-tooltip-text*="ull"]`).first().click();
    await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(inFullscreen ? 0 : 1);
}

/** 全屏布局：编辑列、页面与各可见侧栏（左侧栏 / 右侧栏 / minimap 槽）的位置 */
async function layout(page: Page): Promise<Layout> {
    return page.evaluate(() => {
        const box = (el: Element) => { const r = el.getBoundingClientRect(); return { left: r.left, right: r.right, width: r.width }; };
        const wrapper = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
        // 只计有内容的侧栏：A4 模式下 CKEditor 的空侧栏仍占位（用于居中），不属于「可见侧栏」
        const sidebars = Array.from(wrapper.children)
            .filter((el) => !el.classList.contains('ck-fullscreen__editable') && el.childElementCount > 0
                && el.getClientRects().length > 0)
            .map((el) => {
                const cs = getComputedStyle(el);
                const b = box(el);
                return { name: el.className, box: b, outer: b.width + parseFloat(cs.marginLeft) + parseFloat(cs.marginRight) };
            })
            .filter((s) => s.box.width > 0);
        return {
            wrapperWidth: wrapper.clientWidth,
            column: (wrapper.querySelector('.ck-fullscreen__editable') as HTMLElement).clientWidth,
            page: box(wrapper.querySelector('.ck-editor__editable')!),
            sidebars,
        };
    });
}

/** 非全屏时文档编辑器的布局快照，用于确认退出全屏后完全复原 */
async function inlineSnapshot(page: Page) {
    return page.evaluate(() => {
        const host = document.querySelector('vaadin-ckeditor#ckeditor')!;
        const rect = (sel: string) => { const el = host.querySelector(sel); if (!el || !el.getClientRects().length) return null; const r = el.getBoundingClientRect(); return [Math.round(r.left), Math.round(r.width)]; };
        return {
            editable: rect('.editor-container__editor .ck-editor__editable'),
            outline: rect('#editor-outline .ck-document-outline'),
            minimap: rect('.editor-container__editor-wrapper > .minimap-container .ck-minimap'),
        };
    });
}

async function minimapTrackerTop(page: Page, scope: string): Promise<number> {
    return page.locator(`${scope} .ck-minimap__position-tracker`).evaluate((el) => parseFloat((el as HTMLElement).style.top || '0'));
}

for (const viewport of [{ width: 1600, height: 900 }, { width: 1280, height: 800 }]) {
    test.describe(`document editor ${viewport.width}×${viewport.height}`, () => {
        test.use({ viewport });

        for (const sidebars of SIDEBAR_CASES) {
            for (const mode of ['a4-fit', 'a4', 'full-width'] as const) {
                test(`${label(sidebars)} · ${mode}: page fits beside the visible sidebars (#156)`, async ({ page }) => {
                    test.skip(sidebars.outline && !PREMIUM, 'DocumentOutline is a premium plugin; set E2E_PREMIUM=1');
                    await open(page, { type: 'decoupled', page: mode,
                        ...(sidebars.outline ? { outline: '1' } : {}), ...(sidebars.minimap ? { minimap: '1' } : {}) });
                    const before = await inlineSnapshot(page);
                    await toggleFullscreen(page, false);

                    const l = await layout(page);
                    const names = l.sidebars.map((s) => s.name).join(' ');
                    expect(names.includes('ck-fullscreen__left-sidebar'), names).toBe(sidebars.outline);
                    expect(names.includes('ck-fullscreen__minimap'), names).toBe(sidebars.minimap);

                    if (mode === 'a4') {
                        expect(l.page.width).toBeCloseTo(A4_WIDTH, 0);
                    } else {
                        // 编辑列 = 全屏宽度 − 可见侧栏（含外边距）
                        const sidebarSpace = l.sidebars.reduce((sum, s) => sum + s.outer, 0);
                        expect(l.column).toBeCloseTo(l.wrapperWidth - sidebarSpace, 0);
                        expect(l.page.width).toBeCloseTo(mode === 'a4-fit' ? l.column - 2 * GUTTER : l.column, 0);
                    }
                    // 侧栏不被压缩、页面不与侧栏重叠
                    for (const s of l.sidebars) {
                        // 左侧栏可能被 CKEditor 自己折叠成按钮（A4 页放不下时的设计行为），折叠不算挤压
                        if (s.name.includes('left-sidebar') && !s.name.includes('--collapsed')) {
                            expect(s.box.width).toBeGreaterThanOrEqual(270);
                        }
                        if (s.name.includes('minimap')) expect(s.box.width).toBeCloseTo(MINIMAP_WIDTH, 0);
                        // 大纲（左侧栏）必须在页面左边，minimap 与右侧栏在页面右边，且互不重叠
                        if (s.name.includes('left-sidebar')) {
                            expect(s.box.right, `${s.name} must be left of the page`).toBeLessThanOrEqual(l.page.left + 0.5);
                        } else {
                            expect(s.box.left, `${s.name} must be right of the page`).toBeGreaterThanOrEqual(l.page.right - 0.5);
                        }
                    }

                    if (sidebars.outline && LICENSED) {
                        // 大纲在全屏中可用：点击条目跳转到对应标题
                        await page.locator('.ck-fullscreen__main-wrapper .ck-document-outline__item')
                            .filter({ hasText: /^Section 9$/ }).click();
                        await expect.poll(() => page.evaluate(() => {
                            const scroller = document.querySelector('.ck-fullscreen__editable-wrapper')!.getBoundingClientRect();
                            const heading = Array.from(document.querySelectorAll('.ck-fullscreen__main-wrapper .ck-editor__editable h2'))
                                .find((h) => h.textContent === 'Section 9')!.getBoundingClientRect();
                            return heading.top >= scroller.top && heading.bottom <= scroller.bottom;
                        })).toBe(true);
                    }

                    // 以 GPL 许可证加载付费大纲插件时编辑器被锁定、内容为空，无从滚动
                    const hasContent = !sidebars.outline || LICENSED;
                    if (sidebars.minimap && hasContent) {
                        // minimap 跟随全屏滚动，并能反向驱动滚动
                        const scope = '.ck-fullscreen__main-wrapper';
                        await page.evaluate(() => {
                            const w = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
                            w.scrollTop = 0;
                        });
                        const top0 = await minimapTrackerTop(page, scope);
                        await page.evaluate(() => {
                            const w = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
                            w.scrollTop = (w.scrollHeight - w.clientHeight) / 2;
                        });
                        await expect.poll(() => minimapTrackerTop(page, scope)).toBeGreaterThan(top0 + 5);

                        const scrollBefore = await page.evaluate(() => document.querySelector('.ck-fullscreen__editable-wrapper')!.scrollTop);
                        // 点在位置指示框之外（点指示框是拖动），哪一侧空间大点哪一侧
                        const fb = (await page.locator(`${scope} .ck-minimap`).boundingBox())!;
                        const tb = (await page.locator(`${scope} .ck-minimap__position-tracker`).boundingBox())!;
                        const above = tb.y - fb.y;
                        const below = fb.y + fb.height - (tb.y + tb.height);
                        await page.mouse.click(fb.x + fb.width / 2, above > below ? fb.y + above / 2 : tb.y + tb.height + below / 2);
                        await expect.poll(() => page.evaluate(() => document.querySelector('.ck-fullscreen__editable-wrapper')!.scrollTop))
                            .not.toBeCloseTo(scrollBefore, -1);
                    }

                    if (sidebars.minimap && hasContent && mode === 'a4-fit') {
                        // 全屏中窗口变窄：页面变窄，minimap 按新页宽重建后仍跟随滚动
                        const widthBefore = l.page.width;
                        await page.setViewportSize({ width: viewport.width - 300, height: viewport.height });
                        await expect.poll(async () => (await layout(page)).page.width).toBeLessThan(widthBefore - 250);
                        const scope = '.ck-fullscreen__main-wrapper';
                        await expect.poll(() => page.evaluate(() => {
                            const body = (document.querySelector('.ck-fullscreen__main-wrapper .ck-minimap__iframe') as HTMLIFrameElement)
                                ?.contentDocument?.body;
                            const editable = document.querySelector('.ck-fullscreen__main-wrapper .ck-editor__editable')!;
                            return body ? Math.abs(parseFloat(body.style.width) - editable.getBoundingClientRect().width) : 999;
                        })).toBeLessThan(2);
                        await page.evaluate(() => { document.querySelector('.ck-fullscreen__editable-wrapper')!.scrollTop = 0; });
                        await expect.poll(() => minimapTrackerTop(page, scope)).toBeLessThan(2);
                        await page.evaluate(() => {
                            const w = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
                            w.scrollTop = w.scrollHeight - w.clientHeight;
                        });
                        await expect.poll(() => minimapTrackerTop(page, scope)).toBeGreaterThan(10);
                        await page.setViewportSize(viewport);
                    }

                    // 退出全屏后布局与进入前一致，minimap 回到原处且继续跟随原滚动容器
                    await toggleFullscreen(page, true);
                    expect(await inlineSnapshot(page)).toEqual(before);
                    if (sidebars.minimap && hasContent) {
                        const scope = 'vaadin-ckeditor#ckeditor .editor-container__editor-wrapper';
                        const top0 = await minimapTrackerTop(page, scope);
                        await page.locator(scope).evaluate((w) => { w.scrollTop = (w.scrollHeight - w.clientHeight) / 2; });
                        await expect.poll(() => minimapTrackerTop(page, scope)).toBeGreaterThan(top0 + 5);
                    }
                });
            }
        }
    });
}

test.describe('classic editor', () => {
    test.use({ viewport: { width: 1600, height: 900 } });

    for (const mode of ['a4-fit', 'a4', 'full-width'] as const) {
        test(`classic · ${mode}: page width with setWidthFull() (#156)`, async ({ page }) => {
            await open(page, { type: 'classic', page: mode });
            const inlineWidth = await page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable').evaluate((el) => el.getBoundingClientRect().width);
            await toggleFullscreen(page, false);
            const l = await layout(page);
            expect(l.sidebars).toEqual([]);
            const expected = mode === 'a4' ? A4_WIDTH : mode === 'a4-fit' ? l.column - 2 * GUTTER : l.column;
            // 连接器写在编辑根上的内联 width:100%（setWidthFull）不能压窄全屏页面
            expect(l.page.width).toBeCloseTo(expected, 0);
            await toggleFullscreen(page, true);
            // 退出后内联宽度照常生效
            expect(await page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable').evaluate((el) => el.getBoundingClientRect().width))
                .toBeCloseTo(inlineWidth, 0);
        });
    }
});

test.describe('balloon and inline editors', () => {
    test.use({ viewport: { width: 1600, height: 900 } });

    for (const type of ['balloon', 'inline'] as const) {
        test(`${type}: setFullscreenPage is a documented no-op (#156)`, async ({ page }) => {
            const errors: string[] = [];
            page.on('pageerror', (e) => errors.push(e.message));
            page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
            const width = async (mode: string) => {
                await open(page, { type, page: mode });
                return page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable').evaluate((el) => el.getBoundingClientRect().width);
            };
            const a4 = await width('a4');
            expect(await width('full-width')).toBeCloseTo(a4, 0);
            expect(await width('a4-fit')).toBeCloseTo(a4, 0);
            await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(0);
            expect(errors).toEqual([]);
        });
    }
});
