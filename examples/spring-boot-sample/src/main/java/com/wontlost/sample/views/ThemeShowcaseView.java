package com.wontlost.sample.views;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Lumo 明暗主题夹具（issue #149 / #150）：真实加载 Lumo 样式表，编辑器使用默认的 AUTO 主题跟随
 * Vaadin。工具栏包含 split button（insertImage）、bookmark 与常见下拉，内容预置一个书签与链接，
 * 用于目视与 Playwright 检查暗色下的悬停 / 激活态与书签图标对比度。
 *
 * <p>{@code ?theme=dark} 在 {@code <html>} 上设置 Lumo 暗色；缺省为亮色。</p>
 */
@Route("theme-showcase")
@StyleSheet(Lumo.STYLESHEET)
public class ThemeShowcaseView extends VerticalLayout implements BeforeEnterObserver {

    public ThemeShowcaseView() {
        add(new H2("Lumo theme"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .addPlugin(CKEditorPlugin.BOOKMARK)
                .withToolbar("heading", "|", "bold", "italic", "link", "bookmark", "|",
                        "insertImage", "insertTable", "blockQuote", "|",
                        "bulletedList", "numberedList", "|", "undo", "redo")
                .withValue("<p><a id=\"intro\"></a>Bookmark before this text and a "
                        + "<a href=\"https://vaadin.com\">link</a>.</p>"
                        + "<blockquote><p>Quoted text</p></blockquote>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean dark = event.getLocation().getQueryParameters().getParameters()
                .getOrDefault("theme", java.util.List.of()).contains("dark");
        event.getUI().getPage().executeJs(dark
                ? "document.documentElement.setAttribute('theme', 'dark');"
                : "document.documentElement.removeAttribute('theme');");
    }
}
