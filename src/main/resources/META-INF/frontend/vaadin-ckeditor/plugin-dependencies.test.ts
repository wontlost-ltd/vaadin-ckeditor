/**
 * 防漂移：Java 端的插件依赖表、插件枚举与前端插件表，对照真实安装的 ckeditor5 49.1.0（以及
 * ckeditor5-premium-features 49.1.0 的 requires 快照）。升级 CKEditor 时，这里会指出哪些条目需要重新核对。
 *
 * 依赖表（CKEditorPluginDependencies）中的每一条 A → B 必须满足其一：
 *   1. B 在 A 真实的 requires 闭包里（CKEditor 本来就会加载它，列出只让插件集合与 Java 端一致）；
 *   2. B 在下面 DOCUMENTED_EDGES 中：requires 里没有，但 CKEditor 文档的安装示例把它与 A 一起加载，并附文档链接。
 * GeneralHtmlSupport 不允许以第 2 种方式出现：强制加载它会改变 getData() 的输出（W13）。
 *
 * premium 包不在本目录的依赖中（vitest 用 test-mocks 替身），所以 premium 插件的 requires 闭包以
 * test-mocks/premium-requires.json 快照对照。重新生成：在任意目录 `npm i ckeditor5-premium-features@<版本>`，然后
 * `CKEDITOR_PREMIUM_DIR=<该目录>/node_modules CKEDITOR_PREMIUM_UPDATE=1 npx vitest run plugin-dependencies.test.ts`；
 * 只设 CKEDITOR_PREMIUM_DIR 时与真实包比对而不写入。
 */
import { describe, it, expect } from 'vitest';
import * as ckeditor5 from 'ckeditor5';
import dependenciesSource from '../../../../java/com/wontlost/ckeditor/CKEditorPluginDependencies.java?raw';
import pluginSource from '../../../../java/com/wontlost/ckeditor/CKEditorPlugin.java?raw';
import premiumSource from '../../../../java/com/wontlost/ckeditor/VaadinCKEditorPremium.java?raw';
import premiumSnapshot from './test-mocks/premium-requires.json';
import { PLUGIN_REGISTRY, PLUGINS_REQUIRING_CONFIG, MUTUALLY_EXCLUSIVE_PLUGINS } from './plugin-resolver';
import packageJson from './package.json';

// 本目录不依赖 @types/node：只声明 live 模式用到的 Node API（vitest 在 Node 中运行）
declare const process: { env: Record<string, string | undefined>; cwd(): string };
type NodeFs = { readFileSync(path: string, encoding: 'utf8'): string; writeFileSync(path: string, data: string): void };

type Ctor = { pluginName?: string; requires?: unknown[]; licenseFeatureCode?: string };
type Module = Record<string, unknown>;

const IMAGES = 'https://ckeditor.com/docs/ckeditor5/latest/features/images/images-installation.html';
const TABLES = 'https://ckeditor.com/docs/ckeditor5/latest/features/tables/tables.html';
const LISTS = 'https://ckeditor.com/docs/ckeditor5/latest/features/lists/lists.html';
const SPECIAL = 'https://ckeditor.com/docs/ckeditor5/latest/features/special-characters.html';
const UPLOADCARE = 'https://ckeditor.com/docs/ckeditor5/latest/features/file-management/uploadcare.html';

/** requires 里没有、但 CKEditor 文档与之一起安装的依赖：`A → B` → 文档链接。 */
const DOCUMENTED_EDGES: Record<string, string> = {
    'ImageToolbar → Image': IMAGES,
    'ImageCaption → Image': IMAGES,
    'ImageStyle → Image': IMAGES,
    'ImageResize → Image': IMAGES,
    'ImageUpload → Image': IMAGES,
    'ImageInsert → Image': IMAGES,
    'ImageBlock → Image': IMAGES,
    'ImageInline → Image': IMAGES,
    'LinkImage → Image': IMAGES,
    'LinkImage → Link': IMAGES,
    'AutoImage → Image': IMAGES,
    'TableToolbar → Table': TABLES,
    'TableProperties → Table': TABLES,
    'TableCellProperties → Table': TABLES,
    'TableCaption → Table': TABLES,
    'TableColumnResize → Table': TABLES,
    'TableLayout → Table': 'https://ckeditor.com/docs/ckeditor5/latest/features/tables/layout-tables.html',
    'AutoLink → Link': 'https://ckeditor.com/docs/ckeditor5/latest/features/link.html',
    'TodoList → List': 'https://ckeditor.com/docs/ckeditor5/latest/features/lists/todo-lists.html',
    'ListProperties → List': 'https://ckeditor.com/docs/ckeditor5/latest/features/lists/lists-properties.html',
    'ListFormatting → List': LISTS,
    'AdjacentListsSupport → List': LISTS,
    'IndentBlock → Indent': 'https://ckeditor.com/docs/ckeditor5/latest/features/indent.html',
    'SpecialCharactersEssentials → SpecialCharacters': SPECIAL,
    'SpecialCharactersArrows → SpecialCharacters': SPECIAL,
    'SpecialCharactersCurrency → SpecialCharacters': SPECIAL,
    'SpecialCharactersLatin → SpecialCharacters': SPECIAL,
    'SpecialCharactersMathematical → SpecialCharacters': SPECIAL,
    'SpecialCharactersText → SpecialCharacters': SPECIAL,
    'SimpleUploadAdapter → ImageUpload': 'https://ckeditor.com/docs/ckeditor5/latest/features/images/image-upload/simple-upload-adapter.html',
    'Base64UploadAdapter → ImageUpload': 'https://ckeditor.com/docs/ckeditor5/latest/features/images/image-upload/base64-upload-adapter.html',
    'CKBox → ImageUpload': 'https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckbox.html',
    'CKBoxImageEdit → CKBox': 'https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckbox.html',
    // premium
    'MultiLevelList → List': 'https://ckeditor.com/docs/ckeditor5/latest/features/lists/multi-level-lists.html',
    'MergeFields → Mention': 'https://ckeditor.com/docs/ckeditor5/latest/features/merge-fields.html',
    'Uploadcare → Image': UPLOADCARE,
    'Uploadcare → ImageUpload': UPLOADCARE,
    'Uploadcare → ImageInsert': UPLOADCARE,
    'UploadcareImageEdit → Image': UPLOADCARE,
    'UploadcareImageEdit → ImageToolbar': UPLOADCARE,
    'ProposedChanges → DocumentCompare':
        'https://ckeditor.com/docs/ckeditor5/latest/features/editing-toolkit/compare/editing-toolkit-proposing-changes.html',
};

// ---------- Java 源码解析 ----------

const constants = new Map([...pluginSource.matchAll(/^\s{4}([A-Z0-9_]+)\("([A-Za-z0-9]+)"/gm)].map((m) => [m[1], m[2]]));
const jsName = (constant: string) => {
    const name = constants.get(constant.replace('CKEditorPlugin.', '').trim());
    if (!name) throw new Error(`unknown constant ${constant}`);
    return name;
};
const listOf = (body: string) => body.split(',').map((s) => s.trim()).filter(Boolean);

/** DEPENDENCIES：deps.put(CKEditorPlugin.A, Set.of(CKEditorPlugin.B, ...)) */
const freeEdges: [string, string][] = [...dependenciesSource.matchAll(/deps\.put\(CKEditorPlugin\.([A-Z0-9_]+), Set\.of\(([^)]*)\)\)/g)]
    .flatMap((m) => listOf(m[2]).map((dep) => [jsName(m[1]), jsName(dep)] as [string, string]));

/** PREMIUM_DEPENDENCIES：premDeps.put("X", cloudServicesDeps | Set.of(CKEditorPlugin.B, ...)) */
const premiumEdges: [string, string][] = [...dependenciesSource.matchAll(/premDeps\.put\("([A-Za-z]+)", ([^;]*)\);/g)]
    .flatMap((m) => {
        const deps = m[2].trim() === 'cloudServicesDeps' ? ['CLOUD_SERVICES'] : listOf(m[2].replace(/^Set\.of\(|\)$/g, ''));
        return deps.map((dep) => [m[1], jsName(dep)] as [string, string]);
    });

/** PREMIUM_PLUGIN_DEPENDENCIES："X", Set.of("Y", ...) */
const premiumPluginEdges: [string, string][] = (() => {
    const block = dependenciesSource.match(/PREMIUM_PLUGIN_DEPENDENCIES = Map\.of\(([\s\S]*?)\n\s*\);/)![1];
    return [...block.matchAll(/"([A-Za-z]+)", Set\.of\(([^)]*)\)/g)]
        .flatMap((m) => listOf(m[2]).map((dep) => [m[1], dep.replace(/"/g, '')] as [string, string]));
})();

const premiumEnumNames = [...premiumSource.matchAll(/^\s+[A-Z_]+\("([A-Za-z]+)"/gm)].map((m) => m[1]);
const premiumVersion = premiumSource.match(/@NpmPackage\(value = "ckeditor5-premium-features", version = "([^"]+)"\)/)![1];

// ---------- requires 闭包 ----------

function requiresClosure(module: Module, root: string): Set<string> {
    const nameOf = new Map<unknown, string>();
    for (const [key, value] of Object.entries(module)) {
        if (typeof value === 'function') nameOf.set(value, (value as Ctor).pluginName ?? key);
    }
    const result = new Set<string>();
    const visit = (ctor: Ctor | undefined) => {
        for (const dep of ctor?.requires ?? []) {
            const name = typeof dep === 'string' ? dep : nameOf.get(dep) ?? (dep as Ctor).pluginName ?? (dep as { name: string }).name;
            if (result.has(name)) continue;
            result.add(name);
            visit(typeof dep === 'function' ? dep as Ctor : module[name] as Ctor | undefined);
        }
    };
    visit(module[root] as Ctor | undefined);
    return result;
}

const umbrella = ckeditor5 as unknown as Module;

const edgeKey = ([from, to]: [string, string]) => `${from} → ${to}`;

describe('CKEditorPluginDependencies 对照 ckeditor5 49.1.0 的 requires', () => {
    it('解析到 Java 端的依赖表', () => {
        expect(freeEdges.length).toBeGreaterThan(30);
        expect(premiumEdges).toContainEqual(['ExportPdf', 'CloudServices']);
        expect(premiumPluginEdges).toEqual([['ProposedChanges', 'DocumentCompare']]);
        expect(packageJson.dependencies.ckeditor5).toBe('49.1.0');
    });

    it.each(freeEdges.map(edgeKey))('%s：在 requires 闭包里，或有文档依据', (key) => {
        const [from, to] = key.split(' → ');
        const inRequires = requiresClosure(umbrella, from).has(to);
        expect(inRequires || key in DOCUMENTED_EDGES, `${key} is neither in ${from}.requires nor documented`).toBe(true);
    });

    it('不强制加载会改变输出的 GeneralHtmlSupport（只有真实 requires 它的插件可以）', () => {
        for (const [from, to] of freeEdges.filter(([, to]) => to === 'GeneralHtmlSupport')) {
            expect(requiresClosure(umbrella, from).has(to), `${from} → ${to}`).toBe(true);
        }
        expect(Object.keys(DOCUMENTED_EDGES).filter((k) => k.endsWith('→ GeneralHtmlSupport'))).toEqual([]);
        expect(freeEdges.filter(([from]) => ['HtmlComment', 'HtmlEmbed', 'SourceEditing'].includes(from))).toEqual([]);
    });

    it('互斥的插件不互为依赖', () => {
        for (const group of MUTUALLY_EXCLUSIVE_PLUGINS) {
            const inGroup = freeEdges.filter(([from, to]) => group.includes(from) && group.includes(to));
            expect(inGroup, group.join(' / ')).toEqual([]);
        }
    });

    it('文档依据表中没有过期条目', () => {
        const all = new Set([...freeEdges, ...premiumEdges, ...premiumPluginEdges].map(edgeKey));
        expect(Object.keys(DOCUMENTED_EDGES).filter((k) => !all.has(k))).toEqual([]);
    });
});

describe('CKEditorPlugin 枚举与前端插件表对照 ckeditor5 49.1.0 的导出', () => {
    it.each([...constants.entries()])('%s (%s) 是 ckeditor5 导出的插件，且在 PLUGIN_REGISTRY 中', (_constant, name) => {
        expect(typeof umbrella[name], name).toBe('function');
        expect(PLUGIN_REGISTRY[name]).toBe(umbrella[name]);
    });

    it('PLUGIN_REGISTRY 的每一项都是同名的 ckeditor5 导出', () => {
        for (const [name, ctor] of Object.entries(PLUGIN_REGISTRY)) {
            expect(ctor, name).toBe(umbrella[name]);
        }
    });

    it('“需要配置”与“互斥”名单里只有真实导出', () => {
        for (const name of [...PLUGINS_REQUIRING_CONFIG, ...MUTUALLY_EXCLUSIVE_PLUGINS.flat()]) {
            expect(typeof umbrella[name], name).toBe('function');
        }
        // W15：Title 只 requires Paragraph，不需要配置
        expect(PLUGINS_REQUIRING_CONFIG).not.toContain('Title');
        expect(requiresClosure(umbrella, 'Title')).toEqual(new Set(['Paragraph']));
    });
});

describe('premium 依赖对照 ckeditor5-premium-features 的 requires 快照', () => {
    const snapshot = premiumSnapshot as { version: string; exports: string[]; requires: Record<string, string[]> };

    it('快照与 @NpmPackage 版本一致', () => {
        expect(snapshot.version).toBe(premiumVersion);
    });

    it('Java 端列出的 premium 名称都是 premium 包的导出', () => {
        const names = new Set([...premiumEnumNames, ...premiumEdges.map(([f]) => f), ...premiumPluginEdges.flat()]);
        expect([...names].filter((n) => !snapshot.exports.includes(n))).toEqual([]);
    });

    it.each([...premiumEdges, ...premiumPluginEdges].map(edgeKey))('%s：在 requires 闭包里，或有文档依据', (key) => {
        const [from, to] = key.split(' → ');
        const inRequires = (snapshot.requires[from] ?? []).includes(to);
        expect(inRequires || key in DOCUMENTED_EDGES, `${key} is neither in ${from}.requires nor documented`).toBe(true);
    });

    // 设置 CKEDITOR_PREMIUM_DIR 时与真实包比对（CKEDITOR_PREMIUM_UPDATE=1 时重写快照）
    it.runIf(!!process.env.CKEDITOR_PREMIUM_DIR)('快照与真实 premium 包一致', async () => {
        const dir = process.env.CKEDITOR_PREMIUM_DIR!.replace(/\/$/, '');
        const fs = await import(/* @vite-ignore */ ['node', 'fs'].join(':')) as NodeFs;
        const pkg = JSON.parse(fs.readFileSync(`${dir}/ckeditor5-premium-features/package.json`, 'utf8')) as { version: string };
        // 该目录自带一份 ckeditor5：只读取类的静态 requires，不创建编辑器，所以暂时绕过“重复加载”检查
        const global = globalThis as { CKEDITOR_VERSION?: string };
        const version = global.CKEDITOR_VERSION;
        delete global.CKEDITOR_VERSION;
        const module = await import(/* @vite-ignore */ `${dir}/ckeditor5-premium-features/dist/ckeditor5-premium-features.js`)
            .finally(() => { global.CKEDITOR_VERSION = version; }) as Module;
        const exports = Object.keys(module).filter((k) => typeof module[k] === 'function').sort();
        const names = [...new Set([...premiumEnumNames, ...premiumEdges.map(([f]) => f), ...premiumPluginEdges.map(([f]) => f)])].sort();
        const requires = Object.fromEntries(names.map((n) => [n, [...requiresClosure(module, n)].sort()]));
        const live = { version: pkg.version, exports, requires };
        if (process.env.CKEDITOR_PREMIUM_UPDATE === '1') {
            fs.writeFileSync(`${process.cwd()}/test-mocks/premium-requires.json`, JSON.stringify(live, null, 2) + '\n');
        }
        expect(live).toEqual(snapshot);
    }, 60_000);
});
