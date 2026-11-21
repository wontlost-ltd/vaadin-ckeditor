import { expect, test, type Page } from '@playwright/test';
import { waitForCKEditorReady } from '../helpers/ck';

/**
 * 实时字数与上限（#70）。夹具 /word-count：BASIC 预设（不含 WordCount，由 add-on 自动加载），
 * 限 5 词 / 40 字符，显示计数，绑定 Binder。
 */

interface CKHost extends HTMLElement {
    editor: {
        setData(html: string): void;
        plugins: { get(name: string): { words: number; characters: number } };
    };
}

const HOST = 'vaadin-ckeditor#ckeditor';

async function open(page: Page) {
    const errors: string[] = [];
    page.on('pageerror', (e) => errors.push(e.message));
    await page.goto('/word-count');
    const editable = await waitForCKEditorReady(page);
    return { editable, errors };
}

test('count updates while typing; counter, event and server agree', async ({ page }) => {
    const { editable, errors } = await open(page);
    // 初始计数随编辑器就绪上报
    await expect(page.locator('#count')).toHaveText('words=0 chars=0');

    await editable.click();
    await page.keyboard.type('one two three');

    await expect(page.locator('#count')).toHaveText('words=3 chars=13');
    await expect(page.locator(`${HOST} .word-count-container .ck-word-count__words`)).toHaveText('Words: 3');
    await expect(page.locator(`${HOST} .word-count-container .ck-word-count__characters`)).toHaveText('Characters: 13');
    await expect(page.locator('#server-stats')).toHaveText('words=3 chars=13');
    expect(errors).toEqual([]);
});

test('events are throttled to at most one per 500 ms', async ({ page }) => {
    const { editable } = await open(page);
    await expect(page.locator('#events')).toHaveText('1');      // 初始计数
    await editable.click();
    const started = Date.now();
    await page.keyboard.type('abcdefghijklmnopqrstuvwxyz abcdefghij', { delay: 40 });
    const elapsed = Date.now() - started;
    await expect(page.locator('#count')).toHaveText('words=2 chars=37');
    const events = Number(await page.locator('#events').textContent()) - 1;
    // 每 500 ms 至多一次，外加窗口末尾的补发；37 次按键远多于此
    expect(events).toBeGreaterThan(1);
    expect(events).toBeLessThanOrEqual(Math.ceil(elapsed / 500) + 1);
});

test('field and Binder become invalid over the word limit and valid again', async ({ page }) => {
    const { editable } = await open(page);
    const host = page.locator(HOST);
    await editable.click();
    await page.keyboard.type('one two three four five');
    await expect(page.locator('#server-stats')).toHaveText('words=5 chars=23');
    await expect(host).not.toHaveAttribute('invalid', /.*/);

    await page.keyboard.type(' six');
    await expect(host).toHaveAttribute('invalid', '');
    await expect(host.locator('.vaadin-ckeditor-error-message')).toHaveText('Maximum 5 words allowed (6 entered)');
    await expect(host.locator('.word-count-container')).toHaveClass(/word-count-container--over-limit/);
    await expect(host.locator('.ck-editor__editable')).toHaveAttribute('aria-invalid', 'true');
    // 不阻止输入：超限后仍可继续输入
    await page.keyboard.type(' seven');
    await expect(page.locator('#count')).toHaveText('words=7 chars=33');

    await page.locator('#btn-validate').click();
    await expect(page.locator('#binder-status')).toHaveText('Maximum 5 words allowed (7 entered)');

    // 按钮拿走了焦点：回到编辑区末尾再删
    await editable.click();
    await page.keyboard.press('ControlOrMeta+End');
    for (let i = 0; i < ' six seven'.length; i++) {
        await page.keyboard.press('Backspace');
    }
    await expect(host).not.toHaveAttribute('invalid', /.*/);
    await expect(host.locator('.vaadin-ckeditor-error-message')).toBeHidden();
    await expect(host.locator('.word-count-container')).not.toHaveClass(/over-limit/);
    await page.locator('#btn-validate').click();
    await expect(page.locator('#binder-status')).toHaveText('valid');
});

test('character limit marks the field invalid', async ({ page }) => {
    const { editable } = await open(page);
    await editable.click();
    await page.keyboard.type('supercalifragilistic expialidocious word');   // 3 词，40 字符
    await expect(page.locator('#server-stats')).toHaveText('words=3 chars=40');
    await expect(page.locator(HOST)).not.toHaveAttribute('invalid', /.*/);
    await page.keyboard.type('s');
    await expect(page.locator(`${HOST} .vaadin-ckeditor-error-message`))
        .toHaveText('Maximum 40 characters allowed (41 entered)');
});

/** 服务端计数与浏览器中 WordCount 插件逐条一致 */
const CORPUS = [
    '<p>Hello world</p>',
    '<p>Hello</p><p>world</p>',
    '<p>a <strong>b</strong> <em>c</em>d</p>',
    '<p>line<br>break and&nbsp;&nbsp;spaces</p>',
    '<ul><li>one</li><li>two three</li></ul><ol><li>four</li></ol>',
    '<h2>Title</h2><blockquote><p>quoted text</p></blockquote>',
    '<p>don\'t stop-me now... 3.14 isn\'t pi!</p>',
    '<p>你好世界 こんにちは 안녕하세요</p>',
    '<p>héllo wörld 123 emoji 😀 here</p>',
    '<p>   </p><p>after empty</p>',
    '<blockquote><p>a</p><p>b c</p></blockquote><p>tail</p>',
    '<p>x<code>y z</code> <i>w</i><u>v</u></p>',
    '<ul><li><p>nested</p><ul><li>deep item</li></ul></li></ul>',
    '<pre><code>  indented\n    code();</code></pre>',
    '<p>tab\tand\nnewline in source</p>',
];

test('server-side counting matches the WordCount plugin', async ({ page }) => {
    await open(page);
    for (const html of CORPUS) {
        const client = await page.evaluate(([selector, data]) => {
            const editor = (document.querySelector(selector) as CKHost).editor;
            editor.setData(data);
            const plugin = editor.plugins.get('WordCount');
            return `words=${plugin.words} chars=${plugin.characters}`;
        }, [HOST, html] as const);
        await expect(page.locator('#server-stats'), html).toHaveText(client);
    }
});
