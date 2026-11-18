import { expect, test, type Locator, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * Lumo 明暗主题下的交互态与书签对比度（issues #149 / #150）。
 * 夹具 /theme-showcase 加载真实 Lumo 样式表，编辑器使用默认 AUTO 主题跟随 Vaadin。
 */

/** WCAG 对比度；前景 / 背景中的半透明颜色按给定的不透明底色合成。 */
async function contrastOf(locator: Locator, foreground: string, background: string, base: string): Promise<number> {
    return locator.evaluate((_el, [fg, bg, under]) => {
        const parse = (value: string): number[] => {
            const nums = value.match(/[\d.]+/g)!.map(Number);
            return [nums[0] / 255, nums[1] / 255, nums[2] / 255, nums.length > 3 ? nums[3] : 1];
        };
        const composite = (top: number[], bottom: number[]) =>
            top.slice(0, 3).map((c, i) => c * top[3] + bottom[i] * (1 - top[3]));
        const luminance = (rgb: number[]) => {
            const [r, g, b] = rgb.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
            return 0.2126 * r + 0.7152 * g + 0.0722 * b;
        };
        const baseRgb = parse(under);
        const bgRgb = composite(parse(bg), baseRgb);
        const fgRgb = composite(parse(fg), bgRgb);
        const [hi, lo] = [luminance(fgRgb), luminance(bgRgb)].sort((a, b) => b - a);
        return (hi + 0.05) / (lo + 0.05);
    }, [foreground, background, base] as const);
}

async function computed(locator: Locator, property: string): Promise<string> {
    return locator.evaluate((el, prop) => getComputedStyle(el).getPropertyValue(prop), property);
}

/** 等待 fill 过渡结束：连续两次读取一致即视为稳定。 */
async function settledFill(fill: Locator): Promise<string> {
    let previous = '';
    await expect.poll(async () => {
        const current = await computed(fill, 'fill');
        const stable = current === previous;
        previous = current;
        return stable;
    }, { intervals: [100] }).toBe(true);
    return previous;
}

async function open(page: Page, theme: 'light' | 'dark'): Promise<Locator> {
    await page.goto(theme === 'dark' ? '/theme-showcase?theme=dark' : '/theme-showcase');
    const editable = await waitForCKEditorReady(page);
    await expect(page.locator('vaadin-ckeditor#ckeditor')).toHaveAttribute('data-ck-theme', theme);
    return editable;
}

for (const theme of ['light', 'dark'] as const) {
    test.describe(`${theme} theme`, () => {
        test('insertImage split button: both halves use a matching hover surface (issue #150)', async ({ page }) => {
            await open(page, theme);
            const toolbar = page.locator('vaadin-ckeditor .ck.ck-toolbar').first();
            const split = page.locator('vaadin-ckeditor .ck-toolbar .ck-splitbutton').first();
            const action = split.locator('.ck-splitbutton__action');
            const arrow = split.locator('.ck-splitbutton__arrow');

            await action.hover();
            const toolbarBg = await computed(toolbar, 'background-color');
            const actionBg = await computed(action, 'background-color');
            const arrowBg = await computed(arrow, 'background-color');
            // 旧缺陷：暗色下箭头一半回退到 CKEditor 49 的亮色 #f3f4f7
            expect(arrowBg).toBe(actionBg);

            const arrowIcon = arrow.locator('.ck-icon').first();
            const iconColor = await computed(arrowIcon, 'color');
            // 非文本 UI 元素（图标）WCAG AA ≥ 3:1
            expect(await contrastOf(arrow, iconColor, arrowBg, toolbarBg)).toBeGreaterThanOrEqual(3);
        });

        test('active toolbar button keeps its active state on hover and meets AA', async ({ page }) => {
            const editable = await open(page, theme);
            const bold = page.locator('vaadin-ckeditor .ck-toolbar .ck-button[data-cke-tooltip-text^="Bold"]').first();
            await editable.click();
            await bold.click();
            await expect(bold).toHaveClass(/ck-on/);

            const toolbarBg = await computed(page.locator('vaadin-ckeditor .ck.ck-toolbar').first(), 'background-color');
            const italic = page.locator('vaadin-ckeditor .ck-toolbar .ck-button[data-cke-tooltip-text^="Italic"]').first();
            await italic.hover();
            const inactiveHoverBg = await computed(italic, 'background-color');
            await page.mouse.move(0, 0);
            const idleBg = await computed(bold, 'background-color');
            await bold.hover();
            const hoverBg = await computed(bold, 'background-color');
            const iconColor = await computed(bold.locator('.ck-icon').first(), 'color');

            // 悬停不能把激活态底色还原成普通按钮的悬停底色，否则指针移上去激活态就看不出来
            expect(hoverBg).not.toBe(inactiveHoverBg);
            expect(await contrastOf(bold, iconColor, idleBg, toolbarBg)).toBeGreaterThanOrEqual(3);
            expect(await contrastOf(bold, iconColor, hoverBg, toolbarBg)).toBeGreaterThanOrEqual(3);
        });

        test('bookmark icon is clearly visible and keeps hover/selected colours (issue #149)', async ({ page }) => {
            const editable = await open(page, theme);
            await page.mouse.move(0, 0);
            const bookmark = editable.locator('.ck-bookmark').first();
            const fill = bookmark.locator('.ck-icon__fill').first();
            const editableBg = await computed(editable, 'background-color');

            // 图标 fill 带 CKEditor 的过渡动画，轮询到稳定值再断言
            const idleFill = await settledFill(fill);
            expect(await contrastOf(fill, idleFill, editableBg, editableBg)).toBeGreaterThanOrEqual(3);

            await bookmark.click();
            await expect(bookmark).toHaveClass(/ck-widget_selected/);
            const expectedSelected = await page.evaluate(() => {
                const probe = document.createElement('div');
                probe.style.color = 'var(--ck-bookmark-icon-selected-fill-color)';
                document.querySelector('vaadin-ckeditor .ck-content')!.appendChild(probe);
                const value = getComputedStyle(probe).color;
                probe.remove();
                return value;
            });
            await expect.poll(() => computed(fill, 'fill')).toBe(expectedSelected);
            const selectedFill = await computed(fill, 'fill');
            expect(await contrastOf(fill, selectedFill, editableBg, editableBg)).toBeGreaterThanOrEqual(3);
        });
    });
}

test('bookmark icon colour is configurable via --ck-bookmark-icon-fill-color', async ({ page }) => {
    const editable = await open(page, 'dark');
    await page.addStyleTag({ content: 'vaadin-ckeditor { --ck-bookmark-icon-fill-color: rgb(255, 0, 0); }' });
    await page.mouse.move(0, 0);
    const fill = editable.locator('.ck-bookmark .ck-icon__fill').first();
    await expect.poll(() => computed(fill, 'fill')).toBe('rgb(255, 0, 0)');
});
