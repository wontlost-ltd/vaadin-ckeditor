import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 全屏编辑区宽度（issue #156）。CKEditor 全屏模式把编辑区固定为 A4 纸宽 795.701px
 * （ckeditor5-fullscreen theme/fullscreen.css），没有配置项；add-on 开放
 * --ck-fullscreen-editable-width，未设置时保持 CKEditor 默认。
 */

const CKEDITOR_DEFAULT_WIDTH = 795.701;

async function enterFullscreen(page: Page, query = ''): Promise<void> {
    await page.setViewportSize({ width: 1600, height: 900 });
    await page.goto(`/fullscreen${query}`);
    await waitForCKEditorReady(page);
    await page.locator('vaadin-ckeditor#ckeditor .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
    await expect(page.locator('.ck-fullscreen__main-wrapper .ck-editor__editable')).toBeVisible();
}

async function widthOf(page: Page, selector: string): Promise<number> {
    return page.locator(selector).first().evaluate((el) => el.getBoundingClientRect().width);
}

test('fullscreen keeps the CKEditor page width when the variable is not set (#156)', async ({ page }) => {
    await enterFullscreen(page);
    expect(await widthOf(page, '.ck-fullscreen__main-wrapper .ck-editor__editable'))
        .toBeCloseTo(CKEDITOR_DEFAULT_WIDTH, 1);
});

for (const [value, expected] of [['1200px', 1200], ['90vw', 1440]] as const) {
    test(`--ck-fullscreen-editable-width: ${value} widens the fullscreen editable and source view (#156)`, async ({ page }) => {
        await enterFullscreen(page, `?width=${value}`);
        expect(await widthOf(page, '.ck-fullscreen__main-wrapper .ck-editor__editable')).toBeCloseTo(expected, 0);

        await page.locator('.ck-fullscreen__main-wrapper .ck-toolbar .ck-source-editing-button').click();
        await expect(page.locator('.ck-fullscreen__main-wrapper .ck-source-editing-area')).toBeVisible();
        expect(await widthOf(page, '.ck-fullscreen__main-wrapper .ck-source-editing-area')).toBeCloseTo(expected, 0);
    });
}

test('the variable does not affect the editor outside fullscreen (#156)', async ({ page }) => {
    await page.setViewportSize({ width: 1600, height: 900 });
    const inlineWidth = async (query: string) => {
        await page.goto(`/fullscreen${query}`);
        await waitForCKEditorReady(page);
        return widthOf(page, 'vaadin-ckeditor#ckeditor .ck-editor__editable');
    };
    expect(await inlineWidth('?width=1200px')).toBeCloseTo(await inlineWidth(''), 0);
});
