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
import org.jsoup.Jsoup;
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
@NpmPackage(value = "ckeditor5", version = "47.4.0")
@NpmPackage(value = "lit", version = "^3.3.2")
public class VaadinCKEditor extends CustomField<String> implements HasAriaLabel {

    private static final Logger logger = Logger.getLogger(VaadinCKEditor.class.getName());
    private static final String VERSION = "5.0.2";

    /**
     * 默认 autosave 等待时间（毫秒）
     */
    private static final int DEFAULT_AUTOSAVE_WAITING_TIME = 2000;

    /**
     * 默认语言
     */
    private static final String DEFAULT_LANGUAGE = "en";

    /**
     * 默认 license key (GPL 开源协议)
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
    private FallbackMode fallbackMode = FallbackMode.TEXTAREA;

    // 内部管理器
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
        // 初始化内部管理器
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
     * 初始化内部管理器
     */
    private void initializeManagers() {
        // 初始化内容管理器
        this.contentManager = new ContentManager(htmlSanitizer);

        // 初始化上传管理器（如果有上传处理器）
        if (uploadHandler != null) {
            // 使用 WeakReference 避免内存泄漏
            // Lambda 表达式隐式捕获 this，如果 UploadManager 的生命周期长于 VaadinCKEditor，
            // 会导致 VaadinCKEditor 无法被垃圾回收
            final WeakReference<VaadinCKEditor> editorRef = new WeakReference<>(this);

            this.uploadManager = new UploadManager(
                uploadHandler,
                null, // 使用默认配置
                (uploadId, url, error) -> {
                    // 通过 WeakReference 获取编辑器实例
                    VaadinCKEditor editor = editorRef.get();
                    if (editor == null) {
                        // 编辑器已被垃圾回收，忽略回调
                        logger.fine("Upload callback ignored: editor has been garbage collected");
                        return;
                    }

                    // 在 UI 线程中执行回调
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
     * 获取经过清理的内容。
     * 如果设置了 HtmlSanitizer，则应用清理；否则返回原始内容。
     *
     * @return 清理后的 HTML 内容
     */
    public String getSanitizedValue() {
        if (contentManager != null) {
            return contentManager.getSanitizedValue(editorData);
        }
        return editorData;
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
        // 只有值真正发生变化时才触发事件
        if (java.util.Objects.equals(oldValue, newValue)) {
            return;
        }
        super.setModelValue(newValue, fromClient);
        this.editorData = newValue;
        fireEvent(new ComponentValueChangeEvent<>(this, this, oldValue, fromClient));
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
                // 确保错误消息不为 null
                errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = e.getClass().getSimpleName() + " occurred during autosave";
                }
            }
        } else {
            logger.log(Level.WARNING, "Autosave triggered but no callback registered");
        }

        // 委托给 EventDispatcher 触发 AutosaveEvent
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
     * Enable general HTML support
     */
    public void setGeneralHtmlSupportEnabled(boolean enabled) {
        getElement().setProperty("ghsEnabled", enabled);
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
        if (contentManager != null) {
            return contentManager.getPlainText(editorData);
        }
        return Jsoup.parse(editorData).text();
    }

    /**
     * Get sanitized HTML (remove dangerous tags)
     */
    public String getSanitizedHtml() {
        if (contentManager != null) {
            return contentManager.getSanitizedHtml(editorData);
        }
        return sanitizeHtml(editorData, Safelist.relaxed());
    }

    /**
     * Sanitize HTML with specified rules
     */
    public String sanitizeHtml(String html, Safelist safelist) {
        if (contentManager != null) {
            return contentManager.sanitizeHtml(html, safelist);
        }
        if (html == null) {
            return "";
        }
        return Jsoup.clean(html, safelist);
    }

    /**
     * 获取内容字符数（不包含 HTML 标签）
     *
     * @return 字符数
     */
    public int getCharacterCount() {
        if (contentManager != null) {
            return contentManager.getCharacterCount(editorData);
        }
        return getPlainText().length();
    }

    /**
     * 获取内容单词数
     *
     * @return 单词数
     */
    public int getWordCount() {
        if (contentManager != null) {
            return contentManager.getWordCount(editorData);
        }
        String text = getPlainText();
        if (text.isEmpty()) return 0;
        return text.trim().split("\\s+").length;
    }

    /**
     * 检查内容是否为空
     *
     * @return 如果内容为空或只包含空白则返回 true
     */
    public boolean isContentEmpty() {
        if (contentManager != null) {
            return contentManager.isContentEmpty(editorData);
        }
        return getPlainText().trim().isEmpty();
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
     * 添加编辑器就绪事件监听器。
     * 当编辑器完全初始化并准备好接受用户输入时触发。
     *
     * <p>使用示例：</p>
     * <pre>
     * editor.addEditorReadyListener(event -&gt; {
     *     logger.info("Editor ready in {} ms", event.getInitializationTimeMs());
     *     event.getSource().focus();
     * });
     * </pre>
     *
     * @param listener 事件监听器
     * @return 用于移除监听器的注册对象
     */
    public Registration addEditorReadyListener(ComponentEventListener<EditorReadyEvent> listener) {
        return eventDispatcher.addEditorReadyListener(listener);
    }

    /**
     * 添加编辑器错误事件监听器。
     * 当编辑器遇到错误时触发。
     *
     * <p>使用示例：</p>
     * <pre>
     * editor.addEditorErrorListener(event -&gt; {
     *     EditorError error = event.getError();
     *     if (error.getSeverity() == ErrorSeverity.FATAL) {
     *         Notification.show("编辑器错误: " + error.getMessage(),
     *             Notification.Type.ERROR_MESSAGE);
     *     }
     * });
     * </pre>
     *
     * @param listener 事件监听器
     * @return 用于移除监听器的注册对象
     */
    public Registration addEditorErrorListener(ComponentEventListener<EditorErrorEvent> listener) {
        return eventDispatcher.addEditorErrorListener(listener);
    }

    /**
     * 添加自动保存事件监听器。
     * 当编辑器内容自动保存时触发。
     *
     * @param listener 事件监听器
     * @return 用于移除监听器的注册对象
     */
    public Registration addAutosaveListener(ComponentEventListener<AutosaveEvent> listener) {
        return eventDispatcher.addAutosaveListener(listener);
    }

    /**
     * 添加内容变更事件监听器。
     * 当编辑器内容发生变化时触发。
     *
     * @param listener 事件监听器
     * @return 用于移除监听器的注册对象
     */
    public Registration addContentChangeListener(ComponentEventListener<ContentChangeEvent> listener) {
        return eventDispatcher.addContentChangeListener(listener);
    }

    /**
     * 添加降级事件监听器。
     * 当编辑器因错误触发降级模式时触发。
     *
     * @param listener 事件监听器
     * @return 用于移除监听器的注册对象
     */
    public Registration addFallbackListener(ComponentEventListener<FallbackEvent> listener) {
        return eventDispatcher.addFallbackListener(listener);
    }

    // ==================== Enterprise Handlers ====================

    /**
     * 设置错误处理器。
     *
     * @param handler 错误处理器
     */
    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
        eventDispatcher.setErrorHandler(handler);
    }

    /**
     * 获取错误处理器。
     *
     * @return 错误处理器，可能为 null
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * 设置 HTML 清理器。
     *
     * @param sanitizer HTML 清理器
     */
    public void setHtmlSanitizer(HtmlSanitizer sanitizer) {
        this.htmlSanitizer = sanitizer;
    }

    /**
     * 获取 HTML 清理器。
     *
     * @return HTML 清理器，可能为 null
     */
    public HtmlSanitizer getHtmlSanitizer() {
        return htmlSanitizer;
    }

    /**
     * 设置上传处理器。
     *
     * @param handler 上传处理器
     */
    public void setUploadHandler(UploadHandler handler) {
        this.uploadHandler = handler;
    }

    /**
     * 获取上传处理器。
     *
     * @return 上传处理器，可能为 null
     */
    public UploadHandler getUploadHandler() {
        return uploadHandler;
    }

    /**
     * 设置降级模式。
     *
     * @param mode 降级模式
     */
    public void setFallbackMode(FallbackMode mode) {
        this.fallbackMode = mode;
        getElement().setProperty("fallbackMode", mode.getJsName());
    }

    /**
     * 获取降级模式。
     *
     * @return 当前降级模式
     */
    public FallbackMode getFallbackMode() {
        return fallbackMode;
    }

    /**
     * 获取已注册监听器的统计信息。
     * 用于调试和监控。
     *
     * @return 监听器统计信息
     */
    public EventDispatcher.ListenerStats getListenerStats() {
        return eventDispatcher.getListenerStats();
    }

    /**
     * 清理所有事件监听器。
     * 通常在组件销毁前调用。
     */
    public void cleanupListeners() {
        eventDispatcher.cleanup();
    }

    // ==================== Client Callable for Events ====================

    @ClientCallable
    private void fireEditorReady(double initTimeMs) {
        eventDispatcher.fireEditorReady((long) initTimeMs);
    }

    @ClientCallable
    private void fireEditorError(String code, String message, String severity,
                                  boolean recoverable, String stackTrace) {
        // 使用 EnumParser 安全解析 severity
        ErrorSeverity errorSeverity = EnumParser.parse(
            severity, ErrorSeverity.class, ErrorSeverity.ERROR, "fireEditorError");

        EditorError error = new EditorError(
            code, message,
            errorSeverity,
            recoverable, stackTrace
        );

        // 委托给 EventDispatcher 处理（包括错误处理器和事件分发）
        eventDispatcher.fireEditorError(error);
    }

    @ClientCallable
    private void fireContentChange(String oldContent, String newContent, String source) {
        // 使用 EnumParser 安全解析 source
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
     * 处理来自客户端的文件上传请求。
     * 文件内容以 Base64 编码传输。
     * 使用 UploadManager 确保线程安全和正确的回调顺序。
     *
     * @param uploadId 上传标识符，用于回调
     * @param fileName 文件名
     * @param mimeType MIME 类型
     * @param base64Data Base64 编码的文件内容
     */
    @ClientCallable
    private void handleFileUpload(String uploadId, String fileName, String mimeType, String base64Data) {
        if (uploadManager == null) {
            // 没有设置上传处理器，返回错误
            getElement().executeJs("this._resolveUpload($0, null, $1)",
                uploadId, "No upload handler configured");
            return;
        }

        // 使用 UploadManager 处理上传（线程安全）
        uploadManager.handleUpload(uploadId, fileName, mimeType, base64Data);
    }

    /**
     * 检查是否有正在进行的上传
     *
     * @return 是否有活跃上传
     */
    public boolean hasActiveUploads() {
        return uploadManager != null && uploadManager.hasActiveUploads();
    }

    /**
     * 获取活跃上传数量
     *
     * @return 活跃上传数
     */
    public int getActiveUploadCount() {
        return uploadManager != null ? uploadManager.getActiveUploadCount() : 0;
    }

    /**
     * 取消指定的上传任务
     *
     * @param uploadId 上传 ID
     * @return 是否成功取消
     */
    public boolean cancelUpload(String uploadId) {
        return uploadManager != null && uploadManager.cancelUpload(uploadId);
    }

    // ==================== Internal Methods for Builder ====================

    /**
     * Clear all plugins. Package-private for Builder access.
     */
    void clearPlugins() {
        plugins.clear();
    }

    /**
     * Add plugins from collection. Package-private for Builder access.
     */
    void addPluginsInternal(Collection<CKEditorPlugin> pluginsToAdd) {
        plugins.addAll(pluginsToAdd);
    }

    /**
     * Add custom plugins from collection. Package-private for Builder access.
     */
    void addCustomPluginsInternal(Collection<CustomPlugin> pluginsToAdd) {
        customPlugins.addAll(pluginsToAdd);
    }

    /**
     * Check if has plugins. Package-private for Builder access.
     */
    boolean hasPlugins() {
        return !plugins.isEmpty();
    }

    /**
     * Get plugins. Package-private for Builder access.
     */
    Set<CKEditorPlugin> getPluginsInternal() {
        return new LinkedHashSet<>(plugins);
    }

    /**
     * Set editor type. Package-private for Builder access.
     */
    void setEditorTypeInternal(CKEditorType type) {
        this.editorType = type;
    }

    /**
     * Set theme. Package-private for Builder access.
     */
    void setThemeInternal(CKEditorTheme theme) {
        this.theme = theme;
    }

    /**
     * Set language. Package-private for Builder access.
     */
    void setLanguageInternal(String language) {
        this.language = language;
    }

    /**
     * Get language. Package-private for Builder access.
     */
    String getLanguageInternal() {
        return this.language;
    }

    /**
     * Set fallback mode. Package-private for Builder access.
     */
    void setFallbackModeInternal(FallbackMode mode) {
        this.fallbackMode = mode;
    }

    /**
     * Set error handler. Package-private for Builder access.
     */
    void setErrorHandlerInternal(ErrorHandler handler) {
        this.errorHandler = handler;
    }

    /**
     * Set HTML sanitizer. Package-private for Builder access.
     */
    void setHtmlSanitizerInternal(HtmlSanitizer sanitizer) {
        this.htmlSanitizer = sanitizer;
    }

    /**
     * Set upload handler. Package-private for Builder access.
     */
    void setUploadHandlerInternal(UploadHandler handler) {
        this.uploadHandler = handler;
    }

    /**
     * Set editor data. Package-private for Builder access.
     */
    void setEditorDataInternal(String data) {
        this.editorData = data != null ? data : "";
    }

    /**
     * Set read-only. Package-private for Builder access.
     */
    void setReadOnlyInternal(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Set autosave. Package-private for Builder access.
     */
    void setAutosaveInternal(boolean enabled, int waitingTime, Consumer<String> callback) {
        this.autosave = enabled;
        this.autosaveWaitingTime = waitingTime;
        this.autosaveCallback = callback;
    }

    /**
     * Set license key. Package-private for Builder access.
     */
    void setLicenseKeyInternal(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    /**
     * Set toolbar. Package-private for Builder access.
     */
    void setToolbarInternal(String[] toolbar) {
        this.toolbar = toolbar;
    }

    /**
     * Set config. Package-private for Builder access.
     */
    void setConfigInternal(CKEditorConfig config) {
        this.config = config;
    }

    /**
     * Get config. Package-private for Builder access.
     */
    CKEditorConfig getConfigInternal() {
        return this.config;
    }

    // ==================== Backward Compatibility ====================

    /**
     * Dependency resolution mode for plugins.
     *
     * @deprecated Use {@link VaadinCKEditorBuilder.DependencyMode} instead.
     *             This alias is kept for backward compatibility.
     */
    @Deprecated
    public static class DependencyMode {
        /** @deprecated Use {@link VaadinCKEditorBuilder.DependencyMode#AUTO_RESOLVE} */
        @Deprecated
        public static final VaadinCKEditorBuilder.DependencyMode AUTO_RESOLVE =
            VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE;

        /** @deprecated Use {@link VaadinCKEditorBuilder.DependencyMode#AUTO_RESOLVE_WITH_RECOMMENDED} */
        @Deprecated
        public static final VaadinCKEditorBuilder.DependencyMode AUTO_RESOLVE_WITH_RECOMMENDED =
            VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE_WITH_RECOMMENDED;

        /** @deprecated Use {@link VaadinCKEditorBuilder.DependencyMode#STRICT} */
        @Deprecated
        public static final VaadinCKEditorBuilder.DependencyMode STRICT =
            VaadinCKEditorBuilder.DependencyMode.STRICT;

        /** @deprecated Use {@link VaadinCKEditorBuilder.DependencyMode#MANUAL} */
        @Deprecated
        public static final VaadinCKEditorBuilder.DependencyMode MANUAL =
            VaadinCKEditorBuilder.DependencyMode.MANUAL;

        private DependencyMode() {}
    }

    /**
     * Builder for VaadinCKEditor.
     *
     * @deprecated Use {@link VaadinCKEditorBuilder} directly instead.
     *             This alias is kept for backward compatibility.
     */
    @Deprecated
    public static class Builder extends VaadinCKEditorBuilder {
        /**
         * @deprecated Use {@link VaadinCKEditorBuilder#create()} instead.
         */
        @Deprecated
        public Builder() {
            super();
        }
    }
}
