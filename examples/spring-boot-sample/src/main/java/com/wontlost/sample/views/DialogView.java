package com.wontlost.sample.views;

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPlugin;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Dialog 夹具（issue #143 / #144 / #145 的 Playwright 回归）：编辑器嵌入 Vaadin Dialog，
 * 插件与工具栏沿用报告者配置——SPECIAL_CHARACTERS 不带 ESSENTIALS、FULLSCREEN、STYLE。
 * 查询参数 ?dark 强制 Lumo 暗色主题；?modeless 使用 MODELESS 模态（默认 VISUAL）；
 * ?two 在同一 Dialog 内再放一个编辑器（id ckeditor-2），验证两个宿主各自接管浮层容器。
 *
 * <p>Dialog 页脚的「Reattach」按钮把第一个编辑器在两个槽位之间移动：Flow 的 {@code add()}
 * 在同一次往返里先 remove 再 add，前端走 disconnectedCallback → connectedCallback →
 * 重建编辑器，用来验证重连后浮层容器被重新接管且旧容器不残留。{@code #move-count}
 * 把服务端记录的移动次数投影到 DOM 作为断言探针。
 */
@Route("dialog")
public class DialogView extends VerticalLayout implements BeforeEnterObserver {

    private boolean modeless;
    private boolean two;

    public DialogView() {
        add(new H2("Dialog"));
        Button open = new Button("Open dialog", e -> openDialog());
        open.setId("open-dialog");
        add(open);
    }

    private void openDialog() {
        CKEditorConfig config = new CKEditorConfig();
        config.setHeading(CKEditorConfig.HeadingOption.heading(0, "Normálny", ""),
                CKEditorConfig.HeadingOption.heading(1, "Nadpis 1", ""),
                CKEditorConfig.HeadingOption.heading(2, "Nadpis 2", ""));
        config.setStyle(CKEditorConfig.StyleDefinition.block("H1 článok", "h1", "style1"),
                CKEditorConfig.StyleDefinition.block("H2 paragraf", "h2", "style2"),
                CKEditorConfig.StyleDefinition.inline("Zmena 1", "zmena1"),
                CKEditorConfig.StyleDefinition.inline("Zmena 2", "zmena2"));

        VaadinCKEditor editor = newEditor(config, "ckeditor", "<p>Hello from dialog</p>");

        // 两个槽位：Reattach 按钮把第一个编辑器在其间移动，触发前端断开 / 重连。
        Div slotA = new Div(editor);
        slotA.setId("slot-a");
        Div slotB = new Div();
        slotB.setId("slot-b");

        Div wrapper = new Div(slotA, slotB);
        wrapper.setSizeFull();
        if (two) {
            wrapper.add(newEditor(config, "ckeditor-2", "<p>Second editor</p>"));
        }

        Span moveCount = new Span("0");
        moveCount.setId("move-count");
        final int[] moves = {0};
        Button reattach = new Button("Reattach", e -> {
            boolean inA = editor.getParent().orElse(null) == slotA;
            (inA ? slotB : slotA).add(editor);
            moves[0]++;
            moveCount.setText(String.valueOf(moves[0]));
        });
        reattach.setId("reattach");

        Dialog dialog = new Dialog(wrapper);
        dialog.setHeaderTitle("Nový záznam");
        dialog.getFooter().add(reattach, moveCount);
        dialog.setWidth("700px");
        dialog.setHeight("600px");
        dialog.setModality(modeless ? ModalityMode.MODELESS : ModalityMode.VISUAL);
        dialog.open();
    }

    private static VaadinCKEditor newEditor(CKEditorConfig config, String id, String value) {
        VaadinCKEditor editor = VaadinCKEditor.create()
                .withToolbar("undo", "redo", "|", "specialCharacters", "|", "fullscreen", "|",
                        "sourceEditing", "-", "bold", "italic", "|", "style", "heading")
                .withPlugins(CKEditorPlugin.ESSENTIALS, CKEditorPlugin.PARAGRAPH, CKEditorPlugin.UNDO,
                        CKEditorPlugin.BOLD, CKEditorPlugin.ITALIC, CKEditorPlugin.HEADING,
                        CKEditorPlugin.SPECIAL_CHARACTERS, CKEditorPlugin.SOURCE_EDITING,
                        CKEditorPlugin.STYLE, CKEditorPlugin.FULLSCREEN)
                .withConfig(config)
                .withValue(value)
                .withHeight("200px")
                .build();
        editor.setId(id);
        return editor;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var params = event.getLocation().getQueryParameters().getParameters();
        modeless = params.containsKey("modeless");
        two = params.containsKey("two");
        if (params.containsKey("dark")) {
            event.getUI().getPage().executeJs("document.documentElement.setAttribute('theme', 'dark');");
        }
    }
}
