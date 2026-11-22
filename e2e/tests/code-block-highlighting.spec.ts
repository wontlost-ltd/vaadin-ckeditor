import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 代码块编辑时语法高亮（`/code-highlight` 夹具）：
 * 输入后关键字被着色、保存的 HTML（服务端 getValue()）不含高亮 span、切换语言重新着色、
 * 撤销 / 重做照常、运行期开关与换配色、暗色编辑器下的配色，以及大代码块的耗时。
 */

const SPANS = 'pre span[class*="hljs-"]';

/** 编辑区内第 index 个代码块中带 hljs 类的 span 文本。 */
async function tokenTexts(page: Page, index = 0): Promise<string[]> {
    return page.locator('.ck-editor__main .ck-content pre').nth(index).locator('span[class*="hljs-"]').allInnerTexts();
}

async function serverValue(page: Page): Promise<string> {
    return page.locator('#server-value').innerText();
}

/** 把光标放到第一个代码块（JavaScript）最后一行的字符串之后（点击其右边缘，与平台的行尾快捷键无关）。 */
async function caretToEndOfFirstCodeBlock(page: Page): Promise<void> {
    const str = page.locator('.ck-editor__main .ck-content pre').first().locator('span.hljs-string').last();
    const box = (await str.boundingBox())!;
    await str.click({ position: { x: box.width - 1, y: box.height / 2 } });
}

test.describe('代码块语法高亮', () => {
    test('启用后代码块着色，纯文本块不着色，服务端值不含高亮 span', async ({ page }) => {
        await page.goto('/code-highlight');
        await waitForCKEditorReady(page);

        await expect.poll(() => tokenTexts(page, 0)).toEqual(expect.arrayContaining(['const', '42', '// a comment', 'return', '"text"']));
        await expect.poll(() => tokenTexts(page, 1)).toEqual(expect.arrayContaining(['def', 'return']));
        expect(await tokenTexts(page, 2)).toEqual([]);

        const keyword = page.locator('.ck-content pre').first().locator('span.hljs-keyword').first();
        // github 配色的关键字颜色 #d73a49
        await expect(keyword).toHaveCSS('color', 'rgb(215, 58, 73)');
        expect(await serverValue(page)).not.toMatch(/hljs|<span/);
    });

    test('输入时新代码被着色，回传服务端的 HTML 干净', async ({ page }) => {
        await page.goto('/code-highlight');
        await waitForCKEditorReady(page);
        await expect.poll(() => tokenTexts(page, 0)).toContain('const');

        await caretToEndOfFirstCodeBlock(page);
        await page.keyboard.press('Enter');
        await page.keyboard.type('let total = 7;');

        await expect.poll(() => tokenTexts(page, 0)).toEqual(expect.arrayContaining(['let', '7']));
        await expect.poll(() => serverValue(page), { timeout: 15_000 }).toContain('let total = 7;');
        const value = await serverValue(page);
        expect(value).not.toMatch(/hljs|<span/);
        expect(value).toContain('<pre><code class="language-javascript">const answer = 42;');
    });

    test('撤销 / 重做恢复文本，高亮随之更新', async ({ page }) => {
        await page.goto('/code-highlight');
        await waitForCKEditorReady(page);
        await expect.poll(() => tokenTexts(page, 0)).toContain('const');

        await caretToEndOfFirstCodeBlock(page);
        await page.keyboard.press('Enter');
        await page.keyboard.type('var undoMe = 1;');
        await expect.poll(() => tokenTexts(page, 0)).toContain('var');

        // 用工具栏按钮撤销（与平台快捷键无关）；CKEditor 按批次合并键入，逐步回退直到文本消失
        const undo = page.locator('.ck-toolbar button[data-cke-tooltip-text^="Undo"]');
        const redo = page.locator('.ck-toolbar button[data-cke-tooltip-text^="Redo"]');
        for (let i = 0; i < 5 && (await page.locator('.ck-content pre').first().innerText()).includes('undoMe'); i++) {
            await undo.click();
        }
        await expect(page.locator('.ck-content pre').first()).not.toContainText('undoMe');
        await expect.poll(() => tokenTexts(page, 0)).not.toContain('var');
        await expect.poll(() => tokenTexts(page, 0)).toContain('const');

        for (let i = 0; i < 5 && !(await page.locator('.ck-content pre').first().innerText()).includes('undoMe = 1;'); i++) {
            await redo.click();
        }
        await expect(page.locator('.ck-content pre').first()).toContainText('var undoMe = 1;');
        await expect.poll(() => tokenTexts(page, 0)).toContain('var');
        await expect.poll(() => serverValue(page), { timeout: 15_000 }).toContain('var undoMe = 1;');
        expect(await serverValue(page)).not.toMatch(/hljs/);
    });

    test('切换代码块语言重新着色', async ({ page }) => {
        await page.goto('/code-highlight');
        await waitForCKEditorReady(page);
        // 第三个代码块是标为纯文本的 Python 代码：不着色，切换为 Python 后着色
        await expect.poll(() => tokenTexts(page, 0)).toContain('const');
        expect(await tokenTexts(page, 2)).toEqual([]);
        await page.locator('.ck-content pre').nth(2).click();

        const codeBlockButton = page.locator('.ck-toolbar .ck-splitbutton').filter({ hasText: 'Insert code block' });
        await codeBlockButton.locator('.ck-splitbutton__arrow').click();
        await page.locator('.ck-dropdown__panel-visible .ck-list__item button').filter({ hasText: /^Python$/ }).click();
        await expect.poll(() => tokenTexts(page, 2)).toEqual(expect.arrayContaining(['import', 'print']));
        await expect.poll(() => serverValue(page), { timeout: 15_000 })
            .toContain('<pre><code class="language-python">import os\nprint(os.name)</code></pre>');
        expect(await serverValue(page)).not.toMatch(/hljs/);
    });

    test('运行期开关与换配色', async ({ page }) => {
        // 未启用时不下载高亮插件模块与 highlight.js（自定义插件按需加载）
        const chunks: string[] = [];
        page.on('request', (r) => {
            if (/code-block-highlighting|\/common-[^/]*\.js/.test(r.url())) {
                chunks.push(r.url());
            }
        });
        await page.goto('/code-highlight?off=1');
        await waitForCKEditorReady(page);
        await page.waitForTimeout(500);
        expect(await page.locator(SPANS).count()).toBe(0);
        expect(chunks).toEqual([]);

        // 运行中首次启用：编辑器以当前内容重建一次，随后下载插件并着色
        await page.locator('#btn-toggle').click();
        await expect.poll(() => tokenTexts(page, 0)).toContain('const');
        expect(chunks.length).toBeGreaterThan(0);
        expect(await page.locator('.ck-content pre').count()).toBe(3);

        await page.locator('#btn-atom-dark').click();
        const editable = page.locator('.ck-editor__main .ck-content');
        await expect(editable).toHaveClass(/ck-code-hl_theme-atom-one-dark/);
        // atom-one-dark：关键字 #c678dd，代码块底色 #282c34
        await expect(page.locator('.ck-content pre').first().locator('span.hljs-keyword').first()).toHaveCSS('color', 'rgb(198, 120, 221)');
        await expect(page.locator('.ck-content pre').first()).toHaveCSS('background-color', 'rgb(40, 44, 52)');

        await page.locator('#btn-toggle').click();
        await expect.poll(() => page.locator(SPANS).count()).toBe(0);
        await expect(editable).not.toHaveClass(/ck-code-hl/);
    });

    test('暗色编辑器下 auto 配色使用 github-dark', async ({ page }) => {
        await page.goto('/code-highlight?dark=1');
        await waitForCKEditorReady(page);
        const editable = page.locator('.ck-editor__main .ck-content');
        await expect(editable).toHaveClass(/ck-code-hl_dark/);
        // github-dark：关键字 #ff7b72
        const keyword = page.locator('.ck-content pre').first().locator('span.hljs-keyword').first();
        await expect(keyword).toHaveCSS('color', 'rgb(255, 123, 114)');

        // 应用可用任意选择器覆盖配色变量（内置配色以 :where() 声明，零特异性）
        await page.addStyleTag({ content: '.ck-code-hl { --ck-code-hl-keyword: rgb(1, 2, 3); }' });
        await expect(keyword).toHaveCSS('color', 'rgb(1, 2, 3)');
    });

    test('性能：大代码块的首次着色、按键着色，以及与未启用时的按键开销对比', async ({ page, browserName }) => {
        test.skip(browserName !== 'chromium', '只在 Chromium 上测量');

        /** 在第 4 个代码块（夹具追加的大代码块）开头插入一个字符，返回模型改动（含视图转换与渲染）与重新着色的耗时。 */
        const keystroke = () => page.evaluate(() => {
            type Ed = {
                plugins: { has(n: string): boolean; get(n: string): { lastHighlightMs: number; flush(): void } };
                model: {
                    document: { getRoot(): { getChild(i: number): unknown } };
                    createPositionAt(el: unknown, offset: number): unknown;
                    change(cb: (w: { insertText(t: string, p: unknown): void }) => void): void;
                };
            };
            const editor = (document.querySelector('vaadin-ckeditor#ckeditor') as unknown as { editor: Ed }).editor;
            const block = editor.model.document.getRoot().getChild(4);
            const started = performance.now();
            editor.model.change((w) => w.insertText('x', editor.model.createPositionAt(block, 0)));
            const insert = performance.now() - started;
            // 未启用时插件根本不加载（基线）
            if (!editor.plugins.has('VaadinCodeBlockHighlighting')) {
                return { insert, highlight: 0 };
            }
            const plugin = editor.plugins.get('VaadinCodeBlockHighlighting');
            plugin.flush();
            return { insert, highlight: plugin.lastHighlightMs };
        });

        for (const lines of [100, 500, 1500]) {
            await page.goto(`/code-highlight?off=1&lines=${lines}`);
            await waitForCKEditorReady(page);
            const baseline = await keystroke();

            await page.goto(`/code-highlight?lines=${lines}`);
            await waitForCKEditorReady(page);
            const big = page.locator('.ck-content pre').nth(3);
            await expect.poll(() => big.locator('span.hljs-keyword').count(), { timeout: 30_000 }).toBeGreaterThan(lines);
            const first = await page.evaluate(() => (document.querySelector('vaadin-ckeditor#ckeditor') as unknown as {
                editor: { plugins: { get(n: string): { lastHighlightMs: number } } };
            }).editor.plugins.get('VaadinCodeBlockHighlighting').lastHighlightMs);
            const highlighted = await keystroke();

            console.log(`[perf] ${lines} lines: first highlight ${first.toFixed(0)}ms; keystroke without highlighting ${baseline.insert.toFixed(1)}ms; `
                + `with highlighting ${highlighted.insert.toFixed(1)}ms + re-highlight ${highlighted.highlight.toFixed(1)}ms`);
            // 预算（本机实测见 USER_GUIDE「Code block syntax highlighting」，CI 机器放宽约 3 倍）
            expect(highlighted.highlight).toBeLessThan(lines >= 1500 ? 300 : 120);
            expect(await serverValue(page)).not.toMatch(/hljs/);
        }
    });
});
