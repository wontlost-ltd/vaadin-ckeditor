import { expect, test, type CDPSession, type Page } from '@playwright/test';
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * 性能套件的公共工具（issue #137）。
 *
 * - 预算集中在 budgets.json，按指标名查找，测试代码里不写死阈值；
 * - 每个指标同时写入结果文件（默认 perf-results/results.json，可用 PERF_RESULTS 覆盖），
 *   CI 把它作为 artifact 上传，便于观察趋势；
 * - 内存类指标一律在强制 GC 之后读取，降低噪声。
 */

const HERE = dirname(fileURLToPath(import.meta.url));

export interface Budget {
    max: number;
    unit: string;
    note?: string;
}

const BUDGETS: Record<string, Budget> = JSON.parse(readFileSync(join(HERE, 'budgets.json'), 'utf8')).budgets;

const RESULTS_FILE = process.env.PERF_RESULTS ?? join(HERE, '..', 'perf-results', 'results.json');

interface MetricRecord {
    name: string;
    value: number;
    unit: string;
    budget: number | null;
    test: string;
    at: string;
}

/** 记录一个指标（不做判定）。 */
export function record(name: string, value: number, unit: string): void {
    const rounded = Math.round(value * 100) / 100;
    test.info().annotations.push({ type: 'perf', description: `${name} = ${rounded} ${unit}` });
    mkdirSync(dirname(RESULTS_FILE), { recursive: true });
    let records: MetricRecord[] = [];
    try {
        records = JSON.parse(readFileSync(RESULTS_FILE, 'utf8'));
    } catch {
        // 首次写入
    }
    records = records.filter((r) => r.name !== name);
    records.push({
        name, value: rounded, unit,
        budget: BUDGETS[name]?.max ?? null,
        test: test.info().titlePath.join(' › '),
        at: new Date().toISOString(),
    });
    writeFileSync(RESULTS_FILE, JSON.stringify(records, null, 2));
}

/** PERF_BASELINE=1 时只记录、不判定，用于采集基线后再调整 budgets.json。 */
const BASELINE_MODE = process.env.PERF_BASELINE === '1';

/** 记录指标并按 budgets.json 判定；预算缺失视为配置错误。 */
export function expectWithinBudget(name: string, value: number, unit = BUDGETS[name]?.unit ?? ''): void {
    if (BASELINE_MODE) {
        record(name, value, unit);
        return;
    }
    const budget = BUDGETS[name];
    expect(budget, `budgets.json 缺少指标 "${name}"`).toBeDefined();
    record(name, value, budget.unit);
    expect(value, `${name} = ${value} ${budget.unit}，超出预算 ${budget.max} ${budget.unit}`)
        .toBeLessThanOrEqual(budget.max);
}

export function median(values: number[]): number {
    const sorted = [...values].sort((a, b) => a - b);
    const mid = Math.floor(sorted.length / 2);
    return sorted.length % 2 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
}

/** 构造 /perf 夹具的 URL。 */
export function perfUrl(params: Record<string, string | number | boolean> = {}): string {
    const query = new URLSearchParams(Object.entries(params).map(([k, v]) => [k, String(v)]));
    const qs = query.toString();
    return qs ? `/perf?${qs}` : '/perf';
}

/**
 * 等待页面上所有编辑器在**客户端**就绪（CKEditor 实例存在且 state === 'ready'），
 * 并要求实例数恰为 expected。
 */
export async function waitForEditorsReady(page: Page, expected: number, timeout = 120_000): Promise<void> {
    await page.waitForFunction((n) => {
        const hosts = Array.from(document.querySelectorAll('vaadin-ckeditor')) as Array<HTMLElement & { editor?: { state?: string } | null }>;
        return hosts.length === n && hosts.every((h) => h.editor?.state === 'ready');
    }, expected, { timeout, polling: 50 });
}

/** 服务端探针：EditorReadyEvent 携带的各实例 initTimeMs。 */
export async function readServerInitTimes(page: Page, expected: number): Promise<number[]> {
    await expect(page.locator('#ready-count')).toHaveText(String(expected), { timeout: 60_000 });
    const text = await page.locator('#init-times').innerText();
    return text.split(',').filter(Boolean).map(Number);
}

export interface MemorySnapshot {
    jsHeapUsedBytes: number;
    domNodes: number;
    jsEventListeners: number;
    documents: number;
}

export async function openCdp(page: Page): Promise<CDPSession> {
    const cdp = await page.context().newCDPSession(page);
    await cdp.send('Performance.enable');
    return cdp;
}

/** 强制两轮 GC 后读取堆、DOM 节点与事件监听器计数。 */
export async function measureMemory(cdp: CDPSession): Promise<MemorySnapshot> {
    await cdp.send('HeapProfiler.collectGarbage');
    await cdp.send('HeapProfiler.collectGarbage');
    const { metrics } = await cdp.send('Performance.getMetrics');
    const metric = (name: string) => metrics.find((m) => m.name === name)?.value ?? 0;
    const counters = await cdp.send('Memory.getDOMCounters');
    return {
        jsHeapUsedBytes: metric('JSHeapUsedSize'),
        domNodes: counters.nodes,
        jsEventListeners: counters.jsEventListeners,
        documents: counters.documents,
    };
}

/**
 * 收集 Vaadin 客户端-服务端往返的报文体积（`v-r=uidl` / `v-r=init`）。
 * 返回的函数会读出并清空当前累计值。
 */
export function trackUidlTraffic(page: Page): () => Promise<{ requestBytes: number; responseBytes: number; roundTrips: number }> {
    let pending: Promise<void>[] = [];
    let requestBytes = 0;
    let responseBytes = 0;
    let roundTrips = 0;
    page.on('response', (response) => {
        const url = response.url();
        if (!/[?&]v-r=(uidl|init)/.test(url)) return;
        roundTrips++;
        requestBytes += Buffer.byteLength(response.request().postData() ?? '', 'utf8');
        pending.push(response.body().then((b) => { responseBytes += b.length; }).catch(() => {}));
    });
    return async () => {
        await Promise.all(pending);
        const snapshot = { requestBytes, responseBytes, roundTrips };
        pending = [];
        requestBytes = 0;
        responseBytes = 0;
        roundTrips = 0;
        return snapshot;
    };
}
