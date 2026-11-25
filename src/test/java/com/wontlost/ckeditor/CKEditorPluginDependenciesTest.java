package com.wontlost.ckeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CKEditorPluginDependencies class.
 */
class CKEditorPluginDependenciesTest {

    @Test
    @DisplayName("Image plugins should depend on base Image plugin")
    void imagePluginsShouldDependOnBaseImage() {
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.IMAGE_TOOLBAR))
            .contains(CKEditorPlugin.IMAGE);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.IMAGE_CAPTION))
            .contains(CKEditorPlugin.IMAGE);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.IMAGE_STYLE))
            .contains(CKEditorPlugin.IMAGE);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.IMAGE_RESIZE))
            .contains(CKEditorPlugin.IMAGE);
    }

    @Test
    @DisplayName("Table plugins should depend on base Table plugin")
    void tablePluginsShouldDependOnBaseTable() {
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.TABLE_TOOLBAR))
            .contains(CKEditorPlugin.TABLE);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.TABLE_PROPERTIES))
            .contains(CKEditorPlugin.TABLE);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.TABLE_CELL_PROPERTIES))
            .contains(CKEditorPlugin.TABLE);
    }

    @Test
    @DisplayName("Style plugin should depend on GeneralHtmlSupport")
    void stylePluginShouldDependOnGeneralHtmlSupport() {
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.STYLE))
            .contains(CKEditorPlugin.GENERAL_HTML_SUPPORT);
    }

    @Test
    @DisplayName("LinkImage should depend on both Image and Link")
    void linkImageShouldDependOnImageAndLink() {
        Set<CKEditorPlugin> deps = CKEditorPluginDependencies.getDependencies(CKEditorPlugin.LINK_IMAGE);
        assertThat(deps).contains(CKEditorPlugin.IMAGE, CKEditorPlugin.LINK);
    }

    @Test
    @DisplayName("resolve should include core plugins by default")
    void resolveShouldIncludeCorePlugins() {
        Set<CKEditorPlugin> input = EnumSet.of(CKEditorPlugin.BOLD);
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(input);

        assertThat(resolved)
            .contains(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH, CKEditorPlugin.BOLD);
    }

    @Test
    @DisplayName("resolve should transitively resolve dependencies")
    void resolveShouldTransitivelyResolveDependencies() {
        // IMAGE_CAPTION depends on IMAGE
        Set<CKEditorPlugin> input = EnumSet.of(CKEditorPlugin.IMAGE_CAPTION);
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(input);

        assertThat(resolved)
            .contains(CKEditorPlugin.IMAGE_CAPTION, CKEditorPlugin.IMAGE);
    }

    @Test
    @DisplayName("resolve should handle complex dependency chains")
    void resolveShouldHandleComplexDependencyChains() {
        // LINK_IMAGE depends on IMAGE and LINK
        // AUTO_IMAGE depends on IMAGE and CLIPBOARD
        Set<CKEditorPlugin> input = EnumSet.of(CKEditorPlugin.LINK_IMAGE, CKEditorPlugin.AUTO_IMAGE);
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(input);

        assertThat(resolved).contains(
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.LINK,
            CKEditorPlugin.LINK_IMAGE,
            CKEditorPlugin.AUTO_IMAGE,
            CKEditorPlugin.CLIPBOARD
        );
    }

    @Test
    @DisplayName("resolve without core plugins should not include them")
    void resolveWithoutCorePluginsShouldNotIncludeThem() {
        Set<CKEditorPlugin> input = EnumSet.of(CKEditorPlugin.BOLD);
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(input, false);

        assertThat(resolved)
            .contains(CKEditorPlugin.BOLD)
            .doesNotContain(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH);
    }

    @Test
    @DisplayName("getRecommended should return recommended plugins")
    void getRecommendedShouldReturnRecommendedPlugins() {
        Set<CKEditorPlugin> recommended = CKEditorPluginDependencies.getRecommended(CKEditorPlugin.IMAGE);

        assertThat(recommended).contains(
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE
        );
    }

    @Test
    @DisplayName("resolveWithRecommended should include recommended plugins")
    void resolveWithRecommendedShouldIncludeRecommendedPlugins() {
        Set<CKEditorPlugin> input = EnumSet.of(CKEditorPlugin.IMAGE);
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolveWithRecommended(input);

        assertThat(resolved).contains(
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE
        );
    }

    @Test
    @DisplayName("hasDependencies should return correct value")
    void hasDependenciesShouldReturnCorrectValue() {
        assertThat(CKEditorPluginDependencies.hasDependencies(CKEditorPlugin.IMAGE_TOOLBAR)).isTrue();
        assertThat(CKEditorPluginDependencies.hasDependencies(CKEditorPlugin.BOLD)).isFalse();
        assertThat(CKEditorPluginDependencies.hasDependencies(CKEditorPlugin.ESSENTIALS)).isFalse();
    }

    @Test
    @DisplayName("getDependents should return plugins that depend on given plugin")
    void getDependentsShouldReturnCorrectPlugins() {
        Set<CKEditorPlugin> dependents = CKEditorPluginDependencies.getDependents(CKEditorPlugin.IMAGE);

        assertThat(dependents).contains(
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE,
            CKEditorPlugin.IMAGE_UPLOAD,
            CKEditorPlugin.IMAGE_INSERT,
            CKEditorPlugin.LINK_IMAGE,
            CKEditorPlugin.AUTO_IMAGE
        );
    }

    @Test
    @DisplayName("checkRemovalImpact should identify broken plugins")
    void checkRemovalImpactShouldIdentifyBrokenPlugins() {
        Set<CKEditorPlugin> current = EnumSet.of(
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION
        );

        Set<CKEditorPlugin> broken = CKEditorPluginDependencies.checkRemovalImpact(
            CKEditorPlugin.IMAGE, current);

        assertThat(broken).contains(CKEditorPlugin.IMAGE_TOOLBAR, CKEditorPlugin.IMAGE_CAPTION);
    }

    @Test
    @DisplayName("validateDependencies should find missing dependencies")
    void validateDependenciesShouldFindMissingDependencies() {
        // Add IMAGE_CAPTION without IMAGE - should report missing dependency
        Set<CKEditorPlugin> plugins = EnumSet.of(CKEditorPlugin.IMAGE_CAPTION, CKEditorPlugin.BOLD);

        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing =
            CKEditorPluginDependencies.validateDependencies(plugins);

        assertThat(missing).containsKey(CKEditorPlugin.IMAGE_CAPTION);
        assertThat(missing.get(CKEditorPlugin.IMAGE_CAPTION)).contains(CKEditorPlugin.IMAGE);
    }

    @Test
    @DisplayName("validateDependencies should return empty map when all satisfied")
    void validateDependenciesShouldReturnEmptyWhenAllSatisfied() {
        Set<CKEditorPlugin> plugins = EnumSet.of(
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_TOOLBAR
        );

        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing =
            CKEditorPluginDependencies.validateDependencies(plugins);

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("topologicalSort should order dependencies before dependents")
    void topologicalSortShouldOrderDependenciesFirst() {
        Set<CKEditorPlugin> plugins = EnumSet.of(
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR
        );

        List<CKEditorPlugin> sorted = CKEditorPluginDependencies.topologicalSort(plugins);

        // IMAGE should come before IMAGE_CAPTION and IMAGE_TOOLBAR
        int imageIndex = sorted.indexOf(CKEditorPlugin.IMAGE);
        int captionIndex = sorted.indexOf(CKEditorPlugin.IMAGE_CAPTION);
        int toolbarIndex = sorted.indexOf(CKEditorPlugin.IMAGE_TOOLBAR);

        assertThat(imageIndex).isLessThan(captionIndex);
        assertThat(imageIndex).isLessThan(toolbarIndex);
    }

    @Test
    @DisplayName("getLoadOrder should resolve and sort plugins")
    void getLoadOrderShouldResolveAndSort() {
        Set<CKEditorPlugin> plugins = EnumSet.of(CKEditorPlugin.IMAGE_CAPTION);

        List<CKEditorPlugin> loadOrder = CKEditorPluginDependencies.getLoadOrder(plugins);

        // Should include resolved dependencies
        assertThat(loadOrder).contains(
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH,
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_CAPTION
        );

        // IMAGE should come before IMAGE_CAPTION
        int imageIndex = loadOrder.indexOf(CKEditorPlugin.IMAGE);
        int captionIndex = loadOrder.indexOf(CKEditorPlugin.IMAGE_CAPTION);
        assertThat(imageIndex).isLessThan(captionIndex);
    }

    @Test
    @DisplayName("getDependencyTree should return formatted tree string")
    void getDependencyTreeShouldReturnFormattedTree() {
        String tree = CKEditorPluginDependencies.getDependencyTree(CKEditorPlugin.LINK_IMAGE);

        assertThat(tree)
            .contains("LinkImage")
            .contains("Image")
            .contains("Link");
    }

    @Test
    @DisplayName("getDependencies should return empty set for plugins without dependencies")
    void getDependenciesShouldReturnEmptyForIndependentPlugins() {
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.BOLD)).isEmpty();
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.ITALIC)).isEmpty();
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.HEADING)).isEmpty();
    }

    @Test
    @DisplayName("getRecommended should return empty set for plugins without recommendations")
    void getRecommendedShouldReturnEmptyWhenNoRecommendations() {
        assertThat(CKEditorPluginDependencies.getRecommended(CKEditorPlugin.BOLD)).isEmpty();
    }

    @Test
    @DisplayName("resolve should handle empty input")
    void resolveShouldHandleEmptyInput() {
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(EnumSet.noneOf(CKEditorPlugin.class));

        // Should still include core plugins
        assertThat(resolved).contains(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH);
        assertThat(resolved).hasSize(2);
    }

    // W13：SourceEditing / HtmlComment / HtmlEmbed 的 requires 都不含 GeneralHtmlSupport；强制加载 GHS 会改变数据输出
    @ParameterizedTest
    @EnumSource(value = CKEditorPlugin.class, names = {"SOURCE_EDITING", "HTML_COMMENT", "HTML_EMBED"})
    @DisplayName("SourceEditing, HtmlComment and HtmlEmbed recommend GeneralHtmlSupport but do not force it")
    void htmlPluginsDoNotForceGeneralHtmlSupport(CKEditorPlugin plugin) {
        assertThat(CKEditorPluginDependencies.getDependencies(plugin)).doesNotContain(CKEditorPlugin.GENERAL_HTML_SUPPORT);
        assertThat(CKEditorPluginDependencies.resolve(EnumSet.of(plugin))).doesNotContain(CKEditorPlugin.GENERAL_HTML_SUPPORT);
        assertThat(CKEditorPluginDependencies.getRecommended(plugin)).contains(CKEditorPlugin.GENERAL_HTML_SUPPORT);
        assertThat(CKEditorPluginDependencies.resolveWithRecommended(EnumSet.of(plugin)))
            .contains(CKEditorPlugin.GENERAL_HTML_SUPPORT);
    }

    @Test
    @DisplayName("Style still depends on GeneralHtmlSupport (StyleEditing requires it)")
    void styleDependsOnGeneralHtmlSupport() {
        assertThat(CKEditorPluginDependencies.resolve(EnumSet.of(CKEditorPlugin.STYLE)))
            .contains(CKEditorPlugin.GENERAL_HTML_SUPPORT);
    }

    // StandardEditingMode 与 RestrictedEditingMode 互斥；前端只保留互斥组的第一个，曾把用户选的 StandardEditingMode 丢掉
    @Test
    @DisplayName("StandardEditingMode does not pull in the mutually exclusive RestrictedEditingMode")
    void standardEditingModeIsStandalone() {
        assertThat(CKEditorPluginDependencies.resolve(EnumSet.of(CKEditorPlugin.STANDARD_EDITING_MODE)))
            .contains(CKEditorPlugin.STANDARD_EDITING_MODE)
            .doesNotContain(CKEditorPlugin.RESTRICTED_EDITING_MODE);
    }

    @Test
    @DisplayName("EmojiPicker, Minimap and BlockToolbar have no forced dependencies (empty requires, docs load them alone)")
    void standalonePlugins() {
        assertThat(CKEditorPluginDependencies.resolve(EnumSet.of(CKEditorPlugin.EMOJI_PICKER))).doesNotContain(CKEditorPlugin.EMOJI);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.MINIMAP)).isEmpty();
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.BLOCK_TOOLBAR)).isEmpty();
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.FULL_PAGE)).isEmpty();
    }

    @Test
    @DisplayName("premium: AIAssistant needs an adapter, not CloudServices; ProposedChanges needs DocumentCompare; MergeFields needs Mention")
    void premiumDependencyCorrections() {
        assertThat(CKEditorPluginDependencies.getPremiumDependencies("AIAssistant")).isEmpty();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("AIAssistant")).isFalse();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ProposedChanges")).isFalse();
        assertThat(CKEditorPluginDependencies.getPremiumPluginDependencies("ProposedChanges")).containsExactly("DocumentCompare");
        assertThat(CKEditorPluginDependencies.getPremiumDependencies("MergeFields")).containsExactly(CKEditorPlugin.MENTION);
        assertThat(CKEditorPluginDependencies.getPremiumDependencies("SlashCommand")).containsExactly(CKEditorPlugin.MENTION);

        assertThat(CKEditorPluginDependencies.validatePremiumPluginDependencies(List.of(CustomPlugin.fromPremium("ProposedChanges"))))
            .containsExactly(Map.entry("ProposedChanges", Set.of("DocumentCompare")));
        assertThat(CKEditorPluginDependencies.validatePremiumPluginDependencies(
            List.of(CustomPlugin.fromPremium("ProposedChanges"), CustomPlugin.fromPremium("DocumentCompare")))).isEmpty();

        assertThat(CKEditorPluginDependencies.isAiAssistantWithoutBuiltInAdapter(List.of(CustomPlugin.fromPremium("AIAssistant"))))
            .isTrue();
        assertThat(CKEditorPluginDependencies.isAiAssistantWithoutBuiltInAdapter(List.of(
            CustomPlugin.fromPremium("AIAssistant"), CustomPlugin.fromPremium("OpenAITextAdapter")))).isFalse();
        assertThat(CKEditorPluginDependencies.isAiAssistantWithoutBuiltInAdapter(List.of(CustomPlugin.fromPremium("AIChat"))))
            .isFalse();
        // AWSTextAdapter 已不可用（CKEditor 移除了 Amazon Bedrock 集成，使用即抛错），不能消除提示
        assertThat(CKEditorPluginDependencies.isAiAssistantWithoutBuiltInAdapter(List.of(
            CustomPlugin.fromPremium("AIAssistant"), CustomPlugin.fromPremium("AWSTextAdapter")))).isTrue();
    }

    @Test
    @DisplayName("Upload adapters should depend on ImageUpload")
    void uploadAdaptersShouldDependOnImageUpload() {
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.SIMPLE_UPLOAD_ADAPTER))
            .contains(CKEditorPlugin.IMAGE_UPLOAD);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.BASE64_UPLOAD_ADAPTER))
            .contains(CKEditorPlugin.IMAGE_UPLOAD);
    }

    // ==================== Cycle Detection / Topological Sort Tests ====================

    @Test
    @DisplayName("topologicalSort should complete for all plugins without hanging")
    void topologicalSortShouldCompleteForAllPlugins() {
        Set<CKEditorPlugin> allPlugins = EnumSet.allOf(CKEditorPlugin.class);

        // This would hang or throw if there were an undetected cycle
        List<CKEditorPlugin> sorted = CKEditorPluginDependencies.topologicalSort(allPlugins);

        assertThat(sorted)
            .isNotNull()
            .hasSize(allPlugins.size())
            .containsExactlyInAnyOrderElementsOf(allPlugins);
    }

    @Test
    @DisplayName("topologicalSort should place dependencies before dependents")
    void topologicalSortShouldPlaceDependenciesBeforeDependents() {
        Set<CKEditorPlugin> plugins = EnumSet.of(
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH
        );

        List<CKEditorPlugin> sorted = CKEditorPluginDependencies.topologicalSort(plugins);

        // IMAGE must come before IMAGE_CAPTION (IMAGE is a dependency of IMAGE_CAPTION)
        int imageIndex = sorted.indexOf(CKEditorPlugin.IMAGE);
        int captionIndex = sorted.indexOf(CKEditorPlugin.IMAGE_CAPTION);
        assertThat(imageIndex)
            .isLessThan(captionIndex)
            .as("IMAGE should be sorted before IMAGE_CAPTION");
    }

    @Test
    @DisplayName("topologicalSort should handle empty input")
    void topologicalSortShouldHandleEmptyInput() {
        List<CKEditorPlugin> sorted = CKEditorPluginDependencies.topologicalSort(
            EnumSet.noneOf(CKEditorPlugin.class));

        assertThat(sorted).isEmpty();
    }

    @Test
    @DisplayName("No circular dependencies should exist in the built-in dependency graph")
    void noCircularDependenciesShouldExistInBuiltInGraph() {
        // For each plugin with dependencies, verify no plugin depends on itself
        // directly or transitively by resolving a single plugin and checking the result
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            Set<CKEditorPlugin> directDeps = CKEditorPluginDependencies.getDependencies(plugin);
            // Resolve transitive dependencies (without core plugins to keep it focused)
            Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(
                EnumSet.of(plugin), false);
            // Remove the plugin itself — resolve() includes the input plugin
            resolved.remove(plugin);
            // The direct dependencies should not include the plugin itself
            assertThat(directDeps)
                .as("Plugin %s should not directly depend on itself", plugin.name())
                .doesNotContain(plugin);
        }
    }

    // ==================== Premium Dependency Tests ====================

    @Test
    @DisplayName("getPremiumDependencies should return dependencies for known premium plugin")
    void getPremiumDependenciesShouldReturnDependencies() {
        Set<CKEditorPlugin> deps = CKEditorPluginDependencies.getPremiumDependencies("ExportPdf");
        assertThat(deps).isNotNull();
    }

    @Test
    @DisplayName("getPremiumDependencies should return empty for unknown plugin")
    void getPremiumDependenciesShouldReturnEmptyForUnknown() {
        Set<CKEditorPlugin> deps = CKEditorPluginDependencies.getPremiumDependencies("NonExistentPlugin");
        assertThat(deps).isEmpty();
    }

    @Test
    @DisplayName("requiresCloudServices should return true for export/import and AI plugins")
    void requiresCloudServicesShouldReturnTrueForCloudDependentPlugins() {
        // Export/Import plugins
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ExportPdf")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ExportWord")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ImportWord")).isTrue();
        // CKEditor AI plugins whose requires chain includes CloudServices
        assertThat(CKEditorPluginDependencies.requiresCloudServices("AIChat")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("AIQuickActions")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("AIReviewMode")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("AITranslate")).isTrue();
        // Collaboration plugins（"RealTimeCollaboration" 不是插件类，umbrella 从未导出；真实类名是 RealTimeCollaborative*）
        assertThat(CKEditorPluginDependencies.requiresCloudServices("RealTimeCollaboration")).isFalse();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("RealTimeCollaborativeEditing")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("RealTimeContentFetcher")).isTrue();
        // 这些插件的 requires 链上没有 CloudServices（ckeditor5-premium-features 49.1.0，见 plugin-dependencies.test.ts）
        for (String name : List.of("AIAssistant", "AIEditorIntegration", "AIChatBalloon", "AIEditorConfig", "ProposedChanges")) {
            assertThat(CKEditorPluginDependencies.requiresCloudServices(name)).as(name).isFalse();
        }
    }

    @Test
    @DisplayName("CKBox 由开源 ckeditor5 包导出、属 premium feature：不在 premium 包清单中，isPremiumFeature，带上 CloudServices")
    void ckboxIsALicensedPluginOfTheCkeditor5PackageWithCloudServices() {
        assertThat(CKEditorPlugin.getPremiumFeaturePlugins())
            .contains(CKEditorPlugin.CKBOX, CKEditorPlugin.CKBOX_IMAGE_EDIT, CKEditorPlugin.TABLE_LAYOUT);
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            // 需要 license 的内置插件绝不会出现在 premium 包清单里（加载通道不同）
            assertThat(plugin.isPremium()).isFalse();
            if (plugin.isPremiumFeature()) {
                assertThat(CKEditorPluginDependencies.getKnownPremiumPlugins()).doesNotContain(plugin.getJsName());
            }
        }
        assertThat(CKEditorPlugin.BOLD.isPremiumFeature()).isFalse();
        assertThat(CKEditorPluginDependencies.getKnownPremiumPlugins()).doesNotContain("CKBox", "CKBoxImageEdit");
        assertThat(CKEditorPluginDependencies.requiresCloudServices("CKBox")).isFalse();
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.CKBOX))
            .contains(CKEditorPlugin.CLOUD_SERVICES, CKEditorPlugin.IMAGE_UPLOAD);
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.CKBOX_IMAGE_EDIT))
            .containsExactly(CKEditorPlugin.CKBOX);
        assertThat(CKEditorPluginDependencies.resolve(Set.of(CKEditorPlugin.CKBOX_IMAGE_EDIT)))
            .contains(CKEditorPlugin.CKBOX, CKEditorPlugin.CLOUD_SERVICES);
    }

    @Test
    @DisplayName("requiresCloudServices should return false for non-cloud-services plugins")
    void requiresCloudServicesShouldReturnFalseForNonCloudServices() {
        assertThat(CKEditorPluginDependencies.requiresCloudServices("Pagination")).isFalse();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("UnknownPlugin")).isFalse();
    }

    @Test
    @DisplayName("hasPremiumDependencies should detect known premium plugins")
    void hasPremiumDependenciesShouldDetectKnown() {
        assertThat(CKEditorPluginDependencies.hasPremiumDependencies("ExportPdf")).isTrue();
        assertThat(CKEditorPluginDependencies.hasPremiumDependencies("UnknownPlugin")).isFalse();
    }

    @Test
    @DisplayName("getKnownPremiumPlugins should return non-empty set")
    void getKnownPremiumPluginsShouldReturnNonEmptySet() {
        Set<String> known = CKEditorPluginDependencies.getKnownPremiumPlugins();
        assertThat(known).isNotEmpty();
        assertThat(known).contains("ExportPdf", "ExportWord", "ImportWord");
    }

    @Test
    @DisplayName("getCloudServicesRequiredPlugins should return collaboration plugins")
    void getCloudServicesRequiredPluginsShouldReturnCollaborationPlugins() {
        Set<String> csPlugins = CKEditorPluginDependencies.getCloudServicesRequiredPlugins();
        assertThat(csPlugins).isNotEmpty();
        assertThat(csPlugins).contains("ExportPdf", "ExportWord", "ImportWord");
    }

    @Test
    @DisplayName("resolveWithPremium should include premium dependencies")
    void resolveWithPremiumShouldIncludeDependencies() {
        Set<CKEditorPlugin> base = EnumSet.of(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH);
        List<CustomPlugin> premiumPlugins = List.of(CustomPlugin.fromPremium("ExportPdf"));

        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolveWithPremium(base, premiumPlugins);
        assertThat(resolved).isNotEmpty();
        assertThat(resolved).containsAll(base);
    }

    @Test
    @DisplayName("validatePremiumDependencies should return empty for satisfied dependencies")
    void validatePremiumDependenciesShouldReturnEmptyWhenSatisfied() {
        Set<CKEditorPlugin> allPlugins = EnumSet.allOf(CKEditorPlugin.class);
        List<CustomPlugin> premiumPlugins = List.of(CustomPlugin.fromPremium("ExportPdf"));

        Map<String, Set<CKEditorPlugin>> missing =
            CKEditorPluginDependencies.validatePremiumDependencies(allPlugins, premiumPlugins);
        assertThat(missing).isEmpty();
    }

    // ==================== review: cycle fail-fast + collaboration soft-dep ====================

    @Test
    @DisplayName("topologicalSort returns a complete order for the (acyclic) real plugin graph")
    void topologicalSortIsCompleteForRealGraph() {
        // 真实静态依赖图无环，排序应返回全部输入插件，顺序中依赖先于被依赖者
        Set<CKEditorPlugin> input = EnumSet.of(
            CKEditorPlugin.TABLE_LAYOUT, CKEditorPlugin.TABLE,
            CKEditorPlugin.TABLE_COLUMN_RESIZE, CKEditorPlugin.ESSENTIALS);
        List<CKEditorPlugin> sorted = CKEditorPluginDependencies.topologicalSort(input);

        assertThat(sorted).containsExactlyInAnyOrderElementsOf(input);
        // 依赖必须排在被依赖者之前
        assertThat(sorted.indexOf(CKEditorPlugin.TABLE_COLUMN_RESIZE))
            .isLessThan(sorted.indexOf(CKEditorPlugin.TABLE_LAYOUT));
    }

    @Test
    @DisplayName("getCollaborationPluginsMissingCloudServices flags Comments/TrackChanges without CloudServices")
    void collaborationMissingCloudServicesFlagged() {
        Set<CKEditorPlugin> base = EnumSet.of(CKEditorPlugin.ESSENTIALS);
        List<CustomPlugin> premium = List.of(
            CustomPlugin.fromPremium("Comments"),
            CustomPlugin.fromPremium("TrackChanges"));

        Set<String> missing =
            CKEditorPluginDependencies.getCollaborationPluginsMissingCloudServices(premium, base);

        assertThat(missing).containsExactlyInAnyOrder("Comments", "TrackChanges");
    }

    @Test
    @DisplayName("collaboration advisory is silent once CloudServices is present")
    void collaborationAdvisorySilentWithCloudServices() {
        Set<CKEditorPlugin> withCloud = EnumSet.of(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.CLOUD_SERVICES);
        List<CustomPlugin> premium = List.of(CustomPlugin.fromPremium("Comments"));

        assertThat(CKEditorPluginDependencies.getCollaborationPluginsMissingCloudServices(premium, withCloud))
            .isEmpty();
    }

    @Test
    @DisplayName("collaboration advisory ignores non-collaboration premium plugins")
    void collaborationAdvisoryIgnoresOtherPremium() {
        Set<CKEditorPlugin> base = EnumSet.of(CKEditorPlugin.ESSENTIALS);
        List<CustomPlugin> premium = List.of(CustomPlugin.fromPremium("ExportPdf"));

        assertThat(CKEditorPluginDependencies.getCollaborationPluginsMissingCloudServices(premium, base))
            .isEmpty();
    }

    @Test
    @DisplayName("collaboration advisory is null-safe")
    void collaborationAdvisoryNullSafe() {
        assertThat(CKEditorPluginDependencies.getCollaborationPluginsMissingCloudServices(null, null))
            .isEmpty();
    }

    // ==================== OSGi Manifest Validation ====================

    @Test
    @DisplayName("bnd.bnd should exist and contain required OSGi headers")
    void bndFileShouldContainRequiredHeaders() throws IOException {
        // Resolve via Maven basedir property for IDE/CI consistency
        String basedir = System.getProperty("basedir", ".");
        Path bndFile = Path.of(basedir, "bnd.bnd");
        assertThat(bndFile).exists();

        String content = Files.readString(bndFile);
        assertThat(content)
            .contains("Bundle-SymbolicName:")
            .contains("Export-Package:")
            .contains("com.wontlost.ckeditor");
    }

    @Test
    @DisplayName("issue #143: bare SpecialCharacters resolves to include the Essentials categories")
    void bareSpecialCharactersShouldPullInEssentials() {
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(
            Set.of(CKEditorPlugin.SPECIAL_CHARACTERS));

        assertThat(resolved).contains(CKEditorPlugin.SPECIAL_CHARACTERS_ESSENTIALS);
    }

    @Test
    @DisplayName("issue #143: SpecialCharacters with an explicit category keeps that selection")
    void specialCharactersWithCategoryShouldNotForceEssentials() {
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(
            Set.of(CKEditorPlugin.SPECIAL_CHARACTERS, CKEditorPlugin.SPECIAL_CHARACTERS_LATIN));

        assertThat(resolved)
            .contains(CKEditorPlugin.SPECIAL_CHARACTERS, CKEditorPlugin.SPECIAL_CHARACTERS_LATIN)
            .doesNotContain(CKEditorPlugin.SPECIAL_CHARACTERS_ESSENTIALS);
    }

    @Test
    @DisplayName("issue #143: a category alone still pulls in SpecialCharacters only")
    void categoryAloneShouldResolveBaseOnly() {
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(
            Set.of(CKEditorPlugin.SPECIAL_CHARACTERS_ARROWS));

        assertThat(resolved)
            .contains(CKEditorPlugin.SPECIAL_CHARACTERS)
            .doesNotContain(CKEditorPlugin.SPECIAL_CHARACTERS_ESSENTIALS);
    }

    @Test
    @DisplayName("TableLayout (premium feature exported by the open-source ckeditor5 package) depends on Table + TableColumnResize")
    void tableLayoutShouldResolveTableDependencies() {
        Set<CKEditorPlugin> resolved = CKEditorPluginDependencies.resolve(Set.of(CKEditorPlugin.TABLE_LAYOUT));

        assertThat(resolved).contains(CKEditorPlugin.TABLE, CKEditorPlugin.TABLE_COLUMN_RESIZE);
        assertThat(CKEditorPluginDependencies.getPremiumDependencies("TableLayout")).isEmpty();
    }

    @Test
    @DisplayName("issue #143: STRICT validation reports bare SpecialCharacters as missing a category")
    void validateDependenciesShouldReportSpecialCharactersWithoutCategory() {
        // STRICT 报出的缺失项必须与 AUTO_RESOLVE 会补齐的项一致，否则两种模式对同一输入结论相左
        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing = CKEditorPluginDependencies.validateDependencies(
            Set.of(CKEditorPlugin.SPECIAL_CHARACTERS));

        assertThat(missing)
            .containsEntry(CKEditorPlugin.SPECIAL_CHARACTERS, Set.of(CKEditorPlugin.SPECIAL_CHARACTERS_ESSENTIALS));
    }

    @Test
    @DisplayName("issue #143: STRICT validation accepts SpecialCharacters with any explicit category")
    void validateDependenciesShouldAcceptSpecialCharactersWithCategory() {
        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing = CKEditorPluginDependencies.validateDependencies(
            Set.of(CKEditorPlugin.SPECIAL_CHARACTERS, CKEditorPlugin.SPECIAL_CHARACTERS_CURRENCY));

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("issue #143: STRICT builder rejects bare SpecialCharacters with an actionable message")
    void strictBuilderShouldRejectBareSpecialCharacters() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.STRICT)
            .withPlugins(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH, CKEditorPlugin.SPECIAL_CHARACTERS);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("SpecialCharacters requires: SpecialCharactersEssentials");
    }
}
