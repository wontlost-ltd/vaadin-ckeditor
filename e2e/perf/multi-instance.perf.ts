import { test } from '@playwright/test';
import {
    expectWithinBudget, measureMemory, median, openCdp, perfUrl, readServerInitTimes, record, waitForEditorsReady,
} from './perf-utils';

/**
 * 测量项 2：同页多实例的扩展性。
 *
 * 对 N = 1 / 5 / 10 / 20 分别测量全部就绪的端到端耗时、GC 后的 JS 堆与 DOM 节点数。
 * 绝对耗时随机器波动，因此关键判定是**边际成本的比例**：
 * 从 10 个到 20 个实例的每实例增量，不应显著高于从 1 个到 5 个的每实例增量；
 * 若存在 O(N²) 行为（例如每个实例都遍历全部实例、全局监听器累积），比例会明显变大。
 */
const COUNTS = [1, 5, 10, 20] as const;
const RUNS = 2;

test('同页多实例：耗时、内存与边际成本', async ({ page }) => {
    const cdp = await openCdp(page);
    const wall: Record<number, number> = {};
    const heap: Record<number, number> = {};
    const nodes: Record<number, number> = {};

    for (const count of COUNTS) {
        const runs: number[] = [];
        for (let run = 0; run < RUNS; run++) {
            const start = Date.now();
            await page.goto(perfUrl({ count }));
            await waitForEditorsReady(page, count);
            runs.push(Date.now() - start);
        }
        await readServerInitTimes(page, count);
        const memory = await measureMemory(cdp);
        wall[count] = median(runs);
        heap[count] = memory.jsHeapUsedBytes;
        nodes[count] = memory.domNodes;
        expectWithinBudget(`multi.n${count}.readyMs`, wall[count], 'ms');
        record(`multi.n${count}.heapMB`, heap[count] / 1024 / 1024, 'MB');
        record(`multi.n${count}.domNodes`, nodes[count], 'nodes');
    }

    const marginalSmall = (wall[5] - wall[1]) / 4;
    const marginalLarge = (wall[20] - wall[10]) / 10;
    record('multi.marginalMs.1to5', marginalSmall, 'ms/editor');
    record('multi.marginalMs.10to20', marginalLarge, 'ms/editor');
    // 分母设下限，避免小样本时增量接近 0 造成比例失真
    expectWithinBudget('multi.marginalRatio', marginalLarge / Math.max(marginalSmall, 20), 'x');

    expectWithinBudget('multi.heapPerEditorMB', (heap[20] - heap[1]) / 19 / 1024 / 1024, 'MB');
    expectWithinBudget('multi.domNodesPerEditor', (nodes[20] - nodes[1]) / 19, 'nodes');
});
