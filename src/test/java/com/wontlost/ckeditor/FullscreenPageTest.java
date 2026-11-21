package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * 全屏页面宽度（issue #156）：默认值、下发到客户端的属性、序列化与自定义宽度校验。
 */
class FullscreenPageTest {

    @Test
    @DisplayName("默认为 A4_FIT，并在初始化时下发到客户端")
    void defaultIsA4Fit() {
        VaadinCKEditor editor = VaadinCKEditor.create().build();

        assertThat(editor.getFullscreenPage()).isSameAs(FullscreenPage.A4_FIT);
        assertThat(editor.getElement().getProperty("fullscreenPage")).isEqualTo("a4-fit");
        assertThat(editor.getElement().getProperty("fullscreenPageWidth")).isEmpty();
    }

    @Test
    @DisplayName("预定义值的模式名与客户端约定一致")
    void predefinedModes() {
        assertThat(FullscreenPage.A4_FIT.getMode()).isEqualTo("a4-fit");
        assertThat(FullscreenPage.A4.getMode()).isEqualTo("a4");
        assertThat(FullscreenPage.FULL_WIDTH.getMode()).isEqualTo("full-width");
        assertThat(FullscreenPage.A4.getCssWidth()).isNull();
    }

    @Test
    @DisplayName("setFullscreenPage 与 builder 写入模式与宽度属性，可在运行期切换")
    void setterAndBuilderSyncProperties() {
        VaadinCKEditor editor = VaadinCKEditor.create()
                .withFullscreenPage(FullscreenPage.custom(" MIN(1400PX, 90%) "))
                .build();
        assertThat(editor.getFullscreenPage()).isEqualTo(FullscreenPage.custom("min(1400px, 90%)"));
        assertThat(editor.getElement().getProperty("fullscreenPage")).isEqualTo("custom");
        assertThat(editor.getElement().getProperty("fullscreenPageWidth")).isEqualTo("min(1400px, 90%)");

        editor.setFullscreenPage(FullscreenPage.A4);
        assertThat(editor.getElement().getProperty("fullscreenPage")).isEqualTo("a4");
        assertThat(editor.getElement().getProperty("fullscreenPageWidth")).isEmpty();

        assertThatThrownBy(() -> editor.setFullscreenPage(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("序列化往返：预定义值还原为同一实例，自定义值保持宽度；编辑器保留设置")
    void serializationRoundTrip() throws Exception {
        assertThat(roundTrip(FullscreenPage.A4)).isSameAs(FullscreenPage.A4);
        assertThat(roundTrip(FullscreenPage.A4_FIT)).isSameAs(FullscreenPage.A4_FIT);
        assertThat(roundTrip(FullscreenPage.FULL_WIDTH)).isSameAs(FullscreenPage.FULL_WIDTH);
        assertThat(roundTrip(FullscreenPage.custom("1200px"))).isEqualTo(FullscreenPage.custom("1200px"));

        VaadinCKEditor editor = VaadinCKEditor.create().withFullscreenPage(FullscreenPage.FULL_WIDTH).build();
        VaadinCKEditor copy = roundTrip(editor);
        assertThat(copy.getFullscreenPage()).isSameAs(FullscreenPage.FULL_WIDTH);
        assertThat(copy.getElement().getProperty("fullscreenPage")).isEqualTo("full-width");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1200px", "90vw", "80%", "62.5rem", "calc(100% - 40px)", "min(1400px, 90%)",
            "clamp(600px, 80%, 1400px)", "max(50cqw, 800px)", "210mm"})
    @DisplayName("接受带单位的数字及 calc / min / max / clamp 组合")
    void acceptsCssWidths(String width) {
        assertThat(FullscreenPage.custom(width).getCssWidth()).isEqualTo(width);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "auto", "100", "px", "red", "1200px; color: red", "1200px}", "url(x)",
            "var(--w)", "attr(width)", "expression(alert(1))", "env(safe-area-inset-left)",
            "1200px !important", "\"1200px\"", "calc(100% - 40px", "min(1px))(", "1200 px",
            "1200foo", "fit-content", "calc(1px)\\;", "0", "calc(1px) url(x)"})
    @DisplayName("拒绝关键字、未知单位 / 函数、引号、分号、花括号、!important 与不平衡括号")
    void rejectsUnsafeOrInvalidWidths(String width) {
        assertThatThrownBy(() -> FullscreenPage.custom(width)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null 与超长宽度被拒绝")
    void rejectsNullAndOverlong() {
        assertThatThrownBy(() -> FullscreenPage.custom(null)).isInstanceOf(IllegalArgumentException.class);
        String overlong = "calc(" + "1px + ".repeat(20) + "1px)";
        assertThatThrownBy(() -> FullscreenPage.custom(overlong)).isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
