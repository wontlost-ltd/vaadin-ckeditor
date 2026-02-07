package com.wontlost.ckeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CustomPlugin class.
 */
class CustomPluginTest {

    @Test
    @DisplayName("Builder should create plugin with required name")
    void builderShouldCreatePluginWithRequiredName() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin").build();

        assertThat(plugin.getJsName()).isEqualTo("MyPlugin");
        assertThat(plugin.isPremium()).isFalse();
        assertThat(plugin.getImportPath()).isNull();
    }

    @Test
    @DisplayName("Builder should allow setting import path")
    void builderShouldAllowSettingImportPath() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
            .withImportPath("my-ckeditor-plugin")
            .build();

        assertThat(plugin.getImportPath()).isEqualTo("my-ckeditor-plugin");
    }

    @Test
    @DisplayName("Builder should allow setting toolbar items")
    void builderShouldAllowSettingToolbarItems() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
            .withToolbarItems("myButton", "myDropdown")
            .build();

        assertThat(plugin.getToolbarItems())
            .hasSize(2)
            .contains("myButton", "myDropdown");
    }

    @Test
    @DisplayName("Builder should allow setting dependencies")
    void builderShouldAllowSettingDependencies() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
            .withDependencies("Bold", "Italic")
            .build();

        assertThat(plugin.getDependencies())
            .hasSize(2)
            .contains("Bold", "Italic");
    }

    @Test
    @DisplayName("Builder should allow marking as premium")
    void builderShouldAllowMarkingAsPremium() {
        CustomPlugin plugin = CustomPlugin.builder("MyPremiumPlugin")
            .premium()
            .build();

        assertThat(plugin.isPremium()).isTrue();
    }

    @Test
    @DisplayName("of factory should create plugin with name and import path")
    void ofFactoryShouldCreatePluginWithNameAndImportPath() {
        CustomPlugin plugin = CustomPlugin.of("MyPlugin", "@scope/my-plugin");

        assertThat(plugin.getJsName()).isEqualTo("MyPlugin");
        assertThat(plugin.getImportPath()).isEqualTo("@scope/my-plugin");
        assertThat(plugin.isPremium()).isFalse();
    }

    @Test
    @DisplayName("fromCKEditor5 factory should create plugin without import path")
    void fromCKEditor5FactoryShouldCreatePluginWithoutImportPath() {
        CustomPlugin plugin = CustomPlugin.fromCKEditor5("SomeBuiltInPlugin");

        assertThat(plugin.getJsName()).isEqualTo("SomeBuiltInPlugin");
        assertThat(plugin.getImportPath()).isNull();
        assertThat(plugin.isPremium()).isFalse();
    }

    @Test
    @DisplayName("fromPremium factory should create premium plugin")
    void fromPremiumFactoryShouldCreatePremiumPlugin() {
        CustomPlugin plugin = CustomPlugin.fromPremium("PremiumFeature");

        assertThat(plugin.getJsName()).isEqualTo("PremiumFeature");
        assertThat(plugin.isPremium()).isTrue();
    }

    @Test
    @DisplayName("Null name should throw exception")
    void nullNameShouldThrowException() {
        assertThatThrownBy(() -> CustomPlugin.builder(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("null");
    }

    @Test
    @DisplayName("Plugins with same name should be equal")
    void pluginsWithSameNameShouldBeEqual() {
        CustomPlugin plugin1 = CustomPlugin.builder("MyPlugin").build();
        CustomPlugin plugin2 = CustomPlugin.builder("MyPlugin")
            .withImportPath("different-path")
            .build();

        assertThat(plugin1).isEqualTo(plugin2);
        assertThat(plugin1.hashCode()).isEqualTo(plugin2.hashCode());
    }

    @Test
    @DisplayName("Plugins with different names should not be equal")
    void pluginsWithDifferentNamesShouldNotBeEqual() {
        CustomPlugin plugin1 = CustomPlugin.builder("Plugin1").build();
        CustomPlugin plugin2 = CustomPlugin.builder("Plugin2").build();

        assertThat(plugin1).isNotEqualTo(plugin2);
    }

    @Test
    @DisplayName("toString should include plugin information")
    void toStringShouldIncludePluginInfo() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
            .withImportPath("my-plugin-path")
            .premium()
            .build();

        String str = plugin.toString();

        assertThat(str).contains("MyPlugin");
        assertThat(str).contains("my-plugin-path");
        assertThat(str).contains("true"); // isPremium
    }

    @Test
    @DisplayName("Empty toolbar items should return empty set")
    void emptyToolbarItemsShouldReturnEmptySet() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin").build();

        assertThat(plugin.getToolbarItems()).isEmpty();
    }

    @Test
    @DisplayName("Empty dependencies should return empty set")
    void emptyDependenciesShouldReturnEmptySet() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin").build();

        assertThat(plugin.getDependencies()).isEmpty();
    }

    // ==================== Premium Plugin Tests ====================

    @Test
    @DisplayName("fromPremium should create ExportPdf plugin")
    void fromPremiumShouldCreateExportPdfPlugin() {
        CustomPlugin plugin = CustomPlugin.fromPremium("ExportPdf");

        assertThat(plugin.getJsName()).isEqualTo("ExportPdf");
        assertThat(plugin.isPremium()).isTrue();
        assertThat(plugin.getImportPath()).isNull(); // Premium uses dynamic import
    }

    @Test
    @DisplayName("fromPremium should create ExportWord plugin")
    void fromPremiumShouldCreateExportWordPlugin() {
        CustomPlugin plugin = CustomPlugin.fromPremium("ExportWord");

        assertThat(plugin.getJsName()).isEqualTo("ExportWord");
        assertThat(plugin.isPremium()).isTrue();
    }

    @Test
    @DisplayName("fromPremium should create ImportWord plugin")
    void fromPremiumShouldCreateImportWordPlugin() {
        CustomPlugin plugin = CustomPlugin.fromPremium("ImportWord");

        assertThat(plugin.getJsName()).isEqualTo("ImportWord");
        assertThat(plugin.isPremium()).isTrue();
    }

    @Test
    @DisplayName("fromPremium should create collaboration plugins")
    void fromPremiumShouldCreateCollaborationPlugins() {
        CustomPlugin comments = CustomPlugin.fromPremium("Comments");
        CustomPlugin trackChanges = CustomPlugin.fromPremium("TrackChanges");
        CustomPlugin revisionHistory = CustomPlugin.fromPremium("RevisionHistory");

        assertThat(comments.isPremium()).isTrue();
        assertThat(trackChanges.isPremium()).isTrue();
        assertThat(revisionHistory.isPremium()).isTrue();
    }

    @Test
    @DisplayName("fromPremium should create productivity plugins")
    void fromPremiumShouldCreateProductivityPlugins() {
        CustomPlugin formatPainter = CustomPlugin.fromPremium("FormatPainter");
        CustomPlugin slashCommand = CustomPlugin.fromPremium("SlashCommand");
        CustomPlugin template = CustomPlugin.fromPremium("Template");
        CustomPlugin caseChange = CustomPlugin.fromPremium("CaseChange");

        assertThat(formatPainter.isPremium()).isTrue();
        assertThat(slashCommand.isPremium()).isTrue();
        assertThat(template.isPremium()).isTrue();
        assertThat(caseChange.isPremium()).isTrue();
    }

    @Test
    @DisplayName("fromPremium should create AI plugin")
    void fromPremiumShouldCreateAIPlugin() {
        CustomPlugin aiAssistant = CustomPlugin.fromPremium("AIAssistant");

        assertThat(aiAssistant.getJsName()).isEqualTo("AIAssistant");
        assertThat(aiAssistant.isPremium()).isTrue();
    }

    // ==================== Builder Chaining Tests ====================

    @Test
    @DisplayName("Builder should support full chaining")
    void builderShouldSupportFullChaining() {
        CustomPlugin plugin = CustomPlugin.builder("CompletePlugin")
            .withImportPath("@my/complete-plugin")
            .withToolbarItems("button1", "button2")
            .withDependencies("Bold", "Italic")
            .premium()
            .build();

        assertThat(plugin.getJsName()).isEqualTo("CompletePlugin");
        assertThat(plugin.getImportPath()).isEqualTo("@my/complete-plugin");
        assertThat(plugin.getToolbarItems()).containsExactlyInAnyOrder("button1", "button2");
        assertThat(plugin.getDependencies()).containsExactlyInAnyOrder("Bold", "Italic");
        assertThat(plugin.isPremium()).isTrue();
    }

    @Test
    @DisplayName("Builder can be reused for multiple plugins")
    void builderCanBeReusedForMultipleConfigurations() {
        // This test verifies that each build creates independent plugin
        CustomPlugin plugin1 = CustomPlugin.builder("Plugin1")
            .withToolbarItems("item1")
            .build();

        CustomPlugin plugin2 = CustomPlugin.builder("Plugin2")
            .withToolbarItems("item2", "item3")
            .build();

        assertThat(plugin1.getToolbarItems()).containsOnly("item1");
        assertThat(plugin2.getToolbarItems()).containsExactlyInAnyOrder("item2", "item3");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Plugin with empty name should be valid")
    void pluginWithEmptyNameShouldBeValid() {
        CustomPlugin plugin = CustomPlugin.builder("").build();

        assertThat(plugin.getJsName()).isEmpty();
    }

    @Test
    @DisplayName("Plugin with special characters in name should work")
    void pluginWithSpecialCharactersInNameShouldWork() {
        CustomPlugin plugin = CustomPlugin.builder("My-Plugin_v2.0").build();

        assertThat(plugin.getJsName()).isEqualTo("My-Plugin_v2.0");
    }

    @Test
    @DisplayName("Plugin with scoped npm package path should work")
    void pluginWithScopedNpmPackagePathShouldWork() {
        CustomPlugin plugin = CustomPlugin.builder("ScopedPlugin")
            .withImportPath("@organization/ckeditor-feature")
            .build();

        assertThat(plugin.getImportPath()).isEqualTo("@organization/ckeditor-feature");
    }

    @Test
    @DisplayName("Single toolbar item should be stored correctly")
    void singleToolbarItemShouldBeStoredCorrectly() {
        CustomPlugin plugin = CustomPlugin.builder("SingleButton")
            .withToolbarItems("onlyButton")
            .build();

        assertThat(plugin.getToolbarItems()).containsExactly("onlyButton");
    }

    @Test
    @DisplayName("Single dependency should be stored correctly")
    void singleDependencyShouldBeStoredCorrectly() {
        CustomPlugin plugin = CustomPlugin.builder("Dependent")
            .withDependencies("Essentials")
            .build();

        assertThat(plugin.getDependencies()).containsExactly("Essentials");
    }

    // ==================== Equality and HashCode ====================

    @Test
    @DisplayName("Premium and non-premium plugins with same name should be equal")
    void premiumAndNonPremiumPluginsWithSameNameShouldBeEqual() {
        CustomPlugin regular = CustomPlugin.builder("SamePlugin").build();
        CustomPlugin premium = CustomPlugin.builder("SamePlugin").premium().build();

        // Equality is based on name only
        assertThat(regular).isEqualTo(premium);
    }

    @Test
    @DisplayName("Plugin should not equal null")
    void pluginShouldNotEqualNull() {
        CustomPlugin plugin = CustomPlugin.builder("TestPlugin").build();

        assertThat(plugin).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Plugin should not equal different type")
    void pluginShouldNotEqualDifferentType() {
        CustomPlugin plugin = CustomPlugin.builder("TestPlugin").build();

        assertThat(plugin).isNotEqualTo("TestPlugin");
        assertThat(plugin).isNotEqualTo(123);
    }

    @Test
    @DisplayName("Plugin should equal itself")
    void pluginShouldEqualItself() {
        CustomPlugin plugin = CustomPlugin.builder("TestPlugin").build();

        assertThat(plugin).isEqualTo(plugin);
    }

    // ==================== Import Path Validation ====================

    @Test
    @DisplayName("Absolute Unix path should be rejected")
    void absoluteUnixPathShouldBeRejected() {
        assertThatThrownBy(() -> CustomPlugin.builder("MyPlugin")
            .withImportPath("/etc/passwd")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Absolute paths");
    }

    @Test
    @DisplayName("Windows drive path should be rejected")
    void windowsDrivePathShouldBeRejected() {
        assertThatThrownBy(() -> CustomPlugin.builder("MyPlugin")
            .withImportPath("C:\\Windows\\System32")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Absolute paths");
    }

    @Test
    @DisplayName("Windows UNC path should be rejected")
    void windowsUncPathShouldBeRejected() {
        assertThatThrownBy(() -> CustomPlugin.builder("MyPlugin")
            .withImportPath("\\\\server\\share\\malware.js")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Absolute paths");
    }

    @Test
    @DisplayName("URL in import path should be rejected")
    void urlInImportPathShouldBeRejected() {
        assertThatThrownBy(() -> CustomPlugin.builder("MyPlugin")
            .withImportPath("http://evil.com/malware.js")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URLs are not allowed");
    }

    @Test
    @DisplayName("Deep path traversal should be rejected")
    void deepPathTraversalShouldBeRejected() {
        assertThatThrownBy(() -> CustomPlugin.builder("MyPlugin")
            .withImportPath("../../../etc/passwd")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Deep path traversal");
    }

    @Test
    @DisplayName("Valid relative path should be accepted")
    void validRelativePathShouldBeAccepted() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
            .withImportPath("./my-local-plugin")
            .build();

        assertThat(plugin.getImportPath()).isEqualTo("./my-local-plugin");
    }

    @Test
    @DisplayName("Valid two-level relative path should be accepted")
    void validTwoLevelRelativePathShouldBeAccepted() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin")
            .withImportPath("../../shared/plugin")
            .build();

        assertThat(plugin.getImportPath()).isEqualTo("../../shared/plugin");
    }

    // ==================== Factory Method Comparison ====================

    @Test
    @DisplayName("Different factory methods should produce equivalent base plugins")
    void differentFactoryMethodsShouldProduceEquivalentPlugins() {
        CustomPlugin fromBuilder = CustomPlugin.builder("TestPlugin").build();
        CustomPlugin fromOf = CustomPlugin.of("TestPlugin", null);
        CustomPlugin fromCKEditor5 = CustomPlugin.fromCKEditor5("TestPlugin");

        assertThat(fromBuilder).isEqualTo(fromOf);
        assertThat(fromBuilder).isEqualTo(fromCKEditor5);
        assertThat(fromOf).isEqualTo(fromCKEditor5);
    }
}
