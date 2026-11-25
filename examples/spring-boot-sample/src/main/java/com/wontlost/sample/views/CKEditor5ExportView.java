package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CustomPlugin;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * 按名称加载 {@code ckeditor5} 包的任意导出，路由 /ckeditor5-export。
 * {@code Font}（FontFamily、FontSize、FontColor、FontBackgroundColor 的组合插件）不是 {@code CKEditorPlugin} 常量，
 * 也不在连接器的静态插件表中，用来验证生产构建里两种写法都能加载：
 *
 * <ul>
 *   <li>{@code #ck-from}：{@code CustomPlugin.fromCKEditor5("Font")}；</li>
 *   <li>{@code #ck-of}：{@code CustomPlugin.of("Font", "ckeditor5")}；</li>
 *   <li>{@code #ck-missing}：{@code fromCKEditor5("NoSuchPlugin")}，编辑器照常创建、不加载该名称。</li>
 * </ul>
 */
@Route("ckeditor5-export")
public class CKEditor5ExportView extends VerticalLayout {

    public CKEditor5ExportView() {
        add(new H2("Any ckeditor5 export by name"));
        add(editor("ck-from", CustomPlugin.fromCKEditor5("Font")),
            editor("ck-of", CustomPlugin.of("Font", "ckeditor5")),
            editor("ck-missing", CustomPlugin.fromCKEditor5("NoSuchPlugin")));
    }

    private static VaadinCKEditor editor(String id, CustomPlugin plugin) {
        VaadinCKEditor editor = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .addCustomPlugin(plugin)
            .withToolbar("fontSize", "fontColor", "|", "bold", "italic")
            .withValue("<p>Text</p>")
            .build();
        editor.setId(id);
        return editor;
    }
}
