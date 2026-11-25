package com.wontlost.ckeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CKEditorPreset enum.
 */
class CKEditorPresetTest {

    @Test
    @DisplayName("All presets should have display names")
    void allPresetsShouldHaveDisplayNames() {
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            assertThat(preset.getDisplayName())
                .as("Preset %s should have a display name", preset.name())
                .isNotNull()
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("All presets should have plugins")
    void allPresetsShouldHavePlugins() {
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            assertThat(preset.getPlugins())
                .as("Preset %s should have plugins", preset.name())
                .isNotNull()
                .isNotEmpty();
        }
    }

    @Test
    @DisplayName("All presets should include core plugins")
    void allPresetsShouldIncludeCorePlugins() {
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            assertThat(preset.getPlugins())
                .as("Preset %s should include ESSENTIALS", preset.name())
                .contains(CKEditorPlugin.ESSENTIALS);

            assertThat(preset.getPlugins())
                .as("Preset %s should include PARAGRAPH", preset.name())
                .contains(CKEditorPlugin.PARAGRAPH);
        }
    }

    @Test
    @DisplayName("BASIC preset should have minimal plugins")
    void basicPresetShouldHaveMinimalPlugins() {
        Set<CKEditorPlugin> plugins = CKEditorPreset.BASIC.getPlugins();

        assertThat(plugins)
            .contains(
                CKEditorPlugin.ESSENTIALS,
                CKEditorPlugin.PARAGRAPH,
                CKEditorPlugin.BOLD,
                CKEditorPlugin.ITALIC,
                CKEditorPlugin.LINK
            );

        // Should not include advanced features
        assertThat(plugins)
            .doesNotContain(
                CKEditorPlugin.TABLE,
                CKEditorPlugin.CODE_BLOCK,
                CKEditorPlugin.FONT_SIZE
            );
    }

    @Test
    @DisplayName("STANDARD preset should have more plugins than BASIC")
    void standardPresetShouldHaveMorePluginsThanBasic() {
        assertThat(CKEditorPreset.STANDARD.getPlugins().size())
            .isGreaterThan(CKEditorPreset.BASIC.getPlugins().size());
    }

    @Test
    @DisplayName("FULL preset should have more plugins than BASIC and STANDARD")
    void fullPresetShouldHaveMorePluginsThanBasicPresets() {
        int fullSize = CKEditorPreset.FULL.getPlugins().size();

        // FULL preset is optimized for fast loading, so it may have fewer plugins than DOCUMENT
        assertThat(fullSize)
            .isGreaterThan(CKEditorPreset.STANDARD.getPlugins().size())
            .isGreaterThan(CKEditorPreset.BASIC.getPlugins().size());
    }

    @Test
    @DisplayName("EMPTY preset should only have core plugins")
    void emptyPresetShouldOnlyHaveCorePlugins() {
        Set<CKEditorPlugin> plugins = CKEditorPreset.EMPTY.getPlugins();

        assertThat(plugins)
            .hasSize(2)
            .containsExactlyInAnyOrder(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH);
    }

    @Test
    @DisplayName("All presets should have default toolbar")
    void allPresetsShouldHaveDefaultToolbar() {
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            assertThat(preset.getDefaultToolbar())
                .as("Preset %s should have a toolbar array", preset.name())
                .isNotNull();
        }
    }

    @Test
    @DisplayName("hasPlugin should correctly check plugin existence")
    void hasPluginShouldCorrectlyCheckPluginExistence() {
        assertThat(CKEditorPreset.STANDARD.hasPlugin(CKEditorPlugin.BOLD)).isTrue();
        assertThat(CKEditorPreset.STANDARD.hasPlugin(CKEditorPlugin.TABLE)).isTrue();
        assertThat(CKEditorPreset.BASIC.hasPlugin(CKEditorPlugin.TABLE)).isFalse();
    }

    @Test
    @DisplayName("getEstimatedSize should return reasonable values")
    void getEstimatedSizeShouldReturnReasonableValues() {
        assertThat(CKEditorPreset.EMPTY.getEstimatedSize()).isLessThan(CKEditorPreset.BASIC.getEstimatedSize());
        assertThat(CKEditorPreset.BASIC.getEstimatedSize()).isLessThan(CKEditorPreset.STANDARD.getEstimatedSize());
        assertThat(CKEditorPreset.STANDARD.getEstimatedSize()).isLessThan(CKEditorPreset.FULL.getEstimatedSize());
    }

    @Test
    @DisplayName("DOCUMENT preset should include document-specific plugins")
    void documentPresetShouldIncludeDocumentPlugins() {
        Set<CKEditorPlugin> plugins = CKEditorPreset.DOCUMENT.getPlugins();

        assertThat(plugins)
            .contains(
                CKEditorPlugin.WORD_COUNT,
                CKEditorPlugin.PAGE_BREAK,
                CKEditorPlugin.AUTOSAVE
            );
    }

    // Title 以前被前端当作“需配置”丢弃，预设从未真正加载它；现在它会加载，所以预设不再列出它，保持原有输出
    @Test
    @DisplayName("no preset lists TITLE (it never loaded before 6.2.0 and would change the document structure)")
    void noPresetListsTitle() {
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            assertThat(preset.getPlugins()).as(preset.name()).doesNotContain(CKEditorPlugin.TITLE);
        }
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("所有内置 preset 都必须能在 STRICT 依赖模式下构建")
    void allPresetsBuildUnderStrictMode() {
        // review (Codex): 依赖测试此前只覆盖直接依赖，未验证「preset 自身是否自洽」。
        // 实际上 AI_DOCUMENT / EMAIL / NOTION 三个内置 preset 缺少必需依赖，
        // 在 STRICT 模式下会直接抛 IllegalStateException——内置 preset 被内置模式拒绝。
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> VaadinCKEditor.create()
                    .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.STRICT)
                    .withPreset(preset)
                    .build(),
                "preset " + preset + " 必须在 STRICT 模式下可构建");
        }
    }

    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("传递依赖缺失必须被 STRICT 校验发现")
    void validateDependenciesCoversTransitiveClosure() {
        // 只声明 SIMPLE_UPLOAD_ADAPTER：它直接依赖 IMAGE_UPLOAD，
        // 而 IMAGE_UPLOAD 又依赖 IMAGE。若校验只看一层，就会漏报 IMAGE。
        java.util.Set<CKEditorPlugin> plugins =
            java.util.EnumSet.of(CKEditorPlugin.SIMPLE_UPLOAD_ADAPTER);
        java.util.Map<CKEditorPlugin, java.util.Set<CKEditorPlugin>> missing =
            CKEditorPluginDependencies.validateDependencies(plugins);

        java.util.Set<CKEditorPlugin> allMissing = java.util.EnumSet.noneOf(CKEditorPlugin.class);
        missing.values().forEach(allMissing::addAll);

        org.assertj.core.api.Assertions.assertThat(allMissing)
            .as("必须同时报出直接依赖与传递依赖，否则 STRICT 与 AUTO_RESOLVE 结论不一致")
            .contains(CKEditorPlugin.IMAGE_UPLOAD, CKEditorPlugin.IMAGE);
    }
}
