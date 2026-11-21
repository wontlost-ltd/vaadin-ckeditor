import { expect, test, type JSHandle, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 粘性工具栏（issue #157）：AppLayout + TabSheet 布局中，聚焦编辑器后滚动，工具栏应贴住滚动容器顶部，
 * 上方不得出现 CKEditor 自带粘性面板的占位块（报告中的「矩形空白 + 闪烁」）。
 *
 * add-on 以 CSS position: sticky 实现粘性工具栏，CKEditor 的 StickyPanelView 仍会在滚动时切换
 * isSticky 并写内联样式（显示占位块、把内容设为 fixed）。滚动容器在 TabSheet 的 shadow DOM 里时，
 * 旧实现的 JS 清理找不到它，占位块一直留着；即使找到，也要晚一帧才清理。
 */

interface Sample {
    placeholderHeight: number;
    contentPosition: string;
    /** 工具栏顶边减去滚动容器的 padding 顶边（position: sticky 以 padding 内沿为界） */
    toolbarOffset: number;
}

/** 与 add-on 的 CSS sticky 一致：从宿主向上（穿过 slot 与 shadow root）找第一个真正可滚动的祖先 */
async function scrollContainer(page: Page): Promise<JSHandle<Element>> {
    return page.evaluateHandle(() => {
        let el: Element | null = document.querySelector('vaadin-ckeditor#ckeditor');
        while (el) {
            const style = getComputedStyle(el);
            if (el.scrollHeight > el.clientHeight + 1 && /(auto|scroll)/.test(style.overflowY)) {
                return el;
            }
            el = el.assignedSlot ?? el.parentElement ?? ((el.getRootNode() as ShadowRoot).host ?? null);
        }
        throw new Error('no scroll container');
    });
}

/**
 * 可滚动范围内的若干位置。编辑器是内容的最后一块且远高于视口，到最大滚动位置时它仍覆盖整个可见区，
 * 所以这些位置上工具栏都应贴顶（不会碰到 sticky 包含块的下边界）。
 */
async function stickyTargets(scroller: JSHandle<Element>): Promise<number[]> {
    const max = await scroller.evaluate((sc) => sc.scrollHeight - sc.clientHeight);
    expect(max, 'fixture must be scrollable').toBeGreaterThan(2000);
    return [0.05, 0.15, 0.4, 0.7, 0.95].map((f) => Math.round(max * f));
}

/**
 * 滚动到 y 并采样两次：
 * - sync：滚动容器自身的 scroll 监听里，此时 CKEditor（document 捕获阶段）已更新粘性面板，
 *   但尚未到下一帧的 requestAnimationFrame——这一刻若占位块可见，就会被画出来（闪烁）
 * - settled：两帧之后的稳定状态
 */
async function scrollAndSample(scroller: JSHandle<Element>, y: number): Promise<{ sync: Sample; settled: Sample }> {
    return scroller.evaluate((sc, target) => new Promise((resolve, reject) => {
        // 目标不可达或与当前位置相同时浏览器不派发 scroll 事件，直接报错而不是挂起
        if (target <= 0 || target > sc.scrollHeight - sc.clientHeight || target === sc.scrollTop) {
            reject(new Error(`scrollTop ${target} would not scroll (now ${sc.scrollTop})`));
            return;
        }
        const sample = (): Sample => {
            const q = (s: string) => document.querySelector(`vaadin-ckeditor#ckeditor ${s}`) as HTMLElement;
            return {
                placeholderHeight: q('.ck-sticky-panel__placeholder').getBoundingClientRect().height,
                contentPosition: getComputedStyle(q('.ck-sticky-panel__content')).position,
                toolbarOffset: q('.ck-toolbar').getBoundingClientRect().top - sc.getBoundingClientRect().top
                    - parseFloat(getComputedStyle(sc).paddingTop),
            };
        };
        sc.addEventListener('scroll', () => {
            const sync = sample();
            requestAnimationFrame(() => requestAnimationFrame(() => resolve({ sync, settled: sample() })));
        }, { once: true });
        sc.scrollTop = target;
    }), y);
}

for (const theme of ['light', 'dark'] as const) {
    test(`focused editor: toolbar sticks flush to the scroll container without a placeholder band (${theme}, #157)`, async ({ page }) => {
        await page.goto(theme === 'dark' ? '/sticky-toolbar?theme=dark' : '/sticky-toolbar');
        const editable = await waitForCKEditorReady(page);
        await editable.click({ position: { x: 40, y: 15 } });
        const scroller = await scrollContainer(page);

        for (const y of await stickyTargets(scroller)) {
            const { sync, settled } = await scrollAndSample(scroller, y);
            for (const s of [sync, settled]) {
                expect(s.placeholderHeight, `placeholder at scrollTop ${y}`).toBe(0);
                expect(s.contentPosition, `toolbar positioning at scrollTop ${y}`).toBe('static');
            }
            // 粘住后工具栏紧贴滚动容器顶部（只差 CKEditor 外框的 1px 边框）
            expect(settled.toolbarOffset, `toolbar offset at scrollTop ${y}`).toBeGreaterThanOrEqual(0);
            expect(settled.toolbarOffset, `toolbar offset at scrollTop ${y}`).toBeLessThanOrEqual(2);
        }
    });
}

test('unfocused editor keeps the toolbar in place and does not show a placeholder (#157)', async ({ page }) => {
    await page.goto('/sticky-toolbar');
    await waitForCKEditorReady(page);
    const scroller = await scrollContainer(page);
    const [, , middle] = await stickyTargets(scroller);
    const { settled } = await scrollAndSample(scroller, middle);
    expect(settled.placeholderHeight).toBe(0);
    // CSS sticky 不依赖焦点，未聚焦时同样贴顶
    expect(settled.toolbarOffset).toBeGreaterThanOrEqual(0);
    expect(settled.toolbarOffset).toBeLessThanOrEqual(2);
});
