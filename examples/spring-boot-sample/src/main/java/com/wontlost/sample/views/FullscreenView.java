package com.wontlost.sample.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.FullscreenPage;
import com.wontlost.ckeditor.VaadinCKEditor;

import java.util.List;
import java.util.Map;

/**
 * 全屏页面宽度夹具（issue #156）。
 *
 * <ul>
 *   <li>{@code ?page=a4|a4-fit|full-width|custom:<css 宽度>}：第一个编辑器的 {@link FullscreenPage}，缺省为默认值</li>
 *   <li>{@code ?page2=...}：出现时添加第二个编辑器（id {@code ckeditor-2}）并使用该设置</li>
 *   <li>{@code ?width=<css 长度>}：在 {@code <html>} 上设置应用级覆盖 {@code --ck-fullscreen-editable-width}</li>
 * </ul>
 * 按钮 {@code #set-a4} 在运行期把第一个编辑器切换为 {@link FullscreenPage#A4}。
 */
@Route("fullscreen")
public class FullscreenView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        add(new H2("Fullscreen"));

        VaadinCKEditor editor = createEditor("ckeditor", first(params, "page"));
        Button setA4 = new Button("A4", e -> editor.setFullscreenPage(FullscreenPage.A4));
        setA4.setId("set-a4");
        add(setA4, editor);
        String page2 = first(params, "page2");
        if (page2 != null) {
            add(createEditor("ckeditor-2", page2));
        }

        String width = first(params, "width");
        // 夹具只接受「数字 + px / vw / %」，避免非法值被浏览器丢弃后悄悄回退
        if (width != null && !width.matches("\\d+(\\.\\d+)?(px|vw|%)")) {
            throw new IllegalArgumentException("Unsupported width for the fixture: " + width);
        }
        event.getUI().getPage().executeJs(width == null
                ? "document.documentElement.style.removeProperty('--ck-fullscreen-editable-width');"
                : "document.documentElement.style.setProperty('--ck-fullscreen-editable-width', $0);", width);
    }

    private static VaadinCKEditor createEditor(String id, String page) {
        var builder = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.STANDARD)
                .addPlugin(CKEditorPlugin.FULLSCREEN)
                .addPlugin(CKEditorPlugin.SOURCE_EDITING)
                .withToolbar("heading", "|", "bold", "italic", "|", "fullscreen", "sourceEditing")
                .withValue("<p>Test text</p>");
        if (page != null) {
            builder.withFullscreenPage(parse(page));
        }
        VaadinCKEditor editor = builder.build();
        editor.setId(id);
        return editor;
    }

    static FullscreenPage parse(String page) {
        return switch (page) {
            case "a4" -> FullscreenPage.A4;
            case "a4-fit" -> FullscreenPage.A4_FIT;
            case "full-width" -> FullscreenPage.FULL_WIDTH;
            default -> {
                if (!page.startsWith("custom:")) {
                    throw new IllegalArgumentException("Unknown page: " + page);
                }
                yield FullscreenPage.custom(page.substring("custom:".length()));
            }
        };
    }

    private static String first(Map<String, List<String>> params, String name) {
        return params.getOrDefault(name, List.of()).stream().findFirst().orElse(null);
    }
}
