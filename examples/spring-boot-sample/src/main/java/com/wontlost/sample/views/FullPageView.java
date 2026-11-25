package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.HtmlSupport;
import com.wontlost.ckeditor.HtmlSupport.Rule;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Full page HTML（{@code FullPage} 插件）夹具，路由 /full-page。
 *
 * <ul>
 *   <li>{@code #ckeditor}：{@code HtmlSupport.fullPageAllowRenderStylesFromHead(true)}，构建器据此加载 FullPage，
 *       {@code <head>} 中的样式渲染到页面；{@code #server-value} 为其服务端值；</li>
 *   <li>{@code #ck-explicit}：{@code withPlugins(CKEditorPlugin.FULL_PAGE)}，不设该选项：保留 {@code <head>}，但不渲染其中的样式。</li>
 * </ul>
 */
@Route("full-page")
public class FullPageView extends VerticalLayout {

    static final String CONTENT = "<html><head><title>Doc title</title>"
            + "<style>p.fp { color: rgb(1, 2, 3); }</style></head>"
            + "<body><p class=\"fp\">Body</p></body></html>";

    static final String CONTENT_EXPLICIT = "<html><head><title>Other</title>"
            + "<style>p.fp2 { color: rgb(4, 5, 6); }</style></head>"
            + "<body><p class=\"fp2\">Second</p></body></html>";

    public FullPageView() {
        add(new H2("Full page HTML"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withConfig(new CKEditorConfig().setHtmlSupport(HtmlSupport.create()
                        .allow(Rule.element("p").classes("fp"))
                        .fullPageAllowRenderStylesFromHead(true)))
                .withValue(CONTENT)
                .build();
        editor.setId("ckeditor");

        Span serverValue = new Span(editor.getValue());
        serverValue.setId("server-value");
        editor.addValueChangeListener(e -> serverValue.setText(e.getValue()));

        VaadinCKEditor explicit = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withPlugins(CKEditorPlugin.FULL_PAGE)
                .withConfig(new CKEditorConfig().setHtmlSupport(HtmlSupport.create()
                        .allow(Rule.element("p").classes("fp2"))))
                .withValue(CONTENT_EXPLICIT)
                .build();
        explicit.setId("ck-explicit");

        add(editor, serverValue, explicit);
    }
}
