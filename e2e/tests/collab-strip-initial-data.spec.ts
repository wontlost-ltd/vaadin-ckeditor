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
test.describe('stripInitialDataIfChannelSeeded', () => {
    test('writes localStorage seed key on first visit (unseeded branch)', async ({ page }) => {
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

    test('strip path: pre-seeded localStorage causes initialData to be removed (seeded branch)', async ({ page }) => {
        await page.goto('/collab-seed');
        await waitForEditorHost(page);

        // Pre-seed the channel with a marker so the strip branch will fire on next load.
        const seededMarker = '1700000000001';
        await page.evaluate(
            ({ key, value }) => localStorage.setItem(key, value),
            { key: STORAGE_KEY, value: seededMarker }
        );

        // Capture warn-level migration messages so we can assert the strip ran.
        const channelLogs: string[] = [];
        page.on('console', (msg) => {
            const text = msg.text();
            if (text.includes('已初始化') || text.includes('移除 initialData') || text.includes('playwright-channel')) {
                channelLogs.push(text);
            }
        });

        await page.reload();
        await waitForEditorHost(page);
        await page.waitForTimeout(2_000);

        // 1. The seed key must still be present (we did not touch it after seeding).
        const after = await page.evaluate((key) => localStorage.getItem(key), STORAGE_KEY);
        expect(after).toBe(seededMarker);

        // 2. The Java code logs "频道 xxx 已初始化，移除 initialData 避免冲突" via console.info.
        //    Console message capture can race with page reload, so poll briefly.
        await expect.poll(() => channelLogs.length, { timeout: 5_000 }).toBeGreaterThan(0);
        expect(channelLogs.some((msg) => /移除 initialData|playwright-channel/.test(msg))).toBe(true);
    });
});
