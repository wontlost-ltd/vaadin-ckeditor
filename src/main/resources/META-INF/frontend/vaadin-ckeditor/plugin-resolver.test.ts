/**
 * Unit Tests for Plugin Resolver Module
 *
 * Run with: npx vitest run plugin-resolver.test.ts
 * Or in watch mode: npx vitest plugin-resolver.test.ts
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
    filterConflictingPlugins,
    PluginResolver,
    PLUGIN_REGISTRY,
    registerCKEditorPlugin,
    registerDefaultCKEditorPlugin,
    unregisterCKEditorPlugin,
    getGlobalPluginRegistry,
    registerCKEditorPluginLoader,
    unregisterCKEditorPluginLoader,
    pluginConfigEntries,
    pluginSetChanged,
    type FilterOptions,
} from './plugin-resolver';

// Mock logger
const createMockLogger = () => ({
    debug: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
});

describe('filterConflictingPlugins', () => {
    let logger: ReturnType<typeof createMockLogger>;

    beforeEach(() => {
        logger = createMockLogger();
    });

    describe('default behavior', () => {
        it('should pass through standard plugins', () => {
            const plugins = ['Bold', 'Italic', 'Underline'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(plugins);
            expect(result.removed).toEqual([]);
        });

        // W14：ckeditor5 49.1.0 导出 Typing（CKEditorPlugin.TYPING），此前被当作“不可用”丢掉
        it('keeps Typing, which ckeditor5 exports', () => {
            const plugins = ['Bold', 'Typing', 'Italic'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['Bold', 'Typing', 'Italic']);
            expect(result.removed).toEqual([]);
            expect(PLUGIN_REGISTRY['Typing']).toBeTypeOf('function');
        });

        // W15：Title 只 requires Paragraph，不需要配置
        it('should remove plugins requiring configuration, but not Title', () => {
            const plugins = ['Bold', 'Minimap', 'Title', 'Italic'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['Bold', 'Title', 'Italic']);
            expect(result.removed).toEqual(['Minimap']);
            expect(logger.warn).toHaveBeenCalledTimes(1);
        });

        it('should remove CKFinder by default (requires uploadUrl/server config)', () => {
            const plugins = ['Bold', 'CKFinder', 'Italic'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['Bold', 'Italic']);
            expect(result.removed).toContain('CKFinder');
        });

        it('should keep CKFinder when allowConfigRequiredPlugins is set', () => {
            const plugins = ['Bold', 'CKFinder'];
            const options: FilterOptions = { allowConfigRequiredPlugins: true };
            const result = filterConflictingPlugins(plugins, logger, options);

            expect(result.filtered).toContain('CKFinder');
            expect(result.removed).not.toContain('CKFinder');
        });

        it('should keep first plugin from mutually exclusive group', () => {
            const plugins = ['RestrictedEditingMode', 'StandardEditingMode'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['RestrictedEditingMode']);
            expect(result.removed).toEqual(['StandardEditingMode']);
            expect(logger.warn).toHaveBeenCalled();
        });

        it('should handle reverse order of mutually exclusive plugins', () => {
            const plugins = ['StandardEditingMode', 'RestrictedEditingMode'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['StandardEditingMode']);
            expect(result.removed).toEqual(['RestrictedEditingMode']);
        });
    });

    describe('strictPluginLoading option', () => {
        it('should skip most filtering when enabled', () => {
            const plugins = ['Bold', 'Minimap', 'Title', 'Typing'];
            const options: FilterOptions = { strictPluginLoading: true };
            const result = filterConflictingPlugins(plugins, logger, options);

            // Should include Minimap (config-required) - strict mode doesn't filter it
            expect(result.filtered).toContain('Minimap');
            expect(result.filtered).toContain('Title');
            expect(result.filtered).toContain('Typing');
            expect(logger.debug).toHaveBeenCalledWith(
                'Strict plugin loading enabled - skipping automatic filtering'
            );
        });

        it('should still filter mutually exclusive plugins in strict mode', () => {
            const plugins = ['RestrictedEditingMode', 'StandardEditingMode'];
            const options: FilterOptions = { strictPluginLoading: true };
            const result = filterConflictingPlugins(plugins, logger, options);

            expect(result.filtered).toHaveLength(1);
            expect(result.removed).toHaveLength(1);
        });
    });

    describe('configuredPlugins option（issue #137）', () => {
        it('只放行列出的插件，其余需配置的插件仍被剔除', () => {
            const plugins = ['Bold', 'Minimap', 'CKFinder'];
            const result = filterConflictingPlugins(plugins, logger, { configuredPlugins: ['Minimap'] });

            expect(result.filtered).toContain('Minimap');
            expect(result.filtered).not.toContain('CKFinder');
            expect(result.removed).toEqual(['CKFinder']);
        });
    });

    describe('allowConfigRequiredPlugins option', () => {
        it('should allow config-required plugins when enabled', () => {
            const plugins = ['Bold', 'Minimap', 'Title'];
            const options: FilterOptions = { allowConfigRequiredPlugins: true };
            const result = filterConflictingPlugins(plugins, logger, options);

            expect(result.filtered).toContain('Minimap');
            expect(result.filtered).toContain('Title');
            expect(result.removed).toEqual([]);
        });
    });

    describe('edge cases', () => {
        it('should handle empty plugin list', () => {
            const result = filterConflictingPlugins([], logger);

            expect(result.filtered).toEqual([]);
            expect(result.removed).toEqual([]);
        });

        it('should handle unknown plugins', () => {
            const plugins = ['UnknownPlugin', 'AnotherUnknown'];
            const result = filterConflictingPlugins(plugins, logger);

            // Unknown plugins should pass through (they might be custom)
            expect(result.filtered).toEqual(plugins);
            expect(result.removed).toEqual([]);
        });

        it('should handle duplicate plugins', () => {
            const plugins = ['Bold', 'Bold', 'Italic'];
            const result = filterConflictingPlugins(plugins, logger);

            // Duplicates should be preserved (CKEditor handles deduplication)
            expect(result.filtered).toEqual(['Bold', 'Bold', 'Italic']);
        });
    });
});

describe('PLUGIN_REGISTRY', () => {
    it('should contain essential plugins', () => {
        expect(PLUGIN_REGISTRY).toHaveProperty('Essentials');
        expect(PLUGIN_REGISTRY).toHaveProperty('Paragraph');
        expect(PLUGIN_REGISTRY).toHaveProperty('Bold');
        expect(PLUGIN_REGISTRY).toHaveProperty('Italic');
    });

    it('registers TableLayout from the ckeditor5 package (a premium feature needing a licence, not a ckeditor5-premium-features export)', () => {
        expect(PLUGIN_REGISTRY).toHaveProperty('TableLayout');
    });

    it('should contain all basic formatting plugins', () => {
        const basicPlugins = [
            'Bold', 'Italic', 'Underline', 'Strikethrough',
            'Code', 'Superscript', 'Subscript',
        ];

        for (const plugin of basicPlugins) {
            expect(PLUGIN_REGISTRY).toHaveProperty(plugin);
        }
    });

    it('should contain image-related plugins', () => {
        const imagePlugins = [
            'Image', 'ImageToolbar', 'ImageCaption', 'ImageStyle',
            'ImageResize', 'ImageUpload', 'ImageInsert',
        ];

        for (const plugin of imagePlugins) {
            expect(PLUGIN_REGISTRY).toHaveProperty(plugin);
        }
    });

    it('should contain table-related plugins', () => {
        const tablePlugins = [
            'Table', 'TableToolbar', 'TableProperties',
            'TableCellProperties', 'TableCaption', 'TableColumnResize',
        ];

        for (const plugin of tablePlugins) {
            expect(PLUGIN_REGISTRY).toHaveProperty(plugin);
        }
    });
});

describe('Global Plugin Registry', () => {
    beforeEach(() => {
        // Clear registry before each test
        const registry = getGlobalPluginRegistry();
        for (const key of Object.keys(registry)) {
            delete registry[key];
        }
    });

    it('should register and retrieve custom plugins', () => {
        const mockPlugin = class MockPlugin {};
        registerCKEditorPlugin('MockPlugin', mockPlugin);

        const registry = getGlobalPluginRegistry();
        expect(registry['MockPlugin']).toBe(mockPlugin);
    });

    it('should allow overwriting registered plugins', () => {
        const mockPlugin1 = class MockPlugin1 {};
        const mockPlugin2 = class MockPlugin2 {};

        registerCKEditorPlugin('TestPlugin', mockPlugin1);
        registerCKEditorPlugin('TestPlugin', mockPlugin2);

        const registry = getGlobalPluginRegistry();
        expect(registry['TestPlugin']).toBe(mockPlugin2);
    });

    it('should use Symbol for namespace isolation', () => {
        // The registry should use Symbol.for to avoid conflicts
        const key = Symbol.for('vaadin-ckeditor-custom-plugins');
        registerCKEditorPlugin('SymbolTest', { test: true });

        // Direct access via symbol should work
        expect((window as any)[key]).toBeDefined();
        expect((window as any)[key]['SymbolTest']).toEqual({ test: true });
    });

    // review: registry had no cleanup path → unregister API
    it('unregisterCKEditorPlugin removes a registered plugin and returns true', () => {
        registerCKEditorPlugin('Removable', class R {});
        expect(getGlobalPluginRegistry()['Removable']).toBeDefined();

        expect(unregisterCKEditorPlugin('Removable')).toBe(true);
        expect(getGlobalPluginRegistry()['Removable']).toBeUndefined();
    });

    it('unregisterCKEditorPlugin returns false when the plugin was not registered', () => {
        expect(unregisterCKEditorPlugin('NeverRegistered')).toBe(false);
    });

    it('registerDefaultCKEditorPlugin registers only when the name is free (application plugin wins)', () => {
        const appPlugin = class AppWordCount {};
        const builtIn = class VaadinWordCount {};
        expect(registerDefaultCKEditorPlugin('Defaulted', builtIn)).toBe(true);
        expect(getGlobalPluginRegistry()['Defaulted']).toBe(builtIn);

        registerCKEditorPlugin('Replaced', appPlugin);
        expect(registerDefaultCKEditorPlugin('Replaced', builtIn)).toBe(false);
        expect(getGlobalPluginRegistry()['Replaced']).toBe(appPlugin);
    });

    it('a registered plugin is resolved by name without an import path (CustomPlugin.fromRegistry)', async () => {
        const logger = createMockLogger();
        const registered = class Registered {};
        registerCKEditorPlugin('RegisteredOnly', registered);
        const resolver = new PluginResolver(logger);

        const plugins = await resolver.resolvePlugins([{ name: 'RegisteredOnly', premium: false }]);

        expect(plugins).toEqual([registered]);
        expect(resolver.loadErrors).toEqual([]);
    });

    it('built-in CKEditor plugins keep precedence over registered ones with the same name', async () => {
        registerCKEditorPlugin('Bold', class FakeBold {});
        const resolver = new PluginResolver(createMockLogger());
        const plugins = await resolver.resolvePlugins([{ name: 'Bold', premium: false }]);
        expect(plugins).toEqual([PLUGIN_REGISTRY['Bold']]);
    });

    it('unregister only removes the named plugin, leaving others intact', () => {
        registerCKEditorPlugin('KeepMe', class K {});
        registerCKEditorPlugin('DropMe', class D {});

        unregisterCKEditorPlugin('DropMe');

        const registry = getGlobalPluginRegistry();
        expect(registry['DropMe']).toBeUndefined();
        expect(registry['KeepMe']).toBeDefined();
    });
});

describe('PluginResolver', () => {
    let logger: ReturnType<typeof createMockLogger>;
    let resolver: PluginResolver;

    beforeEach(() => {
        logger = createMockLogger();
        resolver = new PluginResolver(logger);
    });

    describe('resolvePlugins', () => {
        it('should resolve standard plugins from registry', async () => {
            const configs = [
                { name: 'Bold', premium: false },
                { name: 'Italic', premium: false },
            ];

            const plugins = await resolver.resolvePlugins(configs);

            expect(plugins).toHaveLength(2);
            expect(plugins[0]).toBe(PLUGIN_REGISTRY['Bold']);
            expect(plugins[1]).toBe(PLUGIN_REGISTRY['Italic']);
        });

        it('should warn for unknown plugins', async () => {
            const configs = [
                { name: 'UnknownPlugin', premium: false },
            ];

            const plugins = await resolver.resolvePlugins(configs);

            expect(plugins).toHaveLength(0);
            expect(logger.warn).toHaveBeenCalledWith(
                'Plugin not found in registry or the ckeditor5 package: UnknownPlugin'
            );
        });

        it('records a missing standard plugin in loadErrors (review: parity with premium/custom)', async () => {
            const configs = [
                { name: 'Bold', premium: false },
                { name: 'NotARealPlugin', premium: false },
            ];

            await resolver.resolvePlugins(configs);

            expect(resolver.loadErrors).toContain(
                'Plugin not found in registry or the ckeditor5 package: NotARealPlugin'
            );
        });

        // W16：CustomPlugin.fromCKEditor5(name) 加载 ckeditor5 包的任意导出，而不只是静态插件表里的名称
        it('resolves any ckeditor5 export by name (fromCKEditor5), not only registry names', async () => {
            const ckeditor5 = await import('ckeditor5');
            expect(PLUGIN_REGISTRY['Font']).toBeUndefined();
            expect(PLUGIN_REGISTRY['TableScroll']).toBeUndefined();
            const plugins = await resolver.resolvePlugins([
                { name: 'Font', premium: false },
                { name: 'TableScroll', premium: false },
            ]);
            expect(plugins).toEqual([ckeditor5.Font, ckeditor5.TableScroll]);
            expect(resolver.loadErrors).toEqual([]);
        });

        it('an app-registered plugin keeps precedence over a ckeditor5 export of the same name', async () => {
            class MyFont {}
            registerCKEditorPlugin('Font', MyFont);
            try {
                expect(await resolver.resolvePlugins([{ name: 'Font', premium: false }])).toEqual([MyFont]);
            } finally {
                unregisterCKEditorPlugin('Font');
            }
        });

        it('does not load a non-class ckeditor5 export or an inherited property', async () => {
            // version 是字符串导出，toString 是继承属性
            expect(await resolver.resolvePlugins([
                { name: 'version', premium: false },
                { name: 'toString', premium: false },
            ])).toEqual([]);
            expect(resolver.loadErrors).toHaveLength(2);
        });

        it('importPath "ckeditor5" (CustomPlugin.of(name, "ckeditor5")) uses the bundled package, not a runtime import', async () => {
            const ckeditor5 = await import('ckeditor5');
            expect(await resolver.resolvePlugins([{ name: 'Font', premium: false, importPath: 'ckeditor5' }]))
                .toEqual([ckeditor5.Font]);
            expect(await resolver.resolvePlugins([{ name: 'Nope', premium: false, importPath: 'ckeditor5' }])).toEqual([]);
            expect(resolver.loadErrors).toContain('Plugin Nope is not exported by ckeditor5');
        });

        it('importPath "ckeditor5-premium-features" uses the bundled premium package', async () => {
            const premium = await import('ckeditor5-premium-features') as Record<string, unknown>;
            const name = Object.keys(premium).find((k) => typeof premium[k] === 'function')!;
            expect(await resolver.resolvePlugins([{ name, premium: false, importPath: 'ckeditor5-premium-features' }]))
                .toEqual([premium[name]]);
        });

        it('leaves loadErrors empty when all standard plugins resolve', async () => {
            await resolver.resolvePlugins([{ name: 'Bold', premium: false }]);
            expect(resolver.loadErrors).toHaveLength(0);
        });

        it('should respect filter options', async () => {
            const configs = [
                { name: 'Bold', premium: false },
                { name: 'Minimap', premium: false },
            ];

            // Without option - Minimap should be filtered
            const plugins1 = await resolver.resolvePlugins(configs);
            expect(plugins1).toHaveLength(1);

            // With option - Minimap should be included
            const plugins2 = await resolver.resolvePlugins(configs, {
                allowConfigRequiredPlugins: true,
            });
            expect(plugins2).toHaveLength(2);
        });

        it('should use instance filter options', async () => {
            resolver.setFilterOptions({ allowConfigRequiredPlugins: true });

            const configs = [
                { name: 'Bold', premium: false },
                { name: 'Minimap', premium: false },
            ];

            const plugins = await resolver.resolvePlugins(configs);
            expect(plugins).toHaveLength(2);
        });
    });

    describe('utility methods', () => {
        it('should list available plugin names', () => {
            const names = resolver.getAvailablePluginNames();

            expect(names).toContain('Bold');
            expect(names).toContain('Italic');
            expect(names.length).toBeGreaterThan(50);
        });

        it('should check plugin availability', () => {
            expect(resolver.isPluginAvailable('Bold')).toBe(true);
            expect(resolver.isPluginAvailable('UnknownPlugin')).toBe(false);
        });

        it('should check if plugin requires configuration', () => {
            expect(resolver.requiresConfiguration('Minimap')).toBe(true);
            expect(resolver.requiresConfiguration('CKFinder')).toBe(true);
            expect(resolver.requiresConfiguration('Bold')).toBe(false);
        });
    });
});

describe('自定义插件的按需加载器与配置', () => {
    it('加载器只在插件被使用时调用，优先于 importPath；登记的类优先于加载器', async () => {
        const logger = createMockLogger();
        const LazyPlugin = class { static pluginName = 'LazyPlugin'; };
        const loader = vi.fn(async () => ({ default: LazyPlugin }));
        registerCKEditorPluginLoader('LazyPlugin', loader);
        try {
            const resolver = new PluginResolver(logger);
            await resolver.resolvePlugins([{ name: 'Bold', premium: false }]);
            expect(loader).not.toHaveBeenCalled();

            const plugins = await resolver.resolvePlugins([
                { name: 'LazyPlugin', premium: false, importPath: './never-imported' },
            ]);
            expect(plugins).toEqual([LazyPlugin]);
            expect(loader).toHaveBeenCalledTimes(1);
            expect(resolver.loadErrors).toEqual([]);

            const Registered = class { static pluginName = 'LazyPlugin'; };
            registerCKEditorPlugin('LazyPlugin', Registered);
            expect(await resolver.resolvePlugins([{ name: 'LazyPlugin', premium: false, importPath: './x' }])).toEqual([Registered]);
            unregisterCKEditorPlugin('LazyPlugin');
        } finally {
            expect(unregisterCKEditorPluginLoader('LazyPlugin')).toBe(true);
        }
        expect(unregisterCKEditorPluginLoader('LazyPlugin')).toBe(false);
    });

    it('加载器可直接返回插件类或具名导出', async () => {
        const Named = class { static pluginName = 'NamedPlugin'; };
        registerCKEditorPluginLoader('NamedPlugin', async () => ({ NamedPlugin: Named }));
        registerCKEditorPluginLoader('ClassPlugin', async () => Named);
        const resolver = new PluginResolver(createMockLogger());
        expect(await resolver.resolvePlugins([
            { name: 'NamedPlugin', premium: false, importPath: './a' },
            { name: 'ClassPlugin', premium: false, importPath: './b' },
        ])).toEqual([Named, Named]);
        unregisterCKEditorPluginLoader('NamedPlugin');
        unregisterCKEditorPluginLoader('ClassPlugin');
    });

    it('加载器失败记入 loadErrors，不抛出', async () => {
        registerCKEditorPluginLoader('BrokenPlugin', () => Promise.reject(new Error('chunk 404')));
        const resolver = new PluginResolver(createMockLogger());
        expect(await resolver.resolvePlugins([{ name: 'BrokenPlugin', premium: false, importPath: './b' }])).toEqual([]);
        expect(resolver.loadErrors[0]).toContain('BrokenPlugin');
        unregisterCKEditorPluginLoader('BrokenPlugin');
    });

    it('无 importPath 的注册表插件名（CustomPlugin.fromRegistry）也回退到加载器；失败记入 loadErrors', async () => {
        const Lazy = class { static pluginName = 'RegistryLazy'; };
        const loader = vi.fn(async () => ({ default: Lazy }));
        registerCKEditorPluginLoader('RegistryLazy', loader);
        registerCKEditorPluginLoader('RegistryBroken', () => Promise.reject(new Error('chunk 404')));
        try {
            const resolver = new PluginResolver(createMockLogger());
            expect(await resolver.resolvePlugins([
                { name: 'RegistryLazy', premium: false, configKey: 'registryLazy', config: { level: 2 } },
                { name: 'RegistryBroken', premium: false },
                { name: 'Bold', premium: false },
            ])).toEqual([Lazy, PLUGIN_REGISTRY['Bold']]);
            expect(loader).toHaveBeenCalledTimes(1);
            expect(resolver.loadErrors).toHaveLength(1);
            expect(resolver.loadErrors[0]).toContain('RegistryBroken');

            // 注册表中的类优先于加载器
            const Registered = class { static pluginName = 'RegistryLazy'; };
            registerCKEditorPlugin('RegistryLazy', Registered);
            expect(await new PluginResolver(createMockLogger())
                .resolvePlugins([{ name: 'RegistryLazy', premium: false }])).toEqual([Registered]);
            expect(loader).toHaveBeenCalledTimes(1);
            unregisterCKEditorPlugin('RegistryLazy');
        } finally {
            unregisterCKEditorPluginLoader('RegistryLazy');
            unregisterCKEditorPluginLoader('RegistryBroken');
        }
    });

    it('add-on 登记了代码块高亮插件的加载器', async () => {
        await import('./addon-plugins');
        const resolver = new PluginResolver(createMockLogger());
        const plugins = await resolver.resolvePlugins([
            { name: 'VaadinCodeBlockHighlighting', premium: false, importPath: './vaadin-ckeditor/code-block-highlighting' },
        ]);
        expect((plugins[0] as { pluginName: string }).pluginName).toBe('VaadinCodeBlockHighlighting');
    });

    it('pluginConfigEntries 收集 configKey → config；pluginSetChanged 只比较名称与来源', () => {
        const a = [
            { name: 'Bold', premium: false },
            { name: 'X', premium: false, importPath: './x', configKey: 'x', config: { enabled: true } },
        ];
        const b = [
            { name: 'X', premium: false, importPath: './x', configKey: 'x', config: { enabled: false } },
            { name: 'Bold', premium: false },
        ];
        expect(pluginConfigEntries(a)).toEqual({ x: { enabled: true } });
        expect(pluginConfigEntries(undefined)).toEqual({});
        expect(pluginSetChanged(a, b)).toBe(false);
        expect(pluginSetChanged(a, [b[1]])).toBe(true);
        expect(pluginSetChanged(a, [{ ...b[0], premium: true }, b[1]])).toBe(true);
        expect(pluginSetChanged(undefined, [])).toBe(false);
    });
});
