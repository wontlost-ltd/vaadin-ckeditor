import { test, expect } from '@playwright/test';
import { waitForCKEditorReady, waitForInlineEditorReady, waitForEditorHost } from '../helpers/ck';

/**
 * Visual regression baselines.
 *
 * <p>Pixel rendering varies by OS, browser font stack, and CPU architecture.
 * We pin baselines to the CI runner (Linux x64 / Ubuntu Jammy) and skip
 * the spec elsewhere to keep them green during local development.</p>
 *
 * <p><b>How baselines are produced:</b></p>
 * <ol>
 *   <li>First CI run on a fresh branch fails the visual tests because no
 *       baselines exist. The CI job uploads the actual screenshots as the
 *       <code>playwright-test-results</code> artifact.</li>
 *   <li>Maintainer downloads the artifact, copies the <code>actual.png</code>
 *       files into <code>e2e/tests/__screenshots__/</code> renamed to the
 *       expected file name (without the <code>-actual</code> suffix), and
 *       commits them.</li>
 *   <li>Subsequent CI runs compare against the committed baselines.</li>
 * </ol>
 *
 * <p>To regenerate baselines later, either:</p>
 * <ul>
 *   <li>delete the committed PNGs and let CI re-produce them, or</li>
 *   <li>run a Linux container locally: <code>podman run --rm --platform
 *       linux/amd64 -v $PWD:/work -w /work/e2e mcr.microsoft.com/playwright:v1.50.0-jammy
 *       npx playwright test visual-regression --update-snapshots</code></li>
 * </ul>
 *
 * <p>The spec is also skipped when <code>SKIP_VISUAL_REGRESSION=1</code> is
 * set, e.g. on first-time setup CI runs that intentionally produce baselines
 * without failing the build.</p>
 */
test.describe('Visual regression', () => {
    test.skip(process.platform !== 'linux', 'Baselines are pinned to Linux/CI');
    test.skip(
        process.env.SKIP_VISUAL_REGRESSION === '1',
        'Visual regression intentionally skipped (e.g. during baseline bootstrap)'
    );

    // 0.25 means up to 25% of pixels may differ; CKEditor renders some non-deterministic
    // pixels around the focus ring and toolbar tooltips that we don't want to chase.
    const SCREENSHOT_OPTIONS = {
        animations: 'disabled' as const,
        maxDiffPixelRatio: 0.05,
        fullPage: false,
    };

    test('classic editor visual baseline', async ({ page }) => {
        await page.goto('/classic');
        await waitForCKEditorReady(page);
        await page.waitForTimeout(500);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('classic.png', SCREENSHOT_OPTIONS);
    });

    test('balloon editor visual baseline', async ({ page }) => {
        await page.goto('/balloon');
        await waitForInlineEditorReady(page);
        await page.waitForTimeout(500);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('balloon.png', SCREENSHOT_OPTIONS);
    });

    test('inline editor visual baseline', async ({ page }) => {
        await page.goto('/inline');
        await waitForInlineEditorReady(page);
        await page.waitForTimeout(500);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('inline.png', SCREENSHOT_OPTIONS);
    });

    test('decoupled editor visual baseline', async ({ page }) => {
        await page.goto('/decoupled');
        await waitForInlineEditorReady(page);
        await page.waitForTimeout(500);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('decoupled.png', SCREENSHOT_OPTIONS);
    });

    test('dark theme visual baseline', async ({ page }) => {
        await page.goto('/dark');
        await waitForCKEditorReady(page);
        await page.waitForTimeout(500);

        const host = await waitForEditorHost(page);
        await expect(host).toHaveScreenshot('dark.png', SCREENSHOT_OPTIONS);
    });
});
