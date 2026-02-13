# CKEditor 5 for Vaadin

A comprehensive CKEditor 5 rich text editor component for Vaadin 24+, with 70+ plugins, premium feature support, and seamless Lumo theme integration.

## Quick Start

```java
// One-liner with preset
VaadinCKEditor editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);

// Builder pattern for full control
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withType(CKEditorType.CLASSIC)
    .withTheme(CKEditorTheme.AUTO)
    .withAutosave(content -> save(content), 5000)
    .build();
```

## Features

- **70+ Built-in Plugins** — Text formatting, headings, images, tables, lists, code blocks, find & replace, word count, and more
- **4 Editor Types** — Classic, Inline, Balloon, and Decoupled
- **5 Presets** — BASIC, STANDARD, FULL, DOCUMENT, COLLABORATIVE — or pick plugins individually
- **Premium Plugin Support** — Export PDF/Word, Import Word, Comments, Track Changes, Real-Time Collaboration, AI Assistant (requires CKEditor license)
- **Custom Plugins** — Load third-party or local CKEditor 5 plugins via npm
- **Vaadin Theme Integration** — Auto-syncs with Lumo light/dark mode
- **Custom Toolbar Styling** — Fine-grained control over toolbar colors, button states, and per-button styles
- **File Upload** — Built-in upload handling with validation, progress tracking, and timeout control
- **HTML Sanitization** — Configurable policies (Strict, Basic, Relaxed) powered by jsoup
- **Autosave** — Debounced autosave with configurable intervals
- **Error Handling & Fallback** — Graceful degradation to textarea or read-only mode
- **Automatic Dependency Resolution** — Plugin dependencies resolved automatically
- **Builder Pattern API** — Fluent, type-safe configuration
- **i18n** — 10+ languages included

## Presets

| Preset | Description | Size |
|--------|-------------|------|
| `BASIC` | Text formatting, lists, links | ~300KB |
| `STANDARD` | + Images, tables, media, find/replace | ~600KB |
| `FULL` | + Fonts, colors, code blocks, highlighting | ~700KB |
| `DOCUMENT` | Professional document editing | ~800KB |
| `COLLABORATIVE` | Base for collaboration features | ~850KB |

## Editor Types

```java
VaadinCKEditor.create().withType(CKEditorType.CLASSIC).build();   // Fixed toolbar
VaadinCKEditor.create().withType(CKEditorType.BALLOON).build();   // Floating toolbar
VaadinCKEditor.create().withType(CKEditorType.INLINE).build();    // In-place editing
VaadinCKEditor.create().withType(CKEditorType.DECOUPLED).build(); // Separated toolbar
```

## Premium Features

Premium plugins require a CKEditor license from [ckeditor.com/pricing](https://ckeditor.com/pricing).

```java
VaadinCKEditorPremium.enable(); // Call once at startup

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.STANDARD)
    .withLicenseKey(licenseKey)
    .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
    .addCustomPlugin(CustomPlugin.fromPremium("ExportWord"))
    .build();
```

Available: ExportPdf, ExportWord, ImportWord, FormatPainter, Comments, TrackChanges, RevisionHistory, RealTimeCollaboration, AIAssistant, SlashCommand, Template, TableOfContents, CaseChange, Pagination, and more.

## Configuration

```java
vaadin.whitelisted-packages = com.wontlost
```

## Requirements

- Java 21+
- Vaadin 25+
- CKEditor 5 v47+

## Documentation

- [User Guide](https://github.com/wontlost-ltd/vaadin-ckeditor/blob/main/docs/USER_GUIDE.md) — Full documentation
- [Quick Reference](https://github.com/wontlost-ltd/vaadin-ckeditor/blob/main/docs/QUICK_REFERENCE.md) — Cheat sheet
- [Issues](https://github.com/wontlost-ltd/vaadin-ckeditor/issues) — Bug reports & feature requests

## License

Apache License 2.0. CKEditor 5 is dual-licensed (GPL 2+ / Commercial). Premium features require a commercial CKEditor license.
