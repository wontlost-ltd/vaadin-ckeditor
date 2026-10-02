import { test } from '@playwright/test';
import { expectWithinBudget, median, perfUrl, readServerInitTimes, waitForEditorsReady } from './perf-utils';

/**
 * 测量项 1：单编辑器初始化耗时（四种类型 × 三种预设）。
 *
 * 指标取自连接器回传给 Java 的 initTimeMs（EditorReadyEvent），即 CKEditor
 * `create()` 本身的耗时；另记录从导航开始到客户端就绪的端到端耗时，
 * 它额外包含 Vaadin 引导与服务端往返。每组跑 RUNS 次取中位数。
 */
const TYPES = ['classic', 'balloon', 'inline', 'decoupled'] as const;
const PRESETS = ['BASIC', 'STANDARD', 'FULL'] as const;
const RUNS = 3;

for (const type of TYPES) {
    for (const preset of PRESETS) {
        test(`初始化：${type} / ${preset}`, async ({ page }) => {
            const initTimes: number[] = [];
            const endToEnd: number[] = [];
            for (let run = 0; run < RUNS; run++) {
                const start = Date.now();
                await page.goto(perfUrl({ type, preset }));
                await waitForEditorsReady(page, 1);
                endToEnd.push(Date.now() - start);
                const [initTime] = await readServerInitTimes(page, 1);
                initTimes.push(initTime);
            }
            expectWithinBudget(`init.${type}.${preset}.createMs`, median(initTimes), 'ms');
            expectWithinBudget(`init.${type}.${preset}.endToEndMs`, median(endToEnd), 'ms');
        });
    }
}
