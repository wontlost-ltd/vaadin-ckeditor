import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * General HTML Support（HtmlSupport 构建器与 setHtmlSupport(true)）。夹具 /html-support，见 HtmlSupportView。
 */

interface CKHost extends HTMLElement {
    editor: { getData(): string; plugins: { has(name: string): boolean } };
}

async function data(page: Page, id: string): Promise<string> {
    return page.evaluate((sel) => (document.querySelector(sel) as CKHost).editor.getData(), `vaadin-ckeditor#${id}`);
}

async function open(page: Page) {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await page.goto('/html-support');
    await waitForCKEditorReady(page);
    await expect(page.locator('vaadin-ckeditor#ckeditor-all .ck-editor__editable')).toBeVisible();
    return errors;
}

test('rules keep details, section classes, styles and allowed iframes; drop the rest', async ({ page }) => {
    const errors = await open(page);
    const html = await data(page, 'ckeditor');

    expect(html).toContain('<section class="intro"><p class="lead" style="color:#336699;">Lead</p></section>');
    expect(html).toContain('<details open=""><summary>More</summary><p>Hidden text</p></details>');
    expect(html).toContain('src="https://www.youtube.com/embed/abc"');
    expect(html).toContain('width="560"');
    expect(html).toContain('<div class="box">Box</div>');
    // 不匹配 src 模式的 iframe 失去 src；on* 与 script 被默认 disallow
    expect(html).not.toContain('evil.example');
    expect(html).not.toContain('onclick');
    expect(html).not.toContain('<script');
    expect(await page.evaluate(() => (window as unknown as { ghsScriptRan?: boolean }).ghsScriptRan)).toBeUndefined();
    expect(await page.evaluate(() => (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost)
        .editor.plugins.has('GeneralHtmlSupport'))).toBe(true);
    expect(errors).toEqual([]);
});

test('details round-trips through the server value after editing', async ({ page }) => {
    await open(page);
    await page.locator('vaadin-ckeditor#ckeditor .ck-editor__editable summary').click();
    await page.keyboard.press('End');
    await page.keyboard.type(' info');
    await expect(page.locator('#server-value')).toContainText('<details open=""><summary>More info</summary>');
    await expect(page.locator('#server-value')).toContainText('<section class="intro">');
});

test('setHtmlSupport(true) now allows all HTML except scripts, iframes and event handlers', async ({ page }) => {
    await open(page);
    const html = await data(page, 'ckeditor-all');
    expect(html).toContain('class="x"');
    expect(html).toContain('data-foo="1"');
    expect(html).not.toContain('onmouseover');
    expect(html).not.toContain('<iframe');
    expect(html).not.toContain('<script');
});
