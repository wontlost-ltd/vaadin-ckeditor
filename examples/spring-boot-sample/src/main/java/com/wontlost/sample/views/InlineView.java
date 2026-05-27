package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

@Route("inline")
public class InlineView extends VerticalLayout {

    public InlineView() {
        add(new H2("Inline"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(CKEditorType.INLINE)
                .withValue("<p>Hello from Inline</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
