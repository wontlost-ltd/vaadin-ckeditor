import { expect, test, type Page } from '@playwright/test';

/**
 * 全屏按钮与编辑器类型。CKEditor 5 49.1.0 的 FullscreenCommand 只为 ClassicEditor / DecoupledEditor
 * 创建处理器，其它编辑器用 FullscreenAbstractEditorHandler，其 defaultOnEnter() 不移动任何元素：
 * BALLOON / INLINE 上点按钮或按 Ctrl+Shift+F 会隐藏整页、只剩空的全屏容器。
 * add-on 对 BALLOON / INLINE 不加载 Fullscreen 插件、移除 fullscreen 工具栏项；CLASSIC / DECOUPLED 不变。
 */

const VIAS = ['preset', 'explicit', 'config'] as const;

interface CKHost extends HTMLElement {
    editor?: {
        state: string;
        plugins: { has(name: string): boolean };
        commands: { get(name: string): unknown };
        model: { change(cb: (w: { setSelection(el: unknown, where: string): void }) => void): void;
            document: { getRoot(): { getChild(i: number): unknown } } };
        editing: { view: { focus(): void } };
    };
}

async function open(page: Page, type: string, via: string): Promise<string[]> {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await page.goto(`/fullscreen-types?type=${type}&via=${via}`);
    await page.waitForFunction(() => (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost)?.editor?.state === 'ready',
        null, { timeout: 30_000 });
    return errors;
}

/** 让各类型的工具栏出现：classic / decoupled 常驻；inline 聚焦后出现；balloon 选中文字后出现 */
async function revealToolbar(page: Page): Promise<void> {
    await page.evaluate(() => {
        const editor = (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost).editor!;
        editor.editing.view.focus();
        editor.model.change((w) => w.setSelection(editor.model.document.getRoot().getChild(0), 'in'));
    });
    await expect(page.locator('.ck-toolbar:visible').first()).toBeVisible();
}

const fullscreenButton = (page: Page) => page.locator('.ck-toolbar button[data-cke-tooltip-text*="ull"]:visible');

for (const via of VIAS) {
    for (const type of ['balloon', 'inline'] as const) {
        test(`${type} via ${via}: no fullscreen button, plugin or keystroke`, async ({ page }) => {
            const errors = await open(page, type, via);
            await revealToolbar(page);
            await expect(page.locator('.ck-toolbar:visible .ck-button').first()).toBeVisible();
            await expect(fullscreenButton(page)).toHaveCount(0);
            expect(await page.evaluate(() => {
                const editor = (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost).editor!;
                return { plugin: editor.plugins.has('Fullscreen'), command: !!editor.commands.get('toggleFullscreen') };
            })).toEqual({ plugin: false, command: false });
            // CKEditor 的全屏快捷键也不应把页面切成空的全屏容器
            await page.keyboard.press('Control+Shift+F');
            await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(0);
            await expect(page.locator('vaadin-ckeditor#ckeditor')).toBeVisible();
            expect(errors).toEqual([]);
        });
    }

    for (const type of ['classic', 'decoupled'] as const) {
        test(`${type} via ${via}: fullscreen button still works`, async ({ page }) => {
            const errors = await open(page, type, via);
            await revealToolbar(page);
            await fullscreenButton(page).first().click();
            await expect(page.locator('.ck-fullscreen__main-wrapper .ck-editor__editable')).toBeVisible();
            await page.locator('.ck-fullscreen__main-wrapper .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
            await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(0);
            expect(errors).toEqual([]);
        });
    }
}

/** 菜单栏「视图」菜单中的全屏项：CKEditor 默认菜单栏结构包含 menuBar:fullscreen */
async function menuBarHasFullscreen(page: Page): Promise<boolean> {
    const viewMenu = page.locator('.ck-menu-bar .ck-menu-bar__menu > .ck-menu-bar__menu__button').filter({ hasText: /^View$/ });
    await viewMenu.click();
    const items = page.locator('.ck-menu-bar__menu__panel .ck-menu-bar__menu__item .ck-button__label');
    await expect(items.first()).toBeVisible();
    return (await items.allTextContents()).some((label) => /full\s*screen/i.test(label));
}

for (const [type, expected] of [['inline', false], ['balloon', false], ['classic', true], ['decoupled', true]] as const) {
    test(`${type} with menu bar: fullscreen menu item ${expected ? 'kept' : 'not registered'}`, async ({ page }) => {
        const errors: string[] = [];
        page.on('pageerror', (e) => errors.push(e.message));
        await page.goto(`/fullscreen-types?type=${type}&via=explicit&menubar=1`);
        await page.waitForFunction(() => (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost)?.editor?.state === 'ready',
            null, { timeout: 30_000 });
        // 菜单栏与工具栏都只能渲染组件工厂里注册过的组件：插件不加载，menuBar:fullscreen / fullscreen 就不存在
        expect(await page.evaluate(() => {
            const factory = ((document.querySelector('vaadin-ckeditor#ckeditor') as CKHost).editor as unknown as
                { ui: { componentFactory: { has(name: string): boolean } } }).ui.componentFactory;
            return [factory.has('menuBar:fullscreen'), factory.has('fullscreen')];
        })).toEqual([expected, expected]);
        if (expected) {
            // 支持的类型：菜单栏「View」菜单里仍有全屏项
            await revealToolbar(page);
            expect(await menuBarHasFullscreen(page)).toBe(true);
        }
        expect(errors).toEqual([]);
    });
}
