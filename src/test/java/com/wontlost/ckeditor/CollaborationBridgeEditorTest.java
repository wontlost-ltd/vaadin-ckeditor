package com.wontlost.ckeditor;

import com.wontlost.ckeditor.collaboration.CollaborationBridge;
import com.wontlost.ckeditor.collaboration.CollaborationUser;
import com.wontlost.ckeditor.collaboration.InMemoryCollaborationStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;

/**
 * VaadinCKEditor 与 CollaborationBridge 的接线：自定义插件加入插件列表、超时下发、请求处理与错误消息。
 */
class CollaborationBridgeEditorTest {

    private final InMemoryCollaborationStore store = new InMemoryCollaborationStore();
    private final CollaborationBridge bridge = CollaborationBridge.builder(store, "doc", new CollaborationUser("bob", "Bob"))
        .requestTimeout(Duration.ofSeconds(5)).build();

    private static JsonNode bridgeEntry(VaadinCKEditor editor) {
        JsonNode plugins = JsonUtil.valueToTree(editor.getElement().getPropertyRaw("plugins"));
        for (JsonNode plugin : plugins) {
            if ("VaadinCollaborationBridge".equals(plugin.path("name").asText())) {
                return plugin;
            }
        }
        return null;
    }

    @Test
    @DisplayName("builder 与 setter：以自定义插件加入插件列表，与 premium 插件并存；超时下发；可移除")
    void wiring() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withType(CKEditorType.DECOUPLED)
            .withPreset(CKEditorPreset.BASIC)
            .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
            .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
            .withCollaborationBridge(bridge)
            .build();

        JsonNode entry = bridgeEntry(editor);
        assertThat(entry).isNotNull();
        assertThat(entry.path("premium").asBoolean()).isFalse();
        assertThat(entry.path("importPath").asText()).isEqualTo("./vaadin-ckeditor/collaboration-bridge");
        assertThat(JsonUtil.valueToTree(editor.getElement().getPropertyRaw("plugins")).toString())
            .contains("\"Comments\"", "\"TrackChanges\"");
        assertThat(editor.getElement().getProperty("collaborationTimeoutMs", 0.0)).isEqualTo(5000.0);
        assertThat(editor.getCollaborationBridge()).isSameAs(bridge);

        editor.setCollaborationBridge(null);
        assertThat(bridgeEntry(editor)).isNull();
        assertThat(editor.getCollaborationBridge()).isNull();

        editor.setCollaborationBridge(bridge);
        editor.setCollaborationBridge(bridge);
        long count = 0;
        for (JsonNode plugin : JsonUtil.valueToTree(editor.getElement().getPropertyRaw("plugins"))) {
            if ("VaadinCollaborationBridge".equals(plugin.path("name").asText())) {
                count++;
            }
        }
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("请求处理：成功返回 JSON；拒绝返回 CollaborationException 的消息；内部错误只返回通用消息")
    void responses() {
        VaadinCKEditor.CollaborationResponse load = VaadinCKEditor.collaborationResponse(bridge, "load", "{}").join();
        assertThat(load.error()).isNull();
        assertThat(load.resultJson()).contains("\"me\":{\"id\":\"bob\"");

        VaadinCKEditor.CollaborationResponse missing = VaadinCKEditor.collaborationResponse(bridge, "getSuggestion", "{\"id\":\"x\"}").join();
        assertThat(missing.resultJson()).isNull();
        assertThat(missing.error()).isEqualTo("suggestion x not found");

        assertThat(VaadinCKEditor.collaborationResponse(bridge, "load", "{not json").join().error()).isEqualTo("Invalid request");
        assertThat(VaadinCKEditor.collaborationResponse(null, "load", "{}").join().error())
            .isEqualTo("No collaboration bridge configured");

        InMemoryCollaborationStore failing = new InMemoryCollaborationStore() {
            @Override
            public CompletableFuture<java.util.List<CollaborationUser>> loadUsers(
                    com.wontlost.ckeditor.collaboration.CollaborationContext context) {
                return CompletableFuture.failedFuture(new IllegalStateException("jdbc:secret@db"));
            }
        };
        CollaborationBridge broken = CollaborationBridge.builder(failing, "doc", new CollaborationUser("bob", "Bob")).build();
        VaadinCKEditor.CollaborationResponse internal = VaadinCKEditor.collaborationResponse(broken, "load", "{}").join();
        assertThat(internal.error()).isEqualTo("Collaboration request failed").doesNotContain("secret");
    }
}
