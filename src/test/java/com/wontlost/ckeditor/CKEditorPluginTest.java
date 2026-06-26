package com.wontlost.ckeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CKEditorPlugin enum.
 */
class CKEditorPluginTest {

    @Test
    @DisplayName("All plugins should have non-null JS names")
    void allPluginsShouldHaveJsNames() {
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            assertThat(plugin.getJsName())
                .as("Plugin %s should have a JS name", plugin.name())
                .isNotNull()
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("All plugins should have a category")
    void allPluginsShouldHaveCategory() {
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            assertThat(plugin.getCategory())
                .as("Plugin %s should have a category", plugin.name())
                .isNotNull();
        }
    }

    @Test
    @DisplayName("Core plugins should exist")
    void corePluginsShouldExist() {
        assertThat(CKEditorPlugin.ESSENTIALS).isNotNull();
        assertThat(CKEditorPlugin.PARAGRAPH).isNotNull();
        assertThat(CKEditorPlugin.UNDO).isNotNull();
    }

    @Test
    @DisplayName("Basic style plugins should have correct toolbar items")
    void basicStylePluginsShouldHaveToolbarItems() {
        assertThat(CKEditorPlugin.BOLD.getToolbarItems()).contains("bold");
        assertThat(CKEditorPlugin.ITALIC.getToolbarItems()).contains("italic");
        assertThat(CKEditorPlugin.UNDERLINE.getToolbarItems()).contains("underline");
    }

    @Test
    @DisplayName("All built-in plugins should not be marked as premium")
    void allBuiltInPluginsShouldNotBePremium() {
        // All built-in plugins are free - premium features require
        // the ckeditor5-premium-features package and CustomPlugin
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            assertThat(plugin.isPremium())
                .as("Plugin %s should not be premium", plugin.name())
                .isFalse();
        }
    }

    @Test
    @DisplayName("getByCategory should return correct plugins")
    void getByCategoryShouldReturnCorrectPlugins() {
        Set<CKEditorPlugin> basicStyles = CKEditorPlugin.getByCategory(CKEditorPlugin.Category.BASIC_STYLES);

        assertThat(basicStyles)
            .contains(CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC, CKEditorPlugin.UNDERLINE);
    }

    @Test
    @DisplayName("fromJsName should find plugin by JS name")
    void fromJsNameShouldFindPlugin() {
        assertThat(CKEditorPlugin.fromJsName("Bold")).isEqualTo(CKEditorPlugin.BOLD);
        assertThat(CKEditorPlugin.fromJsName("Table")).isEqualTo(CKEditorPlugin.TABLE);
        assertThat(CKEditorPlugin.fromJsName("NonExistent")).isNull();
    }

    @Test
    @DisplayName("List plugin should have multiple toolbar items")
    void listPluginShouldHaveMultipleToolbarItems() {
        Set<String> toolbarItems = CKEditorPlugin.LIST.getToolbarItems();

        assertThat(toolbarItems)
            .hasSize(2)
            .contains("bulletedList", "numberedList");
    }

    @Test
    @DisplayName("Category enum should have display names")
    void categoryShouldHaveDisplayNames() {
        assertThat(CKEditorPlugin.Category.CORE.getDisplayName()).isEqualTo("Core");
        assertThat(CKEditorPlugin.Category.BASIC_STYLES.getDisplayName()).isEqualTo("Basic Styles");
        assertThat(CKEditorPlugin.Category.CUSTOM.getDisplayName()).isEqualTo("Custom");
    }

    @Test
    @DisplayName("STYLE plugin should have style toolbar item")
    void stylePluginShouldHaveStyleToolbarItem() {
        assertThat(CKEditorPlugin.STYLE.getToolbarItems()).contains("style");
        assertThat(CKEditorPlugin.STYLE.getCategory()).isEqualTo(CKEditorPlugin.Category.HTML);
        assertThat(CKEditorPlugin.STYLE.getJsName()).isEqualTo("Style");
    }

    @Test
    @DisplayName("新增的免费 media-embed 配套插件应存在且分类正确")
    void mediaEmbedCompanionFreePluginsShouldExist() {
        assertThat(CKEditorPlugin.AUTO_MEDIA_EMBED.getJsName()).isEqualTo("AutoMediaEmbed");
        assertThat(CKEditorPlugin.AUTO_MEDIA_EMBED.getCategory()).isEqualTo(CKEditorPlugin.Category.MEDIA);

        assertThat(CKEditorPlugin.MEDIA_EMBED_STYLE.getJsName()).isEqualTo("MediaEmbedStyle");
        assertThat(CKEditorPlugin.MEDIA_EMBED_STYLE.getCategory()).isEqualTo(CKEditorPlugin.Category.MEDIA);
        assertThat(CKEditorPlugin.MEDIA_EMBED_STYLE.getToolbarItems())
            .contains("mediaEmbed:alignLeft", "mediaEmbed:alignCenter", "mediaEmbed:alignRight");

        assertThat(CKEditorPlugin.MEDIA_EMBED_TOOLBAR.getJsName()).isEqualTo("MediaEmbedToolbar");
        assertThat(CKEditorPlugin.MEDIA_EMBED_TOOLBAR.getCategory()).isEqualTo(CKEditorPlugin.Category.MEDIA);
    }

    @Test
    @DisplayName("CKFinder 应作为免费上传类插件存在")
    void ckFinderShouldExistAsUploadPlugin() {
        assertThat(CKEditorPlugin.CKFINDER.getJsName()).isEqualTo("CKFinder");
        assertThat(CKEditorPlugin.CKFINDER.getCategory()).isEqualTo(CKEditorPlugin.Category.UPLOAD);
    }

    @Test
    @DisplayName("LineHeight 不应再出现在免费插件枚举中（实为 premium）")
    void lineHeightShouldNotBeAFreePlugin() {
        // LineHeight 在 ckeditor5 48.x 属 premium（见 VaadinCKEditorPremium.PremiumPlugin.LINE_HEIGHT），
        // 不由 umbrella ckeditor5 导出，故不得作为免费内置插件存在。
        assertThat(CKEditorPlugin.fromJsName("LineHeight")).isNull();
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            assertThat(plugin.getJsName())
                .as("免费枚举不应包含 LineHeight")
                .isNotEqualTo("LineHeight");
        }
    }
}
