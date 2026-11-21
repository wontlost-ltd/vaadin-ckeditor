package com.wontlost.sample.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.CKEditorPreset;
import com.wontlost.ckeditor.CKEditorType;
import com.wontlost.ckeditor.CustomPlugin;
import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;
import com.wontlost.ckeditor.collaboration.CollaborationBridge;
import com.wontlost.ckeditor.collaboration.CollaborationPermission;
import com.wontlost.ckeditor.collaboration.CollaborationUser;
import com.wontlost.ckeditor.collaboration.CommentData;
import com.wontlost.ckeditor.collaboration.CommentThreadData;
import com.wontlost.ckeditor.collaboration.InMemoryCollaborationStore;
import com.wontlost.ckeditor.collaboration.SuggestionData;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评论 / 修订建议持久化桥夹具，路由 /collab-bridge。
 *
 * <ul>
 *   <li>{@code ?user=alice|bob}：当前用户（服务端决定，缺省 bob）</li>
 *   <li>{@code ?perm=document:write,comment:write}：权限（缺省为 CKEditor 默认权限）</li>
 *   <li>{@code ?reset=1}：重新播种共享的内存 store（alice 的线程 t1 与建议 s1）</li>
 * </ul>
 * 共享的 {@link InMemoryCollaborationStore} 模拟所有用户共用的服务器；{@code #btn-refresh} 把 store 内容投影到
 * {@code #store-state}（每行一条，便于断言）。Comments / TrackChanges 是 premium 插件：没有 license 时只能在
 * {@code CKEDITOR_PREMIUM_STUB=1} 构建的 sample（premium 替身）中运行，见 e2e/README.md。
 */
@Route("collab-bridge")
public class CollaborationBridgeView extends VerticalLayout implements BeforeEnterObserver {

    static final String DOC = "collab-bridge-doc";
    private static final InMemoryCollaborationStore STORE = new InMemoryCollaborationStore();
    private static final CollaborationUser ALICE = new CollaborationUser("alice", "Alice");
    private static final CollaborationUser BOB = new CollaborationUser("bob", "Bob");

    static {
        seed();
    }

    private static synchronized void seed() {
        STORE.clear(DOC);
        STORE.putUser(DOC, ALICE);
        STORE.putCommentThread(DOC, new CommentThreadData("t1", null, null, null, null, null, List.of(
            new CommentData("c1", "alice", Instant.parse("2026-11-01T10:00:00Z"), "<p>Alice's comment</p>", null))));
        STORE.putSuggestion(DOC, new SuggestionData("s1", "insertion", "alice", Instant.parse("2026-11-01T11:00:00Z"),
            false, null, null, null, "open"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        removeAll();
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        if ("1".equals(first(params, "reset", "0"))) {
            seed();
        }
        CollaborationUser user = "alice".equals(first(params, "user", "bob")) ? ALICE : BOB;
        CollaborationBridge.Builder bridge = CollaborationBridge.builder(STORE, DOC, user);
        String perm = first(params, "perm", null);
        if (perm != null) {
            bridge.permissions(Arrays.stream(perm.split(","))
                .map(p -> CollaborationPermission.valueOf(p.trim().toUpperCase().replace(':', '_')))
                .toArray(CollaborationPermission[]::new));
        }

        VaadinCKEditorBuilder builder = VaadinCKEditor.create()
            .withPreset(CKEditorPreset.BASIC)
            .withType(CKEditorType.CLASSIC)
            .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
            .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
            .withCollaborationBridge(bridge.build())
            .withValue("<p>Document with comments and suggestions.</p>");
        String licenseKey = System.getenv("CKEDITOR_LICENSE_KEY");
        if (licenseKey != null && !licenseKey.isBlank()) {
            builder.withLicenseKey(licenseKey);
        }
        VaadinCKEditor editor = builder.build();
        editor.setId("ckeditor");

        Pre state = new Pre(describeStore());
        state.setId("store-state");
        Button refresh = new Button("Refresh store", e -> state.setText(describeStore()));
        refresh.setId("btn-refresh");

        add(new H2("Collaboration bridge (" + user.id() + ")"), editor, refresh, state);
    }

    /** 每条评论 / 建议一行：comment t1/c1 by alice "<p>…</p>"、suggestion s1 insertion by alice open。 */
    static String describeStore() {
        String comments = STORE.getCommentThreads(DOC).stream()
            .flatMap(t -> t.comments().stream().map(c -> "comment " + t.threadId() + "/" + c.commentId()
                + " by " + c.authorId() + " \"" + c.content() + "\"" + (t.resolvedBy() != null ? " resolved by " + t.resolvedBy() : "")))
            .collect(Collectors.joining("\n"));
        String suggestions = STORE.getSuggestions(DOC).stream()
            .map(s -> "suggestion " + s.id() + " " + s.type() + " by " + s.authorId() + " " + s.state())
            .collect(Collectors.joining("\n"));
        return comments + "\n" + suggestions;
    }

    private static String first(Map<String, List<String>> params, String key, String fallback) {
        List<String> values = params.get(key);
        return values == null || values.isEmpty() ? fallback : values.get(0);
    }
}
