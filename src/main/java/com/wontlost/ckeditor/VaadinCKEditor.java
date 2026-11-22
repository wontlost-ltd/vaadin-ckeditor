package com.wontlost.ckeditor;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.vaadin.flow.component.ClientCallable;
import com.wontlost.ckeditor.collaboration.CollaborationBridge;
import com.wontlost.ckeditor.collaboration.CollaborationException;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.data.binder.HasValidator;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.ValidationStatusChangeEvent;
import com.vaadin.flow.data.binder.ValidationStatusChangeListener;
import com.vaadin.flow.data.binder.Validator;
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
import com.wontlost.ckeditor.internal.EditorTextStats;
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
@NpmPackage(value = "ckeditor5", version = "49.1.0")
@NpmPackage(value = "lit", version = "^3.3.3")
@NpmPackage(value = "highlight.js", version = "11.11.1")
public class VaadinCKEditor extends CustomField<String> implements HasAriaLabel, HasValidator<String> {

    private static final Logger logger = Logger.getLogger(VaadinCKEditor.class.getName());
    /** Keep in sync with version field in vaadin-ckeditor.ts */
    private static final String VERSION = "6.1.0";

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
    /**
     * 客户端上一次上报 contentChange 时的新内容（服务端镜像，issue #137）。
     * 客户端在镜像可用时不再回传旧内容，由这里补全 {@link ContentChangeEvent#getOldContent()}。
     */
    private String lastContentChangeValue = "";
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
    /**
     * autosave 回调。公开 API 的参数类型是 {@link Consumer}，用户传入的 lambda 通常不可序列化，
     * 因此由 {@link #writeObject} 按「可序列化才写入」处理，避免一个回调拖垮整个会话的序列化。
     */
    private transient Consumer<String> autosaveCallback;
    private String licenseKey = DEFAULT_LICENSE_KEY;
    private ErrorHandler errorHandler;
    private HtmlSanitizer htmlSanitizer;
    /**
     * 是否在客户端内容写入模型时即执行净化。
     * 默认 false —— 保持既有行为（getValue() 返回原始 HTML），避免破坏现有用户。
     */
    private boolean sanitizeOnInput = false;
    private UploadHandler uploadHandler;
    private UploadHandler.UploadConfig uploadConfig;
    private FallbackMode fallbackMode = FallbackMode.TEXTAREA;
    private FullscreenPage fullscreenPage = FullscreenPage.A4_FIT;
    private CollaborationBridge collaborationBridge;
    /** 代码块高亮插件名，与连接器 addon-plugins.ts 中的登记一致。 */
    static final String CODE_HIGHLIGHT_PLUGIN = "VaadinCodeBlockHighlighting";
    private static final String CODE_HIGHLIGHT_IMPORT_PATH = "./vaadin-ckeditor/code-block-highlighting";
    /** initialize() 之后插件列表才下发到客户端；之前的改动随 initialize 一次性下发。 */
    private boolean initialized = false;
    private boolean codeBlockHighlighting = false;
    private CodeHighlightTheme codeBlockHighlightTheme = CodeHighlightTheme.AUTO;

    /**
     * The add-on's word count bridge plugin ({@code word-count-plugin.ts}), registered in the browser's
     * plugin registry. It is added automatically by {@link #addWordCountChangeListener} and
     * {@link #setWordCountVisible(boolean) setWordCountVisible(true)}; it requires CKEditor's
     * {@code WordCount}. Register a subclass under the same name with {@code registerCKEditorPlugin}
     * to extend or replace it.
     */
    public static final CustomPlugin WORD_COUNT_PLUGIN = CustomPlugin.fromRegistry("VaadinWordCount");

    /** Default error message when the word limit is exceeded ({0} = limit, {1} = count). */
    public static final String DEFAULT_MAX_WORDS_ERROR = "Maximum {0} words allowed ({1} entered)";
    /** Default error message when the character limit is exceeded ({0} = limit, {1} = count). */
    public static final String DEFAULT_MAX_CHARACTERS_ERROR = "Maximum {0} characters allowed ({1} entered)";

    /** 字数上限，0 表示不限。 */
    private int maxWords;
    /** 字符数上限，0 表示不限。 */
    private int maxCharacters;
    private String maxWordsErrorMessage = DEFAULT_MAX_WORDS_ERROR;
    private String maxCharactersErrorMessage = DEFAULT_MAX_CHARACTERS_ERROR;
    private boolean wordCountVisible;
    /** true 时不在值变化时自行校验（与 Vaadin 字段的 setManualValidation 语义一致）。 */
    private boolean manualValidation;
    /** 当前 invalid 状态是否由字数 / 字符数上限设置——只撤销自己设置的状态，不覆盖应用自己的 setInvalid。 */
    private boolean limitInvalid;
    /** 上限校验最近一次设置的错误消息。 */
    private String limitErrorMessage;
    private final List<ValidationStatusChangeListener<String>> validationStatusListeners = new ArrayList<>();

    // Internal managers
    /**
     * 上传管理器持有进行中的任务与 Future，属于瞬态：不参与会话序列化，
     * 反序列化后由 {@link #readObject} 按 uploadHandler / uploadConfig 重建。
     */
    private transient UploadManager uploadManager;
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
        // 先于 Binder 注册：Binder 的值变化监听器随后运行，其完整校验结果（含 asRequired 等）最终生效
        addValueChangeListener(e -> validateLimits());
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
        syncFullscreenPage();

        // Set plugin list
        getElement().setPropertyJson("plugins", buildPluginsJson());
        initialized = true;

        // Set toolbar
        if (toolbar != null && toolbar.length > 0) {
            getElement().setPropertyJson("toolbar", buildToolbarJson());
        }

        // Set configuration
        if (config != null) {
            getElement().setPropertyJson("config", config.toJson());
        }

        // builder 的 withValue 不触发值变化事件：按初始值校验一次上限
        validateLimits();
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

        // 把 errorHandler 接到 eventDispatcher 上。
        // 公开的 setErrorHandler() 会同时写字段与 dispatcher，但 builder 走的是
        // setErrorHandlerInternal()，只写字段；若此处不补接，builder 配置的
        // ErrorHandler 将永远不会被 fireEditorError 调用（静默失效）。
        // 与上面 contentManager、下面 uploadManager 的装配方式保持一致：
        // 统一在初始化阶段从字段重建内部管理器。
        if (errorHandler != null) {
            eventDispatcher.setErrorHandler(errorHandler);
        }

        // Initialize upload manager if upload handler is configured
        if (uploadHandler != null) {
            this.uploadManager = createUploadManager();
        }
    }

    private UploadManager createUploadManager() {
        // Use WeakReference to avoid memory leaks
        // Lambda implicitly captures 'this', if UploadManager outlives VaadinCKEditor,
        // it would prevent VaadinCKEditor from being garbage collected
        final WeakReference<VaadinCKEditor> editorRef = new WeakReference<>(this);

        return new UploadManager(
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

    // ==================== Serialization ====================

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
        out.defaultWriteObject();
        boolean serializable = autosaveCallback == null || autosaveCallback instanceof java.io.Serializable;
        if (!serializable) {
            logger.warning("autosave callback is not Serializable and will be lost when the session is "
                + "serialized; pass a serializable callback to keep it across session replication");
        }
        out.writeObject(serializable ? autosaveCallback : null);
        // 只记录「是否存在」：反序列化后仅重建原本存在的上传管理器，语义与序列化前一致
        out.writeBoolean(uploadManager != null);
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.autosaveCallback = (Consumer<String>) in.readObject();
        if (in.readBoolean() && uploadHandler != null) {
            this.uploadManager = createUploadManager();
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
            if (plugin.getConfigKey() != null) {
                pluginObj.put("configKey", plugin.getConfigKey());
                pluginObj.set("config", plugin.getConfig());
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

    /**
     * 返回编辑器当前的 HTML 内容。
     *
     * <p><strong>⚠️ 安全提示：本方法返回的是未经净化的原始 HTML。</strong>
     * 内容来自浏览器端，必须视为不可信输入——CKEditor 的模型过滤属于客户端控制，
     * 攻击者可绕过它直接调用服务端 RPC 提交任意 HTML。</p>
     *
     * <p><strong>这一点对 Binder 尤其重要。</strong>本组件继承 {@code CustomField<String>}，
     * 因此 {@code binder.forField(editor).bind(...)} 读取的正是本方法，
     * 而 <em>不会</em> 经过 {@link #setHtmlSanitizer(HtmlSanitizer)} 配置的净化器。
     * 换言之，仅调用 {@code setHtmlSanitizer(...)} 并不能让 Binder 绑定的值得到净化。</p>
     *
     * <p>需要净化后的内容时，请选择其一：</p>
     * <ul>
     *   <li>显式调用 {@link #getSanitizedValue()}；或</li>
     *   <li>调用 {@link #setSanitizeOnInput(boolean) setSanitizeOnInput(true)}，
     *       使来自客户端的内容在写入模型时即被净化——此时本方法返回的也是净化后的值，
     *       Binder 路径同样受保护。</li>
     * </ul>
     *
     * <p>之所以默认返回原始 HTML，是为了不破坏既有用户的行为（富文本往返场景确实
     * 需要原始标记）；净化改为显式开启。</p>
     *
     * @return 编辑器 HTML 内容；未开启 {@link #setSanitizeOnInput(boolean)} 时为未净化的原始内容
     * @see #getSanitizedValue()
     * @see #setSanitizeOnInput(boolean)
     */
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
        // 先规范化 null 为 ""，再交给 super.setValue。
        // 否则事件构造时 getValue() 会读到 null（presentation 阶段写入），
        // 而方法返回后 getValue() 又变为 ""，导致监听器值与最终值不一致（与 issue #85 同源）。
        String newValue = value != null ? value : "";
        this.editorData = newValue;
        super.setValue(newValue);
        getElement().setProperty("editorData", this.editorData);
        updateEditorData();
    }

    @Override
    protected void setModelValue(String value, boolean fromClient) {
        String oldValue = this.editorData;
        String newValue = value != null ? value : "";

        // 可选的入口净化（setSanitizeOnInput(true) 时启用）。
        // 只净化 fromClient==true 的值：客户端内容不可信，是真正的信任边界；
        // 服务端自己 setValue 的内容视为可信，不做改写以免破坏程序化设值。
        if (sanitizeOnInput && fromClient && htmlSanitizer != null && !newValue.isEmpty()) {
            newValue = contentManager.getSanitizedValue(newValue);
        }
        // Only update when value actually changes
        if (java.util.Objects.equals(oldValue, newValue)) {
            return;
        }
        // 必须先更新 editorData，再调用 super.setModelValue。
        // 原因：本类重写了 getValue() 返回 editorData，而 Vaadin 在
        // ComponentValueChangeEvent 构造时会调用 getValue() 读取新值
        // （见 AbstractField.ComponentValueChangeEvent，this.value = hasValue.getValue()）。
        // 若先 fire 事件再赋值，监听器拿到的将是旧内容（issue #85）。
        this.editorData = newValue;
        // super.setModelValue 会经由 Vaadin 的 AbstractField 触发 ValueChangeEvent，
        // 因此不能再额外调用 fireEvent，避免重复事件。
        super.setModelValue(newValue, fromClient);
    }

    @ClientCallable
    private void setEditorData(String data) {
        setModelValue(data, true);
    }

    @ClientCallable
    private void saveEditorData(String data) {
        saveEditorDataInternal(data);
    }

    /**
     * autosave 落盘逻辑（package-private 测试缝隙）：调用回调，捕获其异常并产出成功/失败的
     * AutosaveEvent。抽出以便在不经过客户端 RPC 的前提下直接测试回调一致性与异常处理（review）。
     */
    void saveEditorDataInternal(String data) {
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

    /**
     * 让客户端编辑器应用最新的 {@code editorData} 属性。
     *
     * <p>不把内容作为参数再传一遍（issue #137）：同一次往返中属性变更先于 JS 调用生效，
     * 客户端读到的 {@code this.editorData} 已是新值；属性值未变（服务端重设同一内容以覆盖
     * 用户编辑）时 Vaadin 不会重发属性，而客户端属性仍保持该值，结果同样正确。
     * 这样大文档只占一份报文体积，而不是属性与参数各一份。</p>
     */
    private void updateEditorData() {
        getElement().executeJs("this.updateData(this.editorData)");
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

    /**
     * 同步 enabled 状态到前端（issue #46）。
     *
     * <p>沿用 Vaadin 推荐的 {@link com.vaadin.flow.component.Component#onEnabledStateChanged(boolean)}
     * 钩子，而非覆写 {@code setEnabled}——Vaadin 自身负责 disabled 属性的传播，这里只把状态
     * 推给 CKEditor 前端，使禁用时编辑器不可交互（CKEditor 5 通过只读锁实现禁用）。</p>
     */
    @Override
    public void onEnabledStateChanged(boolean enabled) {
        super.onEnabledStateChanged(enabled);
        getElement().setProperty("isEnabled", enabled);
        getId().ifPresent(id ->
            getElement().executeJs("this.isEnabled = $0", enabled));
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
     * Set the page width used in fullscreen mode (the {@code FULLSCREEN} plugin). Defaults to
     * {@link FullscreenPage#A4_FIT}; use {@link FullscreenPage#A4} for CKEditor's fixed 795.701px page.
     * Applies to this editor only and takes effect immediately, also while fullscreen is open.
     * CKEditor's fullscreen supports {@code CLASSIC} and {@code DECOUPLED} editors only, so for
     * {@code BALLOON} and {@code INLINE} editors the value is stored but has no effect.
     *
     * @param page the page width, not null
     */
    public void setFullscreenPage(FullscreenPage page) {
        this.fullscreenPage = Objects.requireNonNull(page, "page");
        syncFullscreenPage();
    }

    /**
     * @return the page width used in fullscreen mode, {@link FullscreenPage#A4_FIT} unless changed
     */
    public FullscreenPage getFullscreenPage() {
        return fullscreenPage;
    }

    /**
     * Enable syntax highlighting of code blocks while editing (opt-in, off by default).
     *
     * <p>Needs the {@link CKEditorPlugin#CODE_BLOCK} plugin (part of the {@code FULL}, {@code AI_DOCUMENT} and
     * {@code NOTION} presets; otherwise add it with {@code addPlugin(CKEditorPlugin.CODE_BLOCK)}).
     * It is a custom plugin shipped with the add-on ({@code VaadinCodeBlockHighlighting}, see
     * {@link CustomPlugin}): it is added to the editor only once highlighting is enabled, and its module
     * and highlight.js (the "common" bundle of about 36 languages) are downloaded only then. Enabling
     * it on an editor that is already displayed recreates the editor once with its current content
     * (undo history is reset); after that, switching on/off and changing the theme apply immediately.
     * Highlighting applies to the editing view only: the model is not changed, undo/redo is not
     * affected, and the HTML returned by {@link #getValue()} contains no highlight markup. Each code
     * block is highlighted according to its language from {@code codeBlock.languages}; plain text and
     * languages unknown to highlight.js stay unhighlighted.</p>
     *
     * @param enabled true to highlight code blocks
     * @since 6.2.0
     * @see #setCodeBlockHighlightTheme(CodeHighlightTheme)
     */
    public void setCodeBlockHighlighting(boolean enabled) {
        this.codeBlockHighlighting = enabled;
        syncCodeBlockHighlighting();
    }

    /**
     * @return whether code block syntax highlighting is enabled
     * @since 6.2.0
     */
    public boolean isCodeBlockHighlighting() {
        return codeBlockHighlighting;
    }

    /**
     * Set the color theme for code block syntax highlighting. Defaults to
     * {@link CodeHighlightTheme#AUTO}, which follows the editor's light/dark theme.
     * Takes effect immediately.
     *
     * @param theme the theme, not null
     * @since 6.2.0
     */
    public void setCodeBlockHighlightTheme(CodeHighlightTheme theme) {
        this.codeBlockHighlightTheme = Objects.requireNonNull(theme, "theme");
        syncCodeBlockHighlighting();
    }

    /**
     * @return the code block highlighting theme, {@link CodeHighlightTheme#AUTO} unless changed
     * @since 6.2.0
     */
    public CodeHighlightTheme getCodeBlockHighlightTheme() {
        return codeBlockHighlightTheme;
    }

    /**
     * 代码块高亮以 add-on 自带的自定义插件实现（连接器 addon-plugins.ts 登记的按需加载器）：
     * 从未启用过就不加入插件列表（不下载插件与 highlight.js）；启用后插件留在列表中，开关与配色经
     * 插件配置 {@code codeBlockHighlighting: {enabled, theme}} 传递，编辑器显示中改变也不重建。
     */
    private void syncCodeBlockHighlighting() {
        boolean registered = customPlugins.stream().anyMatch(p -> CODE_HIGHLIGHT_PLUGIN.equals(p.getJsName()));
        if (!codeBlockHighlighting && !registered) {
            return;
        }
        ObjectNode pluginConfig = createObjectNode();
        pluginConfig.put("enabled", codeBlockHighlighting);
        pluginConfig.put("theme", codeBlockHighlightTheme.getJsName());
        customPlugins.removeIf(p -> CODE_HIGHLIGHT_PLUGIN.equals(p.getJsName()));
        customPlugins.add(CustomPlugin.builder(CODE_HIGHLIGHT_PLUGIN)
            .withImportPath(CODE_HIGHLIGHT_IMPORT_PATH)
            .withConfig("codeBlockHighlighting", pluginConfig)
            .build());
        syncPluginsToClient();
    }

    /** initialize() 之后插件列表的变化重新下发；之前的变化随 initialize 一次性下发。 */
    private void syncPluginsToClient() {
        if (initialized) {
            getElement().setPropertyJson("plugins", buildPluginsJson());
        }
    }

    private void syncFullscreenPage() {
        getElement().setProperty("fullscreenPage", fullscreenPage.getMode());
        getElement().setProperty("fullscreenPageWidth",
                fullscreenPage.getCssWidth() != null ? fullscreenPage.getCssWidth() : "");
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
     * Move the editor caret (collapsed selection) to the very start of the document
     * and focus the editor.
     *
     * <p>Useful when leading block content (e.g. a table used as a letterhead) would
     * otherwise be auto-selected/highlighted on focus (issue #52). Calling this places
     * the caret before the first content so nothing is highlighted.</p>
     */
    public void setCaretToStart() {
        getId().ifPresent(id ->
            getElement().executeJs("this.setCaretToStart()"));
    }

    /**
     * Move the editor caret (collapsed selection) to the very end of the document
     * and focus the editor.
     */
    public void setCaretToEnd() {
        getId().ifPresent(id ->
            getElement().executeJs("this.setCaretToEnd()"));
    }

    /**
     * Programmatically focus the editor's editable area.
     */
    public void focusEditor() {
        getId().ifPresent(id ->
            getElement().executeJs("this.focusEditor()"));
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
     * <p>This is the historical server-side estimate (whitespace-separated words, each CJK character
     * counted as a word). It can differ from CKEditor's WordCount plugin, which
     * {@link #addWordCountChangeListener(ComponentEventListener)} and {@link #setMaxWords(int)} use.</p>
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

    // ==================== Word Count & Limits ====================

    /**
     * Listen to the live word and character count while the user types.
     *
     * <p>The counts come from CKEditor's
     * <a href="https://ckeditor.com/docs/ckeditor5/latest/features/word-count.html">WordCount</a> plugin through
     * the add-on's {@link #WORD_COUNT_PLUGIN} custom plugin, added automatically by this method and by
     * {@link #setWordCountVisible(boolean)}.
     * Events arrive at most once per 500 ms, only when a count changed, plus once with the initial counts.
     * Register the listener (or turn the counter on) before the editor is attached, or add
     * {@link #WORD_COUNT_PLUGIN} with {@code addCustomPlugin} up front: on an editor that is already displayed,
     * adding the plugin recreates the editor once with its current content (undo history starts anew).</p>
     *
     * @param listener the listener
     * @return registration for removing the listener
     */
    public Registration addWordCountChangeListener(ComponentEventListener<WordCountChangeEvent> listener) {
        requireWordCountPlugin();
        Registration registration = eventDispatcher.addWordCountChangeListener(listener);
        syncWordCountEventsProperty();
        return () -> {
            registration.remove();
            syncWordCountEventsProperty();
        };
    }

    /**
     * 以自定义插件机制加载字数桥接插件；插件列表在 initialize() 后变化时重新下发
     * （已显示的编辑器由连接器以当前内容重建一次）。
     */
    private void requireWordCountPlugin() {
        if (customPlugins.add(WORD_COUNT_PLUGIN)) {
            syncPluginsToClient();
        }
    }

    private void syncWordCountEventsProperty() {
        getElement().setProperty("wordCountEvents", eventDispatcher.hasWordCountChangeListeners());
    }

    /**
     * Show CKEditor's word and character counter below the editor. Off by default. The counter turns
     * red when a limit set with {@link #setMaxWords(int)} / {@link #setMaxCharacters(int)} is exceeded.
     * Use {@code config.set("wordCount", ...)} with {@code displayWords} / {@code displayCharacters}
     * to show only one of the counts.
     *
     * @param visible true to show the counter
     */
    public void setWordCountVisible(boolean visible) {
        this.wordCountVisible = visible;
        if (visible) {
            requireWordCountPlugin();
        }
        getElement().setProperty("wordCountVisible", visible);
    }

    /**
     * @return whether the word count display is visible
     */
    public boolean isWordCountVisible() {
        return wordCountVisible;
    }

    /**
     * Limit the number of words. Exceeding the limit does not block typing; the field becomes invalid
     * (Vaadin's red error state with {@link #setMaxWordsErrorMessage(String) the error message}) and a
     * bound {@code Binder} reports a validation error, since the limit is part of
     * {@link #getDefaultValidator()}.
     *
     * <p>Words are counted on the server from the value with the rules of CKEditor's WordCount plugin,
     * so the limit cannot be bypassed by the client and agrees with the counter the user sees. The
     * invalid state follows the value: it updates whenever the value reaches the server, on every
     * change by default and on blur after {@link #setSynchronized(boolean) setSynchronized(false)}.</p>
     *
     * @param maxWords the limit, {@code 0} for no limit
     * @throws IllegalArgumentException if negative
     */
    public void setMaxWords(int maxWords) {
        this.maxWords = requireLimit(maxWords, "maxWords");
        getElement().setProperty("maxWords", maxWords);
        limitsChanged();
    }

    /**
     * @return the word limit, {@code 0} when unlimited
     */
    public int getMaxWords() {
        return maxWords;
    }

    /**
     * Limit the number of characters (spaces included, as CKEditor's WordCount counts them).
     * Behaves like {@link #setMaxWords(int)}.
     *
     * @param maxCharacters the limit, {@code 0} for no limit
     * @throws IllegalArgumentException if negative
     */
    public void setMaxCharacters(int maxCharacters) {
        this.maxCharacters = requireLimit(maxCharacters, "maxCharacters");
        getElement().setProperty("maxCharacters", maxCharacters);
        limitsChanged();
    }

    /**
     * @return the character limit, {@code 0} when unlimited
     */
    public int getMaxCharacters() {
        return maxCharacters;
    }

    /**
     * Set the error message shown when the word limit is exceeded. {@code {0}} is replaced with the
     * limit and {@code {1}} with the current count. Default: {@value #DEFAULT_MAX_WORDS_ERROR}.
     *
     * @param template the message template, not null
     */
    public void setMaxWordsErrorMessage(String template) {
        this.maxWordsErrorMessage = Objects.requireNonNull(template, "template");
        limitsChanged();
    }

    /**
     * Set the error message shown when the character limit is exceeded. {@code {0}} is replaced with
     * the limit and {@code {1}} with the current count. Default: {@value #DEFAULT_MAX_CHARACTERS_ERROR}.
     *
     * @param template the message template, not null
     */
    public void setMaxCharactersErrorMessage(String template) {
        this.maxCharactersErrorMessage = Objects.requireNonNull(template, "template");
        limitsChanged();
    }

    /**
     * Validator for the word / character limits. {@code Binder.forField(editor)} adds it automatically
     * (unless default validators are disabled), so {@code binder.validate()} and {@code writeBean} fail
     * over the limit. Always passes when no limit is set.
     */
    @Override
    public Validator<String> getDefaultValidator() {
        return (value, context) -> checkLimits(value);
    }

    @Override
    public Registration addValidationStatusChangeListener(ValidationStatusChangeListener<String> listener) {
        validationStatusListeners.add(listener);
        return () -> validationStatusListeners.remove(listener);
    }

    /**
     * When {@code true}, the editor does not mark itself invalid on value changes; validation is left to
     * the application (e.g. {@code binder.validate()}). Default {@code false}.
     */
    @Override
    public void setManualValidation(boolean enabled) {
        this.manualValidation = enabled;
    }

    private static int requireLimit(int limit, String name) {
        if (limit < 0) {
            throw new IllegalArgumentException(name + " must be 0 (no limit) or positive, got: " + limit);
        }
        return limit;
    }

    /** 上限或消息变化：重新自校验，并通知 Binder 重新校验（值未变时 Binder 不会自行触发）。 */
    private void limitsChanged() {
        validateLimits();
        boolean valid = !checkLimits(getValue()).isError();
        for (ValidationStatusChangeListener<String> listener : List.copyOf(validationStatusListeners)) {
            listener.validationStatusChanged(new ValidationStatusChangeEvent<>(this, valid));
        }
    }

    ValidationResult checkLimits(String value) {
        if (maxWords == 0 && maxCharacters == 0) {
            return ValidationResult.ok();
        }
        EditorTextStats.Stats stats = EditorTextStats.of(value);
        if (maxWords > 0 && stats.words() > maxWords) {
            return ValidationResult.error(limitMessage(maxWordsErrorMessage, maxWords, stats.words()));
        }
        if (maxCharacters > 0 && stats.characters() > maxCharacters) {
            return ValidationResult.error(limitMessage(maxCharactersErrorMessage, maxCharacters, stats.characters()));
        }
        return ValidationResult.ok();
    }

    private static String limitMessage(String template, int limit, int count) {
        // 不用 MessageFormat：模板里的撇号（如 "can't"）在 MessageFormat 中是转义符
        return template.replace("{0}", Integer.toString(limit)).replace("{1}", Integer.toString(count));
    }

    /**
     * 自校验字数 / 字符数上限。只撤销自己设置的 invalid，应用或 Binder 设置的其它错误不受影响。
     */
    private void validateLimits() {
        if (manualValidation) {
            return;
        }
        ValidationResult result = checkLimits(getValue());
        if (result.isError()) {
            limitErrorMessage = result.getErrorMessage();
            setErrorMessage(limitErrorMessage);
            setInvalid(true);
            limitInvalid = true;
        } else if (limitInvalid) {
            // 只撤销仍由上限设置的状态：错误消息已被 Binder / 应用换成别的，说明状态已不属于这里
            if (isInvalid() && Objects.equals(getErrorMessage(), limitErrorMessage)) {
                setInvalid(false);
            }
            limitInvalid = false;
        }
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
     * <p>客户端只在存在监听器时才上报内容变更（issue #137）。注册后需一次服务端往返
     * 客户端才会得知，在此之前发生的变更不会产生事件。</p>
     *
     * @param listener the event listener
     * @return registration object for removing the listener
     */
    public Registration addContentChangeListener(ComponentEventListener<ContentChangeEvent> listener) {
        Registration registration = eventDispatcher.addContentChangeListener(listener);
        syncContentChangeEventsProperty();
        return () -> {
            registration.remove();
            syncContentChangeEventsProperty();
        };
    }

    /**
     * 告知客户端当前是否有 ContentChange 监听器：没有时客户端既不上报 contentChange，
     * 也不为它序列化文档（issue #137）。
     */
    private void syncContentChangeEventsProperty() {
        getElement().setProperty("contentChangeEvents", eventDispatcher.hasContentChangeListeners());
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
     * 设置是否在客户端内容写入模型时立即净化。
     *
     * <p>默认 {@code false}：{@link #getValue()} 返回未净化的原始 HTML，净化仅在
     * 显式调用 {@link #getSanitizedValue()} 时发生。这保持了既有行为，
     * 但意味着 {@code Binder} 绑定路径（读取 {@code getValue()}）不会被净化。</p>
     *
     * <p>置为 {@code true} 后，所有来自客户端的内容在写入模型时即被
     * {@link #setHtmlSanitizer(HtmlSanitizer)} 配置的净化器处理，
     * 于是 {@link #getValue()}、Binder、以及 {@code ValueChangeEvent} 拿到的
     * 都是已净化的值——即在信任边界处一次性净化，而不依赖每个调用点记得改用
     * {@code getSanitizedValue()}。</p>
     *
     * <p>需要配合 {@link #setHtmlSanitizer(HtmlSanitizer)} 使用；未配置净化器时本开关无效果。
     * 服务端自行 {@code setValue(...)} 的内容不受影响（视为可信来源）。</p>
     *
     * @param sanitizeOnInput true 表示在客户端输入写入模型时净化
     * @see #setHtmlSanitizer(HtmlSanitizer)
     * @see #getValue()
     */
    public void setSanitizeOnInput(boolean sanitizeOnInput) {
        this.sanitizeOnInput = sanitizeOnInput;
    }

    /**
     * 是否已开启客户端输入的入口净化。
     *
     * @return true 表示开启
     * @see #setSanitizeOnInput(boolean)
     */
    public boolean isSanitizeOnInput() {
        return sanitizeOnInput;
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
        syncContentChangeEventsProperty();
        syncWordCountEventsProperty();
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

    /**
     * 客户端一次 change:data 的合并上报（issue #137）：新内容只传一次，
     * 依次触发 ContentChangeEvent 与值同步，顺序与拆分前的两次 RPC 一致。
     *
     * @param newContent       编辑器当前内容
     * @param oldContent       上次已知内容；为 null 时使用服务端镜像 {@link #lastContentChangeValue}
     * @param source           变更来源标签
     * @param fireChange       是否触发 ContentChangeEvent
     * @param syncToServer     是否作为客户端输入写入字段值
     */
    @ClientCallable
    private void syncContent(String newContent, String oldContent, String source,
                             boolean fireChange, boolean syncToServer) {
        syncContentInternal(newContent, oldContent, source, fireChange, syncToServer);
    }

    /** {@link #syncContent} 的实现（package-private 测试缝隙，便于不经客户端 RPC 直接测试）。 */
    void syncContentInternal(String newContent, String oldContent, String source,
                             boolean fireChange, boolean syncToServer) {
        String content = newContent != null ? newContent : "";
        if (fireChange) {
            ChangeSource changeSource = EnumParser.parse(
                source, ChangeSource.class, ChangeSource.UNKNOWN, "syncContent");
            String previous = oldContent != null ? oldContent : lastContentChangeValue;
            lastContentChangeValue = content;
            eventDispatcher.fireContentChange(previous, content, changeSource);
        }
        if (syncToServer) {
            setModelValue(content, true);
        }
    }

    /**
     * 客户端 WordCount 插件上报的计数（已节流、去重，见 word-count-reporter.ts）。
     */
    @ClientCallable
    private void fireWordCountChange(int words, int characters) {
        if (words < 0 || characters < 0) {
            logger.fine("Ignoring invalid word count from client");
            return;
        }
        eventDispatcher.fireWordCountChange(words, characters);
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

    // ==================== Comments / track changes persistence ====================

    /**
     * Persist CKEditor comments and track-changes suggestions through a server-side store, without CKEditor
     * Cloud. Adds the custom plugin {@link CollaborationBridge#PLUGIN_NAME} which installs the
     * {@code CommentsRepository} and {@code TrackChanges} adapters in the browser; load the premium plugins
     * themselves with {@code CustomPlugin.fromPremium("Comments")} / {@code fromPremium("TrackChanges")} and a
     * licence key. Set it before the editor is shown (with the builder or before attaching): on a displayed
     * editor, adding or removing the plugin recreates the editor once with its current content (undo history
     * starts anew).
     *
     * @param bridge the bridge, or {@code null} to remove it
     * @since 6.2.0
     * @see CollaborationBridge
     */
    public void setCollaborationBridge(CollaborationBridge bridge) {
        customPlugins.removeIf(p -> CollaborationBridge.PLUGIN_NAME.equals(p.getJsName()));
        this.collaborationBridge = bridge;
        if (bridge != null) {
            customPlugins.add(bridge.toCustomPlugin());
            getElement().setProperty("collaborationTimeoutMs", bridge.getRequestTimeout().toMillis());
        }
        syncPluginsToClient();
    }

    /**
     * @return the collaboration bridge, or {@code null}
     * @since 6.2.0
     */
    public CollaborationBridge getCollaborationBridge() {
        return collaborationBridge;
    }

    /**
     * One adapter call of the {@code VaadinCollaborationBridge} plugin. The answer is delivered as a
     * {@code vaadin-ckeditor-collaboration} DOM event on this element.
     */
    @ClientCallable
    private void collaborationRequest(String requestId, String operation, String payloadJson) {
        handleCollaborationRequest(requestId, operation, payloadJson);
    }

    /** {@link #collaborationRequest} 的实现：处理完成后在 UI 线程以 DOM 事件应答。 */
    private void handleCollaborationRequest(String requestId, String operation, String payloadJson) {
        collaborationResponse(collaborationBridge, operation, payloadJson).thenAccept(response ->
            getUI().ifPresent(ui -> ui.access(() ->
                respondToCollaborationRequest(requestId, response.resultJson(), response.error()))));
    }

    /** 一次协作请求的应答：成功时 {@code resultJson} 非空，失败时 {@code error} 为可展示给用户的消息。 */
    record CollaborationResponse(String resultJson, String error) {
    }

    /**
     * 处理一次协作请求（package-private 测试缝隙）。{@link CollaborationException} 的消息原样回给浏览器；
     * 其他失败只回通用消息，详情记录在服务端日志，避免泄露内部信息。返回的 future 总是正常完成。
     */
    static java.util.concurrent.CompletableFuture<CollaborationResponse> collaborationResponse(
            CollaborationBridge bridge, String operation, String payloadJson) {
        if (bridge == null) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                new CollaborationResponse(null, "No collaboration bridge configured"));
        }
        java.util.concurrent.CompletableFuture<tools.jackson.databind.JsonNode> result;
        try {
            result = bridge.handle(operation, JsonUtil.getMapper().readTree(payloadJson == null ? "null" : payloadJson));
        } catch (RuntimeException e) {
            result = java.util.concurrent.CompletableFuture.failedFuture(
                new CollaborationException(CollaborationException.Reason.INVALID_REQUEST, "Invalid request"));
        }
        return result.handle((value, error) -> {
            if (error == null) {
                return new CollaborationResponse(String.valueOf(value), null);
            }
            Throwable cause = CollaborationBridge.unwrap(error);
            if (cause instanceof CollaborationException) {
                return new CollaborationResponse(null, cause.getMessage());
            }
            logger.log(Level.WARNING, "Collaboration request " + operation + " failed", cause);
            return new CollaborationResponse(null, "Collaboration request failed");
        });
    }

    private void respondToCollaborationRequest(String requestId, String resultJson, String error) {
        getElement().executeJs(
            "this.dispatchEvent(new CustomEvent('vaadin-ckeditor-collaboration', "
                + "{detail: {requestId: $0, result: $1, error: $2}}))",
            requestId, resultJson, error);
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
    String getLicenseKeyInternal() { return this.licenseKey; }
    void setToolbarInternal(String[] toolbar) { this.toolbar = toolbar != null ? toolbar.clone() : null; }
    void setConfigInternal(CKEditorConfig config) { this.config = config; }
    CKEditorConfig getConfigInternal() { return this.config; }

    void setAutosaveInternal(boolean enabled, int waitingTime, Consumer<String> callback) {
        this.autosave = enabled;
        this.autosaveWaitingTime = waitingTime;
        this.autosaveCallback = callback;
    }
}
