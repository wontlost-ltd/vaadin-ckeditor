package com.wontlost.ckeditor;

import java.util.Arrays;
import java.util.Collections;
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

    /** Line height */
    LINE_HEIGHT("LineHeight", Category.PARAGRAPH, "lineHeight"),

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

    // ==================== Media ====================

    /** Embed media from URLs */
    MEDIA_EMBED("MediaEmbed", Category.MEDIA, "mediaEmbed"),

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
     * Check if this is a premium feature (requires license).
     * Note: All built-in plugins are free. Premium features require
     * the ckeditor5-premium-features package and a license key.
     * Use CustomPlugin to load premium plugins.
     * @return always false for built-in plugins
     */
    public boolean isPremium() {
        return false;
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
