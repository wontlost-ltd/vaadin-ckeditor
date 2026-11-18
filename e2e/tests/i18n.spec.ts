import { expect, test } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 界面语言（issue #151）：此前只打包了 10 种语言，"sk" 等语言静默回退为英文；
 * withConfig 中的 config.setLanguage(...) 还会被构建器的默认 "en" 覆盖。
 * 夹具见 examples/spring-boot-sample 的 I18nView（/i18n）。
 */
test.describe('UI language (issue #151)', () => {
    const cases = [
        { query: 'lang=sk', bold: 'Tučné', contentLang: 'sk' },
        { query: 'lang=sk-SK', bold: 'Tučné', contentLang: 'sk-sk' },
        { query: 'locale=sk-SK', bold: 'Tučné', contentLang: 'sk-sk' },
        { query: 'ui=sk&content=sk', bold: 'Tučné', contentLang: 'sk' },
        { query: 'lang=sr-latn', bold: 'Podebljano', contentLang: 'sr-latn' },
        { query: 'lang=de', bold: 'Fett', contentLang: 'de' },
        { query: '', bold: 'Bold', contentLang: 'en' },
    ];

    for (const { query, bold, contentLang } of cases) {
        test(`/i18n?${query} translates the toolbar`, async ({ page }) => {
            await page.goto(`/i18n?${query}`);
            const editable = await waitForCKEditorReady(page);

            const tooltips = await page.locator('vaadin-ckeditor .ck-toolbar .ck-button')
                .evaluateAll((buttons) => buttons.map((b) => b.getAttribute('data-cke-tooltip-text') ?? ''));
            expect(tooltips.some((text) => text.startsWith(bold)), tooltips.join(' | ')).toBe(true);
            await expect(editable).toHaveAttribute('lang', contentLang);
        });
    }
});
