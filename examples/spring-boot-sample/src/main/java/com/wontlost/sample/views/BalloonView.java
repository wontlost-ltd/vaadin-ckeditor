package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

@Route("balloon")
public class BalloonView extends VerticalLayout {

    public BalloonView() {
        add(new H2("Balloon"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(CKEditorType.BALLOON)
                .withValue("<p>Hello from Balloon</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
