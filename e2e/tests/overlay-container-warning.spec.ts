import { expect, test, type Page } from '@playwright/test';

/**
 * CKEditor 49 在 EditorUI 构造时若发现任意祖先 shadow root 而未配置 ui.overlayContainer，
 * 会告警 ui-overlay-container-not-configured。Vaadin 的布局组件（vaadin-vertical-layout、
 * vaadin-app-layout 等）都带 shadow root，因此任何 Flow 视图都会触发该误报。
 * 连接器为 light DOM 宿主显式指定 document.body（与 CKEditor 自己的选择一致）后不应再出现。
 */
const WARNING = 'ui-overlay-container-not-configured';

function collectWarnings(page: Page): string[] {
    const warnings: string[] = [];
    page.on('console', (msg) => {
        if (msg.type() === 'warning' || msg.type() === 'error') {
            warnings.push(msg.text());
        }
    });
    return warnings;
}

test('a plain VerticalLayout view does not trigger the overlay-container warning', async ({ page }) => {
    const warnings = collectWarnings(page);
    await page.goto('/classic');
    await page.locator('.ck-editor__editable').first().waitFor({ state: 'visible', timeout: 30_000 });
    expect(warnings.filter((w) => w.includes(WARNING))).toEqual([]);
    // 浮层容器仍挂在 document.body 上，与 CKEditor 48 的默认一致
    expect(await page.evaluate(() => document.querySelector('.ck-body-wrapper')?.parentElement?.tagName)).toBe('BODY');
});

test('an editor inside a Vaadin dialog does not trigger the overlay-container warning', async ({ page }) => {
    const warnings = collectWarnings(page);
    await page.goto('/dialog');
    await page.click('#open-dialog');
    await page.locator('vaadin-dialog .ck-editor__editable').first().waitFor({ state: 'visible', timeout: 30_000 });
    expect(warnings.filter((w) => w.includes(WARNING))).toEqual([]);
});
