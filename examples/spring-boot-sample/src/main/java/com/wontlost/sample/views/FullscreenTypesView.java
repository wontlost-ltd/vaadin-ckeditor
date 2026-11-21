package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

import java.util.List;
import java.util.Map;

/**
 * 全屏按钮与编辑器类型夹具：CKEditor 的 Fullscreen 只支持 CLASSIC / DECOUPLED，
 * BALLOON / INLINE 上不应出现全屏按钮，也不应加载 Fullscreen 插件。
 *
 * <ul>
 *   <li>{@code ?type=classic|decoupled|balloon|inline}</li>
 *   <li>{@code ?via=preset}：AI_DOCUMENT 预设（自带 Fullscreen 插件与 fullscreen 工具栏项）</li>
 *   <li>{@code ?via=explicit}：STANDARD + addPlugin(FULLSCREEN) + withToolbar(..., "fullscreen")</li>
 *   <li>{@code ?via=config}：STANDARD + addPlugin(FULLSCREEN) + CKEditorConfig.setToolbar(items, false)（对象形式，
 *       优先于预设工具栏）含 "fullscreen"</li>
 *   <li>{@code ?menubar=1}：显示 CKEditor 菜单栏（config.menuBar.isVisible），默认菜单含「视图 › 全屏」；
 *       与 via=config 同用时会替换其 CKEditorConfig，只配合 preset / explicit 使用</li>
 * </ul>
 */
@Route("fullscreen-types")
public class FullscreenTypesView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        CKEditorType type = CKEditorType.valueOf(first(params, "type", "classic").toUpperCase());
        String via = first(params, "via", "explicit");

        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
                .withType(type)
                .withValue("<p>Fullscreen button test</p>");
        switch (via) {
            case "preset" -> builder.withPreset(CKEditorPreset.AI_DOCUMENT);
            case "config" -> builder.withPreset(CKEditorPreset.STANDARD)
                    .addPlugin(CKEditorPlugin.FULLSCREEN)
                    .withConfig(new CKEditorConfig().setToolbar(new String[]{"bold", "italic", "|", "fullscreen"}, false));
            default -> builder.withPreset(CKEditorPreset.STANDARD)
                    .addPlugin(CKEditorPlugin.FULLSCREEN)
                    .withToolbar("bold", "italic", "|", "fullscreen");
        }
        if ("1".equals(first(params, "menubar", "0"))) {
            CKEditorConfig config = new CKEditorConfig();
            config.set("menuBar", new tools.jackson.databind.ObjectMapper().readTree("{\"isVisible\":true}"));
            builder.withConfig(config);
        }
        VaadinCKEditor editor = builder.build();
        editor.setId("ckeditor");
        add(new H2("Fullscreen button: " + type + " / " + via), editor);
    }

    private static String first(Map<String, List<String>> params, String name, String fallback) {
        return params.getOrDefault(name, List.of()).stream().findFirst().orElse(fallback);
    }
}
