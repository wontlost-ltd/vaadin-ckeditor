package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.HtmlSupport;
import com.wontlost.ckeditor.HtmlSupport.Rule;
import com.wontlost.ckeditor.VaadinCKEditor;

import java.util.regex.Pattern;

/**
 * General HTML Support 夹具，路由 /html-support。
 *
 * <ul>
 *   <li>{@code #ckeditor}：{@link HtmlSupport} 规则（div / section / article 带类名、details / summary、
 *       仅 YouTube 的 iframe、段落类名与样式），STANDARD 预设不含 GHS，由构建器自动加载；</li>
 *   <li>{@code #ckeditor-all}：{@code setHtmlSupport(true)}（允许全部，脚本、iframe、on* 除外）。</li>
 * </ul>
 * {@code #server-value} 为第一个编辑器的服务端值。
 */
@Route("html-support")
public class HtmlSupportView extends VerticalLayout {

    static final String CONTENT = "<section class=\"intro\"><p class=\"lead\" style=\"color:#336699;\">Lead</p></section>"
            + "<details open=\"\"><summary>More</summary><p>Hidden text</p></details>"
            + "<iframe src=\"https://www.youtube.com/embed/abc\" width=\"560\" height=\"315\"></iframe>"
            + "<iframe src=\"https://evil.example/x\"></iframe>"
            + "<div class=\"box\" onclick=\"alert(1)\">Box</div>"
            + "<script>window.ghsScriptRan = true;</script>";

    public HtmlSupportView() {
        add(new H2("General HTML Support"));

        CKEditorConfig rules = new CKEditorConfig().setHtmlSupport(HtmlSupport.create()
                .allow(Rule.elements("div", "section", "article").classes(true))
                .allow(Rule.elements("details", "summary").attributes("open"))
                .allowIframes()
                .allow(Rule.element("iframe")
                        .attribute("src", Pattern.compile("^https://www\\.youtube(-nocookie)?\\.com/embed/"))
                        .attributes("width", "height", "allowfullscreen"))
                .allow(Rule.element("p").classes("lead", "note").styles("color", "text-align")));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withConfig(rules)
                .withValue(CONTENT)
                .build();
        editor.setId("ckeditor");

        Span serverValue = new Span(editor.getValue());
        serverValue.setId("server-value");
        editor.addValueChangeListener(e -> serverValue.setText(e.getValue()));

        VaadinCKEditor allowAll = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withConfig(new CKEditorConfig().setHtmlSupport(true))
                .withValue("<div class=\"x\" data-foo=\"1\" onmouseover=\"alert(1)\">All</div>"
                        + "<iframe src=\"https://www.youtube.com/embed/abc\"></iframe><script>alert(1)</script>")
                .build();
        allowAll.setId("ckeditor-all");

        add(editor, serverValue, allowAll);
    }
}
