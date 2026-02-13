/**
 * Vaadin CKEditor 5 Web Component
 *
 * A modular CKEditor 5 integration for Vaadin using the official ckeditor5 npm package.
 * Plugins are loaded dynamically based on configuration from the Java backend.
 */
import { LitElement, html, css, PropertyValues } from 'lit';
import { customElement, property, state } from 'lit/decorators.js';

// Import modular components
import { ThemeManager } from './theme-manager';
import { UploadAdapterManager } from './upload-adapter';
import { FallbackRenderer, type FallbackMode } from './fallback-renderer';
import {
    PluginResolver,
    registerCKEditorPlugin,
    type PluginConfig,
} from './plugin-resolver';

// 内置插件
import CommentPermissionEnforcer from './comment-permission-enforcer';

// Import sticky toolbar CSS (extracted for maintainability)
import './sticky-toolbar.css';

// Import document editor CSS for Decoupled/Document editor styling
// These styles are applied globally since VaadinCKEditor uses Light DOM
import './document-editor.css';

/**
 * Debug logger - only logs in development mode
 * Set window.VAADIN_CKEDITOR_DEBUG = true to enable debug logging
 */
const DEBUG = typeof window !== 'undefined' && (window as Window & { VAADIN_CKEDITOR_DEBUG?: boolean }).VAADIN_CKEDITOR_DEBUG === true;

const logger = {
    debug: (...args: unknown[]) => { if (DEBUG) console.debug('[VaadinCKEditor]', ...args); },
    info: (...args: unknown[]) => console.info('[VaadinCKEditor]', ...args),
    warn: (...args: unknown[]) => console.warn('[VaadinCKEditor]', ...args),
    error: (...args: unknown[]) => console.error('[VaadinCKEditor]', ...args),
};

// Timing constants
/** Delay (ms) for toolbar repaint mouse-leave event */
const TOOLBAR_REPAINT_DELAY_MS = 10;
/** Delay (ms) for sticky panel setup after CKEditor initialization */
const STICKY_PANEL_SETUP_DELAY_MS = 100;
/** Timeout (ms) for requestIdleCallback during editor destruction */
const DESTROY_IDLE_TIMEOUT_MS = 100;
/** Opacity value used to trigger container repaint without visible flicker */
const REPAINT_OPACITY = '0.99';
/** Maximum polling attempts for minimap iframe injection */
const MINIMAP_INJECT_MAX_ATTEMPTS = 20;
/** A4 paper minimum height in pixels (portrait, ~297mm at 96dpi) */
const A4_MIN_HEIGHT_PX = '1123px';
/** A4 paper width in pixels (portrait, ~210mm at 96dpi) */
const A4_WIDTH_PX = '796px';

// Import CKEditor 5 core editors and types (plugins are now in plugin-resolver.ts)
import {
    ClassicEditor,
    BalloonEditor,
    InlineEditor,
    DecoupledEditor,
    Editor,
    EditorConfig,
    type Translations,
} from 'ckeditor5';

// Import CKEditor 5 styles
import 'ckeditor5/ckeditor5.css';

// Import CKEditor 5 translations for i18n support
import jaTranslations from 'ckeditor5/translations/ja.js';
import zhCnTranslations from 'ckeditor5/translations/zh-cn.js';
import zhTranslations from 'ckeditor5/translations/zh.js';
import koTranslations from 'ckeditor5/translations/ko.js';
import deTranslations from 'ckeditor5/translations/de.js';
import frTranslations from 'ckeditor5/translations/fr.js';
import esTranslations from 'ckeditor5/translations/es.js';
import ptTranslations from 'ckeditor5/translations/pt.js';
import ruTranslations from 'ckeditor5/translations/ru.js';
import arTranslations from 'ckeditor5/translations/ar.js';

// Translation registry mapping language codes to their translation modules
const TRANSLATION_REGISTRY: Record<string, Translations> = {
    'ja': jaTranslations,
    'zh-cn': zhCnTranslations,
    'zh': zhTranslations,
    'ko': koTranslations,
    'de': deTranslations,
    'fr': frTranslations,
    'es': esTranslations,
    'pt': ptTranslations,
    'ru': ruTranslations,
    'ar': arTranslations,
};

// Note: PluginConfig, PLUGIN_REGISTRY, filterConflictingPlugins, and global registry
// are now in plugin-resolver.ts module

// Re-export registerCKEditorPlugin for backward compatibility
export { registerCKEditorPlugin };

/**
 * Server communication interface
 */
interface VaadinServer {
    setEditorData(data: string): void;
    saveEditorData(data: string): void;
    // Enterprise event methods
    fireEditorReady(initTimeMs: number): void;
    fireEditorError(code: string, message: string, severity: string, recoverable: boolean, stackTrace: string): void;
    fireContentChange(oldContent: string, newContent: string, source: string): void;
    fireFallback(mode: string, reason: string, originalError: string): void;
    // Upload handler
    handleFileUpload(uploadId: string, fileName: string, mimeType: string, base64Data: string): void;
}

/**
 * Individual button style configuration
 */
interface ButtonStyleConfig {
    background?: string;
    hoverBackground?: string;
    activeBackground?: string;
    iconColor?: string;
}

/**
 * Toolbar style configuration for customizing CKEditor toolbar appearance.
 * Supports global toolbar styling and per-button customization.
 */
interface ToolbarStyleConfig {
    background?: string;
    borderColor?: string;
    borderRadius?: string;
    buttonBackground?: string;
    buttonHoverBackground?: string;
    buttonActiveBackground?: string;
    buttonOnBackground?: string;
    buttonOnColor?: string;
    iconColor?: string;
    buttonStyles?: Record<string, ButtonStyleConfig>;
}
// Note: UploadResolver interface is now in upload-adapter.ts module
// Note: PLUGIN_REGISTRY is now in plugin-resolver.ts module
// Note: DARK_THEME_VARS and darkThemeRefCount are now managed by ThemeManager module

/**
 * VaadinCKEditor Web Component
 *
 * Theme Integration:
 * - Supports 'auto', 'light', and 'dark' theme modes
 * - 'auto' (default): Automatically syncs with Vaadin's Lumo theme via [theme~="dark"] attribute
 * - Also supports OS-level dark mode detection via prefers-color-scheme media query
 * - Manual theme control available via themeType property
 */
@customElement('vaadin-ckeditor')
export class VaadinCKEditor extends LitElement {

    /**
     * Static styles are not used since VaadinCKEditor uses Light DOM.
     * Document editor styles are loaded via document-editor.css import.
     * Sticky toolbar styles are loaded via sticky-toolbar.css import.
     */
    static styles = css``;

    // Properties synced from Java backend
    @property({ type: String }) editorId = '';
    @property({ type: String }) editorType: 'classic' | 'balloon' | 'inline' | 'decoupled' = 'classic';
    @property({ type: String }) themeType: 'auto' | 'light' | 'dark' = 'auto';
    @property({ type: String }) editorData = '';
    @property({ type: String }) editorWidth = 'auto';
    @property({ type: String }) editorHeight = 'auto';
    @property({ type: String }) language = 'en';
    @property({ type: String }) overrideCssUrl = '';
    @property({ type: Boolean }) isReadOnly = false;
    @property({ type: Boolean }) autosave = false;
    @property({ type: Number }) autosaveWaitingTime = 2000;
    @property({ type: Boolean }) minimapEnabled = false;
    /**
     * When true, minimap renders content as simple boxes for better performance.
     * Use this option if the minimap updates too slowly with large documents.
     *
     * @default false
     */
    @property({ type: Boolean }) minimapSimplePreview = false;
    /**
     * When true, enables Document Outline sidebar for decoupled editor.
     * Requires DocumentOutline plugin to be loaded.
     *
     * @default false
     */
    @property({ type: Boolean }) documentOutlineEnabled = false;
    /**
     * When true, enables annotation sidebar for collaboration features (decoupled editor only).
     * Provides container for Comments, TrackChanges, and RevisionHistory sidebar panels.
     * Also creates a presence list container for showing active collaborators.
     *
     * @default false
     */
    @property({ type: Boolean }) annotationSidebarEnabled = false;
    /**
     * 当启用时，对非当前用户的评论隐藏 Edit/Remove 下拉菜单，
     * 使前端 UI 与 CKEditor Cloud Services 的 comment:write 权限模型保持一致。
     * 需要 annotationSidebarEnabled=true 和 CommentsRepository 插件。
     *
     * @default false
     */
    @property({ type: Boolean }) commentPermissionEnforcerEnabled = false;
    @property({ type: Boolean }) ghsEnabled = false;
    @property({ type: Boolean }) hideToolbar = false;
    @property({ type: Boolean }) sync = true;
    @property({ type: Array }) plugins: PluginConfig[] = [];
    @property({ type: Array }) toolbar: string[] = [];
    @property({ type: Object }) config: Record<string, unknown> = {};
    @property({ type: String }) licenseKey = 'GPL';
    @property({ type: Object }) toolbarStyle?: ToolbarStyleConfig;
    @property({ type: String }) fallbackMode: 'textarea' | 'readonly' | 'error' | 'hidden' = 'textarea';

    /**
     * When true, disables automatic plugin filtering.
     * Plugins requiring special configuration will still be loaded, which may cause
     * runtime errors if not properly configured. Mutually exclusive plugins will
     * still be filtered to prevent CKEditor crashes.
     *
     * @default false
     */
    @property({ type: Boolean }) strictPluginLoading = false;

    /**
     * When true, allows loading plugins that require special configuration
     * (Minimap, Title, CloudServices, etc.) without automatic removal.
     * Use this when you have properly configured these plugins.
     *
     * @default false
     */
    @property({ type: Boolean }) allowConfigRequiredPlugins = false;

    // Internal state
    @state() private editor: Editor | null = null;
    @state() private cursorPosition: unknown = null;

    // Modular components
    private themeManager = new ThemeManager();
    private uploadManager?: UploadAdapterManager;
    private fallbackRenderer?: FallbackRenderer;

    // Event listener references for cleanup
    private selectionChangeListener?: () => void;
    private dataChangeListener?: () => void;
    private focusChangeListener?: (_evt: unknown, _data: unknown, isFocused: boolean) => void;
    private readOnlyChangeListener?: (_evt: unknown, _propertyName: unknown, isReadOnly: boolean) => void;

    // Content change tracking
    private lastKnownContent = '';
    // Track change source for ContentChangeEvent
    // Possible values: 'API', 'USER_INPUT', 'UNDO_REDO', 'PASTE', 'UNKNOWN'
    private changeSource: string = 'USER_INPUT';
    // Counter for API changes - resilient to rapid sequential updateData calls
    private apiChangeDepth = 0;

    // Destroy state management
    private isDestroying = false;
    private destroyPromise: Promise<void> | null = null;
    private isDisconnected = false;

    // Creation state management - prevent concurrent creation
    private isCreating = false;
    private createPromise: Promise<void> | null = null;

    // Command listener cleanup references (undo/redo/clipboard)
    private undoExecuteListener?: { off: () => void };
    private redoExecuteListener?: { off: () => void };
    private clipboardInputListener?: { off: () => void };

    // Collaboration tracking listener references for cleanup
    private applyOperationListener?: { off: () => void };
    private cloudServicesSyncListener?: { off: () => void };

    // Timer tracking for cleanup
    private toolbarRepaintTimeoutId: ReturnType<typeof setTimeout> | null = null;

    // requestAnimationFrame tracking for minimap style injection
    private minimapInjectRafId: number | null = null;

    // requestAnimationFrame tracking for repaint methods
    private repaintRafIds: number[] = [];

    // Custom CSS link reference for cleanup
    private customCssLink?: HTMLLinkElement;

    // Custom toolbar style element for cleanup
    private toolbarStyleElement?: HTMLStyleElement;

    // Server communication
    private $server?: VaadinServer;

    // Version info — keep in sync with VaadinCKEditor.java VERSION constant
    private readonly version = '5.0.5';

    constructor() {
        super();
    }

    /**
     * Create render root - use light DOM for CKEditor compatibility
     */
    createRenderRoot(): HTMLElement | DocumentFragment {
        return this;
    }

    /**
     * First update lifecycle - initialize editor
     */
    protected firstUpdated(_changedProperties: PropertyValues): void {
        super.firstUpdated(_changedProperties);
        logger.debug(' firstUpdated called, editorId:', this.editorId);

        // Initialize theme system (auto-sync with Vaadin or use explicit setting)
        this.initializeThemeSystem();

        this.createEditor();
    }

    /**
     * Initialize theme system based on themeType setting.
     * Delegates to ThemeManager for all theme operations.
     * - 'auto': Watch Vaadin's theme attribute and OS preference
     * - 'light'/'dark': Use explicit theme setting
     */
    private initializeThemeSystem(): void {
        this.themeManager.initialize(this.themeType, (theme) => {
            // Set data-ck-theme attribute on the component for CSS targeting
            this.setAttribute('data-ck-theme', theme);
            // Force repaint after theme change
            this.forceEditorRepaint();
        });
    }

    /**
     * Property changed handler
     */
    protected updated(changedProperties: PropertyValues): void {
        super.updated(changedProperties);

        if (changedProperties.has('editorData') && this.editor) {
            const currentData = this.editor.getData();
            if (currentData !== this.editorData) {
                this.editor.setData(this.editorData);
            }
        }

        if (changedProperties.has('isReadOnly') && this.editor) {
            this.updateReadOnly();
        }

        if (changedProperties.has('hideToolbar') && this.editor) {
            this.updateToolbarVisibility();
        }

        // Handle theme changes from Java backend
        if (changedProperties.has('themeType')) {
            this.handleThemeTypeChange();
        }
    }

    /**
     * Handle themeType property changes.
     * Delegates to ThemeManager.
     */
    private handleThemeTypeChange(): void {
        this.themeManager.handleThemeTypeChange(this.themeType);
    }

    /**
     * Force a repaint of CKEditor UI elements after theme change.
     * This ensures CSS variable changes are visually applied.
     *
     * Refactored to reduce nesting depth (max 3 levels).
     */
    private forceEditorRepaint(): void {
        this.triggerToolbarRepaint();
        this.triggerContainerRepaint();
        this.triggerEditableRepaint();
    }

    /**
     * Trigger toolbar repaint via mouse events.
     */
    private triggerToolbarRepaint(): void {
        const toolbar = this.querySelector('.ck.ck-toolbar') as HTMLElement;
        if (!toolbar) return;

        const enterEvent = new MouseEvent('mouseenter', {
            bubbles: true,
            cancelable: true,
            view: window
        });
        toolbar.dispatchEvent(enterEvent);

        this.toolbarRepaintTimeoutId = setTimeout(() => {
            this.toolbarRepaintTimeoutId = null;
            const leaveEvent = new MouseEvent('mouseleave', {
                bubbles: true,
                cancelable: true,
                view: window
            });
            toolbar.dispatchEvent(leaveEvent);
        }, TOOLBAR_REPAINT_DELAY_MS);
    }

    /**
     * Trigger editor container repaint via opacity trick.
     */
    private triggerContainerRepaint(): void {
        const editorContainer = this.querySelector('.ck.ck-editor') as HTMLElement;
        if (!editorContainer) return;

        const originalOpacity = editorContainer.style.opacity;
        editorContainer.style.opacity = REPAINT_OPACITY;
        void editorContainer.offsetHeight; // Force reflow
        const rafId = requestAnimationFrame(() => {
            if (this.isDisconnected) return;
            editorContainer.style.opacity = originalOpacity || '';
        });
        this.repaintRafIds.push(rafId);
    }

    /**
     * Trigger editable area repaint and dispatch theme-changed event.
     */
    private triggerEditableRepaint(): void {
        const editable = this.querySelector('.ck.ck-editor__editable') as HTMLElement;
        if (!editable) return;

        const rafId = requestAnimationFrame(() => {
            if (this.isDisconnected) return;
            const shouldFocusCycle = document.activeElement !== editable;

            if (shouldFocusCycle) {
                this.performFocusCycle(editable);
            } else {
                this.dispatchThemeChangedEvent();
            }
        });
        this.repaintRafIds.push(rafId);
    }

    /**
     * Perform focus/blur cycle and dispatch theme-changed event.
     */
    private performFocusCycle(editable: HTMLElement): void {
        const activeElement = document.activeElement as HTMLElement;
        editable.focus();

        const rafId = requestAnimationFrame(() => {
            if (this.isDisconnected) return;
            editable.blur();
            if (activeElement?.focus) {
                activeElement.focus();
            }
            this.dispatchThemeChangedEvent();
        });
        this.repaintRafIds.push(rafId);
    }

    /**
     * Dispatch theme-changed custom event.
     */
    private dispatchThemeChangedEvent(): void {
        this.dispatchEvent(new CustomEvent('theme-changed', {
            detail: { theme: this.themeManager.getCurrentTheme() },
            bubbles: true
        }));
    }

    /**
     * Get editor constructor based on type
     */
    private getEditorConstructor(): typeof ClassicEditor | typeof BalloonEditor | typeof InlineEditor | typeof DecoupledEditor {
        switch (this.editorType) {
            case 'balloon':
                return BalloonEditor;
            case 'inline':
                return InlineEditor;
            case 'decoupled':
                return DecoupledEditor;
            case 'classic':
            default:
                return ClassicEditor;
        }
    }

    /**
     * Resolve plugins from configuration.
     * Delegates to PluginResolver module for better separation of concerns.
     * Returns array of plugin constructors (both regular and context plugins).
     */
    private async resolvePlugins(): Promise<unknown[]> {
        const resolver = new PluginResolver(logger);
        const resolved = await resolver.resolvePlugins(this.plugins, {
            strictPluginLoading: this.strictPluginLoading,
            allowConfigRequiredPlugins: this.allowConfigRequiredPlugins,
        });

        // Report plugin load errors to backend (non-fatal)
        if (resolver.loadErrors.length > 0 && this.$server) {
            for (const errorMsg of resolver.loadErrors) {
                this.$server.fireEditorError(
                    'PLUGIN_LOAD_FAILED',
                    errorMsg,
                    'WARNING',
                    true,
                    ''
                );
            }
        }

        return resolved;
    }

    /**
     * Build editor configuration
     */
    private async buildConfig(): Promise<EditorConfig> {
        const resolvedPlugins = await this.resolvePlugins();

        // 评论权限强制插件：隐藏非当前用户评论的 Edit/Remove 按钮
        if (this.commentPermissionEnforcerEnabled && this.annotationSidebarEnabled) {
            resolvedPlugins.push(CommentPermissionEnforcer);
            logger.debug('CommentPermissionEnforcer plugin injected');
        }

        // Get translations for the specified language
        const translations = this.language !== 'en' ? TRANSLATION_REGISTRY[this.language] : undefined;

        // 协作模式下检查频道是否已有数据，有则移除 initialData 避免警告
        const configWithInitialData = await this.stripInitialDataIfChannelExists(this.config);

        let editorConfig: EditorConfig = {
            licenseKey: this.licenseKey,
            plugins: resolvedPlugins as EditorConfig['plugins'],
            language: this.language,
            ...(translations ? { translations: [translations] } : {}),
            ...configWithInitialData,
        };

        // Add toolbar if specified (but don't override config.toolbar if it has shouldNotGroupWhenFull)
        // config.toolbar takes precedence when it's an object with additional options
        if (this.toolbar && this.toolbar.length > 0) {
            // Only use this.toolbar if config.toolbar is not set or is a simple array
            const configToolbar = this.config?.toolbar;
            const configToolbarIsObject = configToolbar && typeof configToolbar === 'object' && !Array.isArray(configToolbar);
            if (!configToolbarIsObject) {
                editorConfig.toolbar = this.toolbar;
            }
        }

        // Add autosave configuration
        if (this.autosave) {
            editorConfig = {
                ...editorConfig,
                autosave: {
                    save: (editor: Editor) => this.handleAutosave(editor),
                    waitingTime: this.autosaveWaitingTime,
                },
            };
        }

        // Add general HTML support configuration
        if (this.ghsEnabled) {
            editorConfig = {
                ...editorConfig,
                htmlSupport: {
                    allow: [
                        {
                            name: /.*/,
                            attributes: true,
                            classes: true,
                            styles: true,
                        },
                    ],
                },
            };
        }

        // Add minimap for decoupled editor
        // Minimap shows a scaled-down preview of the editor content for navigation
        if (this.editorType === 'decoupled' && this.minimapEnabled) {
            const minimapContainer = this.querySelector('.minimap-container');
            if (minimapContainer) {
                editorConfig = {
                    ...editorConfig,
                    minimap: {
                        container: minimapContainer as HTMLElement,
                        // extraClasses applies to iframe body element
                        // 'minimap-body' sets A4 paper width (796px) for proper scaling
                        // 'ck-content' ensures content styling is applied
                        extraClasses: 'minimap-body ck-content',
                        // useSimplePreview renders content as boxes for better performance
                        // Useful for large documents where full rendering is too slow
                        useSimplePreview: this.minimapSimplePreview,
                    },
                };
                logger.debug('Minimap container configured, simplePreview:', this.minimapSimplePreview);
            } else {
                logger.warn('Minimap enabled but container .minimap-container not found');
            }
        }

        // Add document outline for decoupled editor
        if (this.editorType === 'decoupled' && this.documentOutlineEnabled) {
            const outlineContainer = this.querySelector('#editor-outline');
            logger.debug('Looking for outline container #editor-outline:', outlineContainer ? 'found' : 'not found');
            if (outlineContainer) {
                editorConfig = {
                    ...editorConfig,
                    ...{ documentOutline:
                        {
                            container: outlineContainer as HTMLElement,
                        }
                    },
                };
                logger.debug('Document outline container configured');
            } else {
                // Log warning if outline container not found but outline is enabled
                logger.warn('Document outline enabled but container #editor-outline not found in DOM');
            }
        }

        // Add annotation sidebar for collaboration features (Comments, TrackChanges, RevisionHistory)
        if (this.editorType === 'decoupled' && this.annotationSidebarEnabled) {
            const sidebarContainer = this.querySelector('#annotation-sidebar');
            if (sidebarContainer) {
                editorConfig = {
                    ...editorConfig,
                    sidebar: {
                        container: sidebarContainer as HTMLElement,
                        preventScrollOutOfView: true,
                    },
                };
                logger.debug('Annotation sidebar container configured');
            } else {
                logger.warn('Annotation sidebar enabled but container #annotation-sidebar not found');
            }

            const presenceListContainer = this.querySelector('#presence-list-container');
            if (presenceListContainer) {
                editorConfig = {
                    ...editorConfig,
                    presenceList: {
                        container: presenceListContainer as HTMLElement,
                    },
                };
                logger.debug('Presence list container configured');
            } else {
                logger.warn('Annotation sidebar enabled but container #presence-list-container not found');
            }

            // RevisionHistory 需要四个容器：editorContainer、viewerContainer、viewerEditorElement、viewerSidebarContainer
            const editorContainer = this.querySelector('#editor-container');
            const revisionHistoryContainer = this.querySelector('#revision-history-container');
            const revisionHistoryEditor = this.querySelector('#revision-history-editor');
            const revisionHistorySidebar = this.querySelector('#revision-history-sidebar');
            if (editorContainer && revisionHistoryContainer && revisionHistoryEditor && revisionHistorySidebar) {
                editorConfig = {
                    ...editorConfig,
                    revisionHistory: {
                        editorContainer: editorContainer as HTMLElement,
                        viewerContainer: revisionHistoryContainer as HTMLElement,
                        viewerEditorElement: revisionHistoryEditor as HTMLElement,
                        viewerSidebarContainer: revisionHistorySidebar as HTMLElement,
                    },
                };
                logger.debug('Revision history containers configured');
            } else {
                logger.warn('Revision history containers not fully found in DOM');
            }
        }

        // Add custom upload adapter for server-side file handling
        // This enables the UploadHandler Java API to receive uploaded files
        // Note: The adapter is set up after editor creation in setupCustomUploadAdapter()

        return editorConfig;
    }

    /**
     * 协作模式下检查频道是否已被初始化
     *
     * 当 config 同时包含 cloudServices（协作模式）和 initialData 时，
     * 通过 localStorage 记录已初始化的频道。首次加载时保留 initialData
     * 用于种子数据；后续加载检测到已初始化则移除 initialData，
     * 避免 "editor-initial-data-replaced-with-revision-data" 警告。
     *
     * localStorage key 格式: ck-channel-seeded:{channelId}
     *
     * @param config 原始编辑器配置
     * @returns 处理后的配置（可能移除了 initialData）
     */
    private async stripInitialDataIfChannelExists(
        config: Record<string, unknown>
    ): Promise<Record<string, unknown>> {
        // 仅在协作模式（有 cloudServices 配置）且设置了 initialData 时检查
        const cloudServices = config.cloudServices as Record<string, unknown> | undefined;
        const collaboration = config.collaboration as Record<string, unknown> | undefined;
        const hasInitialData = 'initialData' in config && config.initialData != null;

        if (!cloudServices?.tokenUrl || !collaboration?.channelId || !hasInitialData) {
            return config;
        }

        const channelId = collaboration.channelId as string;
        const storageKey = `ck-channel-seeded:${channelId}`;

        try {
            if (localStorage.getItem(storageKey)) {
                // 频道已被初始化过，移除 initialData 让 Cloud Services 加载已有数据
                logger.info(`频道 "${channelId}" 已初始化，移除 initialData 避免冲突`);
                const { initialData: _, ...configWithoutInitialData } = config;
                return configWithoutInitialData;
            }

            // 首次加载：标记频道为已初始化，保留 initialData 用于种子
            localStorage.setItem(storageKey, String(Date.now()));
            logger.info(`频道 "${channelId}" 首次初始化，使用 initialData 种子数据`);
        } catch {
            // localStorage 不可用（隐私模式等），保留 initialData 让 CKEditor 自行处理
            logger.warn('localStorage 不可用，保留 initialData');
        }

        return config;
    }

    /**
     * Create the CKEditor instance.
     * This is the main entry point that coordinates the editor creation process.
     * The method is split into smaller focused methods for better maintainability.
     */
    private async createEditor(): Promise<void> {
        // Pre-creation checks
        if (!this.canCreateEditor()) {
            return;
        }

        // Wait for any existing editor cleanup
        await this.waitForPreviousEditorCleanup();

        // Acquire creation lock
        this.isCreating = true;

        // Validate editor element exists
        const editorElement = this.querySelector(`[id="${CSS.escape(this.editorId)}"]`) as HTMLElement;
        if (!editorElement) {
            logger.error(`Editor element not found: #${this.editorId}`);
            this.isCreating = false;
            return;
        }

        // Execute the creation process
        this.createPromise = this.executeEditorCreation(editorElement);
        await this.createPromise;
    }

    /**
     * Check if editor creation can proceed.
     * Returns false if component is disconnected or creation is already in progress.
     */
    private canCreateEditor(): boolean {
        // Safety check: don't create if component is disconnected
        if (this.isDisconnected) {
            logger.debug(' Skipping editor creation - component is disconnected');
            return false;
        }

        // Prevent concurrent creation - if already creating, wait for the existing creation to finish
        if (this.isCreating && this.createPromise) {
            logger.debug(' Editor creation already in progress, waiting...');
            return false;
        }

        return true;
    }

    /**
     * Wait for any existing editor to be destroyed before creating a new one.
     */
    private async waitForPreviousEditorCleanup(): Promise<void> {
        if (this.editor || this.isDestroying) {
            logger.debug(' Waiting for previous editor cleanup...');
            if (this.destroyPromise) {
                await this.destroyPromise;
            }
        }
    }

    /**
     * Execute the actual editor creation process.
     * Wrapped in try/finally to ensure creation lock is always released.
     */
    private async executeEditorCreation(editorElement: HTMLElement): Promise<void> {
        try {
            // Create editor instance
            const initTimeMs = await this.createEditorInstance(editorElement);
            if (!this.editor) return;

            // Setup editor UI and features
            this.setupEditorUI();

            // Setup event handlers
            this.setupEditorHooks();

            // Apply custom styles
            this.applyCustomStyles();

            // Log initialization and notify server
            this.onEditorReady(initTimeMs);

        } catch (error) {
            logger.error('Failed to create CKEditor:', error);
            this.handleEditorCreationError(error);
        } finally {
            // Release creation lock - ensures execution regardless of success or failure
            this.isCreating = false;
        }
    }

    /**
     * Create the CKEditor instance with configuration.
     * Returns the initialization time in milliseconds.
     */
    private async createEditorInstance(editorElement: HTMLElement): Promise<number> {
        const EditorConstructor = this.getEditorConstructor();

        // Wait for Lit update to complete, ensuring full DOM render (including dynamic elements like outline container)
        await this.updateComplete;
        // Wait one extra frame to ensure the browser has finished layout
        await new Promise<void>(resolve => requestAnimationFrame(() => resolve()));

        const config = await this.buildConfig();

        // Double-check we're still connected after the await
        if (this.isDisconnected) {
            logger.debug(' Aborting editor creation - component disconnected during setup');
            return 0;
        }

        logger.info(`Starting editor creation with ${this.plugins.length} plugins...`);
        const startTime = performance.now();

        this.editor = await EditorConstructor.create(editorElement, config);

        const endTime = performance.now();
        const initTimeMs = endTime - startTime;
        logger.info(`Editor created in ${initTimeMs.toFixed(0)}ms`);

        // Set editor ID for reference
        (this.editor as unknown as { id: string }).id = this.editorId;

        return initTimeMs;
    }

    /**
     * Setup editor UI state: data, read-only, toolbar, dimensions, decoupled toolbar.
     */
    private setupEditorUI(): void {
        if (!this.editor) return;

        // Set initial data and track it
        if (this.editorData) {
            this.editor.setData(this.editorData);
        }
        this.lastKnownContent = this.editor.getData();

        // Set read-only state
        this.updateReadOnly();

        // Set toolbar visibility
        this.updateToolbarVisibility();

        // Set editor dimensions
        this.updateEditorDimensions();

        // Handle decoupled editor toolbar
        if (this.editorType === 'decoupled') {
            this.setupDecoupledToolbar();
        }
    }

    /**
     * Setup editor event listeners and hooks.
     */
    private setupEditorHooks(): void {
        if (!this.editor) return;

        // Setup event listeners
        this.setupEventListeners();

        // Setup custom upload adapter for server-side file handling
        this.setupCustomUploadAdapter();
    }

    /**
     * Apply custom CSS and toolbar styles.
     */
    private applyCustomStyles(): void {
        // Load custom CSS if specified
        if (this.overrideCssUrl) {
            this.loadCustomCss();
        }

        // Inject custom toolbar styles if specified
        if (this.toolbarStyle) {
            this.injectToolbarStyles();
        }

        // Inject minimap iframe styles if minimap is enabled
        if (this.minimapEnabled) {
            this.injectMinimapStyles();
        }
    }

    /**
     * Inject CSS styles directly into the minimap iframe.
     * This ensures the styles are applied correctly even if CKEditor's style cloning
     * doesn't capture all our custom CSS rules.
     * The minimap will inherit the editor's background and foreground colors.
     */
    private injectMinimapStyles(): void {
        // Poll for the minimap iframe instead of using a fixed delay.
        // The iframe is created asynchronously by CKEditor's minimap plugin.
        const maxAttempts = MINIMAP_INJECT_MAX_ATTEMPTS;
        let attempt = 0;

        const tryInject = (): void => {
            this.minimapInjectRafId = null;
            const minimapIframe = this.querySelector('.ck-minimap__iframe') as HTMLIFrameElement;
            if (!minimapIframe || !minimapIframe.contentDocument) {
                if (++attempt < maxAttempts) {
                    this.minimapInjectRafId = requestAnimationFrame(tryInject);
                    return;
                }
                logger.debug('Minimap iframe not found after polling, skipping style injection');
                return;
            }

            const iframeDoc = minimapIframe.contentDocument;

            // Check if styles already injected
            if (iframeDoc.getElementById('vaadin-ckeditor-minimap-styles')) {
                return;
            }

            // Get the main editor's computed styles for background and foreground colors
            const editorContent = this.querySelector('.ck-content');
            let backgroundColor = 'white';
            let textColor = 'inherit';

            if (editorContent) {
                const computedStyle = window.getComputedStyle(editorContent);
                backgroundColor = computedStyle.backgroundColor || 'white';
                textColor = computedStyle.color || 'inherit';
            }

            // Apply styles directly to iframe body element for maximum compatibility
            // This ensures colors are applied even if CSS selectors don't match
            const body = iframeDoc.body;
            if (body) {
                body.style.setProperty('background', backgroundColor, 'important');
                body.style.setProperty('background-color', backgroundColor, 'important');
                body.style.setProperty('color', textColor, 'important');
                body.style.setProperty('height', 'auto', 'important');
                body.style.setProperty('min-height', A4_MIN_HEIGHT_PX, 'important');
                body.style.setProperty('width', A4_WIDTH_PX, 'important');
                body.style.setProperty('min-width', A4_WIDTH_PX, 'important');
                body.style.setProperty('max-width', A4_WIDTH_PX, 'important');
                body.style.setProperty('margin', '0', 'important');
                body.style.setProperty('padding', '20mm 12mm', 'important');
                body.style.setProperty('box-sizing', 'border-box', 'important');
                body.style.setProperty('overflow', 'visible', 'important');
            }

            // Also create style element for html and nested elements
            const style = iframeDoc.createElement('style');
            style.id = 'vaadin-ckeditor-minimap-styles';
            style.textContent = `
                html {
                    height: auto !important;
                    min-height: 100% !important;
                }
                body.minimap-body .ck-content,
                body.minimap-body .ck.ck-editor__editable {
                    width: 100% !important;
                    min-height: 100% !important;
                    height: auto !important;
                    box-sizing: border-box !important;
                    background: ${backgroundColor} !important;
                    background-color: ${backgroundColor} !important;
                    color: ${textColor} !important;
                }
            `;
            iframeDoc.head.appendChild(style);
            logger.debug('Minimap styles injected with colors:', { backgroundColor, textColor });
        };

        this.minimapInjectRafId = requestAnimationFrame(tryInject);
    }

    /**
     * Called when editor is ready. Logs initialization info and notifies server.
     */
    private onEditorReady(initTimeMs: number): void {
        logger.debug('Editor initialized:', {
            'vaadin-ckeditor': this.version,
            editorId: this.editorId,
            editorType: this.editorType,
            plugins: this.plugins.map(p => p.name),
        });

        // 批注侧栏对齐：将每个 ck-sidebar-item 绝对定位到对应 ck-comment-marker 的位置
        if (this.annotationSidebarEnabled) {
            this.setupAnnotationSidebarSync();
        }

        // Fire editor ready event to Java backend
        if (this.$server) {
            this.$server.fireEditorReady(initTimeMs);
        }
    }

    /**
     * 批注侧栏滚动同步：将每个 ck-sidebar-item 绝对定位到对应 ck-comment-marker 的视口 Y 坐标。
     *
     * 文档编辑器有两层滚动：wrapper（外层）和 editable（内层）。
     * CKEditor 原生的 style.top 定位在两层滚动场景下不准确，
     * 本方法覆盖 ck-sidebar-item 的位置，使其始终与 marker 对齐。
     * 当批注重叠时自动堆叠（stack），保证最近的 marker 对应的批注排在最上面。
     */
    private setupAnnotationSidebarSync(): void {
        const wrapper = this.querySelector('.editor-container__editor-wrapper') as HTMLElement;
        const sidebar = this.querySelector('.annotation-sidebar-container') as HTMLElement;
        if (!wrapper || !sidebar) return;
        const editable = wrapper.querySelector('.ck-editor__editable') as HTMLElement;
        if (!editable) return;

        sidebar.style.overflow = 'visible';
        sidebar.style.position = 'relative';

        const syncPositions = () => {
            const items = sidebar.querySelectorAll<HTMLElement>('.ck-sidebar-item');
            const markers = editable.querySelectorAll<HTMLElement>('.ck-comment-marker');
            if (!items.length || !markers.length) return;

            const wrapperRect = wrapper.getBoundingClientRect();
            const sidebarRect = sidebar.getBoundingClientRect();
            const sidebarOffset = sidebarRect.top - wrapperRect.top;

            let lastBottom = -Infinity;
            const n = Math.min(items.length, markers.length);

            for (let i = 0; i < n; i++) {
                const markerRect = markers[i].getBoundingClientRect();
                const markerRelY = markerRect.top - wrapperRect.top;
                let desiredTop = markerRelY - sidebarOffset;

                // 堆叠：如果与上一个批注重叠，向下推
                if (desiredTop < lastBottom + 4) {
                    desiredTop = lastBottom + 4;
                }

                items[i].style.position = 'absolute';
                items[i].style.top = desiredTop + 'px';
                items[i].style.width = '100%';
                lastBottom = desiredTop + items[i].offsetHeight;
            }

            // 超出 markers 数量的 sidebar items（如新建评论的输入框）也绝对定位
            for (let i = n; i < items.length; i++) {
                items[i].style.position = 'absolute';
                items[i].style.top = (lastBottom + 4) + 'px';
                items[i].style.width = '100%';
                lastBottom += items[i].offsetHeight + 4;
            }
        };

        wrapper.addEventListener('scroll', syncPositions);
        editable.addEventListener('scroll', syncPositions);
        syncPositions();

        // MutationObserver 监听侧栏变化（新增/删除评论），自动重新对齐
        new MutationObserver(syncPositions).observe(sidebar, {
            childList: true,
            subtree: true,
            attributes: true
        });
    }

    /**
     * Setup event listeners for the editor
     * Listeners are saved as fields for proper cleanup during destroy
     */
    private setupEventListeners(): void {
        if (!this.editor) return;

        const editor = this.editor;

        // Track cursor position
        this.selectionChangeListener = () => {
            const activeEditor = this.editor;
            if (!activeEditor) return;
            this.cursorPosition = activeEditor.model.document.selection.getFirstPosition();
        };

        // Handle data changes
        this.dataChangeListener = () => {
            const activeEditor = this.editor;
            if (!activeEditor) return;
            const newContent = activeEditor.getData();

            // Fire content change event if content actually changed
            if (this.$server && newContent !== this.lastKnownContent) {
                // Use tracked change source, defaulting to USER_INPUT
                const source = this.apiChangeDepth > 0 ? 'API' : this.changeSource;
                this.$server.fireContentChange(this.lastKnownContent, newContent, source);
                this.lastKnownContent = newContent;
                // Reset change source after firing event
                this.changeSource = 'USER_INPUT';
            }

            if (this.sync && this.$server) {
                this.$server.setEditorData(newContent);
            }
        };

        // Handle focus changes
        this.focusChangeListener = (_evt: unknown, _data: unknown, isFocused: boolean) => {
            const activeEditor = this.editor;
            if (!activeEditor) return;
            if (!this.sync && !isFocused && this.$server) {
                this.$server.setEditorData(activeEditor.getData());
            }
        };

        // Handle read-only changes
        this.readOnlyChangeListener = (_evt: unknown, _propertyName: unknown, isReadOnly: boolean) => {
            const activeEditor = this.editor;
            if (!activeEditor) return;
            if (isReadOnly) {
                activeEditor.enableReadOnlyMode(this.editorId);
            } else {
                activeEditor.disableReadOnlyMode(this.editorId);
            }
        };

        editor.model.document.selection.on('change:range', this.selectionChangeListener);
        editor.model.document.on('change:data', this.dataChangeListener);
        editor.editing.view.document.on('change:isFocused', this.focusChangeListener);
        editor.on('change:isReadOnly', this.readOnlyChangeListener);

        // Track undo/redo operations for ChangeSource
        // Listen to command execution to detect undo/redo
        const undoCommand = editor.commands.get('undo');
        const redoCommand = editor.commands.get('redo');
        const undoRedoHandler = () => { this.changeSource = 'UNDO_REDO'; };
        if (undoCommand) {
            this.undoExecuteListener = undoCommand.on('execute', undoRedoHandler);
        }
        if (redoCommand) {
            this.redoExecuteListener = redoCommand.on('execute', undoRedoHandler);
        }

        // Track paste operations for ChangeSource
        this.clipboardInputListener = editor.editing.view.document.on('clipboardInput', () => {
            this.changeSource = 'PASTE';
        });

        // Track collaboration operations for ChangeSource (Premium feature)
        // Real-time collaboration uses a specific operation type
        this.setupCollaborationTracking(editor);
    }

    /**
     * Setup collaboration change tracking for Premium collaboration features.
     * Detects changes from real-time collaboration and sets changeSource accordingly.
     */
    private setupCollaborationTracking(editor: Editor): void {
        try {
            // Check if RealTimeCollaborativeEditing plugin is available
            // This plugin is only present when using CKEditor Premium collaboration features
            const hasCollaboration = editor.plugins.has('RealTimeCollaborativeEditing');

            if (hasCollaboration) {
                logger.debug('Collaboration plugin detected, setting up change tracking');

                // Listen to the model's applyOperation event to detect remote operations
                // Remote operations from collaboration have a specific baseVersion pattern
                this.applyOperationListener = editor.model.on('applyOperation', (_evt, args) => {
                    const operation = args[0] as { baseVersion?: number; isLocal?: boolean };

                    // Check if this is a remote operation (from collaboration)
                    // Remote operations typically have isLocal = false or can be detected
                    // by checking if the operation comes from the collaboration channel
                    if (operation && operation.isLocal === false) {
                        this.changeSource = 'COLLABORATION';
                    }
                }, { priority: 'highest' }) as unknown as { off: () => void };

                // Alternative: Listen to the collaboration channel directly if available
                try {
                    const cloudServices = editor.plugins.get('CloudServices') as {
                        on?: (event: string, callback: () => void) => { off: () => void };
                    };
                    if (cloudServices && cloudServices.on) {
                        this.cloudServicesSyncListener = cloudServices.on('change:syncInProgress', () => {
                            // When sync is in progress, next changes might be from collaboration
                            this.changeSource = 'COLLABORATION';
                        });
                    }
                } catch {
                    // CloudServices not available, which is fine
                }
            }
        } catch {
            // Collaboration plugins not loaded - this is expected for non-premium users
            logger.debug('Collaboration plugins not available, skipping collaboration tracking');
        }
    }

    /**
     * Setup custom upload adapter for server-side file handling
     * This enables the UploadHandler Java API to receive uploaded files
     */
    private setupCustomUploadAdapter(): void {
        if (!this.editor || !this.$server) return;

        try {
            // Get FileRepository plugin if available
            const fileRepository = this.editor.plugins.get('FileRepository') as {
                createUploadAdapter?: (loader: { file: Promise<File> }) => {
                    upload: () => Promise<{ default: string }>;
                    abort: () => void;
                };
            };

            if (fileRepository) {
                // Set up custom upload adapter factory
                fileRepository.createUploadAdapter = this.getUploadAdapterFactory();
                logger.debug('Custom upload adapter configured for server-side file handling');
            }
        } catch {
            // FileRepository plugin not loaded - this is fine, image upload might not be enabled
            logger.debug('FileRepository plugin not available, skipping upload adapter setup');
        }
    }

    /**
     * Setup decoupled editor toolbar and menu bar.
     * Mounts the toolbar and menu bar elements to their respective containers.
     * Follows the official CKEditor 5 Builder structure.
     */
    private setupDecoupledToolbar(): void {
        if (!this.editor || this.editorType !== 'decoupled') return;

        const decoupledEditor = this.editor as DecoupledEditor;

        // Mount toolbar
        const toolbarContainer = this.querySelector('#toolbar-container');
        const toolbarElement = decoupledEditor.ui?.view?.toolbar?.element;
        if (toolbarContainer && toolbarElement) {
            toolbarContainer.appendChild(toolbarElement);
            logger.debug('Toolbar mounted to #toolbar-container');
        }

        // Mount menu bar if available (CKEditor 5.40+)
        const menuBarContainer = this.querySelector('#menu-bar-container');
        const menuBarView = (decoupledEditor.ui?.view as { menuBarView?: { element?: HTMLElement } })?.menuBarView;
        const menuBarElement = menuBarView?.element;
        if (menuBarContainer && menuBarElement) {
            menuBarContainer.appendChild(menuBarElement);
            logger.debug('Menu bar mounted to #menu-bar-container');
        } else if (menuBarContainer) {
            // Hide empty menu bar container if no menu bar available
            (menuBarContainer as HTMLElement).style.display = 'none';
        }

        // Update UI to reflect the changes
        decoupledEditor.ui.update();
    }

    /**
     * Update read-only state
     */
    private updateReadOnly(): void {
        if (!this.editor) return;

        if (this.isReadOnly) {
            this.editor.enableReadOnlyMode(this.editorId);
        } else {
            this.editor.disableReadOnlyMode(this.editorId);
        }
    }

    /**
     * Update toolbar visibility
     */
    private updateToolbarVisibility(): void {
        if (!this.editor) return;

        const toolbar = (this.editor.ui as unknown as { view?: { toolbar?: { element?: HTMLElement } } }).view?.toolbar?.element;
        if (toolbar) {
            toolbar.style.display = this.hideToolbar ? 'none' : 'flex';
        }
    }

    /**
     * Update editor dimensions
     */
    private updateEditorDimensions(): void {
        if (!this.editor) return;

        this.editor.editing.view.change(writer => {
            const root = this.editor!.editing.view.document.getRoot();
            if (root) {
                if (this.editorHeight && this.editorHeight !== 'auto') {
                    writer.setStyle('height', this.editorHeight, root);
                }
                if (this.editorWidth && this.editorWidth !== 'auto') {
                    writer.setStyle('width', this.editorWidth, root);
                }
            }
        });
    }

    /**
     * Handle editor creation error - fire error event and trigger fallback
     */
    private handleEditorCreationError(error: unknown): void {
        const errorMessage = error instanceof Error ? error.message : String(error);
        const stackTrace = error instanceof Error ? error.stack || '' : '';

        // Fire error event to Java backend
        if (this.$server) {
            this.$server.fireEditorError(
                'EDITOR_CREATION_FAILED',
                errorMessage,
                'FATAL',
                false,
                stackTrace
            );
        }

        // Trigger fallback mode
        this.activateFallbackMode('Editor creation failed: ' + errorMessage, errorMessage);
    }

    /**
     * Activate fallback mode when editor fails.
     * Delegates to FallbackRenderer module.
     */
    private activateFallbackMode(reason: string, originalError: string): void {
        // Fire fallback event to Java backend
        if (this.$server) {
            this.$server.fireFallback(this.fallbackMode, reason, originalError);
        }

        // Initialize fallback renderer if needed
        const container = this.querySelector(`[id="${CSS.escape(this.editorId)}"]`) as HTMLElement;
        if (!container) return;

        if (!this.fallbackRenderer) {
            this.fallbackRenderer = new FallbackRenderer(container, this.$server);
        } else {
            this.fallbackRenderer.setServer(this.$server);
        }

        // Apply fallback based on mode
        if (this.fallbackMode === 'hidden') {
            this.style.display = 'none';
        } else {
            this.fallbackRenderer.render(this.fallbackMode as FallbackMode, this.editorData, reason);
        }
    }

    /**
     * Handle autosave
     * Uses try/finally to ensure Promise always resolves
     */
    private handleAutosave(editor: Editor): Promise<void> {
        try {
            if (this.$server) {
                this.$server.saveEditorData(editor.getData());
            }
        } catch (e) {
            logger.error('Autosave failed:', e);
        }
        return Promise.resolve();
    }

    /**
     * Load custom CSS
     * Keeps a reference for cleanup in disconnectedCallback
     */
    private loadCustomCss(): void {
        if (!this.overrideCssUrl) return;

        // Validate URL: only allow http(s) and relative paths (case-insensitive check)
        const normalizedUrl = this.overrideCssUrl.trim().toLowerCase();
        if (normalizedUrl.includes('javascript:') || normalizedUrl.includes('data:')) {
            logger.warn('Rejected overrideCssUrl with unsafe protocol:', this.overrideCssUrl);
            return;
        }

        // Remove existing custom CSS if any
        this.removeCustomCss();

        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = this.overrideCssUrl;
        link.id = `vaadin-ckeditor-custom-css-${this.editorId}`;
        document.head.appendChild(link);
        this.customCssLink = link;
    }

    /**
     * Remove custom CSS link from document head
     */
    private removeCustomCss(): void {
        if (this.customCssLink && this.customCssLink.parentNode) {
            this.customCssLink.parentNode.removeChild(this.customCssLink);
            this.customCssLink = undefined;
        }
    }

    /**
     * Public API: Update editor data from server
     * Marks the change as API-originated for proper ChangeSource tracking
     */
    public updateData(value: string): void {
        if (this.editor) {
            this.apiChangeDepth++;
            try {
                this.editor.setData(value || '');
            } finally {
                // Decrement after a microtask to ensure change event fires first
                queueMicrotask(() => {
                    this.apiChangeDepth--;
                });
            }
        }
    }

    /**
     * Public API: Set read-only mode
     */
    public setReadOnly(readOnly: boolean): void {
        this.isReadOnly = readOnly;
        this.updateReadOnly();
    }

    /**
     * Public API: Insert text at cursor position
     */
    public insertText(text: string): void {
        if (this.editor && this.cursorPosition) {
            this.editor.model.change(writer => {
                this.editor!.model.insertContent(writer.createText(text), this.cursorPosition as Parameters<typeof this.editor.model.insertContent>[1]);
            });
        }
    }

    /**
     * Public API: Resolve a pending upload from server.
     * Called by server after processing the upload via UploadHandler.
     * Delegates to UploadAdapterManager.
     * @param uploadId - The upload ID returned from handleFileUpload
     * @param url - The URL of the uploaded file (null if error)
     * @param errorMessage - Error message if upload failed (null if success)
     */
    public _resolveUpload(uploadId: string, url: string | null, errorMessage: string | null): void {
        if (this.uploadManager) {
            this.uploadManager.resolveUpload(uploadId, url, errorMessage);
        } else {
            logger.warn(`No upload manager available for upload ID: ${uploadId}`);
        }
    }

    /**
     * Get the upload adapter factory from UploadAdapterManager.
     * Initializes the upload manager if not already done.
     * Returns an upload adapter factory function for CKEditor configuration.
     */
    private getUploadAdapterFactory(): (loader: { file: Promise<File> }) => { upload: () => Promise<{ default: string }>, abort: () => void } {
        if (!this.uploadManager) {
            this.uploadManager = new UploadAdapterManager(this.editorId, logger);
        }
        this.uploadManager.setServer(this.$server);
        return this.uploadManager.createUploadAdapterFactory();
    }

    /**
     * Public API: Destroy the editor instance.
     * Safe to call multiple times - uses reentrance protection.
     *
     * IMPORTANT: This method now uses a safer cleanup strategy that avoids
     * the page freeze issue caused by CKEditor 5's internal cleanup conflicting
     * with Vaadin's DOM management.
     */
    public async destroyEditor(): Promise<void> {
        logger.debug(' destroyEditor() START, isDestroying:', this.isDestroying, 'hasEditor:', !!this.editor, 'isDisconnected:', this.isDisconnected);

        // Prevent reentrance - return existing promise if already destroying
        if (this.isDestroying) {
            logger.debug(' destroyEditor() already in progress, returning existing promise');
            return this.destroyPromise ?? Promise.resolve();
        }

        const editor = this.editor;
        if (!editor) {
            logger.debug(' destroyEditor() no editor to destroy');
            return;
        }

        logger.debug(' destroyEditor() proceeding with destroy, editor.state:', editor.state);
        this.isDestroying = true;

        this.destroyPromise = (async () => {
            try {
                // Step 1: Clear editor reference FIRST to prevent any callbacks
                // from accessing the editor during destruction
                this.editor = null;
                this.cursorPosition = null;

                // Step 2: Remove all event listeners BEFORE destroy
                // Use try-catch for each to ensure all get attempted
                try {
                    if (this.selectionChangeListener) {
                        editor.model.document.selection.off('change:range', this.selectionChangeListener);
                    }
                } catch (e) { /* ignore */ }

                try {
                    if (this.dataChangeListener) {
                        editor.model.document.off('change:data', this.dataChangeListener);
                    }
                } catch (e) { /* ignore */ }

                try {
                    if (this.focusChangeListener) {
                        editor.editing.view.document.off('change:isFocused', this.focusChangeListener);
                    }
                } catch (e) { /* ignore */ }

                try {
                    if (this.readOnlyChangeListener) {
                        editor.off('change:isReadOnly', this.readOnlyChangeListener);
                    }
                } catch (e) { /* ignore */ }

                // Remove undo/redo/clipboard listeners
                try {
                    if (this.undoExecuteListener) {
                        this.undoExecuteListener.off();
                    }
                } catch (e) { /* ignore */ }

                try {
                    if (this.redoExecuteListener) {
                        this.redoExecuteListener.off();
                    }
                } catch (e) { /* ignore */ }

                try {
                    if (this.clipboardInputListener) {
                        this.clipboardInputListener.off();
                    }
                } catch (e) { /* ignore */ }

                // Remove collaboration tracking listeners
                try {
                    if (this.applyOperationListener) {
                        this.applyOperationListener.off();
                    }
                } catch (e) { /* ignore */ }

                try {
                    if (this.cloudServicesSyncListener) {
                        this.cloudServicesSyncListener.off();
                    }
                } catch (e) { /* ignore */ }

                // Clear listener references
                this.selectionChangeListener = undefined;
                this.dataChangeListener = undefined;
                this.focusChangeListener = undefined;
                this.readOnlyChangeListener = undefined;
                this.undoExecuteListener = undefined;
                this.redoExecuteListener = undefined;
                this.clipboardInputListener = undefined;
                this.applyOperationListener = undefined;
                this.cloudServicesSyncListener = undefined;

                // Step 3: For decoupled editor, remove toolbar from DOM
                if (this.editorType === 'decoupled') {
                    try {
                        const toolbarElement = (editor as DecoupledEditor).ui?.view?.toolbar?.element;
                        if (toolbarElement?.parentElement) {
                            toolbarElement.parentElement.removeChild(toolbarElement);
                        }
                    } catch (e) { /* ignore */ }
                }

                // Step 4: If component is disconnected from DOM, skip destroy()
                // CKEditor's destroy() can hang when the DOM is already detached
                if (this.isDisconnected) {
                    logger.debug(' Skipping editor.destroy() - component already disconnected, letting GC handle cleanup');
                    return;
                }

                logger.debug(' About to call requestIdleCallback/setTimeout for editor.destroy()');

                // Step 5: Use requestIdleCallback (or setTimeout fallback) to defer destroy
                // This prevents blocking the main thread and allows Vaadin to complete its work
                await new Promise<void>((resolve) => {
                    const doDestroy = async () => {
                        try {
                            // Check editor state before destroying
                            if (editor.state === 'ready') {
                                await editor.destroy();
                                logger.debug(' Editor destroyed successfully');
                            } else {
                                logger.debug(' Editor not in ready state, skipping destroy');
                            }
                        } catch (error) {
                            // Log but don't throw - destruction errors shouldn't break the app
                            logger.warn('Error during destroy (non-fatal):', error);
                        }
                        resolve();
                    };

                    // Use requestIdleCallback if available, otherwise use setTimeout
                    if ('requestIdleCallback' in window) {
                        (window as typeof window & { requestIdleCallback: (cb: () => void, opts?: { timeout: number }) => number })
                            .requestIdleCallback(() => doDestroy(), { timeout: DESTROY_IDLE_TIMEOUT_MS });
                    } else {
                        setTimeout(() => doDestroy(), 0);
                    }
                });

            } catch (error) {
                logger.error('Failed to destroy editor:', error);
            } finally {
                logger.debug(' destroyEditor() FINALLY block, cleaning up state');
                this.isDestroying = false;
            }
        })();

        // Clean up destroyPromise after the async IIFE settles,
        // so callers who awaited the returned promise see it resolve correctly.
        this.destroyPromise.finally(() => {
            this.destroyPromise = null;
        });

        logger.debug(' destroyEditor() returning promise');
        return this.destroyPromise;
    }

    /**
     * Render the component
     *
     * For decoupled editor, the structure follows the official CKEditor 5 Builder layout:
     * - editor-container_document-editor: main container with border
     * - editor-container__menu-bar: menu bar mount point (optional)
     * - editor-container__toolbar: toolbar mount point
     * - editor-container__editor-wrapper: scrollable wrapper containing sidebar and editor
     *   - editor-container__sidebar: Document Outline container (when enabled)
     *   - editor-container__editor: editor wrapper with A4 styling
     */
    render() {
        if (this.editorType === 'decoupled') {
            const includeOutlineClass = this.documentOutlineEnabled ? 'editor-container_include-outline' : '';
            const includeMinimapClass = this.minimapEnabled ? 'editor-container_include-minimap' : '';
            const includeAnnotationClass = this.annotationSidebarEnabled ? 'editor-container_include-annotations' : '';
            // Apply custom editor height if specified (otherwise uses CSS default of 700px)
            const heightStyle = this.editorHeight && this.editorHeight !== 'auto'
                ? `--ck-editor-height: ${this.editorHeight};`
                : '';
            return html`
                <div class="editor-container editor-container_document-editor ${includeOutlineClass} ${includeMinimapClass} ${includeAnnotationClass}"
                     id="editor-container"
                     style="${heightStyle}">
                    <div class="editor-container__menu-bar" id="menu-bar-container"></div>
                    <div class="editor-container__toolbar" id="toolbar-container"></div>
                    <div class="editor-container__editor-wrapper">
                        <div class="editor-container__sidebar" id="editor-outline" style="display: ${this.documentOutlineEnabled ? 'block' : 'none'}"></div>
                        <div class="editor-container__editor">
                            <div id="${this.editorId}" class="editor-content"></div>
                        </div>
                        <div class="minimap-container" style="display: ${this.minimapEnabled ? 'block' : 'none'}"></div>
                        <div class="annotation-sidebar-wrapper" style="display: ${this.annotationSidebarEnabled ? 'flex' : 'none'}">
                            <div class="presence-list-container" id="presence-list-container"></div>
                            <div class="annotation-sidebar-container" id="annotation-sidebar"></div>
                        </div>
                    </div>
                </div>
                <div class="revision-history" id="revision-history-container" style="display: none;">
                    <div class="revision-history__wrapper">
                        <div class="revision-history__editor" id="revision-history-editor"></div>
                        <div class="revision-history__sidebar" id="revision-history-sidebar"></div>
                    </div>
                </div>
            `;
        }

        return html`
            <div class="editor-container">
                <div id="${this.editorId}" class="editor-content"></div>
            </div>
        `;
    }

    /**
     * Called when component is connected to the DOM.
     */
    connectedCallback(): void {
        super.connectedCallback();
        logger.debug(' connectedCallback, editorId:', this.editorId);
        this.isDisconnected = false;

        // Suppress CKEditor Pagination plugin internal errors (non-fatal)
        if (!this.paginationErrorHandler) {
            this.paginationErrorHandler = (event: PromiseRejectionEvent) => {
                const err = event.reason;
                if (err instanceof TypeError &&
                    err.stack?.includes('PageStarterInfoToPageBreakInfo')) {
                    event.preventDefault();
                    logger.debug('Suppressed Pagination plugin internal error:', err.message);
                }
            };
            window.addEventListener('unhandledrejection', this.paginationErrorHandler);
        }

        // Setup scroll handler for sticky panel (must be called on each connection)
        this.setupStickyPanelObserver();
    }

    /**
     * Validate a CSS property value to prevent CSS injection.
     * Rejects values containing characters that could break out of a CSS rule.
     */
    private static isSafeCssValue(value: string): boolean {
        return !/[{};]/.test(value);
    }

    /**
     * Inject custom toolbar styles based on toolbarStyle configuration.
     * Uses scoped CSS selectors to ensure multi-instance isolation.
     */
    private injectToolbarStyles(): void {
        if (!this.toolbarStyle || !this.editorId) {
            return;
        }

        // Remove existing style element if any
        this.removeToolbarStyles();

        const style = this.toolbarStyle;
        // Escape editorId for safe use in CSS attribute selector
        const safeEditorId = this.editorId.replace(/[\\"]/g, '');
        const scope = `vaadin-ckeditor[editor-id="${safeEditorId}"]`;
        const rules: string[] = [];

        // Helper to safely add a CSS value (rejects values with injection characters)
        const safe = VaadinCKEditor.isSafeCssValue;

        // Global toolbar styles
        if (style.background || style.borderColor || style.borderRadius) {
            const toolbarProps: string[] = [];
            if (style.background && safe(style.background)) toolbarProps.push(`background: ${style.background} !important`);
            if (style.borderColor && safe(style.borderColor)) toolbarProps.push(`border-color: ${style.borderColor} !important`);
            if (style.borderRadius && safe(style.borderRadius)) toolbarProps.push(`border-radius: ${style.borderRadius} !important`);
            if (toolbarProps.length > 0) {
                rules.push(`${scope} .ck.ck-toolbar { ${toolbarProps.join('; ')}; }`);
            }
        }

        // Global button styles
        if (style.buttonBackground && safe(style.buttonBackground)) {
            rules.push(`${scope} .ck.ck-toolbar .ck-button { background: ${style.buttonBackground} !important; }`);
        }
        if (style.buttonHoverBackground && safe(style.buttonHoverBackground)) {
            rules.push(`${scope} .ck.ck-toolbar .ck-button:hover:not(.ck-disabled) { background: ${style.buttonHoverBackground} !important; }`);
        }
        if (style.buttonActiveBackground && safe(style.buttonActiveBackground)) {
            rules.push(`${scope} .ck.ck-toolbar .ck-button:active:not(.ck-disabled) { background: ${style.buttonActiveBackground} !important; }`);
        }
        if (style.buttonOnBackground && safe(style.buttonOnBackground)) {
            rules.push(`${scope} .ck.ck-toolbar .ck-button.ck-on { background: ${style.buttonOnBackground} !important; }`);
        }
        if (style.buttonOnColor && safe(style.buttonOnColor)) {
            rules.push(`${scope} .ck.ck-toolbar .ck-button.ck-on { color: ${style.buttonOnColor} !important; }`);
        }
        if (style.iconColor && safe(style.iconColor)) {
            rules.push(`${scope} .ck.ck-toolbar .ck-icon { color: ${style.iconColor} !important; }`);
        }

        // Per-button styles
        if (style.buttonStyles) {
            for (const [buttonName, buttonStyle] of Object.entries(style.buttonStyles)) {
                // Sanitize buttonName to prevent CSS injection via attribute selector
                const safeName = buttonName.replace(/[\\"\]]/g, '');
                if (!safeName) continue;
                const btnSelector = `${scope} .ck.ck-toolbar .ck-button[data-cke-tooltip-text*="${safeName}"]`;
                if (buttonStyle.background && safe(buttonStyle.background)) {
                    rules.push(`${btnSelector} { background: ${buttonStyle.background} !important; }`);
                }
                if (buttonStyle.hoverBackground && safe(buttonStyle.hoverBackground)) {
                    rules.push(`${btnSelector}:hover:not(.ck-disabled) { background: ${buttonStyle.hoverBackground} !important; }`);
                }
                if (buttonStyle.activeBackground && safe(buttonStyle.activeBackground)) {
                    rules.push(`${btnSelector}:active:not(.ck-disabled) { background: ${buttonStyle.activeBackground} !important; }`);
                }
                if (buttonStyle.iconColor && safe(buttonStyle.iconColor)) {
                    rules.push(`${btnSelector} .ck-icon { color: ${buttonStyle.iconColor} !important; }`);
                }
            }
        }

        // Only inject if we have rules
        if (rules.length > 0) {
            const styleEl = document.createElement('style');
            styleEl.id = `vaadin-ckeditor-toolbar-style-${this.editorId}`;
            styleEl.textContent = rules.join('\n');
            document.head.appendChild(styleEl);
            this.toolbarStyleElement = styleEl;
            logger.debug('Injected toolbar styles for editor:', this.editorId);
        }
    }

    /**
     * Remove custom toolbar styles from the document.
     * Called during cleanup to prevent style leakage.
     */
    private removeToolbarStyles(): void {
        if (this.toolbarStyleElement) {
            this.toolbarStyleElement.remove();
            this.toolbarStyleElement = undefined;
            logger.debug('Removed toolbar styles for editor:', this.editorId);
        }
        // Also try to remove by ID in case reference was lost
        if (this.editorId) {
            const existingStyle = document.getElementById(`vaadin-ckeditor-toolbar-style-${this.editorId}`);
            if (existingStyle) {
                existingStyle.remove();
            }
        }
    }

    // Pagination plugin error suppression handler
    // CKEditor 5 Pagination plugin has an internal bug in _mapElementPageStarterInfoToPageBreakInfo
    // that throws "Cannot read properties of undefined (reading 'parent')" during page break
    // recalculation after dimension changes. This is non-fatal — the editor works correctly.
    private paginationErrorHandler: ((event: PromiseRejectionEvent) => void) | null = null;

    // Sticky panel scroll handler state
    private stickyPanelScrollHandler: (() => void) | null = null;
    private stickyPanelScrollContainer: Element | null = null;
    private stickyPanelSetupTimeoutId: ReturnType<typeof setTimeout> | null = null;
    private stickyPanelScrollTicking = false;

    /**
     * Set up scroll handler to override CKEditor's sticky panel inline styles.
     * CKEditor's sticky panel sets inline styles (width, top, margin-left) on scroll
     * that cannot be overridden by CSS !important, so we must remove them via JavaScript.
     *
     * Uses requestAnimationFrame throttling to avoid performance issues during scrolling.
     */
    private setupStickyPanelObserver(): void {
        if (this.stickyPanelScrollHandler) {
            return; // Already set up
        }

        /**
         * Clean sticky panel inline styles.
         * This removes CKEditor's inline positioning that conflicts with our CSS sticky implementation.
         */
        const cleanStickyPanelStylesImpl = () => {
            // Clean sticky panel content inline styles
            const stickyContent = this.querySelector('.ck-sticky-panel__content');
            if (stickyContent instanceof HTMLElement) {
                // Use cssText to forcefully override all inline styles
                stickyContent.style.cssText = 'width: auto !important; position: static !important;';
            }

            // Hide placeholder
            const placeholder = this.querySelector('.ck-sticky-panel__placeholder');
            if (placeholder instanceof HTMLElement) {
                placeholder.style.cssText = 'display: none !important; height: 0 !important;';
            }
        };

        /**
         * Throttled scroll handler using requestAnimationFrame.
         * Ensures we only run style cleanup once per animation frame.
         */
        const throttledScrollHandler = () => {
            if (!this.stickyPanelScrollTicking) {
                this.stickyPanelScrollTicking = true;
                requestAnimationFrame(() => {
                    if (!this.isDisconnected) {
                        cleanStickyPanelStylesImpl();
                    }
                    this.stickyPanelScrollTicking = false;
                });
            }
        };

        this.stickyPanelScrollHandler = throttledScrollHandler;

        /**
         * Find the scroll container (parent with overflow: auto/scroll).
         * Only checks vertical overflow since sticky positioning is vertical.
         */
        const findScrollContainer = (): Element | null => {
            let el: Element | null = this.parentElement;
            while (el) {
                const style = window.getComputedStyle(el);
                // Check for vertical scrolling (overflow or overflow-y)
                if (style.overflow === 'auto' || style.overflow === 'scroll' ||
                    style.overflowY === 'auto' || style.overflowY === 'scroll') {
                    return el;
                }
                el = el.parentElement;
            }
            return null;
        };

        // Set up after a short delay to let CKEditor initialize
        this.stickyPanelSetupTimeoutId = setTimeout(() => {
            // Guard against disconnected state (if component was unmounted during delay)
            if (this.isDisconnected) {
                return;
            }

            // Initial cleanup
            cleanStickyPanelStylesImpl();

            // Find scroll container and add scroll listener
            this.stickyPanelScrollContainer = findScrollContainer();
            if (this.stickyPanelScrollContainer && this.stickyPanelScrollHandler) {
                this.stickyPanelScrollContainer.addEventListener('scroll', this.stickyPanelScrollHandler);
                logger.debug('Sticky panel scroll handler attached to:', this.stickyPanelScrollContainer.className);
            }

            // Also listen on window scroll as fallback
            if (this.stickyPanelScrollHandler) {
                window.addEventListener('scroll', this.stickyPanelScrollHandler);
            }
        }, STICKY_PANEL_SETUP_DELAY_MS);
    }

    /**
     * Clean up the sticky panel scroll handler and timeout.
     */
    private cleanupStickyPanelObserver(): void {
        // Clear pending timeout if component disconnected before setup completed
        if (this.stickyPanelSetupTimeoutId) {
            clearTimeout(this.stickyPanelSetupTimeoutId);
            this.stickyPanelSetupTimeoutId = null;
        }

        if (this.stickyPanelScrollHandler) {
            if (this.stickyPanelScrollContainer) {
                this.stickyPanelScrollContainer.removeEventListener('scroll', this.stickyPanelScrollHandler);
                this.stickyPanelScrollContainer = null;
            }
            window.removeEventListener('scroll', this.stickyPanelScrollHandler);
            this.stickyPanelScrollHandler = null;
        }

        this.stickyPanelScrollTicking = false;
    }

    /**
     * Cleanup on disconnect.
     * Uses deferred cleanup to avoid blocking Vaadin's DOM operations.
     */
    disconnectedCallback(): void {
        logger.debug('disconnectedCallback START, editorId:', this.editorId, 'hasEditor:', !!this.editor);
        // Mark as disconnected FIRST - this prevents synchronous destroy
        this.isDisconnected = true;

        // Clean up theme manager (observers and dark theme)
        this.themeManager.cleanup();

        // Clean up pagination error handler
        if (this.paginationErrorHandler) {
            window.removeEventListener('unhandledrejection', this.paginationErrorHandler);
            this.paginationErrorHandler = null;
        }

        // Clean up sticky panel observer
        this.cleanupStickyPanelObserver();

        // Clean up toolbar repaint timer
        if (this.toolbarRepaintTimeoutId) {
            clearTimeout(this.toolbarRepaintTimeoutId);
            this.toolbarRepaintTimeoutId = null;
        }

        // Clean up minimap injection polling
        if (this.minimapInjectRafId !== null) {
            cancelAnimationFrame(this.minimapInjectRafId);
            this.minimapInjectRafId = null;
        }

        // Clean up repaint rAF IDs
        for (const rafId of this.repaintRafIds) {
            cancelAnimationFrame(rafId);
        }
        this.repaintRafIds = [];

        // Clean up custom CSS
        this.removeCustomCss();

        // Clean up custom toolbar styles
        this.removeToolbarStyles();

        // Clean up upload manager - reject all pending uploads
        if (this.uploadManager) {
            this.uploadManager.cleanup();
        }

        // Clean up fallback renderer
        if (this.fallbackRenderer) {
            this.fallbackRenderer.clear();
        }

        super.disconnectedCallback();

        // Defer cleanup to avoid blocking the main thread
        // and to let Vaadin complete its DOM operations first
        queueMicrotask(() => {
            logger.debug('disconnectedCallback microtask executing');
            void this.destroyEditor();
        });
        logger.debug('disconnectedCallback END (microtask scheduled)');
    }
}

