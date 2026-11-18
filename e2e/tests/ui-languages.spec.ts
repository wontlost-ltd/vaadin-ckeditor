import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { expect, test } from '@playwright/test';
import { waitForEditorHost } from '../helpers/ck';

/**
 * UI 语言矩阵：CKEditor 5 随包发布的每个翻译包（ckeditor5/dist/translations/*.js），
 * 加上官方专业翻译列表里与包文件名不一致的代码，逐一在 /language?lang=xx 夹具上验证：
 *
 *  1. add-on 接受该代码（Builder 不拒绝，夹具不渲染 #language-rejected）；
 *  2. 工具栏按钮标签与翻译包字典一致（字典缺词时回落英文原文，这是 CKEditor 自身行为）；
 *  3. 编辑器根节点的 lang / dir 与预期一致（ar、fa、he、ku、ug、ur 为 RTL）；
 *  4. 期间没有控制台错误、没有失败的脚本请求。
 *
 * 期望值的来源是 e2e 自己安装的 ckeditor5（版本由下方守卫测试与 add-on 钉住的版本对齐），
 * 不从被测页面里读，避免"拿被测物当判据"。
 */

const CK_DIST = dirname(fileURLToPath(import.meta.resolve('ckeditor5')));
const TRANSLATIONS_DIR = join(CK_DIST, 'translations');
const ADDON_FRONTEND_PACKAGE = new URL(
    '../../src/main/resources/META-INF/frontend/vaadin-ckeditor/package.json', import.meta.url);

/** 与 @ckeditor/ckeditor5-utils 的 RTL_LANGUAGE_CODES 对齐（只列两字母代码） */
const RTL = new Set(['ar', 'dv', 'fa', 'he', 'ku', 'ug', 'ur']);

/** 包文件名（不含 en，英文是 CKEditor 的源语言） */
const BUNDLED = readdirSync(TRANSLATIONS_DIR)
    .filter(f => f.endsWith('.js') && !f.endsWith('.umd.js'))
    .map(f => f.slice(0, -3))
    .filter(code => code !== 'en')
    .sort();

/**
 * 额外输入 → 期望落到的翻译包代码。
 * zh-tw：官方专业翻译列表用 zh-tw 表示繁体（台湾），但包里繁体文件名是 zh.js。
 * pt-BR / ZH-CN：大小写混写，验证 add-on 归一化。
 */
const EXTRA: Record<string, string> = {
    'en': 'en',
    'zh-tw': 'zh',
    'pt-BR': 'pt-br',
    'ZH-CN': 'zh-cn',
};

const CASES: Array<[input: string, bundle: string]> = [
    ...BUNDLED.map(code => [code, code] as [string, string]),
    ...Object.entries(EXTRA),
];

/** 工具栏上要核对的英文标签（BASIC 预设） */
const LABELS = ['Bold', 'Italic', 'Underline', 'Link', 'Bulleted List', 'Numbered List', 'Block quote', 'Undo', 'Redo'];

async function loadDictionary(code: string): Promise<Record<string, unknown>> {
    if (code === 'en') {
        return {};
    }
    const mod = await import(pathToFileURL(join(TRANSLATIONS_DIR, `${code}.js`)).href);
    const entry = (mod.default as Record<string, { dictionary: Record<string, unknown> }>)[code];
    return entry?.dictionary ?? {};
}

/** CKEditor 的取词规则：字典缺词或为空串时显示英文原文；复数词条取单数形式 */
function translationOf(dictionary: Record<string, unknown>, key: string): string {
    const raw = dictionary[key];
    const value = Array.isArray(raw) ? raw[0] : raw;
    return typeof value === 'string' && value !== '' ? value : key;
}

test('e2e ckeditor5 reference version matches the add-on pin', () => {
    const addonPin = JSON.parse(readFileSync(ADDON_FRONTEND_PACKAGE, 'utf8')).dependencies.ckeditor5;
    const reference = JSON.parse(readFileSync(join(CK_DIST, '..', 'package.json'), 'utf8')).version;
    expect(reference, 'bump e2e devDependency ckeditor5 together with the add-on').toBe(addonPin);
});

test.describe('UI languages', () => {
    for (const [input, bundle] of CASES) {
        test(`ui language ${input}`, async ({ page }, testInfo) => {
            const consoleErrors: string[] = [];
            const failedRequests: string[] = [];
            page.on('console', msg => {
                if (msg.type() === 'error') {
                    consoleErrors.push(msg.text());
                }
            });
            page.on('pageerror', err => consoleErrors.push(err.message));
            page.on('requestfailed', req => failedRequests.push(req.url()));
            page.on('response', res => {
                if (res.status() >= 400) {
                    failedRequests.push(`${res.status()} ${res.url()}`);
                }
            });

            await page.goto(`/language?lang=${encodeURIComponent(input)}`);
            await expect(page.locator('#language-rejected, vaadin-ckeditor#ckeditor').first()).toBeAttached({ timeout: 30_000 });
            const rejected = page.locator('#language-rejected');
            if (await rejected.count() > 0) {
                throw new Error(`add-on rejected '${input}': ${await rejected.textContent()}`);
            }

            await waitForEditorHost(page);
            const root = page.locator('vaadin-ckeditor#ckeditor .ck.ck-editor').first();
            await root.waitFor({ state: 'visible', timeout: 30_000 });

            const dictionary = await loadDictionary(bundle);
            const toolbarLabels = await page.locator('vaadin-ckeditor#ckeditor .ck-toolbar .ck-button__label').allTextContents();

            const expected: string[] = [];
            const actual: string[] = [];
            let translatedCount = 0;
            for (const label of LABELS) {
                const want = translationOf(dictionary, label);
                if (want !== label) {
                    translatedCount++;
                }
                expected.push(want);
                actual.push(toolbarLabels.includes(want) ? want : `<missing ${want}>`);
            }

            // 运行时探针：取字典里第一个真正译出的词条，经编辑器自身的 locale.t() 翻译。
            // 部分包（hi、en-au、en-gb）恰好没译 BASIC 工具栏标签，靠它证明包确实生效。
            const probeKey = Object.keys(dictionary)
                .find(key => typeof dictionary[key] === 'string' && translationOf(dictionary, key) !== key);
            const probe = probeKey === undefined ? undefined : await page.locator('vaadin-ckeditor#ckeditor')
                .evaluate((host, key) => (host as unknown as { editor: { locale: { t: (k: string) => string } } })
                    .editor.locale.t(key), probeKey);

            const lang = await root.getAttribute('lang');
            const dir = await root.getAttribute('dir');
            const expectedDir = RTL.has(bundle.split('-')[0]) ? 'rtl' : 'ltr';

            const result = {
                input, bundle, lang, dir, expectedDir, translatedCount, probeKey, probe,
                labels: toolbarLabels, consoleErrors, failedRequests,
            };
            testInfo.annotations.push({ type: 'ui-language', description: JSON.stringify(result) });

            expect(actual, `toolbar labels for '${input}' (bundle ${bundle})`).toEqual(expected);
            expect(lang, 'editor lang attribute').toBe(bundle);
            expect(dir, 'editor dir attribute').toBe(expectedDir);
            if (bundle !== 'en') {
                expect(probeKey, `bundle ${bundle} has no translated entry`).toBeDefined();
                expect(probe, `runtime translation of '${probeKey}'`).toBe(translationOf(dictionary, probeKey!));
            }
            expect(consoleErrors, 'console errors').toEqual([]);
            expect(failedRequests, 'failed requests').toEqual([]);
        });
    }
});
