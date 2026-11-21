package com.wontlost.ckeditor.collaboration;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Reference {@link CollaborationStore} keeping everything in memory, per channel. For demos and tests; data is
 * lost on restart. Thread-safe: each channel is guarded by its own lock, and every future is already completed
 * when returned.
 *
 * <p>One instance can be shared by many editors and sessions (it is the "server" all users of a document talk
 * to); seed it with {@link #putUser}, {@link #putCommentThread} and {@link #putSuggestion}.</p>
 *
 * @since 6.2.0
 */
public class InMemoryCollaborationStore implements CollaborationStore, Serializable {

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    /** One document's data; access under {@code synchronized (channel)}. */
    private static final class Channel implements Serializable {
        final Map<String, CollaborationUser> users = new LinkedHashMap<>();
        final Map<String, CommentThreadData> threads = new LinkedHashMap<>();
        final Map<String, SuggestionData> suggestions = new LinkedHashMap<>();
    }

    private Channel channel(String channelId) {
        return channels.computeIfAbsent(channelId, id -> new Channel());
    }

    private <T> CompletableFuture<T> inChannel(CollaborationContext context, java.util.function.Function<Channel, T> action) {
        Channel channel = channel(context.channelId());
        try {
            synchronized (channel) {
                return CompletableFuture.completedFuture(action.apply(channel));
            }
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<Void> run(CollaborationContext context, java.util.function.Consumer<Channel> action) {
        return inChannel(context, channel -> {
            action.accept(channel);
            return null;
        });
    }

    // ---------------------------------------------------------------- seeding / inspection

    /**
     * Adds or replaces a user of a channel.
     *
     * @param channelId the channel
     * @param user      the user
     * @return this store
     */
    public InMemoryCollaborationStore putUser(String channelId, CollaborationUser user) {
        Channel channel = channel(channelId);
        synchronized (channel) {
            channel.users.put(user.id(), user);
        }
        return this;
    }

    /**
     * Adds or replaces a thread (seed data).
     *
     * @param channelId the channel
     * @param thread    the thread
     * @return this store
     */
    public InMemoryCollaborationStore putCommentThread(String channelId, CommentThreadData thread) {
        Channel channel = channel(channelId);
        synchronized (channel) {
            channel.threads.put(thread.threadId(), thread);
        }
        return this;
    }

    /**
     * Adds or replaces a suggestion (seed data).
     *
     * @param channelId  the channel
     * @param suggestion the suggestion
     * @return this store
     */
    public InMemoryCollaborationStore putSuggestion(String channelId, SuggestionData suggestion) {
        Channel channel = channel(channelId);
        synchronized (channel) {
            channel.suggestions.put(suggestion.id(), suggestion);
        }
        return this;
    }

    /**
     * Removes all users, threads and suggestions of a channel.
     *
     * @param channelId the channel
     * @return this store
     */
    public InMemoryCollaborationStore clear(String channelId) {
        channels.remove(channelId);
        return this;
    }

    /**
     * @param channelId the channel
     * @return a snapshot of the channel's threads
     */
    public List<CommentThreadData> getCommentThreads(String channelId) {
        return snapshot(channelId, c -> List.copyOf(c.threads.values()));
    }

    /**
     * @param channelId the channel
     * @return a snapshot of the channel's suggestions
     */
    public List<SuggestionData> getSuggestions(String channelId) {
        return snapshot(channelId, c -> List.copyOf(c.suggestions.values()));
    }

    private <T> T snapshot(String channelId, java.util.function.Function<Channel, T> read) {
        Channel channel = channel(channelId);
        synchronized (channel) {
            return read.apply(channel);
        }
    }

    // ---------------------------------------------------------------- CollaborationStore

    @Override
    public CompletableFuture<List<CollaborationUser>> loadUsers(CollaborationContext context) {
        return inChannel(context, c -> List.copyOf(c.users.values()));
    }

    @Override
    public CompletableFuture<List<CommentThreadData>> loadCommentThreads(CollaborationContext context) {
        return inChannel(context, c -> List.copyOf(c.threads.values()));
    }

    @Override
    public CompletableFuture<List<SuggestionData>> loadSuggestions(CollaborationContext context) {
        return inChannel(context, c -> List.copyOf(c.suggestions.values()));
    }

    @Override
    public CompletableFuture<Optional<CommentThreadData>> getCommentThread(CollaborationContext context, String threadId) {
        return inChannel(context, c -> Optional.ofNullable(c.threads.get(threadId)));
    }

    @Override
    public CompletableFuture<Void> addCommentThread(CollaborationContext context, CommentThreadData thread) {
        return run(context, c -> {
            if (c.threads.containsKey(thread.threadId())) {
                throw CollaborationException.invalid("comment thread " + thread.threadId() + " already exists");
            }
            c.threads.put(thread.threadId(), thread);
            rememberAuthor(c, context);
        });
    }

    @Override
    public CompletableFuture<Void> updateCommentThread(CollaborationContext context, String threadId, CommentThreadUpdate update) {
        return run(context, c -> {
            CommentThreadData t = thread(c, threadId);
            c.threads.put(threadId, new CommentThreadData(threadId,
                update.context() != null ? update.context() : t.context(),
                t.resolvedAt(), t.resolvedBy(),
                update.unlinkedAtChanged() ? update.unlinkedAt() : t.unlinkedAt(),
                update.attributes() != null ? update.attributes() : t.attributes(),
                t.comments()));
        });
    }

    @Override
    public CompletableFuture<Void> resolveCommentThread(CollaborationContext context, String threadId,
                                                        Instant resolvedAt, String resolvedBy) {
        return run(context, c -> {
            CommentThreadData t = thread(c, threadId);
            c.threads.put(threadId, new CommentThreadData(threadId, t.context(), resolvedAt, resolvedBy,
                t.unlinkedAt(), t.attributes(), t.comments()));
        });
    }

    @Override
    public CompletableFuture<Void> reopenCommentThread(CollaborationContext context, String threadId) {
        return run(context, c -> {
            CommentThreadData t = thread(c, threadId);
            c.threads.put(threadId, new CommentThreadData(threadId, t.context(), null, null,
                t.unlinkedAt(), t.attributes(), t.comments()));
        });
    }

    @Override
    public CompletableFuture<Void> removeCommentThread(CollaborationContext context, String threadId) {
        return run(context, c -> {
            thread(c, threadId);
            c.threads.remove(threadId);
        });
    }

    @Override
    public CompletableFuture<Void> addComment(CollaborationContext context, String threadId, CommentData comment) {
        return run(context, c -> {
            CommentThreadData t = c.threads.get(threadId);
            if (t == null) {
                // 修订建议的讨论：CKEditor 不调用 addCommentThread，线程 id 即建议 id
                if (!c.suggestions.containsKey(threadId)) {
                    throw CollaborationException.notFound("comment thread " + threadId);
                }
                t = new CommentThreadData(threadId, null, null, null, null, null, List.of());
            }
            if (t.comment(comment.commentId()).isPresent()) {
                throw CollaborationException.invalid("comment " + comment.commentId() + " already exists");
            }
            List<CommentData> comments = new ArrayList<>(t.comments());
            comments.add(comment);
            c.threads.put(threadId, new CommentThreadData(threadId, t.context(), t.resolvedAt(), t.resolvedBy(),
                t.unlinkedAt(), t.attributes(), comments));
            rememberAuthor(c, context);
        });
    }

    @Override
    public CompletableFuture<Void> updateComment(CollaborationContext context, String threadId, CommentData comment) {
        return run(context, c -> replaceComments(c, threadId, comment.commentId(), () -> comment));
    }

    @Override
    public CompletableFuture<Void> removeComment(CollaborationContext context, String threadId, String commentId) {
        return run(context, c -> replaceComments(c, threadId, commentId, () -> null));
    }

    @Override
    public CompletableFuture<Optional<SuggestionData>> getSuggestion(CollaborationContext context, String id) {
        return inChannel(context, c -> Optional.ofNullable(c.suggestions.get(id)));
    }

    @Override
    public CompletableFuture<Void> addSuggestion(CollaborationContext context, SuggestionData suggestion) {
        return run(context, c -> {
            if (c.suggestions.containsKey(suggestion.id())) {
                throw CollaborationException.invalid("suggestion " + suggestion.id() + " already exists");
            }
            c.suggestions.put(suggestion.id(), suggestion);
            rememberAuthor(c, context);
        });
    }

    @Override
    public CompletableFuture<Void> updateSuggestion(CollaborationContext context, String id, SuggestionUpdate update) {
        return run(context, c -> {
            SuggestionData s = c.suggestions.get(id);
            if (s == null) {
                throw CollaborationException.notFound("suggestion " + id);
            }
            c.suggestions.put(id, new SuggestionData(id, s.type(), s.authorId(), s.createdAt(),
                update.hasComments() != null ? update.hasComments() : s.hasComments(),
                s.data(),
                update.attributes() != null ? update.attributes() : s.attributes(),
                s.originalSuggestionId(),
                update.state() != null ? update.state() : s.state()));
        });
    }

    // ---------------------------------------------------------------- helpers

    private static CommentThreadData thread(Channel c, String threadId) {
        CommentThreadData t = c.threads.get(threadId);
        if (t == null) {
            throw CollaborationException.notFound("comment thread " + threadId);
        }
        return t;
    }

    /** Replaces (supplier result non-null) or removes (null) one comment of a thread. */
    private static void replaceComments(Channel c, String threadId, String commentId, Supplier<CommentData> replacement) {
        CommentThreadData t = thread(c, threadId);
        if (t.comment(commentId).isEmpty()) {
            throw CollaborationException.notFound("comment " + commentId);
        }
        CommentData next = replacement.get();
        List<CommentData> comments = new ArrayList<>();
        for (CommentData comment : t.comments()) {
            if (!comment.commentId().equals(commentId)) {
                comments.add(comment);
            } else if (next != null) {
                comments.add(next);
            }
        }
        c.threads.put(threadId, new CommentThreadData(threadId, t.context(), t.resolvedAt(), t.resolvedBy(),
            t.unlinkedAt(), t.attributes(), comments));
    }

    /** Authors stay loadable after a restart of the editor: remember the user who wrote something. */
    private static void rememberAuthor(Channel c, CollaborationContext context) {
        c.users.putIfAbsent(context.user().id(), context.user());
    }
}
