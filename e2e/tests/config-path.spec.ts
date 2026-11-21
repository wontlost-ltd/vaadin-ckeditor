import { expect, test } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * `CKEditorConfig.set(path, value)`：/config-path 夹具只用点号路径与类型化重载配置编辑器。
 * 验证 CKEditor 实际读到的是嵌套对象（两次 toolbar.* 调用合并、ui.viewportOffset 合并进类型化 setter 的对象），
 * 且配置真的生效（工具栏只含给定按钮、标题下拉为给定选项）。
 */

interface CKHost extends HTMLElement {
    editor: { config: { get(path: string): unknown } };
}

test('dotted config paths reach CKEditor as nested objects', async ({ page }) => {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await page.goto('/config-path');
    await waitForCKEditorReady(page);

    const read = await page.evaluate(() => {
        const config = (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost).editor.config;
        return {
            toolbar: config.get('toolbar'),
            link: config.get('link'),
            viewportOffset: config.get('ui.viewportOffset'),
            headingTitles: (config.get('heading.options') as Array<{ title: string }>).map((o) => o.title),
        };
    });

    expect(read.toolbar).toEqual({
        items: ['heading', '|', 'bold', 'italic', 'link'],
        shouldNotGroupWhenFull: true,
    });
    expect(read.link).toMatchObject({ defaultProtocol: 'https://', addTargetToExternalLinks: true });
    expect(read.viewportOffset).toEqual({ top: 12, right: 0, bottom: 0, left: 0 });
    expect(read.headingTitles).toEqual(['Body', 'Section']);

    // 配置确实生效：工具栏只有给定的 4 个按钮（heading 下拉 + 3 个按钮），标题下拉为给定选项
    const toolbar = page.locator('vaadin-ckeditor#ckeditor .ck-toolbar__items');
    await expect(toolbar.locator('> .ck-dropdown, > .ck-button')).toHaveCount(4);
    await toolbar.locator('.ck-heading-dropdown .ck-dropdown__button').click();
    await expect(page.locator('.ck-heading-dropdown .ck-dropdown__panel .ck-button__label'))
        .toHaveText(['Body', 'Section']);
    expect(errors).toEqual([]);
});
