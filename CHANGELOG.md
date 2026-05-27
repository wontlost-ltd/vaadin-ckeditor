# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- Vaadin Platform: 25.0.5 → 25.1.6
- CKEditor 5 (`ckeditor5`, `ckeditor5-premium-features`): 47.5.0 → 48.1.1
- Jackson databind (`tools.jackson.core`): 3.0.3 → 3.1.3
- Jakarta Servlet API: 6.0.0 → 6.1.0
- JUnit Jupiter: 5.11.4 → 6.0.3 (PR #80 by @mstahv — forward compatible with Vaadin 25.2-SNAPSHOT)
- `EditorConstructor.create(element, config)` 迁移到 CKEditor 48 单参数签名：
  - ClassicEditor 使用顶层 `config.attachTo`（v48 官方契约）
  - Balloon/Inline/Decoupled 使用 `config.root.element`

### Added
- 新增 `editor-config-normalizer.ts`：CKEditor 47 → 48 配置兼容层（纯函数模块，便于单测）
  - `normalizeRootConfig` — 顶层 `initialData`/`placeholder`/`label` 自动迁移到 `root.*`；`root.*` 优先于顶层并产生迁移警告
  - `normalizeAIConfig48` — AI 配置迁移：
    - `ai.chat.shortcuts[].check` → `commandId`
    - `ai.chat.models.modelSelectorAlwaysVisible` → `ai.models.showModelSelector`
    - `ai.chat.models` → `ai.models`（已有 `ai.models` 时优先保留）
    - `ai.reviewMode.translations` → `ai.translate.languages`
    - `ai.quickActions.extraCommands[].type` `'CHAT'/'ACTION'` → `'chat'/'action'`
    - `ai.quickActions.extraCommands[].displayedPrompt` → `label`（action 类型同时移除 displayedPrompt；chat 类型同时保留 displayedPrompt 与 label）
  - `buildCreateConfig` — 按 editorType 构造 v48 create 配置；主动清理残留 attachTo/element 字段
  - `stripInitialDataIfChannelSeeded` — 协作模式 channel seed 逻辑，依赖通过 `ChannelInitialDataDeps` 注入（storage / now / 回调）
  - `cloneConfig` — `structuredClone` 失败时回退到 ai 子树深拷贝，兼容含 function/DOM 等非 JSON 值的用户配置
  - Dev 模式（`window.VAADIN_CKEDITOR_DEBUG = true`）打印迁移警告
- `ckeditor5-premium-features.d.ts`：新增 CKEditor 48 AI 配置类型契约
  - `AIChatController`、`AIContextItemType`、`AIChatShortcutType`、`AIQuickActionCommandType`
  - `AIQuickActionsExtraCommandConfig` 重构为判别联合（`AIQuickActionsChatCommandConfig` + `AIQuickActionsActionCommandConfig`）；chat 必填 `displayedPrompt`，action 仅用 `label`
- `theme-manager.ts` DARK_THEME_VARS：新增 10 个 CKEditor 48 官方 `--ck-color-ai-*` 暗色 token 映射
- `vitest.config.ts`：排除 `**/target/**` 防止 mvn package 后扫描副本；新增 regex alias 解决 premium CSS 子路径解析
- `test-mocks/empty.css`：vitest 环境下 premium CSS 占位
- 测试覆盖：normalizer 30 + theme-manager 17 + plugin-resolver 26 + upload-adapter 29 + comment-permission-enforcer 12 = 114 个前端测试全部通过；Java 488 个单测全部通过

### Notes
- 推荐消费端 Spring Boot 4.0.4+ 以匹配 Jackson 3.1
- 顶层 `initialData`/`placeholder`/`label` 配置仍兼容，但建议迁移到 `root.*`
- 旧 AI 配置会被前端自动 normalize 到 v48 字段，无需手动迁移

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

[Unreleased]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.1.0...HEAD
[5.1.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.5...v5.1.0
[5.0.5]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.3...v5.0.5
[5.0.3]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.2...v5.0.3
[5.0.2]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.1...v5.0.2
[5.0.1]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.0...v5.0.1
[5.0.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/releases/tag/v5.0.0
