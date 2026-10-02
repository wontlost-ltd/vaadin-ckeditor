package com.wontlost.ckeditor;

import com.wontlost.ckeditor.handler.ErrorHandler;
import com.wontlost.ckeditor.handler.HtmlSanitizer;
import com.wontlost.ckeditor.handler.HtmlSanitizer.SanitizationPolicy;
import com.wontlost.ckeditor.handler.UploadHandler;
import com.wontlost.ckeditor.internal.UploadManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会话序列化往返测试（issue #137 测量项 6 发现的问题）。
 *
 * <p>Vaadin 会话在 Tomcat 持久化、集群复制或钝化时整体序列化；组件图中任何
 * 不可序列化的成员都会让整个会话序列化失败。这里验证编辑器在配置了全部常用
 * 处理器后仍可往返，且往返后状态与行为保持一致。</p>
 */
class SerializationTest {

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static VaadinCKEditor fullyConfigured() {
        return VaadinCKEditor.create()
            .withPreset(CKEditorPreset.FULL)
            .withValue("<p>hello</p>")
            .withHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.BASIC))
            .withErrorHandler(ErrorHandler.logging(Logger.getLogger("serialization-test")))
            .withUploadHandler((context, stream) -> CompletableFuture.completedFuture(new UploadHandler.UploadResult("/u")))
            .build();
    }

    @Test
    @DisplayName("配置了净化器、错误处理器、上传处理器的编辑器可往返，值与配置保持")
    void fullyConfiguredEditorRoundTrips() throws Exception {
        VaadinCKEditor editor = fullyConfigured();
        editor.setValue("<p>before</p>");

        VaadinCKEditor copy = roundTrip(editor);

        assertEquals("<p>before</p>", copy.getValue());
        copy.setValue("<p>safe</p><script>alert(1)</script>");
        assertFalse(copy.getSanitizedValue().contains("<script"), "往返后净化器应仍然生效");
        assertNotNull(copy.getErrorHandler());
        assertNotNull(copy.getUploadHandler());
    }

    /** 静态字段不参与序列化，可用来观察副本里的监听器是否被调用。 */
    private static final AtomicInteger LISTENER_CALLS = new AtomicInteger();

    @Test
    @DisplayName("往返后上传管理器被重建，监听器仍然生效")
    void transientStateIsRebuilt() throws Exception {
        VaadinCKEditor editor = fullyConfigured();
        LISTENER_CALLS.set(0);
        editor.addContentChangeListener(e -> LISTENER_CALLS.incrementAndGet());

        VaadinCKEditor copy = roundTrip(editor);

        assertInstanceOf(UploadManager.class, field(copy, "uploadManager"));
        assertTrue(copy.getElement().getProperty("contentChangeEvents", false));
        copy.syncContentInternal("<p>after</p>", "", "USER_INPUT", true, false);
        assertEquals(1, LISTENER_CALLS.get());
    }

    @Test
    @DisplayName("未配置上传处理器时往返后不凭空创建上传管理器")
    void uploadManagerNotCreatedWhenAbsent() throws Exception {
        VaadinCKEditor copy = roundTrip(VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).build());
        assertNull(field(copy, "uploadManager"));
    }

    @Test
    @DisplayName("可序列化的 autosave 回调随会话保留；不可序列化的被丢弃而不是让整个会话失败")
    void autosaveCallbackSerializationIsBestEffort() throws Exception {
        VaadinCKEditor kept = VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).build();
        kept.setAutosaveCallback((Consumer<String> & Serializable) value -> { });
        assertNotNull(field(roundTrip(kept), "autosaveCallback"));

        VaadinCKEditor dropped = VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).build();
        Consumer<String> plain = value -> { };
        dropped.setAutosaveCallback(plain);
        assertNull(field(roundTrip(dropped), "autosaveCallback"));
    }

    @Test
    @DisplayName("往返后的 ErrorHandler.logging 仍能处理错误")
    void loggingErrorHandlerSurvivesRoundTrip() throws Exception {
        ErrorHandler handler = roundTrip(ErrorHandler.logging(Logger.getLogger("serialization-test")));
        assertFalse(handler.handleError(new com.wontlost.ckeditor.event.EditorErrorEvent.EditorError(
            "CODE", "message", com.wontlost.ckeditor.event.EditorErrorEvent.ErrorSeverity.WARNING, true, null)));
    }

    @Test
    @DisplayName("带自定义插件（Premium 的 Comments / TrackChanges 依赖它）的编辑器可往返")
    void customPluginsRoundTrip() throws Exception {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .addCustomPlugin(CustomPlugin.builder("MyPlugin")
                .withImportPath("my-ckeditor-plugin")
                .withToolbarItems("myButton")
                .build())
            .build();

        VaadinCKEditor copy = roundTrip(editor);
        assertTrue(copy.getElement().getPropertyRaw("plugins").toString().contains("MyPlugin"));
    }

    @Test
    @DisplayName("ErrorHandler.logging 运行时持有调用方传入的同一个 logger，往返后按名称取回")
    void loggingHandlerKeepsStrongReference() throws Exception {
        Logger logger = Logger.getLogger("serialization-test-strong-ref");
        ErrorHandler handler = ErrorHandler.logging(logger);
        assertSame(logger, field(handler, "logger"));

        ErrorHandler copy = roundTrip(handler);
        assertSame(Logger.getLogger("serialization-test-strong-ref"), field(copy, "logger"));
    }
}
