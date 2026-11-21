package com.wontlost.sample.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.Lumo;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * 粘性工具栏夹具（issue #157）：按报告者的布局搭建——AppLayout 顶栏 + 铺满的 TabSheet，编辑器包在
 * min-height 600px 的 Div 里、高度 100%，内容很长；滚动容器是 TabSheet 在 shadow DOM 里的内容区。
 * 编辑器获得焦点后滚动时，工具栏应贴在可见区顶部，且上方不出现空白占位块。
 *
 * <p>{@code ?theme=dark} 使用 Lumo 暗色（报告者的环境），缺省为亮色。</p>
 */
@Route("sticky-toolbar")
@StyleSheet(Lumo.STYLESHEET)
public class StickyToolbarView extends AppLayout implements BeforeEnterObserver {

    public StickyToolbarView() {
        addToNavbar(new H2("Sticky toolbar"), new Button("Save"));

        StringBuilder lines = new StringBuilder();
        for (int i = 1; i <= 120; i++) {
            lines.append("<p>").append(i).append("</p>");
        }
        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withHeight("100%")
                .withValue(lines.toString())
                .build();
        editor.setId("ckeditor");

        Div wrapper = new Div(editor);
        wrapper.setWidthFull();
        wrapper.setMinHeight("600px");

        VerticalLayout tabContent = new VerticalLayout(wrapper);
        TabSheet tabs = new TabSheet();
        // TabSheet 铺满 AppLayout 内容区，滚动发生在 TabSheet 自身（shadow DOM 中的 vaadin-tabsheet-scroller）
        tabs.setSizeFull();
        tabs.add(new Tab("Info"), new Div("Info tab"));
        tabs.add(new Tab("Content"), tabContent);
        tabs.setSelectedIndex(1);
        setContent(tabs);
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
