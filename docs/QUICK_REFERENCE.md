# Vaadin CKEditor 5 - Quick Reference

## Installation

```xml
<dependency>
    <groupId>com.wontlost</groupId>
    <artifactId>ckeditor-vaadin</artifactId>
    <version>5.0.3</version>
</dependency>
```

## Basic Usage

```java
// Quick start with preset (returns editor directly)
VaadinCKEditor editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);

// Get/set content
String html = editor.getValue();
editor.setValue("<p>Hello World</p>");
```

## Editor Types

```java
.withType(CKEditorType.CLASSIC)   // Fixed toolbar (default)
.withType(CKEditorType.BALLOON)   // Floating toolbar
.withType(CKEditorType.INLINE)    // In-place editing
.withType(CKEditorType.DECOUPLED) // Separated toolbar
```

## Presets

| Preset | Plugins |
|--------|---------|
| `BASIC` | Text formatting, lists, links |
| `STANDARD` | + Images, tables, media |
| `FULL` | + Fonts, colors, code blocks |
| `DOCUMENT` | + Page breaks, word count |

## Common Plugins

```java
// Text
CKEditorPlugin.BOLD, ITALIC, UNDERLINE, STRIKETHROUGH

// Font
CKEditorPlugin.FONT_SIZE, FONT_FAMILY, FONT_COLOR

// Paragraph
CKEditorPlugin.HEADING, ALIGNMENT, BLOCK_QUOTE, LIST

// Media
CKEditorPlugin.IMAGE, TABLE, MEDIA_EMBED, CODE_BLOCK
```

## Toolbar Items

```java
// Formatting
"bold", "italic", "underline", "strikethrough", "code"

// Font
"fontSize", "fontFamily", "fontColor", "fontBackgroundColor"

// Paragraph
"heading", "alignment", "bulletedList", "numberedList", "blockQuote"

// Insert
"link", "insertImage", "insertTable", "mediaEmbed", "codeBlock"

// Tools
"undo", "redo", "findAndReplace", "sourceEditing"

// Separator
"|"
```

## Configuration

```java
CKEditorConfig config = new CKEditorConfig()
    .setPlaceholder("Type here...")
    .setLanguage("en")
    .setToolbar("bold", "italic", "|", "link")
    .setFontSize("12px", "14px", "16px", "18px")
    .setAutosave(5000);

VaadinCKEditor.create()
    .withConfig(config)
    .build();
```

## Events

```java
// Editor ready
editor.addEditorReadyListener(e -> log("Ready"));

// Content change
editor.addContentChangeListener(e -> {
    String content = e.getNewContent();
    ChangeSource source = e.getChangeSource();
});

// Autosave
editor.addAutosaveListener(e -> save(e.getContent()));

// Error
editor.addEditorErrorListener(e -> log(e.getError().getMessage()));
```

## File Upload

```java
editor.setUploadHandler((context, stream) -> {
    String url = saveFile(context.getFileName(), stream);
    return CompletableFuture.completedFuture(new UploadResult(url));
});
```

## HTML Sanitization

```java
// Use built-in policy
editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.RELAXED));

// Get clean content
String safe = editor.getSanitizedValue();
String text = editor.getPlainText();
```

## Error Handling

```java
editor.setErrorHandler(error -> {
    if (error.getSeverity() == ErrorSeverity.FATAL) {
        showError(error.getMessage());
        return true; // Handled
    }
    return false;
});
```

## Themes

```java
.withTheme(CKEditorTheme.AUTO)  // Follow Vaadin/OS (default)
.withTheme(CKEditorTheme.LIGHT) // Force light
.withTheme(CKEditorTheme.DARK)  // Force dark
```

## Common Patterns

### Read-only Mode

```java
editor.setReadOnly(true);
editor.setReadOnlyWithToolbarAction(true); // Also hide toolbar
```

### View-only Mode (Recommended for Display)

```java
// One-liner: sets readOnly + hideToolbar + fallbackMode
VaadinCKEditor viewer = VaadinCKEditor.create()
    .withViewOnly()
    .withValue(htmlContent)
    .build();
```

### Custom Toolbar Style

```java
ToolbarStyle style = new ToolbarStyle()
    .setBackground("#f5f5f5")
    .setBorderRadius("8px")
    .setButtonHoverBackground("#e0e0e0");
config.setToolbarStyle(style);
```

### Autosave

```java
VaadinCKEditor.create()
    .withAutosave(content -> saveToDb(content), 5000)
    .build();
```

### Code Blocks

```java
config.setCodeBlock("    ",
    CodeBlockLanguage.of("java", "Java"),
    CodeBlockLanguage.of("javascript", "JavaScript"),
    CodeBlockLanguage.of("python", "Python")
);
```

### @Mentions

```java
config.setMention(
    MentionFeed.users("Alice", "Bob"),
    MentionFeed.tags("bug", "feature")
);
```

## Debug

```javascript
// Enable in browser console
window.VAADIN_CKEDITOR_DEBUG = true;
```
