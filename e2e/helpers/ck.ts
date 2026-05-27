import type { Locator, Page } from '@playwright/test';

/**
 * Helpers for locating CKEditor 5 elements rendered inside the Vaadin Light DOM.
 *
 * VaadinCKEditor uses Light DOM (no shadow root), so standard query selectors
 * work but CKEditor mounts asynchronously after Lit hydration. These helpers
 * wait for the editor host element and then for CKEditor's own surface.
 */

const HOST_SELECTOR = 'vaadin-ckeditor#ckeditor';
const EDITABLE_SELECTOR = '.ck-editor__main .ck-content';

/**
 * Wait until the VaadinCKEditor web component is connected to the DOM.
 */
export async function waitForEditorHost(page: Page): Promise<Locator> {
    const host = page.locator(HOST_SELECTOR);
    await host.waitFor({ state: 'attached', timeout: 30_000 });
    return host;
}

/**
 * Wait until CKEditor has finished initialising and the editable surface is
 * present and visible. Returns the locator for the editable area.
 */
export async function waitForCKEditorReady(page: Page): Promise<Locator> {
    await waitForEditorHost(page);
    const editable = page.locator(EDITABLE_SELECTOR).first();
    await editable.waitFor({ state: 'visible', timeout: 30_000 });
    return editable;
}

/**
 * Inline-editor variant: the editable surface uses `.ck-editor__editable` directly,
 * without the `.ck-editor__main` wrapper that classic editors provide.
 */
export async function waitForInlineEditorReady(page: Page): Promise<Locator> {
    await waitForEditorHost(page);
    const editable = page.locator('.ck-editor__editable').first();
    await editable.waitFor({ state: 'visible', timeout: 30_000 });
    return editable;
}

/**
 * Read the rendered HTML of the editable surface. Useful for verifying
 * round-trip of initialData / placeholder migration.
 */
export async function readEditableHtml(page: Page, editable?: Locator): Promise<string> {
    const target = editable ?? page.locator(EDITABLE_SELECTOR).first();
    return target.innerHTML();
}
