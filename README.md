[![Published on Vaadin Directory](https://img.shields.io/badge/Vaadin%20Directory-published-00b4f0.svg)](https://vaadin.com/directory/component/<owner><element>)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Feroself%2Fvaadin-litelement-ckeditor.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Feroself%2Fvaadin-litelement-ckeditor?ref=badge_shield)

# Vaadin CKEditor 5

A comprehensive CKEditor 5 integration for Vaadin 24+.

## Features

- **70+ Free Plugins** - All plugins from the official `ckeditor5` npm package
- **Premium Plugin Support** - Easy integration with `ckeditor5-premium-features`
- **Multiple Editor Types** - Classic, Inline, Balloon, and Decoupled editors
- **Theme Integration** - Auto-sync with Vaadin's Lumo theme (light/dark)
- **Custom Toolbar Styling** - Fine-grained control over toolbar and button appearance
- **File Upload Handling** - Built-in upload with validation and progress tracking
- **Error Handling** - Comprehensive error events and fallback modes
- **HTML Sanitization** - Built-in sanitization policies for security
- **Autosave** - Debounced autosave with configurable intervals
- **i18n Support** - 10+ languages included
- **TypeScript** - Fully typed frontend component

## Documentation

- **[User Guide](docs/USER_GUIDE.md)** - Comprehensive documentation with all features
- **[Quick Reference](docs/QUICK_REFERENCE.md)** - Cheat sheet for common tasks

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

```kotlin
implementation("com.wontlost:ckeditor-vaadin:5.1.0")
```

## Quick Start

### Basic Editor

```java
// Using preset
VaadinCKEditor editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);

// With builder
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withValue("<p>Hello World!</p>")
    .build();

add(editor);
```

### Available Presets

| Preset | Description | Estimated Size |
|--------|-------------|----------------|
| `EMPTY` | Minimal - only essentials | ~100KB |
| `BASIC` | Simple formatting (Bold, Italic, Lists, Links) | ~300KB |
| `STANDARD` | Common features (+ Headings, Images, Tables) | ~600KB |
| `FULL` | All formatting features | ~700KB |
| `DOCUMENT` | Professional document editing | ~800KB |
| `EMAIL` | Email composition with image upload | ~500KB |
| `NOTION` | Notion-style block editing | ~900KB |

### Custom Plugin Selection

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPlugins(
        CKEditorPlugin.ESSENTIALS,
        CKEditorPlugin.PARAGRAPH,
        CKEditorPlugin.BOLD,
        CKEditorPlugin.ITALIC,
        CKEditorPlugin.HEADING,
        CKEditorPlugin.LINK,
        CKEditorPlugin.IMAGE,
        CKEditorPlugin.TABLE
    )
    .withToolbar("heading", "|", "bold", "italic", "|", "link", "insertImage", "insertTable")
    .build();
```

### Editor Types

```java
// Classic (default)
VaadinCKEditor classic = VaadinCKEditor.create()
    .withType(CKEditorType.CLASSIC)
    .build();

// Inline - toolbar appears on selection
VaadinCKEditor inline = VaadinCKEditor.create()
    .withType(CKEditorType.INLINE)
    .build();

// Balloon - floating toolbar
VaadinCKEditor balloon = VaadinCKEditor.create()
    .withType(CKEditorType.BALLOON)
    .build();

// Decoupled - separate toolbar, supports minimap
VaadinCKEditor decoupled = VaadinCKEditor.create()
    .withType(CKEditorType.DECOUPLED)
    .build();
```

### Theme Integration

```java
// Auto (default) - syncs with Vaadin's Lumo theme
VaadinCKEditor editor = VaadinCKEditor.create()
    .withTheme(CKEditorTheme.AUTO)
    .build();

// Force dark theme
VaadinCKEditor darkEditor = VaadinCKEditor.create()
    .withTheme(CKEditorTheme.DARK)
    .build();
```

### Autosave

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withAutosave(content -> {
        // Save content to database
        saveToDatabase(content);
    }, 3000) // 3 second debounce
    .build();
```

### Read-Only Mode

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .readOnly(true)
    .build();

// Toggle at runtime
editor.setReadOnly(false);
```

---

## Premium Features

Premium features require a CKEditor license. Get yours at [ckeditor.com/pricing](https://ckeditor.com/pricing).

### Step 1: Enable Premium Package

**Option A: Use `VaadinCKEditorPremium` class (Recommended)**

Simply call `VaadinCKEditorPremium.enable()` once at application startup to automatically install the premium npm package:

```java
import com.wontlost.ckeditor.VaadinCKEditorPremium;

@SpringBootApplication
public class Application implements AppShellConfigurator {
    public static void main(String[] args) {
        VaadinCKEditorPremium.enable();  // Enable premium features
        SpringApplication.run(Application.class, args);
    }
}
```

**Option B: Manual npm installation**

In your Vaadin project's frontend directory:

```bash
npm install ckeditor5-premium-features
```

### Step 2: Configure License Key

> **🔐 Security Note**: CKEditor 5 license keys are designed to be used in frontend code. To protect your license key from unauthorized use, configure **Approved Hosts Whitelisting** in the [CKEditor Customer Portal](https://dashboard.ckeditor.com/):
> - Exact domains: `example.com`
> - Wildcard subdomains: `*.example.com`
> - IP addresses/ranges: `192.168.*.*`
>
> This ensures your license key only works on your approved domains.

```java
// Recommended: Use environment variable to avoid committing keys to version control
String licenseKey = System.getenv("CKEDITOR_LICENSE_KEY");

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withLicenseKey(licenseKey)
    .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
    .addCustomPlugin(CustomPlugin.fromPremium("ExportWord"))
    .addCustomPlugin(CustomPlugin.fromPremium("ImportWord"))
    .withToolbar("heading", "|", "bold", "italic", "|", "exportPdf", "exportWord")
    .build();
```

### Available Premium Plugins

| Plugin | Description | Toolbar Item |
|--------|-------------|--------------|
| `ExportPdf` | Export to PDF | `exportPdf` |
| `ExportWord` | Export to Word | `exportWord` |
| `ImportWord` | Import from Word | `importWord` |
| `FormatPainter` | Copy formatting | `formatPainter` |
| `SlashCommand` | Slash commands | - |
| `TableOfContents` | Table of contents | `tableOfContents` |
| `DocumentOutline` | Document outline | - |
| `Template` | Content templates | `insertTemplate` |
| `CaseChange` | Change text case | `caseChange` |
| `MergeFields` | Mail merge fields | `mergeFields` |
| `Pagination` | Page breaks | `pageBreak` |
| `AIAssistant` | AI writing assistant | `aiAssistant` |
| `AIChat` | AI chat sidebar | `toggleAi` |
| `AIQuickActions` | AI quick actions | `aiQuickActions` |
| `AIEditorIntegration` | AI editor integration | - |
| `AIReviewMode` | AI review mode | - |
| `AITranslate` | AI translation | - |

### Collaboration Features

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withLicenseKey("your-license-key")
    .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
    .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
    .addCustomPlugin(CustomPlugin.fromPremium("RevisionHistory"))
    .build();
```

---

## Custom Plugins

### Third-Party Plugin

```java
CustomPlugin myPlugin = CustomPlugin.builder("MyPlugin")
    .withImportPath("my-ckeditor-plugin")
    .withToolbarItems("myButton")
    .build();

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .addCustomPlugin(myPlugin)
    .build();
```

### Plugin from npm Package

```java
// First: npm install @example/ckeditor-plugin

CustomPlugin examplePlugin = CustomPlugin.builder("ExampleFeature")
    .withImportPath("@example/ckeditor-plugin")
    .withToolbarItems("exampleButton")
    .build();
```

---

## Configuration

### CKEditorConfig

```java
CKEditorConfig config = new CKEditorConfig()
    .withHeading(new String[]{"paragraph", "heading1", "heading2", "heading3"})
    .withFontSizes(new String[]{"tiny", "small", "default", "big", "huge"})
    .withFontFamilies(new String[]{"Arial", "Georgia", "Times New Roman"})
    .withImageUploadUrl("/api/upload")
    .withCustomConfig("placeholder", "Start typing...");

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withConfig(config)
    .build();
```

### General HTML Support (GHS)

Enable to preserve all HTML attributes:

```java
editor.setGhsEnabled(true);
```

### Minimap (Decoupled Editor)

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withType(CKEditorType.DECOUPLED)
    .withPreset(CKEditorPreset.FULL)
    .build();

editor.setMinimapEnabled(true);
```

### Custom Toolbar Styling

Customize the toolbar appearance with fine-grained control over colors and styles. Supports multi-instance isolation - each editor can have its own unique styling.

```java
CKEditorConfig config = new CKEditorConfig()
    .setToolbarStyle(ToolbarStyle.builder()
        // Toolbar container
        .background("#f8f9fa")
        .borderColor("#dee2e6")
        .borderRadius("8px")
        // Button states
        .buttonBackground("transparent")
        .buttonHoverBackground("rgba(0, 0, 0, 0.05)")
        .buttonActiveBackground("rgba(0, 0, 0, 0.1)")
        .buttonOnBackground("#e3f2fd")
        .buttonOnColor("#1976d2")
        // Icon color
        .iconColor("#495057")
        .build());

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withConfig(config)
    .build();
```

#### Per-Button Styling

Apply custom styles to individual toolbar buttons:

```java
CKEditorConfig config = new CKEditorConfig()
    .setToolbarStyle(ToolbarStyle.builder()
        .background("#ffffff")
        // Highlight the Bold button
        .buttonStyle("Bold", ButtonStyle.builder()
            .background("#fff3e0")
            .hoverBackground("#ffe0b2")
            .iconColor("#e65100")
            .build())
        // Highlight the Italic button
        .buttonStyle("Italic", ButtonStyle.builder()
            .background("#e3f2fd")
            .hoverBackground("#bbdefb")
            .iconColor("#1565c0")
            .build())
        .build());
```

#### Available Style Properties

**ToolbarStyle**:
| Property | Description |
|----------|-------------|
| `background` | Toolbar background color |
| `borderColor` | Toolbar border color |
| `borderRadius` | Toolbar border radius (e.g., "8px") |
| `buttonBackground` | Default button background |
| `buttonHoverBackground` | Button hover state background |
| `buttonActiveBackground` | Button active/pressed state background |
| `buttonOnBackground` | Button "on" state background (e.g., Bold when active) |
| `buttonOnColor` | Button "on" state text/icon color |
| `iconColor` | Default icon color |

**ButtonStyle** (for individual buttons):
| Property | Description |
|----------|-------------|
| `background` | Button background color |
| `hoverBackground` | Hover state background |
| `activeBackground` | Active/pressed state background |
| `iconColor` | Icon color |

---

## API Reference

### Getting/Setting Content

```java
// Get content
String html = editor.getValue();

// Set content
editor.setValue("<p>New content</p>");

// Listen for changes
editor.addValueChangeListener(event -> {
    String newValue = event.getValue();
});
```

### Editor State

```java
// Read-only
editor.setReadOnly(true);
boolean isReadOnly = editor.isReadOnly();

// Focus
editor.focus();
```

---

## Dependency Management

The addon automatically resolves plugin dependencies:

```java
// AUTO_RESOLVE (default) - adds missing dependencies
VaadinCKEditor editor = VaadinCKEditor.create()
    .addPlugin(CKEditorPlugin.IMAGE_CAPTION) // IMAGE added automatically
    .build();

// STRICT - fails if dependencies missing
VaadinCKEditor editor = VaadinCKEditor.create()
    .addPlugin(CKEditorPlugin.IMAGE_CAPTION)
    .withDependencyMode(DependencyMode.STRICT)
    .build(); // Throws exception

// MANUAL - no checking
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPlugins(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.BOLD)
    .withDependencyMode(DependencyMode.MANUAL)
    .build();
```

---

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)


---

## Support

- **Issues**: [GitHub Issues](https://github.com/wontlost-ltd/vaadin-ckeditor/issues)
- **CKEditor Docs**: [ckeditor.com/docs](https://ckeditor.com/docs/ckeditor5/latest/)
- **Vaadin**: [vaadin.com](https://vaadin.com)

---

## Professional Services

Need help integrating CKEditor premium features? We offer:

- **Setup & Configuration** - Premium plugin integration
- **Custom Development** - Bespoke editor features
- **Training** - Team onboarding sessions
- **Support Plans** - Priority issue resolution

Contact: [service@wontlost.com]

---

## Complete Documentation

For comprehensive documentation including all configuration options, events, and API details, see:

- **[User Guide](docs/USER_GUIDE.md)** - Full documentation
- **[Quick Reference](docs/QUICK_REFERENCE.md)** - Cheat sheet
- **[Changelog](CHANGELOG.md)** - Version history
- **[Contributing](CONTRIBUTING.md)** - How to contribute
---

## License

This addon is licensed under Apache 2.0.

CKEditor 5 is dual-licensed:
- **GPL 2+** for open-source projects
- **Commercial license** for proprietary use

Premium features require a commercial license from [CKEditor](https://ckeditor.com/pricing).

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Feroself%2Fvaadin-litelement-ckeditor.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Feroself%2Fvaadin-litelement-ckeditor?ref=badge_large)

