/**
 * Vaadin CKEditor 5 Web Component
 *
 * A modular CKEditor 5 integration for Vaadin using the official ckeditor5 npm package.
 * Plugins are loaded dynamically based on configuration from the Java backend.
 */
import { LitElement, html, css, render, nothing, PropertyValues } from 'lit';
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
import {
    buildCreateConfig,
    normalizeAIConfig48,
    normalizeRootConfig,
    stripInitialDataIfChannelSeeded,
    type RootConfig,
} from './editor-config-normalizer';
import { shouldRefreshSourceView } from './source-editing-refresh';
import { decideDataChange } from './data-change-decision';
import { replaceObserver, disposeObserver } from './observer-lifecycle';
import { shouldRecreateEditor } from './reconnect-decision';
import { createRefcount } from './dark-theme-refcount';
import { isAllowedCssUrl } from './css-url-validator';
import { shouldLoadMediaEmbedResize, loadMediaEmbedResizePlugin } from './media-embed-resize';

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
/**
 * 销毁「孤儿」编辑器时的最长等待时间。
 * 该场景下组件已从 DOM 断开，而 CKEditor 的 destroy() 在 detached 状态下可能永不 settle，
 * 因此必须设上界，避免创建锁与补偿重建被永久阻塞。
 */
const ORPHAN_DESTROY_TIMEOUT_MS = 2000;
/**
 * 编辑器容器的 class。
 * 与 render() 中 `<div id="${editorId}" class="...">` 保持一致——
 * detachStaleEditorContainer() 重建容器时用它还原「干净」状态，
 * 不能沿用旧节点的 className（那上面可能已被 CKEditor 注入运行时 class）。
 */
const EDITOR_CONTENT_CLASS = 'editor-content';
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

// NOTE: Premium features CSS (ckeditor5-premium-features/ckeditor5-premium-features.css) is loaded
// dynamically in plugin-resolver.ts when premium plugins are used, to avoid baseline payload increase.

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
    @property({ type: Boolean }) isEnabled = true;
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
    /**
     * 当启用时，为 AI Chat/Assistant 插件创建侧栏容器（仅 decoupled 编辑器）。
     * AI 插件在 sidebar 模式下需要一个 DOM 元素作为 config.ai.container.element。
     *
     * @default false
     */
    @property({ type: Boolean }) aiSidebarEnabled = false;
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
    @state() private aiSidebarCollapsed = true;

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
    /**
     * 本次创建是否以「孤儿」收场（创建期间组件断开，实例已就地销毁）。
     * 由 executeEditorCreation 的 finally 在释放 isCreating 后消费，触发补偿重建。
     */
    private pendingOrphanRecreate = false;
    private createPromise: Promise<void> | null = null;

    // CKEditor 48 root config built during buildConfig(), consumed by createEditorInstance()
    private pendingRootConfig: RootConfig = {};

    // Listener cleanup functions (undo/redo/clipboard/collaboration)
    private listenerCleanups: Array<() => void> = [];

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

    // AI sidebar collapse observer for cleanup
    private aiSidebarCollapseObserver?: MutationObserver;
    private annotationSidebarObserver?: MutationObserver;
    /** 注销标注侧栏 scroll 监听的回调；由 setupAnnotationSidebarSync 设置。 */
    private annotationScrollSyncDispose?: () => void;

    // Server communication
    private $server?: VaadinServer;

    // Version info — keep in sync with VaadinCKEditor.java VERSION constant
    private readonly version = '5.4.1';

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

        // createEditor 内部虽已捕获创建过程中的错误，但它自身在进入 try 之前会
        // await 上一次的 destroyPromise；若那个 promise 被 reject，这里不加 catch
        // 就会变成 unhandled rejection。显式兜底以保证错误走既有日志路径。
        void this.createEditor().catch((e) => {
            logger.error(' createEditor() failed unexpectedly:', e);
        });
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
                // 必须走 updateData 而不是直接 setData：
                // (1) updateData 会递增 apiChangeDepth，使随之而来的 change:data 被
                //     decideDataChange 判定为「API 触发」；直接 setData 则 depth 为 0，
                //     服务端推来的内容会被当成用户输入回传给 $server.setEditorData
                //     （issue #38 修复过的问题，会从这条属性路径重新出现）；
                // (2) updateData 还会调用 refreshSourceViewIfActive()，避免源码视图
                //     仍显示旧快照（issue #57）。
                this.updateData(this.editorData);
            }
        }

        if (changedProperties.has('isReadOnly') && this.editor) {
            this.updateReadOnly();
        }

        if (changedProperties.has('isEnabled') && this.editor) {
            this.updateEnabled();
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

        // 嵌入媒体缩放（issue #71）：MediaEmbedResize 由 umbrella ckeditor5 导出，
        // 但属功能性 premium（依赖的 editing 子插件 isPremiumPlugin=true），
        // 故启用时才从 ckeditor5 按需动态加载，加载失败（如缺 license）静默降级。
        if (shouldLoadMediaEmbedResize(this.config)) {
            const resizePlugin = await loadMediaEmbedResizePlugin();
            if (resizePlugin) {
                resolvedPlugins.push(resizePlugin);
                logger.debug('MediaEmbedResize plugin injected');
            } else {
                logger.warn('MediaEmbedResize requested but could not be loaded from ckeditor5 (a commercial license may be required)');
            }
        }

        // Get translations for the specified language
        const translations = this.language !== 'en' ? TRANSLATION_REGISTRY[this.language] : undefined;

        // 协作模式下检查频道是否已有数据，有则移除 initialData 避免警告
        const configAfterChannel = await this.stripInitialDataIfChannelExists(this.config);

        // CKEditor 48 配置规范化：兼容旧顶层 initialData/placeholder/label 与 AI v47 字段
        const { config: configAfterAi, warnings: aiWarnings } = normalizeAIConfig48(configAfterChannel);
        const { rootConfig, remainingConfig, warnings: rootWarnings } =
            normalizeRootConfig(configAfterAi, this.editorType);

        this.pendingRootConfig = rootConfig;
        this.warnConfigMigration([...aiWarnings, ...rootWarnings]);

        let editorConfig: EditorConfig = {
            licenseKey: this.licenseKey,
            plugins: resolvedPlugins as EditorConfig['plugins'],
            language: this.language,
            ...(translations ? { translations: [translations] } : {}),
            ...remainingConfig,
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
        // 支持 decoupled 和 balloon 编辑器类型的协作侧栏
        if ((this.editorType === 'decoupled' || this.editorType === 'balloon') && this.annotationSidebarEnabled) {
            const sidebarContainer = this.querySelector('#annotation-sidebar');
            if (sidebarContainer) {
                (editorConfig as Record<string, unknown>).sidebar = {
                    container: sidebarContainer as HTMLElement,
                    preventScrollOutOfView: true,
                };
                logger.debug('Annotation sidebar container configured');
            } else {
                logger.warn('Annotation sidebar enabled but container #annotation-sidebar not found');
            }

            const presenceListContainer = this.querySelector('#presence-list-container');
            if (presenceListContainer) {
                (editorConfig as Record<string, unknown>).presenceList = {
                    container: presenceListContainer as HTMLElement,
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
                (editorConfig as Record<string, unknown>).revisionHistory = {
                    editorContainer: editorContainer as HTMLElement,
                    viewerContainer: revisionHistoryContainer as HTMLElement,
                    viewerEditorElement: revisionHistoryEditor as HTMLElement,
                    viewerSidebarContainer: revisionHistorySidebar as HTMLElement,
                };
                logger.debug('Revision history containers configured');
            } else {
                logger.warn('Revision history containers not fully found in DOM');
            }
        }

        // AI sidebar container: resolve ai.container.element to actual DOM element
        // CKEditor AI plugin in sidebar mode requires config.ai.container.element to be an HTMLElement
        if (this.editorType === 'decoupled' && this.aiSidebarEnabled) {
            const aiConfig = (editorConfig as Record<string, unknown>).ai as Record<string, unknown> | undefined;
            if (!aiConfig) {
                logger.warn('aiSidebarEnabled is true but no "ai" config provided. AI plugins require config.ai with provider settings.');
            } else {
                const containerConfig = aiConfig.container as Record<string, unknown> | undefined;
                if (containerConfig && containerConfig.type === 'sidebar') {
                    const aiSidebarContainer = this.querySelector('#ai-sidebar-container');
                    if (aiSidebarContainer) {
                        containerConfig.element = aiSidebarContainer as HTMLElement;
                        logger.debug('AI sidebar container configured');
                    } else {
                        logger.warn('AI sidebar enabled but container #ai-sidebar-container not found');
                    }
                } else {
                    logger.debug('AI config present but container type is not "sidebar", skipping DOM binding');
                }
            }
        }

        // Add custom upload adapter for server-side file handling
        // This enables the UploadHandler Java API to receive uploaded files
        // Note: The adapter is set up after editor creation in setupCustomUploadAdapter()

        return editorConfig;
    }

    /**
     * 协作模式下检查频道是否已被初始化，逻辑委托给纯函数以便单测覆盖。
     */
    private async stripInitialDataIfChannelExists(
        config: Record<string, unknown>
    ): Promise<Record<string, unknown>> {
        const result = stripInitialDataIfChannelSeeded(config, {
            storage: localStorage,
            now: () => Date.now(),
            onSeeded: (channelId) => logger.info(`频道 "${channelId}" 首次初始化，使用 initialData 种子数据`),
            onAlreadySeeded: (channelId) => logger.info(`频道 "${channelId}" 已初始化，移除 initialData 避免冲突`),
            onStorageUnavailable: () => logger.warn('localStorage 不可用，保留 initialData'),
        });
        return result.config;
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

        // 先占锁，再等待上一轮清理。
        // 顺序很重要：JS 虽是单线程，但 await 会让出执行权——若先 await 再占锁，
        // 两个并发调用者（如 connectedCallback 与 finally 的补偿重建）可能都通过了
        // canCreateEditor() 的检查并停在同一个 await 上，恢复后各自占锁、各建一个编辑器。
        // 把「检查-占锁」放在同一个同步区间内即可消除该窗口。
        this.isCreating = true;

        // 占锁之后的每一条出口都必须释放锁，否则组件会永久卡在「创建中」而无法重试
        // ——尤其 waitForPreviousEditorCleanup() 内部 await 的 destroyPromise 是可能
        // reject 的（destroy 失败），若不兜底，异常会带着锁一起抛出去。
        let handedOffToExecution = false;
        try {
            // Wait for any existing editor cleanup
            await this.waitForPreviousEditorCleanup();

            // Validate editor element exists
            const editorElement = this.querySelector(`[id="${CSS.escape(this.editorId)}"]`) as HTMLElement;
            if (!editorElement) {
                logger.error(`Editor element not found: #${this.editorId}`);
                return;
            }

            // Execute the creation process.
            // executeEditorCreation 自带 finally 负责释放锁，故此处移交所有权，
            // 不再由本函数的 finally 重复释放（否则会提前解锁）。
            handedOffToExecution = true;
            this.createPromise = this.executeEditorCreation(editorElement);
            await this.createPromise;
        } finally {
            if (!handedOffToExecution) {
                this.isCreating = false;
            }
        }
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

        // Prevent concurrent creation - if already creating, wait for the existing creation to finish.
        // 只判断 isCreating，不再要求 createPromise 同时非空：
        // createPromise 直到 await 之后才被赋值，而 isCreating 在进入 await 前就已占上；
        // 若仍要求两者同时成立，第二个并发调用者会在这段窗口内被放行，创建出第二个编辑器。
        if (this.isCreating) {
            logger.debug(' Editor creation already in progress, waiting...');
            return false;
        }

        return true;
    }

    /**
     * Wait for any existing editor to be destroyed before creating a new one.
     */
    private async waitForPreviousEditorCleanup(): Promise<void> {
        // 只要存在未完成的销毁就必须等待，不能再以 (editor || isDestroying) 为前置条件。
        // 孤儿销毁场景下 editor 已为 null、isDestroying 也为 false（那条路径不走
        // destroyEditor），但后台仍有一个 destroy() 在跑，且它结束时会向 source element
        // 写回内容——而重连复用的正是同一个 DOM 节点（editorId 是 @property，不变）。
        // 若不等它就创建新实例，迟到的销毁会把新编辑器的 DOM 清空。
        // 直接以 destroyPromise 是否存在为准，可同时覆盖常规销毁与孤儿销毁两条路径。
        if (!this.destroyPromise) {
            return;
        }
        logger.debug(' Waiting for previous editor cleanup...');

        // 等待必须有上界。
        // detached 状态下 CKEditor 的 destroy() 可能永不 settle（destroyEditor 中已有
        // 该结论，孤儿路径正因此加了超时）。若这里再无界 await 同一个 promise，
        // 挂起只是从「孤儿销毁」搬到了「补偿创建」：isCreating 被永久占用，
        // 重连后依旧空白——等于把第 4 轮修掉的问题换个入口重新引入。
        const timedOut = Symbol('cleanup-timeout');
        const result = await Promise.race([
            this.destroyPromise.then(() => undefined),
            new Promise<typeof timedOut>((resolve) =>
                setTimeout(() => resolve(timedOut), ORPHAN_DESTROY_TIMEOUT_MS)),
        ]);

        if (result === timedOut) {
            // 放弃等待，但必须先切断旧实例对 DOM 的所有权：
            // 迟到的 destroy() 会向 source element 写回，而新实例复用同一节点。
            // 这里把容器整个换成一个全新的空节点，旧销毁即便稍后完成，
            // 触碰到的也只是已被摘下的游离节点，不会影响新编辑器。
            logger.debug(' Previous cleanup timed out - detaching stale container to protect the new editor');
            this.detachStaleEditorContainer();
        }
    }

    /**
     * 用一个同 id 的全新空容器替换当前编辑器容器，切断旧编辑器实例对该 DOM 的所有权。
     *
     * <p>仅在「上一次销毁超时、但仍可能在后台完成」时调用：旧实例持有的是被替换下来的
     * 那个游离节点，其迟到的写回不会再影响页面上的新编辑器。</p>
     */
    private detachStaleEditorContainer(): void {
        const stale = this.querySelector(`[id="${CSS.escape(this.editorId)}"]`) as HTMLElement | null;
        if (!stale || !stale.parentNode) {
            return;
        }

        // 必须替换**容器节点对象本身**，只清空子节点是不够的：
        // CKEditor 的 ElementApiMixin.updateSourceElement() 在销毁末尾执行
        // setDataInElement(this.sourceElement, ...)，而后者是 `el.innerHTML = data`
        // （见 @ckeditor/ckeditor5-utils）。这里的 sourceElement 正是我们传给
        // Editor.create() 的这个容器。若沿用同一个对象，迟到的销毁会直接把
        // 新编辑器的内容整片抹掉。
        //
        // 换成一个同 id、同 class 的新节点后，陈旧实例持有的是被摘下的游离节点，
        // 它的 innerHTML 写回不再影响页面。
        const fresh = document.createElement(stale.tagName.toLowerCase());
        fresh.id = this.editorId;
        // 用模板声明的固定 class，而不是 stale.className——后者可能已被 CKEditor
        // 注入运行时 class（如 ck / ck-editor__editable 等），继承过来会污染新实例。
        fresh.className = EDITOR_CONTENT_CLASS;
        stale.parentNode.replaceChild(fresh, stale);

        // 让 Lit 重新接管。
        //
        // 仅调用 requestUpdate() 是不够的：Lit 3 的模板实例在首次克隆时就绑定了
        // AttributePart，不会因为一次重渲染而重新扫描被外部 replaceChild 掉的节点。
        // 实测（jsdom + 本仓 lit 版本）：此后把 editorId 改成新值，Lit 会把它写到
        // **已游离的旧节点**上，页面上的新节点仍保留旧 id —— 因为 setId() 是公开 API，
        // editorId 确实可能在运行期变化，这条路径是可达的。
        //
        // 先把 renderRoot 渲染成 nothing，丢弃旧模板实例，再触发一次更新重新建立
        // part 绑定；实测这样后续动态更新会正确落到页面上的节点。
        const root = this.renderRoot as HTMLElement | undefined;
        if (root) {
            render(nothing, root);
        }
        this.requestUpdate();
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

            // 若本次创建以「孤儿」收场（创建过程中组件被移出 DOM，刚建好的实例已就地销毁），
            // 在此处补一次重建判定：此时 isCreating 刚刚复位，守卫才可能放行。
            // 覆盖的场景是「创建中断开、且在创建结束前又重新连上」——那时
            // connectedCallback 早已执行过（当时 isCreating 为 true 被守卫拦下），
            // 若不在这里补触发，就再没有人会创建编辑器，组件永久空白。
            if (this.pendingOrphanRecreate) {
                this.pendingOrphanRecreate = false;
                this.recreateEditorOnReconnect();
            }
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

        const createConfig = this.buildEditorCreateConfig(editorElement, config);
        const created = await (EditorConstructor as unknown as {
            create: (cfg: Record<string, unknown>) => Promise<Editor>;
        }).create(createConfig);

        // create() 是重操作（插件多时可达数秒），期间组件可能已被移出 DOM。
        // 若此时仍无条件赋值给 this.editor，就会留下一个「孤儿编辑器」：
        // disconnectedCallback 早已执行过，它调用 destroyEditor 时 this.editor 还是 null
        // 而直接返回，之后再没有任何人来销毁它——DOM 子树、CKEditor 监听器与全局注册表
        // 全部滞留，每次路由往返泄漏一个完整编辑器实例。
        // 这里立即销毁刚建好的实例，并且不赋值给 this.editor。
        if (this.isDisconnected) {
            logger.debug(' Component disconnected during editor creation - destroying orphan instance');
            try {
                // 必须加超时：本分支恰好处于「组件已从 DOM 断开」的状态，而 destroyEditor()
                // 中已有明确结论——detached 时 CKEditor 的 destroy() 可能永不 settle。
                // 若在此无界 await，外层 executeEditorCreation 的 finally 就永远到不了，
                // isCreating 会永久为 true，组件此后再也无法创建编辑器。
                // 超时后放弃等待（实例交给 GC），继续走解锁与补偿重建流程。
                // 给 destroy() 挂上自己的 catch：它可能迟到 reject，
                // 而下方超时胜出后已无人 await 它，会变成 unhandled rejection。
                const destroyed: Promise<void> = created.destroy().then(
                    () => undefined,
                    (e: unknown) => {
                        logger.debug(' Orphan editor destroy rejected (ignored):', e);
                    }
                );

                // 记录这次销毁，**不能只是不等它**。
                // 关键：Promise.race 只停止等待，并不取消底层 destroy()；而 editorId 是
                // @property，重连后复用的是同一个 DOM 节点。CKEditor 的
                // Balloon/Inline/Decoupled 在 destroy() 末尾会向 source element 写回内容，
                // 若放任迟到的销毁与新实例并发，它会把刚建好的编辑器 DOM 清空。
                // 因此这里把销毁 promise 存起来，由后续创建流程的
                // waitForPreviousEditorCleanup() 等待它，实现「同一节点同一时刻只有一个所有者」。
                this.destroyPromise = destroyed;
                void destroyed.finally(() => {
                    if (this.destroyPromise === destroyed) {
                        this.destroyPromise = null;
                    }
                });

                // 超时只用于「不阻塞解锁」，销毁本身仍在后台推进并被上面登记。
                await Promise.race([
                    destroyed,
                    new Promise<void>((resolve) => setTimeout(resolve, ORPHAN_DESTROY_TIMEOUT_MS)),
                ]);
            } catch (e) {
                logger.debug(' Orphan editor destroy failed (ignored):', e);
            }
            // 标记「本次创建以孤儿收场」，真正的补偿重建交由
            // executeEditorCreation 的 finally 在释放 isCreating 之后执行。
            //
            // 不能在这里 queueMicrotask：实测该 microtask 会排在外层 async 函数的
            // finally **之前**（内层 return 时外层还停在 await，finally 尚未执行），
            // 届时守卫看到 isCreating 仍为 true 会直接返回，补偿失效。
            this.pendingOrphanRecreate = true;
            return 0;
        }

        this.editor = created;

        const endTime = performance.now();
        const initTimeMs = endTime - startTime;
        logger.info(`Editor created in ${initTimeMs.toFixed(0)}ms`);

        // 此处**不得**给 editor 实例赋 .id（issue #122）。
        // Editor 在构造期间就已被注册进它自己的私有 Context：
        // Context#_addEditor -> Collection#add -> _getItemIdBeforeAdding()，
        // 后者发现新实例没有 .id，于是自动生成 uid() **写到实例上**，并以该值
        // 为键存入内部 _itemMap。此后再覆盖 .id，实例便与它在 _itemMap 中的键脱钩：
        //   destroy() -> Context#_removeEditor() -> editors.has(editor)
        //   -> _itemMap.has(被覆盖的 id) === false -> remove() 从不执行
        // 编辑器因此永远留在 context.editors 里，而 _removeEditor 仍会走到
        // `this._contextOwner === editor` 分支调用 Context#destroy()，后者遍历
        // 仍含该编辑器的 editors 再次调用 destroy() —— 形成无限递归，占满主线程并卡死标签页。
        // 若将来确实需要在实例侧标识编辑器，请使用 WeakMap<Editor, string>，
        // 不要在 CKEditor 拥有的对象上挂属性（Collection 的 idProperty 可自定义，
        // 换个属性名同样可能撞上这一类问题）。

        return initTimeMs;
    }

    /**
     * 构造 CKEditor 48 单参数 create 配置（委托纯函数实现，便于单测覆盖）。
     */
    private buildEditorCreateConfig(
        editorElement: HTMLElement,
        config: EditorConfig
    ): Record<string, unknown> {
        return buildCreateConfig(
            editorElement,
            config as unknown as Record<string, unknown>,
            this.pendingRootConfig,
            this.editorType
        );
    }

    /**
     * 注销标注侧栏的 scroll 监听（幂等）。
     * 供重复 setup 前的清理与 disconnectedCallback 共用。
     */
    private disposeAnnotationScrollSync(): void {
        if (this.annotationScrollSyncDispose) {
            this.annotationScrollSyncDispose();
            this.annotationScrollSyncDispose = undefined;
        }
    }

    /**
     * 在开发模式下打印 v47 → v48 配置迁移警告。
     * 通过 window.VAADIN_CKEDITOR_DEBUG（与现有 DEBUG 标志一致）控制是否输出，避免生产环境噪音。
     */
    private warnConfigMigration(warnings: string[]): void {
        if (warnings.length === 0 || !DEBUG) {
            return;
        }

        for (const warning of warnings) {
            logger.warn(`[CKEditor 48 migration] ${warning}`);
        }
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

        // Set enabled state (issue #46)
        this.updateEnabled();

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

        // AI 侧栏折叠：监听 .ck-tabs 的 .ck-hidden class 切换，同步 CSS class 用于非 :has() 浏览器
        if (this.aiSidebarEnabled) {
            this.setupAiSidebarCollapseObserver();
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

        // 先移除上一次 setup 注册的 scroll 监听再重新注册。
        // setupAnnotationSidebarSync 会在每次 onEditorReady 时执行，若只加不减：
        // (1) 同一 wrapper 上会叠加 N 个监听器，一次滚动触发 N 次 syncPositions，
        //     长评论列表下明显卡顿；
        // (2) syncPositions 闭包持有 wrapper/sidebar/editable 与组件自身，
        //     组件断开后这些监听器仍绑在游离节点上，造成泄漏。
        // 相邻的 MutationObserver 已用 replaceObserver 处理了同样的问题，此处补齐。
        this.disposeAnnotationScrollSync();
        wrapper.addEventListener('scroll', syncPositions);
        editable.addEventListener('scroll', syncPositions);
        this.annotationScrollSyncDispose = () => {
            wrapper.removeEventListener('scroll', syncPositions);
            editable.removeEventListener('scroll', syncPositions);
        };
        syncPositions();

        // MutationObserver 监听侧栏变化（新增/删除评论），自动重新对齐。
        // 用 replaceObserver 存为字段并在 disconnectedCallback 中断开，避免组件断开后泄漏、
        // 重复 setup 时叠加（review 发现）。
        this.annotationSidebarObserver = replaceObserver(
            this.annotationSidebarObserver,
            () => new MutationObserver(syncPositions),
            (o) => o.observe(sidebar, { childList: true, subtree: true, attributes: true }),
        );
    }

    /**
     * AI 侧栏折叠 :has() 回退。
     * CKEditor AI tabs 组件在折叠时为 .ck-tabs 添加 .ck-hidden class。
     * CSS 使用 :has(.ck-tabs.ck-hidden) 隐藏侧栏，但 :has() 在 Firefox < 121 不支持。
     * 本方法通过 MutationObserver 监听 class 变化，在侧栏容器上切换 data-collapsed 属性
     * 供 CSS 作为回退选择器使用。
     */
    private setupAiSidebarCollapseObserver(): void {
        const aiSidebar = this.querySelector('#ai-sidebar-container') as HTMLElement;
        if (!aiSidebar) return;

        const syncCollapsed = () => {
            const tabs = aiSidebar.querySelector('.ck-tabs');
            if (!tabs) return;
            const isCollapsed = tabs.classList.contains('ck-hidden');
            // AI sidebar container collapse state
            if (isCollapsed) {
                // Move focus out of the collapsing sidebar before hiding it
                if (aiSidebar.contains(document.activeElement)) {
                    const editorEditable = this.querySelector('.ck-editor__editable') as HTMLElement;
                    if (editorEditable) { editorEditable.focus(); }
                }
                aiSidebar.setAttribute('data-collapsed', '');
                aiSidebar.setAttribute('aria-hidden', 'true');
                aiSidebar.inert = true;
                // Fallback for browsers without inert support: prevent tab focus
                if (!('inert' in HTMLElement.prototype)) {
                    aiSidebar.setAttribute('tabindex', '-1');
                    aiSidebar.querySelectorAll<HTMLElement>(
                        'button, input, textarea, select, a, [tabindex], [contenteditable]'
                    ).forEach(el => {
                        el.dataset.prevTabindex = el.getAttribute('tabindex') ?? '';
                        el.setAttribute('tabindex', '-1');
                    });
                }
            } else {
                aiSidebar.removeAttribute('data-collapsed');
                aiSidebar.removeAttribute('aria-hidden');
                aiSidebar.inert = false;
                // Restore tabindex for non-inert browsers
                if (!('inert' in HTMLElement.prototype)) {
                    aiSidebar.removeAttribute('tabindex');
                    aiSidebar.querySelectorAll<HTMLElement>('[data-prev-tabindex]').forEach(el => {
                        const prev = el.dataset.prevTabindex;
                        if (prev) { el.setAttribute('tabindex', prev); } else { el.removeAttribute('tabindex'); }
                        delete el.dataset.prevTabindex;
                    });
                }
            }
            // Update reactive state — Lit re-render will apply/remove editor-container--ai-active
            this.aiSidebarCollapsed = isCollapsed;
        };

        // Initial sync — .ck-tabs may already have .ck-hidden at setup time
        syncCollapsed();

        const observeTabs = (tabs: Element) => {
            // Narrow observer: watch only .ck-tabs class attribute changes
            this.aiSidebarCollapseObserver = new MutationObserver(syncCollapsed);
            this.aiSidebarCollapseObserver.observe(tabs, {
                attributes: true,
                attributeFilter: ['class'],
            });
        };

        const tabs = aiSidebar.querySelector('.ck-tabs');
        if (tabs) {
            observeTabs(tabs);
        } else {
            // .ck-tabs not yet rendered — watch sidebar childList until it appears,
            // then re-scope observer to .ck-tabs attribute changes only
            this.aiSidebarCollapseObserver = new MutationObserver(() => {
                syncCollapsed();
                const newTabs = aiSidebar.querySelector('.ck-tabs');
                if (newTabs) {
                    // .ck-tabs appeared — disconnect broad observer, switch to narrow
                    this.aiSidebarCollapseObserver?.disconnect();
                    observeTabs(newTabs);
                }
            });
            this.aiSidebarCollapseObserver.observe(aiSidebar, {
                childList: true,
                subtree: true,
            });
        }
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
            if (!activeEditor || !this.$server) return;
            const newContent = activeEditor.getData();

            const decision = decideDataChange({
                newContent,
                lastKnownContent: this.lastKnownContent,
                sync: this.sync,
                apiChangeDepth: this.apiChangeDepth,
                changeSource: this.changeSource,
            });

            if (decision.fireContentChange) {
                this.$server.fireContentChange(this.lastKnownContent, newContent, decision.contentChangeSource);
                if (decision.nextLastKnownContent !== null) {
                    this.lastKnownContent = decision.nextLastKnownContent;
                }
                if (decision.resetChangeSource) {
                    this.changeSource = 'USER_INPUT';
                }
            }

            // issue #38: 服务端回填（apiChangeDepth>0）不回写服务端，
            // 否则 Binder.readBean() 会触发 fromClient=true 的 ValueChangeEvent，
            // 使 Binder.hasChanges() 在无用户改动时误为 true。
            if (decision.syncToServer) {
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
            undoCommand.on('execute', undoRedoHandler);
            this.listenerCleanups.push(() => undoCommand.off('execute', undoRedoHandler));
        }
        if (redoCommand) {
            redoCommand.on('execute', undoRedoHandler);
            this.listenerCleanups.push(() => redoCommand.off('execute', undoRedoHandler));
        }

        // Track paste operations for ChangeSource
        const pasteHandler = () => { this.changeSource = 'PASTE'; };
        editor.editing.view.document.on('clipboardInput', pasteHandler);
        this.listenerCleanups.push(() => editor.editing.view.document.off('clipboardInput', pasteHandler));

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
                const applyOpHandler = (_evt: unknown, args: unknown[]) => {
                    const operation = args[0] as { baseVersion?: number; isLocal?: boolean };
                    if (operation && operation.isLocal === false) {
                        this.changeSource = 'COLLABORATION';
                    }
                };
                editor.model.on('applyOperation', applyOpHandler, { priority: 'highest' });
                this.listenerCleanups.push(() => editor.model.off('applyOperation', applyOpHandler));

                // Alternative: Listen to the collaboration channel directly if available
                try {
                    const cloudServices = editor.plugins.get('CloudServices') as unknown as {
                        on?: (event: string, callback: () => void) => void;
                        off?: (event: string, callback: () => void) => void;
                    };
                    if (cloudServices?.on) {
                        const syncHandler = () => { this.changeSource = 'COLLABORATION'; };
                        cloudServices.on('change:syncInProgress', syncHandler);
                        if (cloudServices.off) {
                            this.listenerCleanups.push(() => cloudServices.off!('change:syncInProgress', syncHandler));
                        }
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
                const originalFactory = fileRepository.createUploadAdapter;
                fileRepository.createUploadAdapter = this.getUploadAdapterFactory();

                // 该工厂闭包捕获了 this（Lit 元素），若不还原就会形成
                // FileRepository 插件 -> 闭包 -> 组件 -> 整棵 DOM 子树 的引用链。
                // destroyEditor() 在组件已断开时会跳过 editor.destroy()（交给 GC），
                // 此时 CKEditor 自身不拆卸插件，这条链会把整个编辑器钉住，
                // 每次路由往返泄漏一个实例。登记到 listenerCleanups——
                // 它在 destroyEditor() 的 isDisconnected 提前 return **之前**无条件执行。
                this.listenerCleanups.push(() => {
                    fileRepository.createUploadAdapter = originalFactory;
                });
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
        // 在宿主元素上反映只读状态，便于外部 CSS 通过 vaadin-ckeditor[readonly]
        // 或 .readonly 选择器定制只读外观（issue #44）。即使 editor 尚未就绪也先反映。
        this.toggleAttribute('readonly', this.isReadOnly);
        this.classList.toggle('readonly', this.isReadOnly);

        if (!this.editor) return;

        if (this.isReadOnly) {
            this.editor.enableReadOnlyMode(this.editorId);
        } else {
            this.editor.disableReadOnlyMode(this.editorId);
        }
    }

    /**
     * 同步 enabled 状态（issue #46）。CKEditor 5 无独立的 disabled 概念，
     * 用一个区别于 editorId 的只读锁实现 disable，避免与用户显式的 readOnly 互相覆盖；
     * 同时在宿主元素上反映 disabled 属性/类，便于外部 CSS 定制。
     */
    private updateEnabled(): void {
        this.toggleAttribute('disabled', !this.isEnabled);
        this.classList.toggle('disabled', !this.isEnabled);

        if (!this.editor) return;

        const disabledLockId = `${this.editorId}--disabled`;
        if (this.isEnabled) {
            this.editor.disableReadOnlyMode(disabledLockId);
        } else {
            this.editor.enableReadOnlyMode(disabledLockId);
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

        // 白名单校验：只允许 http/https 绝对地址与相对路径（review: 子串黑名单可被绕过）
        if (!isAllowedCssUrl(this.overrideCssUrl)) {
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
                // issue #57: 源码视图下 setData 只更新 model，<textarea> 仍是旧快照。
                // 退出并重新进入源码视图，强制源码 textarea 从新 model 重新填充。
                this.refreshSourceViewIfActive();
            } finally {
                // Decrement after a microtask to ensure change event fires first
                queueMicrotask(() => {
                    this.apiChangeDepth--;
                });
            }
        }
    }

    /**
     * 若编辑器当前处于 SourceEditing 源码视图，toggle off→on 以刷新源码 textarea（issue #57）。
     */
    private refreshSourceViewIfActive(): void {
        const editor = this.editor;
        if (!editor || !editor.plugins.has('SourceEditing')) {
            return;
        }
        const sourceEditing = editor.plugins.get('SourceEditing') as unknown as {
            isSourceEditingMode: boolean;
        };
        if (!shouldRefreshSourceView({
            hasSourceEditingPlugin: true,
            isSourceEditingMode: sourceEditing.isSourceEditingMode,
        })) {
            return;
        }
        // 退出再进入源码视图，使 textarea 从刚更新的 model 重新填充
        sourceEditing.isSourceEditingMode = false;
        sourceEditing.isSourceEditingMode = true;
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
        if (!this.editor) {
            return;
        }
        // cursorPosition 仅在 change:range 事件触发后才有值；编辑器为空或首次插入
        // （光标在首字符前、尚未发生选区变化）时它仍为 null。回退到当前选区的首位置，
        // 确保 insertText 在这些场景下也能工作（issue #69）。
        const position = this.cursorPosition
            ?? this.editor.model.document.selection.getFirstPosition();
        if (!position) {
            return;
        }
        this.editor.model.change(writer => {
            this.editor!.model.insertContent(writer.createText(text), position as Parameters<typeof this.editor.model.insertContent>[1]);
        });
    }

    /**
     * Public API: 把光标（折叠选区）移到文档起始/末尾并聚焦（issue #52）。
     * @param edge - 'start' 移到文首，'end' 移到文末
     */
    private moveCaretTo(edge: 'start' | 'end'): void {
        if (!this.editor) {
            return;
        }
        const editor = this.editor;
        const root = editor.model.document.getRoot();
        if (!root) {
            return;
        }
        // createPositionAt 的 offset：文首用 0，文末用 'end'
        const offset = edge === 'start' ? 0 : 'end';
        editor.model.change(writer => {
            writer.setSelection(writer.createPositionAt(root, offset));
        });
        editor.editing.view.focus();
    }

    /** Public API: 光标移到文首并聚焦 */
    public setCaretToStart(): void {
        this.moveCaretTo('start');
    }

    /** Public API: 光标移到文末并聚焦 */
    public setCaretToEnd(): void {
        this.moveCaretTo('end');
    }

    /** Public API: 聚焦编辑器可编辑区 */
    public focusEditor(): void {
        this.editor?.editing.view.focus();
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
                // 每个 off() 独立 try，确保任一失败不影响后续清理
                const safeOff = (remove: () => void): void => {
                    try { remove(); } catch { /* ignore */ }
                };

                if (this.selectionChangeListener) {
                    safeOff(() => editor.model.document.selection.off('change:range', this.selectionChangeListener!));
                }
                if (this.dataChangeListener) {
                    safeOff(() => editor.model.document.off('change:data', this.dataChangeListener!));
                }
                if (this.focusChangeListener) {
                    safeOff(() => editor.editing.view.document.off('change:isFocused', this.focusChangeListener!));
                }
                if (this.readOnlyChangeListener) {
                    safeOff(() => editor.off('change:isReadOnly', this.readOnlyChangeListener!));
                }

                // Remove undo/redo/clipboard/collaboration listeners
                for (const cleanup of this.listenerCleanups) {
                    safeOff(cleanup);
                }
                this.listenerCleanups = [];

                // Clear listener references
                this.selectionChangeListener = undefined;
                this.dataChangeListener = undefined;
                this.focusChangeListener = undefined;
                this.readOnlyChangeListener = undefined;

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
        //
        // 必须按身份比对后再清空：destroyPromise 现在也承载「孤儿销毁」的登记
        // （见 createEditorInstance 中的 orphan 分支）。若在此无条件置 null，
        // 当本次销毁 settle 时恰好已有一个更新的孤儿销毁登记在案，就会把它抹掉，
        // 使 waitForPreviousEditorCleanup() 不再等待——重新打开「迟到的销毁
        // 清空新编辑器 DOM」这个所有权漏洞。
        const settled = this.destroyPromise;
        void settled.finally(() => {
            if (this.destroyPromise === settled) {
                this.destroyPromise = null;
            }
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
            // AI active class driven by reactive @state to survive Lit re-renders
            const includeAiClass = (this.aiSidebarEnabled && !this.aiSidebarCollapsed) ? 'editor-container--ai-active' : '';
            // Apply custom editor height if specified (otherwise uses CSS default of 700px)
            const heightStyle = this.editorHeight && this.editorHeight !== 'auto'
                ? `--ck-editor-height: ${this.editorHeight};`
                : '';
            return html`
                <div class="editor-container editor-container_document-editor ${includeOutlineClass} ${includeMinimapClass} ${includeAnnotationClass} ${includeAiClass}"
                     id="editor-container"
                     style="${heightStyle}">
                    <div class="editor-container__menu-bar" id="menu-bar-container"></div>
                    <div class="editor-container__toolbar" id="toolbar-container"></div>
                    <div class="editor-container__editor-wrapper">
                        <div class="editor-container__sidebar" id="editor-outline" role="navigation" aria-label="Document Outline" ?hidden="${!this.documentOutlineEnabled}"></div>
                        <div class="editor-container__editor">
                            <div id="${this.editorId}" class="${EDITOR_CONTENT_CLASS}"></div>
                        </div>
                        <div class="editor-container__sidebar editor-container__sidebar_ckeditor-ai" id="ai-sidebar-container" role="complementary" aria-label="AI Assistant" ?hidden="${!this.aiSidebarEnabled}"></div>
                        <div class="minimap-container" role="region" aria-label="Document Minimap" ?hidden="${!this.minimapEnabled}"></div>
                        <div class="annotation-sidebar-wrapper" role="complementary" aria-label="Comments and Annotations" ?hidden="${!this.annotationSidebarEnabled}">
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

        // Balloon editor with annotation sidebar (for collaboration features)
        if (this.editorType === 'balloon' && this.annotationSidebarEnabled) {
            const includeAnnotationClass = 'editor-container_include-annotations';
            const heightStyle = this.editorHeight && this.editorHeight !== 'auto'
                ? `--ck-editor-height: ${this.editorHeight};`
                : '';
            return html`
                <div class="editor-container editor-container_balloon-editor ${includeAnnotationClass}"
                     id="editor-container"
                     style="${heightStyle}">
                    <div class="editor-container__editor-wrapper">
                        <div class="editor-container__editor">
                            <div id="${this.editorId}" class="${EDITOR_CONTENT_CLASS}"></div>
                        </div>
                        <div class="annotation-sidebar-wrapper" role="complementary" aria-label="Comments and Annotations">
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
                <div id="${this.editorId}" class="${EDITOR_CONTENT_CLASS}"></div>
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

        // Suppress CKEditor Pagination plugin internal errors (non-fatal).
        // review: 单个全局 unhandledrejection listener 跨所有实例共享（带引用计数），
        // 避免每个编辑器各注册一个、同一个 rejection 触发 N 次冗余回调。
        VaadinCKEditor.acquirePaginationErrorHandler();
        this.paginationHandlerAcquired = true;

        // Setup scroll handler for sticky panel (must be called on each connection)
        this.setupStickyPanelObserver();

        // 重挂载时重建编辑器与主题系统。
        //
        // 背景：编辑器原先只在 firstUpdated() 中创建，而 Lit 对同一个元素实例
        // 只会调用一次 firstUpdated。disconnectedCallback 会销毁编辑器并把
        // this.editor 置空，于是「移出 DOM 再放回」之后就永远不会再创建——
        // 用户看到一个空白容器。Vaadin 中这类场景很常见：Div.remove() 后再 add()、
        // 在布局间移动组件、@PreserveOnRefresh 视图、Tab/Accordion 切换等。
        //
        // 这里只处理「重挂载」：首次连接时 hasUpdated 为 false，创建仍交给
        // firstUpdated（此时 shadow DOM 尚未渲染，容器元素还不存在）。
        // 条件同时排除正在创建/销毁的中间态，避免与 firstUpdated 或未完成的
        // 销毁流程重复触发。
        this.recreateEditorOnReconnect();
    }

    /**
     * 重挂载后按需重建编辑器与主题系统（幂等）。
     *
     * <p>两个调用点共用同一套守卫：
     * <ul>
     *   <li>{@code connectedCallback} —— 处理「销毁已完成后再重新连上」的常规情况；</li>
     *   <li>{@code disconnectedCallback} 的销毁 microtask 结束时 —— 处理「同一 tick 内
     *       remove 再 add」：那时 connectedCallback 早于销毁执行，看到的 editor 还没被清空，
     *       会跳过重建，必须在销毁真正完成后补一次。</li>
     * </ul>
     *
     * <p>首次连接不在此处理：那时 {@code hasUpdated} 为 false，shadow DOM 尚未渲染、
     * 容器元素还不存在，创建仍由 {@code firstUpdated} 负责。
     */
    private recreateEditorOnReconnect(): void {
        // 判定逻辑抽成纯函数便于单测（见 reconnect-decision.ts）
        if (!shouldRecreateEditor({
            hasUpdated: this.hasUpdated,
            hasEditor: !!this.editor,
            isCreating: this.isCreating,
            isDestroying: this.isDestroying,
            isDisconnected: this.isDisconnected,
            isConnected: this.isConnected,
        })) {
            return;
        }
        logger.debug(' recreating editor and theme system after reconnect');
        this.initializeThemeSystem();
        void this.createEditor().catch((e) => {
            logger.error(' createEditor() failed during reconnect:', e);
        });
    }

    /**
     * Validate a CSS property value to prevent CSS injection.
     * Rejects values containing characters that could break out of a CSS rule.
     */
    private static isSafeCssValue(value: string): boolean {
        return !/[{};]/.test(value);
    }

    /**
     * 转义值以安全嵌入双引号 CSS 属性选择器 [attr="..."] 中。
     * 去除可越出引号上下文的字符：反斜杠、双引号、右方括号（review: editorId 与 buttonName 统一处理）。
     */
    private static cssAttrValueSafe(value: string): string {
        return value.replace(/[\\"\]]/g, '');
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
        // 统一用 cssAttrValueSafe 转义，防止值越出双引号属性选择器 [attr="..."]（review: 与 buttonName 保持一致）
        const scope = `vaadin-ckeditor[editor-id="${VaadinCKEditor.cssAttrValueSafe(this.editorId)}"]`;
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
                const safeName = VaadinCKEditor.cssAttrValueSafe(buttonName);
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
    //
    // review: 改为单个进程级共享 listener + 引用计数，避免每实例各注册一个导致冗余触发。
    private paginationHandlerAcquired = false;
    private static paginationRefcount = createRefcount();
    private static paginationListener: ((event: PromiseRejectionEvent) => void) | null = null;

    private static acquirePaginationErrorHandler(): void {
        VaadinCKEditor.paginationRefcount = VaadinCKEditor.paginationRefcount.acquire();
        if (VaadinCKEditor.paginationRefcount.justApplied) {
            VaadinCKEditor.paginationListener = (event: PromiseRejectionEvent) => {
                const err = event.reason;
                if (err instanceof TypeError &&
                    err.stack?.includes('PageStarterInfoToPageBreakInfo')) {
                    event.preventDefault();
                    logger.debug('Suppressed Pagination plugin internal error:', err.message);
                }
            };
            window.addEventListener('unhandledrejection', VaadinCKEditor.paginationListener);
        }
    }

    private static releasePaginationErrorHandler(): void {
        VaadinCKEditor.paginationRefcount = VaadinCKEditor.paginationRefcount.release();
        if (VaadinCKEditor.paginationRefcount.justRemoved && VaadinCKEditor.paginationListener) {
            window.removeEventListener('unhandledrejection', VaadinCKEditor.paginationListener);
            VaadinCKEditor.paginationListener = null;
        }
    }

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

        // Clean up pagination error handler (release shared global listener)
        if (this.paginationHandlerAcquired) {
            VaadinCKEditor.releasePaginationErrorHandler();
            this.paginationHandlerAcquired = false;
        }

        // Clean up sticky panel observer
        this.cleanupStickyPanelObserver();

        // Clean up AI sidebar collapse observer
        if (this.aiSidebarCollapseObserver) {
            this.aiSidebarCollapseObserver.disconnect();
            this.aiSidebarCollapseObserver = undefined;
        }

        // Clean up annotation sidebar observer (review: was never disconnected → leak)
        this.annotationSidebarObserver = disposeObserver(this.annotationSidebarObserver);
        this.disposeAnnotationScrollSync();

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
            void this.destroyEditor().then(() => {
                // 销毁是延迟到 microtask 的，而「同一 tick 内 remove() 再 add()」
                // （Vaadin 在布局间移动组件的常见模式）的实际回调顺序是：
                //   disconnectedCallback → connectedCallback → 本 microtask
                // 因此 connectedCallback 执行时 this.editor 尚未被清空，其重建守卫
                // 会跳过创建；等本 microtask 跑完，编辑器已被销毁却无人重建，
                // 组件就永久停在空白状态。
                // 这里在销毁完成后补一次判断：若此刻组件其实已经重新连上，则重建。
                if (!this.isDisconnected && this.isConnected) {
                    logger.debug(' reconnected during destroy - recreating editor');
                    this.recreateEditorOnReconnect();
                }
            });
        });
        logger.debug('disconnectedCallback END (microtask scheduled)');
    }
}

