# ckeditor-vaadin-testbench

Vaadin **TestBench** element API for the [CKEditor 5 Vaadin integration](../README.md).
Provides `VaadinCKEditorElement` — a type-safe page object for driving the
`vaadin-ckeditor` component in end-to-end tests.

## ⚠️ Premium / License

[Vaadin TestBench](https://vaadin.com/docs/latest/flow/testing/end-to-end) is a
**commercial (Premium) feature** and requires a Vaadin subscription.

- This module depends on `com.vaadin:vaadin-testbench-core` with **`provided` scope** —
  it only needs the TestBench API at *compile time* and does **not** bundle TestBench at runtime.
- **You must bring your own TestBench dependency and license** to actually run tests.
- The core `ckeditor-vaadin` addon has **no** TestBench dependency; this optional module
  keeps the Premium dependency out of the free addon.

## Installation

```xml
<dependency>
    <groupId>com.wontlost</groupId>
    <artifactId>ckeditor-vaadin-testbench</artifactId>
    <version>5.3.1</version>
    <scope>test</scope>
</dependency>
```

You also need TestBench itself on your test classpath (provided by your Vaadin
subscription), e.g.:

```xml
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-testbench-core-junit5</artifactId>
    <scope>test</scope>
</dependency>
```

> Versions are managed by `vaadin-bom`; no explicit version is needed.

## Usage

```java
import com.wontlost.ckeditor.testbench.VaadinCKEditorElement;
import com.vaadin.testbench.TestBenchTestCase;

public class EditorIT extends TestBenchTestCase {

    @Test
    public void editsContent() {
        // locate the editor on the page
        VaadinCKEditorElement editor = $(VaadinCKEditorElement.class).first();

        // set and read content
        editor.setData("<p>Hello</p>");
        assertEquals("<p>Hello</p>", editor.getData());

        // insert text at the caret
        editor.setCaretToEnd();
        editor.insertText(" world");

        // toggle read-only
        editor.setReadOnly(true);
        assertTrue(editor.isReadOnly());

        // inspect configuration
        assertEquals("classic", editor.getEditorType());
    }
}
```

## API

`VaadinCKEditorElement extends com.vaadin.testbench.TestBenchElement`, bound to the
`vaadin-ckeditor` tag via `@Element("vaadin-ckeditor")`.

| Method | Backed by | Description |
|--------|-----------|-------------|
| `String getData()` | `executeScript` → CKEditor `getData()` | Live HTML content (falls back to the `editorData` property before the editor is ready) |
| `void setData(String html)` | `callFunction("updateData")` | Set **live** editor content. Does **not** sync the server-side `getValue()` (the update goes through the API-change path that intentionally skips `$server.setEditorData`). Assert browser content via `getData()`; for server value/Binder, drive real user input instead. |
| `void insertText(String text)` | `callFunction("insertText")` | Insert text at the caret |
| `void setReadOnly(boolean)` | `callFunction("setReadOnly")` | Toggle read-only |
| `boolean isReadOnly()` | `getPropertyBoolean("isReadOnly")` | Read-only state |
| `boolean isEnabled()` | `getPropertyBoolean("isEnabled")` | Enabled state |
| `void focusEditor()` | `callFunction("focusEditor")` | Focus the editable area |
| `void setCaretToStart()` / `setCaretToEnd()` | `callFunction(...)` | Move the caret |
| `String getEditorType()` | `getPropertyString("editorType")` | `classic` / `balloon` / `inline` / `decoupled` |
| `String getThemeType()` | `getPropertyString("themeType")` | `auto` / `light` / `dark` |
| `String getLanguage()` | `getPropertyString("language")` | UI language code |
| `String getFallbackMode()` | `getPropertyString("fallbackMode")` | `textarea` / `readonly` / `error` / `hidden` |

The component renders into light DOM, so TestBench can also query CKEditor's DOM inside
the element; prefer `getData()` over scraping rendered DOM for stable content assertions.

## Building

```bash
mvn -f ckeditor-vaadin-testbench/pom.xml compile
```

Compilation only needs the TestBench API (resolved as `provided`); **no license is
required to compile or distribute this module**. A license is required only to *run*
TestBench tests in a consuming project.
