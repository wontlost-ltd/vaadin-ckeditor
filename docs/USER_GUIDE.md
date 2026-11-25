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
- [Language](#language)
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
    <version>6.1.0</version>
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
- `HEADING`, `ALIGNMENT`, `INDENT`, `INDENT_BLOCK`, `BLOCK_QUOTE`
- 注：`LineHeight` 在 CKEditor 49.1.0 仍属 premium（Essential+），由 `ckeditor5-premium-features` 导出：请用
  `VaadinCKEditorPremium.PremiumPlugin.LINE_HEIGHT`（即 `CustomPlugin.fromPremium("LineHeight")`）

**Lists:**
- `LIST`, `TODO_LIST`, `LIST_PROPERTIES`

**Links:**
- `LINK`, `AUTO_LINK`

**Images:**
- `IMAGE`, `IMAGE_TOOLBAR`, `IMAGE_CAPTION`, `IMAGE_STYLE`
- `IMAGE_RESIZE`, `IMAGE_UPLOAD`, `IMAGE_INSERT`, `IMAGE_BLOCK`, `IMAGE_INLINE`

**Tables:**
- `TABLE`, `TABLE_TOOLBAR`, `TABLE_PROPERTIES`, `TABLE_CELL_PROPERTIES`
- `TABLE_CAPTION`, `TABLE_COLUMN_RESIZE`, `TABLE_LAYOUT`（预设表格布局，`insertTableLayout`）

Some of these plugins are premium features according to CKEditor's docs (for example `IMAGE_RESIZE`, `TABLE_PROPERTIES`,
`TABLE_COLUMN_RESIZE`, `TABLE_LAYOUT`, `PASTE_FROM_OFFICE`, `FIND_AND_REPLACE` and `WORD_COUNT`). `CKEditorPlugin#isPremiumFeature()`
identifies them, and the [Premium Integration Guide](PREMIUM_INTEGRATION.md#premium-features-exported-by-the-ckeditor5-package)
lists them all. They run under the default `'GPL'` key, because CKEditor's core skips plugin licence checks for it.
Commercial use needs the plan CKEditor states.

**Media & Code:**
- `MEDIA_EMBED`, `AUTO_MEDIA_EMBED`, `MEDIA_EMBED_STYLE`, `MEDIA_EMBED_TOOLBAR`, `HTML_EMBED`, `CODE_BLOCK`
- 注：`MEDIA_EMBED_STYLE`（媒体对齐样式）的按钮属 `config.mediaEmbed.toolbar`，须配合 `MEDIA_EMBED_TOOLBAR` 使用。
  用 `CKEditorConfig#setMediaEmbedToolbar(...)` 设置这些按钮：

```java
config.setMediaEmbedToolbar(
    "mediaEmbed:alignLeft", "mediaEmbed:alignCenter", "mediaEmbed:alignRight");
```

**HTML:**
- `GENERAL_HTML_SUPPORT`, `HTML_COMMENT`, `STYLE`, `FULL_PAGE` (whole HTML documents, see [General HTML Support](#general-html-support))
- `HTML_COMMENT`, `HTML_EMBED` and `SOURCE_EDITING` do not load `GENERAL_HTML_SUPPORT`: CKEditor does not require it,
  and it changes what `getValue()` returns. They only *recommend* it
  (`DependencyMode.AUTO_RESOLVE_WITH_RECOMMENDED` adds it). Before 6.2.0 they always loaded it.

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

`SPECIAL_CHARACTERS` needs at least one category plugin
(`SPECIAL_CHARACTERS_ESSENTIALS`, `..._LATIN`, `..._ARROWS`, …) or its panel is
empty. `AUTO_RESOLVE` and `AUTO_RESOLVE_WITH_RECOMMENDED` add
`SPECIAL_CHARACTERS_ESSENTIALS` when no category is selected, `STRICT` reports
it as a missing dependency, and `MANUAL` uses the plugins exactly as given.

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

#### Heading options

The third argument of `HeadingOption.heading(level, title, className)` is the CSS class of the
**item in the heading dropdown** (CKEditor's `class` property, e.g. `ck-heading_heading1`). It is
not added to the heading in the content. To output a class on the heading element, use
`withViewClasses(...)`. Each option needs its own model name; to offer a second variant of a level
that already has an entry, give it one with `withModel(...)`:

```java
config.setHeading(
    HeadingOption.paragraph("Paragraph", "ck-heading_paragraph"),
    HeadingOption.heading(1, "Heading 1", "ck-heading_heading1"),          // <h1>
    HeadingOption.heading(1, "Article title", "")                          // <h1 class="article">
        .withModel("heading1Article")
        .withViewClasses("article"),
    HeadingOption.heading(4, "Section", "").withViewClasses("section"));   // <h4 class="section">
```

Variants with view classes are converted with `converterPriority: 'high'`, as in the CKEditor
[custom heading elements](https://ckeditor.com/docs/ckeditor5/latest/features/headings.html) guide,
so `<h1 class="article">` loads as the variant and a plain `<h1>` as "Heading 1".
`setHeading` rejects two options with the same model with an `IllegalArgumentException`:
CKEditor would otherwise fail to start with `schema-cannot-register-item-twice`.

### Common Configuration Options

**Emoji** (needs `CKEditorPlugin.EMOJI`; `definitions` inlines the emoji database instead of downloading it, CKEditor 49.1+):

```java
config.setEmoji(new CKEditorConfig.EmojiOptions()
    .definitionsUrl("/emoji/en.json")   // or .definitions(arrayNode)
    .version(16)
    .skinTone("medium")
    .dropdownLimit(50));
```

```java
CKEditorConfig config = new CKEditorConfig();

// Placeholder text
config.setPlaceholder("Start typing...");

// Language (UI and content) — see the Language section below
config.setLanguage("sk");
// Or separate UI and content languages (+ Text Part Language options)
config.setLanguage("en", "sk", new String[]{"sk", "de"});

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

### General HTML Support

[General HTML Support](https://ckeditor.com/docs/ckeditor5/latest/features/html/general-html-support.html)
(GHS) keeps HTML that no editor feature handles. `HtmlSupport` builds CKEditor's `htmlSupport` option
(`allow` / `disallow` rules with `name`, `attributes`, `classes`, `styles`, plus `allowEmpty`,
`preserveEmptyBlocksInEditingView`, `htmlIframeSandbox` and `fullPage.allowRenderStylesFromHead`):

```java
config.setHtmlSupport(HtmlSupport.create()
    // <div>, <section>, <article> with any class
    .allow(Rule.elements("div", "section", "article").classes(true))
    // collapsible blocks
    .allow(Rule.elements("details", "summary").attributes("open"))
    // YouTube embeds only: iframes need an explicit opt-in
    .allowIframes()
    .allow(Rule.element("iframe")
        .attribute("src", Pattern.compile("^https://www\\.youtube(-nocookie)?\\.com/embed/"))
        .attributes("width", "height", "allowfullscreen", "title"))
    // classes and styles on paragraphs
    .allow(Rule.element("p").classes("lead", "note").styles("color", "text-align"))
    .allow(Rule.element("span").classes(Pattern.compile("^text-")).style("color", Pattern.compile("^#[0-9a-f]{6}$"))));
```

| Rule method | GHS |
|-------------|-----|
| `Rule.element("div")`, `Rule.elements("div", "section")`, `Rule.element(Pattern)`, `Rule.anyElement()` | `name: 'div'`, `name: /^(div\|section)$/`, `name: /.../`, no name (every element) |
| `attributes(true)`, `attributes("id", "title")`, `attribute(Pattern)`, `attribute("rel", "noopener")`, `attribute("src", Pattern)` | `attributes: true` or `[ 'id', /^data-/, { key: 'src', value: /.../ } ]` |
| `classes(true)`, `classes("a", "b")`, `classes(Pattern)` | `classes: true` or `[ 'a', /^text-/ ]` |
| `styles(true)`, `styles("color")`, `style("color", "red")`, `style("color", Pattern)` | `styles: true` or `[ 'color', { key: 'color', value: /.../ } ]` |

- **Safe by default.** Unless you call `allowScripts()`, `allowIframes()` or `allowEventHandlers()`,
  three `disallow` rules are added: `<script>`, `<iframe>` and `on*` attributes on every element
  (GHS applies `disallow` after `allow`). Opt in only for content you trust.
- **CKEditor is not an XSS filter.** Whatever GHS allows is returned by `getValue()`. Allowed `href` /
  `src` values are not checked for `javascript:` URLs, and `htmlIframeSandbox` only affects the editing
  view, not the data. Sanitize on the server before storing or displaying the HTML (see below).
- Patterns are Java `Pattern`s in the syntax Java and JavaScript share; they run in the browser.
  Flags `CASE_INSENSITIVE`, `MULTILINE`, `DOTALL` are supported. Constructs that are Java-only, or that
  JavaScript reads differently, throw `IllegalArgumentException` when the rule is built: character-class
  intersections (`[a-z&&[^e]]`, two literal `&` in JavaScript) and nested classes, `\p{...}` / `\P{...}`,
  `\h`, `\v`, `\R`, `\X`, `\A`, `\Z`, `\z`, `\G`, `\Q...\E`, `\x{...}`, octal `\0`, atomic groups `(?>...)`,
  inline flags `(?i)`, possessive quantifiers, and `]` as the first character of a class. Everything else
  follows the browser's semantics: `\s`, for example, also matches Unicode whitespace such as the
  non-breaking space in JavaScript, while Java's default `\s` matches ASCII whitespace only.
- The builder loads `CKEditorPlugin.GENERAL_HTML_SUPPORT` whenever `setHtmlSupport(HtmlSupport)` or
  `setHtmlSupport(true)` is used (not in `DependencyMode.MANUAL`).
- **Full page HTML.** `fullPageAllowRenderStylesFromHead(true)` is read by CKEditor's
  [Full page HTML](https://ckeditor.com/docs/ckeditor5/latest/features/html/full-page-html.html) plugin
  `FullPage` (`CKEditorPlugin.FULL_PAGE`, a plan-gated premium feature; the `GPL` key works). Setting the option
  makes the builder load `FULL_PAGE` (not in `DependencyMode.MANUAL`); you can also add `FULL_PAGE` without the
  option. With `FullPage`, `getValue()` returns the whole document: `<html>`, `<head>` (`<title>`, `<meta>`,
  `<style>`) and `<body>`. With the option set to `true`, the `<style>` elements of `<head>` are rendered in the
  page. **Security risk:** CKEditor recommends a `sanitizeCss` callback for that, which cannot be passed from Java;
  use it only for trusted content. Before 6.2.0 the add-on could not load `FullPage`, so the option had no effect.
- `setHtmlSupport(true)` is `HtmlSupport.allowAll()`: every element with all attributes, classes and
  styles, except scripts, iframes and `on*` attributes. Before 6.2.0 it sent the name as the string
  `"/.*/"`, which CKEditor compares literally, so it had no effect.
  `setGeneralHtmlSupportEnabled(true)` is unchanged and allows everything, scripts included.

**Server-side sanitizer.** `HtmlSanitizer` (jsoup) runs on the server and is independent of GHS: it
does not know your GHS rules. With `getSanitizedValue()` or `setSanitizeOnInput(true)`, elements GHS
keeps are still removed unless the safelist allows them, e.g. for the example above:

```java
editor.setHtmlSanitizer(HtmlSanitizer.withSafelist(Safelist.relaxed()
    .addTags("section", "article", "details", "summary", "iframe")
    .addAttributes(":all", "class")
    .addAttributes("p", "style")
    .addAttributes("details", "open")
    .addAttributes("iframe", "src", "width", "height", "allowfullscreen", "title")
    .addProtocols("iframe", "src", "https")));
```

### Any CKEditor option: `set(path, value)`

Options without a dedicated setter (premium features, AI, cloud services, ...) are set with
`set(path, value)`. The path uses dots for nesting, the same notation CKEditor uses in its
[`Config`](https://ckeditor.com/docs/ckeditor5/latest/api/module_utils_config-Config.html) API
(`config.get('toolbar.collapsed')`), so the keys from the CKEditor documentation can be copied as-is:

```java
CKEditorConfig config = new CKEditorConfig()
    // https://ckeditor.com/docs/ckeditor5/latest/api/module_cloud-services_cloudservicesconfig-CloudServicesConfig.html
    .set("cloudServices.tokenUrl", "https://example.com/cs-token")
    .set("cloudServices.webSocketUrl", "wss://example.com/ws")
    .set("collaboration.channelId", "document-" + documentId)
    // values: String, int / long / double / boolean, Number, List, Map, JsonNode
    .set("fontSize.options", List.of(10, 12, "default", 16, 20))
    // AI Assistant with the OpenAI adapter (add CustomPlugin.fromPremium("AIAssistant") and
    // CustomPlugin.fromPremium("OpenAITextAdapter")):
    // https://ckeditor.com/docs/ckeditor5/latest/features/ai/ai-assistant/ai-assistant-integration.html
    .set("ai.assistant.adapter.openAI", Map.of(
        "apiUrl", "https://example.com/ai-proxy",          // your proxy, keeps the API key off the client
        "requestParameters.model", "gpt-4o"));             // map keys are paths too
// → { cloudServices: { tokenUrl, webSocketUrl }, collaboration: { channelId }, fontSize: { options: [...] },
//     ai: { assistant: { adapter: { openAI: { apiUrl, requestParameters: { model } } } } } }
```

- Parents are **merged**: `set("cloudServices.webSocketUrl", ...)` keeps an existing `tokenUrl`,
  also inside objects built by the typed setters (`set("ui.viewportOffset.top", 56)` after
  `setUiViewportOffset(...)` keeps the other sides).
- The last segment is **replaced**: `set("cloudServices", Map.of(...))` replaces the whole object,
  as in earlier versions. A parent that is not an object (e.g. a toolbar array) is replaced by one.
- **Keys cannot contain a literal dot.** CKEditor splits every key on `.` at every level when the
  editor is created, so such a key could never reach a plugin; there is no escape syntax.
  Empty segments (`"a..b"`, `".a"`) throw `IllegalArgumentException`.
- `getJsonNode`, `getString`, `getInt`, `getBoolean` and `hasConfig` accept the same paths.
- Callbacks (functions) and DOM elements cannot be passed from Java, e.g. a token *function* for
  `cloudServices.tokenUrl`; use a URL.

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

## Language

The editor UI can use any language CKEditor 5 ships a translation for — English plus 72 translations such as
`sk`, `cs`, `de`, `pl`, `pt-br`, `sr-latn` or `zh-cn` (the full list is the file names in
`ckeditor5/dist/translations/`). Only the selected translation is downloaded, on demand, as a
separate chunk; English needs no download.

```java
// Language code
VaadinCKEditor.create().withLanguage("sk").build();

// Vaadin / Java Locale, e.g. the UI locale
VaadinCKEditor.create().withLocale(UI.getCurrent().getLocale()).build();

// Separate UI language, content language and Text Part Language options
CKEditorConfig config = new CKEditorConfig();
config.setLanguage("sk", "sk", new String[]{"sk", "en"});
VaadinCKEditor.create().withConfig(config).build();
```

Rules:

- Codes are case-insensitive and accept `-` or `_`: `"sk"`, `"sk-SK"`, `"sk_SK"` and `Locale.of("sk", "SK")`
  all select the Slovak UI. When there is no exact translation, region and script subtags are dropped until one
  matches (`"de-AT"` → `de`, `"sr-Latn-RS"` → `sr-latn`, `"en-US"` → `en`). The editor then uses that translation
  code for both UI and content (`lang="sk"`); set a content language explicitly to change that.
- Chinese follows CKEditor's bundle names: `zh` is Traditional and `zh-cn` is Simplified.
  `"zh-tw"`, `"zh-hk"` and `"zh-Hant"` select `zh`; `"zh-Hans"` and `"zh-sg"` select `zh-cn`.
- `withLanguage(...)` accepts a primary language with an optional script and an optional region
  (`"sk"`, `"sr-Latn-RS"`, `"es-419"`) and throws `IllegalArgumentException` for anything else
  (variants, private-use subtags, malformed separators). `withLocale(...)` uses the locale's language,
  script and region and ignores variants and extensions.
- A language CKEditor has no translation for falls back to the English UI and logs a browser warning.
- `withLanguage(...)` / `withLocale(...)` set the UI language and replace the UI language in `withConfig(...)`,
  but keep the config's content language and Text Part Language options. Without them, the language from
  `withConfig(...)` is used as is. On the client, `config.language` (a string, or the `ui` of the object form)
  takes precedence over the component's `language` property.
- The content language (`setLanguage(ui, content, ...)`) sets the `lang` attribute and text direction
  (e.g. `ar`, `he` are right-to-left) of the edited content; it needs no translation.
- `textPartLanguages` are the options of the Text Part Language dropdown (`CKEditorPlugin.TEXT_PART_LANGUAGE`);
  their titles are the language names in the UI language.
- Premium features (`ckeditor5-premium-features`) ship their own translations, which are not loaded
  automatically; their UI stays in English.

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

`AUTO` follows the Vaadin `theme` attribute on `<html>` (`dark` / `light`); when it is unset or uses the
system color scheme (`@ColorScheme(SYSTEM)`, `LIGHT_DARK`, `DARK_LIGHT` → `theme="light-dark"` / `"dark-light"`)
it follows the OS `prefers-color-scheme`, like Lumo.

### Theme Variables

The dark theme is built from CKEditor CSS variables, so you can tune it — or match it to Lumo — with plain CSS
in your theme. Set the variables on `vaadin-ckeditor` (or `html` for every editor):

```css
vaadin-ckeditor {
    /* Bookmark anchor icon: idle, hover, selected */
    --ck-bookmark-icon-fill-color: var(--lumo-secondary-text-color);
    --ck-bookmark-icon-hover-fill-color: var(--lumo-primary-color);
    --ck-bookmark-icon-selected-fill-color: var(--lumo-primary-color);
}
```

`--ck-bookmark-icon-fill-color` defaults to the editor text color (`--ck-color-text`), so the icon is
dark in the light theme and near-white in the dark theme.

#### Fullscreen page width

CKEditor's fullscreen mode (`CKEditorPlugin.FULLSCREEN`) shows the content as a page. Choose its width per editor
with `FullscreenPage`:

| Value | Page width |
|-------|------------|
| `FullscreenPage.A4_FIT` (default since 6.2.0) | Editing column minus a 28px gutter on each side; margins and minimum height keep A4 proportions |
| `FullscreenPage.A4` | CKEditor's fixed A4 width, `795.701px` (the look before 6.2.0) |
| `FullscreenPage.FULL_WIDTH` | The whole editing column |
| `FullscreenPage.custom("1200px")` | Any CSS width: a number with a unit or `%`, or `calc()` / `min()` / `max()` / `clamp()` of them |

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .addPlugin(CKEditorPlugin.FULLSCREEN)
    .withFullscreenPage(FullscreenPage.custom("min(1400px, 90%)"))
    .build();

editor.setFullscreenPage(FullscreenPage.A4);   // back to CKEditor's fixed page, also while fullscreen is open
```

**How `A4_FIT` fits.** The *editing column* is the fullscreen area minus every visible sidebar and its margins:
the document outline on the left (CKEditor moves it into fullscreen), the minimap on the right (the add-on moves it into
fullscreen, see below) and CKEditor's comment / AI panels. Sidebars keep their width and are never overlapped; when a
sidebar is absent its space goes to the page. The page is as wide as the column minus a gutter equal to the fullscreen
top margin (`--ck-fullscreen-editor-top-margin`, 28px) on each side. Its padding (20mm × 12mm on a 210mm page) and
minimum height (297mm) scale with that width, so an empty page looks like an A4 sheet. The page grows downwards with the
content, as in CKEditor, so the window height does not limit the width. Resizing the window or zooming the browser
(Ctrl/Cmd + / −) changes the page size: at 200 % zoom the page still fills the window, with text twice as large.
`FULL_WIDTH` uses the same column without gutters. `A4` keeps CKEditor's own layout.

**Document editor sidebars in fullscreen** (`CKEditorType.DECOUPLED`):

| Sidebar | In fullscreen |
|---------|---------------|
| Document outline (`setDocumentOutlineEnabled`) | Moved by CKEditor into the fullscreen left sidebar; still navigates |
| Minimap (`setMinimapEnabled`) | Moved by the add-on to the right of the page and re-bound to the fullscreen scroll area (CKEditor leaves it behind, hidden and tracking the old scroll area) |

Both return to the normal layout when fullscreen closes. The page also ignores `setWidth` / `setHeight` while in
fullscreen (they used to squeeze the page), and uses them again afterwards.

CKEditor's fullscreen mode supports the `CLASSIC` and `DECOUPLED` editor types only. For `BALLOON` and `INLINE`
editors `setFullscreenPage` has no effect.

> **Behaviour change in 6.2.0:** fullscreen used CKEditor's fixed 795.701px page before. Use `FullscreenPage.A4` to keep
> that look.

Percentages in `custom(...)` refer to the editing column. A fixed width (from `custom(...)` or the `html` override
below) that does not fit beside the visible sidebars never slides under them: the fullscreen area scrolls horizontally
instead. In mixed expressions only the fixed part counts towards that minimum (`max(50cqw, 1200px)` needs 1200px), and
a page never grows wider than the editing column (`calc(100% + 120px)` is capped at the column). Every value applies to both the editing view and the
source view, and only to this editor.

**Application-wide override.** `--ck-fullscreen-editable-width` set on `html` overrides the setting of every editor.
Use a length (`1200px`, `90vw`); with `A4_FIT` the margins and minimum height follow the overridden width:

```css
html {
    --ck-fullscreen-editable-width: 1200px;
}
```

### Code block syntax highlighting

CKEditor's code blocks are not colored while editing ([ckeditor/ckeditor5#5769](https://github.com/ckeditor/ckeditor5/issues/5769)).
The add-on can highlight them with [highlight.js](https://highlightjs.org/) — opt-in, off by default. It is a
[custom plugin](#custom-plugins) shipped with the add-on (`VaadinCodeBlockHighlighting`): editors that never enable
highlighting do not load it, nor highlight.js.


```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)                                 // FULL / AI_DOCUMENT / NOTION include CODE_BLOCK
    .withCodeBlockHighlighting(CodeHighlightTheme.AUTO)              // or withCodeBlockHighlighting(true)
    .build();

editor.setCodeBlockHighlightTheme(CodeHighlightTheme.ATOM_ONE_DARK); // takes effect immediately
editor.setCodeBlockHighlighting(false);                              // so does switching it off
```

Enabling highlighting for the first time on an editor that is already displayed adds the plugin, which recreates the
editor once with its current content (the undo history starts anew). After that, switching it on and off and changing the
theme apply immediately.

| `CodeHighlightTheme` | Colors |
|----------------------|--------|
| `AUTO` (default) | highlight.js `github` on a light editor, `github-dark` on a dark one (Lumo dark, OS dark mode or `CKEditorTheme.DARK`); keeps the editor's code block background |
| `GITHUB` / `ATOM_ONE_LIGHT` | Light palette with a light code block background |
| `GITHUB_DARK` / `ATOM_ONE_DARK` | Dark palette with a dark code block background, in light and dark editors |
| `NONE` | No built-in colors: code carries highlight.js `hljs-*` classes, style them with any highlight.js theme CSS |

**What is highlighted.** Each code block uses its language from `codeBlock.languages` (the toolbar's language list).
CKEditor's default languages are all supported (`cs` → C#, `html` → XML/HTML); custom languages work when
highlight.js knows the name or alias (`kotlin`, `sql`, `go`, `rust`, `yaml`, `bash`, …: the highlight.js "common" bundle
of about 36 languages). Plain text and unknown languages stay uncolored.

**The saved HTML stays clean.** Highlighting changes the editing view only, never the document model: `getValue()`
returns the same `<pre><code class="language-…">` HTML as without highlighting, undo/redo history is not touched,
and value-change events do not fire because of it. There is no option to store the highlight markup; highlight the
saved HTML where it is displayed (highlight.js and Prism both understand the `language-*` classes).

**Colors** are CSS variables on the editing root (`--ck-code-hl-keyword`, `-string`, `-comment`, `-number`, `-title`,
`-builtin`, …); override any of them in your theme:

```css
.ck-code-hl { --ck-code-hl-keyword: #b00; }
```

**Performance.** highlight.js (the common bundle: about 150 KB minified, 37 KB brotli) is loaded the first time an editor enables
highlighting. After typing, only the changed code block is re-tokenized (debounced by 30 ms) and only lines whose
coloring changed are re-rendered. Measured in Chromium on an Apple Silicon laptop (`e2e/tests/code-block-highlighting.spec.ts`):

| Code block | First highlight | Keystroke without highlighting | Keystroke with highlighting | Re-highlight after the keystroke |
|------------|-----------------|--------------------------------|-----------------------------|----------------------------------|
| 100 lines (5 KB) | 25 ms | 10 ms | 10 ms | 4 ms |
| 500 lines (27 KB) | 61 ms | 20 ms | 33 ms | 15 ms |
| 1500 lines (82 KB) | 179 ms | 58 ms | 85 ms | 43 ms |

Code blocks longer than 100,000 characters are not highlighted. While an input method (IME) composition is in progress
highlighting waits until it ends. Lines that also carry a comment or suggestion highlight keep their previous coloring
until that highlight goes away.

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

### Word Count and Limits

Live word and character counts come from CKEditor's
[WordCount](https://ckeditor.com/docs/ckeditor5/latest/features/word-count.html) plugin through the
add-on's `VaadinWordCount` [registry plugin](#plugins-from-the-browser-registry)
(`VaadinCKEditor.WORD_COUNT_PLUGIN`), which requires `WordCount`. It is added automatically when a
listener is registered or the counter is shown, so any preset works.

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withMaxWords(500)            // 0 = no limit
    .withMaxCharacters(3000)
    .withWordCountVisible(true)   // "Words: 12  Characters: 64" below the editor
    .build();

editor.addWordCountChangeListener(e ->
    status.setText(e.getWords() + " / 500 words"));

editor.setMaxWordsErrorMessage("Keep it under {0} words (now {1})");

Binder<Post> binder = new Binder<>(Post.class);
binder.forField(editor).bind(Post::getBody, Post::setBody);   // the limits are part of the default validator
```

- **Rate:** CKEditor updates the count at most every 250 ms; the add-on forwards it to the server at
  most once per 500 ms, only when a number changed, plus once with the initial counts. Register the
  listener (or show the counter) before the editor is attached: the first call on a displayed editor
  adds the plugin, which recreates the editor once with its current content (undo history starts anew).
- **Limits mark the field invalid; they do not block input.** Over the limit the editor shows Vaadin's
  error state (red border, error message, `aria-invalid`), the counter turns red, and a bound `Binder`
  fails `validate()` / `writeBean()`. Truncating rich text would break paste, undo and IME composition,
  and CKEditor has no built-in hard limit; its own
  [demo](https://ckeditor.com/docs/ckeditor5/latest/features/word-count.html) warns instead of blocking.
- **Counted on the server.** Validation counts the value on the server with the WordCount rules
  (characters exclude block boundaries; words match `([\p{L}\p{N}]+\S?)+`), so a client cannot bypass
  it and the number matches the counter. The invalid state follows the value, i.e. it updates on every
  change by default and on blur with `setSynchronized(false)`.
- `setManualValidation(true)` stops the editor from marking itself invalid; the default validator still
  applies through `Binder`. `getWordCount()` / `getCharacterCount()` keep their old estimate and can differ.
- Show only one number with `config.set("wordCount", Map.of("displayCharacters", false))` (needs the
  dotted-path `set` overloads) or the equivalent `JsonNode`.
- `WordCount` is a CKEditor premium feature (`CKEditorPlugin.WORD_COUNT.isPremiumFeature()`, see the
  [Premium Integration Guide](PREMIUM_INTEGRATION.md#premium-features-exported-by-the-ckeditor5-package)):
  the `GPL` key works; commercial license keys must include the WordCount feature.

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

| Code | Severity | Recoverable | Meaning |
|------|----------|-------------|---------|
| `EDITOR_CREATION_FAILED` | FATAL | false | Editor could not be created; a fallback mode follows |
| `PLUGIN_LOAD_FAILED` | WARNING | true | A plugin failed to load; the editor was created without it |
| `EDITOR_RUNTIME_ERROR` | ERROR | true | An error escaped the editor at runtime (CKEditor 49 no longer restarts the editor; content and undo history are kept, but the operation that threw did not complete) |

`recoverable` means the editor instance stays usable after the error. It does not mean the failing operation was retried.

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
    .withDependencies("Paragraph")
    .build();

// From relative path
CustomPlugin localPlugin = CustomPlugin.of("LocalPlugin", "./my-plugins/local-plugin");

// Any export of the ckeditor5 package, by name (see below)
CustomPlugin tableScroll = CustomPlugin.fromCKEditor5("TableScroll");

// From the premium package (ckeditor5-premium-features)
CustomPlugin comments = CustomPlugin.fromPremium("Comments");
// ... or with toolbar items
CustomPlugin template = CustomPlugin.builder("Template").premium().withToolbarItems("insertTemplate").build();
```

### Any `ckeditor5` export by name

`CustomPlugin.fromCKEditor5(name)` loads **any** plugin class the `ckeditor5` package exports, not only the
`CKEditorPlugin` constants, for example `TableScroll`, `PictureEditing`, `ImageSizeAttributes`, `Font` or
`HeadingButtonsUI`. `CustomPlugin.of(name, "ckeditor5")` is the same. An unknown name is skipped and reported as a
load error; the editor is still created.

- The connector resolves the name in the `ckeditor5` module that Vite has already bundled, with a literal
  `import('ckeditor5')`. Before 6.2.0 `fromCKEditor5` only found names in the connector's static plugin table, and
  `of(name, "ckeditor5")` went through a runtime `import("ckeditor5")` that Vite does not rewrite, so it failed in
  production builds ("Failed to resolve module specifier"). `e2e/tests/ckeditor5-export.spec.ts` checks both forms
  against the production build of the sample app.
- **Bundle size.** Looking an export up by a name known only at runtime means Vite (Rollup) cannot tree-shake
  the `ckeditor5` package by export. In practice this changes nothing for Vaadin applications: the production
  build of the sample app already contained the whole package before 6.2.0 (its `ckeditor5` chunk has, for
  example, `MultiRootEditor` and `TableScroll`, which the add-on never used), and the chunk is the same size with
  this lookup (1,981,940 bytes before, 1,981,958 after; about 527 KB gzipped).
- Precedence: the connector's static plugins, then plugins registered with `registerCKEditorPlugin` /
  `registerCKEditorPluginLoader`, then the `ckeditor5` export of that name.
- Plugins from other npm packages still need `registerCKEditorPlugin` or `registerCKEditorPluginLoader` in a
  frontend module (see [Bundling and lazy loading](#bundling-and-lazy-loading)): Vite cannot bundle an import path
  that is only known at runtime. `CustomPlugin.of(name, "ckeditor5-premium-features")` is resolved like
  `fromPremium(name)`.

### Using Custom Plugins

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .addCustomPlugin(myPlugin)
    .withToolbar("bold", "italic", "|", "myButton")
    .build();
```

### Plugins from the browser registry

A plugin class registered in the frontend with `registerCKEditorPlugin`, or a lazy loader registered with
`registerCKEditorPluginLoader` (see [Bundling and lazy loading](#bundling-and-lazy-loading)), can be referenced
by name, without an import path (`CustomPlugin.fromRegistry`). Built-in CKEditor plugin names take precedence.
Use `CustomPlugin.builder(name).withConfig(...)` without `withImportPath` to pass configuration to such a plugin.

```ts
// src/main/frontend/my-plugins.ts, loaded with @JsModule("./my-plugins.ts") on a view or the app shell
// (Vaadin copies the add-on's frontend files to Frontend/generated/jar-resources/)
import { registerCKEditorPlugin } from 'Frontend/generated/jar-resources/vaadin-ckeditor/plugin-resolver';
import Highlighter from './highlighter';
registerCKEditorPlugin('Highlighter', Highlighter);
```

```java
builder.addCustomPlugin(CustomPlugin.fromRegistry("Highlighter"));
```

The add-on's own word count bridge (`VaadinCKEditor.WORD_COUNT_PLUGIN`, `VaadinWordCount`) is such a
plugin; register a subclass under the same name to extend or replace it (an application registration
wins whether it runs before or after the connector).

### Bundling and lazy loading

A path passed to `withImportPath(...)` is imported at runtime, outside Vaadin's Vite build, so in a production build it
only works for modules the browser can load by that URL. To ship a plugin inside the application bundle, register it
from a `@JsModule` script with the connector's registry (`vaadin-ckeditor/plugin-resolver`). Registered plugins take
precedence over the import path:

```ts
// frontend/my-plugins.ts, loaded with @JsModule("./my-plugins.ts") on any Vaadin component
import { registerCKEditorPlugin, registerCKEditorPluginLoader } from 'Frontend/generated/jar-resources/vaadin-ckeditor/plugin-resolver';
import MyPlugin from './my-plugin';

registerCKEditorPlugin('MyPlugin', MyPlugin);                                  // bundled with the page
registerCKEditorPluginLoader('MyHeavyPlugin', () => import('./my-heavy-plugin')); // separate chunk, downloaded
                                                                                  // only by editors that use it
```

A loader may resolve to the plugin class, a module with a `default` export, or a module exporting the plugin under its name.

### Passing configuration from Java

`withConfig(key, json)` hands a value to the plugin as `config[key]` (since 6.2.0). The key is a top-level editor
configuration key: use one that is specific to your plugin (`myPlugin`), never a CKEditor setting such as `toolbar` or
`plugins`, which it would replace:

```java
ObjectNode options = JsonUtil.createObjectNode().put("level", 2);
CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
    .withImportPath("my-plugin")
    .withConfig("myPlugin", options)
    .build();
```

```ts
class MyPlugin extends Plugin {
    init() {
        const options = this.editor.config.get('myPlugin');         // { level: 2 }
        this.listenTo(this.editor, 'vaadin:pluginConfig', (evt, key, value) => {
            if (key === 'myPlugin') { /* apply the new value */ }
        });
    }
}
```

Plugins are applied when the editor is created. If the plugin list of a displayed editor changes (the server sends a new
`plugins` list, as `setCodeBlockHighlighting(true)` does), the connector compares it with the current one: when only
configuration values changed it updates `editor.config` and fires `vaadin:pluginConfig` (key, value) on the editor;
when plugins were added or removed it recreates the editor with its current content (undo history starts anew).

The add-on's own optional features use this mechanism: [code block syntax highlighting](#code-block-syntax-highlighting)
is the custom plugin `VaadinCodeBlockHighlighting`, registered with a lazy loader and configured with
`codeBlockHighlighting: { enabled, theme }`.

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
- `Pagination` - Page boundaries for PDF / Word export (Custom plan; toolbar `previousPage`, `nextPage`, `pageNavigation`)
- `FormatPainter` - Copy formatting
- `SlashCommand` - Slash command menu
- `Template` - Content templates
- `TableOfContents` - Auto-generated TOC
- `CaseChange` - Change text case
- `AIChat` - AI chat sidebar
- `AIEditorIntegration` - AI editor integration
- `AIQuickActions` - AI quick actions
- `AIChatBalloon` - Balloon UI for AI Chat (CKEditor 49.1+)
- `AIEditorConfig` - Register custom plugins with the AI agent (CKEditor 49.1+)
- `ProposedChanges` - Editing Toolkit, compare versions and apply differences (CKEditor 49.1+)
- `RealTimeContentFetcher` - Real-time collaboration content fetcher (CKEditor 49.1+)
- `AIReviewMode` - AI review mode
- `AITranslate` - AI translation
- `Footnotes`, `MultiLevelList`, `LineHeight`, `PasteFromOfficeEnhanced`, `Uploadcare`, `EmailConfigurationHelper`,
  `ExportInlineStyles`, `SourceEditingEnhanced`, `TrackChangesData`, `TrackChangesPreview`, `PresenceList`, …

The [Premium Plugin Catalogue](PREMIUM_INTEGRATION.md#premium-plugin-catalogue) lists every plugin with its toolbar item,
the plans that include it and its feature documentation. This add-on is a self-hosted (npm) build, so commercial use
needs a **Custom-plan** key: see [Licensing for self-hosted (npm) builds](PREMIUM_INTEGRATION.md#licensing-for-self-hosted-npm-builds).
`CKBox`, `TableLayout` and 18 other `CKEditorPlugin` entries are premium features exported by the open-source `ckeditor5`
package rather than `ckeditor5-premium-features` (`isPremiumFeature()` is true); see
[that list](PREMIUM_INTEGRATION.md#premium-features-exported-by-the-ckeditor5-package).

### Persisting Comments and Suggestions (no CKEditor Cloud)

`VaadinCKEditorBuilder#withCollaborationBridge(CollaborationBridge)` connects the comments and track changes plugins to a
server-side `CollaborationStore`: users, permissions, threads and suggestions are loaded when the editor starts, and every
change is checked and saved on the server (authors and timestamps are set there, never taken from the browser). The bridge
is the add-on's custom plugin `VaadinCollaborationBridge`, combined with `CustomPlugin.fromPremium("Comments")` /
`fromPremium("TrackChanges")`. Setup, API, security rules and the verification status (mock-tested; real premium behaviour
needs a licensed key) are in
[Comments and Track Changes Without CKEditor Cloud](PREMIUM_INTEGRATION.md#comments-and-track-changes-without-ckeditor-cloud).

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
| `setCodeBlockHighlighting(boolean)` | Syntax highlighting in code blocks while editing (see [Code block syntax highlighting](#code-block-syntax-highlighting)) |
| `setCodeBlockHighlightTheme(CodeHighlightTheme)` | Highlighting colors (`AUTO`, `GITHUB`, `GITHUB_DARK`, `ATOM_ONE_LIGHT`, `ATOM_ONE_DARK`, `NONE`) |

#### Configuration

| Method | Description |
|--------|-------------|
| `setOverrideCssUrl(String)` | Set custom CSS URL |
| `setGeneralHtmlSupportEnabled(boolean)` | Allow all HTML via GHS, scripts included (prefer `CKEditorConfig.setHtmlSupport(HtmlSupport)`) |
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
| `addWordCountChangeListener(listener)` | Live word / character count |

#### Word Count and Validation

| Method | Description |
|--------|-------------|
| `setMaxWords(int)` / `setMaxCharacters(int)` | Limits; over them the field is invalid (0 = no limit) |
| `setMaxWordsErrorMessage(String)` / `setMaxCharactersErrorMessage(String)` | Messages, `{0}` = limit, `{1}` = count |
| `setWordCountVisible(boolean)` | Show CKEditor's counter below the editor |
| `getDefaultValidator()` | Limit validator used by `Binder` |
| `setManualValidation(boolean)` | Do not mark the field invalid automatically |

### VaadinCKEditorBuilder Methods

#### Convenience Methods

| Method | Description |
|--------|-------------|
| `withViewOnly()` | Sets readOnly + hideToolbar + READ_ONLY fallback mode |
| `withReadOnly(boolean)` | Alias for `readOnly(boolean)` |
| `withHideToolbar(boolean)` | Hide/show toolbar in built editor |
| `withMaxWords(int)` / `withMaxCharacters(int)` | Word / character limits |
| `withWordCountVisible(boolean)` | Show the word count below the editor |

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

**Editor inside a Vaadin `Dialog` (or any top-layer overlay):**

Vaadin 25 renders dialogs in the browser top layer, where `z-index` has no
effect. The connector detects this and sets CKEditor's `ui.overlayContainer`
to the `<vaadin-ckeditor>` element, so CKEditor mounts its floating UI (dialogs
such as Special Characters, balloons, tooltips) and the fullscreen container
inside the editor host; the connector then promotes both to the top layer.
No configuration is needed. If you set `ui.overlayContainer` yourself, the
connector leaves it alone.

1. Keep `FULLSCREEN` on its default container. If you set a custom
   `fullscreen.container` in `CKEditorConfig`, the connector respects it and
   does not intervene; fullscreen will then render behind a modal dialog.
2. `SPECIAL_CHARACTERS` automatically pulls in
   `SPECIAL_CHARACTERS_ESSENTIALS` when no category plugin is selected, so the
   panel is never empty. Add specific categories (`SPECIAL_CHARACTERS_LATIN`,
   `..._ARROWS`, …) to replace that default. In `STRICT` mode the missing
   category is reported as an error instead; in `MANUAL` mode you must add a
   category yourself.
3. If a floating panel still appears behind the dialog, check whether your
   application moves the `<vaadin-ckeditor>` element into or out of an overlay
   while the editor is alive. The top-layer detection runs each time an editor
   instance is created (including the automatic re-creation on reconnect); it
   does not watch for later ancestry changes, so such a move needs the editor
   to be re-created.

### Debug Mode

Enable debug logging in browser:

```javascript
window.VAADIN_CKEDITOR_DEBUG = true;
```

The flag is read once when the connector module loads, so set it **before** the
editor initialises — typically: set it, then reload the page.

### Manual Inspection from DevTools

When you need to inspect a live editor, select the `<vaadin-ckeditor>` element in
the Elements panel (it becomes `$0` in the Console) and work from there. The
component uses Light DOM, so nothing is hidden behind a shadow root.

```javascript
$0.editorId            // component id — matches setId() and the inner <div id>
$0.editor              // the live CKEditor 5 instance
$0.editor.getData()    // current content as HTML
$0.editor.setData('<p>hi</p>');
$0.editor.model        // CKEditor model, for deeper inspection
```

`editor` is declared `private` in TypeScript, but that is a compile-time
restriction only — the property is a plain field at runtime and is fully
accessible from the console, including in production builds.

To go the other way, from a known id to the instance:

```javascript
document.querySelector('vaadin-ckeditor#my-editor-id').editor;
```

On a page with several editors, list them all with their ids:

```javascript
[...document.querySelectorAll('vaadin-ckeditor')].map(h => [h.editorId, h.editor]);
```

Note that `editor.id` is **not** the component id. CKEditor assigns each instance
an internal identifier when it registers the editor in its own `Context`, and the
connector deliberately leaves that value alone — overwriting it corrupts
CKEditor's internal bookkeeping and makes `destroy()` recurse indefinitely.
Use `$0.editorId` to identify an editor; treat `editor.id` as CKEditor's own.

---

## Version History

| Version | CKEditor 5 | Vaadin | Notes           |
|---------|------------|--------|-----------------|
| 6.1.0   | 49.1.0 | 25.3.5 | All CKEditor UI languages loaded on demand, locale fallback, `withLocale(Locale)`, `config.language` precedence (#151); dark-theme split button & bookmark (#149/#150); Dialog dropdown clipping & overlay-container warning fixes |
| 6.0.0   | 49.1.0 | 25.3.5 | **Breaking:** CKEditor 49 (refreshed theme), `PremiumPlugin.REAL_TIME_COLLABORATION` / `TABLE_LAYOUT` removed; Dialog top-layer fixes (#143/#144/#145); `TABLE_LAYOUT`, `setEmoji`, runtime error events; Jackson 3 BOM imported explicitly |
| 5.4.0   | 48.5.0 | 25.2.6 | CKEditor 48.5.0; fix destroy() infinite recursion on reparent (issue #122); fix upload-adapter leak; DevTools inspection guide |
| 5.3.3   | 48.4.0 | 25.2.6 | Builder ErrorHandler, toolbar style round-trip, STRICT preset deps, editor remount/orphan fixes |
| 5.3.2   | 48.4.0 | 25.2.6 | Dependency upgrades (Vaadin 25.2.6, CKEditor 48.4.0) |
| 5.3.1   | 48.2.0 | 25.2.0 | Caret/focus API, resizable media embed, CKFinder; **breaking:** `LINE_HEIGHT` removed |
| 5.3.0   | 48.2.0 | 25.2.0 | Dependency upgrades (Vaadin 25.2.0, CKEditor 48.2.0) |
| 5.2.0   | 48.1.1 | 25.1.6 | Vaadin 25.1.6, CKEditor 48.1.1; Spring Boot 4.0.4+ recommended |
| 5.1.0   | 47.4.0 | 25.0.5 | EMAIL/NOTION presets, AI plugins, slimmed build |
| 5.0.5   | 47.4.0 | 25.0.5 | Premium plugins, upload improvements |
| 5.0.3   | 47.4.0 | 25.0.4 | CI/CD workflows |
| 5.0.1   | 47.4.0 | 25.0.4 | Bug fixes       |
| 5.0.0   | 47.4.0 | 25.0.3 | Initial v5 release |

---

## License

This addon is available under the Apache License 2.0.

CKEditor 5 is available under GPL 2+ or commercial license. See [CKEditor licensing](https://ckeditor.com/legal/ckeditor-oss-license/).
