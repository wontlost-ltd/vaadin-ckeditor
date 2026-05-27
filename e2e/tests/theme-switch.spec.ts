import { expect, test } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

test.describe('Dark theme + CKEditor 48 AI tokens', () => {
    test('dark route applies theme attribute and injects v48 AI token', async ({ page }) => {
        await page.goto('/dark');
        await waitForCKEditorReady(page);

        // Lumo dark attribute applied on documentElement
        const themeAttr = await page.evaluate(() => document.documentElement.getAttribute('theme'));
        expect(themeAttr).toContain('dark');

        // theme-manager sets v48 --ck-color-ai-* tokens via inline style on documentElement.
        // initDarkTheme() runs after the editor is constructed; poll briefly.
        await expect.poll(async () => {
            return page.evaluate(() =>
                document.documentElement.style.getPropertyValue('--ck-color-ai-chat-primary-button-background').trim()
            );
        }, { timeout: 5_000, message: 'v48 AI dark token never appeared on <html>' }).not.toBe('');

        // Once the primary token is set, the rest of the v48 batch should be there too.
        const tokens = await page.evaluate(() => {
            const html = document.documentElement;
            return {
                primaryButtonBg: html.style.getPropertyValue('--ck-color-ai-chat-primary-button-background'),
                inputBg: html.style.getPropertyValue('--ck-color-ai-chat-input-background'),
                errorBg: html.style.getPropertyValue('--ck-color-ai-notification-error-background'),
            };
        });
        expect(tokens.inputBg.trim()).not.toBe('');
        expect(tokens.errorBg.trim()).not.toBe('');
    });
});
