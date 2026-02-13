package com.wontlost.ckeditor;

import com.wontlost.ckeditor.VaadinCKEditorPremium.PremiumPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * 协作功能单元测试
 * 覆盖 PremiumPlugin 协作元数据、CustomPlugin 创建、CKEditorConfig 协作序列化
 */
class CollaborationFeaturesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Nested
    @DisplayName("Premium 协作插件元数据")
    class PremiumCollaborationPlugins {

        @Test
        @DisplayName("插件元数据和 CustomPlugin 转换应与 CKEditor 5 原生命名一致")
        void pluginMetadataAndCustomPluginShouldMatch() {
            Map<PremiumPlugin, ExpectedPlugin> expected = expectedCollaborationPlugins();

            for (Map.Entry<PremiumPlugin, ExpectedPlugin> entry : expected.entrySet()) {
                PremiumPlugin plugin = entry.getKey();
                ExpectedPlugin expectedPlugin = entry.getValue();

                assertThat(plugin.getPluginName()).isEqualTo(expectedPlugin.pluginName);
                assertThat(plugin.getToolbarItems()).containsExactly(expectedPlugin.toolbarItems);

                CustomPlugin customPlugin = plugin.toCustomPlugin();
                assertThat(customPlugin.isPremium()).isTrue();
                assertThat(customPlugin.getJsName()).isEqualTo(expectedPlugin.pluginName);
                assertThat(customPlugin.getImportPath()).isNull();
                if (expectedPlugin.toolbarItems.length == 0) {
                    assertThat(customPlugin.getToolbarItems()).isEmpty();
                } else {
                    assertThat(customPlugin.getToolbarItems())
                        .containsExactlyInAnyOrder(expectedPlugin.toolbarItems);
                }
            }
        }

        @Test
        @DisplayName("协作插件集合应与 CKEditor 5 原生插件列表一致")
        void collaborationPluginSetShouldMatchNativeCollection() {
            Set<String> expectedPluginNames = expectedCollaborationPluginNames();
            Set<String> actualPluginNames = collaborationPlugins().stream()
                .map(PremiumPlugin::getPluginName)
                .collect(Collectors.toSet());

            assertThat(actualPluginNames).containsExactlyInAnyOrderElementsOf(expectedPluginNames);
        }
    }

    @Nested
    @DisplayName("CustomPlugin 协作创建")
    class CustomPluginCollaborationCreation {

        @Test
        @DisplayName("fromPremium 应正确创建 premium 协作插件")
        void fromPremiumShouldCreateCollaborationPlugins() {
            for (PremiumPlugin plugin : collaborationPlugins()) {
                CustomPlugin customPlugin = CustomPlugin.fromPremium(plugin.getPluginName());

                assertThat(customPlugin.isPremium()).isTrue();
                assertThat(customPlugin.getJsName()).isEqualTo(plugin.getPluginName());
                assertThat(customPlugin.getImportPath()).isNull();
                assertThat(customPlugin.getToolbarItems()).isEmpty();
            }
        }

        @Test
        @DisplayName("Builder 应创建带工具栏项的协作插件")
        void builderShouldCreateCollaborationPluginsWithToolbarItems() {
            CustomPlugin comments = CustomPlugin.builder(PremiumPlugin.COMMENTS.getPluginName())
                .premium()
                .withToolbarItems("comment")
                .build();
            CustomPlugin trackChanges = CustomPlugin.builder(PremiumPlugin.TRACK_CHANGES.getPluginName())
                .premium()
                .withToolbarItems("trackChanges")
                .build();
            CustomPlugin revisionHistory = CustomPlugin.builder(PremiumPlugin.REVISION_HISTORY.getPluginName())
                .premium()
                .withToolbarItems("revisionHistory")
                .build();

            assertThat(comments.isPremium()).isTrue();
            assertThat(comments.getToolbarItems()).containsExactly("comment");

            assertThat(trackChanges.isPremium()).isTrue();
            assertThat(trackChanges.getToolbarItems()).containsExactly("trackChanges");

            assertThat(revisionHistory.isPremium()).isTrue();
            assertThat(revisionHistory.getToolbarItems()).containsExactly("revisionHistory");
        }
    }

    @Nested
    @DisplayName("CKEditorConfig 协作序列化")
    class CKEditorConfigCollaborationSerialization {
        private CKEditorConfig config;

        @BeforeEach
        void setUp() {
            config = new CKEditorConfig();
        }

        @Test
        @DisplayName("setPagination 应序列化页面尺寸和边距")
        void setPaginationShouldSerializePageSizesAndMargins() {
            CKEditorConfig.PaginationMargins margins =
                new CKEditorConfig.PaginationMargins("10mm", "15mm", "10mm", "15mm");

            config.setPagination("21cm", "29.7cm", margins);
            ObjectNode json = config.toJson();

            assertThat(json.has("pagination")).isTrue();
            ObjectNode pagination = (ObjectNode) json.get("pagination");
            assertThat(pagination.get("pageWidth").asText()).isEqualTo("21cm");
            assertThat(pagination.get("pageHeight").asText()).isEqualTo("29.7cm");
            assertThat(pagination.get("pageMargins").get("top").asText()).isEqualTo("10mm");
            assertThat(pagination.get("pageMargins").get("right").asText()).isEqualTo("15mm");
            assertThat(pagination.get("pageMargins").get("bottom").asText()).isEqualTo("10mm");
            assertThat(pagination.get("pageMargins").get("left").asText()).isEqualTo("15mm");
        }

        @Test
        @DisplayName("setPagination 应在页面尺寸为 null 时省略")
        void setPaginationShouldOmitNullPageDimensions() {
            CKEditorConfig.PaginationMargins margins =
                new CKEditorConfig.PaginationMargins("5mm", "5mm", "5mm", "5mm");

            config.setPagination(null, null, margins);
            ObjectNode json = config.toJson();

            ObjectNode pagination = (ObjectNode) json.get("pagination");
            assertThat(pagination.has("pageWidth")).isFalse();
            assertThat(pagination.has("pageHeight")).isFalse();
            assertThat(pagination.get("pageMargins").get("top").asText()).isEqualTo("5mm");
        }

        @Test
        @DisplayName("cloudServices 配置应序列化 token 和 websocket URL")
        void cloudServicesConfigShouldSerializeUrls() {
            ObjectNode cloudServices = MAPPER.createObjectNode();
            cloudServices.put("tokenUrl", "https://example.com/token");
            cloudServices.put("webSocketUrl", "wss://example.com/ws");

            config.set("cloudServices", cloudServices);
            ObjectNode json = config.toJson();

            assertThat(json.get("cloudServices").get("tokenUrl").asText())
                .isEqualTo("https://example.com/token");
            assertThat(json.get("cloudServices").get("webSocketUrl").asText())
                .isEqualTo("wss://example.com/ws");
        }

        @Test
        @DisplayName("collaboration 配置应序列化 channelId")
        void collaborationConfigShouldSerializeChannelId() {
            ObjectNode collaboration = MAPPER.createObjectNode();
            collaboration.put("channelId", "document-123");

            config.set("collaboration", collaboration);
            ObjectNode json = config.toJson();

            assertThat(json.get("collaboration").get("channelId").asText())
                .isEqualTo("document-123");
        }

        @Test
        @DisplayName("comments 配置应序列化 editorConfig")
        void commentsConfigShouldSerializeEditorConfig() {
            ObjectNode comments = MAPPER.createObjectNode();
            ObjectNode editorConfig = MAPPER.createObjectNode();
            editorConfig.put("placeholder", "Comment here");
            comments.set("editorConfig", editorConfig);

            config.set("comments", comments);
            ObjectNode json = config.toJson();

            assertThat(json.get("comments").get("editorConfig").get("placeholder").asText())
                .isEqualTo("Comment here");
        }

        @Test
        @DisplayName("sidebar 和 presenceList 配置应序列化容器")
        void sidebarAndPresenceListConfigsShouldSerializeContainers() {
            ObjectNode sidebar = MAPPER.createObjectNode();
            sidebar.put("container", "#sidebar");
            ObjectNode presenceList = MAPPER.createObjectNode();
            presenceList.put("container", "#presence-list");

            config.set("sidebar", sidebar);
            config.set("presenceList", presenceList);
            ObjectNode json = config.toJson();

            assertThat(json.get("sidebar").get("container").asText()).isEqualTo("#sidebar");
            assertThat(json.get("presenceList").get("container").asText()).isEqualTo("#presence-list");
        }

        @Test
        @DisplayName("revisionHistory 配置应序列化所有容器")
        void revisionHistoryConfigShouldSerializeAllContainers() {
            ObjectNode revisionHistory = MAPPER.createObjectNode();
            revisionHistory.put("editorContainer", "#editor");
            revisionHistory.put("viewerContainer", "#viewer");
            revisionHistory.put("sidebarContainer", "#sidebar");
            revisionHistory.put("menuContainer", "#menu");

            config.set("revisionHistory", revisionHistory);
            ObjectNode json = config.toJson();

            ObjectNode stored = (ObjectNode) json.get("revisionHistory");
            assertThat(stored.get("editorContainer").asText()).isEqualTo("#editor");
            assertThat(stored.get("viewerContainer").asText()).isEqualTo("#viewer");
            assertThat(stored.get("sidebarContainer").asText()).isEqualTo("#sidebar");
            assertThat(stored.get("menuContainer").asText()).isEqualTo("#menu");
        }
    }

    // --- 辅助方法 ---

    private static EnumSet<PremiumPlugin> collaborationPlugins() {
        return EnumSet.of(
            PremiumPlugin.COMMENTS,
            PremiumPlugin.TRACK_CHANGES,
            PremiumPlugin.TRACK_CHANGES_DATA,
            PremiumPlugin.TRACK_CHANGES_PREVIEW,
            PremiumPlugin.REVISION_HISTORY,
            PremiumPlugin.REAL_TIME_COLLABORATION,
            PremiumPlugin.REAL_TIME_COLLABORATIVE_EDITING,
            PremiumPlugin.REAL_TIME_COLLABORATIVE_COMMENTS,
            PremiumPlugin.REAL_TIME_COLLABORATIVE_TRACK_CHANGES,
            PremiumPlugin.REAL_TIME_COLLABORATIVE_REVISION_HISTORY,
            PremiumPlugin.PRESENCE_LIST,
            PremiumPlugin.PAGINATION,
            PremiumPlugin.DOCUMENT_OUTLINE
        );
    }

    private static Set<String> expectedCollaborationPluginNames() {
        return Set.of(
            "Comments",
            "TrackChanges",
            "TrackChangesData",
            "TrackChangesPreview",
            "RevisionHistory",
            "RealTimeCollaboration",
            "RealTimeCollaborativeEditing",
            "RealTimeCollaborativeComments",
            "RealTimeCollaborativeTrackChanges",
            "RealTimeCollaborativeRevisionHistory",
            "PresenceList",
            "Pagination",
            "DocumentOutline"
        );
    }

    private static Map<PremiumPlugin, ExpectedPlugin> expectedCollaborationPlugins() {
        Map<PremiumPlugin, ExpectedPlugin> expected = new LinkedHashMap<>();
        expected.put(PremiumPlugin.COMMENTS, new ExpectedPlugin("Comments", new String[]{"comment"}));
        expected.put(PremiumPlugin.TRACK_CHANGES, new ExpectedPlugin("TrackChanges", new String[]{"trackChanges"}));
        expected.put(PremiumPlugin.TRACK_CHANGES_DATA, new ExpectedPlugin("TrackChangesData", new String[0]));
        expected.put(PremiumPlugin.TRACK_CHANGES_PREVIEW, new ExpectedPlugin("TrackChangesPreview", new String[0]));
        expected.put(PremiumPlugin.REVISION_HISTORY, new ExpectedPlugin("RevisionHistory", new String[]{"revisionHistory"}));
        expected.put(PremiumPlugin.REAL_TIME_COLLABORATION, new ExpectedPlugin("RealTimeCollaboration", new String[0]));
        expected.put(PremiumPlugin.REAL_TIME_COLLABORATIVE_EDITING,
            new ExpectedPlugin("RealTimeCollaborativeEditing", new String[0]));
        expected.put(PremiumPlugin.REAL_TIME_COLLABORATIVE_COMMENTS,
            new ExpectedPlugin("RealTimeCollaborativeComments", new String[0]));
        expected.put(PremiumPlugin.REAL_TIME_COLLABORATIVE_TRACK_CHANGES,
            new ExpectedPlugin("RealTimeCollaborativeTrackChanges", new String[0]));
        expected.put(PremiumPlugin.REAL_TIME_COLLABORATIVE_REVISION_HISTORY,
            new ExpectedPlugin("RealTimeCollaborativeRevisionHistory", new String[0]));
        expected.put(PremiumPlugin.PRESENCE_LIST, new ExpectedPlugin("PresenceList", new String[0]));
        expected.put(PremiumPlugin.PAGINATION, new ExpectedPlugin("Pagination", new String[]{"pagination"}));
        expected.put(PremiumPlugin.DOCUMENT_OUTLINE, new ExpectedPlugin("DocumentOutline", new String[0]));
        return expected;
    }

    private static final class ExpectedPlugin {
        private final String pluginName;
        private final String[] toolbarItems;

        private ExpectedPlugin(String pluginName, String[] toolbarItems) {
            this.pluginName = pluginName;
            this.toolbarItems = toolbarItems;
        }
    }
}
