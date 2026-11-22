package com.wontlost.sample.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.internal.EditorTextStats;

/**
 * 实时字数与上限夹具（#70），路由 /word-count。
 *
 * <p>编辑器限 5 词 / 40 字符并显示 CKEditor 计数，绑定到 Binder。探针：</p>
 * <ul>
 *   <li>{@code #count}：WordCountChangeEvent 的最新计数，{@code #events}：事件次数；</li>
 *   <li>{@code #server-stats}：服务端校验所用的计数（仅夹具使用内部类，用于与浏览器中插件的计数比对）；</li>
 *   <li>{@code #binder-status}：点击 {@code #btn-validate} 后 Binder 的校验结果。</li>
 * </ul>
 */
@Route("word-count")
public class WordCountView extends VerticalLayout {

    /** Binder 绑定的 bean */
    public static class Post {
        private String body = "";
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }

    public WordCountView() {
        add(new H2("Word count"));

        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.BASIC)
                .withMaxWords(5)
                .withMaxCharacters(40)
                .withWordCountVisible(true)
                .build();
        editor.setId("ckeditor");
        editor.setLabel("Post");

        Span count = new Span("-");
        count.setId("count");
        Span events = new Span("0");
        events.setId("events");
        final int[] eventCount = {0};
        editor.addWordCountChangeListener(e -> {
            eventCount[0]++;
            events.setText(String.valueOf(eventCount[0]));
            count.setText("words=" + e.getWords() + " chars=" + e.getCharacters());
        });

        Span serverStats = new Span("-");
        serverStats.setId("server-stats");
        editor.addValueChangeListener(e -> {
            EditorTextStats.Stats stats = EditorTextStats.of(e.getValue());
            serverStats.setText("words=" + stats.words() + " chars=" + stats.characters());
        });

        Binder<Post> binder = new Binder<>(Post.class);
        binder.forField(editor).bind(Post::getBody, Post::setBody);
        binder.setBean(new Post());

        Span binderStatus = new Span("-");
        binderStatus.setId("binder-status");
        Button validate = new Button("Validate", e -> binderStatus.setText(binder.validate().isOk()
                ? "valid"
                : binder.validate().getFieldValidationErrors().get(0).getMessage().orElse("invalid")));
        validate.setId("btn-validate");

        add(editor, count, events, serverStats, validate, binderStatus);
    }
}
