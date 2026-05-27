package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

@Route("decoupled")
public class DecoupledView extends VerticalLayout {

    public DecoupledView() {
        add(new H2("Decoupled"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(CKEditorType.DECOUPLED)
                .withValue("<p>Hello from Decoupled</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
