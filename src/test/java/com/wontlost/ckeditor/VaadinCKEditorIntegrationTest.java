package com.wontlost.ckeditor;

import com.wontlost.ckeditor.event.*;
import com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource;
import com.wontlost.ckeditor.event.EditorErrorEvent.EditorError;
import com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity;
import com.wontlost.ckeditor.event.FallbackEvent.FallbackMode;
import com.wontlost.ckeditor.handler.ErrorHandler;
import com.wontlost.ckeditor.handler.HtmlSanitizer;
import com.wontlost.ckeditor.handler.HtmlSanitizer.SanitizationPolicy;
import com.wontlost.ckeditor.internal.EventDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VaadinCKEditor integration tests.
 * Tests component lifecycle, event dispatching, state management, etc.
 */
class VaadinCKEditorIntegrationTest {

    private VaadinCKEditor editor;

    @BeforeEach
    void setUp() {
        editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .build();
    }

    // ==================== Component Lifecycle Tests ====================

    @Nested
    @DisplayName("Component Lifecycle Tests")
    class LifecycleTests {

        @Test
        @DisplayName("Editor should have correct initial state after creation")
        void testInitialState() {
            VaadinCKEditor newEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .build();

            assertNotNull(newEditor);
            assertEquals("", newEditor.getValue());
            assertFalse(newEditor.isReadOnly());
            assertEquals(FallbackMode.TEXTAREA, newEditor.getFallbackMode());
            assertNull(newEditor.getErrorHandler());
            assertNull(newEditor.getHtmlSanitizer());
            assertNull(newEditor.getUploadHandler());
        }

        @Test
        @DisplayName("Should return correct value after setting initial value")
        void testInitialValue() {
            VaadinCKEditor newEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withValue("<p>Hello World</p>")
                .build();

            assertEquals("<p>Hello World</p>", newEditor.getValue());
        }

        @Test
        @DisplayName("Listener stats should be zero after cleanup")
        void testCleanupListeners() {
            editor.addEditorReadyListener(event -> {});
            editor.addEditorErrorListener(event -> {});
            editor.addAutosaveListener(event -> {});

            EventDispatcher.ListenerStats statsBefore = editor.getListenerStats();
            assertEquals(3, statsBefore.total());

            editor.cleanupListeners();

            EventDispatcher.ListenerStats statsAfter = editor.getListenerStats();
            assertEquals(0, statsAfter.total());
        }
    }

    // ==================== Event Listener Registration Tests ====================

    @Nested
    @DisplayName("Event Listener Registration Tests")
    class ListenerRegistrationTests {

        @Test
        @DisplayName("Stats should be correct after registering listeners")
        void testListenerStats() {
            assertEquals(0, editor.getListenerStats().total());

            editor.addEditorReadyListener(event -> {});
            assertEquals(1, editor.getListenerStats().ready);
            assertEquals(1, editor.getListenerStats().total());

            editor.addEditorErrorListener(event -> {});
            assertEquals(1, editor.getListenerStats().error);
            assertEquals(2, editor.getListenerStats().total());

            editor.addAutosaveListener(event -> {});
            assertEquals(1, editor.getListenerStats().autosave);
            assertEquals(3, editor.getListenerStats().total());

            editor.addContentChangeListener(event -> {});
            assertEquals(1, editor.getListenerStats().contentChange);
            assertEquals(4, editor.getListenerStats().total());

            editor.addFallbackListener(event -> {});
            assertEquals(1, editor.getListenerStats().fallback);
            assertEquals(5, editor.getListenerStats().total());
        }

        @Test
        @DisplayName("Stats should decrease after removing listeners")
        void testListenerRemoval() {
            var reg1 = editor.addEditorReadyListener(event -> {});
            var reg2 = editor.addEditorErrorListener(event -> {});

            assertEquals(2, editor.getListenerStats().total());

            reg1.remove();
            assertEquals(1, editor.getListenerStats().total());
            assertEquals(0, editor.getListenerStats().ready);
            assertEquals(1, editor.getListenerStats().error);

            reg2.remove();
            assertEquals(0, editor.getListenerStats().total());
        }

        @Test
        @DisplayName("Multiple registrations of same type should accumulate")
        void testMultipleListeners() {
            editor.addEditorReadyListener(event -> {});
            editor.addEditorReadyListener(event -> {});
            editor.addEditorReadyListener(event -> {});

            assertEquals(3, editor.getListenerStats().ready);
            assertEquals(3, editor.getListenerStats().total());
        }
    }

    // ==================== Value Operations Tests ====================

    @Nested
    @DisplayName("Value Operations Tests")
    class ValueOperationsTests {

        @Test
        @DisplayName("setValue should update value")
        void testSetValue() {
            editor.setValue("<p>New content</p>");
            assertEquals("<p>New content</p>", editor.getValue());
        }

        @Test
        @DisplayName("setValue null should convert to empty string")
        void testSetValueNull() {
            editor.setValue("<p>content</p>");
            editor.setValue(null);
            assertEquals("", editor.getValue());
        }

        @Test
        @DisplayName("clear should empty content")
        void testClear() {
            editor.setValue("<p>Some content</p>");
            editor.clear();
            assertEquals("", editor.getValue());
        }

        @Test
        @DisplayName("getPlainText should extract plain text")
        void testGetPlainText() {
            editor.setValue("<p>Hello <b>World</b></p>");
            assertEquals("Hello World", editor.getPlainText());
        }

        @Test
        @DisplayName("getPlainText should return empty string for empty value")
        void testGetPlainTextEmpty() {
            editor.setValue("");
            assertEquals("", editor.getPlainText());
        }

        @Test
        @DisplayName("getSanitizedHtml should sanitize dangerous tags")
        void testGetSanitizedHtml() {
            editor.setValue("<p>Text</p><script>alert('xss')</script>");
            String sanitized = editor.getSanitizedHtml();
            assertTrue(sanitized.contains("Text"));
            assertFalse(sanitized.contains("<script>"));
        }

        @Test
        @DisplayName("issue #85: ValueChangeListener should receive new value via getValue() on client change")
        void testValueChangeListenerReceivesNewValueFromClient() {
            // 回归测试 issue #85：客户端输入触发 setModelValue 时，
            // 监听器内调用 event.getValue() 必须返回新内容而非旧内容。
            AtomicReference<String> capturedValue = new AtomicReference<>();
            editor.addValueChangeListener(event ->
                capturedValue.set(event.getValue()));

            // 模拟来自客户端的内容变更（@ClientCallable setEditorData 走的就是这条路径）
            editor.setModelValue("<p>typed by user</p>", true);

            // issue #85 的核心断言：监听器读到的必须是新内容，而非旧内容。
            assertEquals("<p>typed by user</p>", capturedValue.get(),
                "event.getValue() should return the new content, not the stale value");
            // getValue() 在事件之外也应保持一致
            assertEquals("<p>typed by user</p>", editor.getValue());
        }

        @Test
        @DisplayName("issue #85: ValueChangeListener should receive new value via getValue() on setValue")
        void testValueChangeListenerReceivesNewValueFromSetValue() {
            // 服务端 setValue 路径同样必须保证监听器读到新值。
            AtomicReference<String> capturedValue = new AtomicReference<>();
            editor.addValueChangeListener(event -> capturedValue.set(event.getValue()));

            editor.setValue("<p>set by server</p>");

            assertEquals("<p>set by server</p>", capturedValue.get(),
                "event.getValue() should return the new content on the setValue path");
            assertEquals("<p>set by server</p>", editor.getValue());
        }

        @Test
        @DisplayName("setModelValue should not fire event when value is unchanged")
        void testSetModelValueNoEventOnUnchanged() {
            editor.setValue("<p>same</p>");

            AtomicInteger fireCount = new AtomicInteger(0);
            editor.addValueChangeListener(event -> fireCount.incrementAndGet());

            // 设置相同的值不应触发事件
            editor.setModelValue("<p>same</p>", true);
            assertEquals(0, fireCount.get(), "no event should fire when value is unchanged");

            // 设置不同的值应触发一次事件
            editor.setModelValue("<p>different</p>", true);
            assertEquals(1, fireCount.get(), "exactly one event should fire on actual change");
        }

        @Test
        @DisplayName("issue #85: setValue(null) listener value must match final getValue()")
        void testSetValueNullListenerConsistency() {
            // P1 边界（审查发现）：setValue(null) 时，事件内读到的值
            // 必须与方法返回后 getValue() 保持一致，均为 ""，不得为 null。
            editor.setValue("<p>existing</p>");

            AtomicReference<String> capturedValue = new AtomicReference<>("SENTINEL");
            editor.addValueChangeListener(event -> capturedValue.set(event.getValue()));

            editor.setValue(null);

            assertEquals("", capturedValue.get(),
                "event.getValue() must be normalized to empty string, not null");
            assertEquals("", editor.getValue(),
                "getValue() must return empty string after setValue(null)");
        }

        @Test
        @DisplayName("issue #85: consecutive client changes carry correct old/new values")
        void testConsecutiveClientChangesOldNewValues() {
            // 验证 oldValue/newValue 语义在连续变更下正确传递。
            AtomicReference<String> lastOld = new AtomicReference<>();
            AtomicReference<String> lastNew = new AtomicReference<>();
            editor.addValueChangeListener(event -> {
                lastOld.set(event.getOldValue());
                lastNew.set(event.getValue());
            });

            editor.setModelValue("<p>first</p>", true);
            assertEquals("<p>first</p>", lastNew.get());

            editor.setModelValue("<p>second</p>", true);
            // 第二次变更：旧值应为第一次的新值，新值应为第二次内容。
            assertEquals("<p>first</p>", lastOld.get(),
                "oldValue should carry the previous content on consecutive changes");
            assertEquals("<p>second</p>", lastNew.get());
            assertEquals("<p>second</p>", editor.getValue());
        }
    }

    // ==================== Content Stats Tests ====================

    @Nested
    @DisplayName("Content Stats Tests")
    class ContentStatsTests {

        @Test
        @DisplayName("getCharacterCount should return correct character count")
        void testCharacterCount() {
            editor.setValue("<p>Hello</p>");
            assertEquals(5, editor.getCharacterCount());

            editor.setValue("<p>Hello World</p>");
            assertEquals(11, editor.getCharacterCount());
        }

        @Test
        @DisplayName("getWordCount should return correct word count")
        void testWordCount() {
            editor.setValue("<p>Hello World</p>");
            assertEquals(2, editor.getWordCount());

            editor.setValue("<p>One two three four five</p>");
            assertEquals(5, editor.getWordCount());
        }

        @Test
        @DisplayName("getWordCount should return zero for empty content")
        void testWordCountEmpty() {
            editor.setValue("");
            assertEquals(0, editor.getWordCount());
        }

        @Test
        @DisplayName("isContentEmpty should correctly detect empty content")
        void testIsContentEmpty() {
            editor.setValue("");
            assertTrue(editor.isContentEmpty());

            editor.setValue("<p></p>");
            assertTrue(editor.isContentEmpty());

            editor.setValue("<p>   </p>");
            assertTrue(editor.isContentEmpty());

            editor.setValue("<p>Content</p>");
            assertFalse(editor.isContentEmpty());
        }
    }

    // ==================== Handler Integration Tests ====================

    @Nested
    @DisplayName("Handler Integration Tests")
    class HandlerIntegrationTests {

        @Test
        @DisplayName("ErrorHandler should be set and retrieved correctly")
        void testErrorHandler() {
            assertNull(editor.getErrorHandler());

            AtomicBoolean handled = new AtomicBoolean(false);
            ErrorHandler handler = error -> {
                handled.set(true);
                return true;
            };

            editor.setErrorHandler(handler);
            assertSame(handler, editor.getErrorHandler());
        }

        @Test
        @DisplayName("HtmlSanitizer should be set and retrieved correctly")
        void testHtmlSanitizer() {
            assertNull(editor.getHtmlSanitizer());

            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT);
            editor.setHtmlSanitizer(sanitizer);
            assertSame(sanitizer, editor.getHtmlSanitizer());
        }

        @Test
        @DisplayName("getSanitizedValue without Sanitizer should return original value")
        void testGetSanitizedValueWithoutSanitizer() {
            String html = "<script>bad</script><p>good</p>";
            editor.setValue(html);
            assertEquals(html, editor.getSanitizedValue());
        }

        @Test
        @DisplayName("getSanitizedValue with Sanitizer should sanitize content")
        void testGetSanitizedValueWithSanitizer() {
            // Sanitizer must be set at build time because ContentManager is created in initialize()
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.BASIC);
            VaadinCKEditor editorWithSanitizer = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withValue("<script>bad</script><p>good</p>")
                .withHtmlSanitizer(sanitizer)
                .build();

            String sanitized = editorWithSanitizer.getSanitizedValue();

            assertFalse(sanitized.contains("<script>"));
            assertTrue(sanitized.contains("good"));
        }
    }

    // ==================== Property Tests ====================

    @Nested
    @DisplayName("Property Tests")
    class PropertyTests {

        @Test
        @DisplayName("setReadOnly should update read-only state")
        void testSetReadOnly() {
            assertFalse(editor.isReadOnly());
            editor.setReadOnly(true);
            assertTrue(editor.isReadOnly());
            editor.setReadOnly(false);
            assertFalse(editor.isReadOnly());
        }

        @Test
        @DisplayName("setFallbackMode should update fallback mode")
        void testSetFallbackMode() {
            assertEquals(FallbackMode.TEXTAREA, editor.getFallbackMode());

            editor.setFallbackMode(FallbackMode.READ_ONLY);
            assertEquals(FallbackMode.READ_ONLY, editor.getFallbackMode());

            editor.setFallbackMode(FallbackMode.ERROR_MESSAGE);
            assertEquals(FallbackMode.ERROR_MESSAGE, editor.getFallbackMode());

            editor.setFallbackMode(FallbackMode.HIDDEN);
            assertEquals(FallbackMode.HIDDEN, editor.getFallbackMode());
        }
    }

    // ==================== Builder Chaining Tests ====================

    @Nested
    @DisplayName("Builder Chaining Tests")
    class BuilderChainTests {

        @Test
        @DisplayName("Builder should support full chaining")
        void testFullBuilderChain() {
            AtomicBoolean errorHandlerCalled = new AtomicBoolean(false);
            ErrorHandler errorHandler = error -> {
                errorHandlerCalled.set(true);
                return false;
            };
            HtmlSanitizer sanitizer = HtmlSanitizer.withPolicy(SanitizationPolicy.RELAXED);

            VaadinCKEditor fullEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withValue("<p>Initial</p>")
                .withLanguage("zh-cn")
                .withType(CKEditorType.CLASSIC)
                .withTheme(CKEditorTheme.DARK)
                .withFallbackMode(FallbackMode.READ_ONLY)
                .withErrorHandler(errorHandler)
                .withHtmlSanitizer(sanitizer)
                .build();

            // Set read-only mode (via setter)
            fullEditor.setReadOnly(true);

            assertEquals("<p>Initial</p>", fullEditor.getValue());
            assertTrue(fullEditor.isReadOnly());
            assertEquals(FallbackMode.READ_ONLY, fullEditor.getFallbackMode());
            assertSame(errorHandler, fullEditor.getErrorHandler());
            assertSame(sanitizer, fullEditor.getHtmlSanitizer());
        }

        @Test
        @DisplayName("withPreset should quickly create editor")
        void testWithPresetShortcut() {
            VaadinCKEditor basicEditor = VaadinCKEditor.withPreset(CKEditorPreset.BASIC);
            assertNotNull(basicEditor);

            VaadinCKEditor standardEditor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);
            assertNotNull(standardEditor);

            VaadinCKEditor fullEditor = VaadinCKEditor.withPreset(CKEditorPreset.FULL);
            assertNotNull(fullEditor);
        }
    }

    // ==================== Plugin Configuration Tests ====================

    @Nested
    @DisplayName("Plugin Configuration Tests")
    class PluginConfigTests {

        @Test
        @DisplayName("addPlugin should add plugin")
        void testAddPlugin() {
            VaadinCKEditor customEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .addPlugin(CKEditorPlugin.TABLE)
                .addPlugin(CKEditorPlugin.CODE_BLOCK)
                .build();

            assertNotNull(customEditor);
        }

        @Test
        @DisplayName("withPlugins should set plugin collection")
        void testWithPlugins() {
            VaadinCKEditor customEditor = VaadinCKEditor.create()
                .withPlugins(
                    CKEditorPlugin.ESSENTIALS,
                    CKEditorPlugin.PARAGRAPH,
                    CKEditorPlugin.BOLD,
                    CKEditorPlugin.ITALIC
                )
                .build();

            assertNotNull(customEditor);
        }

        @Test
        @DisplayName("Dependency mode should be set correctly")
        void testDependencyMode() {
            VaadinCKEditor autoEditor = VaadinCKEditor.create()
                .withPlugins(CKEditorPlugin.IMAGE_CAPTION)
                .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE)
                .build();

            assertNotNull(autoEditor);
        }

        @Test
        @DisplayName("STRICT mode should throw exception when dependency missing")
        void testStrictModeThrowsOnMissingDependency() {
            assertThrows(IllegalStateException.class, () -> {
                VaadinCKEditor.create()
                    .withPlugins(CKEditorPlugin.IMAGE_CAPTION) // Requires IMAGE dependency
                    .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.STRICT)
                    .build();
            });
        }
    }

    // ==================== Toolbar Configuration Tests ====================

    @Nested
    @DisplayName("Toolbar Configuration Tests")
    class ToolbarConfigTests {

        @Test
        @DisplayName("withToolbar should set custom toolbar")
        void testCustomToolbar() {
            VaadinCKEditor customEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withToolbar("bold", "italic", "|", "undo", "redo")
                .build();

            assertNotNull(customEditor);
        }

        @Test
        @DisplayName("withToolbar should support separators")
        void testToolbarWithSeparator() {
            VaadinCKEditor customEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withToolbar("bold", "italic", "|", "bulletedList", "numberedList")
                .build();

            assertNotNull(customEditor);
        }
    }

    // ==================== Config Object Tests ====================

    @Nested
    @DisplayName("Config Object Tests")
    class ConfigTests {

        @Test
        @DisplayName("withConfig should apply config")
        void testWithConfig() {
            CKEditorConfig config = new CKEditorConfig();
            config.setPlaceholder("Enter content...");

            VaadinCKEditor customEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withConfig(config)
                .build();

            assertNotNull(customEditor);
        }

        @Test
        @DisplayName("withConfig should allow detailed configuration")
        void testWithConfigDetailed() {
            CKEditorConfig config = new CKEditorConfig();
            config.setPlaceholder("Type here...");

            VaadinCKEditor customEditor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withConfig(config)
                .build();

            assertNotNull(customEditor);
        }
    }

    // ==================== Version Info Tests ====================

    @Nested
    @DisplayName("Version Info Tests")
    class VersionTests {

        @Test
        @DisplayName("getVersion should return version number")
        void testGetVersion() {
            String version = VaadinCKEditor.getVersion();
            assertNotNull(version);
            assertFalse(version.isEmpty());
            assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"));
        }
    }

    // ==================== Concurrency Safety Tests ====================

    @Nested
    @DisplayName("Concurrency Safety Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Concurrent listener registration should be thread-safe")
        void testConcurrentListenerRegistration() throws InterruptedException {
            int threadCount = 10;
            int listenersPerThread = 100;
            AtomicInteger registrationCount = new AtomicInteger(0);

            Thread[] threads = new Thread[threadCount];
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < listenersPerThread; j++) {
                        editor.addEditorReadyListener(event -> {});
                        registrationCount.incrementAndGet();
                    }
                });
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            assertEquals(threadCount * listenersPerThread, registrationCount.get());
            assertEquals(threadCount * listenersPerThread, editor.getListenerStats().ready);
        }

        @Test
        @DisplayName("Concurrent listener cleanup should be thread-safe")
        void testConcurrentCleanup() throws InterruptedException {
            // First register some listeners
            for (int i = 0; i < 100; i++) {
                editor.addEditorReadyListener(event -> {});
            }

            // Concurrent cleanup
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(() -> editor.cleanupListeners());
            }

            for (Thread thread : threads) {
                thread.start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Should be zero after cleanup
            assertEquals(0, editor.getListenerStats().total());
        }
    }
}
