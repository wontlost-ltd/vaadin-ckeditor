package com.wontlost.ckeditor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CKEditorConfig class.
 */
class CKEditorConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private CKEditorConfig config;

    @BeforeEach
    void setUp() {
        config = new CKEditorConfig();
    }

    @Test
    @DisplayName("New config should have default values")
    void newConfigShouldHaveDefaultValues() {
        ObjectNode json = config.toJson();

        assertThat(json.has("placeholder")).isTrue();
        assertThat(json.has("language")).isTrue();
        assertThat(json.get("language").asString()).isEqualTo("en");
    }

    @Test
    @DisplayName("setPlaceholder should update placeholder")
    void setPlaceholderShouldUpdatePlaceholder() {
        config.setPlaceholder("Enter text here...");
        ObjectNode json = config.toJson();

        assertThat(json.get("placeholder").asString()).isEqualTo("Enter text here...");
    }

    @Test
    @DisplayName("setLanguage should update language")
    void setLanguageShouldUpdateLanguage() {
        config.setLanguage("zh-cn");
        ObjectNode json = config.toJson();

        assertThat(json.get("language").asString()).isEqualTo("zh-cn");
    }

    @Test
    @DisplayName("setToolbar should create toolbar array")
    void setToolbarShouldCreateToolbarArray() {
        config.setToolbar("bold", "italic", "|", "link");
        ObjectNode json = config.toJson();

        assertThat(json.has("toolbar")).isTrue();
        assertThat(json.get("toolbar").isArray()).isTrue();
        assertThat(json.get("toolbar").size()).isEqualTo(4);
    }

    @Test
    @DisplayName("setFontSize should create fontSize config")
    void setFontSizeShouldCreateFontSizeConfig() {
        config.setFontSize("12px", "14px", "16px", "18px");
        ObjectNode json = config.toJson();

        assertThat(json.has("fontSize")).isTrue();
        assertThat(json.get("fontSize").has("options")).isTrue();
        assertThat(json.get("fontSize").get("options").size()).isEqualTo(4);
    }

    @Test
    @DisplayName("setFontSize with supportAllValues should set flag")
    void setFontSizeWithSupportAllValuesShouldSetFlag() {
        config.setFontSize(true, "12px", "14px");
        ObjectNode json = config.toJson();

        assertThat(json.get("fontSize").get("supportAllValues").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("setLink should create link config")
    void setLinkShouldCreateLinkConfig() {
        config.setLink("https://", true);
        ObjectNode json = config.toJson();

        assertThat(json.has("link")).isTrue();
        assertThat(json.get("link").get("defaultProtocol").asString()).isEqualTo("https://");
        assertThat(json.get("link").get("addTargetToExternalLinks").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("setImage should create image config")
    void setImageShouldCreateImageConfig() {
        String[] toolbar = {"imageStyle:inline", "imageStyle:block"};
        String[] styles = {"inline", "block"};

        config.setImage(toolbar, styles);
        ObjectNode json = config.toJson();

        assertThat(json.has("image")).isTrue();
        assertThat(json.get("image").get("toolbar").size()).isEqualTo(2);
        assertThat(json.get("image").get("styles").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("setTable should create table config")
    void setTableShouldCreateTableConfig() {
        config.setTable(new String[]{"tableColumn", "tableRow", "mergeTableCells"});
        ObjectNode json = config.toJson();

        assertThat(json.has("table")).isTrue();
        assertThat(json.get("table").get("contentToolbar").size()).isEqualTo(3);
    }

    @Test
    @DisplayName("setCodeBlock should create codeBlock config")
    void setCodeBlockShouldCreateCodeBlockConfig() {
        config.setCodeBlock("    ",
            CKEditorConfig.CodeBlockLanguage.of("java", "Java"),
            CKEditorConfig.CodeBlockLanguage.of("javascript", "JavaScript")
        );
        ObjectNode json = config.toJson();

        assertThat(json.has("codeBlock")).isTrue();
        assertThat(json.get("codeBlock").get("indentSequence").asString()).isEqualTo("    ");
        assertThat(json.get("codeBlock").get("languages").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("setMediaEmbed should create mediaEmbed config")
    void setMediaEmbedShouldCreateMediaEmbedConfig() {
        config.setMediaEmbed(true);
        ObjectNode json = config.toJson();

        assertThat(json.has("mediaEmbed")).isTrue();
        assertThat(json.get("mediaEmbed").get("previewsInData").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("setMention should create mention config with feeds")
    void setMentionShouldCreateMentionConfig() {
        config.setMention(
            CKEditorConfig.MentionFeed.users("@john", "@jane", "@bob")
        );
        ObjectNode json = config.toJson();

        assertThat(json.has("mention")).isTrue();
        assertThat(json.get("mention").has("feeds")).isTrue();
        assertThat(json.get("mention").get("feeds").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("setSimpleUpload should create simpleUpload config")
    void setSimpleUploadShouldCreateSimpleUploadConfig() {
        config.setSimpleUpload("https://example.com/upload",
            Map.of("Authorization", "Bearer token123"));
        ObjectNode json = config.toJson();

        assertThat(json.has("simpleUpload")).isTrue();
        assertThat(json.get("simpleUpload").get("uploadUrl").asString())
            .isEqualTo("https://example.com/upload");
        assertThat(json.get("simpleUpload").get("headers").get("Authorization").asString())
            .isEqualTo("Bearer token123");
    }

    @Test
    @DisplayName("setAutosave should create autosave config")
    void setAutosaveShouldCreateAutosaveConfig() {
        config.setAutosave(5000);
        ObjectNode json = config.toJson();

        assertThat(json.has("autosave")).isTrue();
        assertThat(json.get("autosave").get("waitingTime").asInt()).isEqualTo(5000);
    }

    @Test
    @DisplayName("setLicenseKey should set license key")
    void setLicenseKeyShouldSetLicenseKey() {
        config.setLicenseKey("my-license-key");
        ObjectNode json = config.toJson();

        assertThat(json.has("licenseKey")).isTrue();
        assertThat(json.get("licenseKey").asString()).isEqualTo("my-license-key");
    }

    @Test
    @DisplayName("setHtmlSupport should create htmlSupport config")
    void setHtmlSupportShouldCreateHtmlSupportConfig() {
        config.setHtmlSupport(true);
        ObjectNode json = config.toJson();

        assertThat(json.has("htmlSupport")).isTrue();
        assertThat(json.get("htmlSupport").has("allow")).isTrue();
    }

    @Test
    @DisplayName("setStyle should create style config with definitions")
    void setStyleShouldCreateStyleConfigWithDefinitions() {
        config.setStyle(
            CKEditorConfig.StyleDefinition.block("Info box", "p", "info-box"),
            CKEditorConfig.StyleDefinition.inline("Marker", "marker"),
            CKEditorConfig.StyleDefinition.codeBlock("Code dark", "fancy-code", "fancy-code-dark")
        );
        ObjectNode json = config.toJson();

        assertThat(json.has("style")).isTrue();
        assertThat(json.get("style").has("definitions")).isTrue();
        assertThat(json.get("style").get("definitions").size()).isEqualTo(3);

        // Verify block style
        assertThat(json.get("style").get("definitions").get(0).get("name").asString())
            .isEqualTo("Info box");
        assertThat(json.get("style").get("definitions").get(0).get("element").asString())
            .isEqualTo("p");
        assertThat(json.get("style").get("definitions").get(0).get("classes").get(0).asString())
            .isEqualTo("info-box");

        // Verify inline style uses span element
        assertThat(json.get("style").get("definitions").get(1).get("element").asString())
            .isEqualTo("span");

        // Verify code block style uses pre element
        assertThat(json.get("style").get("definitions").get(2).get("element").asString())
            .isEqualTo("pre");
    }

    @Test
    @DisplayName("StyleDefinition should support multiple CSS classes")
    void styleDefinitionShouldSupportMultipleCssClasses() {
        config.setStyle(
            CKEditorConfig.StyleDefinition.block("Multi-class style", "div", "class1", "class2", "class3")
        );
        ObjectNode json = config.toJson();

        assertThat(json.get("style").get("definitions").get(0).get("classes").size())
            .isEqualTo(3);
        assertThat(json.get("style").get("definitions").get(0).get("classes").get(0).asString())
            .isEqualTo("class1");
        assertThat(json.get("style").get("definitions").get(0).get("classes").get(1).asString())
            .isEqualTo("class2");
        assertThat(json.get("style").get("definitions").get(0).get("classes").get(2).asString())
            .isEqualTo("class3");
    }

    @Test
    @DisplayName("StyleDefinition getters should return correct values")
    void styleDefinitionGettersShouldReturnCorrectValues() {
        CKEditorConfig.StyleDefinition def = CKEditorConfig.StyleDefinition.block(
            "Test Style", "h2", "heading-style", "custom-class"
        );

        assertThat(def.getName()).isEqualTo("Test Style");
        assertThat(def.getElement()).isEqualTo("h2");
        assertThat(def.getClasses()).containsExactly("heading-style", "custom-class");
    }

    @Test
    @DisplayName("setHeading should create heading config with options")
    void setHeadingShouldCreateHeadingConfig() {
        config.setHeading(
            CKEditorConfig.HeadingOption.paragraph("Paragraph", "ck-heading_paragraph"),
            CKEditorConfig.HeadingOption.heading(1, "Heading 1", "ck-heading_heading1"),
            CKEditorConfig.HeadingOption.heading(2, "Heading 2", "ck-heading_heading2")
        );
        ObjectNode json = config.toJson();

        assertThat(json.has("heading")).isTrue();
        assertThat(json.get("heading").get("options").size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Fluent API should allow method chaining")
    void fluentApiShouldAllowMethodChaining() {
        CKEditorConfig result = config
            .setPlaceholder("Type here...")
            .setLanguage("en")
            .setToolbar("bold", "italic")
            .setFontSize("12px", "14px")
            .setLink("https://", true);

        assertThat(result).isSameAs(config);
    }

    @Test
    @DisplayName("getConfigs should return unmodifiable map")
    void getConfigsShouldReturnUnmodifiableMap() {
        config.setPlaceholder("Test");

        assertThatThrownBy(() -> config.getConfigs().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // ==================== Custom Configuration Tests ====================

    @Test
    @DisplayName("set method should add arbitrary config for premium features")
    void setMethodShouldAddArbitraryConfigForPremiumFeatures() {
        ObjectNode exportPdfConfig = MAPPER.createObjectNode();
        exportPdfConfig.put("fileName", "document.pdf");
        ObjectNode converterOptions = MAPPER.createObjectNode();
        converterOptions.put("format", "A4");
        converterOptions.put("margin_top", "20mm");
        exportPdfConfig.set("converterOptions", converterOptions);

        config.set("exportPdf", exportPdfConfig);
        ObjectNode json = config.toJson();

        assertThat(json.has("exportPdf")).isTrue();
        assertThat(json.get("exportPdf").get("fileName").asString()).isEqualTo("document.pdf");
    }

    @Test
    @DisplayName("set method should support cloud services config")
    void setMethodShouldSupportCloudServicesConfig() {
        ObjectNode cloudServices = MAPPER.createObjectNode();
        cloudServices.put("tokenUrl", "https://example.com/token");
        cloudServices.put("webSocketUrl", "wss://example.com/ws");

        config.set("cloudServices", cloudServices);
        ObjectNode json = config.toJson();

        assertThat(json.has("cloudServices")).isTrue();
        assertThat(json.get("cloudServices").get("tokenUrl").asString())
            .isEqualTo("https://example.com/token");
        assertThat(json.get("cloudServices").get("webSocketUrl").asString())
            .isEqualTo("wss://example.com/ws");
    }

    @Test
    @DisplayName("set method should support AI configuration")
    void setMethodShouldSupportAIConfiguration() {
        ObjectNode aiConfig = MAPPER.createObjectNode();
        ObjectNode openAI = MAPPER.createObjectNode();
        openAI.put("apiUrl", "https://api.openai.com/v1");
        aiConfig.set("openAI", openAI);

        config.set("ai", aiConfig);
        ObjectNode json = config.toJson();

        assertThat(json.has("ai")).isTrue();
        assertThat(json.get("ai").get("openAI").get("apiUrl").asString())
            .isEqualTo("https://api.openai.com/v1");
    }

    @Test
    @DisplayName("set method should support collaboration config")
    void setMethodShouldSupportCollaborationConfig() {
        ObjectNode collaboration = MAPPER.createObjectNode();
        collaboration.put("channelId", "document-123");

        config.set("collaboration", collaboration);
        ObjectNode json = config.toJson();

        assertThat(json.has("collaboration")).isTrue();
        assertThat(json.get("collaboration").get("channelId").asString())
            .isEqualTo("document-123");
    }

    @Test
    @DisplayName("Multiple set calls should accumulate")
    void multipleSetCallsShouldAccumulate() {
        ObjectNode feature1 = MAPPER.createObjectNode();
        feature1.put("key1", "value1");
        ObjectNode feature2 = MAPPER.createObjectNode();
        feature2.put("key2", "value2");

        config.set("feature1", feature1);
        config.set("feature2", feature2);
        ObjectNode json = config.toJson();

        assertThat(json.has("feature1")).isTrue();
        assertThat(json.has("feature2")).isTrue();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Empty toolbar should not create toolbar entry")
    void emptyToolbarShouldNotCreateToolbarEntry() {
        // Empty toolbar is treated as no-op to avoid clearing existing config
        CKEditorConfig freshConfig = new CKEditorConfig();
        freshConfig.setToolbar();
        ObjectNode json = freshConfig.toJson();

        // Empty toolbar array is not added (per implementation)
        assertThat(json.has("toolbar")).isFalse();
    }

    @Test
    @DisplayName("Null placeholder should be converted to empty string")
    void nullPlaceholderShouldBeConvertedToEmptyString() {
        config.setPlaceholder(null);
        ObjectNode json = config.toJson();

        // Null is converted to empty string per implementation
        assertThat(json.get("placeholder").asString()).isEmpty();
    }

    @Test
    @DisplayName("Very long license key should be stored correctly")
    void veryLongLicenseKeyShouldBeStoredCorrectly() {
        String longKey = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0";
        config.setLicenseKey(longKey);
        ObjectNode json = config.toJson();

        assertThat(json.get("licenseKey").asString()).isEqualTo(longKey);
    }

    @Test
    @DisplayName("GPL license key constant should work")
    void gplLicenseKeyConstantShouldWork() {
        config.setLicenseKey("GPL");
        ObjectNode json = config.toJson();

        assertThat(json.get("licenseKey").asString()).isEqualTo("GPL");
    }

    // ==================== Toolbar Style Tests ====================

    @Test
    @DisplayName("setToolbarStyle should create toolbarStyle config")
    void setToolbarStyleShouldCreateToolbarStyleConfig() {
        config.setToolbarStyle(CKEditorConfig.ToolbarStyle.builder()
            .background("#f5f5f5")
            .borderColor("#ddd")
            .borderRadius("8px")
            .build());
        ObjectNode json = config.toJson();

        assertThat(json.has("toolbarStyle")).isTrue();
        assertThat(json.get("toolbarStyle").get("background").asString()).isEqualTo("#f5f5f5");
        assertThat(json.get("toolbarStyle").get("borderColor").asString()).isEqualTo("#ddd");
        assertThat(json.get("toolbarStyle").get("borderRadius").asString()).isEqualTo("8px");
    }

    @Test
    @DisplayName("ToolbarStyle should support all button state styles")
    void toolbarStyleShouldSupportAllButtonStateStyles() {
        config.setToolbarStyle(CKEditorConfig.ToolbarStyle.builder()
            .buttonBackground("#ffffff")
            .buttonHoverBackground("#e0e0e0")
            .buttonActiveBackground("#d0d0d0")
            .buttonOnBackground("#1976d2")
            .buttonOnColor("#ffffff")
            .iconColor("#333333")
            .build());
        ObjectNode json = config.toJson();

        ObjectNode toolbarStyle = (ObjectNode) json.get("toolbarStyle");
        assertThat(toolbarStyle.get("buttonBackground").asString()).isEqualTo("#ffffff");
        assertThat(toolbarStyle.get("buttonHoverBackground").asString()).isEqualTo("#e0e0e0");
        assertThat(toolbarStyle.get("buttonActiveBackground").asString()).isEqualTo("#d0d0d0");
        assertThat(toolbarStyle.get("buttonOnBackground").asString()).isEqualTo("#1976d2");
        assertThat(toolbarStyle.get("buttonOnColor").asString()).isEqualTo("#ffffff");
        assertThat(toolbarStyle.get("iconColor").asString()).isEqualTo("#333333");
    }

    @Test
    @DisplayName("ToolbarStyle should support individual button styles")
    void toolbarStyleShouldSupportIndividualButtonStyles() {
        config.setToolbarStyle(CKEditorConfig.ToolbarStyle.builder()
            .background("#f5f5f5")
            .buttonStyle("Bold", CKEditorConfig.ButtonStyle.builder()
                .background("#fff3e0")
                .hoverBackground("#ffe0b2")
                .activeBackground("#ffcc80")
                .iconColor("#e65100")
                .build())
            .buttonStyle("Italic", CKEditorConfig.ButtonStyle.builder()
                .background("#e3f2fd")
                .iconColor("#1565c0")
                .build())
            .build());
        ObjectNode json = config.toJson();

        ObjectNode toolbarStyle = (ObjectNode) json.get("toolbarStyle");
        assertThat(toolbarStyle.has("buttonStyles")).isTrue();

        ObjectNode buttonStyles = (ObjectNode) toolbarStyle.get("buttonStyles");
        assertThat(buttonStyles.has("Bold")).isTrue();
        assertThat(buttonStyles.has("Italic")).isTrue();

        ObjectNode boldStyle = (ObjectNode) buttonStyles.get("Bold");
        assertThat(boldStyle.get("background").asString()).isEqualTo("#fff3e0");
        assertThat(boldStyle.get("hoverBackground").asString()).isEqualTo("#ffe0b2");
        assertThat(boldStyle.get("activeBackground").asString()).isEqualTo("#ffcc80");
        assertThat(boldStyle.get("iconColor").asString()).isEqualTo("#e65100");

        ObjectNode italicStyle = (ObjectNode) buttonStyles.get("Italic");
        assertThat(italicStyle.get("background").asString()).isEqualTo("#e3f2fd");
        assertThat(italicStyle.get("iconColor").asString()).isEqualTo("#1565c0");
    }

    @Test
    @DisplayName("ToolbarStyle builder should be fluent")
    void toolbarStyleBuilderShouldBeFluent() {
        CKEditorConfig.ToolbarStyle style = CKEditorConfig.ToolbarStyle.builder()
            .background("#fff")
            .borderColor("#ccc")
            .borderRadius("4px")
            .buttonBackground("#eee")
            .buttonHoverBackground("#ddd")
            .buttonActiveBackground("#ccc")
            .buttonOnBackground("#007bff")
            .buttonOnColor("#fff")
            .iconColor("#333")
            .build();

        assertThat(style.getBackground()).isEqualTo("#fff");
        assertThat(style.getBorderColor()).isEqualTo("#ccc");
        assertThat(style.getBorderRadius()).isEqualTo("4px");
        assertThat(style.getButtonBackground()).isEqualTo("#eee");
        assertThat(style.getButtonHoverBackground()).isEqualTo("#ddd");
        assertThat(style.getButtonActiveBackground()).isEqualTo("#ccc");
        assertThat(style.getButtonOnBackground()).isEqualTo("#007bff");
        assertThat(style.getButtonOnColor()).isEqualTo("#fff");
        assertThat(style.getIconColor()).isEqualTo("#333");
    }

    @Test
    @DisplayName("ButtonStyle builder should be fluent")
    void buttonStyleBuilderShouldBeFluent() {
        CKEditorConfig.ButtonStyle style = CKEditorConfig.ButtonStyle.builder()
            .background("#fff3e0")
            .hoverBackground("#ffe0b2")
            .activeBackground("#ffcc80")
            .iconColor("#e65100")
            .build();

        assertThat(style.getBackground()).isEqualTo("#fff3e0");
        assertThat(style.getHoverBackground()).isEqualTo("#ffe0b2");
        assertThat(style.getActiveBackground()).isEqualTo("#ffcc80");
        assertThat(style.getIconColor()).isEqualTo("#e65100");
    }

    @Test
    @DisplayName("ToolbarStyle toJson should omit null values")
    void toolbarStyleToJsonShouldOmitNullValues() {
        config.setToolbarStyle(CKEditorConfig.ToolbarStyle.builder()
            .background("#f5f5f5")
            // Other values are null
            .build());
        ObjectNode json = config.toJson();

        ObjectNode toolbarStyle = (ObjectNode) json.get("toolbarStyle");
        assertThat(toolbarStyle.has("background")).isTrue();
        assertThat(toolbarStyle.has("borderColor")).isFalse();
        assertThat(toolbarStyle.has("borderRadius")).isFalse();
        assertThat(toolbarStyle.has("buttonStyles")).isFalse();
    }

    @Test
    @DisplayName("setToolbarStyle with null should not add config")
    void setToolbarStyleWithNullShouldNotAddConfig() {
        config.setToolbarStyle(null);
        ObjectNode json = config.toJson();

        assertThat(json.has("toolbarStyle")).isFalse();
    }

    @Test
    @DisplayName("ToolbarStyle should support buttonStyles map")
    void toolbarStyleShouldSupportButtonStylesMap() {
        Map<String, CKEditorConfig.ButtonStyle> buttonStyles = new java.util.LinkedHashMap<>();
        buttonStyles.put("Bold", CKEditorConfig.ButtonStyle.builder()
            .background("#fff3e0")
            .build());
        buttonStyles.put("Italic", CKEditorConfig.ButtonStyle.builder()
            .background("#e3f2fd")
            .build());

        config.setToolbarStyle(CKEditorConfig.ToolbarStyle.builder()
            .buttonStyles(buttonStyles)
            .build());
        ObjectNode json = config.toJson();

        ObjectNode toolbarStyle = (ObjectNode) json.get("toolbarStyle");
        ObjectNode styles = (ObjectNode) toolbarStyle.get("buttonStyles");
        assertThat(styles.has("Bold")).isTrue();
        assertThat(styles.has("Italic")).isTrue();
    }

    // ==================== CustomPlugin ImportPath Validation Tests ====================

    @Test
    @DisplayName("CustomPlugin should accept valid npm package names")
    void customPluginShouldAcceptValidNpmPackageNames() {
        // Regular package name
        CustomPlugin plugin1 = CustomPlugin.builder("MyPlugin")
            .withImportPath("my-ckeditor-plugin")
            .build();
        assertThat(plugin1.getImportPath()).isEqualTo("my-ckeditor-plugin");

        // Scoped package
        CustomPlugin plugin2 = CustomPlugin.builder("MyPlugin")
            .withImportPath("@scope/my-plugin")
            .build();
        assertThat(plugin2.getImportPath()).isEqualTo("@scope/my-plugin");

        // Relative path
        CustomPlugin plugin3 = CustomPlugin.builder("MyPlugin")
            .withImportPath("./local-plugin")
            .build();
        assertThat(plugin3.getImportPath()).isEqualTo("./local-plugin");

        // npm subpath import
        CustomPlugin plugin4 = CustomPlugin.builder("MyPlugin")
            .withImportPath("lodash/merge")
            .build();
        assertThat(plugin4.getImportPath()).isEqualTo("lodash/merge");

        // Scoped package subpath
        CustomPlugin plugin5 = CustomPlugin.builder("MyPlugin")
            .withImportPath("@scope/package/subpath")
            .build();
        assertThat(plugin5.getImportPath()).isEqualTo("@scope/package/subpath");

        // Relative path with extension
        CustomPlugin plugin6 = CustomPlugin.builder("MyPlugin")
            .withImportPath("./plugin.js")
            .build();
        assertThat(plugin6.getImportPath()).isEqualTo("./plugin.js");

        // Two-level parent relative path (allowed)
        CustomPlugin plugin7 = CustomPlugin.builder("MyPlugin")
            .withImportPath("../../shared/plugin")
            .build();
        assertThat(plugin7.getImportPath()).isEqualTo("../../shared/plugin");
    }

    @Test
    @DisplayName("CustomPlugin should reject absolute paths")
    void customPluginShouldRejectAbsolutePaths() {
        assertThatThrownBy(() ->
            CustomPlugin.builder("MyPlugin")
                .withImportPath("/etc/passwd")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Absolute paths are not allowed");
    }

    @Test
    @DisplayName("CustomPlugin should reject URLs")
    void customPluginShouldRejectUrls() {
        assertThatThrownBy(() ->
            CustomPlugin.builder("MyPlugin")
                .withImportPath("http://evil.com/malware.js")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("URLs are not allowed");
    }

    @Test
    @DisplayName("CustomPlugin should reject deep path traversal")
    void customPluginShouldRejectDeepPathTraversal() {
        assertThatThrownBy(() ->
            CustomPlugin.builder("MyPlugin")
                .withImportPath("../../../etc/passwd")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not allowed");
    }

    @Test
    @DisplayName("CustomPlugin should allow null importPath")
    void customPluginShouldAllowNullImportPath() {
        CustomPlugin plugin = CustomPlugin.builder("MyPlugin").build();
        assertThat(plugin.getImportPath()).isNull();
    }

    // ==================== SSRF Protection Tests ====================

    @Test
    @DisplayName("setSimpleUpload should reject localhost")
    void setSimpleUploadShouldRejectLocalhost() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://localhost/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject 127.0.0.1")
    void setSimpleUploadShouldReject127001() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://127.0.0.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject 192.168.x.x addresses")
    void setSimpleUploadShouldReject192168() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://192.168.1.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject 10.x.x.x addresses")
    void setSimpleUploadShouldReject10() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://10.0.0.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject 172.16-31.x.x addresses")
    void setSimpleUploadShouldReject172() {
        // 172.16.x.x (minimum)
        assertThatThrownBy(() -> config.setSimpleUpload("http://172.16.0.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // 172.20.x.x (middle)
        assertThatThrownBy(() -> config.setSimpleUpload("http://172.20.1.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // 172.31.x.x (maximum)
        assertThatThrownBy(() -> config.setSimpleUpload("http://172.31.255.255/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should allow 172.15.x.x and 172.32.x.x (not private)")
    void setSimpleUploadShouldAllow17215And17232() {
        // 172.15.x.x is not a private address
        config.setSimpleUpload("http://172.15.1.1/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://172.15.1.1/upload");

        // 172.32.x.x is not a private address
        config.setSimpleUpload("http://172.32.1.1/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://172.32.1.1/upload");
    }

    @Test
    @DisplayName("setSimpleUpload should reject .local domains")
    void setSimpleUploadShouldRejectLocalDomains() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://myserver.local/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject .internal domains")
    void setSimpleUploadShouldRejectInternalDomains() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://api.internal/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject file:// protocol")
    void setSimpleUploadShouldRejectFileProtocol() {
        assertThatThrownBy(() -> config.setSimpleUpload("file:///etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("protocol");
    }

    @Test
    @DisplayName("setSimpleUpload should reject ftp:// protocol")
    void setSimpleUploadShouldRejectFtpProtocol() {
        assertThatThrownBy(() -> config.setSimpleUpload("ftp://example.com/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("protocol");
    }

    @Test
    @DisplayName("setSimpleUpload should allow valid public URLs")
    void setSimpleUploadShouldAllowValidPublicUrls() {
        // HTTP
        config.setSimpleUpload("http://example.com/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://example.com/upload");

        // HTTPS
        config.setSimpleUpload("https://api.example.com/v1/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("https://api.example.com/v1/upload");

        // With port
        config.setSimpleUpload("https://example.com:8443/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("https://example.com:8443/upload");

        // With query params
        config.setSimpleUpload("https://example.com/upload?token=abc");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("https://example.com/upload?token=abc");
    }

    // ==================== allowPrivateNetworks Tests ====================

    @Test
    @DisplayName("allowPrivateNetworks should default to false")
    void allowPrivateNetworksShouldDefaultToFalse() {
        assertThat(config.isAllowPrivateNetworks()).isFalse();
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow localhost when enabled")
    void allowPrivateNetworksShouldAllowLocalhostWhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://localhost/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://localhost/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow 127.0.0.1 when enabled")
    void allowPrivateNetworksShouldAllow127001WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://127.0.0.1:8080/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://127.0.0.1:8080/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow 192.168.x.x when enabled")
    void allowPrivateNetworksShouldAllow192168WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://192.168.1.100/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://192.168.1.100/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow 10.x.x.x when enabled")
    void allowPrivateNetworksShouldAllow10WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://10.0.0.50/api/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://10.0.0.50/api/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow 172.16-31.x.x when enabled")
    void allowPrivateNetworksShouldAllow172WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://172.20.10.5/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://172.20.10.5/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow .local domains when enabled")
    void allowPrivateNetworksShouldAllowLocalDomainsWhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://myserver.local/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://myserver.local/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow .internal domains when enabled")
    void allowPrivateNetworksShouldAllowInternalDomainsWhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://api.internal/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://api.internal/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should support fluent chaining")
    void allowPrivateNetworksShouldSupportFluentChaining() {
        CKEditorConfig result = config
            .allowPrivateNetworks(true)
            .setSimpleUpload("http://localhost/upload")
            .setPlaceholder("Type here...");

        assertThat(result).isSameAs(config);
        assertThat(config.isAllowPrivateNetworks()).isTrue();
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://localhost/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks can be toggled")
    void allowPrivateNetworksCanBeToggled() {
        // Enable
        config.allowPrivateNetworks(true);
        assertThat(config.isAllowPrivateNetworks()).isTrue();

        // Disable
        config.allowPrivateNetworks(false);
        assertThat(config.isAllowPrivateNetworks()).isFalse();

        // After disabling, should reject private addresses
        assertThatThrownBy(() -> config.setSimpleUpload("http://localhost/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("allowPrivateNetworks error message should guide developer")
    void allowPrivateNetworksErrorMessageShouldGuideDeveloper() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://localhost/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("allowPrivateNetworks(true)");
    }

    // ==================== IPv6 SSRF Bypass Prevention Tests ====================

    @Test
    @DisplayName("setSimpleUpload should reject IPv6 localhost ::1")
    void setSimpleUploadShouldRejectIPv6Localhost() {
        // IPv6 localhost without brackets (URI parsing may fail, but should be rejected)
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject IPv4-mapped IPv6 addresses")
    void setSimpleUploadShouldRejectIPv4MappedIPv6() {
        // ::ffff:127.0.0.1 is IPv6 mapped address for 127.0.0.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::ffff:127.0.0.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::ffff:192.168.1.1 is IPv6 mapped address for 192.168.1.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::ffff:192.168.1.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::ffff:10.0.0.1 is IPv6 mapped address for 10.0.0.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::ffff:10.0.0.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject 0.0.0.0 wildcard address")
    void setSimpleUploadShouldRejectWildcardAddress() {
        assertThatThrownBy(() -> config.setSimpleUpload("http://0.0.0.0/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject octal IP representation")
    void setSimpleUploadShouldRejectOctalIP() {
        // 0177.0.0.1 = 127.0.0.1 (octal representation)
        // Java URI will parse this address, then detected by isObfuscatedPrivateIPv4 function
        assertThatThrownBy(() -> config.setSimpleUpload("http://0177.0.0.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject single-segment octal IP")
    void setSimpleUploadShouldRejectSingleSegmentOctalIP() {
        // Single-segment octal IP 0177 = 127 (decimal)
        // Java URI parser can handle single-segment IP, our code will detect and reject as private address
        assertThatThrownBy(() -> config.setSimpleUpload("http://0177/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // Two-segment octal IP: 0177.1 - Java URI parser returns null host
        assertThatThrownBy(() -> config.setSimpleUpload("http://0177.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valid host");
    }

    @Test
    @DisplayName("setSimpleUpload should reject hexadecimal IP representation")
    void setSimpleUploadShouldRejectHexIP() {
        // 0x7f.0.0.1 = 127.0.0.1 (hexadecimal representation)
        // Note: Java URI parser does not support hexadecimal IP, returns null host,
        // thus throws "must have a valid host" error instead of "internal/private"
        assertThatThrownBy(() -> config.setSimpleUpload("http://0x7f.0.0.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valid host");
    }

    @Test
    @DisplayName("setSimpleUpload should reject IPv6 link-local addresses")
    void setSimpleUploadShouldRejectIPv6LinkLocal() {
        // fe80:: is IPv6 link-local address
        assertThatThrownBy(() -> config.setSimpleUpload("http://[fe80::1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject IPv6 unique local addresses (ULA)")
    void setSimpleUploadShouldRejectIPv6ULA() {
        // fc00::/7 is IPv6 unique local address
        assertThatThrownBy(() -> config.setSimpleUpload("http://[fc00::1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        assertThatThrownBy(() -> config.setSimpleUpload("http://[fd00::1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow IPv6 localhost when enabled")
    void allowPrivateNetworksShouldAllowIPv6LocalhostWhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://[::1]/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://[::1]/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow IPv4-mapped IPv6 when enabled")
    void allowPrivateNetworksShouldAllowIPv4MappedIPv6WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://[::ffff:127.0.0.1]/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://[::ffff:127.0.0.1]/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow 0.0.0.0 when enabled")
    void allowPrivateNetworksShouldAllowWildcardWhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://0.0.0.0/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://0.0.0.0/upload");
    }

    @Test
    @DisplayName("setSimpleUpload should reject 169.254.x.x link-local addresses")
    void setSimpleUploadShouldRejectLinkLocal169254() {
        // 169.254.x.x is IPv4 link-local address
        assertThatThrownBy(() -> config.setSimpleUpload("http://169.254.1.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    // ==================== IPv4-Compatible IPv6 SSRF Bypass Prevention Tests ====================

    @Test
    @DisplayName("setSimpleUpload should reject IPv4-compatible IPv6 addresses (::x.x.x.x)")
    void setSimpleUploadShouldRejectIPv4CompatibleIPv6() {
        // ::127.0.0.1 is IPv4-compatible IPv6 address (deprecated but still needs protection)
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::127.0.0.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::192.168.1.1 is IPv4-compatible format for 192.168.1.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::192.168.1.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::10.0.0.1 is IPv4-compatible format for 10.0.0.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::10.0.0.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::0.0.0.0 is IPv4-compatible format for 0.0.0.0
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::0.0.0.0]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("setSimpleUpload should reject SIIT format IPv6 addresses (::ffff:0:x.x.x.x)")
    void setSimpleUploadShouldRejectSIITIPv6() {
        // ::ffff:0:127.0.0.1 is SIIT format IPv6 address
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::ffff:0:127.0.0.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::ffff:0:192.168.1.1 is SIIT format for 192.168.1.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::ffff:0:192.168.1.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");

        // ::ffff:0:10.0.0.1 is SIIT format for 10.0.0.1
        assertThatThrownBy(() -> config.setSimpleUpload("http://[::ffff:0:10.0.0.1]/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("internal/private");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow IPv4-compatible IPv6 when enabled")
    void allowPrivateNetworksShouldAllowIPv4CompatibleIPv6WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://[::127.0.0.1]/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://[::127.0.0.1]/upload");
    }

    @Test
    @DisplayName("allowPrivateNetworks should allow SIIT format IPv6 when enabled")
    void allowPrivateNetworksShouldAllowSIITIPv6WhenEnabled() {
        config.allowPrivateNetworks(true);
        config.setSimpleUpload("http://[::ffff:0:127.0.0.1]/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://[::ffff:0:127.0.0.1]/upload");
    }

    @Test
    @DisplayName("setSimpleUpload should allow public IPv4-compatible IPv6 addresses")
    void setSimpleUploadShouldAllowPublicIPv4CompatibleIPv6() {
        // ::8.8.8.8 is IPv4-compatible format for public address, should be allowed
        config.setSimpleUpload("http://[::8.8.8.8]/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://[::8.8.8.8]/upload");
    }

    // ==================== Decimal Integer IP Format Tests ====================

    @Test
    @DisplayName("setSimpleUpload should allow decimal integer IP representation")
    void setSimpleUploadShouldAllowDecimalIntegerIP() {
        // 2130706433 is decimal integer representation of 127.0.0.1
        // Java URI parser treats it as a normal domain name (host is not null)
        // Since it looks like a domain name rather than IP address, SSRF check won't block
        // Note: Browser support for this format is inconsistent
        config.setSimpleUpload("http://2130706433/upload");
        assertThat(config.getSimpleUploadUrl()).isEqualTo("http://2130706433/upload");

        // 3232235777 is decimal integer representation of 192.168.1.1
        CKEditorConfig config2 = new CKEditorConfig();
        config2.setSimpleUpload("http://3232235777/upload");
        assertThat(config2.getSimpleUploadUrl()).isEqualTo("http://3232235777/upload");
    }

    @Test
    @DisplayName("setSimpleUpload should reject mixed decimal notation")
    void setSimpleUploadShouldRejectMixedDecimalNotation() {
        // 127.1 is shorthand for 127.0.0.1
        // Java URI parser returns null host, thus rejected
        assertThatThrownBy(() -> config.setSimpleUpload("http://127.1/upload"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valid host");
    }

    // ==================== CSS Injection Prevention Tests ====================

    @Test
    @DisplayName("ToolbarStyle should reject url() in CSS values")
    void toolbarStyleShouldRejectUrlInCssValues() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .background("url('http://evil.com/track.png')")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ToolbarStyle should reject expression() in CSS values")
    void toolbarStyleShouldRejectExpressionInCssValues() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .background("expression(alert('xss'))")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ToolbarStyle should reject javascript: in CSS values")
    void toolbarStyleShouldRejectJavascriptInCssValues() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .iconColor("javascript:alert('xss')")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ToolbarStyle should reject semicolons in CSS values")
    void toolbarStyleShouldRejectSemicolonsInCssValues() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .background("#fff; background-image: url('evil.png')")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ToolbarStyle should reject curly braces in CSS values")
    void toolbarStyleShouldRejectCurlyBracesInCssValues() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .background("#fff} .evil { background: red")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ToolbarStyle should accept valid hex colors")
    void toolbarStyleShouldAcceptValidHexColors() {
        CKEditorConfig.ToolbarStyle style = CKEditorConfig.ToolbarStyle.builder()
            .background("#fff")
            .borderColor("#ffffff")
            .buttonOnColor("#1976d2")
            .iconColor("#424242")
            .build();

        assertThat(style.getBackground()).isEqualTo("#fff");
        assertThat(style.getBorderColor()).isEqualTo("#ffffff");
    }

    @Test
    @DisplayName("ToolbarStyle should accept valid rgba colors")
    void toolbarStyleShouldAcceptValidRgbaColors() {
        CKEditorConfig.ToolbarStyle style = CKEditorConfig.ToolbarStyle.builder()
            .buttonHoverBackground("rgba(0, 0, 0, 0.05)")
            .buttonActiveBackground("rgba(255, 255, 255, 0.1)")
            .build();

        assertThat(style.getButtonHoverBackground()).isEqualTo("rgba(0, 0, 0, 0.05)");
    }

    @Test
    @DisplayName("ToolbarStyle should accept valid size values")
    void toolbarStyleShouldAcceptValidSizeValues() {
        CKEditorConfig.ToolbarStyle style = CKEditorConfig.ToolbarStyle.builder()
            .borderRadius("4px")
            .build();

        assertThat(style.getBorderRadius()).isEqualTo("4px");
    }

    @Test
    @DisplayName("ToolbarStyle should accept transparent and inherit keywords")
    void toolbarStyleShouldAcceptKeywords() {
        CKEditorConfig.ToolbarStyle style = CKEditorConfig.ToolbarStyle.builder()
            .buttonBackground("transparent")
            .iconColor("inherit")
            .build();

        assertThat(style.getButtonBackground()).isEqualTo("transparent");
        assertThat(style.getIconColor()).isEqualTo("inherit");
    }

    @Test
    @DisplayName("ButtonStyle should reject url() in CSS values")
    void buttonStyleShouldRejectUrlInCssValues() {
        assertThatThrownBy(() ->
            CKEditorConfig.ButtonStyle.builder()
                .background("url('http://evil.com/image.png')")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ButtonStyle should accept valid CSS values")
    void buttonStyleShouldAcceptValidCssValues() {
        CKEditorConfig.ButtonStyle style = CKEditorConfig.ButtonStyle.builder()
            .background("#fff3e0")
            .hoverBackground("rgba(0, 0, 0, 0.1)")
            .activeBackground("#ffcc80")
            .iconColor("#e65100")
            .build();

        assertThat(style.getBackground()).isEqualTo("#fff3e0");
        assertThat(style.getHoverBackground()).isEqualTo("rgba(0, 0, 0, 0.1)");
    }

    @Test
    @DisplayName("ToolbarStyle should reject data: URLs in CSS values")
    void toolbarStyleShouldRejectDataUrls() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .background("data:text/css,body{background:red}")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }

    @Test
    @DisplayName("ToolbarStyle should reject CSS comments")
    void toolbarStyleShouldRejectCssComments() {
        assertThatThrownBy(() ->
            CKEditorConfig.ToolbarStyle.builder()
                .background("#fff /* comment */")
                .build()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dangerous");
    }
}
