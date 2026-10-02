package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务端开销基线（issue #137 测量项 6）。
 *
 * <ul>
 *   <li><b>会话序列化体积</b>：Vaadin 会话在集群复制 / 钝化时整体序列化，
 *       每个编辑器实例的序列化体积直接决定会话大小；</li>
 *   <li><b>首屏属性报文</b>：plugins / toolbar / config 以 JSON 属性下发，
 *       其体积决定每个实例在首屏 UIDL 中的固定开销。</li>
 * </ul>
 * 预算按首轮实测留出余量；超出说明组件状态或配置报文发生了明显膨胀。
 */
class ServerFootprintTest {

    /** 单个编辑器实例的序列化体积上限（字节）；基线约 13.6–16 KB。 */
    private static final int MAX_SERIALIZED_BYTES = 32 * 1024;
    /** 单个编辑器实例 plugins + toolbar + config 属性 JSON 的总体积上限（字节）；基线约 0.5–2 KB。 */
    private static final int MAX_PROPERTY_JSON_BYTES = 4 * 1024;

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CKEditorPreset.class, names = {"BASIC", "STANDARD", "FULL", "DOCUMENT"})
    @DisplayName("单个编辑器的会话序列化体积在预算内")
    void serializedSizeWithinBudget(CKEditorPreset preset) throws IOException {
        VaadinCKEditor editor = VaadinCKEditor.create().withPreset(preset).withValue("<p>x</p>").build();
        editor.addValueChangeListener(e -> { });
        editor.addContentChangeListener(e -> { });

        int bytes = serializedSize(editor);
        System.out.printf("[perf] serialized.%s = %d bytes%n", preset, bytes);
        assertTrue(bytes <= MAX_SERIALIZED_BYTES,
                () -> preset + " 序列化体积 " + bytes + " B 超出预算 " + MAX_SERIALIZED_BYTES + " B");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CKEditorPreset.class, names = {"BASIC", "STANDARD", "FULL", "DOCUMENT"})
    @DisplayName("首屏属性 JSON 体积在预算内")
    void propertyJsonWithinBudget(CKEditorPreset preset) {
        VaadinCKEditor editor = VaadinCKEditor.create().withPreset(preset).build();

        int bytes = jsonLength(editor, "plugins") + jsonLength(editor, "toolbar") + jsonLength(editor, "config");
        System.out.printf("[perf] propertyJson.%s = %d bytes%n", preset, bytes);
        assertTrue(bytes <= MAX_PROPERTY_JSON_BYTES,
                () -> preset + " 属性 JSON " + bytes + " B 超出预算 " + MAX_PROPERTY_JSON_BYTES + " B");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = CKEditorPreset.class, names = {"BASIC"})
    @DisplayName("会话体积不随文档内容以外的因素膨胀：设值 1 MB 后的增量约等于文档本身")
    void serializedSizeTracksDocumentSize(CKEditorPreset preset) throws IOException {
        VaadinCKEditor editor = VaadinCKEditor.create().withPreset(preset).build();
        int empty = serializedSize(editor);
        String doc = "<p>" + "a".repeat(1024 * 1024) + "</p>";
        editor.setValue(doc);
        int filled = serializedSize(editor);

        double ratio = (double) (filled - empty) / doc.length();
        System.out.printf("[perf] serialized.docOverheadRatio = %.2f%n", ratio);
        // 同一字符串若被组件多处各持一份拷贝，比例会接近 2 或更高
        assertTrue(ratio < 1.5, () -> "设值后序列化增量为文档体积的 " + ratio + " 倍");
    }

    private static int serializedSize(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        return bytes.size();
    }

    private static int jsonLength(VaadinCKEditor editor, String property) {
        Object raw = editor.getElement().getPropertyRaw(property);
        return raw == null ? 0 : raw.toString().length();
    }
}
