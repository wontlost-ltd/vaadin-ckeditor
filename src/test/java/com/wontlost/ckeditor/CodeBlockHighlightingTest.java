package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * 代码块语法高亮的 Java API：默认关闭、属性下发、主题名与客户端约定一致、序列化保留设置。
 */
class CodeBlockHighlightingTest {

    /** 下发到客户端的插件列表中的代码块高亮条目（不存在时为 null）。 */
    private static JsonNode highlightEntry(VaadinCKEditor editor) {
        JsonNode plugins = JsonUtil.valueToTree(editor.getElement().getPropertyRaw("plugins"));
        for (JsonNode plugin : plugins) {
            if ("VaadinCodeBlockHighlighting".equals(plugin.path("name").asText())) {
                return plugin;
            }
        }
        return null;
    }

    @Test
    @DisplayName("默认关闭：插件不在插件列表中（不下载插件模块与 highlight.js）")
    void defaultsOff() {
        VaadinCKEditor editor = VaadinCKEditor.create().withPreset(CKEditorPreset.FULL).build();

        assertThat(editor.isCodeBlockHighlighting()).isFalse();
        assertThat(editor.getCodeBlockHighlightTheme()).isSameAs(CodeHighlightTheme.AUTO);
        assertThat(highlightEntry(editor)).isNull();

        // 只换配色不启用：仍不加载插件
        editor.setCodeBlockHighlightTheme(CodeHighlightTheme.GITHUB);
        assertThat(highlightEntry(editor)).isNull();
    }

    @Test
    @DisplayName("启用后以自定义插件下发：按需加载路径 + 插件配置 {enabled, theme}；之后的开关与配色只改配置")
    void enablingAddsCustomPluginWithConfig() {
        VaadinCKEditor editor = VaadinCKEditor.create().withPreset(CKEditorPreset.FULL).build();

        editor.setCodeBlockHighlighting(true);
        JsonNode entry = highlightEntry(editor);
        assertThat(entry).isNotNull();
        assertThat(entry.path("premium").asBoolean()).isFalse();
        assertThat(entry.path("importPath").asText()).isEqualTo("./vaadin-ckeditor/code-block-highlighting");
        assertThat(entry.path("configKey").asText()).isEqualTo("codeBlockHighlighting");
        assertThat(entry.path("config").path("enabled").asBoolean()).isTrue();
        assertThat(entry.path("config").path("theme").asText()).isEqualTo("auto");

        editor.setCodeBlockHighlightTheme(CodeHighlightTheme.ATOM_ONE_DARK);
        assertThat(highlightEntry(editor).path("config").path("theme").asText()).isEqualTo("atom-one-dark");

        // 停用：插件留在列表（客户端不重建编辑器），配置 enabled=false
        editor.setCodeBlockHighlighting(false);
        assertThat(editor.isCodeBlockHighlighting()).isFalse();
        JsonNode disabled = highlightEntry(editor);
        assertThat(disabled).isNotNull();
        assertThat(disabled.path("config").path("enabled").asBoolean()).isFalse();
        assertThat(disabled.path("config").path("theme").asText()).isEqualTo("atom-one-dark");

        assertThatThrownBy(() -> editor.setCodeBlockHighlightTheme(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("builder：withCodeBlockHighlighting(theme) 同时启用并设置主题，与其他自定义插件共存")
    void builder() {
        VaadinCKEditor withTheme = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.FULL)
                .addCustomPlugin(CustomPlugin.fromPremium("FormatPainter"))
                .withCodeBlockHighlighting(CodeHighlightTheme.GITHUB_DARK)
                .build();
        assertThat(withTheme.isCodeBlockHighlighting()).isTrue();
        assertThat(withTheme.getCodeBlockHighlightTheme()).isSameAs(CodeHighlightTheme.GITHUB_DARK);
        assertThat(highlightEntry(withTheme).path("config").path("theme").asText()).isEqualTo("github-dark");
        JsonNode plugins = JsonUtil.valueToTree(withTheme.getElement().getPropertyRaw("plugins"));
        assertThat(plugins.toString()).contains("\"FormatPainter\"");

        VaadinCKEditor plain = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.FULL)
                .withCodeBlockHighlighting(true)
                .build();
        assertThat(plain.isCodeBlockHighlighting()).isTrue();
        assertThat(plain.getCodeBlockHighlightTheme()).isSameAs(CodeHighlightTheme.AUTO);
        assertThat(highlightEntry(plain)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(CodeHighlightTheme.class)
    @DisplayName("主题名与客户端 CODE_HIGHLIGHT_THEMES 一致，fromJsName 可往返")
    void themeNamesRoundTrip(CodeHighlightTheme theme) {
        assertThat(theme.getJsName()).isIn("auto", "github", "github-dark", "atom-one-light", "atom-one-dark", "none");
        assertThat(CodeHighlightTheme.fromJsName(theme.getJsName())).isSameAs(theme);
        assertThat(CodeHighlightTheme.fromJsName(theme.name())).isSameAs(theme);
    }

    @Test
    @DisplayName("fromJsName 拒绝未知名称")
    void fromJsNameRejectsUnknown() {
        assertThatThrownBy(() -> CodeHighlightTheme.fromJsName("solarized")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CodeHighlightTheme.fromJsName(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("会话序列化后保留开关与主题")
    void serializationKeepsSettings() throws Exception {
        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withCodeBlockHighlighting(CodeHighlightTheme.GITHUB)
                .build();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(editor);
        }
        VaadinCKEditor copy;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            copy = (VaadinCKEditor) in.readObject();
        }
        assertThat(copy.isCodeBlockHighlighting()).isTrue();
        assertThat(copy.getCodeBlockHighlightTheme()).isSameAs(CodeHighlightTheme.GITHUB);
    }
}
