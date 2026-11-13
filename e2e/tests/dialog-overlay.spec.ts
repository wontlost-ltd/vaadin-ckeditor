import { expect, test, type Page } from '@playwright/test';

/**
 * issue #143 / #144 / #145 的端到端回归：编辑器嵌在 Vaadin Dialog（顶层 popover）里时，
 * CKEditor 的对话框、全屏与暗色 Style 面板都必须可见、可交互、可读。
 *
 * 断言策略：不看 z-index，直接用 elementFromPoint 做命中测试——顶层之下的元素
 * 无论 z-index 多大都命中不到，这正是缺陷的形态。
 */

async function openDialogEditor(page: Page, query = ''): Promise<void> {
    await page.goto('/dialog' + query);
    await page.click('#open-dialog');
    await page.locator('vaadin-dialog .ck-editor__editable').first().waitFor({ state: 'visible', timeout: 30_000 });
}

/** 元素中心点的命中目标是否落在元素自身内部 */
async function centerIsHittable(page: Page, selector: string): Promise<boolean> {
    return page.evaluate((sel) => {
        const el = document.querySelector(sel);
        if (!el) {
            return false;
        }
        const r = el.getBoundingClientRect();
        const top = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
        return !!top && el.contains(top);
    }, selector);
}

for (const modality of ['VISUAL', 'MODELESS'] as const) {
    const query = modality === 'MODELESS' ? '?modeless' : '';

    test.describe(`Dialog modality ${modality}`, () => {
        test('#143 special characters dialog opens above the Vaadin dialog with content', async ({ page }) => {
            await openDialogEditor(page, query);
            await page.locator('vaadin-dialog .ck-toolbar button[data-cke-tooltip-text*="pecial"]').first().click();

            const dialog = page.locator('.ck-dialog.ck-special-characters');
            await expect(dialog).toBeVisible();
            // 裸 SpecialCharacters 自动补上 Essentials 分类，面板不再为空
            expect(await dialog.locator('.ck-character-grid__tile').count()).toBeGreaterThan(0);
            expect(await centerIsHittable(page, '.ck-dialog.ck-special-characters')).toBe(true);

            // 在 CKEditor 对话框内点击会触发连接器的 popover 重排（hide+show），焦点必须保持在对话框内。
            // 同时记录浮层容器的原生 Popover 生命周期事件。一次点击会触发 pointerdown 与 focusin 两次重排，
            // 每次重排在同步的 beforetoggle 上都是成对的 open>closed、closed>open；异步的 toggle 则被
            // 浏览器合并（规范规定同一任务内多次切换只排队一次，Chromium 还会跨两次重排合并），因此
            // 外部监听者只会看到 open>open，永远观察不到「关闭」状态。结束后容器与 Vaadin 自身的 overlay 都仍打开。
            await page.evaluate(() => {
                const wrapper = document.querySelector('vaadin-ckeditor[data-ck-overlay-host] > .ck-body-wrapper')!;
                const log: string[] = [];
                for (const type of ['beforetoggle', 'toggle']) {
                    wrapper.addEventListener(type, (e) => {
                        const { oldState, newState } = e as ToggleEvent;
                        log.push(`${type}:${oldState}>${newState}`);
                    });
                }
                (window as unknown as { __popoverToggles: string[] }).__popoverToggles = log;
            });
            const categoryButton = dialog.locator('.ck-character-categories .ck-dropdown__button').first();
            await categoryButton.click();
            expect(await page.evaluate(() => !!document.activeElement?.closest('.ck-dialog'))).toBe(true);
            const toggles = await page.evaluate(() => (window as unknown as { __popoverToggles: string[] }).__popoverToggles);
            const before = toggles.filter((t) => t.startsWith('beforetoggle:'));
            expect(before.length).toBeGreaterThanOrEqual(2);
            expect(before.length % 2).toBe(0);
            before.forEach((t, i) => expect(t).toBe(i % 2 === 0 ? 'beforetoggle:open>closed' : 'beforetoggle:closed>open'));
            await expect.poll(() => page.evaluate(() => (window as unknown as { __popoverToggles: string[] }).__popoverToggles
                .filter((t) => t.startsWith('toggle:')))).not.toHaveLength(0);
            const after = await page.evaluate(() => (window as unknown as { __popoverToggles: string[] }).__popoverToggles);
            expect(after.filter((t) => t.startsWith('toggle:')).every((t) => t === 'toggle:open>open')).toBe(true);
            expect(await page.evaluate(() => ({
                wrapperOpen: document.querySelector('vaadin-ckeditor > .ck-body-wrapper')!.matches(':popover-open'),
                vaadinOpen: !!document.querySelector('vaadin-dialog')!.shadowRoot!.querySelector('[popover]:popover-open'),
            }))).toEqual({ wrapperOpen: true, vaadinOpen: true });
            await categoryButton.click(); // 收起分类下拉，避免遮住字符网格

            // 点选字符：既要插入成功，也不得触发 Vaadin 的「外部点击关闭」
            await dialog.locator('.ck-character-grid__tile').first().click();
            await expect(page.locator('vaadin-dialog .ck-editor__editable')).not.toHaveText('Hello from dialog');
            await expect(page.locator('vaadin-dialog[opened]')).toHaveCount(1);
        });

        test('#144 fullscreen covers the viewport above the dialog and stays editable', async ({ page }) => {
            await openDialogEditor(page, query);
            await page.locator('vaadin-dialog .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
            await expect(page.locator('.ck-fullscreen__main-wrapper')).toBeVisible();

            // 视口四角与 Dialog 框之外的点都应命中全屏容器
            const covered = await page.evaluate(() => {
                const fs = document.querySelector('.ck-fullscreen__main-wrapper')!;
                const { innerWidth: w, innerHeight: h } = window;
                return [[8, 8], [w - 8, 8], [8, h - 8], [w - 8, h - 8]]
                    .map(([x, y]) => document.elementFromPoint(x, y))
                    .every((el) => !!el && fs.contains(el));
            });
            expect(covered).toBe(true);

            const editable = page.locator('.ck-fullscreen__main-wrapper .ck-editor__editable');
            await editable.click();
            await page.keyboard.type('TYPED');
            await expect(editable).toContainText('TYPED');

            // 退出全屏：编辑器回到 Dialog 内，全屏容器移除
            await page.locator('.ck-fullscreen__main-wrapper .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
            await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(0);
            await expect(page.locator('vaadin-dialog .ck-editor__editable')).toBeVisible();
        });
    });
}

test('#145 style panel labels are readable in dark theme', async ({ page }) => {
    await openDialogEditor(page, '?dark');
    await page.locator('vaadin-dialog .ck-toolbar .ck-style-dropdown button').first().click();

    const label = page.locator('.ck-style-grid__button:not(.ck-on) .ck-button__label').first();
    await expect(label).toBeVisible();
    const colors = await label.evaluate((el) => {
        const cs = getComputedStyle(el);
        return { color: cs.color, background: cs.backgroundColor };
    });
    // CKEditor 默认的 #f0f0f0 标签底色在暗色下不可用
    expect(colors.background).not.toBe('rgb(240, 240, 240)');
    const luminance = (rgb: string) => {
        const [r, g, b] = rgb.match(/\d+/g)!.map(Number);
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255;
    };
    expect(Math.abs(luminance(colors.color) - luminance(colors.background))).toBeGreaterThan(0.5);
});

/** 某个宿主内的浮层容器状态：wrapper 数量、是否在顶层、其中 .ck-body 数量 */
async function hostLayerState(page: Page, hostSelector: string) {
    return page.evaluate((sel) => {
        const host = document.querySelector(sel)!;
        const wrappers = host.querySelectorAll(':scope > .ck-body-wrapper');
        return {
            marked: host.hasAttribute('data-ck-overlay-host'),
            wrappers: wrappers.length,
            open: wrappers.length === 1 && wrappers[0].matches(':popover-open'),
            bodies: host.querySelectorAll(':scope > .ck-body-wrapper > .ck-body').length,
        };
    }, hostSelector);
}

/** 页面侧挂在 window 上的引用，用来跨「宿主离开 document / 编辑器被重建」保留旧对象 */
interface E2EWindow extends Window {
    __hosts?: HTMLElement[];
    __prevEditor?: { state: string };
}

/** 记住当前宿主元素：Dialog 关闭后它们随内容整体离开 document，只能通过保留的引用检查释放结果 */
async function rememberHosts(page: Page, selectors: string[]): Promise<void> {
    await page.evaluate((sels) => {
        (window as E2EWindow).__hosts = sels.map((sel) => document.querySelector<HTMLElement>(sel)!);
    }, selectors);
}

/**
 * 被记住的宿主是否已全部释放：已离开 document、宿主内不再有 wrapper、标记属性已移除。
 * 这直接证明 releaseBodyCollection 在销毁路径上执行过，而不是 wrapper 跟着宿主一起消失。
 */
async function rememberedHostsReleased(page: Page): Promise<boolean> {
    return page.evaluate(() => (window as E2EWindow).__hosts!.every((host) =>
        !host.isConnected
        && host.querySelector(':scope > .ck-body-wrapper') === null
        && !host.hasAttribute('data-ck-overlay-host')));
}

test.describe('two editors in one dialog', () => {
    test('each host owns its own top-layer container and their dialogs do not interfere', async ({ page }) => {
        await openDialogEditor(page, '?two');
        await page.locator('vaadin-ckeditor#ckeditor-2 .ck-editor__editable').waitFor({ state: 'visible' });

        // 两个宿主各自接管自己的 .ck-body，互不共用 wrapper
        expect(await hostLayerState(page, '#ckeditor')).toEqual({ marked: true, wrappers: 1, open: true, bodies: 1 });
        expect(await hostLayerState(page, '#ckeditor-2')).toEqual({ marked: true, wrappers: 1, open: true, bodies: 1 });

        // 先在第二个编辑器打开特殊字符并插入：对话框属于第二个宿主，且只改第二个编辑器的内容
        await page.locator('#ckeditor-2 .ck-toolbar button[data-cke-tooltip-text*="pecial"]').first().click();
        const second = page.locator('#ckeditor-2 .ck-dialog.ck-special-characters');
        await expect(second).toBeVisible();
        expect(await centerIsHittable(page, '#ckeditor-2 .ck-dialog.ck-special-characters')).toBe(true);
        await second.locator('.ck-character-grid__tile').first().click();
        await expect(page.locator('#ckeditor-2 .ck-editor__editable')).not.toHaveText('Second editor');
        await expect(page.locator('#ckeditor .ck-editor__editable')).toHaveText('Hello from dialog');

        // 再在第一个编辑器打开特殊字符：它的 wrapper 比第二个更早进入顶层，交互后必须重新压到最上
        await page.locator('#ckeditor .ck-toolbar button[data-cke-tooltip-text*="pecial"]').first().click();
        const first = page.locator('#ckeditor .ck-dialog.ck-special-characters');
        await expect(first).toBeVisible();
        expect(await centerIsHittable(page, '#ckeditor .ck-dialog.ck-special-characters')).toBe(true);
        await first.locator('.ck-character-grid__tile').first().click();
        await expect(page.locator('#ckeditor .ck-editor__editable')).not.toHaveText('Hello from dialog');
        await expect(page.locator('vaadin-dialog[opened]')).toHaveCount(1);
    });

    test('fullscreen of one editor covers the viewport while the other editor stays intact', async ({ page }) => {
        await openDialogEditor(page, '?two');
        await page.locator('vaadin-ckeditor#ckeditor-2 .ck-editor__editable').waitFor({ state: 'visible' });

        await page.locator('#ckeditor-2 .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
        await expect(page.locator('#ckeditor-2 > .ck-fullscreen__main-wrapper')).toBeVisible();
        const covered = await page.evaluate(() => {
            const fs = document.querySelector('.ck-fullscreen__main-wrapper')!;
            const { innerWidth: w, innerHeight: h } = window;
            return [[8, 8], [w - 8, 8], [8, h - 8], [w - 8, h - 8]]
                .map(([x, y]) => document.elementFromPoint(x, y))
                .every((el) => !!el && fs.contains(el));
        });
        expect(covered).toBe(true);

        // 退出全屏后两个编辑器都回到 Dialog 内且内容完好
        await page.locator('.ck-fullscreen__main-wrapper .ck-toolbar button[data-cke-tooltip-text*="ull"]').first().click();
        await expect(page.locator('.ck-fullscreen__main-wrapper')).toHaveCount(0);
        await expect(page.locator('#ckeditor .ck-editor__editable')).toHaveText('Hello from dialog');
        await expect(page.locator('#ckeditor-2 .ck-editor__editable')).toHaveText('Second editor');
        expect(await hostLayerState(page, '#ckeditor')).toEqual({ marked: true, wrappers: 1, open: true, bodies: 1 });
        expect(await hostLayerState(page, '#ckeditor-2')).toEqual({ marked: true, wrappers: 1, open: true, bodies: 1 });
    });

    test('closing the dialog releases both containers', async ({ page }) => {
        await openDialogEditor(page, '?two');
        await page.locator('vaadin-ckeditor#ckeditor-2 .ck-editor__editable').waitFor({ state: 'visible' });
        await rememberHosts(page, ['#ckeditor', '#ckeditor-2']);
        await expect.poll(async () => {
            await page.mouse.click(4, 4);
            return page.locator('vaadin-dialog[opened]').count();
        }, { timeout: 10_000 }).toBe(0);
        await expect.poll(() => page.evaluate(() => document.querySelectorAll('.ck-body, .ck-body-wrapper').length), { timeout: 10_000 }).toBe(0);
        // 销毁被推迟到空闲回调，释放结果要在已分离的宿主上轮询确认
        await expect.poll(() => rememberedHostsReleased(page), { timeout: 10_000 }).toBe(true);
    });
});

test.describe('disconnect / reconnect inside the dialog', () => {
    test('re-attaching the editor re-adopts its container without leaving the old one behind', async ({ page }) => {
        await openDialogEditor(page);
        await expect(page.locator('#slot-a vaadin-ckeditor#ckeditor')).toBeAttached();

        const ROUNDS = 2;
        for (let i = 1; i <= ROUNDS; i++) {
            // 记住当前编辑器实例：重连必须走「销毁旧实例 → 重建新实例」，而不是同一实例幸存
            await page.evaluate(() => {
                const host = document.querySelector('#ckeditor') as HTMLElement & { editor: { state: string } };
                (window as E2EWindow).__prevEditor = host.editor;
            });
            await page.locator('#reattach').click();
            await expect(page.locator('#move-count')).toHaveText(String(i), { timeout: 20_000 });
            const slot = i % 2 === 1 ? '#slot-b' : '#slot-a';
            await expect(page.locator(`${slot} vaadin-ckeditor#ckeditor`)).toBeAttached();
            await page.locator(`${slot} vaadin-ckeditor#ckeditor .ck-editor__editable`).waitFor({ state: 'visible', timeout: 30_000 });
            await expect.poll(() => page.evaluate(() => {
                const host = document.querySelector('#ckeditor') as HTMLElement & { editor: { state: string } | null };
                const prev = (window as E2EWindow).__prevEditor!;
                return { replaced: !!host.editor && host.editor !== prev, prevState: prev.state, newState: host.editor?.state };
            }), { timeout: 10_000 }).toEqual({ replaced: true, prevState: 'destroyed', newState: 'ready' });

            // 重建后的编辑器重新接管容器：宿主内恰好一个在顶层的 wrapper、一个 .ck-body；
            // 页面上也只剩这一个 .ck-body（旧的随销毁清掉，没有漏到 document.body）
            await expect.poll(() => hostLayerState(page, '#ckeditor'), { timeout: 10_000 })
                .toEqual({ marked: true, wrappers: 1, open: true, bodies: 1 });
            expect(await page.evaluate(() => document.querySelectorAll('.ck-body').length)).toBe(1);
        }

        // 重连后的浮层仍在 Dialog 之上且可交互
        await page.locator('#ckeditor .ck-toolbar button[data-cke-tooltip-text*="pecial"]').first().click();
        const dialog = page.locator('#ckeditor .ck-dialog.ck-special-characters');
        await expect(dialog).toBeVisible();
        expect(await centerIsHittable(page, '#ckeditor .ck-dialog.ck-special-characters')).toBe(true);
        await dialog.locator('.ck-character-grid__tile').first().click();
        await expect(page.locator('#ckeditor .ck-editor__editable')).not.toHaveText('Hello from dialog');
        await expect(page.locator('vaadin-dialog[opened]')).toHaveCount(1);
    });
});

test('floating UI container is cleaned up when the dialog closes', async ({ page }) => {
    await openDialogEditor(page);
    await rememberHosts(page, ['#ckeditor']);
    // Dialog 框外点击 → Vaadin 关闭 Dialog → 编辑器销毁。
    // Vaadin 在打开后才挂外部点击监听，首次点击可能早于监听挂上，故用 poll 重试点击。
    await expect.poll(async () => {
        await page.mouse.click(4, 4); // 页面左上角留白，位于 Dialog 框外且在任何视口尺寸内
        return page.locator('vaadin-dialog[opened]').count();
    }, { timeout: 10_000 }).toBe(0);
    await expect.poll(() => page.evaluate(() => document.querySelectorAll('.ck-body').length), { timeout: 10_000 }).toBe(0);
    await expect.poll(() => page.evaluate(() => document.querySelectorAll('vaadin-ckeditor').length), { timeout: 10_000 }).toBe(0);
    await expect.poll(() => rememberedHostsReleased(page), { timeout: 10_000 }).toBe(true);
});
