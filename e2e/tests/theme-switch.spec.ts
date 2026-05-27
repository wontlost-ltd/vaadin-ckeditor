import { expect, test } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

test.describe('Dark theme + CKEditor 48 AI tokens', () => {
    test('dark route applies theme attribute and injects v48 AI token', async ({ page }) => {
        await page.goto('/dark');
        await waitForCKEditorReady(page);

        // Lumo dark attribute applied on documentElement
        const themeAttr = await page.evaluate(() => document.documentElement.getAttribute('theme'));
        expect(themeAttr).toContain('dark');

        // theme-manager injects v48 --ck-color-ai-* tokens on the editor host
        const buttonBg = await page.evaluate(() => {
            const host = document.querySelector('vaadin-ckeditor#ckeditor');
            if (!host) return '';
            return getComputedStyle(host).getPropertyValue('--ck-color-ai-chat-primary-button-background');
        });
        expect(buttonBg.trim()).not.toBe('');
    });
});
