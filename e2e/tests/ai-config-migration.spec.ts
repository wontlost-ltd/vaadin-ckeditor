import { expect, test } from '@playwright/test';
import { readEditableHtml, waitForCKEditorReady } from '../helpers/ck';

/**
 * v47-style top-level `initialData` (declared via VaadinCKEditorBuilder.withValue
 * in ClassicView) must reach the editor through the v48 root config path. This
 * implicitly validates editor-config-normalizer.normalizeRootConfig at runtime.
 */
test('top-level initialData migrates to root.initialData and renders', async ({ page }) => {
    await page.goto('/classic');
    const editable = await waitForCKEditorReady(page);

    const html = await readEditableHtml(page, editable);
    expect(html).toContain('Hello from Classic');

    // Confirm CKEditor 48 is actually loaded (sanity check on the new bundle)
    const ckGlobalReady = await page.evaluate(() => {
        const host = document.querySelector('vaadin-ckeditor#ckeditor') as HTMLElement & {
            editor?: { state?: string };
        };
        return host?.editor?.state === 'ready';
    });
    expect(ckGlobalReady).toBe(true);
});
