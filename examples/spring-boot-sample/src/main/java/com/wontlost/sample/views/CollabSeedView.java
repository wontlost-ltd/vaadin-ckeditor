package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.JsonUtil;
import com.wontlost.ckeditor.VaadinCKEditor;
import tools.jackson.databind.node.ObjectNode;

/**
 * Collaboration seed view. Wires {@code cloudServices.tokenUrl} +
 * {@code collaboration.channelId} + {@code initialData} so Playwright can
 * verify {@link com.wontlost.ckeditor.VaadinCKEditor} hands off the seed
 * exactly once via the localStorage check in
 * {@code stripInitialDataIfChannelSeeded}.
 *
 * <p>The tokenUrl points to a non-existent endpoint — the editor will fail
 * to actually open a real channel, but the strip logic runs before that
 * (it inspects the config object only), so Playwright can read the
 * localStorage seed key to verify behavior.</p>
 */
@Route("collab-seed")
public class CollabSeedView extends VerticalLayout {

    public CollabSeedView() {
        add(new H2("Collab seed (initialData stripping)"));

        ObjectNode cloudServices = JsonUtil.createObjectNode();
        cloudServices.put("tokenUrl", "/stub-token-not-real");

        ObjectNode collaboration = JsonUtil.createObjectNode();
        collaboration.put("channelId", "playwright-channel");

        CKEditorConfig config = new CKEditorConfig();
        config.set("cloudServices", cloudServices);
        config.set("collaboration", collaboration);
        config.set("initialData", JsonUtil.valueToTree("<p>Seed payload (should disappear on reload).</p>"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withType(CKEditorType.CLASSIC)
                .withConfig(config)
                .build();
        editor.setId("ckeditor");
        add(editor);
    }
}
