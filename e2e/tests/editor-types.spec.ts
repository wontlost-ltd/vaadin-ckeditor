import { expect, test } from '@playwright/test';
import { readEditableHtml, waitForCKEditorReady, waitForInlineEditorReady } from '../helpers/ck';

test.describe('4 editor types initialize against CKEditor 48', () => {
    test('classic mounts and seeds initialData', async ({ page }) => {
        await page.goto('/classic');
        const editable = await waitForCKEditorReady(page);
        expect(await readEditableHtml(page, editable)).toContain('Hello from Classic');
    });

    test('balloon mounts and seeds initialData', async ({ page }) => {
        await page.goto('/balloon');
        const editable = await waitForInlineEditorReady(page);
        expect(await readEditableHtml(page, editable)).toContain('Hello from Balloon');
    });

    test('inline mounts and seeds initialData', async ({ page }) => {
        await page.goto('/inline');
        const editable = await waitForInlineEditorReady(page);
        expect(await readEditableHtml(page, editable)).toContain('Hello from Inline');
    });

    test('decoupled mounts and seeds initialData', async ({ page }) => {
        await page.goto('/decoupled');
        const editable = await waitForInlineEditorReady(page);
        expect(await readEditableHtml(page, editable)).toContain('Hello from Decoupled');
    });
});
