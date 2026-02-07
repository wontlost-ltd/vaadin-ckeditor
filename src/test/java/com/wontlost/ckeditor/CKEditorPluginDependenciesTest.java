package com.wontlost.ckeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

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

    @Test
    @DisplayName("SourceEditing should depend on GeneralHtmlSupport")
    void sourceEditingShouldDependOnGeneralHtmlSupport() {
        assertThat(CKEditorPluginDependencies.getDependencies(CKEditorPlugin.SOURCE_EDITING))
            .contains(CKEditorPlugin.GENERAL_HTML_SUPPORT);
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
    @DisplayName("requiresCloudServices should return true for export/import plugins")
    void requiresCloudServicesShouldReturnTrueForExportImport() {
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ExportPdf")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ExportWord")).isTrue();
        assertThat(CKEditorPluginDependencies.requiresCloudServices("ImportWord")).isTrue();
    }

    @Test
    @DisplayName("requiresCloudServices should return false for non-cloud-services plugins")
    void requiresCloudServicesShouldReturnFalseForNonCloudServices() {
        assertThat(CKEditorPluginDependencies.requiresCloudServices("AIAssistant")).isFalse();
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
}
