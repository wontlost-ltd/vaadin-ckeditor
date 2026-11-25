import { expect, test, type Page } from '@playwright/test';

/**
 * `CustomPlugin.fromCKEditor5(name)` / `CustomPlugin.of(name, "ckeditor5")` 加载 `ckeditor5` 包中不在连接器静态插件表里的
 * 导出（这里是 `Font`），在 production jar（Vite 生产构建）中验证。夹具 /ckeditor5-export，见 CKEditor5ExportView。
 */

interface CKHost extends HTMLElement {
    editor?: { plugins: { has(name: string): boolean } };
}

async function hasPlugin(page: Page, id: string, name: string): Promise<boolean> {
    return page.evaluate(([sel, plugin]) =>
        (document.querySelector(sel) as CKHost).editor!.plugins.has(plugin), [`vaadin-ckeditor#${id}`, name] as const);
}

async function open(page: Page) {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await page.goto('/ckeditor5-export');
    for (const id of ['ck-from', 'ck-of', 'ck-missing']) {
        await expect(page.locator(`vaadin-ckeditor#${id} .ck-editor__editable`)).toBeVisible({ timeout: 30_000 });
    }
    return errors;
}

for (const id of ['ck-from', 'ck-of']) {
    test(`${id}: a ckeditor5 export outside the static registry loads and its toolbar items work`, async ({ page }) => {
        const errors = await open(page);
        expect(await hasPlugin(page, id, 'Font')).toBe(true);
        expect(await hasPlugin(page, id, 'FontSize')).toBe(true);
        await expect(page.locator(`vaadin-ckeditor#${id} .ck-toolbar .ck-font-size-dropdown`)).toBeVisible();
        await expect(page.locator(`vaadin-ckeditor#${id} .ck-toolbar .ck-color-ui-dropdown`)).toBeVisible();
        expect(errors).toEqual([]);
    });
}

test('an unknown name is skipped and the editor still works', async ({ page }) => {
    const errors = await open(page);
    expect(await hasPlugin(page, 'ck-missing', 'NoSuchPlugin')).toBe(false);
    expect(await hasPlugin(page, 'ck-missing', 'Bold')).toBe(true);
    expect(errors).toEqual([]);
});
