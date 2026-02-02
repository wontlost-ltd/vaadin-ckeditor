# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Empty file upload validation - rejects zero-byte files with clear error message
- Upload abort race condition protection - prevents cancellation of wrong upload
- SSRF protection documentation with known limitations (decimal integer IP, DNS rebinding)

### Fixed
- Upload adapter abort() now safely handles edge case when no upload is in progress
- Debug logging added for empty file rejection and abort edge cases

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

[Unreleased]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.3...HEAD
[5.0.3]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.2...v5.0.3
[5.0.2]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.1...v5.0.2
[5.0.1]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.0...v5.0.1
[5.0.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/releases/tag/v5.0.0
