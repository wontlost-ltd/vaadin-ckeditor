import { test, expect, type Page } from '@playwright/test';
import { waitForCKEditorReady, waitForInlineEditorReady, waitForEditorHost } from '../helpers/ck';

/**
 * Visual regression baselines.
 *
 * <p>Pixel rendering varies by OS, browser font stack, and CPU architecture.
 * Baselines are produced on the CI runner (Linux x64 / Ubuntu Jammy) and the
 * spec is skipped on any other platform/arch to keep local development green
 * without false positives.</p>
 *
 * <p><b>How baselines are produced:</b></p>
 * <ol>
 *   <li>Run the spec in a Linux/amd64 Playwright container with
 *       <code>--update-snapshots</code>. See e2e/README.md for the command.</li>
 *   <li>Commit the generated PNGs under <code>__screenshots__/</code>.</li>
 *   <li>Subsequent CI runs compare against the committed baselines.</li>
 * </ol>
 *
 * <p>The spec is also skipped when <code>SKIP_VISUAL_REGRESSION=1</code> is
 * set, e.g. on first-time setup CI runs that intentionally produce baselines
 * without failing the build.</p>
 */
test.describe('Visual regression', () => {
    // Baselines are produced on Linux x64. Skip on any other platform/arch so the
    // committed PNGs don't false-positive against other OS/architectures.
    test.skip(
        !(process.platform === 'linux' && process.arch === 'x64'),
        'Baselines are pinned to Linux x64 (the CI runner)'
    );
    test.skip(
        process.env.SKIP_VISUAL_REGRESSION === '1',
        'Visual regression intentionally skipped (e.g. during baseline bootstrap)'
    );

    // Sub-images of the editor host (the smallest baseline is ~75×30 px for inline);
    // allow at most 200 differing pixels to absorb sub-pixel AA jitter without
    // accepting a whole-row colour shift. maxDiffPixelRatio kept as a defence in
    // depth for the larger baselines (e.g. decoupled).
    const SCREENSHOT_OPTIONS = {
        animations: 'disabled' as const,
        maxDiffPixels: 200,
        maxDiffPixelRatio: 0.02,
        fullPage: false,
    };

    /**
     * Wait for the page to be visually stable before snapshotting:
     * fonts loaded + two animation frames flushed. Reduces dependence on
     * arbitrary sleeps and the resulting flake.
     */
    async function waitForLayoutStable(page: Page): Promise<void> {
        await page.evaluate(async () => {
            if (document.fonts && typeof document.fonts.ready?.then === 'function') {
                await document.fonts.ready;
            }
            await new Promise<void>((resolve) => requestAnimationFrame(() => requestAnimationFrame(() => resolve())));
        });
    }

    test('classic editor visual baseline', async ({ page }) => {
        await page.goto('/classic');
        await waitForCKEditorReady(page);
        await waitForLayoutStable(page);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('classic.png', SCREENSHOT_OPTIONS);
    });

    test('balloon editor visual baseline', async ({ page }) => {
        await page.goto('/balloon');
        await waitForInlineEditorReady(page);
        await waitForLayoutStable(page);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('balloon.png', SCREENSHOT_OPTIONS);
    });

    test('inline editor visual baseline', async ({ page }) => {
        await page.goto('/inline');
        await waitForInlineEditorReady(page);
        await waitForLayoutStable(page);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('inline.png', SCREENSHOT_OPTIONS);
    });

    test('decoupled editor visual baseline', async ({ page }) => {
        await page.goto('/decoupled');
        await waitForInlineEditorReady(page);
        await waitForLayoutStable(page);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('decoupled.png', SCREENSHOT_OPTIONS);
    });

    test('dark theme visual baseline', async ({ page }) => {
        await page.goto('/dark');
        await waitForCKEditorReady(page);
        await waitForLayoutStable(page);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('dark.png', SCREENSHOT_OPTIONS);
    });
});
