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
    unregisterCKEditorPlugin,
    getGlobalPluginRegistry,
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

        it('should remove unavailable plugins', () => {
            const plugins = ['Bold', 'Typing', 'Italic'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['Bold', 'Italic']);
            expect(result.removed).toEqual(['Typing']);
            expect(logger.debug).toHaveBeenCalled();
        });

        it('should remove plugins requiring configuration', () => {
            const plugins = ['Bold', 'Minimap', 'Title', 'Italic'];
            const result = filterConflictingPlugins(plugins, logger);

            expect(result.filtered).toEqual(['Bold', 'Italic']);
            expect(result.removed).toContain('Minimap');
            expect(result.removed).toContain('Title');
            expect(logger.warn).toHaveBeenCalledTimes(2);
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

            // Should include Minimap and Title (config-required)
            // Should include Typing (unavailable) - strict mode doesn't filter these
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

    describe('allowConfigRequiredPlugins option', () => {
        it('should allow config-required plugins when enabled', () => {
            const plugins = ['Bold', 'Minimap', 'Title'];
            const options: FilterOptions = { allowConfigRequiredPlugins: true };
            const result = filterConflictingPlugins(plugins, logger, options);

            expect(result.filtered).toContain('Minimap');
            expect(result.filtered).toContain('Title');
            expect(result.removed).toEqual([]);
        });

        it('should still filter unavailable plugins', () => {
            const plugins = ['Bold', 'Typing', 'Minimap'];
            const options: FilterOptions = { allowConfigRequiredPlugins: true };
            const result = filterConflictingPlugins(plugins, logger, options);

            expect(result.filtered).toContain('Minimap');
            expect(result.removed).toContain('Typing');
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
                'Plugin not found in registry: UnknownPlugin'
            );
        });

        it('records a missing standard plugin in loadErrors (review: parity with premium/custom)', async () => {
            const configs = [
                { name: 'Bold', premium: false },
                { name: 'NotARealPlugin', premium: false },
            ];

            await resolver.resolvePlugins(configs);

            expect(resolver.loadErrors).toContain(
                'Plugin not found in registry: NotARealPlugin'
            );
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
