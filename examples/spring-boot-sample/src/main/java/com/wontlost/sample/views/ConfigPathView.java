package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;

import java.util.List;
import java.util.Map;

/**
 * {@code CKEditorConfig.set(path, value)} 夹具：只用点号路径与类型化重载配置工具栏、标题、链接与 UI 偏移，
 * 其中 {@code toolbar} 由两次调用合并而成、{@code ui.viewportOffset} 合并进 {@code setUiViewportOffset} 建立的对象。
 */
@Route("config-path")
public class ConfigPathView extends VerticalLayout {

    public ConfigPathView() {
        add(new H2("CKEditorConfig.set(path, value)"));

        CKEditorConfig config = new CKEditorConfig()
                .set("toolbar.items", List.of("heading", "|", "bold", "italic", "link"))
                .set("toolbar.shouldNotGroupWhenFull", true)
                .set("heading.options", List.of(
                        Map.of("model", "paragraph", "title", "Body", "class", "ck-heading_paragraph"),
                        Map.of("model", "heading2", "view", "h2", "title", "Section", "class", "ck-heading_heading2")))
                .set("link", Map.of("defaultProtocol", "https://", "addTargetToExternalLinks", true))
                .setUiViewportOffset(0, 0, 0, 0)
                .set("ui.viewportOffset.top", 12);

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withConfig(config)
                .withValue("<p>Configured by path</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
