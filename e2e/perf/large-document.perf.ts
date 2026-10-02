import { expect, test, type Page } from '@playwright/test';
import { expectWithinBudget, perfUrl, record, trackUidlTraffic, waitForEditorsReady } from './perf-utils';

/**
 * 测量项 3：大文档往返（约 1 MB HTML）。
 *
 * - 下发：点击「下发大文档」到客户端 CKEditor 的 getData() 含末尾标记；
 * - 回传：在编辑器里输入一个字符，到服务端 getValue() 含末尾标记；
 * - decoupled 下对比 minimap 关闭 / 开启 / 简化预览三种情况的下发耗时。
 * 同时记录下发时 UIDL 报文与文档体积之比，用于发现重复传输。
 */
const DOC_KB = 1024;
const MARKER = 'PERF-END-MARKER';

async function measureSetLarge(page: Page): Promise<number> {
    const start = Date.now();
    await page.locator('#btn-set-large').click();
    // 不能用 editor.getData() 轮询：1 MB 文档下每次序列化约 0.5 秒，测量本身会拖慢被测对象。
    // 标记是文档最后一段，检查可编辑区的最后一个子元素即可，开销与文档体积无关。
    await page.waitForFunction((marker) => {
        const editable = document.querySelector('vaadin-ckeditor .ck-editor__editable');
        return !!editable?.lastElementChild?.textContent?.includes(marker);
    }, MARKER, { timeout: 120_000, polling: 50 });
    return Date.now() - start;
}

test('大文档：服务端下发与客户端回传（classic）', async ({ page }) => {
    const traffic = trackUidlTraffic(page);
    await page.goto(perfUrl({ docKb: DOC_KB }));
    await waitForEditorsReady(page, 1);
    await traffic();

    expectWithinBudget('largeDoc.classic.setValueMs', await measureSetLarge(page), 'ms');
    const down = await traffic();
    expectWithinBudget('largeDoc.downstreamBytesPerDocByte', down.responseBytes / (DOC_KB * 1024), 'x');

    // 回传：在文档开头输入，触发客户端 → 服务端同步整份内容
    const start = Date.now();
    await page.locator('.ck-editor__editable').first().click();
    await page.keyboard.press('Control+Home');
    await page.keyboard.type('x');
    await expect(page.locator('#server-value-has-marker')).toHaveText('true', { timeout: 120_000 });
    expectWithinBudget('largeDoc.classic.clientSyncMs', Date.now() - start, 'ms');
    const up = await traffic();
    expectWithinBudget('largeDoc.upstreamBytesPerDocByte', up.requestBytes / (DOC_KB * 1024), 'x');
});

test('大文档：注册 ContentChange 监听器时，连续输入只回传一份文档，旧内容由服务端镜像补全', async ({ page }) => {
    const traffic = trackUidlTraffic(page);
    await page.goto(perfUrl({ docKb: DOC_KB, contentEvents: true }));
    await waitForEditorsReady(page, 1);
    const initialLength = (await page.evaluate(() =>
        (document.querySelector('vaadin-ckeditor') as HTMLElement & { editor: { getData(): string } }).editor.getData().length));

    // 服务端下发：产生一次来源为 API 的 contentChange，旧内容是创建时的初始内容（显式携带）
    await measureSetLarge(page);
    await expect(page.locator('#content-events')).toHaveText('1');
    await expect(page.locator('#content-source')).toHaveText('API');
    expect(Number(await page.locator('#content-old-length').innerText())).toBe(initialLength);
    const largeLength = Number(await page.locator('#content-new-length').innerText());
    expect(largeLength).toBeGreaterThan(DOC_KB * 1024);
    await traffic();

    // 服务端改值之后的第一次用户输入：API 上报可能被服务端丢弃（disabled / inert），
    // 镜像不可信，旧内容显式携带——报文约为两份文档，只记录不判定
    const typeOneChar = async (expectedEvents: number) => {
        await page.keyboard.type('x');
        await expect(page.locator('#content-events')).toHaveText(String(expectedEvents), { timeout: 120_000 });
    };
    await page.locator('.ck-editor__editable').first().click();
    await page.keyboard.press('Control+Home');
    await typeOneChar(2);
    expect(Number(await page.locator('#content-old-length').innerText())).toBe(largeLength);
    expect(Number(await page.locator('#content-new-length').innerText())).toBe(largeLength + 1);
    await expect(page.locator('#server-value-has-marker')).toHaveText('true', { timeout: 120_000 });
    record('largeDoc.upstreamBytesPerDocByte.firstAfterApi', (await traffic()).requestBytes / (DOC_KB * 1024), 'x');

    // 连续输入的稳态：旧内容不再随报文携带，由服务端镜像（上一次的新内容）补全
    await typeOneChar(3);
    expect(Number(await page.locator('#content-old-length').innerText())).toBe(largeLength + 1);
    expect(Number(await page.locator('#content-new-length').innerText())).toBe(largeLength + 2);
    await expect(page.locator('#server-value-length')).toHaveText(String(largeLength + 2), { timeout: 120_000 });

    const up = await traffic();
    expectWithinBudget('largeDoc.upstreamBytesPerDocByte.withContentEvents', up.requestBytes / (DOC_KB * 1024), 'x');
});

const MINIMAP_VARIANTS: Array<{ name: string; params: Record<string, boolean> }> = [
    { name: 'noMinimap', params: {} },
    { name: 'minimap', params: { minimap: true } },
    { name: 'minimapSimple', params: { minimap: true, simple: true } },
];

for (const variant of MINIMAP_VARIANTS) {
    test(`大文档：decoupled / ${variant.name}`, async ({ page }) => {
        await page.goto(perfUrl({ type: 'decoupled', docKb: DOC_KB, ...variant.params }));
        await waitForEditorsReady(page, 1);
        // minimap 场景必须真的渲染了 minimap，否则测得的只是无 minimap 的耗时
        const minimapRendered = await page.evaluate(() => {
            const host = document.querySelector('vaadin-ckeditor') as (HTMLElement & { editor?: { plugins: { has(name: string): boolean } } }) | null;
            return !!host?.editor?.plugins.has('Minimap')
                && (host.querySelector('.minimap-container')?.childElementCount ?? 0) > 0;
        });
        expect(minimapRendered).toBe('minimap' in variant.params);
        expectWithinBudget(`largeDoc.decoupled.${variant.name}.setValueMs`, await measureSetLarge(page), 'ms');
    });
}
