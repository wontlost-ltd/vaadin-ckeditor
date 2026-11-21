import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 全屏页面宽度（issue #156）。CKEditor 全屏模式把编辑区固定为 A4 纸宽 795.701px
 * （ckeditor5-fullscreen theme/fullscreen.css），没有配置项。add-on 的 FullscreenPage：
 *
 * - A4_FIT（默认）：页宽 = 编辑列宽 − 2 × 留白（--ck-fullscreen-editor-top-margin，28px），
 *   最小高度与页边距按 A4（210 × 297mm，页边距 20mm × 12mm）等比缩放；随窗口与浏览器缩放变化
 * - A4：795.701px；FULL_WIDTH：铺满编辑列；custom(css)：任意 CSS 宽度
 * - html 上的 --ck-fullscreen-editable-width 是应用级覆盖，优先于各编辑器的设置
 */

const A4_WIDTH = 795.701;
const GUTTER = 28;

const PAGE = '.ck-fullscreen__main-wrapper .ck-editor__editable';
const COLUMN = '.ck-fullscreen__main-wrapper .ck-fullscreen__editable';

interface Metrics {
    page: number;
    column: number;
    minHeight: number;
    paddingTop: number;
    paddingLeft: number;
}

async function enterFullscreen(page: Page, query = '', editorId = 'ckeditor'): Promise<void> {
    await page.goto(`/fullscreen${query}`);
    await waitForCKEditorReady(page);
    await page.locator(`vaadin-ckeditor#${editorId} .ck-toolbar button[data-cke-tooltip-text*="ull"]`).first().click();
    await expect(page.locator(PAGE)).toBeVisible();
}

async function leaveFullscreen(page: Page): Promise<void> {
    await page.locator('.ck-fullscreen__main-wrapper .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
    await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(0);
}

async function metrics(page: Page): Promise<Metrics> {
    return page.evaluate(([pageSel, columnSel]) => {
        const el = document.querySelector(pageSel) as HTMLElement;
        const cs = getComputedStyle(el);
        return {
            page: el.getBoundingClientRect().width,
            column: (document.querySelector(columnSel) as HTMLElement).clientWidth,
            minHeight: parseFloat(cs.minHeight),
            paddingTop: parseFloat(cs.paddingTop),
            paddingLeft: parseFloat(cs.paddingLeft),
        };
    }, [PAGE, COLUMN] as const);
}

function expectA4Fit(m: Metrics): void {
    expect(m.page).toBeCloseTo(m.column - 2 * GUTTER, 0);
    expect(m.minHeight / m.page).toBeCloseTo(297 / 210, 2);
    expect(m.paddingTop / m.page).toBeCloseTo(20 / 210, 2);
    expect(m.paddingLeft / m.page).toBeCloseTo(12 / 210, 2);
}

const custom = (css: string) => `custom:${encodeURIComponent(css)}`;

test.describe('A4_FIT (default)', () => {
    for (const viewport of [{ width: 1024, height: 768 }, { width: 1600, height: 900 }, { width: 2400, height: 1200 }]) {
        test(`page follows the available width at ${viewport.width}×${viewport.height} (#156)`, async ({ page }) => {
            await page.setViewportSize(viewport);
            await enterFullscreen(page);
            const m = await metrics(page);
            expectA4Fit(m);
            // 列宽 = 视口宽（无侧栏，减去可能的滚动条）
            expect(m.column).toBeGreaterThan(viewport.width - 20);
            expect(m.column).toBeLessThanOrEqual(viewport.width);
        });
    }

    test('window resize while in fullscreen resizes the page (#156)', async ({ page }) => {
        await page.setViewportSize({ width: 1600, height: 900 });
        await enterFullscreen(page);
        const wide = await metrics(page);
        await page.setViewportSize({ width: 1100, height: 900 });
        await expect.poll(async () => (await metrics(page)).page).toBeLessThan(wide.page - 400);
        expectA4Fit(await metrics(page));
    });

    test('explicit A4_FIT equals the default (#156)', async ({ page }) => {
        await page.setViewportSize({ width: 1600, height: 900 });
        await enterFullscreen(page, '?page=a4-fit');
        expectA4Fit(await metrics(page));
    });
});

test.describe('A4_FIT at 200 % browser zoom', () => {
    // 1600×900 的窗口在 200 % 缩放下：CSS 视口 800×450、设备像素比 2
    test.use({ viewport: { width: 800, height: 450 }, deviceScaleFactor: 2 });

    test('page fills the zoomed viewport instead of a fixed 795.7px column (#156)', async ({ page }) => {
        await enterFullscreen(page);
        const m = await metrics(page);
        expectA4Fit(m);
        expect(m.page).toBeLessThan(A4_WIDTH - 40);
        expect(await page.evaluate(() => window.devicePixelRatio)).toBe(2);
    });
});

test.describe('other pages', () => {
    test.beforeEach(async ({ page }) => {
        await page.setViewportSize({ width: 1600, height: 900 });
    });

    test('A4 keeps CKEditor\'s fixed 795.701px page (#156)', async ({ page }) => {
        await enterFullscreen(page, '?page=a4');
        const m = await metrics(page);
        expect(m.page).toBeCloseTo(A4_WIDTH, 1);
        // 布局不动：页边距与最小高度仍是 CKEditor 的 20mm × 12mm、297mm
        expect(m.paddingTop).toBeCloseTo(20 * 96 / 25.4, 0);
        expect(m.minHeight).toBeCloseTo(297 * 96 / 25.4, 0);
    });

    test('FULL_WIDTH fills the editing column (#156)', async ({ page }) => {
        await enterFullscreen(page, '?page=full-width');
        const m = await metrics(page);
        expect(m.page).toBeCloseTo(m.column, 0);
        // 编辑列铺满视口（空侧栏不占位），而不是收缩包住一张 A4 页
        expect(m.column).toBeGreaterThan(1580);
    });

    for (const [css, expected] of [['1000px', () => 1000], ['60%', (column: number) => column * 0.6]] as const) {
        test(`custom("${css}") sets the page and source view width (#156)`, async ({ page }) => {
            await enterFullscreen(page, `?page=${custom(css)}`);
            const m = await metrics(page);
            expect(m.page).toBeCloseTo(expected(m.column), 0);

            await page.locator('.ck-fullscreen__main-wrapper .ck-toolbar .ck-source-editing-button').click();
            const source = page.locator('.ck-fullscreen__main-wrapper .ck-source-editing-area');
            await expect(source).toBeVisible();
            expect(await source.evaluate((el) => el.getBoundingClientRect().width)).toBeCloseTo(expected(m.column), 0);
        });
    }

    test('two editors on one page use their own setting (#156)', async ({ page }) => {
        await enterFullscreen(page, '?page=a4&page2=full-width');
        expect((await metrics(page)).page).toBeCloseTo(A4_WIDTH, 1);
        await leaveFullscreen(page);

        await page.locator('vaadin-ckeditor#ckeditor-2 .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
        await expect(page.locator(PAGE)).toBeVisible();
        const m = await metrics(page);
        expect(m.page).toBeCloseTo(m.column, 0);
        await expect(page.locator('vaadin-ckeditor#ckeditor')).toHaveAttribute('data-ck-fullscreen-page', 'a4');
        await expect(page.locator('vaadin-ckeditor#ckeditor-2')).toHaveAttribute('data-ck-fullscreen-page', 'full-width');
    });

    test('setFullscreenPage at runtime updates an open fullscreen page (#156)', async ({ page }) => {
        await enterFullscreen(page, '?page=full-width');
        // 全屏容器盖住了按钮，直接触发点击
        await page.locator('#set-a4').evaluate((button: HTMLElement) => button.click());
        await expect.poll(async () => (await metrics(page)).page).toBeCloseTo(A4_WIDTH, 1);
    });

    test('the html --ck-fullscreen-editable-width override wins over the editor setting (#156)', async ({ page }) => {
        await enterFullscreen(page, '?page=full-width&width=1200px');
        expect((await metrics(page)).page).toBeCloseTo(1200, 0);
        await leaveFullscreen(page);

        await enterFullscreen(page, '?width=1200px');
        const m = await metrics(page);
        expect(m.page).toBeCloseTo(1200, 0);
        // A4_FIT 的比例随覆盖后的宽度走
        expect(m.minHeight / m.page).toBeCloseTo(297 / 210, 2);
    });

    test('the setting does not affect the editor outside fullscreen (#156)', async ({ page }) => {
        const inlineWidth = async (query: string) => {
            await page.goto(`/fullscreen${query}`);
            await waitForCKEditorReady(page);
            return page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable').evaluate((el) => el.getBoundingClientRect().width);
        };
        const reference = await inlineWidth('?page=a4');
        expect(await inlineWidth('?page=full-width&width=1200px')).toBeCloseTo(reference, 0);
        expect(await inlineWidth('')).toBeCloseTo(reference, 0);
    });
});
