package com.wontlost.sample.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * UI 语言夹具（Playwright {@code ui-languages.spec.ts}）：{@code /language?lang=xx} 以指定
 * UI 语言构建 BASIC 预设的 Classic 编辑器，供测试逐语言核对工具栏提示文本与 RTL 方向。
 *
 * <p>Builder 拒绝该语言代码时不抛出到路由层，而是渲染 {@code #language-rejected}（内容为异常消息），
 * 让测试把"被拒绝"与"渲染失败"区分开来。
 */
@Route("language")
public class LanguageView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        String lang = event.getLocation().getQueryParameters()
                .getSingleParameter("lang").orElse("en");
        add(new H2("Language: " + lang));
        try {
            VaadinCKEditor editor = VaadinCKEditor.create()
                    .withPreset(CKEditorPreset.BASIC)
                    .withType(CKEditorType.CLASSIC)
                    .withLanguage(lang)
                    .withValue("<p>Hello from " + lang + "</p>")
                    .build();
            editor.setId("ckeditor");
            add(editor);
        } catch (IllegalArgumentException e) {
            Div rejected = new Div(e.getMessage());
            rejected.setId("language-rejected");
            add(rejected);
        }
    }
}
