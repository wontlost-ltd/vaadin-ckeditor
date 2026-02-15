# Vaadin CKEditor 5 Addon - User Guide

Vaadin CKEditor 5 is a rich text editor component for Vaadin applications, providing seamless integration with CKEditor 5 and full support for Vaadin's theming system.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Editor Types](#editor-types)
- [Presets](#presets)
- [Plugins](#plugins)
- [Configuration](#configuration)
- [Toolbar Customization](#toolbar-customization)
- [Themes](#themes)
- [Events](#events)
- [File Upload](#file-upload)
- [Error Handling](#error-handling)
- [HTML Sanitization](#html-sanitization)
- [Fallback Modes](#fallback-modes)
- [Custom Plugins](#custom-plugins)
- [Premium Features](#premium-features)
- [API Reference](#api-reference)

---

## Installation

### Maven

```xml
<dependency>
    <groupId>com.wontlost</groupId>
    <artifactId>ckeditor-vaadin</artifactId>
    <version>5.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.wontlost:ckeditor-vaadin:5.1.0'
```

---

## Quick Start

### Basic Usage

```java
// Create editor with standard preset (shorthand - returns editor directly)
VaadinCKEditor editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);

// Add to layout
add(editor);

// Get content
String html = editor.getValue();

// Set content
editor.setValue("<p>Hello <strong>World</strong>!</p>");
```

### Builder Pattern

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withTheme(CKEditorTheme.AUTO)
    .withType(CKEditorType.CLASSIC)
    .withValue("<p>Initial content</p>")
    .withWidth("100%")
    .withHeight("400px")
    .build();
```

### View-only Mode

For displaying HTML content without editing capabilities:

```java
// One-liner convenience method (recommended)
VaadinCKEditor viewer = VaadinCKEditor.create()
    .withValue(htmlContent)
    .withViewOnly()  // Sets readOnly + hideToolbar + fallbackMode
    .build();
```

---

## Editor Types

| Type | Description | Best For |
|------|-------------|----------|
| `CLASSIC` | Fixed toolbar above content | Traditional document editing |
| `BALLOON` | Floating toolbar on text selection | Clean, minimal UI |
| `INLINE` | Edit directly in page, toolbar on selection | In-place editing |
| `DECOUPLED` | Separated toolbar and content areas | Custom layouts |

```java
// Classic editor (default)
VaadinCKEditor.create()
    .withType(CKEditorType.CLASSIC)
    .build();

// Balloon editor
VaadinCKEditor.create()
    .withType(CKEditorType.BALLOON)
    .build();

// Inline editor
VaadinCKEditor.create()
    .withType(CKEditorType.INLINE)
    .build();

// Decoupled editor (separate toolbar)
VaadinCKEditor.create()
    .withType(CKEditorType.DECOUPLED)
    .build();
```

---

## Presets

Presets provide pre-configured plugin sets for common use cases:

| Preset | Size | Description |
|--------|------|-------------|
| `EMPTY` | Minimal | No plugins, for custom configuration |
| `BASIC` | ~300KB | Text formatting, lists, links |
| `STANDARD` | ~600KB | Images, tables, media, find/replace |
| `FULL` | ~700KB | Fonts, colors, code blocks, highlighting |
| `DOCUMENT` | ~800KB | Professional document editing features |
| `EMAIL` | ~500KB | Email composition with image upload (Base64) |
| `NOTION` | ~900KB | Notion-style block editing with block toolbar |
| `COLLABORATIVE` | ~850KB | Base for collaboration features |

```java
// Simple text editor (shorthand - returns editor directly)
VaadinCKEditor basic = VaadinCKEditor.withPreset(CKEditorPreset.BASIC);

// Full-featured editor
VaadinCKEditor full = VaadinCKEditor.withPreset(CKEditorPreset.FULL);

// Start with preset, then customize (use builder pattern)
VaadinCKEditor custom = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .addPlugin(CKEditorPlugin.CODE_BLOCK)
    .removePlugin(CKEditorPlugin.MEDIA_EMBED)
    .build();
```

---

## Plugins

### Available Plugins

**Core:**
- `ESSENTIALS`, `PARAGRAPH`, `UNDO`, `CLIPBOARD`, `SELECT_ALL`

**Text Formatting:**
- `BOLD`, `ITALIC`, `UNDERLINE`, `STRIKETHROUGH`, `CODE`
- `SUPERSCRIPT`, `SUBSCRIPT`

**Font:**
- `FONT_SIZE`, `FONT_FAMILY`, `FONT_COLOR`, `FONT_BACKGROUND_COLOR`

**Paragraph:**
- `HEADING`, `ALIGNMENT`, `INDENT`, `INDENT_BLOCK`, `BLOCK_QUOTE`, `LINE_HEIGHT`

**Lists:**
- `LIST`, `TODO_LIST`, `LIST_PROPERTIES`

**Links:**
- `LINK`, `AUTO_LINK`

**Images:**
- `IMAGE`, `IMAGE_TOOLBAR`, `IMAGE_CAPTION`, `IMAGE_STYLE`
- `IMAGE_RESIZE`, `IMAGE_UPLOAD`, `IMAGE_INSERT`, `IMAGE_BLOCK`, `IMAGE_INLINE`

**Tables:**
- `TABLE`, `TABLE_TOOLBAR`, `TABLE_PROPERTIES`, `TABLE_CELL_PROPERTIES`
- `TABLE_CAPTION`, `TABLE_COLUMN_RESIZE`

**Media & Code:**
- `MEDIA_EMBED`, `HTML_EMBED`, `CODE_BLOCK`

**Special:**
- `HORIZONTAL_LINE`, `PAGE_BREAK`, `SPECIAL_CHARACTERS`
- `HIGHLIGHT`, `FIND_AND_REPLACE`, `SOURCE_EDITING`
- `WORD_COUNT`, `AUTOSAVE`, `FULLSCREEN`, `MINIMAP`

### Adding/Removing Plugins

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .addPlugin(CKEditorPlugin.CODE_BLOCK)
    .addPlugin(CKEditorPlugin.HIGHLIGHT)
    .removePlugin(CKEditorPlugin.MEDIA_EMBED)
    .build();
```

### Manual Plugin Selection

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPlugins(
        CKEditorPlugin.ESSENTIALS,
        CKEditorPlugin.PARAGRAPH,
        CKEditorPlugin.BOLD,
        CKEditorPlugin.ITALIC,
        CKEditorPlugin.LINK,
        CKEditorPlugin.LIST
    )
    .withToolbar("bold", "italic", "|", "link", "|", "bulletedList", "numberedList")
    .build();
```

### Dependency Resolution

The addon automatically resolves plugin dependencies:

```java
// AUTO_RESOLVE (default) - Automatically adds missing dependencies
VaadinCKEditor.create()
    .withDependencyMode(DependencyMode.AUTO_RESOLVE)
    .addPlugin(CKEditorPlugin.IMAGE_CAPTION) // IMAGE plugin added automatically
    .build();

// STRICT - Throws exception if dependencies missing
VaadinCKEditor.create()
    .withDependencyMode(DependencyMode.STRICT)
    .build();
```

---

## Configuration

### Using CKEditorConfig

```java
CKEditorConfig config = new CKEditorConfig()
    .setPlaceholder("Enter your content here...")
    .setLanguage("en")
    .setToolbar("bold", "italic", "link", "|", "bulletedList", "numberedList")
    .setFontSize("12px", "14px", "16px", "18px", "24px")
    .setFontFamily("Arial", "Georgia", "Times New Roman", "Courier New")
    .setHeading(
        HeadingOption.paragraph("Paragraph", "ck-heading_paragraph"),
        HeadingOption.heading(1, "Title", "ck-heading_heading1"),
        HeadingOption.heading(2, "Heading", "ck-heading_heading2"),
        HeadingOption.heading(3, "Subheading", "ck-heading_heading3")
    );

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withConfig(config)
    .build();
```

### Common Configuration Options

```java
CKEditorConfig config = new CKEditorConfig();

// Placeholder text
config.setPlaceholder("Start typing...");

// Language (UI and content)
config.setLanguage("zh-cn");
// Or separate UI and content languages
config.setLanguage("en", "zh-cn", new String[]{"de", "fr"});

// Font sizes
config.setFontSize("10px", "12px", "14px", "16px", "18px", "24px", "36px");
// Allow any font size input
config.setFontSize(true, "12px", "14px", "16px");

// Font families
config.setFontFamily("Arial", "Verdana", "Georgia", "Courier New");

// Text alignment
config.setAlignment("left", "center", "right", "justify");

// Link configuration
config.setLink("https://", true); // Default protocol, open external in new tab

// Image toolbar and styles
config.setImage(
    new String[]{"imageStyle:block", "imageStyle:side", "|", "imageTextAlternative"},
    new String[]{"full", "side", "alignLeft", "alignRight"}
);

// Table content toolbar
config.setTable(new String[]{
    "tableColumn", "tableRow", "mergeTableCells",
    "tableProperties", "tableCellProperties"
});

// Code block languages
config.setCodeBlock("    ", // Indent with 4 spaces
    CodeBlockLanguage.of("plaintext", "Plain Text"),
    CodeBlockLanguage.of("java", "Java"),
    CodeBlockLanguage.of("javascript", "JavaScript"),
    CodeBlockLanguage.of("python", "Python"),
    CodeBlockLanguage.of("html", "HTML"),
    CodeBlockLanguage.of("css", "CSS")
);

// Media embed
config.setMediaEmbed(true); // Include previews in exported data

// @mentions
config.setMention(
    MentionFeed.users("Alice", "Bob", "Charlie"),
    MentionFeed.tags("bug", "feature", "improvement")
);

// Autosave interval (100-60000ms)
config.setAutosave(5000); // Save every 5 seconds

// Style definitions (classes must be defined in your CSS)
config.setStyle(
    StyleDefinition.block("Info Box", "p", "info-box"),
    StyleDefinition.block("Big Heading", "h2", "big-heading"),
    StyleDefinition.inline("Marker", "marker"),
    StyleDefinition.inline("Spoiler", "spoiler")
);
```

---

## Toolbar Customization

### Toolbar Items

Common toolbar item names:

| Category | Items |
|----------|-------|
| Text | `bold`, `italic`, `underline`, `strikethrough`, `code` |
| Font | `fontSize`, `fontFamily`, `fontColor`, `fontBackgroundColor` |
| Paragraph | `heading`, `alignment`, `indent`, `outdent`, `blockQuote` |
| Lists | `bulletedList`, `numberedList`, `todoList` |
| Insert | `link`, `insertImage`, `insertTable`, `mediaEmbed` |
| Special | `horizontalLine`, `pageBreak`, `specialCharacters` |
| Code | `codeBlock`, `sourceEditing` |
| Tools | `findAndReplace`, `highlight`, `removeFormat` |
| Undo | `undo`, `redo` |

### Separator and Grouping

```java
// Use "|" as separator
config.setToolbar(
    "heading", "|",
    "bold", "italic", "underline", "|",
    "link", "insertImage", "|",
    "bulletedList", "numberedList", "|",
    "undo", "redo"
);

// Disable auto-grouping when toolbar overflows
config.setToolbar(
    new String[]{"bold", "italic", "link"},
    true // shouldNotGroupWhenFull
);
```

### Toolbar Styling

```java
ToolbarStyle style = new ToolbarStyle()
    .setBackground("#f5f5f5")
    .setBorderColor("#ddd")
    .setBorderRadius("8px")
    .setButtonBackground("transparent")
    .setButtonHoverBackground("#e0e0e0")
    .setButtonActiveBackground("#d0d0d0")
    .setIconColor("#333");

// Per-button styles
style.setButtonStyle("bold", new ButtonStyle()
    .setIconColor("#1976d2")
    .setHoverBackground("#e3f2fd")
);

config.setToolbarStyle(style);
```

---

## Themes

### Theme Options

```java
// Auto (default) - Follows Vaadin Lumo theme and OS dark mode
VaadinCKEditor.create()
    .withTheme(CKEditorTheme.AUTO)
    .build();

// Force light theme
VaadinCKEditor.create()
    .withTheme(CKEditorTheme.LIGHT)
    .build();

// Force dark theme
VaadinCKEditor.create()
    .withTheme(CKEditorTheme.DARK)
    .build();
```

### Custom CSS

```java
// Override with custom CSS
editor.setOverrideCssUrl("/frontend/my-editor-styles.css");
```

---

## Events

### Editor Ready

```java
editor.addEditorReadyListener(event -> {
    System.out.println("Editor initialized in " + event.getInitializationTimeMs() + "ms");
});
```

### Content Change

```java
editor.addContentChangeListener(event -> {
    String oldContent = event.getOldContent();
    String newContent = event.getNewContent();
    ChangeSource source = event.getChangeSource();
    int delta = event.getLengthDelta();

    System.out.println("Content changed by " + source + ", delta: " + delta);
});
```

Change sources:
- `USER_INPUT` - Direct user typing
- `API` - Programmatic `setValue()`
- `UNDO_REDO` - Undo/redo operation
- `PASTE` - Paste action
- `COLLABORATION` - Collaborative editing sync
- `UNKNOWN` - Unknown source

### Autosave

```java
// Enable autosave
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withAutosave(content -> saveToDatabase(content), 5000) // 5 second interval
    .build();

// Listen to autosave events
editor.addAutosaveListener(event -> {
    if (event.isSuccess()) {
        System.out.println("Content saved at " + event.getTimestamp());
    } else {
        System.err.println("Save failed: " + event.getErrorMessage());
    }
});
```

### Error Events

```java
editor.addEditorErrorListener(event -> {
    EditorError error = event.getError();

    System.err.println("Error [" + error.getCode() + "]: " + error.getMessage());
    System.err.println("Severity: " + error.getSeverity());
    System.err.println("Recoverable: " + error.isRecoverable());

    if (error.getStackTrace() != null) {
        System.err.println("Stack: " + error.getStackTrace());
    }
});
```

### Fallback Events

```java
editor.addFallbackListener(event -> {
    System.out.println("Fallback mode: " + event.getMode());
    System.out.println("Reason: " + event.getReason());
});
```

---

## File Upload

### Upload Handler

```java
editor.setUploadHandler((context, inputStream) -> {
    String fileName = context.getFileName();
    String mimeType = context.getMimeType();
    long fileSize = context.getFileSize();

    // Validate
    if (fileSize > 10_000_000) { // 10MB limit
        return CompletableFuture.completedFuture(
            UploadResult.failure("File too large (max 10MB)")
        );
    }

    // Save file and return URL
    String savedPath = saveFile(fileName, inputStream);
    return CompletableFuture.completedFuture(
        new UploadResult("/uploads/" + savedPath)
    );
});
```

### Simple Upload Configuration

```java
CKEditorConfig config = new CKEditorConfig();

// Basic upload endpoint
config.setSimpleUpload("/api/upload");

// With authentication headers
config.setSimpleUpload("/api/upload", Map.of(
    "Authorization", "Bearer " + token,
    "X-CSRF-Token", csrfToken
));

// With CORS credentials
config.setSimpleUpload("/api/upload", headers, true);
```

### Upload Validation

```java
UploadConfig uploadConfig = new UploadConfig();
uploadConfig.setMaxFileSize(5_000_000); // 5MB
uploadConfig.setAllowedMimeTypes(
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/webp"
);

// Validate in handler
editor.setUploadHandler((context, stream) -> {
    String error = uploadConfig.validate(context);
    if (error != null) {
        return CompletableFuture.completedFuture(UploadResult.failure(error));
    }
    // Process upload...
});
```

### Upload Timeout Configuration

The upload timeout mechanism prevents upload operations from hanging indefinitely:

- **Frontend timeout**: 5 minutes (300,000ms) by default
- **Backend timeout**: 6 minutes (360s) by default

The backend timeout is 1 minute longer than the frontend to ensure the frontend triggers the timeout error first, avoiding race conditions.

```java
// Custom backend timeout (in UploadManager constructor)
UploadManager manager = new UploadManager(
    handler,
    config,
    callback,
    180  // 3 minutes (seconds)
);
```

```typescript
// Custom frontend timeout
uploadManager.setUploadTimeout(120000); // 2 minutes (milliseconds)
uploadManager.setUploadTimeout(0);      // Disable timeout
```

### SSRF Protection

`setSimpleUpload()` has built-in SSRF (Server-Side Request Forgery) protection:

**Protected addresses:**
- IPv4 private addresses: `127.x.x.x`, `10.x.x.x`, `192.168.x.x`, `172.16-31.x.x`
- IPv6 private addresses: `::1`, `fe80::`, `fc00::/fd00::`
- IPv4-mapped IPv6: `::ffff:127.0.0.1`
- IPv4-compatible IPv6: `::127.0.0.1`
- SIIT format: `::ffff:0:127.0.0.1`
- Octal/hex notation: `0177.0.0.1`, `0x7f.0.0.1`
- Special hostnames: `localhost`, `*.local`, `*.internal`

**Known limitations:**
- Decimal integer IPs (e.g., `2130706433`): Treated as a hostname by Java's URI parser, so SSRF checks are not triggered. The actual threat is low since major browsers have inconsistent support for this format.
- DNS rebinding: Not currently protected. For protection, implement request-level validation on the server side.

**Threat model:**
The URL configured via `setSimpleUpload()` is requested by the browser, and the browser's same-origin policy provides an additional layer of protection. This protection primarily prevents accidental internal network access due to misconfiguration, rather than serving as a complete SSRF defense.

**Development environment:**
```java
CKEditorConfig config = new CKEditorConfig();
config.allowPrivateNetworks(true);  // Allow private networks (development only)
config.setSimpleUpload("http://localhost:8080/api/upload");
```

### CSS Value Restrictions

Toolbar style customization supports the following CSS value types:
- Color values: `#fff`, `rgb()`, `hsl()`, color names
- Size values: `10px`, `1em`, `50%`
- Keywords: `none`, `inherit`, `transparent`

**Unsupported CSS functions (for security reasons):**
- `url()` - May lead to data exfiltration
- `calc()` - Not supported due to complexity
- `var()` - CSS variables not supported

For advanced CSS features, use a custom CSS file instead.

---

## Error Handling

### Custom Error Handler

```java
editor.setErrorHandler(error -> {
    switch (error.getSeverity()) {
        case FATAL:
            showNotification("Critical editor error", NotificationVariant.LUMO_ERROR);
            return true; // Handled, stop propagation

        case ERROR:
            logger.error("Editor error: " + error.getMessage());
            return false; // Continue propagation

        case WARNING:
            logger.warn("Editor warning: " + error.getMessage());
            return false;

        default:
            return false;
    }
});
```

### Logging Handler

```java
// Automatic logging with SLF4J
editor.setErrorHandler(ErrorHandler.logging(LoggerFactory.getLogger(MyView.class)));
```

### Chained Handlers

```java
ErrorHandler logHandler = ErrorHandler.logging(logger);
ErrorHandler notifyHandler = error -> {
    if (error.getSeverity() == ErrorSeverity.FATAL) {
        showNotification(error.getMessage());
        return true;
    }
    return false;
};

editor.setErrorHandler(ErrorHandler.compose(notifyHandler, logHandler));
```

---

## HTML Sanitization

### Built-in Policies

```java
// Strict - Basic text only
editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT));

// Basic - Remove scripts and dangerous tags
editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.BASIC));

// Relaxed - Allow formatting tags
editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.RELAXED));

// None - No sanitization (use with caution)
editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.NONE));
```

### Custom Safelist

```java
Safelist safelist = Safelist.relaxed()
    .addTags("video", "audio", "source")
    .addAttributes("video", "src", "controls", "width", "height")
    .addAttributes("audio", "src", "controls");

editor.setHtmlSanitizer(HtmlSanitizer.withSafelist(safelist));
```

### Getting Sanitized Content

```java
// Get sanitized HTML
String cleanHtml = editor.getSanitizedValue();

// Get plain text (all HTML stripped)
String plainText = editor.getPlainText();

// Manual sanitization with custom rules
String custom = editor.sanitizeHtml(html, Safelist.basicWithImages());
```

---

## Fallback Modes

Handle graceful degradation when the editor fails to load:

```java
// Degrade to native textarea
VaadinCKEditor.create()
    .withFallbackMode(FallbackMode.TEXTAREA)
    .build();

// Read-only mode
VaadinCKEditor.create()
    .withFallbackMode(FallbackMode.READ_ONLY)
    .build();

// Show error message
VaadinCKEditor.create()
    .withFallbackMode(FallbackMode.ERROR_MESSAGE)
    .build();

// Hide editor completely
VaadinCKEditor.create()
    .withFallbackMode(FallbackMode.HIDDEN)
    .build();
```

---

## Custom Plugins

### Creating Custom Plugins

```java
// From npm package
CustomPlugin myPlugin = CustomPlugin.builder("MyPlugin")
    .withImportPath("my-ckeditor-plugin")
    .withToolbarItems("myButton")
    .withDependencies(CKEditorPlugin.PARAGRAPH)
    .build();

// From relative path
CustomPlugin localPlugin = CustomPlugin.of("LocalPlugin", "./my-plugins/local-plugin");

// From CKEditor 5 main package
CustomPlugin ckPlugin = CustomPlugin.fromCKEditor5("SomePlugin");

// From premium package
CustomPlugin premiumPlugin = CustomPlugin.fromPremium("Comments")
    .premium()
    .build();
```

### Using Custom Plugins

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .addCustomPlugin(myPlugin)
    .withToolbar("bold", "italic", "|", "myButton")
    .build();
```

---

## Premium Features

Premium features require a CKEditor license.

### Setup

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.COLLABORATIVE)
    .withLicenseKey("your-license-key")
    .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
    .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
    .build();
```

### Available Premium Plugins

- `Comments` - Comment threads on content
- `TrackChanges` - Track content changes
- `RevisionHistory` - Version history
- `RealTimeCollaborativeEditing` - Real-time collaboration
- `RealTimeCollaborativeComments` - Real-time comments
- `RealTimeCollaborativeTrackChanges` - Real-time track changes
- `ExportPdf` - Export to PDF
- `ExportWord` - Export to Word
- `ImportWord` - Import from Word
- `Pagination` - Page breaks and pagination
- `FormatPainter` - Copy formatting
- `SlashCommand` - Slash command menu
- `Template` - Content templates
- `TableOfContents` - Auto-generated TOC
- `CaseChange` - Change text case
- `AIChat` - AI chat sidebar
- `AIEditorIntegration` - AI editor integration
- `AIQuickActions` - AI quick actions
- `AIReviewMode` - AI review mode
- `AITranslate` - AI translation

---

## API Reference

### VaadinCKEditor Methods

#### Content Operations

| Method | Description |
|--------|-------------|
| `getValue()` | Get HTML content |
| `setValue(String)` | Set HTML content |
| `getSanitizedValue()` | Get sanitized HTML |
| `getPlainText()` | Get text without HTML |
| `clear()` | Clear content |
| `insertText(String)` | Insert text at cursor |

#### State

| Method | Description |
|--------|-------------|
| `isReadOnly()` | Check if read-only |
| `setReadOnly(boolean)` | Set read-only mode |
| `setReadOnlyWithToolbarAction(boolean)` | Read-only with hidden toolbar |
| `setHideToolbar(boolean)` | Show/hide toolbar |

#### Layout

| Method | Description |
|--------|-------------|
| `setWidth(String)` | Set width (CSS value) |
| `setHeight(String)` | Set height (CSS value) |
| `setMinimapEnabled(boolean)` | Enable minimap (DECOUPLED only) |

#### Configuration

| Method | Description |
|--------|-------------|
| `setOverrideCssUrl(String)` | Set custom CSS URL |
| `setGeneralHtmlSupportEnabled(boolean)` | Enable GHS |
| `setSynchronized(boolean)` | Enable sync mode |

#### Handlers

| Method | Description |
|--------|-------------|
| `setErrorHandler(ErrorHandler)` | Set error handler |
| `setHtmlSanitizer(HtmlSanitizer)` | Set HTML sanitizer |
| `setUploadHandler(UploadHandler)` | Set upload handler |
| `setFallbackMode(FallbackMode)` | Set fallback mode |

#### Events

| Method | Description |
|--------|-------------|
| `addEditorReadyListener(listener)` | Listen for editor ready |
| `addContentChangeListener(listener)` | Listen for content changes |
| `addAutosaveListener(listener)` | Listen for autosave |
| `addEditorErrorListener(listener)` | Listen for errors |
| `addFallbackListener(listener)` | Listen for fallback |

### VaadinCKEditorBuilder Methods

#### Convenience Methods

| Method | Description |
|--------|-------------|
| `withViewOnly()` | Sets readOnly + hideToolbar + READ_ONLY fallback mode |
| `withReadOnly(boolean)` | Alias for `readOnly(boolean)` |
| `withHideToolbar(boolean)` | Hide/show toolbar in built editor |

---

## Troubleshooting

### Common Issues

**Editor not loading:**
1. Check browser console for JavaScript errors
2. Verify Vaadin frontend build completed successfully
3. Check if all required plugins are included

**Plugins not working:**
1. Ensure plugin dependencies are satisfied
2. Check that toolbar items match plugin capabilities
3. Verify DependencyMode settings

**Theme not applying:**
1. Check if Lumo theme is properly configured
2. Verify themeType setting
3. Try explicit LIGHT or DARK theme

**Upload failing:**
1. Verify upload endpoint is accessible
2. Check CORS configuration
3. Validate file size and MIME type restrictions

### Debug Mode

Enable debug logging in browser:

```javascript
window.VAADIN_CKEDITOR_DEBUG = true;
```

---

## Version History

| Version | CKEditor 5 | Vaadin | Notes           |
|---------|------------|--------|-----------------|
| 5.1.0   | 47.4.0 | 25.0.5 | EMAIL/NOTION presets, AI plugins, slimmed build |
| 5.0.5   | 47.4.0 | 25.0.5 | Premium plugins, upload improvements |
| 5.0.3   | 47.4.0 | 25.0.4 | CI/CD workflows |
| 5.0.1   | 47.4.0 | 25.0.4 | Bug fixes       |
| 5.0.0   | 47.4.0 | 25.0.3 | Initial v5 release |

---

## License

This addon is available under the Apache License 2.0.

CKEditor 5 is available under GPL 2+ or commercial license. See [CKEditor licensing](https://ckeditor.com/legal/ckeditor-oss-license/).
