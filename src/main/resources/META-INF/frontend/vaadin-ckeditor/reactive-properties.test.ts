/// <reference path="./test-mocks/raw-imports.d.ts" />
import { describe, it, expect, vi, afterEach } from 'vitest';
import { transformWithOxc } from 'vite';
import SOURCE from './vaadin-ckeditor.ts?raw';

/**
 * 反应式属性声明方式的回归测试（issue #120）。
 *
 * 连接器以源 TS 分发，由消费者的 Vite 实时转译：
 * - 不得使用装饰器，否则 rolldown 会生成 `@oxc-project/runtime` 的 helper 导入，
 *   在非根 context path 下 404；
 * - 不得使用带初值的类字段，否则在 `useDefineForClassFields: true`（或无 tsconfig）
 *   下转译成 [[Define]] 语义，实例 own property 遮蔽 Lit 访问器，属性变更不再触发更新。
 *
 * 本文件同时覆盖同页多实例的隔离性与重复注册的边界情况。
 */

/**
 * 转译时使用的虚拟路径：其所在目录及所有上级目录都不存在 tsconfig，
 * 模拟 Vaadin 25.2 及更早版本中 `jar-resources` 不归任何 tsconfig 管的情形
 * （oxc 此时按 [[Define]] 语义处理类字段）。
 */
const NO_TSCONFIG_PATH = '/__vaadin_ckeditor_no_tsconfig__/vaadin-ckeditor.ts';

/** 由服务端同步的公开属性（原 `@property`，以及 issue #137 新增的 contentChangeEvents）。 */
const PUBLIC_PROPERTIES = [
    'editorId', 'editorType', 'themeType', 'editorData', 'editorWidth', 'editorHeight',
    'language', 'overrideCssUrl', 'isReadOnly', 'isEnabled', 'autosave', 'autosaveWaitingTime',
    'minimapEnabled', 'minimapSimplePreview', 'documentOutlineEnabled', 'annotationSidebarEnabled',
    'commentPermissionEnforcerEnabled', 'aiSidebarEnabled', 'ghsEnabled', 'hideToolbar', 'sync',
    'plugins', 'toolbar', 'config', 'licenseKey', 'toolbarStyle', 'fallbackMode',
    'strictPluginLoading', 'allowConfigRequiredPlugins', 'contentChangeEvents',
];

/** 内部状态（原 `@state`）。 */
const INTERNAL_STATES = ['editor', 'cursorPosition', 'aiSidebarCollapsed'];

const ALL_REACTIVE = [...PUBLIC_PROPERTIES, ...INTERNAL_STATES];

/** 默认值（与改为 `declare` 之前的字段初值逐一对应）。 */
const DEFAULTS: Record<string, unknown> = {
    editorId: '', editorType: 'classic', themeType: 'auto', editorData: '',
    editorWidth: 'auto', editorHeight: 'auto', language: 'en', overrideCssUrl: '',
    isReadOnly: false, isEnabled: true, autosave: false, autosaveWaitingTime: 2000,
    minimapEnabled: false, minimapSimplePreview: false, documentOutlineEnabled: false,
    annotationSidebarEnabled: false, commentPermissionEnforcerEnabled: false,
    aiSidebarEnabled: false, ghsEnabled: false, hideToolbar: false, sync: true,
    plugins: [], toolbar: [], config: {}, licenseKey: 'GPL', toolbarStyle: undefined,
    fallbackMode: 'textarea', strictPluginLoading: false, allowConfigRequiredPlugins: false,
    contentChangeEvents: false,
    editor: null, cursorPosition: null, aiSidebarCollapsed: true,
};

type Host = HTMLElement & Record<string, unknown> & {
    requestUpdate: (name?: PropertyKey, ...rest: unknown[]) => void;
};

async function loadModule() {
    return import('./vaadin-ckeditor');
}

/** 新建（未挂载的）实例：只验证属性层面，不触发编辑器创建。 */
function createHost(): Host {
    return document.createElement('vaadin-ckeditor') as unknown as Host;
}

function hasOwn(obj: object, key: string): boolean {
    return Object.prototype.hasOwnProperty.call(obj, key);
}

async function transpileWithoutTsconfig(): Promise<string> {
    const result = await transformWithOxc(SOURCE, NO_TSCONFIG_PATH, {});
    return result.code;
}

/** 截取转译产物中 `class VaadinCKEditor` 的类体（按花括号配对）。 */
function extractClassBody(code: string): string {
    const start = code.indexOf('class VaadinCKEditor');
    expect(start).toBeGreaterThanOrEqual(0);
    const open = code.indexOf('{', start);
    let depth = 0;
    for (let i = open; i < code.length; i++) {
        if (code[i] === '{') depth++;
        if (code[i] === '}' && --depth === 0) return code.slice(open + 1, i);
    }
    throw new Error('unbalanced class body');
}

/**
 * 在连接器模块求值**之前**创建并赋值的元素，模拟服务端在模块加载前就 setProperty。
 * 本文件只动态 import 连接器（`?raw` 不求值），因此此刻元素尚未定义。
 * 元素未挂载：define 不会自动升级它，由用例显式 `customElements.upgrade()`。
 */
const preUpgradeHost = document.createElement('vaadin-ckeditor') as unknown as Host;
preUpgradeHost.editorData = '<p>from server</p>';
preUpgradeHost.plugins = [{ name: 'Bold' }];
preUpgradeHost.isReadOnly = true;

afterEach(() => {
    vi.restoreAllMocks();
});

describe('反应式属性声明（issue #120）', () => {
    it('static properties 与原装饰器列表一一对应，内部状态不映射 attribute', async () => {
        const { VaadinCKEditor } = await loadModule();
        const props = (VaadinCKEditor as unknown as { elementProperties: Map<string, { state?: boolean; attribute?: unknown }> })
            .elementProperties;

        expect([...props.keys()].sort()).toEqual([...ALL_REACTIVE].sort());
        for (const name of INTERNAL_STATES) {
            expect(props.get(name)?.state, name).toBe(true);
            expect(props.get(name)?.attribute, name).toBe(false);
        }
        for (const name of PUBLIC_PROPERTIES) {
            expect(props.get(name)?.state, name).toBeFalsy();
        }
    });

    it('源码不含装饰器语法', () => {
        const code = SOURCE
            .replace(/\/\*[\s\S]*?\*\//g, '')
            .replace(/\/\/.*$/gm, '');
        // 覆盖 `@customElement('x')`、`@state()` 以及不带括号的 `@foo` 形式
        expect(code).not.toMatch(/^\s*@\w+/m);
        expect(code).not.toMatch(/lit\/decorators/);
    });

    it('前提校验：虚拟路径下 oxc 按 [[Define]] 语义保留类字段', async () => {
        // 若此前提失效（oxc 默认语义变化或上级目录出现 tsconfig），下一条用例将无法区分新旧写法
        const result = await transformWithOxc('class A { x = 1 }', NO_TSCONFIG_PATH, {});
        expect(result.code).toMatch(/^\s*x = 1;/m);
    });

    it('无 tsconfig 转译：不生成 decorator helper 导入，类体中没有反应式字段初值', async () => {
        const code = await transpileWithoutTsconfig();
        expect(code).not.toMatch(/^import .*(@oxc-project\/runtime|decorate)/m);

        const body = extractClassBody(code);
        const fieldPattern = new RegExp(`^\\s*(${ALL_REACTIVE.join('|')})\\s*[=;]`, 'm');
        expect(body).not.toMatch(fieldPattern);
    }, 30000);
});

describe('实例初始化', () => {
    it('默认值与原字段初值一致，且不以 own property 遮蔽 Lit 访问器', async () => {
        await loadModule();
        const el = createHost();
        for (const name of ALL_REACTIVE) {
            expect(el[name], name).toEqual(DEFAULTS[name]);
            expect(hasOwn(el, name), name).toBe(false);
        }
    });

    it('升级前设置的属性在首次更新时覆盖默认值', async () => {
        const { VaadinCKEditor } = await loadModule();
        const pre = preUpgradeHost;
        customElements.upgrade(pre);
        expect(pre).toBeInstanceOf(VaadinCKEditor);

        // Lit 在 super() 中暂存升级前的值，首次 performUpdate 开头写回；这里只验证暂存内容
        // （`__instanceProperties` 是 Lit 开发构建中的字段名，vitest 解析到的正是开发构建），
        // 避免挂载触发真实编辑器创建。
        const saved = (pre as unknown as { __instanceProperties?: Map<string, unknown> }).__instanceProperties;
        expect(saved && Object.fromEntries(saved)).toEqual({
            editorData: '<p>from server</p>',
            plugins: [{ name: 'Bold' }],
            isReadOnly: true,
        });
        for (const name of ['editorData', 'plugins', 'isReadOnly']) {
            expect(hasOwn(pre, name), name).toBe(false);
        }
    });
});

describe('同页多实例', () => {
    it('可变默认值（数组 / 对象）不在实例间共享', async () => {
        await loadModule();
        const [a, b] = [createHost(), createHost()];
        expect(a.plugins).not.toBe(b.plugins);
        expect(a.toolbar).not.toBe(b.toolbar);
        expect(a.config).not.toBe(b.config);

        (a.plugins as unknown[]).push({ name: 'Bold' });
        (a.config as Record<string, unknown>).toolbar = ['bold'];
        expect(b.plugins).toEqual([]);
        expect(b.config).toEqual({});
    });

    it('属性变更只调度对应实例的更新', async () => {
        await loadModule();
        const hosts = [createHost(), createHost(), createHost()];
        const changed = hosts.map(() => [] as PropertyKey[]);
        hosts.forEach((el, i) => {
            const original = el.requestUpdate.bind(el);
            el.requestUpdate = (name?: PropertyKey, ...rest: unknown[]) => {
                if (name !== undefined) changed[i].push(name);
                return original(name, ...rest);
            };
        });

        hosts[1].editorData = '<p>b</p>';
        hosts[2].isReadOnly = true;
        hosts[2].themeType = 'dark';

        expect(changed[0]).toEqual([]);
        expect(changed[1]).toEqual(['editorData']);
        expect(changed[2]).toEqual(['isReadOnly', 'themeType']);
        expect(hosts[0].editorData).toBe('');
        expect(hosts[0].isReadOnly).toBe(false);
    });
});

describe('元素注册', () => {
    it('注册的就是导出的类', async () => {
        const { VaadinCKEditor } = await loadModule();
        expect(customElements.get('vaadin-ckeditor')).toBe(VaadinCKEditor);
    });

    it('模块被重复求值时不抛错，沿用先注册的类并发出警告', async () => {
        const { VaadinCKEditor } = await loadModule();
        const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});

        // 不同 URL 迫使模块再次求值，模拟多份 add-on 并存或 HMR
        const secondUrl = './vaadin-ckeditor?second-copy';
        const second = await import(/* @vite-ignore */ secondUrl) as typeof import('./vaadin-ckeditor');

        expect(second.VaadinCKEditor).not.toBe(VaadinCKEditor);
        expect(customElements.get('vaadin-ckeditor')).toBe(VaadinCKEditor);
        expect(warn).toHaveBeenCalledWith('[VaadinCKEditor]', expect.stringContaining('already defined by another class'));
    });
});
