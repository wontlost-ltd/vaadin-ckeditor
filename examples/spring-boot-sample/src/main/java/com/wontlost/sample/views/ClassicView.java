package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Classic editor smoke fixture for Playwright. Available at /classic.
 */
@Route("classic")
public class ClassicView extends VerticalLayout {

    public ClassicView() {
        add(new H2("Classic"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(CKEditorType.CLASSIC)
                .withValue("<p>Hello from Classic</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
