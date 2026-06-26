# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Caret/focus control API on `VaadinCKEditor` (issue #52): `setCaretToStart()`,
  `setCaretToEnd()`, `focusEditor()`. Useful when leading block content (e.g. a
  table letterhead) would otherwise be auto-selected on focus — move the caret
  to the start so nothing is highlighted.
- Resizable embedded media (issue #71): `CKEditorConfig.setMediaEmbedResizable(true)`
  enables drag-to-resize handles on embedded media (videos). Backed by CKEditor's
  `MediaEmbedResize` plugin. The frontend loads it on demand from the `ckeditor5`
  umbrella package only when the flag is enabled; load failures (e.g. missing
  commercial license — see below) degrade silently. Requires `CKEditorPlugin.MEDIA_EMBED`.
- Free media-embed companion plugins, exported by the `ckeditor5` umbrella and now
  registered as built-in `CKEditorPlugin` constants:
  - `MEDIA_EMBED_STYLE` (`MediaEmbedStyle`) — alignment/styling for embedded media
    (`mediaEmbed:alignLeft` / `alignCenter` / `alignRight` toolbar items); the sibling
    feature of the resize support above. Not auto-loaded by `MEDIA_EMBED`, so it must
    be selected explicitly.
  - `MEDIA_EMBED_TOOLBAR` (`MediaEmbedToolbar`) — floating toolbar shown when an
    embedded media widget is selected (hosts the style/alignment buttons).
  - `AUTO_MEDIA_EMBED` (`AutoMediaEmbed`) — auto-converts pasted media links into embeds.
- `CKFINDER` (`CKFinder`) — CKFinder file-manager integration plugin. The plugin class
  itself is free (umbrella-exported); using it requires `config.ckfinder.uploadUrl` and
  a CKFinder server backend, so it is treated as a config-required plugin (filtered out
  of auto-select unless `setAllowConfigRequiredPlugins(true)` is set).
- `CKEditorConfig.setMediaEmbedToolbar(String...)` — fluent setter that writes media
  toolbar buttons to `config.mediaEmbed.toolbar` (where `MediaEmbedStyle`'s alignment
  buttons such as `mediaEmbed:alignLeft` must live, not the top-level `config.toolbar`).
  Empty/null input writes nothing (no empty-array noise).

### Changed
- `media-embed-resize.ts`: the on-demand `MediaEmbedResize` loader now imports from the
  `ckeditor5` umbrella package instead of the `@ckeditor/ckeditor5-media-embed` subpackage.
  Verified against the installed 48.2.0 artifacts: the umbrella re-exports the whole
  media-embed subpackage (`export * from '@ckeditor/ckeditor5-media-embed'`), so
  `import { MediaEmbedResize } from 'ckeditor5'` resolves. Importing from the umbrella
  (rather than mixing umbrella + subpackage entry points) avoids the risk of resolving
  two distinct class instances. Corrects the prior comment that claimed the plugin was
  "free" and "not umbrella-exported": it *is* umbrella-exported, but its
  `MediaEmbedResizeEditing` dependency has `isPremiumPlugin === true`, so loading it under
  a GPL license triggers CKEditor's license check — it is effectively a premium feature.

### Removed
- **Breaking:** `CKEditorPlugin.LINE_HEIGHT` removed from the free built-in plugin enum.
  `LineHeight` is a **premium** feature in CKEditor 48.x — it is not exported by the
  `ckeditor5` umbrella package (only by `ckeditor5-premium-features`, where its class
  reports `isPremiumPlugin === true`) and was never registered in the TypeScript
  `PLUGIN_REGISTRY`, so `withPlugins(CKEditorPlugin.LINE_HEIGHT)` produced a
  "Plugin not found in registry" error at runtime. The premium definition already exists
  as `VaadinCKEditorPremium.PremiumPlugin.LINE_HEIGHT`.
  - **Migration:** replace `withPlugins(CKEditorPlugin.LINE_HEIGHT)` with
    `addCustomPlugin(CustomPlugin.fromPremium("LineHeight"))` and configure a commercial
    license key.

## [5.3.0] - 2026-06-26

### Changed
- Vaadin Platform: 25.1.6 → 25.2.0 (root addon `pom.xml` + `examples/spring-boot-sample/pom.xml`)
  - Companion `provided` dependencies verified against 25.2.0's `flow-server` BOM
    (`jakarta.servlet-api` 6.1.0 matches and is kept).
- CKEditor 5 (`ckeditor5`, `ckeditor5-premium-features`): 48.1.1 → 48.2.0
  - `package-lock.json` regenerated; `tsc --noEmit` clean — no premium AI `.d.ts`
    type-contract drift; no source changes required; frontend vitest 114/114 green
  - `@NpmPackage` annotations on `VaadinCKEditor`/`VaadinCKEditorPremium` and
    `VaadinCKEditorPremium.getVersion()` synced to 48.2.0 (these tell consuming
    apps which npm version to install)
- Jackson databind (`tools.jackson.core`): 3.1.3 → 3.1.4
- `lit`: ^3.3.2 → ^3.3.3 (`package.json` + `@NpmPackage` annotation)
- Frontend test tooling: `vitest` 3.2.4 → 4.1.9, `@vitest/coverage-v8` → ^4.1.9
  (clears the dev-only vitest CVEs — `npm audit` now reports 0 vulnerabilities;
  `vi.fn()` spy types adjusted for vitest 4's stricter `Mock` typing)
- Addon version bumped to 5.3.0 and synced across all version references
  (`pom.xml`, sample `addon.version`, Java `VERSION`, `vaadin-ckeditor.ts` version,
  frontend `package.json`)

### Fixed
- `UploadManager` completion handler refactored: the upload `handle()` lambda
  (previously 5 indent levels) extracted into `processCompletion()` +
  `resolveFailureMessage()` with guard clauses (now ≤3 levels), removing
  duplicated failure-message logic
- `vaadin-ckeditor.ts`: collapsed 4 duplicated `try/catch` listener-removal blocks
  into a single `safeOff()` helper

## [5.2.0] - 2026-05-27

### Changed
- Vaadin Platform: 25.0.5 → 25.1.6
- CKEditor 5 (`ckeditor5`, `ckeditor5-premium-features`): 47.5.0 → 48.1.1
- Jackson databind (`tools.jackson.core`): 3.0.3 → 3.1.3
- Jakarta Servlet API: 6.0.0 → 6.1.0
- JUnit Jupiter: 5.11.4 → 6.0.3 (PR #80 by @mstahv — forward compatible with Vaadin 25.2-SNAPSHOT)
- `EditorConstructor.create(element, config)` migrated to CKEditor 48 single-argument create config:
  - `ClassicEditor` uses top-level `config.attachTo` (v48 official contract)
  - `BalloonEditor` / `InlineEditor` / `DecoupledEditor` use `config.root.element`

### Added
- New `editor-config-normalizer.ts` module: pure-function CKEditor 47 → 48 config compatibility layer
  - `normalizeRootConfig` — auto-migrates top-level `initialData`/`placeholder`/`label` to `root.*`; `root.*` takes precedence over top-level with a dev-mode warning
  - `normalizeAIConfig48` — AI config migration:
    - `ai.chat.shortcuts[].check` → `commandId`
    - `ai.chat.models.modelSelectorAlwaysVisible` → `ai.models.showModelSelector`
    - `ai.chat.models` → `ai.models` (existing `ai.models` keys take precedence)
    - `ai.reviewMode.translations` → `ai.translate.languages`
    - `ai.quickActions.extraCommands[].type` `'CHAT'/'ACTION'` → `'chat'/'action'`
    - `ai.quickActions.extraCommands[].displayedPrompt` → `label` (action type drops `displayedPrompt`; chat type keeps both)
  - `buildCreateConfig` — constructs the v48 create config per `editorType`; defensively clears stale `attachTo`/`element` fields
  - `stripInitialDataIfChannelSeeded` — collaboration channel seed logic, dependency-injected via `ChannelInitialDataDeps` (storage / clock / callbacks)
  - `cloneConfig` — `structuredClone` fallback for configs containing non-cloneable values (functions / DOM nodes)
  - Dev mode (`window.VAADIN_CKEDITOR_DEBUG = true`) prints migration warnings
- `ckeditor5-premium-features.d.ts`: CKEditor 48 AI configuration type contracts
  - `AIChatController`, `AIContextItemType`, `AIChatShortcutType`, `AIQuickActionCommandType`
  - `AIQuickActionsExtraCommandConfig` refactored as discriminated union (`AIQuickActionsChatCommandConfig` + `AIQuickActionsActionCommandConfig`); chat commands require `displayedPrompt`, action commands use `label` only
- `theme-manager.ts` `DARK_THEME_VARS`: 10 new CKEditor 48 official `--ck-color-ai-*` dark-mode tokens
- `vitest.config.ts`: excludes `**/target/**` to avoid scanning Maven-copied frontend sources; regex alias resolves the premium CSS deep import path in test environments
- `test-mocks/empty.css`: stub for the premium CSS deep import under vitest
- End-to-end test suite under `e2e/` (NOT bundled in the published jar):
  - Spring Boot 4 + Vaadin 25.1.6 sample app at `examples/spring-boot-sample/` (7 routes covering 4 EditorTypes, dark theme, upload, collaboration seed)
  - Playwright 1.60.0 test suite — 18 functional tests + 10 visual regression baselines = 28 tests, Chromium + Firefox
  - GitHub Actions `e2e.yml` workflow runs the full suite on every PR + push to `main`

### Fixed
- Cleared 7 transitive npm dev-only vulnerabilities (rollup / vite / minimatch / postcss / picomatch / ws)
- Dependabot alert #41: bump `@playwright/test` 1.50.0 → 1.60.0 (high-severity CVE — Playwright < 1.55.1 downloaded browser binaries without TLS verification)

### Notes
- **Spring Boot 4.0.4+ is recommended** to align Jackson 3.1
- Top-level `initialData`/`placeholder`/`label` config keys are still accepted, but migrating to `root.*` is recommended
- Legacy AI config keys are auto-normalized to v48 field names at runtime — no consumer code changes required
- Test coverage at release: Java JUnit 488/488 + frontend vitest 114/114 + Playwright 28/28 (Chromium + Firefox) — all green

## [5.1.0] - 2026-02-13

### Added
- EMAIL preset - curated plugin set for email composition with Base64 upload adapter
- NOTION preset - Notion-style editing with block toolbar and collaboration-ready plugins
- AI premium plugins: AIChat, AIEditorIntegration, AIQuickActions, AIReviewMode, AITranslate
- AI sidebar with responsive layout, accessibility (inert attribute), and dark theme support
- BLOCK_TOOLBAR dependency on WIDGET and WIDGET_TOOLBAR_REPOSITORY
- CSS custom properties for AI sidebar sizing (`--ck-ai-sidebar-*`)
- Container query height support with `100cqh` fallback

### Changed
- Minimum Vaadin version: 25.0.5
- Slimmed pom.xml: removed legacy assembly profile, OSGi test infrastructure, snapshot repositories
- Release process: Maven Central via `central-publishing-maven-plugin` (replaces legacy Vaadin Directory zip)
- CI workflows streamlined

### Removed
- OSGi integration test (Pax Exam dependencies removed)
- Legacy `directory` Maven profile and assembly descriptors
- Snapshot/prerelease repository declarations

## [5.0.5] - 2025-02-13

### Added
- Premium plugin support and custom plugin builder
- Empty file upload validation - rejects zero-byte files with clear error message
- Upload abort race condition protection - prevents cancellation of wrong upload
- SSRF protection documentation with known limitations (decimal integer IP, DNS rebinding)

### Fixed
- Upload adapter abort() now safely handles edge case when no upload is in progress
- Debug logging added for empty file rejection and abort edge cases
- Code review issues across frontend, backend, and tests

## [5.0.3] - 2025-02-02

### Added
- GitHub Actions CI/CD workflows
  - CI: Build and test on PR/push to main
  - Publish: Automated release to Vaadin Directory on tag

## [5.0.2] - 2025-02-02

### Added
- OSGi integration tests using Pax Exam framework
- Upload timeout mechanism with configurable duration (default 5 minutes)

### Fixed
- VERSION constant synchronized to 5.0.2 in Java and TypeScript
- Internal package no longer exported in OSGi bundle
- Documentation version references updated

## [5.0.1] - 2025-02-02

### Fixed
- Minor documentation corrections

## [5.0.0] - 2025-02-02

### Added
- Complete rewrite of Vaadin CKEditor 5 integration
- New `VaadinCKEditorBuilder` with fluent API for editor configuration
- `withViewOnly()` convenience method for read-only display mode
- `withReadOnly(boolean)` method for toggling edit capability
- `withHideToolbar(boolean)` method for toolbar visibility control
- `EnumParser` utility for safe, locale-independent enum parsing
- `EventDispatcher` for centralized event handling
- `UploadManager` for async file upload with progress tracking and cancellation
- Support for all CKEditor 5 editor types: Classic, Balloon, Inline, Decoupled
- Auto theme support - syncs with Vaadin Lumo light/dark mode
- Comprehensive error handling with `EditorError` and `ErrorSeverity`
- Fallback modes for graceful degradation (READ_ONLY, DISABLED, HIDDEN)
- Builder parameter validation with clear error messages
- Language code validation (ISO 639-1 format)
- Toolbar configuration validation

### Changed
- Minimum Java version: 21
- Minimum Vaadin version: 24.x
- Package renamed from `vaadin-litelement-ckeditor` to `vaadin-ckeditor`
- Simplified API with builder pattern replacing constructor overloads
- Unified `Locale.ROOT` for all case conversions (i18n safe)

### Fixed
- Locale-sensitive string operations now use `Locale.ROOT`
- Upload cancellation properly interrupts underlying CompletableFuture
- CompletionException properly unwrapped to expose root cause

### Removed
- Legacy LitElement-based implementation
- Deprecated configuration methods
- Support for Vaadin 14-23

## [4.x and earlier]

See the [legacy repository](https://github.com/wontlost-ltd/vaadin-ckeditor/tree/v4-archived) for previous versions.

---

## Version Guidelines

- **MAJOR** (x.0.0): Breaking API changes, removed features
- **MINOR** (0.x.0): New features, backward compatible
- **PATCH** (0.0.x): Bug fixes, no API changes

[Unreleased]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.2.0...HEAD
[5.2.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.1.0...v5.2.0
[5.1.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.5...v5.1.0
[5.0.5]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.3...v5.0.5
[5.0.3]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.2...v5.0.3
[5.0.2]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.1...v5.0.2
[5.0.1]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.0...v5.0.1
[5.0.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/releases/tag/v5.0.0
