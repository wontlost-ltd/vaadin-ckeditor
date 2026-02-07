package com.wontlost.ckeditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for VaadinCKEditor.Builder dependency resolution.
 */
class VaadinCKEditorBuilderTest {

    private static final String[] INVALID_LANGUAGES = {
        "",
        "eng",
        "e",
        "en_US",
        "en-USA",
        "123",
        "en-"
    };

    @Test
    @DisplayName("AUTO_RESOLVE mode should automatically add missing dependencies")
    void autoResolveShouldAddMissingDependencies() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.IMAGE_CAPTION)
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE)
            .getResolvedPlugins();

        // Should include IMAGE_CAPTION and its dependency IMAGE
        assertThat(resolved).contains(
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE
        );
        // Should also include core plugins
        assertThat(resolved).contains(
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH
        );
    }

    @Test
    @DisplayName("AUTO_RESOLVE should be the default mode")
    void autoResolveShouldBeDefaultMode() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.TABLE_TOOLBAR)
            .getResolvedPlugins();

        // Should auto-resolve TABLE dependency
        assertThat(resolved).contains(
            CKEditorPlugin.TABLE_TOOLBAR,
            CKEditorPlugin.TABLE
        );
    }

    @Test
    @DisplayName("AUTO_RESOLVE_WITH_RECOMMENDED should include recommended plugins")
    void autoResolveWithRecommendedShouldIncludeRecommended() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.IMAGE)
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE_WITH_RECOMMENDED)
            .getResolvedPlugins();

        // Should include IMAGE and recommended plugins
        assertThat(resolved).contains(
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_TOOLBAR,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.IMAGE_STYLE,
            CKEditorPlugin.IMAGE_RESIZE
        );
    }

    @Test
    @DisplayName("STRICT mode should throw exception for missing dependencies")
    void strictModeShouldThrowForMissingDependencies() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.IMAGE_CAPTION) // Missing IMAGE
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.STRICT);

        assertThatThrownBy(builder::getResolvedPlugins)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Missing plugin dependencies")
            .hasMessageContaining("ImageCaption")
            .hasMessageContaining("Image");
    }

    @Test
    @DisplayName("STRICT mode should pass when all dependencies present")
    void strictModeShouldPassWhenDependenciesSatisfied() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.IMAGE, CKEditorPlugin.IMAGE_CAPTION)
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.STRICT)
            .getResolvedPlugins();

        assertThat(resolved).contains(
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.IMAGE_CAPTION,
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH
        );
    }

    @Test
    @DisplayName("MANUAL mode should not modify plugin list")
    void manualModeShouldNotModifyPlugins() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC)
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.MANUAL)
            .getResolvedPlugins();

        // Should only have exactly what was specified
        assertThat(resolved)
            .containsExactlyInAnyOrder(CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC);
        // Should NOT include core plugins in manual mode
        assertThat(resolved).doesNotContain(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH);
    }

    @Test
    @DisplayName("getMissingDependencies should report missing dependencies")
    void getMissingDependenciesShouldReportMissing() {
        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.IMAGE_CAPTION, CKEditorPlugin.TABLE_TOOLBAR)
            .getMissingDependencies();

        assertThat(missing).containsKey(CKEditorPlugin.IMAGE_CAPTION);
        assertThat(missing.get(CKEditorPlugin.IMAGE_CAPTION)).contains(CKEditorPlugin.IMAGE);

        assertThat(missing).containsKey(CKEditorPlugin.TABLE_TOOLBAR);
        assertThat(missing.get(CKEditorPlugin.TABLE_TOOLBAR)).contains(CKEditorPlugin.TABLE);
    }

    @Test
    @DisplayName("getMissingDependencies should return empty when satisfied")
    void getMissingDependenciesShouldReturnEmptyWhenSatisfied() {
        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.IMAGE, CKEditorPlugin.IMAGE_CAPTION)
            .getMissingDependencies();

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("Preset plugins should have dependencies resolved")
    void presetPluginsShouldHaveDependenciesResolved() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.STANDARD)
            .getResolvedPlugins();

        // Core plugins should always be present
        assertThat(resolved).contains(
            CKEditorPlugin.ESSENTIALS,
            CKEditorPlugin.PARAGRAPH
        );
    }

    @Test
    @DisplayName("Additional plugins should have dependencies resolved")
    void additionalPluginsShouldHaveDependenciesResolved() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .addPlugin(CKEditorPlugin.STYLE)
            .getResolvedPlugins();

        // STYLE depends on GENERAL_HTML_SUPPORT
        assertThat(resolved).contains(
            CKEditorPlugin.STYLE,
            CKEditorPlugin.GENERAL_HTML_SUPPORT
        );
    }

    @Test
    @DisplayName("Removed plugins should not appear in resolved list")
    void removedPluginsShouldNotAppear() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.STANDARD)
            .removePlugin(CKEditorPlugin.BOLD)
            .getResolvedPlugins();

        assertThat(resolved).doesNotContain(CKEditorPlugin.BOLD);
    }

    @Test
    @DisplayName("Complex dependency chain should be fully resolved")
    void complexDependencyChainShouldBeResolved() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(
                CKEditorPlugin.LINK_IMAGE,  // depends on IMAGE and LINK
                CKEditorPlugin.SIMPLE_UPLOAD_ADAPTER  // depends on IMAGE_UPLOAD -> IMAGE
            )
            .getResolvedPlugins();

        assertThat(resolved).contains(
            CKEditorPlugin.LINK_IMAGE,
            CKEditorPlugin.IMAGE,
            CKEditorPlugin.LINK,
            CKEditorPlugin.SIMPLE_UPLOAD_ADAPTER,
            CKEditorPlugin.IMAGE_UPLOAD
        );
    }

    @Test
    @DisplayName("DependencyMode enum should have all expected values")
    void dependencyModeEnumShouldHaveExpectedValues() {
        assertThat(VaadinCKEditorBuilder.DependencyMode.values())
            .containsExactlyInAnyOrder(
                VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE,
                VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE_WITH_RECOMMENDED,
                VaadinCKEditorBuilder.DependencyMode.STRICT,
                VaadinCKEditorBuilder.DependencyMode.MANUAL
            );
    }

    // ==================== License Key Tests ====================

    @Test
    @DisplayName("Builder should accept license key")
    void builderShouldAcceptLicenseKey() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withLicenseKey("test-license-key-12345")
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("Builder should use GPL as default license key")
    void builderShouldUseGplAsDefaultLicenseKey() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .build();

        assertThat(editor).isNotNull();
        // Default license key is GPL (tested via integration)
    }

    // ==================== Custom Plugin Tests ====================

    @Test
    @DisplayName("Builder should accept custom plugins")
    void builderShouldAcceptCustomPlugins() {
        CustomPlugin customPlugin = CustomPlugin.builder("MyPlugin")
            .withImportPath("my-plugin-package")
            .build();

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .addCustomPlugin(customPlugin)
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept premium custom plugins")
    void builderShouldAcceptPremiumCustomPlugins() {
        CustomPlugin premiumPlugin = CustomPlugin.fromPremium("ExportPdf");

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withLicenseKey("premium-license-key")
            .addCustomPlugin(premiumPlugin)
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept multiple custom plugins")
    void builderShouldAcceptMultipleCustomPlugins() {
        CustomPlugin plugin1 = CustomPlugin.fromPremium("ExportPdf");
        CustomPlugin plugin2 = CustomPlugin.fromPremium("ExportWord");
        CustomPlugin plugin3 = CustomPlugin.builder("CustomFeature")
            .withImportPath("custom-package")
            .withToolbarItems("customButton")
            .build();

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.STANDARD)
            .withLicenseKey("test-key")
            .addCustomPlugin(plugin1)
            .addCustomPlugin(plugin2)
            .addCustomPlugin(plugin3)
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Editor Type Tests ====================

    @Test
    @DisplayName("Builder should accept all editor types")
    void builderShouldAcceptAllEditorTypes() {
        for (CKEditorType type : CKEditorType.values()) {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(type)
                .build();

            assertThat(editor).isNotNull();
        }
    }

    // ==================== Theme Tests ====================

    @Test
    @DisplayName("Builder should accept all themes")
    void builderShouldAcceptAllThemes() {
        for (CKEditorTheme theme : CKEditorTheme.values()) {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withTheme(theme)
                .build();

            assertThat(editor).isNotNull();
        }
    }

    // ==================== Language Tests ====================

    @Test
    @DisplayName("Builder should accept language setting")
    void builderShouldAcceptLanguageSetting() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withLanguage("ja")
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept various language codes")
    void builderShouldAcceptVariousLanguageCodes() {
        String[] languages = {"en", "ja", "zh-cn", "zh", "ko", "de", "fr", "es", "pt", "ru", "ar", "pt-br"};

        for (String lang : languages) {
            VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withLanguage(lang)
                .build();

            assertThat(editor).isNotNull();
        }
    }

    @Test
    @DisplayName("Builder should reject invalid language codes")
    void builderShouldRejectInvalidLanguageCodes() {

        for (String lang : INVALID_LANGUAGES) {
            VaadinCKEditorBuilder builder = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC);

            assertThatThrownBy(() -> builder.withLanguage(lang))
                .as("Language '%s' should be rejected", lang)
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("Builder should reject null language code")
    void builderShouldRejectNullLanguageCode() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC);

        assertThatThrownBy(() -> builder.withLanguage(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null or empty");
    }

    // ==================== Initial Value Tests ====================

    @Test
    @DisplayName("Builder should accept initial value")
    void builderShouldAcceptInitialValue() {
        String initialContent = "<p>Hello World!</p>";

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withValue(initialContent)
            .build();

        assertThat(editor).isNotNull();
        assertThat(editor.getValue()).isEqualTo(initialContent);
    }

    @Test
    @DisplayName("Builder should accept empty initial value")
    void builderShouldAcceptEmptyInitialValue() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withValue("")
            .build();

        assertThat(editor).isNotNull();
        assertThat(editor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("Builder should accept null initial value")
    void builderShouldAcceptNullInitialValue() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withValue(null)
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Read-Only Tests ====================

    @Test
    @DisplayName("Builder should accept read-only setting")
    void builderShouldAcceptReadOnlySetting() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .readOnly(true)
            .build();

        assertThat(editor).isNotNull();
        assertThat(editor.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("withReadOnly should be alias for readOnly")
    void withReadOnlyShouldBeAliasForReadOnly() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withReadOnly(true)
            .build();

        assertThat(editor).isNotNull();
        assertThat(editor.isReadOnly()).isTrue();
    }

    @Test
    @DisplayName("withViewOnly should set read-only and fallback mode")
    void withViewOnlyShouldSetReadOnlyAndFallbackMode() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withViewOnly()
            .build();

        assertThat(editor).isNotNull();
        assertThat(editor.isReadOnly()).isTrue();
        assertThat(editor.getFallbackMode())
            .isEqualTo(com.wontlost.ckeditor.event.FallbackEvent.FallbackMode.READ_ONLY);
    }

    @Test
    @DisplayName("withHideToolbar should be configurable")
    void withHideToolbarShouldBeConfigurable() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withHideToolbar(true)
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Dimension Tests ====================

    @Test
    @DisplayName("Builder should accept width and height")
    void builderShouldAcceptWidthAndHeight() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withWidth("800px")
            .withHeight("400px")
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("Builder should accept percentage dimensions")
    void builderShouldAcceptPercentageDimensions() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withWidth("100%")
            .withHeight("50%")
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Toolbar Tests ====================

    @Test
    @DisplayName("Builder should accept custom toolbar")
    void builderShouldAcceptCustomToolbar() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.FULL)
            .withToolbar("bold", "italic", "|", "link", "insertImage")
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("Builder should reject null toolbar")
    void builderShouldRejectNullToolbar() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC);

        assertThatThrownBy(() -> builder.withToolbar((String[]) null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be null");
    }

    @Test
    @DisplayName("Builder should reject empty toolbar")
    void builderShouldRejectEmptyToolbar() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC);

        assertThatThrownBy(() -> builder.withToolbar())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    @DisplayName("Builder should reject toolbar with null item")
    void builderShouldRejectToolbarWithNullItem() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC);

        assertThatThrownBy(() -> builder.withToolbar("bold", null, "italic"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("item must not be null");
    }

    @Test
    @DisplayName("Builder should accept toolbar with separators")
    void builderShouldAcceptToolbarWithSeparators() {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withToolbar("bold", "|", "italic", "-", "undo")
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Config Tests ====================

    @Test
    @DisplayName("Builder should accept CKEditorConfig")
    void builderShouldAcceptConfig() {
        CKEditorConfig config = new CKEditorConfig()
            .setPlaceholder("Start typing...")
            .setFontSize("small", "default", "big");

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.FULL)
            .withConfig(config)
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Chaining Tests ====================

    @Test
    @DisplayName("Builder should support full method chaining")
    void builderShouldSupportFullMethodChaining() {
        CKEditorConfig config = new CKEditorConfig()
            .setPlaceholder("Enter content...");

        CustomPlugin premiumPlugin = CustomPlugin.fromPremium("ExportPdf");

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.STANDARD)
            .addPlugin(CKEditorPlugin.CODE_BLOCK)
            .addPlugins(CKEditorPlugin.HIGHLIGHT, CKEditorPlugin.HORIZONTAL_LINE)
            .removePlugin(CKEditorPlugin.MEDIA_EMBED)
            .addCustomPlugin(premiumPlugin)
            .withType(CKEditorType.CLASSIC)
            .withTheme(CKEditorTheme.AUTO)
            .withLanguage("en")
            .withValue("<p>Initial content</p>")
            .readOnly(false)
            .withWidth("100%")
            .withHeight("500px")
            .withToolbar("heading", "|", "bold", "italic")
            .withConfig(config)
            .withLicenseKey("test-license")
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.AUTO_RESOLVE)
            .build();

        assertThat(editor).isNotNull();
    }

    // ==================== Quick Create Tests ====================

    @Test
    @DisplayName("withPreset static method should create editor with preset")
    void withPresetStaticMethodShouldCreateEditor() {
        VaadinCKEditor editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("All presets should create valid editors")
    void allPresetsShouldCreateValidEditors() {
        for (CKEditorPreset preset : CKEditorPreset.values()) {
            VaadinCKEditor editor = VaadinCKEditor.withPreset(preset);
            assertThat(editor)
                .as("Editor with preset %s should not be null", preset.name())
                .isNotNull();
        }
    }

    // ==================== Plugin Deduplication Tests ====================

    @Test
    @DisplayName("Duplicate plugins should be deduplicated")
    void duplicatePluginsShouldBeDeduplicated() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.BOLD, CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC)
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.MANUAL)
            .getResolvedPlugins();

        // Should only have each plugin once
        long boldCount = resolved.stream()
            .filter(p -> p == CKEditorPlugin.BOLD)
            .count();

        assertThat(boldCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Adding same plugin multiple times should deduplicate")
    void addingSamePluginMultipleTimesShouldDeduplicate() {
        Set<CKEditorPlugin> resolved = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .addPlugin(CKEditorPlugin.BOLD)
            .addPlugin(CKEditorPlugin.BOLD)
            .addPlugin(CKEditorPlugin.BOLD)
            .getResolvedPlugins();

        long boldCount = resolved.stream()
            .filter(p -> p == CKEditorPlugin.BOLD)
            .count();

        assertThat(boldCount).isEqualTo(1);
    }

    // ==================== Upload Config Tests ====================

    @Test
    @DisplayName("withUploadConfig should configure upload settings")
    void withUploadConfigShouldConfigureUploadSettings() {
        com.wontlost.ckeditor.handler.UploadHandler.UploadConfig config =
            new com.wontlost.ckeditor.handler.UploadHandler.UploadConfig()
                .setMaxFileSize(5 * 1024 * 1024) // 5MB
                .setAllowedMimeTypes("image/jpeg", "image/png");

        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withUploadConfig(config)
            .build();

        assertThat(editor).isNotNull();
    }

    @Test
    @DisplayName("withUploadConfig should support fluent chaining")
    void withUploadConfigShouldSupportFluentChaining() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withUploadConfig(new com.wontlost.ckeditor.handler.UploadHandler.UploadConfig()
                .setMaxFileSize(10 * 1024 * 1024))
            .withValue("<p>Hello</p>");

        assertThat(builder).isInstanceOf(VaadinCKEditorBuilder.class);
    }

    // ==================== Autosave Boundary Tests ====================

    @Test
    @DisplayName("withAutosave should reject waitingTime below 100")
    void withAutosaveShouldRejectWaitingTimeBelow100() {
        assertThatThrownBy(() ->
            VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withAutosave(data -> {}, 99)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withAutosave should accept waitingTime at lower boundary 100")
    void withAutosaveShouldAcceptWaitingTimeAt100() {
        assertThatCode(() ->
            VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withAutosave(data -> {}, 100)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("withAutosave should accept waitingTime at upper boundary 60000")
    void withAutosaveShouldAcceptWaitingTimeAt60000() {
        assertThatCode(() ->
            VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withAutosave(data -> {}, 60000)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("withAutosave should reject waitingTime above 60000")
    void withAutosaveShouldRejectWaitingTimeAbove60000() {
        assertThatThrownBy(() ->
            VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withAutosave(data -> {}, 60001)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    // ==================== Builder Build-Once Guard ====================

    @Test
    @DisplayName("build should throw IllegalStateException on second call")
    void buildShouldThrowOnSecondCall() {
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC);
        builder.build();

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already been used");
    }
}
