package com.wontlost.ckeditor;

import com.wontlost.ckeditor.mention.MentionConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.*;

import static com.wontlost.ckeditor.Constants.*;

/**
 * Configuration before Editor is initialized
 */
public class Config {

    static final JsonMapper mapper = JsonMapper.builder().build();

    static final String options = "options";

    static final String uploadUrl = "uploadUrl";

    static final String allow = "allow";

    static final String disallow = "disallow";

    static final String[] HEADING_OPTION = {"model", "view", "title", "class"};
    static final String[] CODEBLOCK_LANGUAGE = {"language", "label", "class"};
    static final String[] IMAGE_RESIZEOPTION = {"name", "value", "label"};

    static final Toolbar[] TOOLBAR = new Toolbar[]{
            Toolbar.textPartLanguage,
            Toolbar.pipe,
            Toolbar.heading,
            Toolbar.fontSize,
            Toolbar.fontFamily,
            Toolbar.fontColor,
            Toolbar.fontBackgroundColor,
            Toolbar.pipe,
            Toolbar.bold,
            Toolbar.italic,
            Toolbar.underline,
            Toolbar.findAndReplace,
            Toolbar.selectAll,
            Toolbar.strikethrough,
            Toolbar.subscript,
            Toolbar.superscript,
            Toolbar.highlight,
            Toolbar.removeFormat,
            Toolbar.pipe,
            Toolbar.horizontalLine,
            Toolbar.pageBreak,
            Toolbar.link,
            Toolbar.bulletedList,
            Toolbar.numberedList,
            Toolbar.alignment,
            Toolbar.todoList,
            Toolbar.indent,
            Toolbar.outdent,
            Toolbar.lineHeight,
            Toolbar.showBlocks,
            Toolbar.code,
            Toolbar.sourceEditing,
            Toolbar.codeBlock,
            Toolbar.pipe,
            Toolbar.specialCharacters,
            Toolbar.exportPdf,
            Toolbar.exportWord,
            Toolbar.imageUpload,
            Toolbar.blockQuote,
            Toolbar.insertTable,
            Toolbar.mediaEmbed,
            Toolbar.htmlEmbed,
            Toolbar.pipe,
            Toolbar.undo,
            Toolbar.redo};

    List<String> removedPlugins = new ArrayList<>();

    List<String> extraPlugins = new ArrayList<>();

    Map<ConfigType, JsonNode> configs = new HashMap<>();

    private void initPlugins() {
        removedPlugins.add(Plugins.WProofreader.name());
        removedPlugins.add(Plugins.StandardEditingMode.name());
        removedPlugins.add(Plugins.RestrictedEditingMode.name());
        removedPlugins.add(Plugins.Markdown.name());
        removedPlugins.add(Plugins.Pagination.name());
        removedPlugins.add(Plugins.Minimap.name());
        configs.put(ConfigType.removePlugins, toArrayNode(removedPlugins));
        configs.put(ConfigType.toolbar, toArrayNode(TOOLBAR));
    }

    public void addExtraPlugin(Plugins plugin) {
        if (!extraPlugins.contains(plugin.name())) {
            extraPlugins.add(plugin.name());
        }
        configs.put(ConfigType.extraPlugins, toArrayNode(extraPlugins));
    }

    public Config() {
        initPlugins();
        configs.put(ConfigType.alignment, mapper.createObjectNode());
        configs.put(ConfigType.autosave, mapper.createObjectNode());
        configs.put(ConfigType.balloonToolbar, mapper.createArrayNode());
        configs.put(ConfigType.blockToolbar, mapper.createArrayNode());
        configs.put(ConfigType.ckfinder, mapper.createObjectNode());
        configs.put(ConfigType.cloudServices, mapper.createObjectNode());
        configs.put(ConfigType.codeBlock, mapper.createObjectNode());
        configs.put(ConfigType.exportPdf, mapper.createObjectNode());
        configs.put(ConfigType.exportWord, mapper.createObjectNode());
        configs.put(ConfigType.extraPlugins, mapper.createArrayNode());
        configs.put(ConfigType.fontBackgroundColor, mapper.createObjectNode());
        configs.put(ConfigType.fontColor, mapper.createObjectNode());
        configs.put(ConfigType.fontFamily, mapper.createObjectNode());
        configs.put(ConfigType.fontSize, mapper.createObjectNode());
        configs.put(ConfigType.heading, mapper.createObjectNode());
        configs.put(ConfigType.highlight, mapper.createObjectNode());
        configs.put(ConfigType.image, mapper.createObjectNode());
        configs.put(ConfigType.lineHeight, mapper.createObjectNode());
        configs.put(ConfigType.indentBlock, mapper.createObjectNode());
        configs.put(ConfigType.initialData, mapper.getNodeFactory().stringNode(""));
        configs.put(ConfigType.language, mapper.getNodeFactory().stringNode("en"));
        configs.put(ConfigType.link, mapper.createObjectNode());
        configs.put(ConfigType.mediaEmbed, mapper.createObjectNode());
        configs.put(ConfigType.mention, mapper.createObjectNode());
        configs.put(ConfigType.placeholder, mapper.getNodeFactory().stringNode(""));
        configs.put(ConfigType.restrictedEditing, mapper.createObjectNode());
        configs.put(ConfigType.simpleUpload, mapper.createObjectNode());
        configs.put(ConfigType.table, mapper.createObjectNode());
        configs.put(ConfigType.title, mapper.createObjectNode());
        configs.put(ConfigType.typing, mapper.createObjectNode());
        configs.put(ConfigType.ui, mapper.createObjectNode());
        configs.put(ConfigType.wordCount, mapper.createObjectNode());
        configs.put(ConfigType.wproofreader, mapper.createObjectNode());
    }

    Config(ObjectNode jsonObject) {
        initPlugins();
        configs.put(ConfigType.alignment, jsonObject.get(ConfigType.alignment.name()));
        configs.put(ConfigType.autosave, jsonObject.get(ConfigType.autosave.name()));
        configs.put(ConfigType.balloonToolbar, jsonObject.get(ConfigType.balloonToolbar.name()));
        configs.put(ConfigType.blockToolbar, jsonObject.get(ConfigType.blockToolbar.name()));
        configs.put(ConfigType.ckfinder, jsonObject.get(ConfigType.ckfinder.name()));
        configs.put(ConfigType.cloudServices, jsonObject.get(ConfigType.cloudServices.name()));
        configs.put(ConfigType.codeBlock, jsonObject.get(ConfigType.codeBlock.name()));
        configs.put(ConfigType.exportPdf, jsonObject.get(ConfigType.exportPdf.name()));
        configs.put(ConfigType.exportWord, jsonObject.get(ConfigType.exportWord.name()));
        configs.put(ConfigType.extraPlugins, jsonObject.get(ConfigType.extraPlugins.name()));
        configs.put(ConfigType.fontBackgroundColor, jsonObject.get(ConfigType.fontBackgroundColor.name()));
        configs.put(ConfigType.fontColor, jsonObject.get(ConfigType.fontColor.name()));
        configs.put(ConfigType.fontFamily, jsonObject.get(ConfigType.fontFamily.name()));
        configs.put(ConfigType.fontSize, jsonObject.get(ConfigType.fontSize.name()));
        configs.put(ConfigType.heading, jsonObject.get(ConfigType.heading.name()));
        configs.put(ConfigType.highlight, jsonObject.get(ConfigType.highlight.name()));
        configs.put(ConfigType.lineHeight, jsonObject.get(ConfigType.lineHeight.name()));
        configs.put(ConfigType.image, jsonObject.get(ConfigType.image.name()));
        configs.put(ConfigType.indentBlock, jsonObject.get(ConfigType.indentBlock.name()));
        configs.put(ConfigType.initialData, jsonObject.get(ConfigType.initialData.name()));
        configs.put(ConfigType.language, jsonObject.get(ConfigType.language.name()));
        configs.put(ConfigType.link, jsonObject.get(ConfigType.link.name()));
        configs.put(ConfigType.mediaEmbed, jsonObject.get(ConfigType.mediaEmbed.name()));
        configs.put(ConfigType.mention, jsonObject.get(ConfigType.mention.name()));
        configs.put(ConfigType.placeholder, jsonObject.get(ConfigType.placeholder.name()));
        configs.put(ConfigType.removePlugins, jsonObject.get(ConfigType.removePlugins.name()) == null ?
                toArrayNode(removedPlugins) :
                jsonObject.get(ConfigType.removePlugins.name()));
        configs.put(ConfigType.restrictedEditing, jsonObject.get(ConfigType.restrictedEditing.name()));
        configs.put(ConfigType.simpleUpload, jsonObject.get(ConfigType.simpleUpload.name()));
        configs.put(ConfigType.table, jsonObject.get(ConfigType.table.name()));
        configs.put(ConfigType.title, jsonObject.get(ConfigType.title.name()));
        configs.put(ConfigType.typing, jsonObject.get(ConfigType.typing.name()));
        configs.put(ConfigType.wordCount, jsonObject.get(ConfigType.wordCount.name()));
        configs.put(ConfigType.ui, jsonObject.get(ConfigType.ui.name()));
        configs.put(ConfigType.wproofreader, jsonObject.get(ConfigType.wproofreader.name()));
    }

    ObjectNode getConfigJson() {
        ObjectNode configResult = mapper.createObjectNode();

        configs.forEach((configType, configJson) ->
                configResult.set(configType.name(), configJson)
        );

        return configResult;
    }

    public Map<ConfigType, JsonNode> getConfigs() {
        return configs;
    }

    /**
     * @param toolbar Toolbar of Editor, refer to enum @Constants.Toolbar
     * @return ArrayNode
     */
    ArrayNode toArrayNode(Toolbar... toolbar) {
        List<String> values = new ArrayList<>();
        if (toolbar == null || toolbar.length == 0) {
            toolbar = TOOLBAR;
        }
        Arrays.stream(toolbar).forEach(item -> values.add(item.getValue()));

        return mapper.valueToTree(values);
    }

    ArrayNode toArrayNode(TextPartLanguage... textPartLanguages) {
        List<String> values = new ArrayList<>();
        Arrays.stream(textPartLanguages).forEach(item -> values.add(item.toString()));
        return mapper.valueToTree(values);
    }

    ObjectNode toJsonObjectArray(String member, String[] names, String[][] values) {
        List<Map<String, String>> list = new ArrayList<>();
        for (String[] object : values) {
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < names.length && i < object.length; i++)
                if (object[i] != null)
                    map.put(names[i], object[i]);
            list.add(map);
        }

        JsonNode json = mapper.valueToTree(list);
        String jsonObject = "{'" + member + "':" + json + "}";

        return mapper.valueToTree(jsonObject);
    }

    ArrayNode toJsonObjectArray(String[] names, String[][] values) {
        String member = "temp";
        ObjectNode json = toJsonObjectArray(member, names, values);

        return (ArrayNode) json.get(member);
    }

    ArrayNode toArrayNode(List<?> options) {
        return mapper.valueToTree(options);
    }

    ArrayNode toArrayNode(String[][] options) {
        return mapper.valueToTree(options);
    }

    /**
     * @param list String list
     * @return ArrayNode
     */
    ArrayNode toArrayNode(String... list) {
        List<String> values = Arrays.asList(list == null ? new String[0] : list);
        return mapper.valueToTree(values);
    }

    ArrayNode mapToArrayNode(List<Map<String, Object>> mapList) {
        return mapper.valueToTree(mapList);
    }

    /**
     * @param placeHolder Place holder of Editor
     */
    public void setPlaceHolder(String placeHolder) {
        configs.put(ConfigType.placeholder, mapper.getNodeFactory().stringNode(Optional.ofNullable(placeHolder).orElse("Type the content here!")));
    }

    /**
     * @param editorToolBar Toolbar of Editor, refer to enum @Constants.Toolbar
     */
    public void setEditorToolBar(Toolbar[] editorToolBar) {
        configs.put(ConfigType.toolbar, toArrayNode(editorToolBar));
    }

    /**
     * @param editorToolBar Toolbar of Editor, refer to enum @Constants.Toolbar
     */
    public void setEditorToolBarObject(Toolbar[] editorToolBar, Boolean shouldNotGroupWhenFull) {
        ObjectNode toolbar = mapper.createObjectNode();
        toolbar.set("items", toArrayNode(editorToolBar));
        toolbar.put("shouldNotGroupWhenFull", shouldNotGroupWhenFull);
        configs.put(ConfigType.toolbar, toolbar);
    }

    /**
     * @param uiLanguage Language of user interface, refer to enum @Language
     */
    public void setUILanguage(Language uiLanguage) {
        configs.put(ConfigType.language, mapper.getNodeFactory().stringNode(uiLanguage == null ? "en" : uiLanguage.getLanguage()));
    }

    public void setLanguage(Language uiLanguage, Language contentLanguage, TextPartLanguage[] textPartLanguages) {
        ObjectNode language = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (int i = 0; i < textPartLanguages.length; i++) {
            ObjectNode langCode = mapper.createObjectNode();
            langCode.put("title", textPartLanguages[i].getTitle());
            langCode.put("languageCode", textPartLanguages[i].getLanguage());
            arrayNode.set(i, langCode);
        }
        language.put("ui", uiLanguage.getLanguage());
        language.put("content", contentLanguage.getLanguage());
        language.set("textPartLanguage", arrayNode);
        configs.put(ConfigType.language, language);
    }

    /**
     * Configuation of alignment
     *
     * @param options The available options are: 'left', 'right', 'center' and 'justify'. Other values are ignored.
     */
    public void setAlignment(String[] options) {
        ObjectNode alignment = mapper.createObjectNode();
        alignment.set(Config.options, toArrayNode(options));
        configs.put(ConfigType.alignment, alignment);
    }

    /**
     * @param balloonToolBar BalloonToolbar of Editor, refer to enum @Constants.Toolbar.
     *                       Contextual toolbar configuration. Used by the BalloonToolbar feature.
     */
    public void setBalloonToolBar(Toolbar[] balloonToolBar) {
        configs.put(ConfigType.balloonToolbar, toArrayNode(balloonToolBar));
    }

    /**
     * @param blockToolBar BlockToolbar of Editor, refer to enum @Constants.Toolbar.
     *                     The block toolbar configuration. Used by the BlockToolbar feature.
     */
    public void setBlockToolBar(Toolbar[] blockToolBar) {
        configs.put(ConfigType.blockToolbar, toArrayNode(blockToolBar));
    }

    /**
     * CKFinder configurations
     *
     * @param openerMethod The type of the CKFinder opener method.
     *                     Supported types are:
     *                     'modal' – Opens CKFinder in a modal,
     *                     'popup' – Opens CKFinder in a new "pop-up" window.
     *                     Defaults to 'modal'.
     * @param uploadUrl    The path (URL) to the connector which handles the file upload in CKFinder file manager.
     *                     When specified, it enables the automatic upload of resources
     *                     such as images inserted into the content.
     *                     Used by the upload adapter.
     * @param options      Configuration settings for CKFinder. Not fully integrated with ckfinder
     */
    public void setCKFinder(String openerMethod, String uploadUrl, Map<String, String> options) {
        ObjectNode ckfinder = mapper.createObjectNode();
        ckfinder.set("openerMethod", mapper.getNodeFactory().stringNode(openerMethod));
        ckfinder.set(Config.uploadUrl, mapper.getNodeFactory().stringNode(uploadUrl));
        ObjectNode ckfinderOptions = mapper.createObjectNode();
        ckfinderOptions.put("connectorInfo", options.get("connectorInfo"));
        ckfinderOptions.put("connectorPath", options.get("connectorPath"));
        ckfinderOptions.put("height", options.get("height"));
        ckfinderOptions.put("width", options.get("width"));
        ckfinder.set(Config.options, ckfinderOptions);
        configs.put(ConfigType.ckfinder, ckfinder);
    }

    /**
     * The configuration of CKEditor Cloud Services
     *
     * @param bundleVersion An optional parameter used for integration with CKEditor Cloud Services
     *                      when uploading the editor build to cloud services. Whenever the editor build or
     *                      the configuration changes, this parameter should be set to a new, unique value
     *                      to differentiate the new bundle (build + configuration) from the old ones.
     * @param tokenUrl      A token URL which should be a URL to the security token endpoint in your application.
     *                      The role of this endpoint is to securely authorize the end users of your application
     *                      to use CKEditor Cloud Services only if they should have access e.g. to upload files
     *                      with Easy Image or to use the Collaboration service.
     * @param uploadUrl     The endpoint URL for CKEditor Cloud Services uploads.
     *                      This option must be set for Easy Image to work correctly.
     *                      The upload URL is unique for each customer and can be found in the CKEditor Ecosystem customer dashboard
     *                      after subscribing to the Easy Image service. To learn how to start using Easy Image,
     *                      check the Easy Image - Quick start documentation.
     *                      Note: Make sure to also set the tokenUrl configuration option.
     * @param webSocketUrl  The URL for web socket communication, used by the RealTimeCollaborativeEditing plugin.
     *                      Every customer (organization in the CKEditor Ecosystem dashboard) has their own,
     *                      unique URLs to communicate with CKEditor Cloud Services. The URL can be found in the
     *                      CKEditor Ecosystem customer dashboard.
     *
     */
    public void setCloudServices(String bundleVersion, String tokenUrl, String uploadUrl, String webSocketUrl) {
        ObjectNode cloudServices = mapper.createObjectNode();
        cloudServices.set("bundleVersion", mapper.getNodeFactory().stringNode(bundleVersion));
        cloudServices.set("tokenUrl", mapper.getNodeFactory().stringNode(tokenUrl));
        cloudServices.set(Config.uploadUrl, mapper.getNodeFactory().stringNode(uploadUrl));
        cloudServices.set("webSocketUrl", mapper.getNodeFactory().stringNode(webSocketUrl));
        configs.put(ConfigType.cloudServices, cloudServices);
    }

    /**
     * Viewport offset can be used to constrain balloons or other UI elements into an element smaller than the viewport. This can be useful if there are any other absolutely positioned elements that may interfere with editor UI.
     *
     * @param viewportOffsets [top, right, bottom, left]
     */
    public void setUiViewportOffset(Double... viewportOffsets) {
        ObjectNode ui = mapper.createObjectNode();
        ObjectNode viewportOffset = mapper.createObjectNode();
        if (viewportOffsets.length > 0 && viewportOffsets[0] != null) {
            viewportOffset.put("top", viewportOffsets[0]);
        }
        if (viewportOffsets.length > 1 && viewportOffsets[1] != null) {
            viewportOffset.put("right", viewportOffsets[1]);
        }
        if (viewportOffsets.length > 2 && viewportOffsets[2] != null) {
            viewportOffset.put("bottom", viewportOffsets[2]);
        }
        if (viewportOffsets.length > 3 && viewportOffsets[3] != null) {
            viewportOffset.put("left", viewportOffsets[3]);
        }
        ui.set("viewportOffset", viewportOffset);
        configs.put(ConfigType.ui, ui);
    }

    /**
     *
     * @param indentSequence A sequence of characters inserted or removed from the code block lines 、
     *                       when its indentation is changed by the user, for instance, using Tab and Shift+Tab keys.
     *                       The default value is a single tab character (" ", \u0009 in Unicode).
     *                       This configuration is used by indentCodeBlock and outdentCodeBlock commands
     *                       (instances of IndentCodeBlockCommand).
     *                       Note: Setting this configuration to false will disable the code block indentation commands
     *                       and associated keystrokes.
     * @param languages      The list of code languages available in the user interface to choose for a particular code block.
     *                       [
     *                       { language: 'plaintext', label: 'Plain text', class: '' },
     *                       { language: 'php', label: 'PHP', class: 'php-code' }
     *                       ]
     *                       class is optional
     *                       updated by Thomas Kohler (tko@hp23.at)
     */
    //public void setCodeBlock(String indentSequence, String[] languages) {
    //    ObjectNode codeBlock = mapper.createObjectNode();
    //    codeBlock.put("indentSequence", mapper.getNodeFactory().stringNode(indentSequence));
    //    codeBlock.put("languages", toArrayNode(languages));
    //    configs.put(ConfigType.codeBlock, codeBlock);
    //}
    public void setCodeBlock(String indentSequence, String[][] languages) {
        ObjectNode codeBlock = mapper.createObjectNode();
        if (indentSequence != null)
            codeBlock.set("indentSequence", mapper.getNodeFactory().stringNode(indentSequence));
        if (languages != null && languages.length > 0)
            codeBlock.set("languages", toJsonObjectArray(CODEBLOCK_LANGUAGE, languages));
        configs.put(ConfigType.codeBlock, codeBlock);
    }

    /**
     * The configuration of the export to PDF feature.
     *
     * @param fileName     The name of the generated PDF file.
     * @param converterUrl A URL to the HTML to PDF converter.
     */
    public void setExportPdf(String fileName, String converterUrl) {
        ObjectNode exportPdf = mapper.createObjectNode();
        exportPdf.set("fileName", mapper.getNodeFactory().stringNode(fileName));
        exportPdf.set("converterUrl", mapper.getNodeFactory().stringNode(converterUrl));
        configs.put(ConfigType.exportPdf, exportPdf);
    }

    /**
     * The configuration of the export to Word feature.
     *
     * @param fileName     The name of the generated Word file.
     * @param converterUrl A URL to the HTML to Word converter.
     */
    public void setExportWord(String fileName, String converterUrl) {
        ObjectNode exportWord = mapper.createObjectNode();
        exportWord.set("fileName", mapper.getNodeFactory().stringNode(fileName));
        exportWord.set("converterUrl", mapper.getNodeFactory().stringNode(converterUrl));
        configs.put(ConfigType.exportWord, exportWord);
    }

    /**
     * The configuration of the font background color feature.
     *
     * @param columns        Represents the number of columns in the font background color dropdown. Defaults to 5.
     * @param documentColors Determines the maximum number of available document colors.
     *                       Setting it to 0 will disable the document colors feature.
     *                       By default it equals to the columns value.
     * @param colors         Available font background colors defined as an array of strings or objects.
     *                       colors:
     *                       [
     *                       {
     *                       color: 'hsl(0, 0%, 0%)',
     *                       label: 'Black'
     *                       },
     *                       {
     *                       color: 'hsl(0, 0%, 30%)',
     *                       label: 'Dim grey'
     *                       }
     *                       ]
     */
    public void setFontBackgroundColor(int columns, int documentColors, Map<String, String> colors) {
        ObjectNode fontBackgroundColor = mapper.createObjectNode();
        fontBackgroundColor.set("columns", mapper.getNodeFactory().numberNode(columns));
        fontBackgroundColor.set("documentColors", mapper.getNodeFactory().numberNode(documentColors));
        fontBackgroundColor.set("colors", mapper.createArrayNode());//TODO: imply colors
        configs.put(ConfigType.fontBackgroundColor, fontBackgroundColor);
    }

    /**
     * The configuration of the font color feature.
     *
     * @param columns        Represents the number of columns in the font background color dropdown. Defaults to 5.
     * @param documentColors Determines the maximum number of available document colors.
     *                       Setting it to 0 will disable the document colors feature.
     *                       By default it equals to the columns value.
     * @param colors         Available font background colors defined as an array of strings or objects.
     *                       colors:
     *                       [
     *                       {
     *                       color: 'hsl(0, 0%, 0%)',
     *                       label: 'Black'
     *                       },
     *                       {
     *                       color: 'hsl(0, 0%, 30%)',
     *                       label: 'Dim grey'
     *                       }
     *                       ]
     */
    public void setFontColor(int columns, int documentColors, Map<String, String> colors) {
        ObjectNode fontBackgroundColor = mapper.createObjectNode();
        fontBackgroundColor.set("columns", mapper.getNodeFactory().numberNode(columns));
        fontBackgroundColor.set("documentColors", mapper.getNodeFactory().numberNode(documentColors));
        fontBackgroundColor.set("colors", mapper.createArrayNode());//TODO: imply colors
        configs.put(ConfigType.fontColor, fontBackgroundColor);
    }

    /**
     *
     * @param supportAllValues By default the plugin removes any font-family value that does not match
     *                         the plugin's configuration. It means that if you paste content with font families
     *                         that the editor does not understand, the font-family attribute will be removed and
     *                         the content will be displayed with the default font.
     *                         You can preserve pasted font family values by switching the supportAllValues option to true
     * @param options          Available font family options defined as an array of strings. The default value is:
     *                         'default',
     *                         'Arial, Helvetica, sans-serif',
     *                         'Courier New, Courier, monospace',
     *                         'Georgia, serif',
     *                         'Lucida Sans Unicode, Lucida Grande, sans-serif',
     *                         'Tahoma, Geneva, sans-serif',
     *                         'Times New Roman, Times, serif',
     *                         'Trebuchet MS, Helvetica, sans-serif',
     *                         'Verdana, Geneva, sans-serif'
     */
    public void setFontFamily(boolean supportAllValues, String[] options) {
        ObjectNode fontFamily = mapper.createObjectNode();
        fontFamily.set("supportAllValues", mapper.getNodeFactory().booleanNode(supportAllValues));
        fontFamily.set(Config.options, toArrayNode(options));
        configs.put(ConfigType.fontFamily, fontFamily);
    }

    /**
     *
     * @param supportAllValues By default the plugin removes any font-size value that
     *                         does not match the plugin's configuration. It means that if you paste content with
     *                         font sizes that the editor does not understand, the font-size attribute will be removed
     *                         and the content will be displayed with the default size.
     *                         You can preserve pasted font size values by switching the supportAllValues option to true
     * @param options          Available font size options. Expressed as predefined presets, numerical "pixel" values
     */
    public void setFontSize(boolean supportAllValues, String[] options) {
        ObjectNode fontSize = mapper.createObjectNode();
        fontSize.set("supportAllValues", mapper.getNodeFactory().booleanNode(supportAllValues));
        fontSize.set(Config.options, toArrayNode(options));
        configs.put(ConfigType.fontSize, fontSize);
    }

    /**
     * Configuration of heading
     *
     * @param options The available heading options.
     *                [
     *                { model: 'paragraph', title: 'Paragraph', class: 'ck-heading_paragraph' },
     *                { model: 'heading1', view: 'h2', title: 'Heading 1', class: 'ck-heading_heading1' },
     *                { model: 'heading2', view: 'h3', title: 'Heading 2', class: 'ck-heading_heading2' },
     *                { model: 'heading3', view: 'h4', title: 'Heading 3', class: 'ck-heading_heading3' }
     *                ]
     *                updated by Thomas Kohler (tko@hp23.at)
     */
    //public void setHeading(String[][] options) {
    //    ObjectNode heading = mapper.createObjectNode();
    //    heading.put(Config.options, toArrayNode(options));
    //    configs.put(ConfigType.heading, heading);
    //}
    public void setHeading(String[][] options) {
        ObjectNode heading = mapper.createObjectNode();
        if (options != null && options.length > 0)
            heading.set("options", toJsonObjectArray(HEADING_OPTION, options));

        configs.put(ConfigType.heading, heading);
    }

    /**
     * Configuration of highlight
     *
     * @param options The available highlight options.
     *                [
     *                {
     *                model: 'yellowMarker',
     *                class: 'marker-yellow',
     *                title: 'Yellow marker',
     *                color: 'var(--ck-highlight-marker-yellow)',
     *                type: 'marker'
     *                },
     *                {
     *                model: 'greenMarker',
     *                class: 'marker-green',
     *                title: 'Green marker',
     *                color: 'var(--ck-highlight-marker-green)',
     *                type: 'marker'
     *                }
     *                ]
     */
    public void setHighlight(String[][] options) {
        ObjectNode highlight = mapper.createObjectNode();
        highlight.set(Config.options, toArrayNode(options));
        configs.put(ConfigType.highlight, highlight);
    }

    /**
     * @param resizeOptions The image resize options.
     *                      resizeOptions: [ {
     *                      name: 'imageResize:original',
     *                      value: null
     *                      },
     *                      {
     *                      name: 'imageResize:50',
     *                      value: '50'
     *                      } ]
     * @param resizeUnit    The available options are 'px' or '%'.
     * @param styles        Available image styles.
     *                      The default value is:
     *                      const imageConfig = {
     *                      styles: [ 'full', 'side' ]
     *                      };
     * @param toolbar       Items to be placed in the image toolbar.
     *                      three toolbar items will be available in ComponentFactory: 'imageStyle:full', 'imageStyle:side', and 'imageTextAlternative'
     *                      so you can configure the toolbar like this:
     *                      const imageConfig = {
     *                      toolbar: [ 'imageStyle:full', 'imageStyle:side', '|', 'imageTextAlternative' ]
     *                      };
     * @param uploadTypes   The image upload configuration.
     *                      The list of accepted image types.
     *                      The accepted types of images can be customized to allow only certain types of images:
     *                      // Allow only JPEG and PNG images:
     *                      const imageUploadConfig = {
     *                      types: [ 'png', 'jpeg' ]
     *                      };
     *                      updated by Thomas Kohler (tko@hp23.at)
     */
    //public void setImage(String[][] resizeOptions, String resizeUnit, String[] styles,
    //                     String[] toolbar, String[] upload) {
    //    ObjectNode image = mapper.createObjectNode();
    //    image.put("resizeOptions", toArrayNode(resizeOptions));
    //    image.put("resizeUnit", mapper.getNodeFactory().stringNode(resizeUnit));
    //    image.put("styles", toArrayNode(styles));
    //    image.put("toolbar", toArrayNode(toolbar));
    //    image.put("types", toArrayNode(upload));
    //    configs.put(ConfigType.image, image);
    //}
    public void setImage(String[][] resizeOptions, String resizeUnit, String[] styles, String[] toolbar, String[] uploadTypes) {
        ObjectNode image = mapper.createObjectNode();

        if (resizeOptions != null && resizeOptions.length > 0)
            image.set("options", toJsonObjectArray(IMAGE_RESIZEOPTION, resizeOptions));

        if (resizeUnit != null)
            image.set("resizeUnit", mapper.getNodeFactory().stringNode(resizeUnit));

        if (styles.length > 0)
            image.set("styles", toArrayNode(styles));

        if (toolbar.length > 0)
            image.set("toolbar", toArrayNode(toolbar));

        if (uploadTypes.length > 0) {
            ObjectNode upload = mapper.createObjectNode();
            upload.set("types", toArrayNode(uploadTypes));
            image.set("upload", upload);
        }
        configs.put(ConfigType.image, image);
    }

    /**
     * The configuration of the block indentation feature.
     *
     * @param offset The size of indentation units for each indentation step. Default 40
     * @param unit   The unit used for indentation offset.
     */
    public void setIndentBlock(int offset, String unit) {
        ObjectNode indentBlock = mapper.createObjectNode();
        indentBlock.set("offset", mapper.getNodeFactory().numberNode(offset));
        indentBlock.set("unit", mapper.getNodeFactory().stringNode(unit));
        configs.put(ConfigType.indentBlock, indentBlock);
    }

    /**
     * By default, the editor is initialized with the content of the element on which this editor is initialized.
     *
     * @param initialData The initial editor data to be used instead of the provided element's HTML content.
     */
    public void setInitialData(String initialData) {
        configs.put(ConfigType.initialData, mapper.getNodeFactory().stringNode(initialData));
    }

    /**
     * The configuration of the link feature.
     *
     * @param defaultProtocol          When set, the editor will add the given protocol to the link when the user creates a link without one. For example, when the user is creating a link and types
     *                                 ckeditor.com in the link form input, during link submission the editor will automatically add the http:// protocol, so the link will look as follows: http://ckeditor.com.
     *                                 The feature also provides email address auto-detection. When you submit hello@example.com, the plugin will automatically change it to mailto:hello@example.com.
     * @param addTargetToExternalLinks When set to true, the target="blank" and rel="noopener noreferrer"
     *                                 attributes are automatically added to all external links in the editor. "External links" are all links in the editor content starting with http, https, or //.
     */
    public void setLink(String defaultProtocol, Boolean addTargetToExternalLinks) {
        ObjectNode link = mapper.createObjectNode();
        link.set("defaultProtocol", mapper.getNodeFactory().stringNode(defaultProtocol));
        link.set("addTargetToExternalLinks", mapper.getNodeFactory().booleanNode(addTargetToExternalLinks));
        configs.put(ConfigType.link, link);
    }

    /**
     * The configuration of the media embed features.
     *
     * @param previewsInData  Controls the data format produced by the feature.
     * @param providers       The default media providers supported by the editor.
     * @param extraProviders  The additional media providers supported by the editor. This configuration helps extend the default providers.
     * @param removeProviders The list of media providers that should not be used despite being available in config.mediaEmbed.providers and config.mediaEmbed.extraProviders
     * @param toolbar         Items to be placed in the media embed toolbar. This option requires adding MediaEmbedToolbar to the plugin list.
     */
    public void setMediaEmbed(Boolean previewsInData, List<String> providers, List<String> extraProviders, List<String> removeProviders, List<String> toolbar) {
        ObjectNode mediaEmbed = mapper.createObjectNode();
        mediaEmbed.set("previewsInData", mapper.getNodeFactory().booleanNode(previewsInData));
        mediaEmbed.set("providers", toArrayNode(providers));
        mediaEmbed.set("extraProviders", toArrayNode(extraProviders));
        mediaEmbed.set("removeProviders", toArrayNode(removeProviders));
        mediaEmbed.set("toolbar", toArrayNode(toolbar));
        configs.put(ConfigType.mediaEmbed, mediaEmbed);
    }

    /**
     * The configuration of the mention feature.
     * refer to https://ckeditor.com/docs/ckeditor5/latest/api/module_mention_mention-MentionConfig.html
     *
     * @param mentionConfig configuration on mention.
     */
    public void setMention(MentionConfig mentionConfig) {
        JsonNode mentionNode = mapper.valueToTree(mentionConfig);
        configs.put(ConfigType.mention, mentionNode);
    }

    /**
     * The list of plugins which should not be loaded despite being available in an editor build.
     *
     * @param plugins names of plugin
     */
    public void setRemovePlugins(List<Plugins> plugins) {
        List<String> toBeRemoved = new ArrayList<>();
        plugins.forEach(plugin -> toBeRemoved.add(plugin.name()));
        configs.put(ConfigType.removePlugins, toArrayNode(toBeRemoved));
    }

    /**
     * The configuration of the restricted editing mode feature.
     *
     * @param allowedAttributes The text attribute names allowed when pasting content ot non-restricted areas.
     * @param allowedCommands   The command names allowed in non-restricted areas of the content.
     *                          Defines which feature commands should be enabled in the restricted editing mode.
     *                          The commands used for typing and deleting text ('input', 'delete' and 'forwardDelete')
     *                          are allowed by the feature inside non-restricted regions and do not need to be defined.
     *                          Note: The restricted editing mode always allows to use the restricted mode navigation
     *                          commands as well as 'undo' and 'redo' commands.
     */
    public void setRestrictedEditing(List<String> allowedAttributes, List<String> allowedCommands) {
        ObjectNode restrictedEditing = mapper.createObjectNode();
        restrictedEditing.set("allowedAttributes", toArrayNode(allowedAttributes));
        restrictedEditing.set("allowedCommands", toArrayNode(allowedCommands));
        configs.put(ConfigType.restrictedEditing, restrictedEditing);
    }

    /**
     *
     * @param uploadUrl       The path (URL) to the server (application) which handles the file upload. When specified,
     *                        enables the automatic upload of resources (images) inserted into the editor content.
     * @param withCredentials This flag enables the withCredentials property of the request sent to the server
     *                        during the upload. It affects cross-site requests only and, for instance,
     *                        allows credentials such as cookies to be sent along with the request.
     * @param headers         An object that defines additional headers sent with the request to the server during the upload.
     *                        This is the right place to implement security mechanisms like authentication and CSRF protection.
     */
    public void setSimpleUpload(String uploadUrl, Boolean withCredentials, List<String> headers) {
        ObjectNode simpleUpload = mapper.createObjectNode();
        simpleUpload.set(Config.uploadUrl, mapper.getNodeFactory().stringNode(uploadUrl));
        simpleUpload.set("withCredentials", mapper.getNodeFactory().booleanNode(withCredentials));
        simpleUpload.set("headers", toArrayNode(headers));
        configs.put(ConfigType.simpleUpload, simpleUpload);
    }

    /**
     * The configuration of the table feature. Used by the table feature in the
     *
     * @param contentToolbar      Items to be placed in the table content toolbar. The TableToolbar plugin is required to make this toolbar work.
     * @param tableToolbar        Items to be placed in the table toolbar. The TableToolbar plugin is required to make this toolbar work.
     * @param tableCellProperties The configuration of the table cell properties user interface (balloon).
     * @param tableProperties     The configuration of the table properties user interface (balloon)
     */
    public void setTable(List<String> contentToolbar, List<String> tableToolbar, ObjectNode tableCellProperties, ObjectNode tableProperties) {
        ObjectNode table = mapper.createObjectNode();
        table.set("contentToolbar", toArrayNode(contentToolbar));
        table.set("tableToolbar", toArrayNode(tableToolbar));
        table.set("tableCellProperties", tableCellProperties);
        table.set("tableProperties", tableCellProperties);
        configs.put(ConfigType.table, table);
    }

    /**
     * The configuration of the line height feature.
     *
     * @param options Items to be placed in the line height toolbar
     */
    public void setLineHeight(List<Integer> options) {
        ObjectNode lineHeight = mapper.createObjectNode();
        lineHeight.set("options", toArrayNode(options));
        configs.put(ConfigType.lineHeight, lineHeight);
    }

    /**
     * The configuration of the title feature.
     *
     * @param placeholder Defines a custom value of the placeholder for the title field.
     */
    public void setTitle(String placeholder) {
        ObjectNode title = mapper.createObjectNode();
        title.set("placeholder", mapper.getNodeFactory().stringNode(placeholder));
        configs.put(ConfigType.title, title);
    }

    /**
     * The configuration of the title feature.
     *
     * @param undo            Default to 20
     * @param transformations Transformations
     */
    public void setTyping(int undo, ObjectNode transformations) {
        ObjectNode typing = mapper.createObjectNode();
        typing.set("undo", mapper.getNodeFactory().numberNode(undo));
        typing.set("transformations", transformations);
        configs.put(ConfigType.typing, typing);
    }

    /**
     * If you are going to use WproofReader, you have to add this config or WproofReaderServer.
     *
     * @param serviceId After signing up for a trial or paid version, you will receive your service ID which is used to activate the service.
     * @param srcUrl    Default: https://svc.webspellchecker.net/spellcheck31/wscbundle/wscbundle.js
     */
    public void setWproofReaderCloud(String serviceId, String srcUrl) {
        ObjectNode wproofReaderCloud = mapper.createObjectNode();
        String defaultSrcUrl = "https://svc.webspellchecker.net/spellcheck31/wscbundle/wscbundle.js";
        wproofReaderCloud.set("serviceId", mapper.getNodeFactory().stringNode(serviceId));
        wproofReaderCloud.set("srcUrl", mapper.getNodeFactory().stringNode(Optional.ofNullable(srcUrl).orElse(defaultSrcUrl)));
        configs.put(ConfigType.wproofreader, wproofReaderCloud);
        setPluginStatus(Plugins.WProofreader, true); //Wproofreader is not enabled initially.
    }

    /**
     * If you are going to use WproofReader, you have to add this config or WproofReaderCloud.
     *
     * @param serviceProtocol Default to 'https'.
     * @param serviceHost     Default to localhost.
     * @param servicePort     Default to 8080.
     * @param servicePath     Default to '/'.
     * @param srcUrl          String like 'https://host_name/virtual_directory/wscbundle/wscbundle.js'
     */
    public void setWproofReaderServer(String serviceProtocol, String serviceHost, Integer servicePort, String servicePath, String srcUrl) {
        ObjectNode wproofReaderServer = mapper.createObjectNode();
        wproofReaderServer.set("serviceProtocol", mapper.getNodeFactory().stringNode(Optional.ofNullable(serviceProtocol).orElse("https")));
        wproofReaderServer.set("serviceHost", mapper.getNodeFactory().stringNode(Optional.ofNullable(serviceHost).orElse("localhost")));
        wproofReaderServer.set("servicePort", mapper.getNodeFactory().numberNode(Optional.ofNullable(servicePort).orElse(8080)));
        wproofReaderServer.set("servicePath", mapper.getNodeFactory().stringNode(Optional.ofNullable(servicePath).orElse("/")));
        wproofReaderServer.set("srcUrl", mapper.getNodeFactory().stringNode(Optional.ofNullable(srcUrl).orElse("/wscbundle/wscbundle.js")));
        configs.put(ConfigType.wproofreader, wproofReaderServer);
        setPluginStatus(Plugins.WProofreader, true); //Wproofreader is not enabled initially.
    }

    /**
     * All plugins are enabled by default
     *
     * @param plugin Plugin
     * @param active Plugin status
     */
    public void setPluginStatus(Plugins plugin, boolean active) {
//        ArrayNode pluginArray = (ArrayNode) configs.get(ConfigType.plugins);
        ArrayNode removePluginArray = (ArrayNode) configs.get(ConfigType.removePlugins);
//        ArrayNode extraPluginArray = (ArrayNode) configs.get(ConfigType.extraPlugins);
        updateArrayNode(active, removePluginArray, plugin.name());
    }

    /**
     * Use standard editing mode by invoking this method
     *
     * @deprecated use setEditingMode(EditingMode editingMode) instead
     */
    @Deprecated
    public void enableStandardMode() {
        setPluginStatus(Plugins.StandardEditingMode, true);
        setPluginStatus(Plugins.RestrictedEditingMode, false);
        updateToolbar();
    }

    /**
     * Premium feature which needs to set a license by #setLicenseKey
     * Pagination works only for decoupled editor
     */
    public void enablePagination() {
        setPluginStatus(Plugins.Pagination, true);
        updateToolbar();
    }

    public void enableMinimap() {
        setPluginStatus(Plugins.Minimap, true);
    }

    /**
     * Used for restricted editing
     *
     * @param editingMode
     */
    public void setEditingMode(EditingMode editingMode) {
        if (EditingMode.Restricted.equals(editingMode)) {
            setPluginStatus(Plugins.StandardEditingMode, false);
            setPluginStatus(Plugins.RestrictedEditingMode, true);
        } else {
            setPluginStatus(Plugins.StandardEditingMode, true);
            setPluginStatus(Plugins.RestrictedEditingMode, false);
        }
        updateToolbar();
    }

    /**
     * Use restricted editing mode by invoking this method
     *
     * @deprecated use setEditingMode(EditingMode editingMode) instead
     */
    @Deprecated
    public void enableRestrictedMode() {
        setPluginStatus(Plugins.StandardEditingMode, false);
        setPluginStatus(Plugins.RestrictedEditingMode, true);
        updateToolbar();
    }

    private void updateArrayNode(boolean active, ArrayNode arrayNode, String name) {
        int index = -1;
        for (int i = 0; i < arrayNode.size(); i++) {
            if (name.equals(arrayNode.get(i).asString())) {
                index = i;
            }
        }
        if (index >= 0) {
            if (active)
                arrayNode.remove(index);
            else
                arrayNode.set(index, name);
        } else {
            if (!active)
                arrayNode.set(arrayNode.size(), name);
        }
    }

    /**
     * Update toolbar accordingly, because there is a conflict between standard edit mode and restrict edit mode.
     * They have different actions in toolbar.
     */
    private void updateToolbar() {
        ArrayNode removePluginArray = (ArrayNode) configs.get(ConfigType.removePlugins);
        boolean paginationEnabled = true;
        if (removePluginArray != null) {
            for (int i = 0; i < removePluginArray.size(); i++) {
                if (Plugins.RestrictedEditingMode.name().equals(removePluginArray.get(i).asString())) {
                    changeToolbarItem(Toolbar.restrictedEditing, true);
                    changeToolbarItem(Toolbar.restrictedEditingException, false);
                } else if (Plugins.StandardEditingMode.name().equals(removePluginArray.get(i).asString())) {
                    changeToolbarItem(Toolbar.restrictedEditing, false);
                    changeToolbarItem(Toolbar.restrictedEditingException, true);
                } else if (Plugins.Pagination.name().equals(removePluginArray.get(i).asString())) {
                    paginationEnabled = false;
                }
            }
        }
        if (paginationEnabled) {
            changeToolbarItem(Toolbar.previousPage, false);
            changeToolbarItem(Toolbar.nextPage, false);
            changeToolbarItem(Toolbar.pageNavigation, false);
        }
    }

    private void changeToolbarItem(Toolbar toolbar, boolean remove) {
        ArrayNode toolbarArray = (ArrayNode) configs.get(ConfigType.toolbar);
        updateArrayNode(remove, toolbarArray, toolbar.getValue());
    }

    /**
     * The configuration of the word count feature.
     *
     * @param container         Allows for providing the HTML element that the word count container will be appended to automatically.
     * @param displayCharacters This option allows for hiding the character counter. The element obtained through
     *                          wordCountContainer will only preserve the words part. Character counter is displayed
     *                          by default when this configuration option is not defined.
     * @param displayWords      This option allows for hiding the word counter. The element obtained through wordCountContainer
     *                          will only preserve the characters part. Word counter is displayed by default when this configuration
     *                          option is not defined.
     * @param onUpdate          This configuration takes a function that is executed whenever the word count plugin updates its
     *                          values. This function is called with one argument, which is an object with the words and characters
     *                          keys containing the number of detected words and characters in the document.
     */
    public void setWordCount(String container, Boolean displayCharacters, Boolean displayWords, ObjectNode onUpdate) {
        ObjectNode wordCount = mapper.createObjectNode();
        wordCount.set("container", mapper.getNodeFactory().stringNode(container));
        wordCount.set("displayCharacters", mapper.getNodeFactory().booleanNode(displayCharacters));
        wordCount.set("displayWords", mapper.getNodeFactory().booleanNode(displayWords));
        wordCount.set("onUpdate", onUpdate);
        configs.put(ConfigType.wordCount, wordCount);
    }

    /**
     * Defaulted to A4 paper
     *
     * @param pageWidth  default 21cm
     * @param pageHeight default 29.7cm
     * @param top        default 20mm
     * @param left       default 12mm
     * @param bottom     defalt 20mm
     * @param right      default 12mm
     */
    public void setPagination(String pageWidth, String pageHeight, String top, String left, String bottom, String right) {
        ObjectNode pagination = mapper.createObjectNode();
        pagination.put("pageWidth", pageWidth);
        pagination.put("pageHeight", pageHeight);
        ObjectNode pageMargins = mapper.createObjectNode();
        pageMargins.put("top", top);
        pageMargins.put("bottom", bottom);
        pageMargins.put("right", right);
        pageMargins.put("left", left);
        pagination.set("pageMargins", pageMargins);
        configs.put(ConfigType.pagination, pagination);
    }

    public void setPaginationA4() {
        this.setPagination("21cm", "29.7cm", "20mm", "12mm", "20mm", "12mm");
    }

    public void setLicenseKey(String license) {
        if (license != null && !license.trim().isEmpty()) {
            configs.put(ConfigType.licenseKey, mapper.getNodeFactory().stringNode(license));
        }
    }

}
