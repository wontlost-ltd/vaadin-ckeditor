package com.wontlost.sample.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorConfig;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

import java.util.List;
import java.util.Locale;

/**
 * 界面语言夹具（issue #151）：三种配置入口分别由查询参数驱动，便于 Playwright 断言工具栏
 * 提示文字已被翻译。
 *
 * <ul>
 *   <li>{@code ?lang=sk} —— {@code withLanguage(String)}</li>
 *   <li>{@code ?locale=sk-SK} —— {@code withLocale(Locale)}（Vaadin 的 {@code UI#getLocale()} 场景）</li>
 *   <li>{@code ?ui=sk&content=sk} —— {@code CKEditorConfig#setLanguage(ui, content, textParts)} 经 {@code withConfig}</li>
 * </ul>
 */
@Route("i18n")
public class I18nView extends VerticalLayout implements BeforeEnterObserver {

    private VaadinCKEditor editor;

    public I18nView() {
        add(new H2("I18n"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters params = event.getLocation().getQueryParameters();
        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withValue("<p>Ahoj svet</p>");

        first(params, "lang").ifPresent(builder::withLanguage);
        first(params, "locale").ifPresent(tag -> builder.withLocale(Locale.forLanguageTag(tag)));
        first(params, "ui").ifPresent(ui -> {
            CKEditorConfig config = new CKEditorConfig();
            config.setLanguage(ui, first(params, "content").orElse(null), new String[]{"sk", "en"});
            builder.withConfig(config);
        });

        if (editor != null) {
            remove(editor);
        }
        editor = builder.build();
        editor.setId("ckeditor");
        add(editor);
    }

    private static java.util.Optional<String> first(QueryParameters params, String name) {
        List<String> values = params.getParameters().get(name);
        return values == null || values.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(values.get(0));
    }
}
