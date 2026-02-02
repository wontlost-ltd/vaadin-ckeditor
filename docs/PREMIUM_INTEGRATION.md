# CKEditor Premium Integration Guide

This guide covers integrating CKEditor 5 premium features with Vaadin.

## Overview

CKEditor 5 premium features require:
1. A valid license key from [ckeditor.com/pricing](https://ckeditor.com/pricing)
2. The `ckeditor5-premium-features` npm package
3. Configuration in your Vaadin application

## Pricing Tiers

| Plan | Price | Best For |
|------|-------|----------|
| **Free** | $0/month | Basic editing, open-source projects |
| **Essential** | $144/month | Small projects, productivity tools |
| **Professional** | $405/month | Collaboration, document conversion |
| **Enterprise** | Custom | Large deployments, SLA, dedicated support |

All plans include a **14-day free trial** with full access.

## Quick Setup

### 1. Get a License Key

1. Visit [ckeditor.com/pricing](https://ckeditor.com/pricing)
2. Start a free trial or purchase a plan
3. Copy your license key from the dashboard

### 2. Install Premium Package

In your Vaadin project's `frontend/` directory:

```bash
cd frontend
npm install ckeditor5-premium-features
```

Or add to your `package.json`:

```json
{
  "dependencies": {
    "ckeditor5-premium-features": "^47.0.0"
  }
}
```

### 3. Configure Your Editor

```java
import com.wontlost.ckeditor.*;

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey("eyJhbGciOiJSUzI1NiIsInR5cCI...")  // Your license key
    .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
    .addCustomPlugin(CustomPlugin.fromPremium("ExportWord"))
    .withToolbar(
        "heading", "|",
        "bold", "italic", "underline", "|",
        "exportPdf", "exportWord"
    )
    .build();
```

## Premium Plugins Reference

### Document Conversion

| Plugin | Class Name | Toolbar Item | Description |
|--------|------------|--------------|-------------|
| Export to PDF | `ExportPdf` | `exportPdf` | Generate PDF from editor content |
| Export to Word | `ExportWord` | `exportWord` | Generate DOCX file |
| Import from Word | `ImportWord` | `importWord` | Import DOCX content |

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(LICENSE_KEY)
    .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
    .addCustomPlugin(CustomPlugin.fromPremium("ExportWord"))
    .addCustomPlugin(CustomPlugin.fromPremium("ImportWord"))
    .withToolbar("exportPdf", "exportWord", "importWord", "|", "undo", "redo")
    .build();
```

### Collaboration

| Plugin | Class Name | Description |
|--------|------------|-------------|
| Comments | `Comments` | Inline commenting |
| Track Changes | `TrackChanges` | Revision tracking |
| Revision History | `RevisionHistory` | Version history |
| Real-time Collaboration | `RealTimeCollaboration` | Multi-user editing |

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(LICENSE_KEY)
    .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
    .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
    .addCustomPlugin(CustomPlugin.fromPremium("RevisionHistory"))
    .build();
```

### Productivity

| Plugin | Class Name | Toolbar Item | Description |
|--------|------------|--------------|-------------|
| Format Painter | `FormatPainter` | `formatPainter` | Copy and apply formatting |
| Slash Commands | `SlashCommand` | - | Type / to trigger commands |
| Templates | `Template` | `insertTemplate` | Predefined content blocks |
| Case Change | `CaseChange` | `caseChange` | Transform text case |
| Merge Fields | `MergeFields` | `insertMergeField` | Mail merge placeholders |

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(LICENSE_KEY)
    .addCustomPlugin(CustomPlugin.fromPremium("FormatPainter"))
    .addCustomPlugin(CustomPlugin.fromPremium("SlashCommand"))
    .addCustomPlugin(CustomPlugin.fromPremium("Template"))
    .build();
```

### Document Structure

| Plugin | Class Name | Description |
|--------|------------|-------------|
| Document Outline | `DocumentOutline` | Document structure panel |
| Table of Contents | `TableOfContents` | Auto-generated TOC |
| Pagination | `Pagination` | Page break management |

### AI Features

| Plugin | Class Name | Toolbar Item | Description |
|--------|------------|--------------|-------------|
| AI Assistant | `AIAssistant` | `aiAssistant` | AI-powered writing help |

> ⚠️ **Security Warning**: Never hardcode API keys in source code. Use environment variables or secure configuration.

```java
// Get API key from environment variable (recommended)
String openAiKey = System.getenv("OPENAI_API_KEY");

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(LICENSE_KEY)
    .addCustomPlugin(CustomPlugin.fromPremium("AIAssistant"))
    .withConfig(new CKEditorConfig()
        .withCustomConfig("ai", Map.of(
            "openAI", Map.of(
                "apiUrl", "https://api.openai.com/v1",
                "requestHeaders", Map.of(
                    "Authorization", "Bearer " + openAiKey
                )
            )
        ))
    )
    .build();
```

## Advanced Configuration

### Export PDF Configuration

```java
CKEditorConfig config = new CKEditorConfig()
    .withCustomConfig("exportPdf", Map.of(
        "fileName", "document.pdf",
        "converterOptions", Map.of(
            "format", "A4",
            "margin_top", "20mm",
            "margin_bottom", "20mm",
            "margin_left", "12mm",
            "margin_right", "12mm"
        )
    ));

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(LICENSE_KEY)
    .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
    .withConfig(config)
    .build();
```

### Real-Time Collaboration Setup

Real-time collaboration requires CKEditor Cloud Services:

```java
CKEditorConfig config = new CKEditorConfig()
    .withCustomConfig("cloudServices", Map.of(
        "tokenUrl", "https://your-server.com/ckeditor-token",
        "webSocketUrl", "wss://your-collaboration-server.com"
    ))
    .withCustomConfig("collaboration", Map.of(
        "channelId", "document-" + documentId
    ));

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(LICENSE_KEY)
    .addCustomPlugin(CustomPlugin.fromPremium("RealTimeCollaboration"))
    .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
    .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
    .withConfig(config)
    .build();
```

## Troubleshooting

### "License key is invalid"

- Verify your license key is copied correctly (no extra spaces)
- Check the license hasn't expired
- Ensure the domain matches your license

### "Cannot find module 'ckeditor5-premium-features'"

```bash
# In your frontend directory
npm install ckeditor5-premium-features

# Or if using pnpm
pnpm add ckeditor5-premium-features
```

### Premium plugin not loading

Check browser console for errors:
- Plugin name must match exactly (case-sensitive)
- Ensure the plugin is exported from `ckeditor5-premium-features`

```java
// Correct
CustomPlugin.fromPremium("ExportPdf")

// Wrong
CustomPlugin.fromPremium("exportPdf")
CustomPlugin.fromPremium("EXPORT_PDF")
```

## Best Practices

### 1. Store License Key Securely

```java
// Don't hardcode in source
.withLicenseKey(System.getenv("CKEDITOR_LICENSE_KEY"))

// Or use Vaadin configuration
.withLicenseKey(VaadinService.getCurrent()
    .getDeploymentConfiguration()
    .getStringProperty("ckeditor.licenseKey", "GPL"))
```

### 2. Lazy Load Premium Features

Only load premium features when needed to reduce bundle size:

```java
if (user.hasPremiumAccess()) {
    builder.addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"));
}
```

### 3. Handle License Errors Gracefully

```java
try {
    editor = VaadinCKEditor.create()
        .withLicenseKey(licenseKey)
        .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
        .build();
} catch (Exception e) {
    // Fallback to basic editor
    editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);
    Notification.show("Premium features unavailable");
}
```

## Getting Help

- **CKEditor Documentation**: [ckeditor.com/docs](https://ckeditor.com/docs/ckeditor5/latest/)
- **CKEditor Support**: [ckeditor.com/support](https://support.ckeditor.com/)
- **License Questions**: [ckeditor.com/license](https://ckeditor.com/docs/ckeditor5/latest/getting-started/licensing/license-key-and-activation.html)

## Professional Services

Need assistance with premium integration? We offer:

| Service | Description | Price |
|---------|-------------|-------|
| **Quick Setup** | Premium plugin configuration | $500 |
| **Full Integration** | Complete setup + custom styling | $2,000 |
| **Custom Development** | Bespoke features | $150/hour |
| **Support Plan** | Priority response | $500/month |

Contact: [service@wontlost.com]
