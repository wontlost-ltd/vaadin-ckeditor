import { expect, test } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

test.describe('Upload adapter wires to StubUploadHandler', () => {
    test('image upload via toolbar inserts an <img> with the stub data URL', async ({ page }) => {
        await page.goto('/upload');
        await waitForCKEditorReady(page);

        // Smallest valid 1x1 transparent PNG (8-byte signature + IHDR + IDAT + IEND).
        // Used purely as fixture bytes — the assertion only checks the resulting
        // data: URL prefix.
        const pngBytes = Buffer.from(
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgAAIAAAUAAeImBZsAAAAASUVORK5CYII=',
            'base64'
        );

        // Set up a one-shot file chooser handler before clicking the toolbar button.
        // .ck-file-dialog-button is CKEditor's stable class for "open native file picker"
        // (applied directly to the <button>, not a wrapper).
        const chooserPromise = page.waitForEvent('filechooser');
        await page.locator('.ck-toolbar button.ck-file-dialog-button').first().click();

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
