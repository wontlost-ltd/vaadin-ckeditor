package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * 全屏编辑区宽度夹具（issue #156）。CKEditor 全屏模式把编辑区固定为 A4 纸宽（795.701px），
 * add-on 开放 {@code --ck-fullscreen-editable-width} 调整。
 *
 * <p>{@code ?width=<css 长度>} 在 {@code <html>} 上设置该变量（等同应用在主题 CSS 里设置），
 * 缺省不设置，保持 CKEditor 默认宽度。</p>
 */
@Route("fullscreen")
public class FullscreenView extends VerticalLayout implements BeforeEnterObserver {

    public FullscreenView() {
        add(new H2("Fullscreen"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .addPlugin(CKEditorPlugin.FULLSCREEN)
                .addPlugin(CKEditorPlugin.SOURCE_EDITING)
                .withToolbar("heading", "|", "bold", "italic", "|", "fullscreen", "sourceEditing")
                .withValue("<p>Test text</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String width = event.getLocation().getQueryParameters().getParameters()
                .getOrDefault("width", java.util.List.of()).stream().findFirst().orElse(null);
        // 夹具只接受「数字 + px / vw / %」，避免非法值被浏览器丢弃后悄悄回退到默认宽度
        if (width != null && !width.matches("\\d+(\\.\\d+)?(px|vw|%)")) {
            throw new IllegalArgumentException("Unsupported width for the fixture: " + width);
        }
        event.getUI().getPage().executeJs(width == null
                ? "document.documentElement.style.removeProperty('--ck-fullscreen-editable-width');"
                : "document.documentElement.style.setProperty('--ck-fullscreen-editable-width', $0);", width);
    }
}
