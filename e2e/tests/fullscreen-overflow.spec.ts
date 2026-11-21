import { expect, test, type Page } from '@playwright/test';

/**
 * 全屏中固定宽度的页面比编辑列宽时（issue #156 后续）：页面不得滑到侧栏下面，
 * 放不下时全屏区域横向滚动，侧栏仍可用，minimap 仍跟随并驱动纵向滚动。
 *
 * 固定宽度来源：FullscreenPage.custom(...) 与 html 上的应用级覆盖 --ck-fullscreen-editable-width。
 * 夹具 /fullscreen-doc（文档编辑器，可选大纲 / minimap）。大纲是 CKEditor 付费插件，含大纲的用例
 * 仅在 E2E_PREMIUM=1 时运行（GPL 许可证下编辑器被锁定、内容为空，只验证布局）。
 */

const PREMIUM = process.env.E2E_PREMIUM === '1';
const VIEWPORT = { width: 1366, height: 768 };
const PAGE_WIDTH = 1200;

type Sidebars = { outline: boolean; minimap: boolean };
const SIDEBAR_CASES: Sidebars[] = [
    { outline: false, minimap: false },
    { outline: false, minimap: true },
    { outline: true, minimap: false },
    { outline: true, minimap: true },
];
const label = (s: Sidebars) => (s.outline && s.minimap ? 'outline + minimap' : s.outline ? 'outline' : s.minimap ? 'minimap' : 'no sidebars');

/** 固定宽度来源：custom 页面，或 html 覆盖（分别配 A4_FIT 与 FULL_WIDTH） */
const SOURCES: { name: string; query: Record<string, string> }[] = [
    { name: `custom(${PAGE_WIDTH}px)`, query: { page: `custom:${PAGE_WIDTH}px` } },
    // 混合表达式：相对部分小于固定下限时，页面取固定下限
    { name: `custom(max(50cqw, ${PAGE_WIDTH}px))`, query: { page: `custom:max(50cqw, ${PAGE_WIDTH}px)` } },
    { name: `html override ${PAGE_WIDTH}px · a4-fit`, query: { page: 'a4-fit', width: `${PAGE_WIDTH}px` } },
    { name: `html override ${PAGE_WIDTH}px · full-width`, query: { page: 'full-width', width: `${PAGE_WIDTH}px` } },
];

interface Box { left: number; right: number; width: number }

async function enterFullscreen(page: Page, query: Record<string, string>): Promise<void> {
    await page.goto(`/fullscreen-doc?${new URLSearchParams({ type: 'decoupled', ...query })}`);
    await page.waitForFunction(() => (document.querySelector('vaadin-ckeditor#ckeditor') as unknown as
        { editor?: { state: string } })?.editor?.state === 'ready', null, { timeout: 30_000 });
    await page.locator('vaadin-ckeditor#ckeditor .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
    await expect(page.locator('.ck-fullscreen__main-wrapper .ck-editor__editable')).toBeVisible();
}

/** 页面、有内容的侧栏与全屏滚动区域的几何信息（视口坐标） */
async function geometry(page: Page) {
    return page.evaluate(() => {
        const box = (el: Element) => { const r = el.getBoundingClientRect(); return { left: r.left, right: r.right, width: r.width }; };
        const scroller = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
        const sidebars = Array.from(scroller.children)
            .filter((el) => !el.classList.contains('ck-fullscreen__editable') && el.childElementCount > 0
                && el.getClientRects().length > 0)
            .map((el) => {
                const cs = getComputedStyle(el);
                return { name: el.className, box: box(el), outer: el.getBoundingClientRect().width
                    + parseFloat(cs.marginLeft) + parseFloat(cs.marginRight) };
            });
        return {
            page: box(scroller.querySelector('.ck-editor__editable')!),
            sidebars,
            scroller: { ...box(scroller), clientWidth: scroller.clientWidth, scrollWidth: scroller.scrollWidth },
        };
    });
}

function expectNoOverlap(page: Box, sidebars: { name: string; box: Box; outer: number }[]): void {
    for (const s of sidebars) {
        if (s.name.includes('left-sidebar')) {
            expect(s.box.right, `${s.name} overlaps the page`).toBeLessThanOrEqual(page.left + 0.5);
        } else {
            expect(s.box.left, `${s.name} overlaps the page`).toBeGreaterThanOrEqual(page.right - 0.5);
        }
    }
}

async function scrollLeftTo(page: Page, where: 'start' | 'end'): Promise<void> {
    await page.evaluate((w) => {
        const s = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
        s.scrollLeft = w === 'start' ? 0 : s.scrollWidth;
    }, where);
}

async function trackerTop(page: Page): Promise<number> {
    return page.locator('.ck-fullscreen__main-wrapper .ck-minimap__position-tracker')
        .evaluate((el) => parseFloat((el as HTMLElement).style.top || '0'));
}

test.describe(`fixed page wider than the editing column (${VIEWPORT.width}×${VIEWPORT.height})`, () => {
    test.use({ viewport: VIEWPORT });

    for (const source of SOURCES) {
        for (const sidebars of SIDEBAR_CASES) {
            test(`${source.name} · ${label(sidebars)}: no overlap, scrolls horizontally when needed`, async ({ page }) => {
                test.skip(sidebars.outline && !PREMIUM, 'DocumentOutline is a premium plugin; set E2E_PREMIUM=1');
                await enterFullscreen(page, { ...source.query,
                    ...(sidebars.outline ? { outline: '1' } : {}), ...(sidebars.minimap ? { minimap: '1' } : {}) });

                const g = await geometry(page);
                expect(g.page.width).toBeCloseTo(PAGE_WIDTH, 0);
                expect(g.sidebars.some((s) => s.name.includes('ck-fullscreen__minimap'))).toBe(sidebars.minimap);
                expect(g.sidebars.some((s) => s.name.includes('left-sidebar'))).toBe(sidebars.outline);
                expectNoOverlap(g.page, g.sidebars);

                // 页面 + 侧栏放不下时出现横向滚动，否则不出现。左侧大纲在放不下时可能被 CKEditor 自己折叠成按钮
                // （_adjustVisibleElements 的设计行为），因此按实际宽度计算所需空间
                for (const s of g.sidebars.filter((x) => x.name.includes('left-sidebar'))) {
                    expect(s.name.includes('--collapsed') || s.box.width >= 270, `${s.name} squeezed`).toBe(true);
                }
                const required = g.page.width + g.sidebars.reduce((sum, s) => sum + s.outer, 0);
                const needed = required > g.scroller.clientWidth + 0.5;
                if (needed) {
                    expect(g.scroller.scrollWidth).toBeGreaterThan(g.scroller.clientWidth);
                } else {
                    expect(g.scroller.scrollWidth).toBeLessThanOrEqual(g.scroller.clientWidth);
                }

                // 滚到最右：右侧栏完整可见且仍不与页面重叠；滚回最左：左侧栏完整可见
                await scrollLeftTo(page, 'end');
                const end = await geometry(page);
                expectNoOverlap(end.page, end.sidebars);
                for (const s of end.sidebars.filter((x) => !x.name.includes('left-sidebar'))) {
                    expect(s.box.right).toBeLessThanOrEqual(end.scroller.left + end.scroller.clientWidth + 0.5);
                }
                await scrollLeftTo(page, 'start');
                const start = await geometry(page);
                for (const s of start.sidebars.filter((x) => x.name.includes('left-sidebar'))) {
                    expect(s.box.left).toBeGreaterThanOrEqual(start.scroller.left - 0.5);
                }

                if (sidebars.minimap && !sidebars.outline) {
                    // 横向可滚动时 minimap 仍跟随纵向滚动，并能驱动滚动（滚到最右，minimap 完整可见）
                    await scrollLeftTo(page, 'end');
                    await page.evaluate(() => { document.querySelector('.ck-fullscreen__editable-wrapper')!.scrollTop = 0; });
                    const top0 = await trackerTop(page);
                    await page.evaluate(() => {
                        const s = document.querySelector('.ck-fullscreen__editable-wrapper') as HTMLElement;
                        s.scrollTop = (s.scrollHeight - s.clientHeight) / 2;
                    });
                    await expect.poll(() => trackerTop(page)).toBeGreaterThan(top0 + 5);
                    const before = await page.evaluate(() => document.querySelector('.ck-fullscreen__editable-wrapper')!.scrollTop);
                    const mb = (await page.locator('.ck-fullscreen__main-wrapper .ck-minimap').boundingBox())!;
                    const tb = (await page.locator('.ck-fullscreen__main-wrapper .ck-minimap__position-tracker').boundingBox())!;
                    const above = tb.y - mb.y;
                    const below = mb.y + mb.height - (tb.y + tb.height);
                    await page.mouse.click(mb.x + mb.width / 2, above > below ? mb.y + above / 2 : tb.y + tb.height + below / 2);
                    await expect.poll(() => page.evaluate(() => document.querySelector('.ck-fullscreen__editable-wrapper')!.scrollTop))
                        .not.toBeCloseTo(before, -1);
                }
            });
        }
    }
});

test.describe('pages that fit keep their layout', () => {
    test.use({ viewport: { width: 1024, height: 768 } });

    for (const query of [{ page: 'custom:60%' }, { page: 'custom:50cqw' }, { page: 'custom:calc(100% + 120px)' },
        { page: 'custom:calc(1000px - 10%)' }, { page: 'a4-fit' }, { page: 'full-width' }]) {
        test(`${query.page} · minimap: no horizontal scroll, page beside the minimap`, async ({ page }) => {
            await enterFullscreen(page, { ...query, minimap: '1' });
            const g = await geometry(page);
            expect(g.scroller.scrollWidth).toBeLessThanOrEqual(g.scroller.clientWidth);
            expectNoOverlap(g.page, g.sidebars);
            const column = await page.locator('.ck-fullscreen__main-wrapper .ck-fullscreen__editable').evaluate((el) => el.clientWidth);
            // 百分比与容器查询单位都相对编辑列
            // calc(100% + 120px) 超出编辑列的部分被限制在编辑列内，不压到 minimap
            const expected = { 'custom:60%': column * 0.6, 'custom:50cqw': column * 0.5,
                'custom:calc(100% + 120px)': column, 'custom:calc(1000px - 10%)': Math.min(1000 - column * 0.1, column),
                'a4-fit': column - 56, 'full-width': column }[query.page]!;
            expect(g.page.width).toBeCloseTo(expected, 0);
        });
    }
});
