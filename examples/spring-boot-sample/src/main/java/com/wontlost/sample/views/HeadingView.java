package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorConfig.HeadingOption;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * 标题选项夹具（issues #158 / #159）：报告者的标题配置，外加同级别的带类名变体。
 * 「H1 článok」与普通「Nadpis 1」同为 h1，靠独立 model + view 类名区分；「H4 článok」只给 h4 加类名。
 * 初始内容同时包含带类名与不带类名的标题，用于验证读入后类名不丢失。
 */
@Route("heading")
public class HeadingView extends VerticalLayout {

    static final String INITIAL_VALUE = "<h1 class=\"style1\">Article</h1><h1>Plain</h1>"
            + "<h4 class=\"style4\">Section</h4><p>Body</p>";

    public HeadingView() {
        add(new H2("Heading options"));

        CKEditorConfig config = new CKEditorConfig().setHeading(
                HeadingOption.paragraph("Normálny", "ck-heading_paragraph"),
                HeadingOption.heading(1, "Nadpis 1", "ck-heading_heading1"),
                HeadingOption.heading(2, "Nadpis 2", "ck-heading_heading2"),
                HeadingOption.heading(3, "Nadpis 3", "ck-heading_heading3"),
                HeadingOption.heading(1, "H1 článok", "")
                        .withModel("heading1Article")
                        .withViewClasses("style1"),
                HeadingOption.heading(4, "H4 článok", "")
                        .withViewClasses("style4"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withToolbar("heading", "|", "bold", "italic", "|", "undo", "redo")
                .withConfig(config)
                .withValue(INITIAL_VALUE)
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
