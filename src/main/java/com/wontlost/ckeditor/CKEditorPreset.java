package com.wontlost.ckeditor;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * CKEditor preset configurations.
 * Provides common plugin combinations for quick setup.
 *
 * <p>Usage example:</p>
 * <pre>
 * // Use a preset
 * VaadinCKEditor.create()
 *     .withPreset(CKEditorPreset.STANDARD)
 *     .build();
 *
 * // Customize based on preset
 * VaadinCKEditor.create()
 *     .withPreset(CKEditorPreset.BASIC)
 *     .addPlugin(CKEditorPlugin.TABLE)
 *     .build();
 * </pre>
 */
public enum CKEditorPreset {

    /**
     * Basic preset (~300KB)
     * Suitable for simple text input, comments
     */
    BASIC(
        "Basic Editor",
        new CKEditorPlugin[] {
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.LINK,
            CKEditorPlugin.LIST,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.UNDO
        },
        new String[] {
            "undo", "redo", "|",
            "bold", "italic", "underline", "|",
            "link", "|",
            "bulletedList", "numberedList", "|",
            "blockQuote"
        }
    ),

    /**
     * Standard preset (~600KB)
     * Suitable for general document editing, blogs
     */
    STANDARD(
        "Standard Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.UNDO,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.CODE,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.ALIGNMENT,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists
            CKEditorPlugin.LIST,
            CKEditorPlugin.TODO_LIST,
            // Links
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            // Images
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            // Media
            CKEditorPlugin.MEDIA_EMBED,
            // Special
            CKEditorPlugin.HORIZONTAL_LINE,
            // Editing
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.FIND_AND_REPLACE,
            CKEditorPlugin.PASTE_FROM_OFFICE
        },
        new String[] {
            "undo", "redo", "|",
            "heading", "|",
            "bold", "italic", "underline", "strikethrough", "code", "|",
            "link", "insertImage", "insertTable", "mediaEmbed", "|",
            "bulletedList", "numberedList", "todoList", "|",
            "alignment", "outdent", "indent", "|",
            "blockQuote", "horizontalLine", "|",
            "findAndReplace"
        }
    ),

    /**
     * Full preset (~700KB)
     * Suitable for complete document processing
     * Optimized for fast loading (~40 plugins)
     */
    FULL(
        "Full Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.UNDO,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.CODE,
            CKEditorPlugin.SUPERSCRIPT,
            CKEditorPlugin.SUBSCRIPT,
            // Font
            CKEditorPlugin.FONT_SIZE,
            CKEditorPlugin.FONT_FAMILY,
            CKEditorPlugin.FONT_COLOR,
            CKEditorPlugin.FONT_BACKGROUND_COLOR,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.ALIGNMENT,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists
            CKEditorPlugin.LIST,
            CKEditorPlugin.TODO_LIST,
            // Links
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            // Images (simplified)
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables (simplified)
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            // Media
            CKEditorPlugin.MEDIA_EMBED,
            // Code
            CKEditorPlugin.CODE_BLOCK,
            // Special
            CKEditorPlugin.HORIZONTAL_LINE,
            // Editing
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.FIND_AND_REPLACE,
            CKEditorPlugin.REMOVE_FORMAT,
            CKEditorPlugin.HIGHLIGHT,
            // Document
            CKEditorPlugin.PASTE_FROM_OFFICE
        },
        new String[] {
            "undo", "redo", "|",
            "heading", "|",
            "fontFamily", "fontSize", "fontColor", "fontBackgroundColor", "|",
            "bold", "italic", "underline", "strikethrough", "code", "subscript", "superscript", "|",
            "removeFormat", "|",
            "link", "insertImage", "insertTable", "mediaEmbed", "|",
            "bulletedList", "numberedList", "todoList", "|",
            "alignment", "outdent", "indent", "|",
            "blockQuote", "codeBlock", "horizontalLine", "|",
            "highlight", "|",
            "findAndReplace"
        }
    ),

    /**
     * Document preset (~800KB)
     * Suitable for professional document editing
     */
    DOCUMENT(
        "Document Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.UNDO,
            CKEditorPlugin.CLIPBOARD,
            CKEditorPlugin.SELECT_ALL,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.SUPERSCRIPT,
            CKEditorPlugin.SUBSCRIPT,
            // Font
            CKEditorPlugin.FONT_SIZE,
            CKEditorPlugin.FONT_FAMILY,
            CKEditorPlugin.FONT_COLOR,
            CKEditorPlugin.FONT_BACKGROUND_COLOR,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.ALIGNMENT,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists
            CKEditorPlugin.LIST,
            CKEditorPlugin.TODO_LIST,
            // Links
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            // Images
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            CKEditorPlugin.TABLE_PROPERTIES,
            CKEditorPlugin.TABLE_CELL_PROPERTIES,
            // Special
            CKEditorPlugin.HORIZONTAL_LINE,
            CKEditorPlugin.PAGE_BREAK,
            // Editing
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.FIND_AND_REPLACE,
            CKEditorPlugin.REMOVE_FORMAT,
            // Document
            CKEditorPlugin.WORD_COUNT,
            CKEditorPlugin.TITLE,
            CKEditorPlugin.PASTE_FROM_OFFICE,
            CKEditorPlugin.AUTOSAVE
        },
        new String[] {
            "undo", "redo", "|",
            "heading", "|",
            "fontFamily", "fontSize", "|",
            "bold", "italic", "underline", "|",
            "fontColor", "fontBackgroundColor", "|",
            "link", "insertImage", "insertTable", "|",
            "bulletedList", "numberedList", "|",
            "alignment", "outdent", "indent", "|",
            "pageBreak", "|",
            "findAndReplace"
        }
    ),

    /**
     * Collaborative preset (requires Premium license)
     * Base configuration for collaborative editing with Comments, Track Changes, and Revision History.
     *
     * <p><b>Important:</b> This preset only includes the base plugins.
     * You must add the collaboration plugins separately using {@code CustomPlugin.fromPremium()}:</p>
     *
     * <pre>
     * // Asynchronous collaboration (Comments, Track Changes)
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withPreset(CKEditorPreset.COLLABORATIVE)
     *     .withLicenseKey("your-license-key")
     *     .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("RevisionHistory"))
     *     .build();
     *
     * // Real-time collaboration (requires CKEditor Cloud Services)
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withPreset(CKEditorPreset.COLLABORATIVE)
     *     .withLicenseKey("your-license-key")
     *     .addCustomPlugin(CustomPlugin.fromPremium("RealTimeCollaborativeEditing"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("RealTimeCollaborativeComments"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("PresenceList"))
     *     .withConfig(config -&gt; config
     *         .set("cloudServices.tokenUrl", "https://your-server/token")
     *         .set("collaboration.channelId", "document-123")
     *     )
     *     .build();
     * </pre>
     *
     * <p>Available collaboration plugins:</p>
     * <ul>
     *   <li><b>Comments</b> - Add comments to document fragments</li>
     *   <li><b>TrackChanges</b> - Track all changes as suggestions</li>
     *   <li><b>RevisionHistory</b> - Document version control</li>
     *   <li><b>RealTimeCollaborativeEditing</b> - Real-time co-editing</li>
     *   <li><b>RealTimeCollaborativeComments</b> - Real-time comments sync</li>
     *   <li><b>RealTimeCollaborativeTrackChanges</b> - Real-time track changes sync</li>
     *   <li><b>RealTimeCollaborativeRevisionHistory</b> - Real-time revision sync</li>
     *   <li><b>PresenceList</b> - Show connected users</li>
     * </ul>
     *
     * @see <a href="https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/collaboration.html">
     *     CKEditor 5 Collaboration Documentation</a>
     */
    COLLABORATIVE(
        "Collaborative Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.UNDO,
            CKEditorPlugin.CLIPBOARD,
            CKEditorPlugin.SELECT_ALL,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.SUPERSCRIPT,
            CKEditorPlugin.SUBSCRIPT,
            // Font
            CKEditorPlugin.FONT_SIZE,
            CKEditorPlugin.FONT_FAMILY,
            CKEditorPlugin.FONT_COLOR,
            CKEditorPlugin.FONT_BACKGROUND_COLOR,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.ALIGNMENT,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists
            CKEditorPlugin.LIST,
            CKEditorPlugin.TODO_LIST,
            // Links
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            // Images
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            CKEditorPlugin.TABLE_PROPERTIES,
            CKEditorPlugin.TABLE_CELL_PROPERTIES,
            // Special
            CKEditorPlugin.HORIZONTAL_LINE,
            CKEditorPlugin.PAGE_BREAK,
            // Editing
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.FIND_AND_REPLACE,
            CKEditorPlugin.REMOVE_FORMAT,
            // Document
            CKEditorPlugin.WORD_COUNT,
            CKEditorPlugin.PASTE_FROM_OFFICE,
            CKEditorPlugin.AUTOSAVE
        },
        // Toolbar with placeholders for collaboration buttons
        // Users should add: "comment", "trackChanges", "revisionHistory"
        new String[] {
            "undo", "redo", "|",
            "heading", "|",
            "fontFamily", "fontSize", "|",
            "bold", "italic", "underline", "|",
            "fontColor", "fontBackgroundColor", "|",
            "link", "insertImage", "insertTable", "|",
            "bulletedList", "numberedList", "todoList", "|",
            "alignment", "outdent", "indent", "|",
            "pageBreak", "|",
            "findAndReplace"
        }
    ),

    /**
     * AI Document preset (~900KB, requires Premium license)
     * Full document editor with AI-powered writing assistant.
     *
     * <p>This preset includes all document editing plugins plus AI integration.
     * AI plugins must be added separately using {@code CustomPlugin.fromPremium()}:</p>
     *
     * <pre>
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withPreset(CKEditorPreset.AI_DOCUMENT)
     *     .withLicenseKey("your-license-key")
     *     .addCustomPlugin(CustomPlugin.fromPremium("AIChat"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("AIEditorIntegration"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("AIQuickActions"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("AIReviewMode"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("AITranslate"))
     *     .build();
     * </pre>
     */
    AI_DOCUMENT(
        "AI Document Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.UNDO,
            CKEditorPlugin.CLIPBOARD,
            CKEditorPlugin.SELECT_ALL,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.CODE,
            CKEditorPlugin.SUPERSCRIPT,
            CKEditorPlugin.SUBSCRIPT,
            // Font
            CKEditorPlugin.FONT_SIZE,
            CKEditorPlugin.FONT_FAMILY,
            CKEditorPlugin.FONT_COLOR,
            CKEditorPlugin.FONT_BACKGROUND_COLOR,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.ALIGNMENT,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists（含 ListProperties）
            CKEditorPlugin.LIST,
            CKEditorPlugin.LIST_PROPERTIES,
            CKEditorPlugin.TODO_LIST,
            // Links（含 LinkImage）
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            CKEditorPlugin.LINK_IMAGE,
            // Images（含 ImageBlock, AutoImage）
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_BLOCK,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.AUTO_IMAGE,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables（含 TableCaption, TableColumnResize）
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            CKEditorPlugin.TABLE_PROPERTIES,
            CKEditorPlugin.TABLE_CELL_PROPERTIES,
            CKEditorPlugin.TABLE_CAPTION,
            CKEditorPlugin.TABLE_COLUMN_RESIZE,
            // Media
            CKEditorPlugin.MEDIA_EMBED,
            // Code
            CKEditorPlugin.CODE_BLOCK,
            // Special（含 Bookmark, Emoji, SpecialCharacters 全系列）
            CKEditorPlugin.HORIZONTAL_LINE,
            CKEditorPlugin.BOOKMARK,
            CKEditorPlugin.EMOJI,
            CKEditorPlugin.SPECIAL_CHARACTERS,
            CKEditorPlugin.SPECIAL_CHARACTERS_ESSENTIALS,
            CKEditorPlugin.SPECIAL_CHARACTERS_ARROWS,
            CKEditorPlugin.SPECIAL_CHARACTERS_CURRENCY,
            CKEditorPlugin.SPECIAL_CHARACTERS_LATIN,
            CKEditorPlugin.SPECIAL_CHARACTERS_MATHEMATICAL,
            CKEditorPlugin.SPECIAL_CHARACTERS_TEXT,
            // Editing（含 Fullscreen, TextTransformation）
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.TEXT_TRANSFORMATION,
            CKEditorPlugin.FIND_AND_REPLACE,
            CKEditorPlugin.REMOVE_FORMAT,
            CKEditorPlugin.FULLSCREEN,
            // Mention
            CKEditorPlugin.MENTION,
            // Balloon toolbar
            CKEditorPlugin.BALLOON_TOOLBAR,
            // Document
            CKEditorPlugin.AUTOSAVE,
            CKEditorPlugin.PASTE_FROM_OFFICE,
            // Cloud Services（AI 需要）
            CKEditorPlugin.CLOUD_SERVICES
        },
        new String[] {
            "undo", "redo", "|",
            "findAndReplace", "fullscreen", "|",
            "heading", "|",
            "fontSize", "fontFamily", "fontColor", "fontBackgroundColor", "|",
            "bold", "italic", "underline", "strikethrough",
            "subscript", "superscript", "code", "removeFormat", "|",
            "emoji", "specialCharacters", "horizontalLine",
            "link", "bookmark", "insertImage", "mediaEmbed",
            "insertTable", "blockQuote", "codeBlock", "|",
            "alignment", "|",
            "bulletedList", "numberedList", "todoList",
            "outdent", "indent"
        }
    ),

    /**
     * Email preset (~500KB, requires Premium license)
     * Optimized for composing professional emails and email templates.
     *
     * <p>Email-specific features must be added via premium plugins.
     * When adding premium plugins, also add their corresponding toolbar items:</p>
     * <pre>
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withPreset(CKEditorPreset.EMAIL)
     *     .withLicenseKey("your-license-key")
     *     .addCustomPlugin(CustomPlugin.fromPremium("EmailConfigurationHelper"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("ExportInlineStyles"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("SourceEditingEnhanced"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("MergeFields"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("Template"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("PasteFromOfficeEnhanced"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("TableLayout"))
     *     // Add premium toolbar items:
     *     .withToolbar("undo", "redo", "|",
     *         "insertMergeField", "previewMergeFields", "|",
     *         "sourceEditingEnhanced", "|",
     *         "heading", "style", "|",
     *         "fontSize", "fontFamily", "fontColor", "fontBackgroundColor", "|",
     *         "bold", "italic", "underline", "|",
     *         "link", "insertImage", "insertTable", "insertTableLayout", "|",
     *         "alignment", "|",
     *         "bulletedList", "numberedList", "outdent", "indent")
     *     .build();
     * </pre>
     */
    EMAIL(
        "Email Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.TEXT_TRANSFORMATION,
            CKEditorPlugin.AUTOSAVE,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.REMOVE_FORMAT,
            // Font
            CKEditorPlugin.FONT_SIZE,
            CKEditorPlugin.FONT_FAMILY,
            CKEditorPlugin.FONT_COLOR,
            CKEditorPlugin.FONT_BACKGROUND_COLOR,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.ALIGNMENT,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists
            CKEditorPlugin.LIST,
            CKEditorPlugin.LIST_PROPERTIES,
            // Links
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            // Images（inline only for email）
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_INLINE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.AUTO_IMAGE,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables（含 PlainTableOutput, ColumnResize；TableLayout 为 premium 需单独添加）
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            CKEditorPlugin.TABLE_COLUMN_RESIZE,
            CKEditorPlugin.TABLE_CAPTION,
            CKEditorPlugin.TABLE_CELL_PROPERTIES,
            CKEditorPlugin.TABLE_PROPERTIES,
            CKEditorPlugin.PLAIN_TABLE_OUTPUT,
            // Special
            CKEditorPlugin.EMOJI,
            // HTML support（邮件需要 GeneralHtmlSupport + Style）
            CKEditorPlugin.GENERAL_HTML_SUPPORT,
            CKEditorPlugin.STYLE,
            // Mention
            CKEditorPlugin.MENTION,
            // Document
            CKEditorPlugin.PASTE_FROM_OFFICE
        },
        new String[] {
            "undo", "redo", "|",
            "heading", "style", "|",
            "fontSize", "fontFamily", "fontColor", "fontBackgroundColor", "|",
            "bold", "italic", "underline", "|",
            "link", "insertImage", "insertTable", "|",
            "alignment", "|",
            "bulletedList", "numberedList", "outdent", "indent"
        }
    ),

    /**
     * Notion-like preset (~900KB, requires Premium license)
     * Block-based editor with collaborative features, slash commands, and balloon toolbar.
     *
     * <p>Collaboration and productivity plugins must be added separately.
     * When adding premium plugins, also add their corresponding toolbar items:</p>
     * <pre>
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withPreset(CKEditorPreset.NOTION)
     *     .withLicenseKey("your-license-key")
     *     .addCustomPlugin(CustomPlugin.fromPremium("SlashCommand"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("CaseChange"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("Template"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("TableOfContents"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("ExportWord"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("ImportWord"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("PasteFromOfficeEnhanced"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
     *     .addCustomPlugin(CustomPlugin.fromPremium("RevisionHistory"))
     *     // Add premium toolbar items:
     *     .withToolbar("undo", "redo", "|",
     *         "revisionHistory", "trackChanges", "comment", "commentsArchive", "|",
     *         "importWord", "exportWord", "exportPdf", "caseChange", "findAndReplace", "|",
     *         "heading", "|",
     *         "bold", "italic", "underline", "strikethrough", "code", "|",
     *         "emoji", "specialCharacters", "horizontalLine",
     *         "link", "insertImage", "mediaEmbed",
     *         "insertTable", "tableOfContents", "insertTemplate",
     *         "highlight", "blockQuote", "codeBlock", "|",
     *         "bulletedList", "numberedList", "todoList",
     *         "outdent", "indent")
     *     .build();
     * </pre>
     */
    NOTION(
        "Notion-like Editor",
        new CKEditorPlugin[] {
            // Core
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.AUTOFORMAT,
            CKEditorPlugin.TEXT_TRANSFORMATION,
            CKEditorPlugin.AUTOSAVE,
            // Basic styles
            CKEditorPlugin.BOLD,
            CKEditorPlugin.ITALIC,
            CKEditorPlugin.UNDERLINE,
            CKEditorPlugin.STRIKETHROUGH,
            CKEditorPlugin.CODE,
            // Paragraph
            CKEditorPlugin.HEADING,
            CKEditorPlugin.BLOCK_QUOTE,
            CKEditorPlugin.INDENT,
            CKEditorPlugin.INDENT_BLOCK,
            // Lists
            CKEditorPlugin.LIST,
            CKEditorPlugin.LIST_PROPERTIES,
            CKEditorPlugin.TODO_LIST,
            // Links
            CKEditorPlugin.LINK,
            CKEditorPlugin.AUTO_LINK,
            CKEditorPlugin.LINK_IMAGE,
            // Images（block only for Notion style）
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_BLOCK,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.AUTO_IMAGE,
            CKEditorPlugin.BASE64_UPLOAD_ADAPTER,
            // Tables
            CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_TOOLBAR,
            // Media
            CKEditorPlugin.MEDIA_EMBED,
            // Code
            CKEditorPlugin.CODE_BLOCK,
            // Special
            CKEditorPlugin.HORIZONTAL_LINE,
            CKEditorPlugin.EMOJI,
            CKEditorPlugin.SPECIAL_CHARACTERS,
            CKEditorPlugin.SPECIAL_CHARACTERS_ESSENTIALS,
            CKEditorPlugin.SPECIAL_CHARACTERS_ARROWS,
            CKEditorPlugin.SPECIAL_CHARACTERS_CURRENCY,
            CKEditorPlugin.SPECIAL_CHARACTERS_LATIN,
            CKEditorPlugin.SPECIAL_CHARACTERS_MATHEMATICAL,
            CKEditorPlugin.SPECIAL_CHARACTERS_TEXT,
            // Editing
            CKEditorPlugin.FIND_AND_REPLACE,
            CKEditorPlugin.HIGHLIGHT,
            // Mention
            CKEditorPlugin.MENTION,
            // Document
            CKEditorPlugin.TITLE,
            CKEditorPlugin.PASTE_FROM_OFFICE,
            // Block toolbar（Notion 风格块级工具栏）
            CKEditorPlugin.BLOCK_TOOLBAR
        },
        new String[] {
            "undo", "redo", "|",
            "findAndReplace", "|",
            "heading", "|",
            "bold", "italic", "underline", "strikethrough", "code", "|",
            "emoji", "specialCharacters", "horizontalLine",
            "link", "insertImage", "mediaEmbed",
            "insertTable",
            "highlight", "blockQuote", "codeBlock", "|",
            "bulletedList", "numberedList", "todoList",
            "outdent", "indent"
        }
    ),

    /**
     * Empty preset
     * For fully custom configuration
     */
    EMPTY(
        "Empty Editor",
        new CKEditorPlugin[] {
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH
        },
        new String[] {}
    );

    private final String displayName;
    private final Set<CKEditorPlugin> plugins;
    private final String[] defaultToolbar;

    CKEditorPreset(String displayName, CKEditorPlugin[] plugins, String[] defaultToolbar) {
        this.displayName = displayName;
        this.plugins = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(plugins)));
        this.defaultToolbar = defaultToolbar;
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get plugins included in this preset
     */
    public Set<CKEditorPlugin> getPlugins() {
        return plugins;
    }

    /**
     * Get default toolbar configuration
     */
    public String[] getDefaultToolbar() {
        return defaultToolbar.clone();
    }

    /**
     * Check if preset includes a specific plugin
     */
    public boolean hasPlugin(CKEditorPlugin plugin) {
        return plugins.contains(plugin);
    }

    /**
     * Get estimated bundle size (KB).
     * Note: These are approximate values based on typical builds.
     * Actual size depends on webpack/vite configuration and tree-shaking.
     */
    public int getEstimatedSize() {
        return switch (this) {
            case BASIC -> 300;
            case STANDARD -> 600;
            case FULL -> 700;
            case DOCUMENT -> 800;
            case COLLABORATIVE -> 850; // Base + premium plugins loaded dynamically
            case AI_DOCUMENT -> 900; // Document + AI premium plugins
            case EMAIL -> 500; // Email + premium email plugins
            case NOTION -> 900; // Notion-like + collaboration premium plugins
            case EMPTY -> 100;
        };
    }
}
