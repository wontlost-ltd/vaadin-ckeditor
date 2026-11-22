package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.*;

/**
 * 自定义插件的 Java 端配置（CustomPlugin.Builder#withConfig）：校验、不可变、随插件列表下发。
 */
class CustomPluginConfigTest {

    @Test
    @DisplayName("withConfig 保存配置键与值的副本，getConfig 返回副本")
    void configIsCopied() {
        ObjectNode value = JsonUtil.createObjectNode();
        value.put("level", 1);
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin").withImportPath("my-plugin").withConfig("myPlugin", value).build();
        value.put("level", 2);

        assertThat(plugin.getConfigKey()).isEqualTo("myPlugin");
        assertThat(plugin.getConfig().path("level").asInt()).isEqualTo(1);
        ((ObjectNode) plugin.getConfig()).put("level", 3);
        assertThat(plugin.getConfig().path("level").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("未设置时为 null，既有插件行为不变")
    void absentByDefault() {
        CustomPlugin plugin = CustomPlugin.of("MyPlugin", "my-plugin");
        assertThat(plugin.getConfigKey()).isNull();
        assertThat(plugin.getConfig()).isNull();
    }

    @Test
    @DisplayName("配置键必须是 JavaScript 标识符，值不能为 null")
    void validation() {
        CustomPlugin.Builder builder = CustomPlugin.builder("MyPlugin");
        assertThatThrownBy(() -> builder.withConfig("a.b", JsonUtil.createObjectNode())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.withConfig("", JsonUtil.createObjectNode())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.withConfig(null, JsonUtil.createObjectNode())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.withConfig("ok", null)).isInstanceOf(NullPointerException.class);
        for (String reserved : new String[] {"__proto__", "constructor", "prototype"}) {
            assertThatThrownBy(() -> builder.withConfig(reserved, JsonUtil.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("插件列表 JSON 携带 configKey 与 config")
    void serializedIntoPluginsProperty() {
        ObjectNode value = JsonUtil.createObjectNode();
        value.put("flag", true);
        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .addCustomPlugin(CustomPlugin.builder("MyPlugin").withImportPath("my-plugin").withConfig("myPlugin", value).build())
                .build();
        JsonNode plugins = JsonUtil.valueToTree(editor.getElement().getPropertyRaw("plugins"));
        JsonNode entry = null;
        for (JsonNode p : plugins) {
            if ("MyPlugin".equals(p.path("name").asText())) {
                entry = p;
            }
        }
        assertThat(entry).isNotNull();
        assertThat(entry.path("configKey").asText()).isEqualTo("myPlugin");
        assertThat(entry.path("config").path("flag").asBoolean()).isTrue();
    }
}
