package com.wontlost.sample.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * 性能测试夹具，路由 /perf（issue #137）。
 *
 * <p>场景由查询参数控制，便于 Playwright 用同一视图覆盖多组参数：
 * <ul>
 *   <li>{@code count}：同页编辑器实例数（默认 1）</li>
 *   <li>{@code type}：classic / balloon / inline / decoupled（默认 classic）</li>
 *   <li>{@code preset}：{@link CKEditorPreset} 名称（默认 BASIC）</li>
 *   <li>{@code minimap} / {@code simple}：为 decoupled 编辑器开启 minimap 及其简化预览</li>
 *   <li>{@code docKb}：「下发大文档」按钮生成的 HTML 体积（KB，默认 1024）</li>
 *   <li>{@code contentEvents}：为每个实例注册 ContentChange 监听器，并把最近一次事件投影到探针</li>
 * </ul>
 *
 * <p>页面上的 {@code <span>} 是断言探针：把服务端观察到的状态投影到 DOM，
 * 让测试不必依赖组件内部实现就能读取初始化耗时、服务端收到的值长度等数据。</p>
 */
@Route("perf")
public class PerfView extends VerticalLayout implements BeforeEnterObserver {

    /** 大文档末尾的标记，测试据此判断整份文档已到达对端。 */
    static final String END_MARKER = "PERF-END-MARKER";

    private final VerticalLayout container = new VerticalLayout();
    private final List<VaadinCKEditor> editors = new ArrayList<>();
    private final List<Long> initTimes = new ArrayList<>();

    private final Span readyCount = probe("ready-count", "0");
    private final Span initTimesProbe = probe("init-times", "");
    private final Span mounted = probe("mounted", "true");
    private final Span serverValueLength = probe("server-value-length", "0");
    private final Span serverValueHasMarker = probe("server-value-has-marker", "false");
    private final Span changeCount = probe("change-count", "0");
    private final Span contentEventCount = probe("content-events", "0");
    private final Span contentOldLength = probe("content-old-length", "-1");
    private final Span contentNewLength = probe("content-new-length", "-1");
    private final Span contentSource = probe("content-source", "-");

    private int count = 1;
    private CKEditorType type = CKEditorType.CLASSIC;
    private CKEditorPreset preset = CKEditorPreset.BASIC;
    private boolean minimap;
    private boolean simplePreview;
    private int docKb = 1024;
    private boolean contentEvents;
    private int changes;
    private int contentChanges;

    public PerfView() {
        add(new H2("Perf"));
        container.setId("editors");
        container.setPadding(false);

        Button setLarge = new Button("Set large document", e -> {
            String html = largeDocument(docKb * 1024);
            editors.forEach(editor -> editor.setValue(html));
        });
        setLarge.setId("btn-set-large");

        // 卸载 / 重新挂载**同一批**实例：对应 Vaadin 中组件被移出又放回布局的场景。
        Button toggleAttach = new Button("Toggle attach", e -> {
            if (container.getComponentCount() > 0) {
                container.removeAll();
                mounted.setText("false");
            } else {
                editors.forEach(container::add);
                mounted.setText("true");
            }
        });
        toggleAttach.setId("btn-toggle-attach");

        // 丢弃旧实例并新建：对应在视图间导航、每次创建新编辑器的场景。
        Button remountNew = new Button("Remount new", e -> rebuildEditors());
        remountNew.setId("btn-remount-new");

        add(new HorizontalLayout(setLarge, toggleAttach, remountNew),
                new HorizontalLayout(readyCount, initTimesProbe, mounted,
                        serverValueLength, serverValueHasMarker, changeCount),
                new HorizontalLayout(contentEventCount, contentOldLength, contentNewLength, contentSource),
                container);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters params = event.getLocation().getQueryParameters();
        count = intParam(params, "count", 1);
        type = CKEditorType.valueOf(stringParam(params, "type", "classic").toUpperCase(Locale.ROOT));
        preset = CKEditorPreset.valueOf(stringParam(params, "preset", "BASIC").toUpperCase(Locale.ROOT));
        minimap = Boolean.parseBoolean(stringParam(params, "minimap", "false"));
        simplePreview = Boolean.parseBoolean(stringParam(params, "simple", "false"));
        docKb = intParam(params, "docKb", 1024);
        contentEvents = Boolean.parseBoolean(stringParam(params, "contentEvents", "false"));
        rebuildEditors();
    }

    private void rebuildEditors() {
        container.removeAll();
        editors.clear();
        initTimes.clear();
        readyCount.setText("0");
        initTimesProbe.setText("");
        for (int i = 0; i < count; i++) {
            VaadinCKEditor editor = createEditor(i);
            editors.add(editor);
            container.add(editor);
        }
        mounted.setText("true");
    }

    private VaadinCKEditor createEditor(int index) {
        // minimap 只调用 setMinimapEnabled(true)，不手动加 MINIMAP 插件：
        // 与 README 的用法一致，由连接器负责补上插件（issue #137）。
        VaadinCKEditor editor = VaadinCKEditor.create()
                .withPreset(preset)
                .withType(type)
                .withValue("<p>Perf editor " + index + "</p>")
                .build();
        editor.setId("ed-" + index);
        if (minimap) {
            editor.setMinimapEnabled(true);
            editor.setMinimapSimplePreview(simplePreview);
        }
        editor.addEditorReadyListener(e -> {
            initTimes.add(e.getInitializationTimeMs());
            readyCount.setText(String.valueOf(initTimes.size()));
            initTimesProbe.setText(initTimes.stream().map(String::valueOf)
                    .reduce((a, b) -> a + "," + b).orElse(""));
        });
        if (contentEvents) {
            editor.addContentChangeListener(e -> {
                contentChanges++;
                contentEventCount.setText(String.valueOf(contentChanges));
                contentOldLength.setText(String.valueOf(e.getOldContent().length()));
                contentNewLength.setText(String.valueOf(e.getNewContent().length()));
                contentSource.setText(e.getChangeSource().name());
            });
        }
        editor.addValueChangeListener(e -> {
            if (!e.isFromClient()) {
                return;
            }
            String value = editor.getValue();
            changes++;
            changeCount.setText(String.valueOf(changes));
            serverValueLength.setText(String.valueOf(value.length()));
            serverValueHasMarker.setText(String.valueOf(value.contains(END_MARKER)));
        });
        return editor;
    }

    /**
     * 生成接近目标体积的 HTML：标题、段落、列表与行内样式交替出现，
     * 让 CKEditor 的模型转换走到常见的分支；末尾附带 {@link #END_MARKER}。
     */
    static String largeDocument(int targetBytes) {
        StringBuilder html = new StringBuilder(targetBytes + 256);
        int section = 0;
        while (html.length() < targetBytes) {
            html.append("<h2>Section ").append(section).append("</h2>")
                    .append("<p>Lorem ipsum dolor sit amet, <strong>consectetur</strong> adipiscing elit, ")
                    .append("sed do <em>eiusmod</em> tempor incididunt ut labore et dolore magna aliqua. ")
                    .append("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris.</p>")
                    .append("<ul><li>Item ").append(section).append(".1</li><li>Item ")
                    .append(section).append(".2</li></ul>");
            section++;
        }
        return html.append("<p>").append(END_MARKER).append("</p>").toString();
    }

    private static Span probe(String id, String initial) {
        Span span = new Span(initial);
        span.setId(id);
        return span;
    }

    private static int intParam(QueryParameters params, String name, int fallback) {
        return params.getSingleParameter(name).map(Integer::parseInt).orElse(fallback);
    }

    private static String stringParam(QueryParameters params, String name, String fallback) {
        return params.getSingleParameter(name).orElse(fallback);
    }
}
