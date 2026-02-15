package com.wontlost.ckeditor;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;
import com.wontlost.ckeditor.event.*;
import com.wontlost.ckeditor.event.EditorErrorEvent.EditorError;
import com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity;
import com.wontlost.ckeditor.event.FallbackEvent.FallbackMode;
import com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource;
import com.wontlost.ckeditor.handler.ErrorHandler;
import com.wontlost.ckeditor.handler.HtmlSanitizer;
import com.wontlost.ckeditor.handler.UploadHandler;
import org.jsoup.safety.Safelist;

import com.wontlost.ckeditor.internal.ContentManager;
import com.wontlost.ckeditor.internal.EnumParser;
import com.wontlost.ckeditor.internal.EventDispatcher;
import com.wontlost.ckeditor.internal.UploadManager;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.wontlost.ckeditor.JsonUtil.*;

/**
 * Vaadin CKEditor 5 component.
 *
 * <p>Modular CKEditor 5 integration with plugin-based customization.</p>
 *
 * <h2>Usage examples:</h2>
 * <pre>
 * // Use preset
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPreset(CKEditorPreset.STANDARD)
 *     .build();
 *
 * // Custom plugins (dependencies auto-resolved)
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPlugins(CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC, CKEditorPlugin.IMAGE_CAPTION)
 *     .withToolbar("bold", "italic", "|", "insertImage")
 *     .build();
 * // IMAGE_CAPTION automatically includes IMAGE plugin as dependency
 *
 * // Customize preset
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPreset(CKEditorPreset.BASIC)
 *     .addPlugin(CKEditorPlugin.TABLE)
 *     .withLanguage("zh-cn")
 *     .build();
 * </pre>
 *
 * <h2>Dependency Resolution:</h2>
 * <p>The builder automatically resolves plugin dependencies by default.
 * For example, adding IMAGE_CAPTION will automatically include the IMAGE plugin.</p>
 *
 * <pre>
 * // Auto-resolve with recommended plugins for full feature set
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPlugins(CKEditorPlugin.IMAGE)
 *     .withDependencyMode(DependencyMode.AUTO_RESOLVE_WITH_RECOMMENDED)
 *     .build();
 * // Includes IMAGE plus recommended: IMAGE_TOOLBAR, IMAGE_CAPTION, IMAGE_STYLE, IMAGE_RESIZE
 *
 * // Strict mode - fail if dependencies missing
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPlugins(CKEditorPlugin.IMAGE_CAPTION) // Missing IMAGE dependency
 *     .withDependencyMode(DependencyMode.STRICT)
 *     .build(); // Throws IllegalStateException
 *
 * // Manual mode - no dependency checking
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPlugins(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH, CKEditorPlugin.BOLD)
 *     .withDependencyMode(DependencyMode.MANUAL)
 *     .build();
 * </pre>
 *
 * @see CKEditorPluginDependencies
 * @see CKEditorPreset
 */
@Tag("vaadin-ckeditor")
@JsModule("./vaadin-ckeditor/vaadin-ckeditor.ts")
@NpmPackage(value = "ckeditor5", version = "47.5.0")
@NpmPackage(value = "lit", version = "^3.3.2")
public class VaadinCKEditor extends CustomField<String> implements HasAriaLabel {

    private static final Logger logger = Logger.getLogger(VaadinCKEditor.class.getName());
    /** Keep in sync with version field in vaadin-ckeditor.ts */
    private static final String VERSION = "5.1.0";

    /**
     * Default autosave waiting time in milliseconds.
     */
    private static final int DEFAULT_AUTOSAVE_WAITING_TIME = 2000;

    /**
     * Default language.
     */
    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * Default license key (GPL open source license).
     */
    private static final String DEFAULT_LICENSE_KEY = "GPL";

    private String editorData;
    private final Set<CKEditorPlugin> plugins = new LinkedHashSet<>();
    private final Set<CustomPlugin> customPlugins = new LinkedHashSet<>();
    private CKEditorConfig config;
    private CKEditorType editorType = CKEditorType.CLASSIC;
    private CKEditorTheme theme = CKEditorTheme.AUTO;
    private String language = DEFAULT_LANGUAGE;
    private String[] toolbar;
    private boolean readOnly = false;
    private boolean autosave = false;
    private int autosaveWaitingTime = DEFAULT_AUTOSAVE_WAITING_TIME;
    private Consumer<String> autosaveCallback;
    private String licenseKey = DEFAULT_LICENSE_KEY;
    private ErrorHandler errorHandler;
    private HtmlSanitizer htmlSanitizer;
    private UploadHandler uploadHandler;
    private UploadHandler.UploadConfig uploadConfig;
    private FallbackMode fallbackMode = FallbackMode.TEXTAREA;

    // Internal managers
    private UploadManager uploadManager;
    private ContentManager contentManager;
    private EventDispatcher eventDispatcher;

    /**
     * Private constructor, use Builder to create instances
     */
    VaadinCKEditor() {
        this.editorData = "";
        this.config = new CKEditorConfig();
        this.eventDispatcher = new EventDispatcher(this);
        // Initialize contentManager to ensure it's never null (null object pattern)
        this.contentManager = new ContentManager(null);
    }

    /**
     * Create editor builder.
     *
     * @return a new builder instance
     */
    public static VaadinCKEditorBuilder create() {
        return VaadinCKEditorBuilder.create();
    }

    /**
     * Quick create editor with preset
     */
    public static VaadinCKEditor withPreset(CKEditorPreset preset) {
        return create().withPreset(preset).build();
    }

    /**
     * Initialize editor (called by builder)
     */
    void initialize() {
        // Initialize internal managers
        initializeManagers();

        String editorId = "editor_" + UUID.randomUUID().toString().substring(0, 8);
        getElement().setProperty("editorId", editorId);
        getElement().setProperty("editorType", editorType.getJsName());
        getElement().setProperty("themeType", theme.getJsName());
        getElement().setProperty("editorData", editorData);
        getElement().setProperty("isReadOnly", readOnly);
        getElement().setProperty("language", language);
        getElement().setProperty("autosave", autosave);
        getElement().setProperty("autosaveWaitingTime", autosaveWaitingTime);
        getElement().setProperty("licenseKey", licenseKey);
        getElement().setProperty("fallbackMode", fallbackMode.getJsName());

        // Set plugin list
        getElement().setPropertyJson("plugins", buildPluginsJson());

        // Set toolbar
        if (toolbar != null && toolbar.length > 0) {
            getElement().setPropertyJson("toolbar", buildToolbarJson());
        }

        // Set configuration
        if (config != null) {
            getElement().setPropertyJson("config", config.toJson());
        }
    }

    /**
     * Initialize internal managers.
     */
    private void initializeManagers() {
        // If HtmlSanitizer is set, recreate contentManager
        // (constructor already created a default instance without sanitizer)
        if (htmlSanitizer != null) {
            this.contentManager = new ContentManager(htmlSanitizer);
        }

        // Initialize upload manager if upload handler is configured
        if (uploadHandler != null) {
            // Use WeakReference to avoid memory leaks
            // Lambda implicitly captures 'this', if UploadManager outlives VaadinCKEditor,
            // it would prevent VaadinCKEditor from being garbage collected
            final WeakReference<VaadinCKEditor> editorRef = new WeakReference<>(this);

            this.uploadManager = new UploadManager(
                uploadHandler,
                uploadConfig, // Use configured upload params (defaults if null)
                (uploadId, url, error) -> {
                    // Get editor instance through WeakReference
                    VaadinCKEditor editor = editorRef.get();
                    if (editor == null) {
                        // Editor has been garbage collected, ignore callback
                        logger.fine("Upload callback ignored: editor has been garbage collected");
                        return;
                    }

                    // Execute callback in UI thread
                    editor.getUI().ifPresent(ui -> ui.access(() -> {
                        if (url != null) {
                            editor.getElement().executeJs("this._resolveUpload($0, $1, null)", uploadId, url);
                        } else {
                            editor.getElement().executeJs("this._resolveUpload($0, null, $1)", uploadId, error);
                        }
                    }));
                }
            );
        }
    }

    private ArrayNode buildPluginsJson() {
        ArrayNode arr = createArrayNode();

        // Add official plugins
        for (CKEditorPlugin plugin : plugins) {
            ObjectNode pluginObj = createObjectNode();
            pluginObj.put("name", plugin.getJsName());
            pluginObj.put("premium", plugin.isPremium());
            arr.add(pluginObj);
        }

        // Add custom plugins
        for (CustomPlugin plugin : customPlugins) {
            ObjectNode pluginObj = createObjectNode();
            pluginObj.put("name", plugin.getJsName());
            pluginObj.put("premium", plugin.isPremium());
            if (plugin.getImportPath() != null) {
                pluginObj.put("importPath", plugin.getImportPath());
            }
            arr.add(pluginObj);
        }

        return arr;
    }

    private ArrayNode buildToolbarJson() {
        return toArrayNode(toolbar);
    }

    // ==================== Value Operations ====================

    @Override
    protected String generateModelValue() {
        return editorData;
    }

    @Override
    protected void setPresentationValue(String value) {
        this.editorData = value;
    }

    @Override
    public String getValue() {
        return editorData;
    }

    /**
     * Get sanitized content.
     * If HtmlSanitizer is set, applies sanitization; otherwise returns original content.
     *
     * @return sanitized HTML content
     */
    public String getSanitizedValue() {
        return contentManager.getSanitizedValue(editorData);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        this.editorData = value != null ? value : "";
        getElement().setProperty("editorData", this.editorData);
        updateEditorData(this.editorData);
    }

    @Override
    protected void setModelValue(String value, boolean fromClient) {
        String oldValue = this.editorData;
        String newValue = value != null ? value : "";
        // Only update when value actually changes
        if (java.util.Objects.equals(oldValue, newValue)) {
            return;
        }
        // super.setModelValue already fires ValueChangeEvent via Vaadin's AbstractField,
        // so we must not call fireEvent again to avoid duplicate events
        super.setModelValue(newValue, fromClient);
        this.editorData = newValue;
    }

    @ClientCallable
    private void setEditorData(String data) {
        setModelValue(data, true);
    }

    @ClientCallable
    private void saveEditorData(String data) {
        boolean success = true;
        String errorMessage = null;

        if (autosaveCallback != null) {
            try {
                autosaveCallback.accept(data);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in autosave callback", e);
                success = false;
                // Ensure error message is not null
                errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = e.getClass().getSimpleName() + " occurred during autosave";
                }
            }
        } else {
            logger.log(Level.WARNING, "Autosave triggered but no callback registered");
        }

        // Delegate to EventDispatcher to fire AutosaveEvent
        eventDispatcher.fireAutosave(data, success, errorMessage);
    }

    private void updateEditorData(String content) {
        getElement().executeJs("this.updateData($0)", content);
    }

    // ==================== Property Setters ====================

    /**
     * Set editor ID.
     * Note: This sets both the Vaadin component ID and the internal editorId property.
     * The editorId is used for frontend component identification.
     *
     * @param id the ID to set, or null to generate a random ID
     */
    @Override
    public void setId(String id) {
        String editorId = id != null ? id : "editor_" + UUID.randomUUID().toString().substring(0, 8);
        super.setId(editorId);
        getElement().setProperty("editorId", editorId);
    }

    @Override
    public Optional<String> getId() {
        return Optional.ofNullable(getElement().getProperty("editorId"));
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        getElement().setProperty("isReadOnly", readOnly);
        getId().ifPresent(id ->
            getElement().executeJs("this.setReadOnly($0)", readOnly));
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        getElement().setProperty("editorWidth", width != null ? width : "auto");
    }

    @Override
    public void setHeight(String height) {
        super.setHeight(height);
        getElement().setProperty("editorHeight", height != null ? height : "auto");
    }

    /**
     * Set toolbar visibility
     */
    public void setHideToolbar(boolean hide) {
        getElement().setProperty("hideToolbar", hide);
    }

    /**
     * Enable read-only mode and hide toolbar
     */
    public void setReadOnlyWithToolbarAction(boolean readOnly) {
        setReadOnly(readOnly);
        setHideToolbar(readOnly);
    }

    /**
     * Enable minimap (DECOUPLED type only)
     */
    public void setMinimapEnabled(boolean enabled) {
        getElement().setProperty("minimapEnabled", enabled);
    }

    /**
     * Enable simple preview mode for minimap (DECOUPLED type only).
     * When true, minimap renders content as simple boxes for better performance.
     * Use this option if the minimap updates too slowly with large documents.
     *
     * @param enabled true to enable simple preview mode
     */
    public void setMinimapSimplePreview(boolean enabled) {
        getElement().setProperty("minimapSimplePreview", enabled);
    }

    /**
     * Enable Document Outline sidebar (DECOUPLED type only).
     * Requires DocumentOutline plugin to be loaded (premium feature).
     *
     * @param enabled true to enable document outline
     */
    public void setDocumentOutlineEnabled(boolean enabled) {
        getElement().setProperty("documentOutlineEnabled", enabled);
    }

    /**
     * Enable annotation sidebar for collaboration features (DECOUPLED type only).
     * Provides container for Comments, TrackChanges and PresenceList UI.
     *
     * @param enabled true to enable annotation sidebar and presence list
     */
    public void setAnnotationSidebarEnabled(boolean enabled) {
        getElement().setProperty("annotationSidebarEnabled", enabled);
    }

    /**
     * 启用 AI 侧栏容器（仅 DECOUPLED 类型）。
     * AI Chat/Assistant 插件在 sidebar 模式下需要 DOM 容器。
     * 启用后会在编辑器右侧创建 AI 侧栏容器，并自动将 config.ai.container.element 绑定到该容器。
     *
     * @param enabled true 启用 AI 侧栏
     */
    public void setAiSidebarEnabled(boolean enabled) {
        getElement().setProperty("aiSidebarEnabled", enabled);
    }

    /**
     * 启用评论权限强制插件，隐藏非当前用户评论的 Edit/Remove 下拉菜单。
     * 需要同时启用 annotationSidebarEnabled 和 CommentsRepository 插件。
     *
     * @param enabled true 启用评论权限 UI 强制
     */
    public void setCommentPermissionEnforcerEnabled(boolean enabled) {
        getElement().setProperty("commentPermissionEnforcerEnabled", enabled);
    }

    /**
     * Enable general HTML support
     */
    public void setGeneralHtmlSupportEnabled(boolean enabled) {
        getElement().setProperty("ghsEnabled", enabled);
    }

    /**
     * Allow plugins that require special configuration (CloudServices, Minimap, etc.).
     * When enabled, these plugins won't be automatically filtered out by the plugin resolver.
     * This is automatically set to true when premium plugins requiring CloudServices are used.
     */
    public void setAllowConfigRequiredPlugins(boolean allow) {
        getElement().setProperty("allowConfigRequiredPlugins", allow);
    }

    /**
     * Set synchronous update mode
     */
    public void setSynchronized(boolean sync) {
        getElement().setProperty("sync", sync);
    }

    /**
     * Set custom CSS URL
     */
    public void setOverrideCssUrl(String url) {
        if (url != null) {
            getElement().setProperty("overrideCssUrl", url);
        }
    }

    /**
     * Set autosave callback
     */
    public void setAutosaveCallback(Consumer<String> callback) {
        this.autosaveCallback = callback;
    }

    // ==================== Content Operations ====================

    /**
     * Clear editor content
     */
    @Override
    public void clear() {
        setValue("");
    }

    /**
     * Insert text at cursor position
     */
    public void insertText(String text) {
        getId().ifPresent(id ->
            getElement().executeJs("this.insertText($0)", text));
    }

    /**
     * Get plain text content (strip HTML tags)
     */
    public String getPlainText() {
        return contentManager.getPlainText(editorData);
    }

    /**
     * Get sanitized HTML (remove dangerous tags)
     */
    public String getSanitizedHtml() {
        return contentManager.getSanitizedHtml(editorData);
    }

    /**
     * Sanitize HTML with specified rules
     */
    public String sanitizeHtml(String html, Safelist safelist) {
        return contentManager.sanitizeHtml(html, safelist);
    }

    /**
     * Get character count of content (excluding HTML tags).
     *
     * @return character count
     */
    public int getCharacterCount() {
        return contentManager.getCharacterCount(editorData);
    }

    /**
     * Get word count of content.
     *
     * @return word count
     */
    public int getWordCount() {
        return contentManager.getWordCount(editorData);
    }

    /**
     * Check if content is empty.
     *
     * @return true if content is empty or contains only whitespace
     */
    public boolean isContentEmpty() {
        return contentManager.isContentEmpty(editorData);
    }

    // ==================== Static Info ====================

    /**
     * Get version
     */
    public static String getVersion() {
        return VERSION;
    }

    // ==================== Enterprise Event Listeners ====================

    /**
     * Add editor ready event listener.
     * Fired when the editor is fully initialized and ready to accept user input.
     *
     * <p>Usage example:</p>
     * <pre>
     * editor.addEditorReadyListener(event -&gt; {
     *     logger.info("Editor ready in {} ms", event.getInitializationTimeMs());
     *     event.getSource().focus();
     * });
     * </pre>
     *
     * @param listener the event listener
     * @return registration object for removing the listener
     */
    public Registration addEditorReadyListener(ComponentEventListener<EditorReadyEvent> listener) {
        return eventDispatcher.addEditorReadyListener(listener);
    }

    /**
     * Add editor error event listener.
     * Fired when the editor encounters an error.
     *
     * <p>Usage example:</p>
     * <pre>
     * editor.addEditorErrorListener(event -&gt; {
     *     EditorError error = event.getError();
     *     if (error.getSeverity() == ErrorSeverity.FATAL) {
     *         Notification.show("Editor error: " + error.getMessage(),
     *             Notification.Type.ERROR_MESSAGE);
     *     }
     * });
     * </pre>
     *
     * @param listener the event listener
     * @return registration object for removing the listener
     */
    public Registration addEditorErrorListener(ComponentEventListener<EditorErrorEvent> listener) {
        return eventDispatcher.addEditorErrorListener(listener);
    }

    /**
     * Add autosave event listener.
     * Fired when editor content is auto-saved.
     *
     * @param listener the event listener
     * @return registration object for removing the listener
     */
    public Registration addAutosaveListener(ComponentEventListener<AutosaveEvent> listener) {
        return eventDispatcher.addAutosaveListener(listener);
    }

    /**
     * Add content change event listener.
     * Fired when editor content changes.
     *
     * @param listener the event listener
     * @return registration object for removing the listener
     */
    public Registration addContentChangeListener(ComponentEventListener<ContentChangeEvent> listener) {
        return eventDispatcher.addContentChangeListener(listener);
    }

    /**
     * Add fallback event listener.
     * Fired when the editor triggers fallback mode due to an error.
     *
     * @param listener the event listener
     * @return registration object for removing the listener
     */
    public Registration addFallbackListener(ComponentEventListener<FallbackEvent> listener) {
        return eventDispatcher.addFallbackListener(listener);
    }

    // ==================== Enterprise Handlers ====================

    /**
     * Set error handler.
     *
     * @param handler the error handler
     */
    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
        eventDispatcher.setErrorHandler(handler);
    }

    /**
     * Get error handler.
     *
     * @return the error handler, may be null
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Set HTML sanitizer.
     *
     * @param sanitizer the HTML sanitizer
     */
    public void setHtmlSanitizer(HtmlSanitizer sanitizer) {
        this.htmlSanitizer = sanitizer;
        // Update ContentManager so getSanitizedValue() uses the new sanitizer
        this.contentManager = new com.wontlost.ckeditor.internal.ContentManager(sanitizer);
    }

    /**
     * Get HTML sanitizer.
     *
     * @return the HTML sanitizer, may be null
     */
    public HtmlSanitizer getHtmlSanitizer() {
        return htmlSanitizer;
    }

    /**
     * Set upload handler.
     *
     * @param handler the upload handler
     */
    public void setUploadHandler(UploadHandler handler) {
        this.uploadHandler = handler;
    }

    /**
     * Get upload handler.
     *
     * @return the upload handler, may be null
     */
    public UploadHandler getUploadHandler() {
        return uploadHandler;
    }

    /**
     * Set fallback mode.
     *
     * @param mode the fallback mode
     */
    public void setFallbackMode(FallbackMode mode) {
        this.fallbackMode = mode;
        getElement().setProperty("fallbackMode", mode.getJsName());
    }

    /**
     * Get fallback mode.
     *
     * @return the current fallback mode
     */
    public FallbackMode getFallbackMode() {
        return fallbackMode;
    }

    /**
     * Get statistics of registered listeners.
     * Used for debugging and monitoring.
     *
     * @return listener statistics
     */
    public EventDispatcher.ListenerStats getListenerStats() {
        return eventDispatcher.getListenerStats();
    }

    /**
     * Clean up all event listeners.
     * Usually called before component destruction.
     */
    public void cleanupListeners() {
        eventDispatcher.cleanup();
        if (uploadManager != null) {
            uploadManager.cleanup();
        }
    }

    // ==================== Client Callable for Events ====================

    @ClientCallable
    private void fireEditorReady(double initTimeMs) {
        eventDispatcher.fireEditorReady((long) initTimeMs);
    }

    @ClientCallable
    private void fireEditorError(String code, String message, String severity,
                                  boolean recoverable, String stackTrace) {
        // Use EnumParser to safely parse severity
        ErrorSeverity errorSeverity = EnumParser.parse(
            severity, ErrorSeverity.class, ErrorSeverity.ERROR, "fireEditorError");

        EditorError error = new EditorError(
            code, message,
            errorSeverity,
            recoverable, stackTrace
        );

        // Delegate to EventDispatcher for handling (including error handler and event dispatch)
        eventDispatcher.fireEditorError(error);
    }

    @ClientCallable
    private void fireContentChange(String oldContent, String newContent, String source) {
        // Use EnumParser to safely parse source
        ChangeSource changeSource = EnumParser.parse(
            source, ChangeSource.class, ChangeSource.UNKNOWN, "fireContentChange");

        eventDispatcher.fireContentChange(oldContent, newContent, changeSource);
    }

    @ClientCallable
    private void fireFallback(String mode, String reason, String originalError) {
        FallbackMode fallbackModeValue = FallbackMode.fromJsName(mode);
        eventDispatcher.fireFallback(fallbackModeValue, reason, originalError);
    }

    /**
     * Handle file upload request from client.
     * File content is transferred as Base64 encoded string.
     * Uses UploadManager to ensure thread safety and correct callback order.
     *
     * @param uploadId upload identifier for callback
     * @param fileName file name
     * @param mimeType MIME type
     * @param base64Data Base64 encoded file content
     */
    @ClientCallable
    private void handleFileUpload(String uploadId, String fileName, String mimeType, String base64Data) {
        if (uploadManager == null) {
            // No upload handler configured, return error
            getElement().executeJs("this._resolveUpload($0, null, $1)",
                uploadId, "No upload handler configured");
            return;
        }

        // Use UploadManager to handle upload (thread-safe)
        uploadManager.handleUpload(uploadId, fileName, mimeType, base64Data);
    }

    /**
     * Check if there are uploads in progress.
     *
     * @return true if there are active uploads
     */
    public boolean hasActiveUploads() {
        return uploadManager != null && uploadManager.hasActiveUploads();
    }

    /**
     * Get the number of active uploads.
     *
     * @return active upload count
     */
    public int getActiveUploadCount() {
        return uploadManager != null ? uploadManager.getActiveUploadCount() : 0;
    }

    /**
     * Cancel the specified upload task.
     *
     * @param uploadId the upload ID
     * @return true if successfully cancelled
     */
    public boolean cancelUpload(String uploadId) {
        return uploadManager != null && uploadManager.cancelUpload(uploadId);
    }

    /**
     * Cancel upload task from client.
     * Called by frontend upload adapter's abort() method.
     *
     * @param uploadId the upload ID
     */
    @ClientCallable
    private void cancelUploadFromClient(String uploadId) {
        cancelUpload(uploadId);
    }

    // ==================== Package-Private Methods for Builder ====================
    // These methods are used by VaadinCKEditorBuilder to configure the editor

    void clearPlugins() { plugins.clear(); }
    void addPluginsInternal(Collection<CKEditorPlugin> p) { plugins.addAll(p); }
    void addCustomPluginsInternal(Collection<CustomPlugin> p) { customPlugins.addAll(p); }
    boolean hasPlugins() { return !plugins.isEmpty(); }
    Set<CKEditorPlugin> getPluginsInternal() { return new LinkedHashSet<>(plugins); }

    void setEditorTypeInternal(CKEditorType type) { this.editorType = type; }
    void setThemeInternal(CKEditorTheme theme) { this.theme = theme; }
    void setLanguageInternal(String language) { this.language = language; }
    String getLanguageInternal() { return this.language; }
    void setFallbackModeInternal(FallbackMode mode) { this.fallbackMode = mode; }

    void setErrorHandlerInternal(ErrorHandler handler) { this.errorHandler = handler; }
    void setHtmlSanitizerInternal(HtmlSanitizer sanitizer) { this.htmlSanitizer = sanitizer; }
    void setUploadHandlerInternal(UploadHandler handler) { this.uploadHandler = handler; }
    void setUploadConfigInternal(UploadHandler.UploadConfig config) { this.uploadConfig = config; }

    void setEditorDataInternal(String data) { this.editorData = data != null ? data : ""; }
    void setReadOnlyInternal(boolean readOnly) { this.readOnly = readOnly; }
    void setLicenseKeyInternal(String licenseKey) { this.licenseKey = licenseKey; }
    void setToolbarInternal(String[] toolbar) { this.toolbar = toolbar != null ? toolbar.clone() : null; }
    void setConfigInternal(CKEditorConfig config) { this.config = config; }
    CKEditorConfig getConfigInternal() { return this.config; }

    void setAutosaveInternal(boolean enabled, int waitingTime, Consumer<String> callback) {
        this.autosave = enabled;
        this.autosaveWaitingTime = waitingTime;
        this.autosaveCallback = callback;
    }
}
