# Contributing to Vaadin CKEditor

Thank you for your interest in contributing to Vaadin CKEditor! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Making Changes](#making-changes)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Release Process](#release-process)

## Code of Conduct

This project follows the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). By participating, you are expected to uphold this code.

## Getting Started

### Prerequisites

- **Java 21** or later
- **Maven 3.9+**
- **Node.js 18+** (for frontend resources)
- **Git**

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/vaadin-ckeditor.git
   cd vaadin-ckeditor
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/wontlost-ltd/vaadin-ckeditor.git
   ```

## Development Setup

### Build the Project

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Generate Javadoc

```bash
mvn javadoc:javadoc
```

### IDE Setup

**IntelliJ IDEA** (recommended):
1. Open the project directory
2. Import as Maven project
3. Enable annotation processing

**Eclipse**:
1. Import as existing Maven project
2. Or run `mvn eclipse:eclipse` and import

## Making Changes

### Branch Naming

Create a feature branch from `main`:

```bash
git checkout -b <type>/<short-description>
```

| Type | Description |
|------|-------------|
| `feat/` | New feature |
| `fix/` | Bug fix |
| `docs/` | Documentation only |
| `refactor/` | Code refactoring |
| `test/` | Adding tests |
| `chore/` | Build, tooling, dependencies |

Example: `feat/markdown-export` or `fix/null-pointer-upload`

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

**Examples**:
```
feat(builder): add withMarkdownSupport() method

fix(upload): handle null response from server

docs: update API reference for new events
```

### Keep Commits Atomic

- One logical change per commit
- Ensure each commit compiles and passes tests
- Squash WIP commits before submitting PR

## Coding Standards

### Java Style

- Follow existing code style in the project
- Use `Locale.ROOT` for case conversions (i18n safe)
- Prefer immutability where possible
- Document public APIs with Javadoc

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Classes | PascalCase | `VaadinCKEditor` |
| Methods | camelCase | `getValue()` |
| Constants | UPPER_SNAKE | `MAX_FILE_SIZE` |
| Packages | lowercase | `com.wontlost.ckeditor` |

### Code Organization

```
src/main/java/com/wontlost/ckeditor/
├── VaadinCKEditor.java          # Main component
├── VaadinCKEditorBuilder.java   # Builder API
├── CKEditorType.java            # Editor types enum
├── CKEditorTheme.java           # Theme enum
├── handler/                     # Event handlers
├── internal/                    # Internal utilities (not public API)
└── ...
```

### Internal vs Public API

- Classes in `internal/` package are not part of the public API
- Do not depend on internal classes in your code
- Public API changes require careful consideration

## Testing

### Test Requirements

- All new features must include unit tests
- Bug fixes should include regression tests
- Maintain or improve code coverage

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=VaadinCKEditorBuilderTest

# Run with coverage report (if JaCoCo configured)
mvn test jacoco:report
```

### Test Structure

```java
@Test
void methodName_condition_expectedBehavior() {
    // Given
    var builder = new VaadinCKEditorBuilder();

    // When
    var result = builder.withValue("<p>Test</p>").build();

    // Then
    assertEquals("<p>Test</p>", result.getValue());
}
```

## Submitting Changes

### Before Submitting

- [ ] Code compiles without warnings
- [ ] All tests pass
- [ ] New code has tests
- [ ] Javadoc added for public APIs
- [ ] CHANGELOG.md updated (for features/fixes)
- [ ] Commits are clean and atomic

### Pull Request Process

1. Push your branch to your fork:
   ```bash
   git push origin feat/your-feature
   ```

2. Open a Pull Request against `main` branch

3. Fill in the PR template:
   - **Summary**: What does this PR do?
   - **Motivation**: Why is this change needed?
   - **Test plan**: How was this tested?

4. Wait for review and address feedback

5. Once approved, maintainer will merge

### PR Title Format

Follow conventional commits:
```
feat(scope): add new feature
fix(scope): correct bug behavior
docs: update contributing guide
```

## Release Process

Releases are managed by maintainers:

1. Update version in `pom.xml`
2. Update `CHANGELOG.md`
3. Create release commit:
   ```bash
   git commit -m "chore: release v5.1.0"
   ```
4. Tag the release:
   ```bash
   git tag -a v5.1.0 -m "Release 5.1.0"
   ```
5. Push with tags:
   ```bash
   git push origin main --tags
   ```
6. The `publish.yml` GitHub Actions workflow automatically:
   - Publishes to Maven Central via `central-publishing-maven-plugin`
   - Creates a GitHub Release with JAR artifacts

## Questions?

- Open an [issue](https://github.com/wontlost-ltd/vaadin-ckeditor/issues) for bugs or feature requests
- Start a [discussion](https://github.com/wontlost-ltd/vaadin-ckeditor/discussions) for questions

---

Thank you for contributing!
