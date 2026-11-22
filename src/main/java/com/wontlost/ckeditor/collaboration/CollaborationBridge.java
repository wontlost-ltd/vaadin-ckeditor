package com.wontlost.ckeditor.collaboration;

import com.vaadin.flow.component.dependency.JsModule;
import com.wontlost.ckeditor.CustomPlugin;
import com.wontlost.ckeditor.JsonUtil;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Connects the CKEditor comments and track changes features of one editor to a server-side
 * {@link CollaborationStore}, without CKEditor Cloud (CKEditor's "adapter integration").
 *
 * <pre>
 * CollaborationBridge bridge = CollaborationBridge.builder(store, "document-42", currentUser)
 *     .permissions(CollaborationPermission.COMMENT_WRITE)        // a commenter; defaults to CKEditor's defaults
 *     .build();
 *
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withType(CKEditorType.DECOUPLED)
 *     .withLicenseKey(System.getenv("CKEDITOR_LICENSE_KEY"))   // comments / track changes are premium
 *     .addCustomPlugin(CustomPlugin.fromPremium("Comments"))
 *     .addCustomPlugin(CustomPlugin.fromPremium("TrackChanges"))
 *     .withCollaborationBridge(bridge)                          // adds the VaadinCollaborationBridge plugin
 *     .withValue(savedHtmlWithMarkers)
 *     .build();
 * </pre>
 *
 * <p><b>How it works.</b> The bridge adds the custom plugin {@code VaadinCollaborationBridge} (see
 * {@link #toCustomPlugin()}; its module is registered in the connector's plugin registry by this class's
 * {@code @JsModule}). When the editor starts, the plugin asks the server for the users, the current user,
 * the permissions, the comment threads and the suggestions, registers them with the {@code Users},
 * {@code Permissions}, {@code CommentsRepository} and {@code TrackChanges} plugins, and installs adapters
 * whose every call ({@code addCommentThread}, {@code addComment}, {@code addSuggestion}, …) is sent to
 * {@link #handle(String, JsonNode)}.</p>
 *
 * <p><b>Trust.</b> Everything from the browser is treated as untrusted input. The current user, the document
 * id and the permissions are fixed on the server when the bridge is built; author ids, creation times and
 * {@code resolvedBy} values sent by the browser are ignored and set here; permissions and ownership
 * (own comments, own threads, the author of a split suggestion) are checked before the store is called.</p>
 *
 * <p><b>Content.</b> With the comments / track changes plugins loaded, the editor's HTML
 * ({@code getValue()}) contains {@code <comment-start>}/{@code <comment-end>} and
 * {@code <suggestion-start>}/{@code <suggestion-end>} markers whose names refer to the stored threads and
 * suggestions. Save that HTML together with the store and load it with {@code setValue(...)}.</p>
 *
 * <p><b>Threads.</b> Store futures may complete on any thread; the editor delivers the result with
 * {@code UI.access}, so results completed outside a request need server push ({@code @Push}) or polling to
 * reach the browser before its timeout ({@link Builder#requestTimeout(Duration)}).</p>
 *
 * @since 6.2.0
 */
@JsModule("./vaadin-ckeditor/collaboration-bridge.ts")
public final class CollaborationBridge implements Serializable {

    /** Name of the custom plugin installing the adapters in the browser. */
    public static final String PLUGIN_NAME = "VaadinCollaborationBridge";
    /** Module path of the plugin (resolved through the connector's plugin registry). */
    static final String PLUGIN_IMPORT_PATH = "./vaadin-ckeditor/collaboration-bridge";
    /** Default time the browser waits for an answer. */
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final CollaborationStore store;
    private final CollaborationContext context;
    private final Clock clock;
    private final Duration requestTimeout;

    private CollaborationBridge(Builder builder) {
        this.store = builder.store;
        this.context = new CollaborationContext(builder.channelId, builder.user, builder.permissions);
        this.clock = builder.clock;
        this.requestTimeout = builder.requestTimeout;
    }

    /**
     * @param store       where comments and suggestions are persisted
     * @param channelId   the document id (CKEditor's channel id)
     * @param currentUser the user editing in this session, determined by the server (e.g. from the login)
     * @return a builder
     */
    public static Builder builder(CollaborationStore store, String channelId, CollaborationUser currentUser) {
        return new Builder(store, channelId, currentUser);
    }

    /**
     * @return the document, current user and permissions this bridge acts for
     */
    public CollaborationContext getContext() {
        return context;
    }

    /**
     * @return how long the browser waits for an answer before rejecting the adapter call
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * The custom plugin that installs the adapters in the browser. {@code VaadinCKEditor#setCollaborationBridge}
     * adds it; add it yourself only when wiring the editor without that method.
     *
     * @return the plugin definition
     */
    public CustomPlugin toCustomPlugin() {
        return CustomPlugin.builder(PLUGIN_NAME).withImportPath(PLUGIN_IMPORT_PATH).build();
    }

    // ================================================================ request handling

    /**
     * Handles one adapter call from the browser. Called by {@code VaadinCKEditor}; public so that other
     * transports (and tests) can use the same checks.
     *
     * @param operation the adapter method, such as {@code addComment}, or {@code load} for the start-up data
     * @param payload   the call's data as sent by the browser (untrusted)
     * @return the result to send back; fails with {@link CollaborationException} when the call is rejected
     */
    public CompletableFuture<JsonNode> handle(String operation, JsonNode payload) {
        JsonNode data = payload == null || payload.isNull() ? JsonUtil.createObjectNode() : payload;
        try {
            return switch (operation == null ? "" : operation) {
                case "load" -> load();
                case "getCommentThread" -> getCommentThread(data);
                case "addCommentThread" -> addCommentThread(data);
                case "updateCommentThread" -> updateCommentThread(data);
                case "resolveCommentThread" -> resolveCommentThread(data);
                case "reopenCommentThread" -> reopenCommentThread(data);
                case "removeCommentThread" -> removeCommentThread(data);
                case "addComment" -> addComment(data);
                case "updateComment" -> updateComment(data);
                case "removeComment" -> removeComment(data);
                case "getSuggestion" -> getSuggestion(data);
                case "addSuggestion" -> addSuggestion(data);
                case "updateSuggestion" -> updateSuggestion(data);
                default -> throw CollaborationException.invalid("unknown operation " + operation);
            };
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<JsonNode> load() {
        CompletableFuture<List<CollaborationUser>> users = store.loadUsers(context);
        CompletableFuture<List<CommentThreadData>> threads = store.loadCommentThreads(context);
        CompletableFuture<List<SuggestionData>> suggestions = store.loadSuggestions(context);
        return CompletableFuture.allOf(users, threads, suggestions).thenApply(ignored -> {
            ObjectNode result = JsonUtil.createObjectNode();
            Map<String, CollaborationUser> byId = new LinkedHashMap<>();
            for (CollaborationUser user : users.join()) {
                byId.put(user.id(), user);
            }
            byId.put(context.user().id(), context.user());
            result.set("me", userJson(context.user()));
            ArrayNode userArray = result.putArray("users");
            byId.values().forEach(u -> userArray.add(userJson(u)));
            ArrayNode permissionArray = result.putArray("permissions");
            for (CollaborationPermission permission : CollaborationPermission.values()) {
                if (context.permissions().contains(permission)) {
                    permissionArray.add(permission.getJsName());
                }
            }
            ArrayNode threadArray = result.putArray("threads");
            threads.join().forEach(t -> threadArray.add(threadJson(t)));
            ArrayNode suggestionArray = result.putArray("suggestions");
            suggestions.join().forEach(s -> suggestionArray.add(suggestionJson(s)));
            return result;
        });
    }

    private CompletableFuture<JsonNode> getCommentThread(JsonNode data) {
        return store.getCommentThread(context, requiredText(data, "threadId"))
            .thenApply(thread -> thread.<JsonNode>map(CollaborationBridge::threadJson).orElse(NullNode.getInstance()));
    }

    private CompletableFuture<JsonNode> addCommentThread(JsonNode data) {
        require(CollaborationPermission.COMMENT_WRITE, "add comment threads");
        String threadId = requiredText(data, "threadId");
        Instant now = clock.instant();
        List<CommentData> comments = new ArrayList<>();
        for (JsonNode comment : data.path("comments")) {
            comments.add(new CommentData(requiredText(comment, "commentId"), context.user().id(), now,
                requiredText(comment, "content"), comment.get("attributes")));
        }
        // 新线程不接受客户端的 resolvedAt / resolvedBy（安全起见由 resolveCommentThread 设置）
        CommentThreadData thread = new CommentThreadData(threadId, context.user().id(), data.get("context"), null, null,
            optionalInstant(data, "unlinkedAt"), data.get("attributes"), comments);
        return store.addCommentThread(context, thread).thenApply(ignored -> {
            ObjectNode result = JsonUtil.createObjectNode();
            result.put("threadId", threadId);
            ArrayNode array = result.putArray("comments");
            for (CommentData comment : comments) {
                array.addObject().put("commentId", comment.commentId()).put("createdAt", comment.createdAt().toString());
            }
            return result;
        });
    }

    private CompletableFuture<JsonNode> updateCommentThread(JsonNode data) {
        require(CollaborationPermission.COMMENT_WRITE, "update comment threads");
        String threadId = requiredText(data, "threadId");
        CommentThreadUpdate update = new CommentThreadUpdate(nonNull(data.get("context")), nonNull(data.get("attributes")),
            data.has("unlinkedAt"), optionalInstant(data, "unlinkedAt"));
        return existingThread(threadId)
            .thenCompose(thread -> store.updateCommentThread(context, threadId, update))
            .thenApply(ignored -> nullNode());
    }

    private CompletableFuture<JsonNode> resolveCommentThread(JsonNode data) {
        requireAny("resolve comment threads", CollaborationPermission.COMMENT_WRITE, CollaborationPermission.DOCUMENT_WRITE);
        String threadId = requiredText(data, "threadId");
        Instant now = clock.instant();
        return existingThread(threadId)
            .thenCompose(thread -> store.resolveCommentThread(context, threadId, now, context.user().id()))
            .thenApply(ignored -> {
                ObjectNode result = JsonUtil.createObjectNode();
                result.put("threadId", threadId);
                result.put("resolvedAt", now.toString());
                result.put("resolvedBy", context.user().id());
                return result;
            });
    }

    private CompletableFuture<JsonNode> reopenCommentThread(JsonNode data) {
        requireAny("reopen comment threads", CollaborationPermission.COMMENT_WRITE, CollaborationPermission.DOCUMENT_WRITE);
        String threadId = requiredText(data, "threadId");
        return existingThread(threadId)
            .thenCompose(thread -> store.reopenCommentThread(context, threadId))
            .thenApply(ignored -> nullNode());
    }

    private CompletableFuture<JsonNode> removeCommentThread(JsonNode data) {
        require(CollaborationPermission.COMMENT_WRITE, "remove comment threads");
        String threadId = requiredText(data, "threadId");
        return existingThread(threadId).thenCompose(thread -> {
            // 线程作者在创建时确定，不随评论删除而改变；作者未知的线程只能由 comment:admin 删除
            boolean own = context.user().id().equals(thread.authorId());
            if (!own && !has(CollaborationPermission.COMMENT_ADMIN)) {
                throw CollaborationException.denied("only the author or a comment admin can remove this thread");
            }
            return store.removeCommentThread(context, threadId);
        }).thenApply(ignored -> nullNode());
    }

    private CompletableFuture<JsonNode> addComment(JsonNode data) {
        require(CollaborationPermission.COMMENT_WRITE, "add comments");
        String threadId = requiredText(data, "threadId");
        CommentData comment = new CommentData(requiredText(data, "commentId"), context.user().id(), clock.instant(),
            requiredText(data, "content"), data.get("attributes"));
        return store.addComment(context, threadId, comment).thenApply(ignored -> {
            ObjectNode result = JsonUtil.createObjectNode();
            result.put("commentId", comment.commentId());
            result.put("createdAt", comment.createdAt().toString());
            return result;
        });
    }

    private CompletableFuture<JsonNode> updateComment(JsonNode data) {
        String threadId = requiredText(data, "threadId");
        String commentId = requiredText(data, "commentId");
        return ownedComment(threadId, commentId, "edit").thenCompose(comment -> {
            CommentData updated = comment;
            if (data.hasNonNull("content")) {
                updated = updated.withContent(data.get("content").asText());
            }
            if (data.hasNonNull("attributes")) {
                updated = updated.withAttributes(data.get("attributes"));
            }
            return store.updateComment(context, threadId, updated);
        }).thenApply(ignored -> nullNode());
    }

    private CompletableFuture<JsonNode> removeComment(JsonNode data) {
        String threadId = requiredText(data, "threadId");
        String commentId = requiredText(data, "commentId");
        return ownedComment(threadId, commentId, "remove")
            .thenCompose(comment -> store.removeComment(context, threadId, commentId))
            .thenApply(ignored -> nullNode());
    }

    private CompletableFuture<JsonNode> getSuggestion(JsonNode data) {
        String id = requiredText(data, "id");
        return store.getSuggestion(context, id).thenApply(suggestion -> suggestionJson(
            suggestion.orElseThrow(() -> CollaborationException.notFound("suggestion " + id))));
    }

    private CompletableFuture<JsonNode> addSuggestion(JsonNode data) {
        require(CollaborationPermission.DOCUMENT_WRITE, "add suggestions");
        String id = requiredText(data, "id");
        String type = requiredText(data, "type");
        String originalId = data.hasNonNull("originalSuggestionId") ? data.get("originalSuggestionId").asText() : null;
        Instant now = clock.instant();
        // 拆分他人的建议时，新建议的作者必须是原建议的作者（TrackChangesAdapter#addSuggestion 文档要求）
        CompletableFuture<String> author = originalId == null
            ? CompletableFuture.completedFuture(context.user().id())
            : store.getSuggestion(context, originalId).thenApply(original -> original
                .orElseThrow(() -> CollaborationException.notFound("suggestion " + originalId)).authorId());
        return author.thenCompose(authorId -> {
            SuggestionData suggestion = new SuggestionData(id, type, authorId, now,
                data.path("hasComments").asBoolean(false), nonNull(data.get("data")), data.get("attributes"),
                originalId, "open");
            return store.addSuggestion(context, suggestion).thenApply(ignored -> suggestionJson(suggestion));
        });
    }

    private CompletableFuture<JsonNode> updateSuggestion(JsonNode data) {
        String id = requiredText(data, "id");
        String state = data.hasNonNull("state") ? data.get("state").asText() : null;
        if (state != null && !Set.of("open", "accepted", "rejected").contains(state)) {
            throw CollaborationException.invalid("suggestion state " + state);
        }
        if (state != null) {
            require(CollaborationPermission.DOCUMENT_WRITE, "accept or discard suggestions");
        } else {
            requireAny("update suggestions", CollaborationPermission.DOCUMENT_WRITE, CollaborationPermission.COMMENT_WRITE);
        }
        SuggestionUpdate update = new SuggestionUpdate(
            data.hasNonNull("hasComments") ? data.get("hasComments").asBoolean() : null,
            nonNull(data.get("attributes")), state);
        return store.getSuggestion(context, id)
            .thenCompose(existing -> {
                existing.orElseThrow(() -> CollaborationException.notFound("suggestion " + id));
                return store.updateSuggestion(context, id, update);
            })
            .thenApply(ignored -> nullNode());
    }

    // ================================================================ checks

    private boolean has(CollaborationPermission permission) {
        return permission.isGrantedBy(context.permissions());
    }

    private void require(CollaborationPermission permission, String action) {
        if (!has(permission)) {
            throw CollaborationException.denied(action + " requires " + permission.getJsName());
        }
    }

    private void requireAny(String action, CollaborationPermission... permissions) {
        for (CollaborationPermission permission : permissions) {
            if (has(permission)) {
                return;
            }
        }
        throw CollaborationException.denied(action);
    }

    private CompletableFuture<CommentThreadData> existingThread(String threadId) {
        return store.getCommentThread(context, threadId).thenApply(thread ->
            thread.orElseThrow(() -> CollaborationException.notFound("comment thread " + threadId)));
    }

    /**
     * The comment, if the current user may change it: own comments need {@code comment:write}, other users'
     * comments need {@code comment:modify_all}.
     */
    private CompletableFuture<CommentData> ownedComment(String threadId, String commentId, String action) {
        return existingThread(threadId).thenApply(thread -> {
            CommentData comment = thread.comment(commentId)
                .orElseThrow(() -> CollaborationException.notFound("comment " + commentId));
            if (comment.authorId().equals(context.user().id())) {
                require(CollaborationPermission.COMMENT_WRITE, action + " comments");
            } else if (!has(CollaborationPermission.COMMENT_MODIFY_ALL)) {
                throw CollaborationException.denied("only the author can " + action + " this comment");
            }
            return comment;
        });
    }

    // ================================================================ JSON

    private static String requiredText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || !value.isTextual() || value.asText().isEmpty()) {
            throw CollaborationException.invalid("missing " + field);
        }
        return value.asText();
    }

    private static Instant optionalInstant(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        try {
            return Instant.parse(value.asText());
        } catch (DateTimeParseException e) {
            throw CollaborationException.invalid(field);
        }
    }

    private static JsonNode nonNull(JsonNode node) {
        return node == null || node.isNull() ? null : node;
    }

    private static JsonNode nullNode() {
        return NullNode.getInstance();
    }

    private static String iso(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    static ObjectNode userJson(CollaborationUser user) {
        ObjectNode node = JsonUtil.createObjectNode();
        node.put("id", user.id());
        node.put("name", user.name());
        if (user.avatar() != null) {
            node.put("avatar", user.avatar());
        }
        return node;
    }

    static ObjectNode threadJson(CommentThreadData thread) {
        ObjectNode node = JsonUtil.createObjectNode();
        node.put("threadId", thread.threadId());
        node.set("context", thread.context());
        node.put("resolvedAt", iso(thread.resolvedAt()));
        node.put("resolvedBy", thread.resolvedBy());
        node.put("unlinkedAt", iso(thread.unlinkedAt()));
        node.set("attributes", thread.attributes());
        ArrayNode comments = node.putArray("comments");
        for (CommentData comment : thread.comments()) {
            ObjectNode c = comments.addObject();
            c.put("commentId", comment.commentId());
            c.put("authorId", comment.authorId());
            c.put("createdAt", iso(comment.createdAt()));
            c.put("content", comment.content());
            c.set("attributes", comment.attributes());
        }
        return node;
    }

    static ObjectNode suggestionJson(SuggestionData suggestion) {
        ObjectNode node = JsonUtil.createObjectNode();
        node.put("id", suggestion.id());
        node.put("type", suggestion.type());
        node.put("authorId", suggestion.authorId());
        node.put("createdAt", iso(suggestion.createdAt()));
        node.put("hasComments", suggestion.hasComments());
        node.set("data", suggestion.data());
        node.set("attributes", suggestion.attributes());
        node.put("originalSuggestionId", suggestion.originalSuggestionId());
        return node;
    }

    /**
     * Unwraps {@link CompletionException} layers.
     *
     * @param error a failure of a future returned by {@link #handle}
     * @return the cause that is meaningful to report
     */
    public static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    // ================================================================ builder

    /** Builder of a {@link CollaborationBridge}. */
    public static final class Builder {
        private final CollaborationStore store;
        private final String channelId;
        private final CollaborationUser user;
        private Set<CollaborationPermission> permissions = CollaborationPermission.DEFAULTS;
        private Clock clock = Clock.systemUTC();
        private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

        private Builder(CollaborationStore store, String channelId, CollaborationUser user) {
            this.store = Objects.requireNonNull(store, "store");
            this.channelId = Objects.requireNonNull(channelId, "channelId");
            this.user = Objects.requireNonNull(user, "currentUser");
            if (channelId.isBlank()) {
                throw new IllegalArgumentException("channelId must not be blank");
            }
        }

        /**
         * The current user's permissions; defaults to {@link CollaborationPermission#DEFAULTS}.
         *
         * @param permissions the permissions (none: a read-only reviewer)
         * @return this builder
         */
        public Builder permissions(CollaborationPermission... permissions) {
            this.permissions = permissions.length == 0
                ? Set.of() : Set.copyOf(EnumSet.of(permissions[0], permissions));
            return this;
        }

        /**
         * The clock used for {@code createdAt} / {@code resolvedAt}; defaults to UTC system time.
         *
         * @param clock the clock
         * @return this builder
         */
        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * How long the browser waits for an answer before the adapter call fails; defaults to 30 seconds.
         *
         * @param timeout a positive duration
         * @return this builder
         */
        public Builder requestTimeout(Duration timeout) {
            if (timeout == null || timeout.isNegative() || timeout.isZero()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.requestTimeout = timeout;
            return this;
        }

        /**
         * @return the bridge
         */
        public CollaborationBridge build() {
            return new CollaborationBridge(this);
        }
    }
}
