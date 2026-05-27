package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.handler.UploadHandler;
import com.wontlost.sample.upload.StubUploadHandler;

@Route("upload")
public class UploadView extends VerticalLayout {

    public UploadView() {
        add(new H2("Upload"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withType(CKEditorType.CLASSIC)
                .withValue("<p>Upload an image via the toolbar to verify the upload adapter.</p>")
                .withUploadHandler(new StubUploadHandler())
                .withUploadConfig(new UploadHandler.UploadConfig()
                        .setMaxFileSize(500_000)
                        .setAllowedMimeTypes("image/png", "image/jpeg"))
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
