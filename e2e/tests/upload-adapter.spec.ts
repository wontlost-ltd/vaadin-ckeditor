import { expect, test } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

test.describe('Upload adapter wires to StubUploadHandler', () => {
    test('image upload via toolbar inserts an <img> with the stub data URL', async ({ page }) => {
        await page.goto('/upload');
        await waitForCKEditorReady(page);

        // Tiny 1x1 PNG fixture (transparent)
        const pngBytes = Buffer.from(
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgAAIAAAUAAeImBZsAAAAASUVORK5CYII=',
            'base64'
        );

        // Set up a one-shot file chooser handler before clicking the toolbar button
        const chooserPromise = page.waitForEvent('filechooser');
        await page.locator('.ck-toolbar button[data-cke-tooltip-text*="image" i], .ck-toolbar .ck-file-dialog-button button')
            .first()
            .click();

        const chooser = await chooserPromise;
        await chooser.setFiles({
            name: 'pixel.png',
            mimeType: 'image/png',
            buffer: pngBytes,
        });

        // Wait for an <img src="data:image/png..."> to appear in the editable
        const insertedImg = page.locator('.ck-content img[src^="data:image/png"]');
        await expect(insertedImg).toBeVisible({ timeout: 15_000 });
    });
});
