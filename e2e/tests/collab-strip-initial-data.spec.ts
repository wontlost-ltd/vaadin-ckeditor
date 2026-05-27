import { expect, test } from '@playwright/test';
import { waitForEditorHost } from '../helpers/ck';

const STORAGE_KEY = 'ck-channel-seeded:playwright-channel';

/**
 * Exercises stripInitialDataIfChannelSeeded by hitting /collab-seed twice and
 * checking the localStorage seed key. The first visit should set the key;
 * subsequent visits must see it already set.
 *
 * Note: cloudServices.tokenUrl is intentionally invalid, so CKEditor itself
 * will fail to open a real channel. The normalizer runs before that, so we
 * only assert on the localStorage side-effect.
 */
test('stripInitialDataIfChannelSeeded seeds localStorage on first visit', async ({ page }) => {
    // Hit the route first to obtain a real origin where localStorage is accessible
    await page.goto('/collab-seed');
    await waitForEditorHost(page);
    await page.evaluate((key) => localStorage.removeItem(key), STORAGE_KEY);

    // Reload so editor creation runs again with the cleared storage
    await page.reload();
    await waitForEditorHost(page);
    await page.waitForTimeout(2_000);

    const stored = await page.evaluate((key) => localStorage.getItem(key), STORAGE_KEY);
    expect(stored).not.toBeNull();
    expect(Number(stored)).toBeGreaterThan(1_700_000_000_000);
});
