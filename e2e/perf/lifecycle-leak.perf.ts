import { expect, test, type Page } from '@playwright/test';
import { expectWithinBudget, measureMemory, openCdp, perfUrl, record, waitForEditorsReady, type MemorySnapshot } from './perf-utils';

/**
 * 测量项 4：反复挂载 / 卸载的泄漏检测。
 *
 * 两种生命周期各跑 CYCLES 轮：
 * - toggle：同一批实例移出布局再放回（disconnected → connected，重连时重建 CKEditor）；
 * - remount：丢弃旧实例并新建（相当于在视图间来回导航）。
 * 先做 WARMUP 轮热身让惰性初始化的缓存就位，再取基线；结束后强制 GC，比较
 * JS 堆、DOM 节点、事件监听器的**每轮增量**。这些指标与机器速度无关，预算可以设得较严。
 */
const COUNT = 3;
const CYCLES = 10;
/** 热身轮数：前几轮会填充 CKEditor / Lit / Vaadin 的惰性缓存与 JIT，堆增长不代表泄漏。 */
const WARMUP = 5;

async function waitUnmounted(page: Page): Promise<void> {
    await expect(page.locator('#mounted')).toHaveText('false');
    await page.waitForFunction(() => document.querySelectorAll('vaadin-ckeditor').length === 0);
    // 卸载后组件会异步销毁 CKEditor（含孤儿销毁超时），留出时间让其完成
    await page.waitForTimeout(500);
}

function perCycle(before: MemorySnapshot, after: MemorySnapshot) {
    return {
        heapKB: (after.jsHeapUsedBytes - before.jsHeapUsedBytes) / 1024 / CYCLES,
        nodes: (after.domNodes - before.domNodes) / CYCLES,
        listeners: (after.jsEventListeners - before.jsEventListeners) / CYCLES,
    };
}

test('同一批实例反复移出 / 放回布局', async ({ page }) => {
    const cdp = await openCdp(page);
    await page.goto(perfUrl({ count: COUNT }));
    await waitForEditorsReady(page, COUNT);

    const toggle = page.locator('#btn-toggle-attach');
    const cycle = async () => {
        await toggle.click();
        await waitUnmounted(page);
        await toggle.click();
        await waitForEditorsReady(page, COUNT);
    };

    for (let i = 0; i < WARMUP; i++) await cycle();
    const before = await measureMemory(cdp);
    for (let i = 0; i < CYCLES; i++) await cycle();
    const after = await measureMemory(cdp);

    const delta = perCycle(before, after);
    record('leak.toggle.heapKBPerCycle', delta.heapKB, 'KB');
    expectWithinBudget('leak.toggle.domNodesPerCycle', delta.nodes, 'nodes');
    expectWithinBudget('leak.toggle.listenersPerCycle', delta.listeners, 'listeners');
    expectWithinBudget('leak.toggle.heapKBPerCyclePerEditor', delta.heapKB / COUNT, 'KB');
});

test('丢弃旧实例并新建', async ({ page }) => {
    const cdp = await openCdp(page);
    await page.goto(perfUrl({ count: COUNT }));
    await waitForEditorsReady(page, COUNT);

    const remount = page.locator('#btn-remount-new');
    const cycle = async () => {
        await remount.click();
        // 新实例的 id 与旧实例相同，等待旧节点全部被替换后再判定就绪
        await page.waitForTimeout(200);
        await waitForEditorsReady(page, COUNT);
        await expect(page.locator('#ready-count')).toHaveText(String(COUNT));
    };

    for (let i = 0; i < WARMUP; i++) await cycle();
    const before = await measureMemory(cdp);
    for (let i = 0; i < CYCLES; i++) await cycle();
    const after = await measureMemory(cdp);

    const delta = perCycle(before, after);
    record('leak.remount.heapKBPerCycle', delta.heapKB, 'KB');
    expectWithinBudget('leak.remount.domNodesPerCycle', delta.nodes, 'nodes');
    expectWithinBudget('leak.remount.listenersPerCycle', delta.listeners, 'listeners');
    expectWithinBudget('leak.remount.heapKBPerCyclePerEditor', delta.heapKB / COUNT, 'KB');
});
