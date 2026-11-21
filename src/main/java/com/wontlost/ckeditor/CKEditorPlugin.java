package com.wontlost.ckeditor;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CKEditor 5 official plugin enumeration.
 * Each plugin corresponds to an export from the ckeditor5 npm package.
 *
 * <p>Usage example:</p>
 * <pre>
 * VaadinCKEditor.create()
 *     .withPlugins(CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC, CKEditorPlugin.IMAGE)
 *     .build();
 * </pre>
 */
public enum CKEditorPlugin {

    // ==================== Core Plugins ====================

    /** Essential editing features (required) */
    ESSENTIALS("Essentials", Category.CORE),

    /** Paragraph support (required) */
    PARAGRAPH("Paragraph", Category.CORE),

    /** Undo/Redo functionality */
    UNDO("Undo", Category.CORE, "undo", "redo"),

    /** Clipboard support */
    CLIPBOARD("Clipboard", Category.CORE),

    /** Input handling */
    TYPING("Typing", Category.CORE),

    /** Select all content */
    SELECT_ALL("SelectAll", Category.CORE, "selectAll"),

    // ==================== Basic Styles ====================

    /** Bold text */
    BOLD("Bold", Category.BASIC_STYLES, "bold"),

    /** Italic text */
    ITALIC("Italic", Category.BASIC_STYLES, "italic"),

    /** Underlined text */
    UNDERLINE("Underline", Category.BASIC_STYLES, "underline"),

    /** Strikethrough text */
    STRIKETHROUGH("Strikethrough", Category.BASIC_STYLES, "strikethrough"),

    /** Inline code */
    CODE("Code", Category.BASIC_STYLES, "code"),

    /** Superscript text */
    SUPERSCRIPT("Superscript", Category.BASIC_STYLES, "superscript"),

    /** Subscript text */
    SUBSCRIPT("Subscript", Category.BASIC_STYLES, "subscript"),

    // ==================== Font Styles ====================

    /** Font size control */
    FONT_SIZE("FontSize", Category.FONT, "fontSize"),

    /** Font family control */
    FONT_FAMILY("FontFamily", Category.FONT, "fontFamily"),

    /** Font color */
    FONT_COLOR("FontColor", Category.FONT, "fontColor"),

    /** Background color */
    FONT_BACKGROUND_COLOR("FontBackgroundColor", Category.FONT, "fontBackgroundColor"),

    // ==================== Paragraph Formatting ====================

    /** Headings */
    HEADING("Heading", Category.PARAGRAPH, "heading"),

    /** Text alignment */
    ALIGNMENT("Alignment", Category.PARAGRAPH, "alignment"),

    /** Indentation */
    INDENT("Indent", Category.PARAGRAPH, "indent", "outdent"),

    /** Block indentation */
    INDENT_BLOCK("IndentBlock", Category.PARAGRAPH),

    /** Block quotes */
    BLOCK_QUOTE("BlockQuote", Category.PARAGRAPH, "blockQuote"),

    // 注意：LineHeight 在 ckeditor5 48.x 属 premium，仅由 ckeditor5-premium-features 导出
    // （其类 isPremiumPlugin=true），umbrella ckeditor5 包不导出。请改用
    // CustomPlugin.fromPremium("LineHeight") 并配置商业 license key。
    // 此处不再作为免费内置插件登记。

    // ==================== Lists ====================

    /** Ordered/Unordered lists */
    LIST("List", Category.LIST, "bulletedList", "numberedList"),

    /** To-do lists */
    TODO_LIST("TodoList", Category.LIST, "todoList"),

    // ==================== Links ====================

    /** Hyperlinks */
    LINK("Link", Category.LINK, "link"),

    /** Auto-detect links */
    AUTO_LINK("AutoLink", Category.LINK),

    // ==================== Images ====================

    /** Basic image support */
    IMAGE("Image", Category.IMAGE),

    /** Image toolbar */
    IMAGE_TOOLBAR("ImageToolbar", Category.IMAGE),

    /** Image captions */
    IMAGE_CAPTION("ImageCaption", Category.IMAGE),

    /** Image styles */
    IMAGE_STYLE("ImageStyle", Category.IMAGE),

    /** Image resizing */
    IMAGE_RESIZE("ImageResize", Category.IMAGE),

    /** Image upload */
    IMAGE_UPLOAD("ImageUpload", Category.IMAGE, "imageUpload"),

    /** Image insertion */
    IMAGE_INSERT("ImageInsert", Category.IMAGE, "insertImage"),

    /** Block images */
    IMAGE_BLOCK("ImageBlock", Category.IMAGE),

    /** Inline images */
    IMAGE_INLINE("ImageInline", Category.IMAGE),

    /** Image linking */
    LINK_IMAGE("LinkImage", Category.IMAGE, "linkImage"),

    /** Auto-detect images from URLs */
    AUTO_IMAGE("AutoImage", Category.IMAGE),

    // ==================== Upload Adapters ====================

    /** Base64 upload adapter */
    BASE64_UPLOAD_ADAPTER("Base64UploadAdapter", Category.UPLOAD),

    /** Simple upload adapter */
    SIMPLE_UPLOAD_ADAPTER("SimpleUploadAdapter", Category.UPLOAD),

    /**
     * CKFinder 文件管理集成（插件类本身免费，由 umbrella ckeditor5 导出）。
     * 需要配置 config.ckfinder.uploadUrl 与服务端 CKFinder（商业后端产品），
     * 因此在前端被归入"需配置插件"，自动全选时会被过滤，须显式配置后启用。
     */
    CKFINDER("CKFinder", Category.UPLOAD),

    /**
     * <b>Premium feature</b>（CKEditor 文档标为 premium，付费 CKBox 附加服务）：CKBox 文件管理与图片上传（toolbar
     * {@code ckbox}）。见 <a href="https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckbox.html">CKBox 文档</a>（含 14 天试用）。
     * <p>插件类由开源的 {@code ckeditor5} 包导出（与免费功能同一个包），不属于 {@code ckeditor5-premium-features}，
     * 所以用本常量而不是 {@code CustomPlugin.fromPremium("CKBox")}。运行还需要：CKBox 服务的
     * {@code ckbox.tokenUrl}（或 {@code cloudServices.tokenUrl}）；以及 CKBox JS 库——页面上的
     * {@code https://cdn.ckbox.io/ckbox/<版本>/ckbox.js} 或 npm 包 {@code ckbox}。插件以 {@code window.CKBox} 判断库是否已加载，
     * 未加载时不注册 {@code ckbox} 命令与按钮。缺配置会报错，故与 CKFinder 一样归入"需配置插件"；会自动带上
     * CloudServices。{@link #isPremiumFeature()} 为 true。</p>
     */
    CKBOX("CKBox", Category.UPLOAD, "ckbox"),

    /**
     * <b>Premium feature</b>（CKBox 附加服务）：CKBox 图片编辑（toolbar {@code ckboxImageEdit}），依赖 {@link #CKBOX}；
     * 要求同 {@link #CKBOX}（见 <a href="https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckbox.html">CKBox 文档</a>）。
     * 由开源的 {@code ckeditor5} 包导出，不属于 {@code ckeditor5-premium-features}。
     */
    CKBOX_IMAGE_EDIT("CKBoxImageEdit", Category.UPLOAD, "ckboxImageEdit"),

    // ==================== Tables ====================

    /** Basic table support */
    TABLE("Table", Category.TABLE, "insertTable"),

    /** Table toolbar */
    TABLE_TOOLBAR("TableToolbar", Category.TABLE),

    /** Table properties */
    TABLE_PROPERTIES("TableProperties", Category.TABLE),

    /** Table cell properties */
    TABLE_CELL_PROPERTIES("TableCellProperties", Category.TABLE),

    /** Table captions */
    TABLE_CAPTION("TableCaption", Category.TABLE),

    /** Table column resizing */
    TABLE_COLUMN_RESIZE("TableColumnResize", Category.TABLE),

    /**
     * <b>Premium feature</b>（CKEditor 文档与定价标为 premium，"Layout Tables"）：预设表格布局（toolbar
     * {@code insertTableLayout}）。见 <a href="https://ckeditor.com/docs/ckeditor5/latest/features/tables/layout-tables.html">Layout tables 文档</a>。
     * <p>由开源的 {@code ckeditor5} 包导出（与免费功能同一个包），不属于 {@code ckeditor5-premium-features}。
     * CKEditor 内核对 {@code 'GPL'} key 跳过插件 license 检查，所以它在 GPL 下可以运行；商业使用需要 CKEditor
     * 标明的 plan（见 {@link #isPremiumFeature()}）。{@link #isPremiumFeature()} 为 true。</p>
     */
    TABLE_LAYOUT("TableLayout", Category.TABLE, "insertTableLayout"),

    /** 输出纯净表格 HTML（去除多余样式，Email 场景必需） */
    PLAIN_TABLE_OUTPUT("PlainTableOutput", Category.TABLE),

    // ==================== Media ====================

    /** Embed media from URLs */
    MEDIA_EMBED("MediaEmbed", Category.MEDIA, "mediaEmbed"),

    /** 粘贴链接自动转嵌入媒体（免费，不被 MediaEmbed 自动加载，需显式启用） */
    AUTO_MEDIA_EMBED("AutoMediaEmbed", Category.MEDIA),

    /** 嵌入媒体对齐/排版样式（免费，提供 mediaEmbed:alignLeft/Center/Right 等按钮） */
    MEDIA_EMBED_STYLE("MediaEmbedStyle", Category.MEDIA,
        "mediaEmbed:alignLeft", "mediaEmbed:alignCenter", "mediaEmbed:alignRight"),

    /** 选中嵌入媒体时的浮动工具栏（免费，承载 style/对齐按钮） */
    MEDIA_EMBED_TOOLBAR("MediaEmbedToolbar", Category.MEDIA),

    /** Embed raw HTML */
    HTML_EMBED("HtmlEmbed", Category.MEDIA, "htmlEmbed"),

    // ==================== Code ====================

    /** Code blocks */
    CODE_BLOCK("CodeBlock", Category.CODE, "codeBlock"),

    // ==================== Special Content ====================

    /** Horizontal lines */
    HORIZONTAL_LINE("HorizontalLine", Category.SPECIAL, "horizontalLine"),

    /** Page breaks */
    PAGE_BREAK("PageBreak", Category.SPECIAL, "pageBreak"),

    /** Special characters */
    SPECIAL_CHARACTERS("SpecialCharacters", Category.SPECIAL, "specialCharacters"),

    /** Essential special character sets */
    SPECIAL_CHARACTERS_ESSENTIALS("SpecialCharactersEssentials", Category.SPECIAL),

    // ==================== Editing Enhancements ====================

    /** Markdown-like autoformatting */
    AUTOFORMAT("Autoformat", Category.EDITING),

    /** Text transformations */
    TEXT_TRANSFORMATION("TextTransformation", Category.EDITING),

    /** Find and replace */
    FIND_AND_REPLACE("FindAndReplace", Category.EDITING, "findAndReplace"),

    /** Remove formatting */
    REMOVE_FORMAT("RemoveFormat", Category.EDITING, "removeFormat"),

    /** Source code editing */
    SOURCE_EDITING("SourceEditing", Category.EDITING, "sourceEditing"),

    /** Show block elements */
    SHOW_BLOCKS("ShowBlocks", Category.EDITING, "showBlocks"),

    /** Text highlighting */
    HIGHLIGHT("Highlight", Category.EDITING, "highlight"),

    // ==================== Mentions ====================

    /** @mentions */
    MENTION("Mention", Category.MENTION),

    // ==================== Document Features ====================

    /** Autosave functionality */
    AUTOSAVE("Autosave", Category.DOCUMENT),

    /** Word/character count */
    WORD_COUNT("WordCount", Category.DOCUMENT),

    /** Document title placeholder */
    TITLE("Title", Category.DOCUMENT),

    /** Paste from Office */
    PASTE_FROM_OFFICE("PasteFromOffice", Category.DOCUMENT),

    // ==================== HTML Support ====================

    /** General HTML support */
    GENERAL_HTML_SUPPORT("GeneralHtmlSupport", Category.HTML),

    /** HTML comments */
    HTML_COMMENT("HtmlComment", Category.HTML),

    /** Style attribute support - applies CSS classes to elements */
    STYLE("Style", Category.HTML, "style"),

    // ==================== Restricted Editing ====================

    /** Restricted editing mode */
    RESTRICTED_EDITING_MODE("RestrictedEditingMode", Category.RESTRICTED, "restrictedEditing"),

    /** Standard editing mode */
    STANDARD_EDITING_MODE("StandardEditingMode", Category.RESTRICTED, "restrictedEditingException"),

    // ==================== Bookmark ====================

    /** Anchor bookmarks for document navigation */
    BOOKMARK("Bookmark", Category.SPECIAL, "bookmark"),

    // ==================== Fullscreen ====================

    /** Full-screen editing mode */
    FULLSCREEN("Fullscreen", Category.EDITING, "fullscreen"),

    /** Balloon toolbar — 选中文本时自动显示的浮动工具栏 */
    BALLOON_TOOLBAR("BalloonToolbar", Category.EDITING),

    /** 块级工具栏（Notion 风格，每个段落左侧显示 + 按钮） */
    BLOCK_TOOLBAR("BlockToolbar", Category.EDITING),

    // ==================== Markdown ====================

    /** Markdown GFM data processor */
    MARKDOWN("Markdown", Category.DOCUMENT),

    /** Paste from Markdown (experimental) */
    PASTE_FROM_MARKDOWN_EXPERIMENTAL("PasteFromMarkdownExperimental", Category.DOCUMENT),

    // ==================== List Extensions ====================

    /** List start number, reversed, style type */
    LIST_PROPERTIES("ListProperties", Category.LIST),

    /** Bold/italic/font in list markers */
    LIST_FORMATTING("ListFormatting", Category.LIST),

    /** Merge adjacent lists */
    ADJACENT_LISTS_SUPPORT("AdjacentListsSupport", Category.LIST),

    // ==================== Special Characters Categories ====================

    /** Arrow symbols */
    SPECIAL_CHARACTERS_ARROWS("SpecialCharactersArrows", Category.SPECIAL),

    /** Currency symbols */
    SPECIAL_CHARACTERS_CURRENCY("SpecialCharactersCurrency", Category.SPECIAL),

    /** Latin extended characters */
    SPECIAL_CHARACTERS_LATIN("SpecialCharactersLatin", Category.SPECIAL),

    /** Mathematical symbols */
    SPECIAL_CHARACTERS_MATHEMATICAL("SpecialCharactersMathematical", Category.SPECIAL),

    /** Text symbols */
    SPECIAL_CHARACTERS_TEXT("SpecialCharactersText", Category.SPECIAL),

    // ==================== Language ====================

    /** Multi-language text markup */
    TEXT_PART_LANGUAGE("TextPartLanguage", Category.EDITING, "textPartLanguage"),

    // ==================== Cloud Services ====================

    /** CKEditor Cloud Services integration */
    CLOUD_SERVICES("CloudServices", Category.UPLOAD),

    /** Cloud Services core */
    CLOUD_SERVICES_CORE("CloudServicesCore", Category.UPLOAD),

    /** Cloud Services upload adapter */
    CLOUD_SERVICES_UPLOAD_ADAPTER("CloudServicesUploadAdapter", Category.UPLOAD),

    // ==================== Easy Image ====================

    /** Cloud-based image upload (requires CloudServices) */
    EASY_IMAGE("EasyImage", Category.IMAGE),

    // ==================== Minimap ====================

    /** Content minimap for document navigation */
    MINIMAP("Minimap", Category.EDITING),

    // ==================== Widget Framework ====================

    /** Widget framework base */
    WIDGET("Widget", Category.CORE),

    /** Widget toolbar management */
    WIDGET_TOOLBAR_REPOSITORY("WidgetToolbarRepository", Category.CORE),

    /** Widget resizing */
    WIDGET_RESIZE("WidgetResize", Category.CORE),

    /** Widget type around navigation */
    WIDGET_TYPE_AROUND("WidgetTypeAround", Category.CORE),

    // ==================== Emoji ====================

    /** Emoji picker and insertion */
    EMOJI("Emoji", Category.SPECIAL, "emoji"),

    /** Visual emoji picker UI */
    EMOJI_PICKER("EmojiPicker", Category.SPECIAL);

    /**
     * Plugin categories for organization
     */
    public enum Category {
        CORE("Core"),
        BASIC_STYLES("Basic Styles"),
        FONT("Font"),
        PARAGRAPH("Paragraph"),
        LIST("List"),
        LINK("Link"),
        IMAGE("Image"),
        UPLOAD("Upload"),
        TABLE("Table"),
        MEDIA("Media"),
        CODE("Code"),
        SPECIAL("Special Content"),
        EDITING("Editing"),
        MENTION("Mention"),
        DOCUMENT("Document"),
        HTML("HTML Support"),
        RESTRICTED("Restricted Editing"),
        CUSTOM("Custom");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String jsName;
    private final Category category;
    private final Set<String> toolbarItems;

    CKEditorPlugin(String jsName, Category category, String... toolbarItems) {
        this.jsName = jsName;
        this.category = category;
        this.toolbarItems = toolbarItems.length > 0
            ? Collections.unmodifiableSet(new HashSet<>(Arrays.asList(toolbarItems)))
            : Collections.emptySet();
    }

    /**
     * Get the JavaScript plugin name
     */
    public String getJsName() {
        return jsName;
    }

    /**
     * Get the plugin category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Get toolbar items provided by this plugin
     */
    public Set<String> getToolbarItems() {
        return toolbarItems;
    }

    /**
     * 是否经 {@code ckeditor5-premium-features} 包加载（<b>加载通道</b>）。
     * <p>本枚举全部由开源的 {@code ckeditor5} 包导出，因此恒为 false；它不表示功能是否免费——CKEditor 把其中一部分
     * 标为 premium feature，见 {@link #isPremiumFeature()}。{@code ckeditor5-premium-features} 里的插件请用
     * {@link CustomPlugin#fromPremium(String)}。命名保留以兼容既有调用方。
     * @return 内置插件恒为 false
     */
    public boolean isPremium() {
        return false;
    }

    /**
     * CKEditor 文档标为 premium feature 的插件（全部由开源 ckeditor5 包导出）：带 {@code licenseFeatureCode} 的插件
     * （与 ckeditor5 49.1.0 一致，由 premium-catalogue.test.ts 对照真实包校验），以及付费服务 CKBox 与 CKFinder。
     */
    private static final Set<CKEditorPlugin> PREMIUM_FEATURES = Collections.unmodifiableSet(EnumSet.of(
        BOOKMARK, FIND_AND_REPLACE, HTML_EMBED, IMAGE_RESIZE, LIST_PROPERTIES, PAGE_BREAK, PASTE_FROM_OFFICE,
        REMOVE_FORMAT, RESTRICTED_EDITING_MODE, SPECIAL_CHARACTERS, TABLE_PROPERTIES, TABLE_CELL_PROPERTIES,
        TABLE_COLUMN_RESIZE, TABLE_LAYOUT, BASE64_UPLOAD_ADAPTER, SIMPLE_UPLOAD_ADAPTER, WORD_COUNT,
        CKBOX, CKBOX_IMAGE_EDIT, CKFINDER));

    /**
     * Whether CKEditor's documentation and pricing mark this plugin as a <b>premium feature</b>, although the
     * open-source {@code ckeditor5} package exports it. Two facts apply, and this method states no legal conclusion:
     * <ul>
     *   <li>CKEditor's core skips plugin licence checks for the {@code 'GPL'} licence key (the add-on's default), so
     *       these plugins run under GPL. Plugins with a licence feature code are blocked only for commercial keys
     *       whose plan does not include them.</li>
     *   <li>CKEditor's docs badge them "Premium feature" and its pricing lists them outside the Free plan; commercial
     *       use needs the plan CKEditor states. CKBox ({@link #CKBOX}, {@link #CKBOX_IMAGE_EDIT}) is a paid
     *       add-on service, and CKFinder ({@link #CKFINDER}) needs the commercial CKFinder server.</li>
     * </ul>
     * See {@code docs/PREMIUM_INTEGRATION.md} ("Licensing for self-hosted (npm) builds") and
     * <a href="https://ckeditor.com/docs/ckeditor5/latest/getting-started/licensing/license-key-and-activation.html">License key and activation</a>.
     * {@link #isPremium()} is the loading channel (always false here), not this.
     *
     * @return true for a premium feature per CKEditor's documentation
     * @since 6.2.0
     */
    public boolean isPremiumFeature() {
        return PREMIUM_FEATURES.contains(this);
    }

    /**
     * @return the plugins for which {@link #isPremiumFeature()} is true
     * @since 6.2.0
     */
    public static Set<CKEditorPlugin> getPremiumFeaturePlugins() {
        return PREMIUM_FEATURES;
    }

    /**
     * Get all plugins in a category
     */
    public static Set<CKEditorPlugin> getByCategory(Category category) {
        Set<CKEditorPlugin> result = new HashSet<>();
        for (CKEditorPlugin plugin : values()) {
            if (plugin.category == category) {
                result.add(plugin);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * Pre-built jsName to plugin lookup map for O(1) lookup.
     */
    private static final Map<String, CKEditorPlugin> JS_NAME_MAP;
    static {
        Map<String, CKEditorPlugin> map = new HashMap<>();
        for (CKEditorPlugin plugin : values()) {
            map.put(plugin.jsName, plugin);
        }
        JS_NAME_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Find plugin by JavaScript name
     *
     * @param jsName JavaScript plugin name
     * @return the corresponding plugin, or null if not found
     */
    public static CKEditorPlugin fromJsName(String jsName) {
        return JS_NAME_MAP.get(jsName);
    }
}
