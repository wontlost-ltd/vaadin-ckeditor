package com.wontlost.ckeditor;

import com.wontlost.ckeditor.event.*;
import com.wontlost.ckeditor.event.EditorErrorEvent.EditorError;
import com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity;
import com.wontlost.ckeditor.event.FallbackEvent.FallbackMode;
import com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource;
import com.wontlost.ckeditor.handler.ErrorHandler;
import com.wontlost.ckeditor.handler.HtmlSanitizer;
import com.wontlost.ckeditor.handler.HtmlSanitizer.SanitizationPolicy;
import com.wontlost.ckeditor.handler.UploadHandler;
import com.wontlost.ckeditor.handler.UploadHandler.UploadContext;
import com.wontlost.ckeditor.handler.UploadHandler.UploadResult;
import com.wontlost.ckeditor.handler.UploadHandler.UploadConfig;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise events and handler API comprehensive tests.
 */
class EnterpriseEventTest {

    // ==================== EditorError Tests ====================

    @Nested
    @DisplayName("EditorError Tests")
    class EditorErrorTests {

        @Test
        @DisplayName("Test all error severities")
        void testAllErrorSeverities() {
            assertEquals(3, ErrorSeverity.values().length);
            assertNotNull(ErrorSeverity.valueOf("WARNING"));
            assertNotNull(ErrorSeverity.valueOf("ERROR"));
            assertNotNull(ErrorSeverity.valueOf("FATAL"));
        }

        @Test
        @DisplayName("Test EditorError construction and getters")
        void testEditorErrorConstruction() {
            EditorError error = new EditorError(
                "TEST_ERROR",
                "Test error message",
                ErrorSeverity.ERROR,
                true,
                "stack trace here"
            );

            assertEquals("TEST_ERROR", error.getCode());
            assertEquals("Test error message", error.getMessage());
            assertEquals(ErrorSeverity.ERROR, error.getSeverity());
            assertTrue(error.isRecoverable());
            assertEquals("stack trace here", error.getStackTrace());
        }

        @Test
        @DisplayName("Test EditorError toString")
        void testEditorErrorToString() {
            EditorError error = new EditorError("CODE", "msg", ErrorSeverity.WARNING, false, null);
            String str = error.toString();
            assertTrue(str.contains("CODE"));
            assertTrue(str.contains("WARNING"));
            assertTrue(str.contains("msg"));
        }

        @Test
        @DisplayName("Test non-recoverable fatal error")
        void testFatalNonRecoverableError() {
            EditorError error = new EditorError(
                "FATAL_ERROR",
                "Critical failure",
                ErrorSeverity.FATAL,
                false,
                "at line 1\nat line 2"
            );

            assertEquals(ErrorSeverity.FATAL, error.getSeverity());
            assertFalse(error.isRecoverable());
            assertNotNull(error.getStackTrace());
        }

        @Test
        @DisplayName("Test null stack trace")
        void testNullStackTrace() {
            EditorError error = new EditorError("E1", "msg", ErrorSeverity.ERROR, true, null);
            assertNull(error.getStackTrace());
        }
    }

    // ==================== FallbackMode Tests ====================

    @Nested
    @DisplayName("FallbackMode Tests")
    class FallbackModeTests {

        @Test
        @DisplayName("Test all FallbackMode values")
        void testAllFallbackModes() {
            assertEquals(4, FallbackMode.values().length);
            assertEquals("textarea", FallbackMode.TEXTAREA.getJsName());
            assertEquals("readonly", FallbackMode.READ_ONLY.getJsName());
            assertEquals("error", FallbackMode.ERROR_MESSAGE.getJsName());
            assertEquals("hidden", FallbackMode.HIDDEN.getJsName());
        }

        @Test
        @DisplayName("Test fromJsName valid parsing")
        void testFromJsNameValid() {
            assertEquals(FallbackMode.TEXTAREA, FallbackMode.fromJsName("textarea"));
            assertEquals(FallbackMode.READ_ONLY, FallbackMode.fromJsName("readonly"));
            assertEquals(FallbackMode.ERROR_MESSAGE, FallbackMode.fromJsName("error"));
            assertEquals(FallbackMode.HIDDEN, FallbackMode.fromJsName("hidden"));
        }

        @Test
        @DisplayName("Test fromJsName returns ERROR_MESSAGE for unknown values")
        void testFromJsNameUnknown() {
            assertEquals(FallbackMode.ERROR_MESSAGE, FallbackMode.fromJsName("unknown"));
            assertEquals(FallbackMode.ERROR_MESSAGE, FallbackMode.fromJsName(""));
            assertEquals(FallbackMode.ERROR_MESSAGE, FallbackMode.fromJsName(null));
        }
    }

    // ==================== ChangeSource Tests ====================

    @Nested
    @DisplayName("ChangeSource Tests")
    class ChangeSourceTests {

        @Test
        @DisplayName("Test all ChangeSource values")
        void testAllChangeSources() {
            assertEquals(6, ChangeSource.values().length);
            assertNotNull(ChangeSource.valueOf("USER_INPUT"));
            assertNotNull(ChangeSource.valueOf("API"));
            assertNotNull(ChangeSource.valueOf("UNDO_REDO"));
            assertNotNull(ChangeSource.valueOf("PASTE"));
            assertNotNull(ChangeSource.valueOf("COLLABORATION"));
            assertNotNull(ChangeSource.valueOf("UNKNOWN"));
        }
    }

    // ==================== ErrorHandler Tests ====================

    @Nested
    @DisplayName("ErrorHandler Tests")
    class ErrorHandlerTests {

        @Test
        @DisplayName("Test logging handler handles all severities")
        void testLoggingHandlerAllSeverities() {
            java.util.logging.Logger logger = java.util.logging.Logger.getLogger("test");
            ErrorHandler handler = ErrorHandler.logging(logger);

            EditorError warning = new EditorError("W1", "warning", ErrorSeverity.WARNING, true, null);
            assertFalse(handler.handleError(warning));

            EditorError error = new EditorError("E1", "error", ErrorSeverity.ERROR, false, null);
            assertFalse(handler.handleError(error));

            EditorError fatal = new EditorError("F1", "fatal", ErrorSeverity.FATAL, false, "trace");
            assertFalse(handler.handleError(fatal));
        }

        @Test
        @DisplayName("Test handler composition - first handles")
        void testComposeFirstHandles() {
            ErrorHandler handler1 = error -> true; // Handle all
            ErrorHandler handler2 = error -> {
                fail("Should not reach second handler");
                return false;
            };
            ErrorHandler composed = ErrorHandler.compose(handler1, handler2);

            EditorError error = new EditorError("E1", "msg", ErrorSeverity.ERROR, true, null);
            assertTrue(composed.handleError(error));
        }

        @Test
        @DisplayName("Test handler composition - passes to second")
        void testComposeSecondHandles() {
            AtomicBoolean firstCalled = new AtomicBoolean(false);
            AtomicBoolean secondCalled = new AtomicBoolean(false);

            ErrorHandler handler1 = error -> {
                firstCalled.set(true);
                return false;
            };
            ErrorHandler handler2 = error -> {
                secondCalled.set(true);
                return error.getCode().equals("HANDLED");
            };
            ErrorHandler composed = ErrorHandler.compose(handler1, handler2);

            EditorError handled = new EditorError("HANDLED", "msg", ErrorSeverity.ERROR, true, null);
            assertTrue(composed.handleError(handled));
            assertTrue(firstCalled.get());
            assertTrue(secondCalled.get());
        }

        @Test
        @DisplayName("Test handler composition - none handles")
        void testComposeNoneHandles() {
            ErrorHandler handler1 = error -> false;
            ErrorHandler handler2 = error -> false;
            ErrorHandler composed = ErrorHandler.compose(handler1, handler2);

            EditorError error = new EditorError("E1", "msg", ErrorSeverity.ERROR, true, null);
            assertFalse(composed.handleError(error));
        }

        @Test
        @DisplayName("Test empty handler array")
        void testComposeEmpty() {
            ErrorHandler composed = ErrorHandler.compose();
            EditorError error = new EditorError("E1", "msg", ErrorSeverity.ERROR, true, null);
            assertFalse(composed.handleError(error));
        }

        @Test
        @DisplayName("Test functional interface lambda")
        void testFunctionalInterface() {
            AtomicInteger callCount = new AtomicInteger(0);
            ErrorHandler handler = error -> {
                callCount.incrementAndGet();
                return error.getSeverity() == ErrorSeverity.WARNING;
            };

            EditorError warning = new EditorError("W1", "msg", ErrorSeverity.WARNING, true, null);
            assertTrue(handler.handleError(warning));
            assertEquals(1, callCount.get());

            EditorError error = new EditorError("E1", "msg", ErrorSeverity.ERROR, true, null);
            assertFalse(handler.handleError(error));
            assertEquals(2, callCount.get());
        }
    }

    // ==================== HtmlSanitizer Tests ====================

    @Nested
    @DisplayName("HtmlSanitizer Tests")
    class HtmlSanitizerTests {

        @Test
        @DisplayName("Test NONE policy - no sanitization")
        void testNonePolicy() {
            HtmlSanitizer none = HtmlSanitizer.withPolicy(SanitizationPolicy.NONE);
            String html = "<script>alert('xss')</script><p>text</p>";
            assertEquals(html, none.sanitize(html));
        }

        @Test
        @DisplayName("Test BASIC policy - removes scripts")
        void testBasicPolicy() {
            HtmlSanitizer basic = HtmlSanitizer.withPolicy(SanitizationPolicy.BASIC);
            String html = "<script>alert('xss')</script><p>text</p>";
            String result = basic.sanitize(html);
            assertFalse(result.contains("<script>"));
            assertTrue(result.contains("text"));
        }

        @Test
        @DisplayName("Test RELAXED policy")
        void testRelaxedPolicy() {
            HtmlSanitizer relaxed = HtmlSanitizer.withPolicy(SanitizationPolicy.RELAXED);
            String html = "<table><tr><td>cell</td></tr></table><script>bad</script>";
            String result = relaxed.sanitize(html);
            assertTrue(result.contains("<table>"));
            assertFalse(result.contains("<script>"));
        }

        @Test
        @DisplayName("Test STRICT policy - only basic formatting")
        void testStrictPolicy() {
            HtmlSanitizer strict = HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT);

            // Keep basic formatting tags
            String basic = "<p>paragraph</p><b>bold</b><i>italic</i>";
            String result = strict.sanitize(basic);
            assertTrue(result.contains("<p>"));
            assertTrue(result.contains("<b>"));
            assertTrue(result.contains("<i>"));

            // Remove div and attributes
            String withDiv = "<div class='test'><p>text</p></div>";
            result = strict.sanitize(withDiv);
            assertTrue(result.contains("<p>"));
            assertFalse(result.contains("<div"));
            assertFalse(result.contains("class"));

            // Keep headings
            String heading = "<h1>Title</h1><h2>Subtitle</h2>";
            result = strict.sanitize(heading);
            assertTrue(result.contains("<h1>"));
            assertTrue(result.contains("<h2>"));

            // Keep lists
            String list = "<ul><li>item</li></ul><ol><li>numbered</li></ol>";
            result = strict.sanitize(list);
            assertTrue(result.contains("<ul>"));
            assertTrue(result.contains("<ol>"));
            assertTrue(result.contains("<li>"));
        }

        @Test
        @DisplayName("Test empty input handling")
        void testEmptyInput() {
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT);
            assertEquals("", sanitizer.sanitize(null));
            assertEquals("", sanitizer.sanitize(""));
        }

        @Test
        @DisplayName("Test sanitizer chaining")
        void testChaining() {
            HtmlSanitizer first = html -> html.replace("a", "b");
            HtmlSanitizer second = html -> html.toUpperCase();
            HtmlSanitizer chained = first.andThen(second);

            assertEquals("BBCD", chained.sanitize("abcd"));
        }

        @Test
        @DisplayName("Test passthrough sanitizer")
        void testPassthrough() {
            HtmlSanitizer passthrough = HtmlSanitizer.passthrough();
            String html = "<script>test</script><div onclick='bad'>text</div>";
            assertEquals(html, passthrough.sanitize(html));
        }

        @Test
        @DisplayName("Test custom Safelist")
        void testCustomSafelist() {
            Safelist custom = new Safelist().addTags("custom", "special");
            HtmlSanitizer sanitizer = HtmlSanitizer.withSafelist(custom);

            String html = "<custom>allowed</custom><p>removed</p>";
            String result = sanitizer.sanitize(html);
            assertTrue(result.contains("<custom>"));
            assertFalse(result.contains("<p>"));
        }

        @Test
        @DisplayName("Test multiple chaining")
        void testMultipleChaining() {
            HtmlSanitizer s1 = html -> html + "1";
            HtmlSanitizer s2 = html -> html + "2";
            HtmlSanitizer s3 = html -> html + "3";

            HtmlSanitizer chained = s1.andThen(s2).andThen(s3);
            assertEquals("x123", chained.sanitize("x"));
        }
    }

    // ==================== UploadHandler Tests ====================

    @Nested
    @DisplayName("UploadHandler Tests")
    class UploadHandlerTests {

        @Test
        @DisplayName("Test UploadContext construction")
        void testUploadContext() {
            UploadContext context = new UploadContext("test.jpg", "image/jpeg", 1024);

            assertEquals("test.jpg", context.getFileName());
            assertEquals("image/jpeg", context.getMimeType());
            assertEquals(1024, context.getFileSize());
            assertTrue(context.isImage());
        }

        @Test
        @DisplayName("Test isImage method")
        void testIsImage() {
            assertTrue(new UploadContext("a.jpg", "image/jpeg", 100).isImage());
            assertTrue(new UploadContext("b.png", "image/png", 100).isImage());
            assertTrue(new UploadContext("c.gif", "image/gif", 100).isImage());
            assertTrue(new UploadContext("d.webp", "image/webp", 100).isImage());

            assertFalse(new UploadContext("e.pdf", "application/pdf", 100).isImage());
            assertFalse(new UploadContext("f.txt", "text/plain", 100).isImage());
            assertFalse(new UploadContext("g.doc", null, 100).isImage());
        }

        @Test
        @DisplayName("Test UploadResult success")
        void testUploadResultSuccess() {
            UploadResult result = new UploadResult("/uploads/image.jpg");

            assertTrue(result.isSuccess());
            assertEquals("/uploads/image.jpg", result.getUrl());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Test UploadResult failure")
        void testUploadResultFailure() {
            UploadResult result = UploadResult.failure("File too large");

            assertFalse(result.isSuccess());
            assertNull(result.getUrl());
            assertEquals("File too large", result.getErrorMessage());
        }

        @Test
        @DisplayName("Test UploadConfig defaults")
        void testUploadConfigDefaults() {
            UploadConfig config = new UploadConfig();

            assertEquals(10 * 1024 * 1024, config.getMaxFileSize());
            assertArrayEquals(
                new String[]{"image/jpeg", "image/png", "image/gif", "image/webp"},
                config.getAllowedMimeTypes()
            );
        }

        @Test
        @DisplayName("Test UploadConfig chaining")
        void testUploadConfigChaining() {
            UploadConfig config = new UploadConfig()
                .setMaxFileSize(5 * 1024 * 1024)
                .setAllowedMimeTypes("image/jpeg", "image/png");

            assertEquals(5 * 1024 * 1024, config.getMaxFileSize());
            assertArrayEquals(new String[]{"image/jpeg", "image/png"}, config.getAllowedMimeTypes());
        }

        @Test
        @DisplayName("Test UploadConfig validation - pass")
        void testUploadConfigValidatePass() {
            UploadConfig config = new UploadConfig();
            UploadContext context = new UploadContext("test.jpg", "image/jpeg", 1024);

            assertNull(config.validate(context));
        }

        @Test
        @DisplayName("Test UploadConfig validation - file too large")
        void testUploadConfigValidateFileTooLarge() {
            UploadConfig config = new UploadConfig().setMaxFileSize(100);
            UploadContext context = new UploadContext("test.jpg", "image/jpeg", 1024);

            String error = config.validate(context);
            assertNotNull(error);
            assertTrue(error.contains("1024"));
            assertTrue(error.contains("100"));
        }

        @Test
        @DisplayName("Test UploadConfig validation - MIME type not allowed")
        void testUploadConfigValidateMimeNotAllowed() {
            UploadConfig config = new UploadConfig();
            UploadContext context = new UploadContext("test.pdf", "application/pdf", 1024);

            String error = config.validate(context);
            assertNotNull(error);
            assertTrue(error.contains("application/pdf"));
        }

        @Test
        @DisplayName("Test UploadHandler functional interface")
        void testUploadHandlerFunctional() {
            UploadHandler handler = (context, stream) ->
                CompletableFuture.completedFuture(new UploadResult("/uploaded/" + context.getFileName()));

            UploadContext context = new UploadContext("image.png", "image/png", 500);
            ByteArrayInputStream stream = new ByteArrayInputStream(new byte[500]);

            CompletableFuture<UploadResult> future = handler.handleUpload(context, stream);
            UploadResult result = future.join();

            assertTrue(result.isSuccess());
            assertEquals("/uploaded/image.png", result.getUrl());
        }

        @Test
        @DisplayName("Test UploadHandler async failure")
        void testUploadHandlerAsyncFailure() {
            UploadHandler handler = (context, stream) ->
                CompletableFuture.completedFuture(UploadResult.failure("Upload failed"));

            UploadContext context = new UploadContext("test.jpg", "image/jpeg", 100);
            CompletableFuture<UploadResult> future = handler.handleUpload(context, null);
            UploadResult result = future.join();

            assertFalse(result.isSuccess());
            assertEquals("Upload failed", result.getErrorMessage());
        }
    }

    // ==================== VaadinCKEditor Builder Tests ====================

    @Nested
    @DisplayName("VaadinCKEditor Builder Enterprise Options Tests")
    class BuilderEnterpriseTests {

        @Test
        @DisplayName("Test withFallbackMode")
        void testWithFallbackMode() {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withFallbackMode(FallbackMode.READ_ONLY)
                .build();

            assertEquals(FallbackMode.READ_ONLY, editor.getFallbackMode());
        }

        @Test
        @DisplayName("Test withErrorHandler")
        void testWithErrorHandler() {
            ErrorHandler handler = error -> true;
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withErrorHandler(handler)
                .build();

            assertSame(handler, editor.getErrorHandler());
        }

        @Test
        @DisplayName("Test withHtmlSanitizer")
        void testWithHtmlSanitizer() {
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT);
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withHtmlSanitizer(sanitizer)
                .build();

            assertSame(sanitizer, editor.getHtmlSanitizer());
        }

        @Test
        @DisplayName("Test withUploadHandler")
        void testWithUploadHandler() {
            UploadHandler handler = (ctx, stream) -> CompletableFuture.completedFuture(new UploadResult("/test"));
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withUploadHandler(handler)
                .build();

            assertSame(handler, editor.getUploadHandler());
        }

        @Test
        @DisplayName("Test combining all enterprise options")
        void testAllEnterpriseOptions() {
            ErrorHandler errorHandler = error -> false;
            HtmlSanitizer sanitizer = HtmlSanitizer.passthrough();
            UploadHandler uploadHandler = (ctx, stream) -> CompletableFuture.completedFuture(new UploadResult("/url"));

            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withFallbackMode(FallbackMode.HIDDEN)
                .withErrorHandler(errorHandler)
                .withHtmlSanitizer(sanitizer)
                .withUploadHandler(uploadHandler)
                .build();

            assertEquals(FallbackMode.HIDDEN, editor.getFallbackMode());
            assertSame(errorHandler, editor.getErrorHandler());
            assertSame(sanitizer, editor.getHtmlSanitizer());
            assertSame(uploadHandler, editor.getUploadHandler());
        }

        @Test
        @DisplayName("Test setter methods")
        void testSetters() {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .build();

            // Test setters
            ErrorHandler handler = error -> true;
            editor.setErrorHandler(handler);
            assertSame(handler, editor.getErrorHandler());

            HtmlSanitizer sanitizer = HtmlSanitizer.passthrough();
            editor.setHtmlSanitizer(sanitizer);
            assertSame(sanitizer, editor.getHtmlSanitizer());

            UploadHandler uploadHandler = (ctx, stream) -> CompletableFuture.completedFuture(new UploadResult("/"));
            editor.setUploadHandler(uploadHandler);
            assertSame(uploadHandler, editor.getUploadHandler());

            editor.setFallbackMode(FallbackMode.ERROR_MESSAGE);
            assertEquals(FallbackMode.ERROR_MESSAGE, editor.getFallbackMode());
        }

        @Test
        @DisplayName("Test default values")
        void testDefaultValues() {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .build();

            assertEquals(FallbackMode.TEXTAREA, editor.getFallbackMode());
            assertNull(editor.getErrorHandler());
            assertNull(editor.getHtmlSanitizer());
            assertNull(editor.getUploadHandler());
        }
    }

    // ==================== Test Helper Methods ====================

    /**
     * Creates a VaadinCKEditor instance for testing.
     */
    private static VaadinCKEditor createTestEditor() {
        return VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .build();
    }

    // ==================== Safe Enum Parsing Tests ====================

    @Nested
    @DisplayName("Safe Enum Parsing Tests")
    class SafeEnumParsingTests {

        @Test
        @DisplayName("Test ErrorSeverity case insensitivity")
        void testErrorSeverityCaseInsensitive() {
            // Test various case combinations
            assertEquals(ErrorSeverity.WARNING, ErrorSeverity.valueOf("WARNING"));
            assertEquals(ErrorSeverity.ERROR, ErrorSeverity.valueOf("ERROR"));
            assertEquals(ErrorSeverity.FATAL, ErrorSeverity.valueOf("FATAL"));
        }

        @Test
        @DisplayName("Test all ChangeSource valid values")
        void testChangeSourceAllValues() {
            assertEquals(ChangeSource.USER_INPUT, ChangeSource.valueOf("USER_INPUT"));
            assertEquals(ChangeSource.API, ChangeSource.valueOf("API"));
            assertEquals(ChangeSource.UNDO_REDO, ChangeSource.valueOf("UNDO_REDO"));
            assertEquals(ChangeSource.PASTE, ChangeSource.valueOf("PASTE"));
            assertEquals(ChangeSource.COLLABORATION, ChangeSource.valueOf("COLLABORATION"));
            assertEquals(ChangeSource.UNKNOWN, ChangeSource.valueOf("UNKNOWN"));
        }

        @Test
        @DisplayName("Test invalid enum value throws exception")
        void testInvalidEnumThrows() {
            assertThrows(IllegalArgumentException.class, () -> ErrorSeverity.valueOf("INVALID"));
            assertThrows(IllegalArgumentException.class, () -> ChangeSource.valueOf("INVALID"));
        }
    }

    // ==================== HtmlSanitizer Integration Tests ====================

    @Nested
    @DisplayName("HtmlSanitizer Integration Tests")
    class HtmlSanitizerIntegrationTests {

        @Test
        @DisplayName("Test getSanitizedValue without sanitizer")
        void testGetSanitizedValueWithoutSanitizer() {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withValue("<script>alert('xss')</script><p>text</p>")
                .build();

            // No sanitizer set, returns original value
            assertEquals("<script>alert('xss')</script><p>text</p>", editor.getSanitizedValue());
        }

        @Test
        @DisplayName("Test getSanitizedValue with sanitizer")
        void testGetSanitizedValueWithSanitizer() {
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.BASIC);
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withValue("<script>alert('xss')</script><p>text</p>")
                .withHtmlSanitizer(sanitizer)
                .build();

            String sanitized = editor.getSanitizedValue();
            assertFalse(sanitized.contains("<script>"));
            assertTrue(sanitized.contains("text"));
        }

        @Test
        @DisplayName("Test getSanitizedValue with null value")
        void testGetSanitizedValueNull() {
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT);
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withHtmlSanitizer(sanitizer)
                .build();

            // Null value should return empty string or null
            String result = editor.getSanitizedValue();
            assertTrue(result == null || result.isEmpty());
        }

        @Test
        @DisplayName("Test STRICT policy removes dangerous tags")
        void testStrictPolicyRemovesDangerousTags() {
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT);
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withValue("<div onclick='bad'><p>safe</p><iframe src='evil'></iframe></div>")
                .withHtmlSanitizer(sanitizer)
                .build();

            String sanitized = editor.getSanitizedValue();
            assertFalse(sanitized.contains("onclick"));
            assertFalse(sanitized.contains("<iframe"));
            assertFalse(sanitized.contains("<div"));
            assertTrue(sanitized.contains("<p>"));
            assertTrue(sanitized.contains("safe"));
        }
    }

    // ==================== AutosaveEvent Tests ====================

    @Nested
    @DisplayName("AutosaveEvent Tests")
    class AutosaveEventTests {

        @Test
        @DisplayName("Test AutosaveEvent success scenario")
        void testAutosaveEventSuccess() {
            VaadinCKEditor editor = createTestEditor();
            AutosaveEvent event = new AutosaveEvent(editor, false, "<p>content</p>");

            assertTrue(event.isSuccess());
            assertEquals("<p>content</p>", event.getContent());
            assertNull(event.getErrorMessage());
            assertTrue(event.getTimestamp() > 0);
        }

        @Test
        @DisplayName("Test AutosaveEvent failure scenario")
        void testAutosaveEventFailure() {
            VaadinCKEditor editor = createTestEditor();
            AutosaveEvent event = new AutosaveEvent(editor, false, "<p>content</p>", false, "Save failed");

            assertFalse(event.isSuccess());
            assertEquals("<p>content</p>", event.getContent());
            assertEquals("Save failed", event.getErrorMessage());
        }

        @Test
        @DisplayName("Test AutosaveEvent timestamp")
        void testAutosaveEventTimestamp() {
            VaadinCKEditor editor = createTestEditor();
            long before = System.currentTimeMillis();
            AutosaveEvent event = new AutosaveEvent(editor, false, "content");
            long after = System.currentTimeMillis();

            assertTrue(event.getTimestamp() >= before);
            assertTrue(event.getTimestamp() <= after);
        }
    }

    // ==================== ContentChangeEvent Tests ====================

    @Nested
    @DisplayName("ContentChangeEvent Tests")
    class ContentChangeEventTests {

        @Test
        @DisplayName("Test hasChanged method")
        void testHasChanged() {
            VaadinCKEditor editor = createTestEditor();

            ContentChangeEvent changed = new ContentChangeEvent(editor, false, "old", "new", ChangeSource.USER_INPUT);
            assertTrue(changed.hasChanged());

            ContentChangeEvent notChanged = new ContentChangeEvent(editor, false, "same", "same", ChangeSource.USER_INPUT);
            assertFalse(notChanged.hasChanged());

            ContentChangeEvent fromNull = new ContentChangeEvent(editor, false, null, "new", ChangeSource.API);
            assertTrue(fromNull.hasChanged());

            ContentChangeEvent toNull = new ContentChangeEvent(editor, false, "old", null, ChangeSource.API);
            assertTrue(toNull.hasChanged());

            ContentChangeEvent bothNull = new ContentChangeEvent(editor, false, null, null, ChangeSource.UNKNOWN);
            assertFalse(bothNull.hasChanged());
        }

        @Test
        @DisplayName("Test getLengthDelta method")
        void testGetLengthDelta() {
            VaadinCKEditor editor = createTestEditor();

            ContentChangeEvent added = new ContentChangeEvent(editor, false, "ab", "abcd", ChangeSource.USER_INPUT);
            assertEquals(2, added.getLengthDelta());

            ContentChangeEvent removed = new ContentChangeEvent(editor, false, "abcd", "ab", ChangeSource.UNDO_REDO);
            assertEquals(-2, removed.getLengthDelta());

            ContentChangeEvent noChange = new ContentChangeEvent(editor, false, "same", "same", ChangeSource.PASTE);
            assertEquals(0, noChange.getLengthDelta());

            ContentChangeEvent fromNull = new ContentChangeEvent(editor, false, null, "new", ChangeSource.API);
            assertEquals(3, fromNull.getLengthDelta());

            ContentChangeEvent toNull = new ContentChangeEvent(editor, false, "old", null, ChangeSource.COLLABORATION);
            assertEquals(-3, toNull.getLengthDelta());
        }

        @Test
        @DisplayName("Test all ChangeSource values")
        void testAllChangeSources() {
            VaadinCKEditor editor = createTestEditor();
            for (ChangeSource source : ChangeSource.values()) {
                ContentChangeEvent event = new ContentChangeEvent(editor, false, "a", "b", source);
                assertEquals(source, event.getChangeSource());
            }
        }
    }

    // ==================== Collaborative Preset Tests ====================

    @Nested
    @DisplayName("COLLABORATIVE Preset Tests")
    class CollaborativePresetTests {

        @Test
        @DisplayName("Test COLLABORATIVE preset exists")
        void testCollaborativePresetExists() {
            assertNotNull(CKEditorPreset.COLLABORATIVE);
            assertEquals("Collaborative Editor", CKEditorPreset.COLLABORATIVE.getDisplayName());
        }

        @Test
        @DisplayName("Test COLLABORATIVE preset contains base plugins")
        void testCollaborativePresetHasBasePlugins() {
            CKEditorPreset preset = CKEditorPreset.COLLABORATIVE;

            // Core plugins
            assertTrue(preset.hasPlugin(CKEditorPlugin.ESSENTIALS));
            assertTrue(preset.hasPlugin(CKEditorPlugin.PARAGRAPH));
            assertTrue(preset.hasPlugin(CKEditorPlugin.UNDO));

            // Document features
            assertTrue(preset.hasPlugin(CKEditorPlugin.AUTOSAVE));
            assertTrue(preset.hasPlugin(CKEditorPlugin.WORD_COUNT));

            // Formatting
            assertTrue(preset.hasPlugin(CKEditorPlugin.BOLD));
            assertTrue(preset.hasPlugin(CKEditorPlugin.ITALIC));
            assertTrue(preset.hasPlugin(CKEditorPlugin.HEADING));
        }

        @Test
        @DisplayName("Test COLLABORATIVE preset has toolbar")
        void testCollaborativePresetHasToolbar() {
            String[] toolbar = CKEditorPreset.COLLABORATIVE.getDefaultToolbar();
            assertNotNull(toolbar);
            assertTrue(toolbar.length > 0);
        }

        @Test
        @DisplayName("Test COLLABORATIVE preset estimated size")
        void testCollaborativePresetSize() {
            int size = CKEditorPreset.COLLABORATIVE.getEstimatedSize();
            assertEquals(850, size);
        }

        @Test
        @DisplayName("Test creating editor with COLLABORATIVE preset")
        void testCreateEditorWithCollaborativePreset() {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.COLLABORATIVE)
                .build();

            assertNotNull(editor);
        }

        @Test
        @DisplayName("Test COLLABORATIVE preset with Premium plugins")
        void testCollaborativeWithPremiumPlugins() {
            // Test adding Premium collaboration plugins
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.COLLABORATIVE)
                .withLicenseKey("test-license-key")
                .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
                .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
                .build();

            assertNotNull(editor);
        }
    }
}
