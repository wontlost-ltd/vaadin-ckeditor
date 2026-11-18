package com.wontlost.sample.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

import java.util.Locale;
import java.util.Optional;

/**
 * UI 语言夹具（Playwright {@code ui-languages.spec.ts}）：{@code /language?lang=xx} 以指定
 * UI 语言构建 BASIC 预设的 Classic 编辑器，供测试逐语言核对工具栏提示文本与 RTL 方向。
 *
 * <p>{@code via} 选择设置语言的入口：
 * <ul>
 *   <li>缺省 —— {@code withLanguage(lang)}；</li>
 *   <li>{@code via=locale} —— {@code withLocale(Locale.forLanguageTag(lang))}（Vaadin {@code UI#getLocale()} 场景）；</li>
 *   <li>{@code via=config} —— 只通过 {@code CKEditorConfig.setLanguage(lang, content, ...)} 设置语言
 *       （不调用 {@code withLanguage}），验证 config.language 优先于组件属性的既有优先级。</li>
 * </ul>
 *
 * <p>{@code content=yy} 额外经 {@code withConfig} 设置内容语言与 Text Part Language 选项
 * {@code [yy, en]}；与缺省 / {@code via=locale} 组合时，验证显式设置的界面语言只替换 config 的 {@code ui}、
 * 保留内容语言。{@code via=config} 未给 {@code content} 时内容语言为 {@code en}。
 *
 * <p>Builder 拒绝该语言代码时不抛出到路由层，而是渲染 {@code #language-rejected}（内容为异常消息），
 * 让测试把"被拒绝"与"渲染失败"区分开来。
 */
@Route("language")
public class LanguageView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        var params = event.getLocation().getQueryParameters();
        String lang = params.getSingleParameter("lang").orElse("en");
        String via = params.getSingleParameter("via").orElse("");
        Optional<String> content = params.getSingleParameter("content");
        add(new H2("Language: " + lang));
        try {
            var builder = VaadinCKEditor.create()
                    .withPreset(CKEditorPreset.BASIC)
                    .withType(CKEditorType.CLASSIC)
                    .withValue("<p>Hello from " + lang + "</p>");
            if ("config".equals(via)) {
                builder.withConfig(languageConfig(lang, content.orElse("en"), content.isPresent()));
            } else {
                content.ifPresent(c -> builder.withConfig(languageConfig("en", c, true)));
                if ("locale".equals(via)) {
                    builder.withLocale(Locale.forLanguageTag(lang));
                } else {
                    builder.withLanguage(lang);
                }
            }
            VaadinCKEditor editor = builder.build();
            editor.setId("ckeditor");
            add(editor);
        } catch (IllegalArgumentException e) {
            Div rejected = new Div(e.getMessage());
            rejected.setId("language-rejected");
            add(rejected);
        }
    }

    private static CKEditorConfig languageConfig(String ui, String content, boolean withTextParts) {
        String[] textParts = withTextParts ? new String[]{content, "en"} : null;
        return new CKEditorConfig().setLanguage(ui, content, textParts);
    }
}
