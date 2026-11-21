package com.wontlost.ckeditor.collaboration;

import com.wontlost.ckeditor.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.*;

/**
 * CollaborationBridge + InMemoryCollaborationStore：作者与时间由服务端决定、权限与归属校验、
 * 建议拆分时沿用原作者、修订建议讨论线程的懒创建、启动加载的数据形状。
 */
class CollaborationBridgeTest {

    private static final String DOC = "doc-1";
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");
    private static final CollaborationUser ALICE = new CollaborationUser("alice", "Alice");
    private static final CollaborationUser BOB = new CollaborationUser("bob", "Bob", "https://example.test/bob.png");

    private InMemoryCollaborationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryCollaborationStore().putUser(DOC, ALICE);
        // alice 的线程 t1（评论 c1）与建议 s1
        store.putCommentThread(DOC, new CommentThreadData("t1", "alice", null, null, null, null, null, List.of(
            new CommentData("c1", "alice", Instant.parse("2026-11-01T00:00:00Z"), "<p>Alice</p>", null))));
        store.putSuggestion(DOC, new SuggestionData("s1", "insertion", "alice", Instant.parse("2026-11-01T00:00:00Z"),
            false, null, null, null, null));
    }

    private CollaborationBridge bridge(CollaborationUser user, CollaborationPermission... permissions) {
        CollaborationBridge.Builder builder = CollaborationBridge.builder(store, DOC, user)
            .clock(Clock.fixed(NOW, ZoneOffset.UTC));
        if (permissions.length > 0) {
            builder.permissions(permissions);
        }
        return builder.build();
    }

    private static JsonNode json(String text) {
        return JsonUtil.getMapper().readTree(text.replace('\'', '"'));
    }

    private static JsonNode call(CollaborationBridge bridge, String operation, String payload) {
        return bridge.handle(operation, json(payload)).join();
    }

    private static CollaborationException rejection(CollaborationBridge bridge, String operation, String payload) {
        CompletableFuture<JsonNode> future = bridge.handle(operation, json(payload));
        Throwable error = catchThrowable(future::join);
        assertThat(error).isInstanceOf(CompletionException.class);
        Throwable cause = CollaborationBridge.unwrap(error);
        assertThat(cause).isInstanceOf(CollaborationException.class);
        return (CollaborationException) cause;
    }

    private CommentThreadData thread(String id) {
        return store.getCommentThreads(DOC).stream().filter(t -> t.threadId().equals(id)).findFirst().orElseThrow();
    }

    @Nested
    @DisplayName("启动加载")
    class Load {
        @Test
        @DisplayName("load 返回 me、用户（含当前用户）、权限、线程与建议，时间为 ISO-8601")
        void loadShape() {
            JsonNode result = call(bridge(BOB, CollaborationPermission.COMMENT_WRITE), "load", "{}");
            assertThat(result.path("me").path("id").asText()).isEqualTo("bob");
            assertThat(result.path("me").path("avatar").asText()).isEqualTo("https://example.test/bob.png");
            assertThat(result.path("users").valueStream().map(u -> u.path("id").asText()).toList()).containsExactly("alice", "bob");
            assertThat(result.path("permissions").toString()).isEqualTo("[\"comment:write\"]");
            JsonNode t1 = result.path("threads").get(0);
            assertThat(t1.path("threadId").asText()).isEqualTo("t1");
            assertThat(t1.path("comments").get(0).path("authorId").asText()).isEqualTo("alice");
            assertThat(t1.path("comments").get(0).path("createdAt").asText()).isEqualTo("2026-11-01T00:00:00Z");
            assertThat(t1.path("resolvedAt").isNull()).isTrue();
            assertThat(result.path("suggestions").get(0).path("id").asText()).isEqualTo("s1");
        }

        @Test
        @DisplayName("默认权限为 CKEditor 默认值 document:write、comment:write、comment:admin")
        void defaultPermissions() {
            JsonNode result = call(bridge(BOB), "load", "{}");
            assertThat(result.path("permissions").toString())
                .isEqualTo("[\"document:write\",\"comment:write\",\"comment:admin\"]");
        }
    }

    @Nested
    @DisplayName("作者与时间由服务端决定")
    class ServerAssigned {
        @Test
        @DisplayName("addCommentThread 忽略客户端的 authorId / createdAt / resolvedAt / resolvedBy")
        void addThreadIgnoresClientAuthor() {
            JsonNode result = call(bridge(BOB), "addCommentThread",
                "{'threadId':'t2','context':{'type':'text','value':'x'},'resolvedAt':'2020-01-01T00:00:00Z','resolvedBy':'alice',"
                    + "'comments':[{'commentId':'c2','content':'<p>Hi</p>','authorId':'alice','createdAt':'2000-01-01T00:00:00Z'}]}");
            assertThat(result.path("threadId").asText()).isEqualTo("t2");
            assertThat(result.path("comments").get(0).path("createdAt").asText()).isEqualTo(NOW.toString());

            CommentThreadData stored = thread("t2");
            assertThat(stored.comments().get(0).authorId()).isEqualTo("bob");
            assertThat(stored.comments().get(0).createdAt()).isEqualTo(NOW);
            assertThat(stored.resolvedAt()).isNull();
            assertThat(stored.resolvedBy()).isNull();
            assertThat(stored.context().path("value").asText()).isEqualTo("x");
            // 写过内容的用户在下次启动时可加载
            assertThat(call(bridge(ALICE), "load", "{}").path("users").valueStream().map(u -> u.path("id").asText()).toList()).contains("bob");
        }

        @Test
        @DisplayName("addComment 以当前用户为作者、服务端时间为 createdAt")
        void addCommentAuthor() {
            JsonNode result = call(bridge(BOB), "addComment",
                "{'threadId':'t1','commentId':'c3','content':'reply','attributes':{'k':1},'authorId':'alice'}");
            assertThat(result.path("createdAt").asText()).isEqualTo(NOW.toString());
            CommentData stored = thread("t1").comment("c3").orElseThrow();
            assertThat(stored.authorId()).isEqualTo("bob");
            assertThat(stored.attributes().path("k").asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("resolveCommentThread 的 resolvedBy 是当前用户")
        void resolveBy() {
            JsonNode result = call(bridge(BOB), "resolveCommentThread", "{'threadId':'t1','resolvedBy':'alice'}");
            assertThat(result.path("resolvedBy").asText()).isEqualTo("bob");
            assertThat(thread("t1").resolvedBy()).isEqualTo("bob");
            assertThat(thread("t1").resolvedAt()).isEqualTo(NOW);

            call(bridge(BOB), "reopenCommentThread", "{'threadId':'t1'}");
            assertThat(thread("t1").resolvedAt()).isNull();
        }

        @Test
        @DisplayName("addSuggestion 以当前用户为作者；拆分他人建议时沿用原建议作者")
        void suggestionAuthor() {
            JsonNode own = call(bridge(BOB), "addSuggestion", "{'id':'s2','type':'insertion','authorId':'alice'}");
            assertThat(own.path("authorId").asText()).isEqualTo("bob");
            assertThat(own.path("createdAt").asText()).isEqualTo(NOW.toString());

            JsonNode split = call(bridge(BOB), "addSuggestion",
                "{'id':'s3','type':'insertion','originalSuggestionId':'s1','attributes':{'groupId':'g'}}");
            assertThat(split.path("authorId").asText()).isEqualTo("alice");
            assertThat(split.path("originalSuggestionId").asText()).isEqualTo("s1");

            assertThat(rejection(bridge(BOB), "addSuggestion", "{'id':'s4','type':'insertion','originalSuggestionId':'nope'}")
                .getReason()).isEqualTo(CollaborationException.Reason.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("归属与权限")
    class Permissions {
        @Test
        @DisplayName("只能编辑 / 删除自己的评论；comment:modify_all 可以编辑他人的")
        void ownComments() {
            CollaborationException denied = rejection(bridge(BOB), "updateComment", "{'threadId':'t1','commentId':'c1','content':'hack'}");
            assertThat(denied.getReason()).isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(rejection(bridge(BOB), "removeComment", "{'threadId':'t1','commentId':'c1'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(thread("t1").comment("c1").orElseThrow().content()).isEqualTo("<p>Alice</p>");

            call(bridge(ALICE), "updateComment", "{'threadId':'t1','commentId':'c1','content':'edited'}");
            CommentData edited = thread("t1").comment("c1").orElseThrow();
            assertThat(edited.content()).isEqualTo("edited");
            assertThat(edited.authorId()).isEqualTo("alice");
            assertThat(edited.createdAt()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));

            call(bridge(BOB, CollaborationPermission.COMMENT_MODIFY_ALL), "updateComment",
                "{'threadId':'t1','commentId':'c1','attributes':{'m':true}}");
            assertThat(thread("t1").comment("c1").orElseThrow().attributes().path("m").asBoolean()).isTrue();
            assertThat(thread("t1").comment("c1").orElseThrow().content()).isEqualTo("edited");

            call(bridge(BOB, CollaborationPermission.COMMENT_MODIFY_ALL), "removeComment", "{'threadId':'t1','commentId':'c1'}");
            assertThat(thread("t1").comments()).isEmpty();
        }

        @Test
        @DisplayName("只有 comment:modify_all 的用户可改删他人评论，但不能写评论、建线程或改自己的评论")
        void modifyAllOnly() {
            CollaborationBridge moderator = bridge(BOB, CollaborationPermission.COMMENT_MODIFY_ALL);
            assertThat(rejection(moderator, "addComment", "{'threadId':'t1','commentId':'c5','content':'x'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(rejection(moderator, "addCommentThread", "{'threadId':'t5','comments':[]}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(rejection(moderator, "resolveCommentThread", "{'threadId':'t1'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            call(moderator, "updateComment", "{'threadId':'t1','commentId':'c1','content':'moderated'}");
            assertThat(thread("t1").comment("c1").orElseThrow().content()).isEqualTo("moderated");

            call(bridge(BOB, CollaborationPermission.COMMENT_WRITE), "addComment", "{'threadId':'t1','commentId':'c6','content':'mine'}");
            assertThat(rejection(moderator, "updateComment", "{'threadId':'t1','commentId':'c6','content':'x'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
        }

        @Test
        @DisplayName("线程作者在创建时确定：删除首条评论不会把线程转给下一个评论者；作者未知的线程只能由 admin 删除")
        void threadOwnershipIsStable() {
            CollaborationBridge bob = bridge(BOB, CollaborationPermission.COMMENT_WRITE);
            call(bob, "addComment", "{'threadId':'t1','commentId':'c2','content':'reply'}");
            call(bridge(ALICE, CollaborationPermission.COMMENT_WRITE), "removeComment", "{'threadId':'t1','commentId':'c1'}");
            assertThat(thread("t1").authorId()).isEqualTo("alice");
            assertThat(rejection(bob, "removeCommentThread", "{'threadId':'t1'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);

            store.putCommentThread(DOC, new CommentThreadData("orphan", null, null, null, null, null, null, List.of()));
            assertThat(rejection(bob, "removeCommentThread", "{'threadId':'orphan'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            call(bridge(BOB, CollaborationPermission.COMMENT_ADMIN), "removeCommentThread", "{'threadId':'orphan'}");

            // 新线程的作者是创建者；修订建议讨论线程的作者是首条评论的作者
            call(bob, "addCommentThread", "{'threadId':'t7','comments':[]}");
            assertThat(thread("t7").authorId()).isEqualTo("bob");
            call(bridge(ALICE), "addComment", "{'threadId':'s1','commentId':'c8','content':'why?'}");
            assertThat(thread("s1").authorId()).isEqualTo("alice");
        }

        @Test
        @DisplayName("删除他人的线程需要 comment:admin")
        void removeThread() {
            assertThat(rejection(bridge(BOB, CollaborationPermission.COMMENT_WRITE), "removeCommentThread", "{'threadId':'t1'}")
                .getReason()).isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            call(bridge(BOB, CollaborationPermission.COMMENT_ADMIN), "removeCommentThread", "{'threadId':'t1'}");
            assertThat(store.getCommentThreads(DOC)).isEmpty();
        }

        @Test
        @DisplayName("作者可删除自己的线程")
        void removeOwnThread() {
            call(bridge(BOB, CollaborationPermission.COMMENT_WRITE), "addCommentThread",
                "{'threadId':'t9','comments':[{'commentId':'c9','content':'x'}]}");
            call(bridge(BOB, CollaborationPermission.COMMENT_WRITE), "removeCommentThread", "{'threadId':'t9'}");
            assertThat(store.getCommentThreads(DOC)).extracting(CommentThreadData::threadId).containsExactly("t1");
        }

        @Test
        @DisplayName("无 comment:write 的只读审阅者不能评论；无 document:write 不能提建议或接受建议")
        void readOnly() {
            CollaborationBridge reviewer = CollaborationBridge.builder(store, DOC, BOB).permissions().build();
            assertThat(reviewer.getContext().permissions()).isEmpty();
            assertThat(rejection(reviewer, "addComment", "{'threadId':'t1','commentId':'c5','content':'x'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(rejection(reviewer, "addCommentThread", "{'threadId':'t5','comments':[]}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(rejection(reviewer, "resolveCommentThread", "{'threadId':'t1'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);

            CollaborationBridge commenter = bridge(BOB, CollaborationPermission.COMMENT_WRITE);
            assertThat(rejection(commenter, "addSuggestion", "{'id':'s5','type':'insertion'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            assertThat(rejection(commenter, "updateSuggestion", "{'id':'s1','state':'accepted'}").getReason())
                .isEqualTo(CollaborationException.Reason.PERMISSION_DENIED);
            // 评论者可以在建议下讨论：hasComments 的更新只需 comment:write
            call(commenter, "updateSuggestion", "{'id':'s1','hasComments':true}");
            assertThat(store.getSuggestions(DOC).get(0).hasComments()).isTrue();
            assertThat(store.getSuggestions(DOC).get(0).state()).isEqualTo("open");

            call(bridge(ALICE, CollaborationPermission.DOCUMENT_WRITE), "updateSuggestion", "{'id':'s1','state':'accepted'}");
            assertThat(store.getSuggestions(DOC).get(0).state()).isEqualTo("accepted");
        }

        @Test
        @DisplayName("comment:admin 隐含 comment:write；comment:modify_all 不隐含（CKEditor Permissions 语义）")
        void implications() {
            assertThat(CollaborationPermission.COMMENT_WRITE.isGrantedBy(java.util.Set.of(CollaborationPermission.COMMENT_ADMIN))).isTrue();
            assertThat(CollaborationPermission.COMMENT_WRITE.isGrantedBy(java.util.Set.of(CollaborationPermission.COMMENT_MODIFY_ALL))).isFalse();
            assertThat(CollaborationPermission.COMMENT_ADMIN.isGrantedBy(java.util.Set.of(CollaborationPermission.COMMENT_WRITE))).isFalse();
            assertThat(CollaborationPermission.DOCUMENT_WRITE.isGrantedBy(java.util.Set.of(CollaborationPermission.COMMENT_ADMIN))).isFalse();
        }
    }

    @Nested
    @DisplayName("数据与错误")
    class DataAndErrors {
        @Test
        @DisplayName("修订建议的讨论：首条评论自动创建与建议同 id 的线程")
        void suggestionThread() {
            call(bridge(BOB), "addComment", "{'threadId':'s1','commentId':'c7','content':'why?'}");
            assertThat(thread("s1").comments()).extracting(CommentData::authorId).containsExactly("bob");
            assertThat(rejection(bridge(BOB), "addComment", "{'threadId':'unknown','commentId':'c8','content':'x'}").getReason())
                .isEqualTo(CollaborationException.Reason.NOT_FOUND);
        }

        @Test
        @DisplayName("getCommentThread 不存在时返回 null；getSuggestion 不存在时 NOT_FOUND")
        void gets() {
            assertThat(call(bridge(BOB), "getCommentThread", "{'threadId':'t1'}").path("threadId").asText()).isEqualTo("t1");
            assertThat(call(bridge(BOB), "getCommentThread", "{'threadId':'nope'}").isNull()).isTrue();
            assertThat(call(bridge(BOB), "getSuggestion", "{'id':'s1'}").path("authorId").asText()).isEqualTo("alice");
            assertThat(rejection(bridge(BOB), "getSuggestion", "{'id':'nope'}").getReason())
                .isEqualTo(CollaborationException.Reason.NOT_FOUND);
        }

        @Test
        @DisplayName("updateCommentThread 只改给出的字段，unlinkedAt 可设为 null")
        void updateThread() {
            call(bridge(BOB), "updateCommentThread", "{'threadId':'t1','attributes':{'a':1},'unlinkedAt':'2026-11-02T00:00:00Z'}");
            assertThat(thread("t1").attributes().path("a").asInt()).isEqualTo(1);
            assertThat(thread("t1").unlinkedAt()).isEqualTo(Instant.parse("2026-11-02T00:00:00Z"));
            call(bridge(BOB), "updateCommentThread", "{'threadId':'t1','unlinkedAt':null}");
            assertThat(thread("t1").unlinkedAt()).isNull();
            assertThat(thread("t1").attributes().path("a").asInt()).isEqualTo(1);
        }

        @Test
        @DisplayName("非法请求：未知操作、缺字段、重复 id、非法时间、非法建议状态")
        void invalid() {
            assertThat(rejection(bridge(BOB), "dropDatabase", "{}").getReason()).isEqualTo(CollaborationException.Reason.INVALID_REQUEST);
            assertThat(rejection(bridge(BOB), "addComment", "{'threadId':'t1','content':'x'}").getReason())
                .isEqualTo(CollaborationException.Reason.INVALID_REQUEST);
            assertThat(rejection(bridge(BOB), "addCommentThread", "{'threadId':'t1','comments':[]}").getReason())
                .isEqualTo(CollaborationException.Reason.INVALID_REQUEST);
            assertThat(rejection(bridge(BOB), "updateCommentThread", "{'threadId':'t1','unlinkedAt':'yesterday'}").getReason())
                .isEqualTo(CollaborationException.Reason.INVALID_REQUEST);
            assertThat(rejection(bridge(BOB), "updateSuggestion", "{'id':'s1','state':'merged'}").getReason())
                .isEqualTo(CollaborationException.Reason.INVALID_REQUEST);
            assertThat(rejection(bridge(BOB), "updateComment", "{'threadId':'t1','commentId':'nope','content':'x'}").getReason())
                .isEqualTo(CollaborationException.Reason.NOT_FOUND);
            assertThat(bridge(BOB).handle(null, null)).isCompletedExceptionally();
        }

        @Test
        @DisplayName("通道隔离：另一文档看不到本文档的数据")
        void channels() {
            CollaborationBridge other = CollaborationBridge.builder(store, "doc-2", BOB).build();
            assertThat(call(other, "load", "{}").path("threads")).isEmpty();
            assertThat(call(other, "getCommentThread", "{'threadId':'t1'}").isNull()).isTrue();
            store.clear(DOC);
            assertThat(store.getCommentThreads(DOC)).isEmpty();
            assertThat(store.getSuggestions(DOC)).isEmpty();
        }

        @Test
        @DisplayName("store 的非 CollaborationException 失败原样传出（由 VaadinCKEditor 记录并以通用消息回给浏览器）")
        void storeFailure() {
            CollaborationStore failing = new InMemoryCollaborationStore() {
                @Override
                public CompletableFuture<Void> addComment(CollaborationContext context, String threadId, CommentData comment) {
                    return CompletableFuture.failedFuture(new IllegalStateException("db down"));
                }
            };
            CollaborationBridge bridge = CollaborationBridge.builder(failing, DOC, BOB).build();
            Throwable error = catchThrowable(() -> bridge.handle("addComment", json("{'threadId':'t','commentId':'c','content':'x'}")).join());
            assertThat(CollaborationBridge.unwrap(error)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("构建与序列化")
    class Building {
        @Test
        @DisplayName("builder 校验参数；超时默认 30 秒")
        void builder() {
            assertThatThrownBy(() -> CollaborationBridge.builder(null, DOC, BOB)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollaborationBridge.builder(store, " ", BOB)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CollaborationBridge.builder(store, DOC, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollaborationBridge.builder(store, DOC, BOB).requestTimeout(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CollaborationUser(" ", "x")).isInstanceOf(IllegalArgumentException.class);
            CollaborationBridge bridge = CollaborationBridge.builder(store, DOC, BOB).build();
            assertThat(bridge.getRequestTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(bridge.getContext().user()).isEqualTo(BOB);
            assertThat(bridge.toCustomPlugin().getJsName()).isEqualTo("VaadinCollaborationBridge");
            assertThat(bridge.toCustomPlugin().isPremium()).isFalse();
            assertThat(bridge.toCustomPlugin().getImportPath()).isEqualTo("./vaadin-ckeditor/collaboration-bridge");
        }

        @Test
        @DisplayName("桥与内存 store 可随会话序列化")
        void serializable() throws Exception {
            call(bridge(BOB), "addComment", "{'threadId':'t1','commentId':'c3','content':'x','attributes':{'k':[1,2]}}");
            CollaborationBridge original = bridge(BOB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(original);
            }
            CollaborationBridge copy;
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                copy = (CollaborationBridge) in.readObject();
            }
            JsonNode thread = call(copy, "getCommentThread", "{'threadId':'t1'}");
            assertThat(thread.path("comments").get(1).path("attributes").path("k").get(1).asInt()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("ObjectNode 输入不会被 store 中的数据共享（防御性拷贝）")
    void defensiveCopies() {
        ObjectNode attributes = JsonUtil.createObjectNode().put("x", 1);
        CommentData comment = new CommentData("c", "a", NOW, "t", attributes);
        attributes.put("x", 2);
        assertThat(comment.attributes().path("x").asInt()).isEqualTo(1);
    }
}
