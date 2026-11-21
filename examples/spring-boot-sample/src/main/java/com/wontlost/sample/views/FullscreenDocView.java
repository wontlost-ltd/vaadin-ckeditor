package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.CustomPlugin;
import com.wontlost.ckeditor.FullscreenPage;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

import java.util.List;
import java.util.Map;

/**
 * 全屏页面宽度与侧栏夹具（issue #156）：文档编辑器（DECOUPLED）的大纲（左）与 minimap（右）。
 *
 * <ul>
 *   <li>{@code ?type=decoupled|classic|balloon|inline}，缺省 decoupled</li>
 *   <li>{@code ?minimap=1}：开启 minimap（仅 decoupled）</li>
 *   <li>{@code ?outline=1}：开启文档大纲（仅 decoupled；DocumentOutline 是 CKEditor 付费插件，
 *       sample 未安装 ckeditor5-premium-features 时插件加载失败；许可证取自环境变量 CKEDITOR_LICENSE_KEY）</li>
 *   <li>{@code ?page=a4|a4-fit|full-width|custom:<css 宽度>}：FullscreenPage，缺省为默认值</li>
 * </ul>
 * 内容含 12 个带标题的小节，足够滚动，供大纲跳转与 minimap 同步验证。
 */
@Route("fullscreen-doc")
public class FullscreenDocView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        CKEditorType type = CKEditorType.valueOf(first(params, "type", "decoupled").toUpperCase());
        boolean decoupled = type == CKEditorType.DECOUPLED;
        boolean minimap = decoupled && "1".equals(first(params, "minimap", "0"));
        boolean outline = decoupled && "1".equals(first(params, "outline", "0"));

        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            content.append("<h2>Section ").append(i).append("</h2>");
            for (int j = 1; j <= 6; j++) {
                content.append("<p>Paragraph ").append(i).append('.').append(j)
                        .append(" with enough text to wrap across the page width.</p>");
            }
        }

        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .withType(type)
                .addPlugin(CKEditorPlugin.FULLSCREEN)
                .withToolbar("heading", "|", "bold", "italic", "|", "fullscreen")
                .withValue(content.toString());
        if (outline) {
            builder.addCustomPlugin(CustomPlugin.fromPremium("DocumentOutline"));
            // 付费插件需要商业许可证；未提供时以 GPL 加载，CKEditor 会锁定编辑器（只读、内容为空）
            String licenseKey = System.getenv("CKEDITOR_LICENSE_KEY");
            if (licenseKey != null && !licenseKey.isBlank()) {
                builder.withLicenseKey(licenseKey);
            }
        }
        String page = first(params, "page", null);
        if (page != null) {
            builder.withFullscreenPage(FullscreenView.parse(page));
        }
        VaadinCKEditor editor = builder.build();
        editor.setId("ckeditor");
        editor.setMinimapEnabled(minimap);
        editor.setDocumentOutlineEnabled(outline);
        editor.setWidthFull();

        add(new H2("Fullscreen document editor"), editor);
        setSizeFull();
    }

    private static String first(Map<String, List<String>> params, String name, String fallback) {
        return params.getOrDefault(name, List.of()).stream().findFirst().orElse(fallback);
    }
}
