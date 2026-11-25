import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * Full page HTML：`CKEditorPlugin.FULL_PAGE` 与 `HtmlSupport.fullPageAllowRenderStylesFromHead`（#169 时无法加载 FullPage，
 * 该选项无效）。夹具 /full-page，见 FullPageView。
 */

interface CKHost extends HTMLElement {
    editor: { getData(): string; plugins: { has(name: string): boolean } };
}

async function hasPlugin(page: Page, id: string, name: string): Promise<boolean> {
    return page.evaluate(([sel, plugin]) => (document.querySelector(sel) as CKHost).editor.plugins.has(plugin),
        [`vaadin-ckeditor#${id}`, name] as const);
}

async function data(page: Page, id: string): Promise<string> {
    return page.evaluate((sel) => (document.querySelector(sel) as CKHost).editor.getData(), `vaadin-ckeditor#${id}`);
}

async function open(page: Page) {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await page.goto('/full-page');
    await waitForCKEditorReady(page);
    await expect(page.locator('vaadin-ckeditor#ck-explicit .ck-editor__editable')).toBeVisible();
    return errors;
}

test('the fullPage option loads FullPage: <head> is kept and its styles are rendered', async ({ page }) => {
    const errors = await open(page);
    expect(await hasPlugin(page, 'ckeditor', 'FullPage')).toBe(true);
    const html = await data(page, 'ckeditor');
    expect(html).toContain('<title>Doc title</title>');
    expect(html).toContain('p.fp { color: rgb(1, 2, 3); }');
    expect(html).toContain('<p class="fp">Body</p>');
    await expect(page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable p.fp')).toHaveCSS('color', 'rgb(1, 2, 3)');
    expect(errors).toEqual([]);
});

test('the server value keeps the whole document after editing', async ({ page }) => {
    await open(page);
    await page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable p.fp').click();
    await page.keyboard.press('End');
    await page.keyboard.type(' edited');
    await expect(page.locator('#server-value')).toContainText('<p class="fp">Body edited</p>');
    await expect(page.locator('#server-value')).toContainText('<title>Doc title</title>');
});

test('CKEditorPlugin.FULL_PAGE without the option keeps <head> but does not render its styles', async ({ page }) => {
    const errors = await open(page);
    expect(await hasPlugin(page, 'ck-explicit', 'FullPage')).toBe(true);
    const html = await data(page, 'ck-explicit');
    expect(html).toContain('<title>Other</title>');
    expect(html).toContain('<p class="fp2">Second</p>');
    await expect(page.locator('vaadin-ckeditor#ck-explicit .ck-editor__editable p.fp2')).not.toHaveCSS('color', 'rgb(4, 5, 6)');
    expect(errors).toEqual([]);
});
