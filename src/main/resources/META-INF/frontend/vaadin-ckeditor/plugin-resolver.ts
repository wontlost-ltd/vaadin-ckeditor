/**
 * Plugin Resolver Module for VaadinCKEditor
 *
 * Handles plugin resolution, conflict detection, and dynamic loading.
 * Extracted from vaadin-ckeditor.ts for better maintainability.
 */

// Import all CKEditor plugins
import {
    // Core plugins
    Essentials,
    Paragraph,
    Undo,
    Clipboard,
    SelectAll,
    // Basic styles
    Bold,
    Italic,
    Underline,
    Strikethrough,
    Code,
    Superscript,
    Subscript,
    // Font styles
    FontSize,
    FontFamily,
    FontColor,
    FontBackgroundColor,
    // Paragraph formatting
    Heading,
    Alignment,
    Indent,
    IndentBlock,
    BlockQuote,
    // Lists
    List,
    TodoList,
    // Links
    Link,
    AutoLink,
    // Images
    Image,
    ImageToolbar,
    ImageCaption,
    ImageStyle,
    ImageResize,
    ImageUpload,
    ImageInsert,
    ImageBlock,
    ImageInline,
    LinkImage,
    AutoImage,
    // Upload adapters
    Base64UploadAdapter,
    SimpleUploadAdapter,
    // Tables
    Table,
    TableToolbar,
    TableProperties,
    TableCellProperties,
    TableCaption,
    TableColumnResize,
    // Media
    MediaEmbed,
    HtmlEmbed,
    // Code
    CodeBlock,
    // Special content
    HorizontalLine,
    PageBreak,
    SpecialCharacters,
    SpecialCharactersEssentials,
    SpecialCharactersArrows,
    SpecialCharactersCurrency,
    SpecialCharactersLatin,
    SpecialCharactersMathematical,
    SpecialCharactersText,
    // Editing enhancements
    Autoformat,
    TextTransformation,
    FindAndReplace,
    RemoveFormat,
    SourceEditing,
    ShowBlocks,
    Highlight,
    Fullscreen,
    // Mentions
    Mention,
    // Document features
    Autosave,
    WordCount,
    Title,
    PasteFromOffice,
    Markdown,
    PasteFromMarkdownExperimental,
    // HTML support
    GeneralHtmlSupport,
    HtmlComment,
    Style,
    // Restricted editing
    RestrictedEditingMode,
    StandardEditingMode,
    // Bookmark
    Bookmark,
    // List extensions
    ListProperties,
    ListFormatting,
    AdjacentListsSupport,
    // Language
    TextPartLanguage,
    // Cloud services
    CloudServices,
    CloudServicesCore,
    CloudServicesUploadAdapter,
    // Easy Image
    EasyImage,
    // Minimap
    Minimap,
    // Widget framework
    Widget,
    WidgetToolbarRepository,
    WidgetResize,
    WidgetTypeAround,
    // Emoji
    Emoji,
    EmojiPicker,
} from 'ckeditor5';

/**
 * Plugin configuration interface
 */
export interface PluginConfig {
    name: string;
    premium: boolean;
    importPath?: string;
}

/**
 * Logger interface for debugging
 */
interface Logger {
    debug: (...args: unknown[]) => void;
    warn: (...args: unknown[]) => void;
    error: (...args: unknown[]) => void;
}

/**
 * CKEditor plugin constructor type.
 * All CKEditor 5 plugins follow this pattern.
 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
type PluginConstructor = new (...args: any[]) => any;

/**
 * Plugin registry mapping plugin names to their constructors.
 * Type-safe registry for all CKEditor 5 plugins.
 */
export const PLUGIN_REGISTRY: Record<string, PluginConstructor> = {
    // Core
    'Essentials': Essentials,
    'Paragraph': Paragraph,
    'Undo': Undo,
    'Clipboard': Clipboard,
    'SelectAll': SelectAll,
    // Basic styles
    'Bold': Bold,
    'Italic': Italic,
    'Underline': Underline,
    'Strikethrough': Strikethrough,
    'Code': Code,
    'Superscript': Superscript,
    'Subscript': Subscript,
    // Font styles
    'FontSize': FontSize,
    'FontFamily': FontFamily,
    'FontColor': FontColor,
    'FontBackgroundColor': FontBackgroundColor,
    // Paragraph formatting
    'Heading': Heading,
    'Alignment': Alignment,
    'Indent': Indent,
    'IndentBlock': IndentBlock,
    'BlockQuote': BlockQuote,
    // Lists
    'List': List,
    'TodoList': TodoList,
    // Links
    'Link': Link,
    'AutoLink': AutoLink,
    // Images
    'Image': Image,
    'ImageToolbar': ImageToolbar,
    'ImageCaption': ImageCaption,
    'ImageStyle': ImageStyle,
    'ImageResize': ImageResize,
    'ImageUpload': ImageUpload,
    'ImageInsert': ImageInsert,
    'ImageBlock': ImageBlock,
    'ImageInline': ImageInline,
    'LinkImage': LinkImage,
    'AutoImage': AutoImage,
    // Upload adapters
    'Base64UploadAdapter': Base64UploadAdapter,
    'SimpleUploadAdapter': SimpleUploadAdapter,
    // Tables
    'Table': Table,
    'TableToolbar': TableToolbar,
    'TableProperties': TableProperties,
    'TableCellProperties': TableCellProperties,
    'TableCaption': TableCaption,
    'TableColumnResize': TableColumnResize,
    // Media
    'MediaEmbed': MediaEmbed,
    'HtmlEmbed': HtmlEmbed,
    // Code
    'CodeBlock': CodeBlock,
    // Special content
    'HorizontalLine': HorizontalLine,
    'PageBreak': PageBreak,
    'SpecialCharacters': SpecialCharacters,
    'SpecialCharactersEssentials': SpecialCharactersEssentials,
    'SpecialCharactersArrows': SpecialCharactersArrows,
    'SpecialCharactersCurrency': SpecialCharactersCurrency,
    'SpecialCharactersLatin': SpecialCharactersLatin,
    'SpecialCharactersMathematical': SpecialCharactersMathematical,
    'SpecialCharactersText': SpecialCharactersText,
    // Editing enhancements
    'Autoformat': Autoformat,
    'TextTransformation': TextTransformation,
    'FindAndReplace': FindAndReplace,
    'RemoveFormat': RemoveFormat,
    'SourceEditing': SourceEditing,
    'ShowBlocks': ShowBlocks,
    'Highlight': Highlight,
    'Fullscreen': Fullscreen,
    // Mentions
    'Mention': Mention,
    // Document features
    'Autosave': Autosave,
    'WordCount': WordCount,
    'Title': Title,
    'PasteFromOffice': PasteFromOffice,
    'Markdown': Markdown,
    'PasteFromMarkdownExperimental': PasteFromMarkdownExperimental,
    // HTML support
    'GeneralHtmlSupport': GeneralHtmlSupport,
    'HtmlComment': HtmlComment,
    'Style': Style,
    // Restricted editing
    'RestrictedEditingMode': RestrictedEditingMode,
    'StandardEditingMode': StandardEditingMode,
    // Bookmark
    'Bookmark': Bookmark,
    // List extensions
    'ListProperties': ListProperties,
    'ListFormatting': ListFormatting,
    'AdjacentListsSupport': AdjacentListsSupport,
    // Language
    'TextPartLanguage': TextPartLanguage,
    // Cloud services
    'CloudServices': CloudServices,
    'CloudServicesCore': CloudServicesCore,
    'CloudServicesUploadAdapter': CloudServicesUploadAdapter,
    // Easy Image
    'EasyImage': EasyImage,
    // Minimap
    'Minimap': Minimap,
    // Widget framework
    'Widget': Widget,
    'WidgetToolbarRepository': WidgetToolbarRepository,
    'WidgetResize': WidgetResize,
    'WidgetTypeAround': WidgetTypeAround,
    // Emoji
    'Emoji': Emoji,
    'EmojiPicker': EmojiPicker,
};

/**
 * Mutually exclusive plugin groups - only one plugin from each group can be enabled.
 * These plugins conflict with each other and will cause CKEditor errors if both are loaded.
 */
const MUTUALLY_EXCLUSIVE_PLUGINS: readonly string[][] = [
    ['RestrictedEditingMode', 'StandardEditingMode'],
];

/**
 * Plugins that require special configuration and should be excluded from "select all".
 * These plugins will cause errors if loaded without proper configuration.
 */
const PLUGINS_REQUIRING_CONFIG: readonly string[] = [
    'Minimap',
    'Title',
    'CloudServices',
    'CloudServicesCore',
    'CloudServicesUploadAdapter',
    'EasyImage',
];

/**
 * Plugins that are not available in the standard CKEditor 5 build.
 * These are either internal plugins or require separate packages.
 */
const UNAVAILABLE_PLUGINS: readonly string[] = [
    'Typing',
];

// Pre-computed lookup sets for O(1) access
const UNAVAILABLE_PLUGINS_SET = new Set(UNAVAILABLE_PLUGINS);
const PLUGINS_REQUIRING_CONFIG_SET = new Set(PLUGINS_REQUIRING_CONFIG);

// Map each plugin to its exclusive group for O(1) conflict detection
const PLUGIN_TO_GROUP_MAP = new Map<string, readonly string[]>();
for (const group of MUTUALLY_EXCLUSIVE_PLUGINS) {
    for (const plugin of group) {
        PLUGIN_TO_GROUP_MAP.set(plugin, group);
    }
}

/**
 * Type-safe global plugin registry using Symbol for namespace isolation.
 */
const CKEDITOR_PLUGIN_REGISTRY_KEY = Symbol.for('vaadin-ckeditor-custom-plugins');

interface GlobalPluginRegistry {
    [pluginName: string]: unknown;
}

interface WindowWithPluginRegistry extends Window {
    [CKEDITOR_PLUGIN_REGISTRY_KEY]?: GlobalPluginRegistry;
}

/**
 * Get the global plugin registry with type safety.
 */
export function getGlobalPluginRegistry(): GlobalPluginRegistry {
    const win = window as WindowWithPluginRegistry;
    if (!win[CKEDITOR_PLUGIN_REGISTRY_KEY]) {
        win[CKEDITOR_PLUGIN_REGISTRY_KEY] = {};
    }
    return win[CKEDITOR_PLUGIN_REGISTRY_KEY];
}

/**
 * Register a custom plugin in the global registry.
 *
 * @param name - The plugin name as referenced in the Java configuration
 * @param plugin - The CKEditor plugin class
 *
 * @example
 * ```javascript
 * import { registerCKEditorPlugin } from './plugin-resolver';
 * import MyCustomPlugin from './my-custom-plugin';
 *
 * registerCKEditorPlugin('MyCustomPlugin', MyCustomPlugin);
 * ```
 */
export function registerCKEditorPlugin(name: string, plugin: unknown): void {
    const registry = getGlobalPluginRegistry();
    registry[name] = plugin;
}

/**
 * Filter result from plugin conflict detection.
 */
export interface FilterResult {
    /** Plugin names that passed filtering */
    filtered: string[];
    /** Plugin names that were removed */
    removed: string[];
}

/**
 * Options for plugin filtering behavior.
 */
export interface FilterOptions {
    /**
     * When true, disables automatic plugin filtering.
     * Plugins requiring special configuration and unavailable plugins will still be loaded,
     * which may cause runtime errors if not properly configured.
     *
     * @default false
     */
    strictPluginLoading?: boolean;

    /**
     * When true, skips filtering of plugins that require special configuration.
     * Use this when you have properly configured plugins like Minimap or Title.
     *
     * @default false
     */
    allowConfigRequiredPlugins?: boolean;
}

/**
 * Filter out conflicting plugins, unavailable plugins, and plugins requiring special configuration.
 * When multiple plugins from a mutually exclusive group are present, only the first one is kept.
 *
 * Time complexity: O(n) where n is the number of plugins.
 *
 * @param pluginNames - Array of plugin names to filter
 * @param logger - Logger for debug/warning messages
 * @param options - Optional filtering options
 * @returns Filtered plugin names and removed plugin names
 */
export function filterConflictingPlugins(
    pluginNames: string[],
    logger: Logger,
    options: FilterOptions = {}
): FilterResult {
    const { strictPluginLoading = false, allowConfigRequiredPlugins = false } = options;

    // If strict mode is enabled, skip all filtering except mutually exclusive plugins
    if (strictPluginLoading) {
        logger.debug('Strict plugin loading enabled - skipping automatic filtering');
        return filterMutuallyExclusiveOnly(pluginNames, logger);
    }

    const removed: string[] = [];
    const filtered: string[] = [];
    const satisfiedGroups = new Set<readonly string[]>();
    // Track first plugin kept per group for O(1) conflict message lookup
    const firstPluginInGroup = new Map<readonly string[], string>();

    for (const name of pluginNames) {
        // O(1) check for unavailable plugins (always filter these)
        if (UNAVAILABLE_PLUGINS_SET.has(name)) {
            removed.push(name);
            logger.debug(`Plugin '${name}' is not available in standard CKEditor 5 build - skipping`);
            continue;
        }

        // O(1) check for plugins requiring configuration (can be bypassed)
        if (!allowConfigRequiredPlugins && PLUGINS_REQUIRING_CONFIG_SET.has(name)) {
            removed.push(name);
            logger.warn(`Plugin '${name}' requires special configuration - removing from automatic selection`);
            continue;
        }

        // O(1) check for mutually exclusive plugins (always filter these)
        const group = PLUGIN_TO_GROUP_MAP.get(name);
        if (group) {
            if (satisfiedGroups.has(group)) {
                const firstKept = firstPluginInGroup.get(group);
                removed.push(name);
                logger.warn(`Plugin '${name}' conflicts with '${firstKept}' - removing '${name}'`);
                continue;
            }
            satisfiedGroups.add(group);
            firstPluginInGroup.set(group, name);
        }

        filtered.push(name);
    }

    return { filtered, removed };
}

/**
 * Filter only mutually exclusive plugins (for strict mode).
 * This ensures CKEditor won't crash due to duplicate plugin registration,
 * while allowing all other plugins to load.
 */
function filterMutuallyExclusiveOnly(pluginNames: string[], logger: Logger): FilterResult {
    const removed: string[] = [];
    const filtered: string[] = [];
    const satisfiedGroups = new Set<readonly string[]>();
    const firstPluginInGroup = new Map<readonly string[], string>();

    for (const name of pluginNames) {
        const group = PLUGIN_TO_GROUP_MAP.get(name);
        if (group) {
            if (satisfiedGroups.has(group)) {
                const firstKept = firstPluginInGroup.get(group);
                removed.push(name);
                logger.warn(`Plugin '${name}' conflicts with '${firstKept}' - removing '${name}'`);
                continue;
            }
            satisfiedGroups.add(group);
            firstPluginInGroup.set(group, name);
        }
        filtered.push(name);
    }

    return { filtered, removed };
}

/**
 * Plugin Resolver class for managing CKEditor plugin resolution.
 */
export class PluginResolver {
    private readonly logger: Logger;
    private filterOptions: FilterOptions = {};
    /** Errors encountered during plugin loading (non-fatal) */
    readonly loadErrors: string[] = [];

    constructor(logger: Logger) {
        this.logger = logger;
    }

    /**
     * Set filter options for plugin loading.
     *
     * @param options - Filter options
     */
    setFilterOptions(options: FilterOptions): void {
        this.filterOptions = options;
    }

    /**
     * Resolve plugin configurations to actual plugin classes.
     *
     * @param pluginConfigs - Array of plugin configurations
     * @param options - Optional filter options (overrides instance options)
     * @returns Promise resolving to array of plugin classes
     */
    async resolvePlugins(
        pluginConfigs: PluginConfig[],
        options?: FilterOptions
    ): Promise<unknown[]> {
        const resolvedPlugins: unknown[] = [];
        const pluginNames = pluginConfigs.map(p => p.name);
        const effectiveOptions = { ...this.filterOptions, ...options };

        // Filter out conflicting plugins
        const { filtered, removed } = filterConflictingPlugins(
            pluginNames,
            this.logger,
            effectiveOptions
        );
        if (removed.length > 0) {
            this.logger.debug(`Removed ${removed.length} conflicting/unavailable plugins:`, removed);
        }

        // Separate plugins by type (use Set for O(1) lookup)
        const filteredSet = new Set(filtered);
        const standardPluginsToLoad = pluginConfigs.filter(
            p => filteredSet.has(p.name) && !p.premium && !p.importPath
        );
        const premiumPluginsToLoad = pluginConfigs.filter(
            p => filteredSet.has(p.name) && p.premium && !p.importPath
        );
        const customPluginsToLoad = pluginConfigs.filter(
            p => filteredSet.has(p.name) && p.importPath
        );

        // Load standard plugins from registry
        for (const pluginConfig of standardPluginsToLoad) {
            const plugin = PLUGIN_REGISTRY[pluginConfig.name];
            if (plugin) {
                resolvedPlugins.push(plugin);
            } else {
                this.logger.warn(`Plugin not found in registry: ${pluginConfig.name}`);
            }
        }

        // Load premium plugins dynamically
        if (premiumPluginsToLoad.length > 0) {
            await this.loadPremiumPlugins(premiumPluginsToLoad, resolvedPlugins);
        }

        // Load custom plugins dynamically
        for (const pluginConfig of customPluginsToLoad) {
            await this.loadCustomPlugin(pluginConfig, resolvedPlugins);
        }

        return resolvedPlugins;
    }

    /**
     * Load premium plugins from the ckeditor5-premium-features package.
     */
    private async loadPremiumPlugins(
        plugins: PluginConfig[],
        resolvedPlugins: unknown[]
    ): Promise<void> {
        try {
            const premiumModule = await import('ckeditor5-premium-features');
            for (const pluginConfig of plugins) {
                const plugin = premiumModule[pluginConfig.name as keyof typeof premiumModule];
                if (plugin) {
                    resolvedPlugins.push(plugin);
                    this.logger.debug(`Loaded premium plugin: ${pluginConfig.name}`);
                } else {
                    this.logger.warn(`Premium plugin not found: ${pluginConfig.name}`);
                }
            }
        } catch (error) {
            const errorMsg = `Failed to load premium plugins (${plugins.map(p => p.name).join(', ')}). ` +
                `Make sure ckeditor5-premium-features is installed.`;
            this.logger.error(errorMsg, error);
            this.loadErrors.push(errorMsg);
        }
    }

    /**
     * Load a custom plugin from the global registry or via dynamic import.
     */
    private async loadCustomPlugin(
        pluginConfig: PluginConfig,
        resolvedPlugins: unknown[]
    ): Promise<void> {
        try {
            // First, check the global registry
            const globalRegistry = getGlobalPluginRegistry();
            if (globalRegistry[pluginConfig.name]) {
                resolvedPlugins.push(globalRegistry[pluginConfig.name]);
                this.logger.debug(`Loaded custom plugin from global registry: ${pluginConfig.name}`);
                return;
            }

            // Otherwise, try dynamic import
            this.logger.debug(`Loading custom plugin: ${pluginConfig.name} from ${pluginConfig.importPath}`);
            const customModule = await import(/* @vite-ignore */ pluginConfig.importPath!);
            const customPlugin = customModule[pluginConfig.name] || customModule.default;

            if (customPlugin) {
                resolvedPlugins.push(customPlugin);
                this.logger.debug(`Loaded custom plugin: ${pluginConfig.name}`);
            } else {
                this.logger.warn(`Custom plugin not found: ${pluginConfig.name} in module ${pluginConfig.importPath}`);
            }
        } catch (error) {
            const errorMsg = `Failed to load custom plugin ${pluginConfig.name} from ${pluginConfig.importPath}`;
            this.logger.error(errorMsg + ':', error);
            this.loadErrors.push(errorMsg);
        }
    }

    /**
     * Get all available plugin names from the registry.
     */
    getAvailablePluginNames(): string[] {
        return Object.keys(PLUGIN_REGISTRY);
    }

    /**
     * Check if a plugin name is available in the registry.
     */
    isPluginAvailable(name: string): boolean {
        return name in PLUGIN_REGISTRY;
    }

    /**
     * Check if a plugin requires special configuration.
     */
    requiresConfiguration(name: string): boolean {
        return PLUGINS_REQUIRING_CONFIG_SET.has(name);
    }
}
