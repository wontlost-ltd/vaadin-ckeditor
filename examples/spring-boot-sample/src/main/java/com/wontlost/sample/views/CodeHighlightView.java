package com.wontlost.sample.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorTheme;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.CodeHighlightTheme;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

import java.util.List;
import java.util.Map;

/**
 * 代码块语法高亮夹具，路由 /code-highlight。
 *
 * <ul>
 *   <li>{@code ?theme=auto|github|github-dark|atom-one-light|atom-one-dark|none}：配色，缺省 auto</li>
 *   <li>{@code ?dark=1}：编辑器暗色（{@link CKEditorTheme#DARK}）</li>
 *   <li>{@code ?lines=N}：再追加一个 N 行的 JavaScript 代码块，供性能测量</li>
 *   <li>{@code ?off=1}：初始不启用高亮（验证运行期开启）</li>
 * </ul>
 * {@code #server-value} 投影服务端 {@code getValue()}，用于断言保存的 HTML 不含高亮 span。
 */
@Route("code-highlight")
public class CodeHighlightView extends VerticalLayout implements BeforeEnterObserver {

    static final String SEED = "<p>Code below.</p>"
            + "<pre><code class=\"language-javascript\">const answer = 42;\n// a comment\nreturn \"text\";</code></pre>"
            + "<pre><code class=\"language-python\">def greet(name):\n    return f\"hi {name}\"</code></pre>"
            + "<pre><code class=\"language-plaintext\">import os\nprint(os.name)</code></pre>";

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        CodeHighlightTheme theme = CodeHighlightTheme.fromJsName(first(params, "theme", "auto"));
        boolean dark = "1".equals(first(params, "dark", "0"));
        boolean off = "1".equals(first(params, "off", "0"));
        int lines = Integer.parseInt(first(params, "lines", "0"));

        StringBuilder value = new StringBuilder(SEED);
        if (lines > 0) {
            value.append("<pre><code class=\"language-javascript\">");
            for (int i = 0; i < lines; i++) {
                if (i > 0) {
                    value.append('\n');
                }
                value.append("function f").append(i).append("(a, b) { return a + b * 2; } // line ").append(i);
            }
            value.append("</code></pre>");
        }

        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
                .withPreset(CKEditorPreset.FULL)
                .withType(CKEditorType.CLASSIC)
                .withTheme(dark ? CKEditorTheme.DARK : CKEditorTheme.LIGHT)
                .withValue(value.toString());
        if (!off) {
            builder.withCodeBlockHighlighting(theme);
        }
        VaadinCKEditor editor = builder.build();
        editor.setId("ckeditor");

        Pre serverValue = new Pre(editor.getValue());
        serverValue.setId("server-value");
        Span changeCount = new Span("0");
        changeCount.setId("change-count");
        int[] changes = {0};
        editor.addValueChangeListener(e -> {
            serverValue.setText(editor.getValue());
            changeCount.setText(String.valueOf(++changes[0]));
        });

        Button toggle = new Button("Toggle highlighting",
                e -> editor.setCodeBlockHighlighting(!editor.isCodeBlockHighlighting()));
        toggle.setId("btn-toggle");
        Button atomDark = new Button("Atom One Dark",
                e -> editor.setCodeBlockHighlightTheme(CodeHighlightTheme.ATOM_ONE_DARK));
        atomDark.setId("btn-atom-dark");

        add(new H2("Code block highlighting"), editor, new HorizontalLayout(toggle, atomDark),
                changeCount, serverValue);
    }

    private static String first(Map<String, List<String>> params, String key, String fallback) {
        List<String> values = params.get(key);
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }
}
