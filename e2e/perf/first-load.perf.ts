import { test } from '@playwright/test';
import { expectWithinBudget, perfUrl, record, trackUidlTraffic, waitForEditorsReady } from './perf-utils';

/**
 * 测量项 5 与 6（客户端可见部分）：首屏加载与服务端报文体积。
 *
 * - 冷启动（全新浏览器上下文、无缓存）加载单编辑器页面；
 * - JS 传输体积来自 Resource Timing；LCP 与长任务通过在页面脚本执行前注册的
 *   PerformanceObserver 收集，TBT 按 Lighthouse 定义近似：Σ max(0, 长任务时长 − 50ms)；
 * - Vaadin 往返报文：首屏 init/uidl 响应体积，以及 FULL 预设下插件、工具栏、配置 JSON 的影响。
 */
const OBSERVERS = `
    window.__perf = { lcp: 0, longTasks: [] };
    new PerformanceObserver((list) => {
        for (const e of list.getEntries()) window.__perf.lcp = e.startTime;
    }).observe({ type: 'largest-contentful-paint', buffered: true });
    new PerformanceObserver((list) => {
        for (const e of list.getEntries()) window.__perf.longTasks.push(e.duration);
    }).observe({ type: 'longtask', buffered: true });
`;

for (const preset of ['BASIC', 'FULL'] as const) {
    test(`首屏加载：classic / ${preset}`, async ({ browser }) => {
        const context = await browser.newContext();
        const page = await context.newPage();
        await page.addInitScript(OBSERVERS);
        const traffic = trackUidlTraffic(page);

        const start = Date.now();
        await page.goto(perfUrl({ preset }));
        await waitForEditorsReady(page, 1);
        const readyMs = Date.now() - start;
        // 留出时间让 LCP 与就绪后的长任务落定
        await page.waitForTimeout(1000);

        const metrics = await page.evaluate(() => {
            const w = window as unknown as { __perf: { lcp: number; longTasks: number[] } };
            const resources = performance.getEntriesByType('resource') as PerformanceResourceTiming[];
            const scripts = resources.filter((r) => r.initiatorType === 'script' || /\.m?js(\?|$)/.test(r.name));
            return {
                lcp: w.__perf.lcp,
                tbt: w.__perf.longTasks.reduce((sum, d) => sum + Math.max(0, d - 50), 0),
                longTaskCount: w.__perf.longTasks.length,
                jsTransferBytes: scripts.reduce((sum, r) => sum + r.transferSize, 0),
                jsDecodedBytes: scripts.reduce((sum, r) => sum + r.decodedBodySize, 0),
                largestScript: scripts.reduce((max, r) => Math.max(max, r.decodedBodySize), 0),
            };
        });
        const uidl = await traffic();

        expectWithinBudget(`firstLoad.${preset}.readyMs`, readyMs, 'ms');
        expectWithinBudget(`firstLoad.${preset}.lcpMs`, metrics.lcp, 'ms');
        expectWithinBudget(`firstLoad.${preset}.tbtMs`, metrics.tbt, 'ms');
        record(`firstLoad.${preset}.longTasks`, metrics.longTaskCount, 'tasks');
        expectWithinBudget(`firstLoad.${preset}.jsTransferKB`, metrics.jsTransferBytes / 1024, 'KB');
        record(`firstLoad.${preset}.jsDecodedKB`, metrics.jsDecodedBytes / 1024, 'KB');
        record(`firstLoad.${preset}.largestScriptKB`, metrics.largestScript / 1024, 'KB');
        expectWithinBudget(`firstLoad.${preset}.uidlResponseKB`, uidl.responseBytes / 1024, 'KB');
        record(`firstLoad.${preset}.uidlRoundTrips`, uidl.roundTrips, 'requests');

        await context.close();
    });
}
