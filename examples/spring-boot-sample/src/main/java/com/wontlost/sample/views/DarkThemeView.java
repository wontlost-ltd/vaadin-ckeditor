package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorTheme;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Dark theme view: forces Lumo dark via document attribute so Playwright
 * can verify the v48 --ck-color-ai-* dark token injection.
 */
@Route("dark")
public class DarkThemeView extends VerticalLayout implements BeforeEnterObserver {

    public DarkThemeView() {
        add(new H2("Dark theme"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(CKEditorType.CLASSIC)
                .withTheme(CKEditorTheme.DARK)
                .withValue("<p>Hello from dark theme</p>")
                .build();
        editor.setId("ckeditor");
        add(editor);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Page page = event.getUI().getPage();
        page.executeJs("document.documentElement.setAttribute('theme', 'dark');");
    }
}
