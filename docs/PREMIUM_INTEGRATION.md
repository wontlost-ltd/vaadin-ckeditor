# CKEditor Premium Integration Guide

This guide covers integrating CKEditor 5 premium features with Vaadin.

## Overview

CKEditor 5 premium features require:
1. A valid license key from [ckeditor.com/pricing](https://ckeditor.com/pricing)
2. The `ckeditor5-premium-features` npm package
3. Configuration in your Vaadin application

## Licensing for self-hosted (npm) builds

This add-on installs CKEditor from npm and bundles it into your Vaadin application. CKEditor calls this the
**self-hosted** distribution channel. Its [License key and activation](https://ckeditor.com/docs/ckeditor5/latest/getting-started/licensing/license-key-and-activation.html) guide says:

- Self-hosted: "This type requires contact with our sales team and a custom plan."
- "The 'GPL' key works only with the self-hosted distribution (npm or ZIP download)."

What this means for the add-on:

- **Default: the `GPL` key.** `VaadinCKEditor` uses `licenseKey = "GPL"` unless you call `withLicenseKey(...)`.
- **Commercial keys need a Custom plan.** The Free, Essential and Professional plans are cloud-hosted on the
  [pricing page](https://ckeditor.com/pricing). In `@ckeditor/ckeditor5-core` 49.1.0 an npm build reports the
  distribution channel `sh` (self-hosted), and the editor is blocked when a key's `distributionChannel` does not
  include it. So for commercial use with this add-on you need a **Custom plan** that includes the features you use,
  or a 14-day trial key to evaluate them.
- **Plan columns below.** They say which CKEditor plan *includes* a feature, per the pricing page (2026-11-22). With
  this add-on, that plan must be a Custom plan. "Essential" or "Professional" in a column does not mean those plans'
  keys work here.

**Premium features exported by the `ckeditor5` package.** Some features CKEditor marks as premium are exported by the
open-source `ckeditor5` package (see [the table below](#premium-features-exported-by-the-ckeditor5-package)). Two facts
apply, and this guide draws no legal conclusion from them:

1. CKEditor's core skips plugin licence checks for the `'GPL'` key. In `@ckeditor/ckeditor5-core` 49.1.0 this is
   `checkPluginsAllowedByLicenseKey`, which returns at once for `licenseKey === "GPL"`. So these plugins run under the
   add-on's default key. With a commercial key, a plugin whose licence feature code the plan does not include is
   blocked.
2. CKEditor's feature docs badge them "Premium feature" ("Unlock this feature with selected CKEditor Plans"), and the
   pricing page lists them outside the Free plan. Commercial use needs the plan CKEditor states.

Read CKEditor's [licensing options](https://ckeditor.com/legal/ckeditor-licensing-options) and
[License key and activation](https://ckeditor.com/docs/ckeditor5/latest/getting-started/licensing/license-key-and-activation.html) to decide which licence applies to you.

Plugins from `ckeditor5-premium-features` (the catalogue below) are a different case: they need a commercial key.

## Plans

Plans as listed on [ckeditor.com/pricing](https://ckeditor.com/pricing) on 2026-11-22 (prices change; the pricing page
is authoritative). Every plan has a 14-day trial. **For this add-on (self-hosted) only the Custom plan applies**, see
above; the other plans are listed because the pricing page describes features in their terms.

| Plan | Price | Premium features |
|------|-------|------------------|
| **Free** | $0 | No premium plugins; Export to PDF / Word 5 conversions a month |
| **Essential** | $160/month ($144 billed annually) | Productivity, document and email features below; Export to PDF / Word 200 a month; CKEditor AI, CKBox or Image Optimizer (Uploadcare) as paid add-ons |
| **Professional** | $319/month ($289 billed annually) | Everything in Essential; Import from Word and Export to PDF / Word 1,000 a month; **Collaboration add-on** ($299/month: comments, track changes, revision history, real-time co-authoring) |
| **Custom** | Quote | Tailored plan: add-ons (CKEditor AI, Collaboration, CKBox, Image Optimizer, Editing Toolkit), Custom-only features such as Pagination, LTS edition, and the **self-hosted (npm) distribution this add-on uses**, as agreed in the quote |

## Quick Setup

### 1. Get a License Key

1. Visit [ckeditor.com/pricing](https://ckeditor.com/pricing)
2. Start a free trial or purchase a plan
3. Copy your license key from the dashboard

### 2. Enable Premium Features (Recommended)

**Option A: Use `VaadinCKEditorPremium` class (Recommended)**

The simplest way to enable premium features is to call `VaadinCKEditorPremium.enable()` once at application startup. This automatically installs the `ckeditor5-premium-features` npm package:

```java
import com.wontlost.ckeditor.VaadinCKEditorPremium;

@SpringBootApplication
public class Application implements AppShellConfigurator {
    public static void main(String[] args) {
        // Enable premium features - call once at startup
        VaadinCKEditorPremium.enable();
        SpringApplication.run(Application.class, args);
    }
}
```

**Option B: Manual npm installation**

Alternatively, install the package manually in your Vaadin project's `frontend/` directory:

```bash
cd frontend
npm install ckeditor5-premium-features
```

Or add to your `package.json`:

```json
{
  "dependencies": {
    "ckeditor5-premium-features": "49.1.0"
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

### 4. Using PremiumPlugin Enum (Type-Safe)

For type-safe premium plugin configuration, use the `PremiumPlugin` enum:

```java
import com.wontlost.ckeditor.*;
import com.wontlost.ckeditor.VaadinCKEditorPremium.PremiumPlugin;

VaadinCKEditor editor = VaadinCKEditor.create()
    .withPreset(CKEditorPreset.FULL)
    .withLicenseKey(System.getenv("CKEDITOR_LICENSE_KEY"))
    .addCustomPlugin(PremiumPlugin.EXPORT_PDF.toCustomPlugin())
    .addCustomPlugin(PremiumPlugin.EXPORT_WORD.toCustomPlugin())
    .addCustomPlugin(PremiumPlugin.FORMAT_PAINTER.toCustomPlugin())
    .build();
```

## Premium Plugin Catalogue

Every plugin below is loaded with `CustomPlugin.fromPremium("<Plugin>")` (or the type-safe
`VaadinCKEditorPremium.PremiumPlugin` enum) from `ckeditor5-premium-features` 49.1.0, and needs a commercial licence
key whose plan includes it. The **Included in** column names the plans that include the feature on
[ckeditor.com/pricing](https://ckeditor.com/pricing) (2026-11-22). With this self-hosted add-on, the key must be a
**Custom-plan** (or trial) key; see [Licensing for self-hosted (npm) builds](#licensing-for-self-hosted-npm-builds).
Dependencies the add-on adds automatically (for example CloudServices for the conversion and AI plugins) come from
`CKEditorPluginDependencies`.

### Document conversion

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `ExportPdf` | `exportPdf` | All plans (conversion quota per plan; unlimited on Custom) | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/converters/export-pdf.html) |
| `ExportWord` | `exportWord` | All plans (conversion quota per plan; unlimited on Custom) | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/converters/export-word.html) |
| `ImportWord` | `importWord` | Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/converters/import-word/import-word.html) |
| `PasteFromOfficeEnhanced` | - | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/pasting/paste-from-office-enhanced.html) |

### Productivity

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `FormatPainter` | `formatPainter` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/format-painter.html) |
| `CaseChange` | `caseChange` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/case-change.html) |
| `SlashCommand` | - | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/slash-commands.html) |
| `Template` | `insertTemplate` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/template.html) |
| `MergeFields` | `insertMergeField` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/merge-fields.html) |

### Document structure and formatting

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `TableOfContents` | `tableOfContents` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/table-of-contents.html) |
| `DocumentOutline` | - | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/document-outline.html) |
| `Footnotes` | `insertFootnote` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/footnotes.html) |
| `FootnotesProperties` | `footnotesStyle` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/footnotes.html) |
| `MultiLevelList` | `multiLevelList` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/lists/multi-level-lists.html) |
| `LineHeight` | `lineHeight` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/line-height.html) |
| `Pagination` | `previousPage`, `nextPage`, `pageNavigation` | Custom only ("Unlock this feature with the Custom Plan") | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/pagination/pagination.html) |

### Email and HTML

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `EmailConfigurationHelper` | - | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/email-editing/email-configuration-helper.html) |
| `ExportInlineStyles` | - | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/export-with-inline-styles.html) |
| `SourceEditingEnhanced` | `sourceEditingEnhanced` | Essential, Professional, Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/source-editing/source-editing-enhanced.html) |

### Images and files

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `Uploadcare` | `uploadcare` (config `uploadcare.pubkey`; the add-on adds Image, ImageUpload and ImageInsert, and Uploadcare loads PictureEditing itself) | Image Optimizer add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/file-management/uploadcare.html) |
| `UploadcareImageEdit` | `uploadcareImageEdit` (in `image.toolbar`) | Image Optimizer add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/file-management/uploadcare.html) |

### AI

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `AIChat` | `toggleAi` | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-chat.html) |
| `AIQuickActions` | `aiQuickActions` | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-actions.html) |
| `AIReviewMode` | - | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-review.html) |
| `AITranslate` | - | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-translate.html) |
| `AIEditorIntegration` | - | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-overview.html) |
| `AIChatBalloon` | - | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-chat.html) |
| `AIEditorConfig` | - | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-overview.html) |
| `AIAssistant (deprecated)` | `aiAssistant` | CKEditor AI add-on (Essential, Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/ai/ckeditor-ai-overview.html) |
| `ProposedChanges` | - | Editing Toolkit, a Custom-plan add-on ("Unlock this feature with the Custom Plan"). Load `DocumentCompare` too: the docs' setup is `plugins: [ DocumentCompare, ProposedChanges ]` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/editing-toolkit/compare/editing-toolkit-proposing-changes.html) |
| `DocumentCompare` | - | Editing Toolkit, Custom-plan add-on | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/editing-toolkit/compare/editing-toolkit-comparing-documents.html) |

### Collaboration

| Plugin | Toolbar item | Included in | Feature docs |
|--------|--------------|------|--------------|
| `Comments` | `comment` | Collaboration add-on (Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/comments/comments.html) |
| `TrackChanges` | `trackChanges` | Collaboration add-on (Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/track-changes/track-changes.html) |
| `TrackChangesData` | - | Collaboration add-on (Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/track-changes/track-changes-data.html) |
| `TrackChangesPreview` | - | Collaboration add-on (Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/track-changes/track-changes-preview.html) |
| `RevisionHistory` | `revisionHistory` | Collaboration add-on (Professional), Custom | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/revision-history/revision-history.html) |
| `RealTimeCollaborativeEditing` | - | Collaboration add-on (Professional), Custom + CKEditor Cloud Services | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/real-time-collaboration/real-time-collaboration.html) |
| `RealTimeCollaborativeComments` | - | Collaboration add-on (Professional), Custom + CKEditor Cloud Services | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/real-time-collaboration/real-time-collaboration.html) |
| `RealTimeCollaborativeTrackChanges` | - | Collaboration add-on (Professional), Custom + CKEditor Cloud Services | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/real-time-collaboration/real-time-collaboration.html) |
| `RealTimeCollaborativeRevisionHistory` | - | Collaboration add-on (Professional), Custom + CKEditor Cloud Services | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/real-time-collaboration/real-time-collaboration.html) |
| `RealTimeContentFetcher` | - | Collaboration add-on (Professional), Custom + CKEditor Cloud Services | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/real-time-collaboration/real-time-collaboration.html) |
| `PresenceList` | - | Collaboration add-on (Professional), Custom + CKEditor Cloud Services | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/real-time-collaboration/users-in-real-time-collaboration.html) |

### Premium features exported by the `ckeditor5` package

CKEditor's feature docs badge these as premium ("Unlock this feature with selected CKEditor Plans"), and the pricing
page lists them outside the Free plan. Each has a 14-day trial. They are exported by the open-source `ckeditor5` package
(the same package as the free features), not by `ckeditor5-premium-features`, so `fromPremium(...)` cannot load them;
the add-on loads them through `CKEditorPlugin` constants, where `isPremiumFeature()` returns true. Several are in the
add-on's presets: `STANDARD`, `FULL` and `DOCUMENT` include, among others, `IMAGE_RESIZE`, `PASTE_FROM_OFFICE`,
`FIND_AND_REPLACE`, `TABLE_PROPERTIES` and `BASE64_UPLOAD_ADAPTER`.

As explained in [Licensing for self-hosted (npm) builds](#licensing-for-self-hosted-npm-builds), these plugins run
under the `'GPL'` key, because CKEditor's core skips plugin licence checks for it. Commercial use needs the plan CKEditor
states. When they are used with the `GPL` key, the builder logs this once at INFO level, with a link to CKEditor's
licence guide.

**Plugins with a licence feature code** (`licenseFeatureCode` in `ckeditor5` 49.1.0; `premium-catalogue.test.ts`
checks this list against the installed package). The pricing page includes them in the Essential, Professional and
Custom plans; with this add-on, that means a Custom plan:

| Plugin | `CKEditorPlugin` | Licence feature code | Feature docs |
|--------|------------------|----------------------|--------------|
| `Bookmark` | `BOOKMARK` | `BKM` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/bookmarks.html) |
| `FindAndReplace` | `FIND_AND_REPLACE` | `FAR` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/find-and-replace.html) |
| `HtmlEmbed` | `HTML_EMBED` | `HE` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/html/html-embed.html) |
| `FullPage` | — (not yet a constant) | `FPH` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/html/full-page-html.html) |
| `ImageResize` | `IMAGE_RESIZE` | `IR` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/images/images-resizing.html) |
| `MediaEmbedResize` | — (`CKEditorConfig#setMediaEmbedResizable(true)`) | `MER` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/media-embed/media-embed-resize.html) |
| `ListProperties` | `LIST_PROPERTIES` | `LP` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/lists/lists-properties.html) |
| `PageBreak` | `PAGE_BREAK` | `PB` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/page-break.html) |
| `PasteFromOffice` | `PASTE_FROM_OFFICE` | `PFO` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/pasting/paste-from-office.html) |
| `RemoveFormat` | `REMOVE_FORMAT` | `RF` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/remove-format.html) |
| `RestrictedEditingMode` | `RESTRICTED_EDITING_MODE` | `RED` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/restricted-editing.html) |
| `SpecialCharacters` | `SPECIAL_CHARACTERS` | `SCH` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/special-characters.html) |
| `TableProperties`, `TableCellProperties` | `TABLE_PROPERTIES`, `TABLE_CELL_PROPERTIES` | `TCP` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/tables/tables-styling.html) |
| `TableColumnResize` | `TABLE_COLUMN_RESIZE` | `TCR` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/tables/tables-resize.html) |
| `TableLayout` | `TABLE_LAYOUT` | `TL` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/tables/layout-tables.html) |
| `Base64UploadAdapter` | `BASE64_UPLOAD_ADAPTER` | `B64A` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/images/image-upload/base64-upload-adapter.html) |
| `SimpleUploadAdapter` | `SIMPLE_UPLOAD_ADAPTER` | `SUA` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/images/image-upload/simple-upload-adapter.html) |
| `WordCount` | `WORD_COUNT` | `WC` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/word-count.html) |

**Paid services** (no licence feature code in the plugin; the service itself is paid):

| Plugin | `CKEditorPlugin` | What it needs | Feature docs |
|--------|------------------|---------------|--------------|
| `CKBox`, `CKBoxImageEdit` | `CKBOX`, `CKBOX_IMAGE_EDIT` (toolbar `ckbox`, `ckboxImageEdit`) | The paid CKBox add-on ("This is a premium feature"); "Using the self-hosted distribution channel (npm) requires having an appropriate commercial plan". Also `ckbox.tokenUrl` (or `cloudServices.tokenUrl`), and **the CKBox JS library**, see below. | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckbox.html) |
| `CKFinder` | `CKFINDER` (toolbar `ckfinder`) | The commercial CKFinder server; `ckfinder.uploadUrl` | [docs](https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckfinder.html) |

**CKBox needs the CKBox library.** The editor plugins only connect CKEditor to CKBox. The file manager itself is the
CKBox JS library. The plugin checks `window.CKBox` (`@ckeditor/ckeditor5-ckbox` 49.1.0) and does not register the `ckbox`
command and button when the library is missing. The [CKBox docs](https://ckeditor.com/docs/ckeditor5/latest/features/file-management/ckbox.html) give two ways to load it:

- **CDN.** Add `<script src="https://cdn.ckbox.io/ckbox/2.13.1/ckbox.js"></script>` before the editor loads. Pin a
  version; the docs advise strongly against `latest`. In Vaadin you can do this with `@JavaScript("https://cdn.ckbox.io/ckbox/2.13.1/ckbox.js")`
  on your `AppShellConfigurator` or a view, or with `UI.getCurrent().getPage().addJavaScript(...)`.
- **npm.** Run `npm install ckeditor5 ckbox`, then import `ckbox` and `ckbox/dist/styles/ckbox.css`. In Vaadin, add
  `@NpmPackage(value = "ckbox", version = "…")` and a `@JsModule` that imports them, and make sure `window.CKBox` is set
  before the editor is created.

> Before 6.2.0 the add-on listed CKBox among the `ckeditor5-premium-features` plugins, so it tried to load CKBox from a
> package that does not export it, and `CustomPlugin.fromPremium("CKBox")` never worked. CKBox is a premium feature
> exported by the open-source `ckeditor5` package: use `CKEditorPlugin.CKBOX`.

### Combining premium and custom plugins

Premium plugins, the add-on's own custom plugins and your custom plugins go into one plugin list. For example, comments
and track changes persisted by your server (see the next section) are the premium plugins plus the add-on's
`VaadinCollaborationBridge` custom plugin:

```java
VaadinCKEditor editor = VaadinCKEditor.create()
    .withType(CKEditorType.DECOUPLED)
    .withPreset(CKEditorPreset.DOCUMENT)
    .withLicenseKey(System.getenv("CKEDITOR_LICENSE_KEY"))
    .addCustomPlugin(CustomPlugin.fromPremium("Comments"))        // premium: ckeditor5-premium-features
    .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
    .addCustomPlugin(CustomPlugin.of("MyPlugin", "./my-plugin"))  // yours, registered with registerCKEditorPlugin
    .withCollaborationBridge(bridge)                              // the add-on's custom plugin "VaadinCollaborationBridge"
    .build();
```

## Comments and Track Changes Without CKEditor Cloud

`CollaborationBridge` (package `com.wontlost.ckeditor.collaboration`, since 6.2.0) persists comments and suggestions
through your server, using CKEditor's **adapter integration**. No CKEditor Cloud Services or real-time collaboration
is involved: each change is saved as it happens, through a Java interface you implement.

> **Verification status.** Comments and track changes are premium features: a licence key with the Collaboration
> add-on is required, and **no licence was used to build this feature**. It is written against the documented 49.x
> APIs and tested with mock plugins shaped like them. See [What is and isn't verified](#what-is-and-isnt-verified)
> before relying on it, and test it with your licence key.

### Setup

```java
// One store per application (it is "the server" all users of a document talk to)
CollaborationStore store = new MyJdbcCollaborationStore(dataSource);     // or InMemoryCollaborationStore for demos

// Per editor: the document and the logged-in user come from the server, never from the browser
CollaborationBridge bridge = CollaborationBridge.builder(store, "document-" + documentId,
        new CollaborationUser(user.getId(), user.getDisplayName(), user.getAvatarUrl()))
    .permissions(CollaborationPermission.DOCUMENT_WRITE, CollaborationPermission.COMMENT_WRITE)  // default: CKEditor's defaults
    .build();

VaadinCKEditor editor = VaadinCKEditor.create()
    .withType(CKEditorType.DECOUPLED)
    .withPreset(CKEditorPreset.DOCUMENT)
    .withLicenseKey(System.getenv("CKEDITOR_LICENSE_KEY"))
    .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
    .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
    .withToolbar("comment", "trackChanges", "|", "bold", "italic")
    .withCollaborationBridge(bridge)
    .withValue(document.getHtml())            // HTML saved earlier, with comment / suggestion markers
    .build();
editor.setAnnotationSidebarEnabled(true);     // DECOUPLED / BALLOON: sidebar for the comment threads
editor.addValueChangeListener(e -> document.setHtml(e.getValue()));  // save the HTML together with the store
```

`withCollaborationBridge` adds the add-on's custom plugin `VaadinCollaborationBridge`. This class's `@JsModule` registers
it in the connector's plugin registry, the same mechanism as `CustomPlugin` (see the [User Guide](USER_GUIDE.md#custom-plugins)).
When the editor starts, the plugin runs `afterInit()`, after every plugin's `init()` and before the content loads. It:

1. requests the users, the current user, the permissions, the comment threads and the suggestions from the bridge;
2. calls `Users#addUser` for each user and `Users#defineMe` for the current user;
3. calls `Permissions#setPermissions`;
4. sets `CommentsRepository#adapter` and preloads every thread with `addCommentThread({..., isFromAdapter: true})`;
5. sets `TrackChanges#adapter` and preloads every suggestion with `addSuggestion(...)`.

This is the order of the official integration guides: users, then comment threads, then suggestions.

Every later adapter call goes to the server through `$server.collaborationRequest`. The answer comes back as a
`vaadin-ckeditor-collaboration` DOM event. The browser waits 30 s by default (`Builder#requestTimeout`); then the
adapter promise rejects and CKEditor reports the error. If the start-up request fails, editor creation fails and the
add-on's fallback mode applies, so the editor never runs silently without persistence.

The bridge works with the comments plugin alone, with track changes (which loads comments), or with both.

**Content and markers.** With these plugins loaded, the editor's HTML (`getValue()`) contains
`<comment-start name="threadId">`/`<comment-end>` and `<suggestion-start name="insertion:id:authorId">`/`<suggestion-end>`
markers. Store that HTML next to the threads and suggestions and pass it back with `setValue`/`withValue`. If you
use an `HtmlSanitizer`, allow these elements. The author shown for a suggestion comes from the store, not from the
author id in a marker name.

**Threads (Java).** Store futures may complete on any thread. Results are delivered with `UI.access`, so a store that
completes asynchronously needs `@Push`, or polling, for the answer to reach the browser before it times out.
`InMemoryCollaborationStore` completes immediately.

### Java API

| Type | Role |
|------|------|
| `CollaborationStore` | Your persistence. Async (`CompletableFuture`) methods mirroring the adapters: `loadUsers`, `loadCommentThreads`, `loadSuggestions`, `getCommentThread`, `addCommentThread`, `updateCommentThread`, `resolveCommentThread`, `reopenCommentThread`, `removeCommentThread`, `addComment`, `updateComment`, `removeComment`, `getSuggestion`, `addSuggestion`, `updateSuggestion`. Every method receives a `CollaborationContext` (document id, user, permissions). |
| `InMemoryCollaborationStore` | Thread-safe reference implementation for demos and tests (`putUser` / `putCommentThread` / `putSuggestion` / `clear`) |
| `CollaborationBridge` | Policy enforcement point. Checks permissions and ownership, sets authors and server timestamps, maps JSON, then calls the store. `handle(operation, payload)` is public so other transports can reuse the checks. |
| `CollaborationUser`, `CollaborationPermission`, `CommentThreadData`, `CommentData`, `SuggestionData`, `CommentThreadUpdate`, `SuggestionUpdate`, `CollaborationException` | Data types. `CollaborationException` messages are shown to the user; other store failures are logged and reported as a generic error. |

**Security rules** enforced on the server for every call; the browser is untrusted:

- **Identity:** the user, document id and permissions are fixed when the bridge is built. Author ids, `createdAt`, `resolvedAt` and `resolvedBy` sent by the browser are ignored and set on the server.
- **Comments:** `addCommentThread`, `addComment` and `updateCommentThread` need `comment:write`. `comment:admin` implies it; `comment:modify_all` does not.
- **Own content:** editing or removing one's own comment needs `comment:write`; another user's comment needs `comment:modify_all`. `removeCommentThread` works on the user's own thread (`comment:write`) or any thread with `comment:admin`. A thread's author is fixed when it is created and does not change when comments are removed.
- **Resolving:** resolving or reopening a thread needs `comment:write` or `document:write`.
- **Suggestions:** `addSuggestion` and changing a suggestion's state (accept or discard) need `document:write`. `hasComments` and attribute updates need `comment:write` or `document:write`.
- **Split suggestions:** a suggestion split from another user's (`originalSuggestionId`) gets the original author, as the `TrackChangesAdapter` documentation requires.
- **Suggestion discussions:** the first comment on a suggestion arrives as `addComment` with the suggestion's id as `threadId`, and no `addCommentThread` call. The thread is created then.

`comment-permission-enforcer.ts`, the UI helper that hides the edit/remove menu on other users' comments, reads the
current user that the bridge defines. It leaves the menu visible when the server grants `comment:modify_all`.

### CKEditor APIs relied on (49.x)

| API | Signature used | Docs |
|-----|----------------|------|
| `CommentsRepository#adapter` | `addCommentThread(data) → Promise<{threadId, comments: [{commentId, createdAt}]}>`, `getCommentThread({threadId}) → Promise<thread \| null>`, `updateCommentThread(data) → Promise<void>`, `resolveCommentThread({threadId}) → Promise<{threadId, resolvedAt, resolvedBy}>`, `reopenCommentThread`, `removeCommentThread`, `addComment(data) → Promise<{commentId, createdAt}>`, `updateComment`, `removeComment` | [CommentsAdapter](https://ckeditor.com/docs/ckeditor5/latest/api/module_comments_comments_commentsrepository-CommentsAdapter.html) |
| `CommentsRepository#addCommentThread` | `({threadId, comments, context, resolvedAt, resolvedBy, unlinkedAt, attributes, isFromAdapter: true})` | [CommentsRepository](https://ckeditor.com/docs/ckeditor5/latest/api/module_comments_comments_commentsrepository-CommentsRepository.html) |
| `TrackChanges#adapter` | `getSuggestion(id) → Promise<SuggestionData>`, `addSuggestion(data) → Promise<SuggestionData>`, `updateSuggestion(id, data) → Promise<void>` | [TrackChangesAdapter](https://ckeditor.com/docs/ckeditor5/latest/api/module_track-changes_trackchanges-TrackChangesAdapter.html) |
| `TrackChanges#addSuggestion` | `({id, type, authorId, createdAt, hasComments, data, attributes, originalSuggestionId})` | [TrackChanges](https://ckeditor.com/docs/ckeditor5/latest/api/module_track-changes_trackchanges-TrackChanges.html) |
| `Users` | `addUser({id, name, avatar})`, `getUser(id)`, `defineMe(id)`, `me` | [Users](https://ckeditor.com/docs/ckeditor5/latest/api/module_collaboration-core_users-Users.html), [users guide](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/users.html) |
| `Permissions` | `setPermissions(permissions)` | [Permissions](https://ckeditor.com/docs/ckeditor5/latest/api/module_collaboration-core_permissions-Permissions.html) |

The signatures were checked against the `.d.ts` files of `ckeditor5-premium-features@49.1.0`:
`@ckeditor/ckeditor5-comments/dist/comments/commentsrepository.d.ts`, `@ckeditor/ckeditor5-track-changes/dist/trackchanges.d.ts`
and `@ckeditor/ckeditor5-collaboration-core/dist/users.d.ts` / `permissions.d.ts`.

The integration guides followed: [comments integration](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/comments/comments-integration.html)
("Adapter integration", and "A simple load and save integration" for the alternative) and
[track changes integration](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/track-changes/track-changes-integration.html).

**Revision history is not bridged.** Its adapter (`getRevision`, `updateRevisions(revisions, channelId)`, see the
[revision history integration](https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/revision-history/revision-history-integration.html))
stores whole document snapshots, and it was left out of this first version.

### What is and isn't verified

**Verified with mocks, no licence:**

- **Vitest** (`collaboration-bridge.test.ts`):
  - A real CKEditor core (`ClassicEditor`, jsdom) runs with mock `Users`, `Permissions`, `CommentsRepository` and `TrackChanges` plugins shaped like the 49.x API.
  - The plugin resolves through the add-on's custom-plugin registry.
  - The start-up load maps users, `me`, permissions, threads (`isFromAdapter: true`, `Date` values) and suggestions.
  - Every adapter method sends the expected operation and payload, without `authorId`, `createdAt` or `channelId`, and maps the result back (ISO strings to `Date`).
  - Server rejections, timeouts, invalid answers and editor destruction all reject the adapter promise.
  - The comment permission enforcer integrates with the bridge.
- **JUnit:**
  - `CollaborationBridgeTest` exercises the bridge with `InMemoryCollaborationStore`: server-assigned authors and times, own-comment and own-thread rules, every permission, split suggestions, suggestion threads, channel isolation, malformed requests, and serialization.
  - `CollaborationBridgeEditorTest` covers the editor wiring and error messages.
- **Playwright** (`collaboration-bridge.spec.ts`): the real connector and plugin talk to the real Java bridge over Vaadin RPC, in a sample built with `CKEDITOR_PREMIUM_STUB=1`. That build replaces `ckeditor5-premium-features` with stand-ins of the same API shape. The spec checks the start-up load, adding comments with a forged author, a rejected edit of another user's comment, a commenter denied suggestions, split-suggestion authorship, accepting a suggestion, thread creation and resolution, and data surviving a reload.

**Needs a licensed key to verify:**

- The real `Comments`, `TrackChanges`, `Users` and `Permissions` plugins accept the adapters and preloaded data, and call the adapter methods at the documented moments with the documented data (for example `updateCommentThread` on unlink, and `addComment` for suggestion threads).
- `afterInit` runs early enough for the real plugins, before the markers in the content are converted.
- Comment and suggestion markers survive the `getValue()` / `setValue()` round trip and resolve to the preloaded threads.
- The sidebar UI and the comment permission enforcer's DOM selectors work against the real annotation views.
- Accept and discard behave as expected. CKEditor calls no adapter method when a suggestion is accepted or discarded; it keeps the suggestion for undo, as the track changes integration guide describes.
- The license checks accept the combination of premium plugins plus a custom plugin.

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
    .addCustomPlugin(CustomPlugin.fromPremium("RealTimeCollaborativeEditing"))
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
