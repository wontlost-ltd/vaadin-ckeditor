package com.wontlost.ckeditor;

import com.wontlost.ckeditor.handler.HtmlSanitizer;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link HtmlSupport}：GHS 配置结构与 CKEditor {@code GeneralHtmlSupportConfig} / {@code MatcherObjectPattern} 一致、
 * 安全默认 disallow、显式放开、正则序列化、{@code setHtmlSupport(boolean)} 兼容、构建器自动加载插件、
 * 与服务端 HtmlSanitizer 互不影响。
 */
class HtmlSupportTest {

    private static final String SAFE_DISALLOW =
        "[{\"name\":\"script\"},{\"name\":\"iframe\"},{\"attributes\":[{\"$regexp\":\"^on\",\"$flags\":\"i\"}]}]";

    @Test
    @DisplayName("the documented examples serialize to the GHS config shape")
    void documentedExamples() {
        ObjectNode json = HtmlSupport.create()
            .allow(HtmlSupport.Rule.elements("div", "section", "article").classes(true))
            .allow(HtmlSupport.Rule.elements("details", "summary").attributes("open"))
            .allowIframes()
            .allow(HtmlSupport.Rule.element("iframe")
                .attribute("src", Pattern.compile("^https://www\\.youtube(-nocookie)?\\.com/embed/"))
                .attributes("width", "height", "allowfullscreen"))
            .allow(HtmlSupport.Rule.element("p").classes("lead", "note").styles("color", "text-align"))
            .toJson();

        assertThat(json.get("allow").toString()).isEqualTo("["
            + "{\"name\":{\"$regexp\":\"^(div|section|article)$\",\"$flags\":\"\"},\"classes\":true},"
            + "{\"name\":{\"$regexp\":\"^(details|summary)$\",\"$flags\":\"\"},\"attributes\":[\"open\"]},"
            + "{\"name\":\"iframe\",\"attributes\":[{\"key\":\"src\",\"value\":{\"$regexp\":"
            + "\"^https://www\\\\.youtube(-nocookie)?\\\\.com/embed/\",\"$flags\":\"\"}},\"width\",\"height\",\"allowfullscreen\"]},"
            + "{\"name\":\"p\",\"classes\":[\"lead\",\"note\"],\"styles\":[\"color\",\"text-align\"]}]");
        // allowIframes 去掉 iframe 的默认 disallow，script 与 on* 仍在
        assertThat(json.get("disallow").toString())
            .isEqualTo("[{\"name\":\"script\"},{\"attributes\":[{\"$regexp\":\"^on\",\"$flags\":\"i\"}]}]");
    }

    @Test
    @DisplayName("scripts, iframes and on* attributes are disallowed unless opted in")
    void safeDefaults() {
        assertThat(HtmlSupport.create().toJson().get("disallow").toString()).isEqualTo(SAFE_DISALLOW);
        assertThat(HtmlSupport.create().allowScripts().allowIframes().allowEventHandlers().toJson()
            .get("disallow").toString()).isEqualTo("[]");
        assertThat(HtmlSupport.create().allowEventHandlers().toJson().get("disallow").toString())
            .isEqualTo("[{\"name\":\"script\"},{\"name\":\"iframe\"}]");
    }

    @Test
    @DisplayName("user disallow rules come before the defaults; name-only removes the element")
    void userDisallow() {
        JsonNode disallow = HtmlSupport.create()
            .disallow(HtmlSupport.Rule.element("font"))
            .disallow(HtmlSupport.Rule.anyElement().styles("position"))
            .toJson().get("disallow");
        assertThat(disallow.get(0).toString()).isEqualTo("{\"name\":\"font\"}");
        assertThat(disallow.get(1).toString()).isEqualTo("{\"styles\":[\"position\"]}");
        assertThat(disallow.size()).isEqualTo(5);
    }

    @Test
    @DisplayName("attribute, class and style forms: true, names, exact values, patterns")
    void propertyForms() {
        ObjectNode rule = HtmlSupport.Rule.element("a")
            .attribute(Pattern.compile("^data-"))
            .attribute("rel", "noopener")
            .classes(Pattern.compile("^btn-"))
            .style("color", Pattern.compile("^#[0-9a-f]{6}$", Pattern.CASE_INSENSITIVE))
            .style("display", "block")
            .toJson();
        assertThat(rule.toString()).isEqualTo("{\"name\":\"a\","
            + "\"attributes\":[{\"$regexp\":\"^data-\",\"$flags\":\"\"},{\"key\":\"rel\",\"value\":\"noopener\"}],"
            + "\"classes\":[{\"$regexp\":\"^btn-\",\"$flags\":\"\"}],"
            + "\"styles\":[{\"key\":\"color\",\"value\":{\"$regexp\":\"^#[0-9a-f]{6}$\",\"$flags\":\"i\"}},"
            + "{\"key\":\"display\",\"value\":\"block\"}]}");

        // true 优先于逐项列出；false 不添加任何内容
        assertThat(HtmlSupport.Rule.element("div").attributes("id").attributes(true).toJson().toString())
            .isEqualTo("{\"name\":\"div\",\"attributes\":true}");
        assertThat(HtmlSupport.Rule.element("div").classes(false).toJson().toString())
            .isEqualTo("{\"name\":\"div\"}");
    }

    @Test
    @DisplayName("other options: allowEmpty, preserveEmptyBlocksInEditingView, htmlIframeSandbox, fullPage")
    void otherOptions() {
        ObjectNode json = HtmlSupport.create()
            .allowEmpty("i", "span")
            .preserveEmptyBlocksInEditingView(true)
            .iframeSandbox("allow-scripts", "allow-same-origin")
            .fullPageAllowRenderStylesFromHead(true)
            .toJson();
        assertThat(json.get("allowEmpty").toString()).isEqualTo("[\"i\",\"span\"]");
        assertThat(json.get("preserveEmptyBlocksInEditingView").asBoolean()).isTrue();
        assertThat(json.get("htmlIframeSandbox").toString()).isEqualTo("[\"allow-scripts\",\"allow-same-origin\"]");
        assertThat(json.get("fullPage").toString()).isEqualTo("{\"allowRenderStylesFromHead\":true}");
        assertThat(HtmlSupport.create().iframeSandbox(false).toJson().get("htmlIframeSandbox").asBoolean()).isFalse();
        assertThat(HtmlSupport.create().toJson().has("allowEmpty")).isFalse();
    }

    @Test
    @DisplayName("invalid names and unsupported pattern flags are rejected")
    void validation() {
        assertThatThrownBy(() -> HtmlSupport.Rule.element("div class")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HtmlSupport.Rule.element((String) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HtmlSupport.Rule.elements()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HtmlSupport.Rule.elements("div", "x|y")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HtmlSupport.Rule.element("p").attributes(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HtmlSupport.create().allowEmpty("1x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HtmlSupport.Rule.element(Pattern.compile("x", Pattern.COMMENTS)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(HtmlSupport.Rule.elements("div").toJson().toString()).isEqualTo("{\"name\":\"div\"}");
    }

    @Test
    @DisplayName("setHtmlSupport(true) allows all except scripts, iframes and on* attributes; (false) stays empty")
    void setHtmlSupportBoolean() {
        CKEditorConfig config = new CKEditorConfig().setHtmlSupport(true);
        JsonNode json = config.toJson().get("htmlSupport");
        assertThat(json.get("allow").toString())
            .isEqualTo("[{\"attributes\":true,\"classes\":true,\"styles\":true}]");
        assertThat(json.get("disallow").toString()).isEqualTo(SAFE_DISALLOW);
        assertThat(config.isHtmlSupportAllowAll()).isTrue();

        CKEditorConfig none = new CKEditorConfig().setHtmlSupport(false);
        assertThat(none.toJson().get("htmlSupport").toString()).isEqualTo("{}");
        assertThat(none.isHtmlSupportAllowAll()).isFalse();
        assertThat(new CKEditorConfig().setHtmlSupport(HtmlSupport.create()
            .allow(HtmlSupport.Rule.element("div"))).isHtmlSupportAllowAll()).isFalse();
        assertThat(new CKEditorConfig().setHtmlSupport(HtmlSupport.create()
            .allow(HtmlSupport.Rule.element(Pattern.compile("[\\s\\S]+")).attributes(true).classes(true).styles(true)))
            .isHtmlSupportAllowAll()).isTrue();
    }

    @Test
    @DisplayName("builder loads GeneralHtmlSupport when htmlSupport has rules (except in MANUAL mode)")
    void builderAddsGhsPlugin() {
        CKEditorConfig config = new CKEditorConfig().setHtmlSupport(HtmlSupport.create()
            .allow(HtmlSupport.Rule.elements("details", "summary")));
        VaadinCKEditor editor = VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).withConfig(config).build();
        assertThat(editor.getPluginsInternal()).contains(CKEditorPlugin.GENERAL_HTML_SUPPORT);

        VaadinCKEditor plain = VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).build();
        assertThat(plain.getPluginsInternal()).doesNotContain(CKEditorPlugin.GENERAL_HTML_SUPPORT);

        VaadinCKEditor manual = VaadinCKEditor.create()
            .withPlugins(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH)
            .withDependencyMode(VaadinCKEditorBuilder.DependencyMode.MANUAL)
            .withConfig(config).build();
        assertThat(manual.getPluginsInternal()).doesNotContain(CKEditorPlugin.GENERAL_HTML_SUPPORT);
    }

    @Test
    @DisplayName("the server-side HtmlSanitizer is independent: it still strips what GHS allows unless its safelist allows it")
    void sanitizerIsIndependent() {
        String html = "<details open><summary>More</summary><p class=\"note\">Body</p></details>";
        assertThat(HtmlSanitizer.withPolicy(HtmlSanitizer.SanitizationPolicy.RELAXED).sanitize(html))
            .doesNotContain("<details").doesNotContain("class=");
        Safelist extended = Safelist.relaxed()
            .addTags("details", "summary").addAttributes("details", "open").addAttributes("p", "class");
        assertThat(HtmlSanitizer.withSafelist(extended).sanitize(html))
            .contains("<details open>").contains("<summary>More</summary>").contains("class=\"note\"");
    }

    @Test
    @DisplayName("HtmlSupport is serializable with the config")
    void serializable() throws Exception {
        HtmlSupport support = HtmlSupport.create()
            .allow(HtmlSupport.Rule.element(Pattern.compile("^x")).attribute("src", Pattern.compile("^https://")));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(support);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertThat(((HtmlSupport) in.readObject()).toJson()).isEqualTo(support.toJson());
        }
    }
}
