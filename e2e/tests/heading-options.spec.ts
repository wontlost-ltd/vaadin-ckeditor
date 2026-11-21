import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 标题选项（issues #158 / #159）。夹具 /heading：
 * 普通 h1–h3、与 h1 同级的变体「H1 článok」（model heading1Article，h1.style1）、
 * 只加类名的「H4 článok」（h4.style4）。
 */

/** 夹具初始内容（HeadingView.INITIAL_VALUE）读入后每个块的模型元素与输出：带类名的标题须匹配到各自的变体 */
const EXPECTED_BLOCKS = [
    { model: 'heading1Article', tag: 'H1', className: 'style1', text: 'Article' },
    { model: 'heading1', tag: 'H1', className: '', text: 'Plain' },
    { model: 'heading4', tag: 'H4', className: 'style4', text: 'Section' },
    { model: 'paragraph', tag: 'P', className: '', text: 'Body' },
];

interface CKHost extends HTMLElement {
    editor: {
        getData(): string;
        model: {
            document: { getRoot(): { getChild(i: number): unknown; getChildren(): Iterable<{ name: string }> } };
            change(cb: (writer: { setSelection(el: unknown, where: string): void }) => void): void;
        };
        editing: { view: { focus(): void } };
    };
}

async function open(page: Page): Promise<string[]> {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
    await page.goto('/heading');
    await waitForCKEditorReady(page);
    return errors;
}

/** 模型根下各块的元素名，以及 getData() 解析后各块的标签、类名、文本 */
async function blocks(page: Page) {
    return page.evaluate(() => {
        const editor = (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost).editor;
        const models = Array.from(editor.model.document.getRoot().getChildren(), (el) => el.name);
        const parsed = new DOMParser().parseFromString(editor.getData(), 'text/html').body.children;
        return Array.from(parsed, (el, i) => ({
            model: models[i], tag: el.tagName, className: el.className, text: el.textContent,
        }));
    });
}

/** 把选区放进第 index 个块（模型根的子元素）里 */
async function selectBlock(page: Page, index: number): Promise<void> {
    await page.evaluate((i) => {
        const editor = (document.querySelector('vaadin-ckeditor#ckeditor') as CKHost).editor;
        editor.editing.view.focus();
        editor.model.change((writer) => writer.setSelection(editor.model.document.getRoot().getChild(i), 'in'));
    }, index);
}

async function chooseHeading(page: Page, title: string): Promise<void> {
    const dropdown = page.locator('vaadin-ckeditor#ckeditor .ck-heading-dropdown');
    await dropdown.locator('.ck-dropdown__button').click();
    await page.locator('.ck-heading-dropdown .ck-dropdown__panel .ck-list__item .ck-button')
        .filter({ hasText: new RegExp(`^${title}$`) })
        .click();
}

test('editor initializes with two options of the same level and keeps their classes (#158)', async ({ page }) => {
    const errors = await open(page);
    // 读入时 <h1 class="style1"> 匹配到变体 heading1Article（而非普通 heading1），往返后类名不丢失
    expect(await blocks(page)).toEqual(EXPECTED_BLOCKS);
    expect(errors.filter((e) => /schema-cannot-register-item-twice|CKEditorError/.test(e))).toEqual([]);

    await page.locator('vaadin-ckeditor#ckeditor .ck-heading-dropdown .ck-dropdown__button').click();
    const items = page.locator('.ck-heading-dropdown .ck-dropdown__panel .ck-list__item .ck-button__label');
    await expect(items).toHaveText(['Normálny', 'Nadpis 1', 'Nadpis 2', 'Nadpis 3', 'H1 článok', 'H4 článok']);
});

test('heading options with view classes write the classes into the content (#159)', async ({ page }) => {
    await open(page);
    const BODY = 3;

    await selectBlock(page, BODY);
    await chooseHeading(page, 'H1 článok');
    expect((await blocks(page))[BODY]).toEqual({ model: 'heading1Article', tag: 'H1', className: 'style1', text: 'Body' });

    await selectBlock(page, BODY);
    await chooseHeading(page, 'Nadpis 1');
    expect((await blocks(page))[BODY]).toEqual({ model: 'heading1', tag: 'H1', className: '', text: 'Body' });

    await selectBlock(page, BODY);
    await chooseHeading(page, 'H4 článok');
    expect((await blocks(page))[BODY]).toEqual({ model: 'heading4', tag: 'H4', className: 'style4', text: 'Body' });

    // 选中带类名的标题时，下拉按钮显示变体自己的标题
    await selectBlock(page, 0);
    await expect(page.locator('vaadin-ckeditor#ckeditor .ck-heading-dropdown .ck-dropdown__button .ck-button__label'))
        .toHaveText('H1 článok');
});
