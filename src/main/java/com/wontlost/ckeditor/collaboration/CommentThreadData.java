package com.wontlost.ckeditor.collaboration;

import tools.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A comment thread, as stored (CKEditor {@code CommentThreadDataJSON}).
 *
 * @param threadId   the thread id; equals the suggestion id for threads attached to suggestions
 * @param authorId   who created the thread (set by the server; it does not change when comments are removed).
 *                   {@code null} defaults to the author of the first comment
 * @param context    CKEditor's {@code CommentThreadContext} ({@code {type, value}}) or {@code null}
 * @param resolvedAt when the thread was resolved, or {@code null}
 * @param resolvedBy who resolved the thread, or {@code null}
 * @param unlinkedAt when the thread's marker was removed from the content, or {@code null}
 * @param attributes custom attributes ({@code {}} if none)
 * @param comments   the comments, oldest first
 * @since 6.2.0
 */
public record CommentThreadData(String threadId, String authorId, JsonNode context, Instant resolvedAt, String resolvedBy,
                                Instant unlinkedAt, JsonNode attributes,
                                List<CommentData> comments) implements Serializable {

    /**
     * @throws NullPointerException if the id is null
     */
    public CommentThreadData {
        Objects.requireNonNull(threadId, "threadId");
        context = context == null || context.isNull() ? null : context.deepCopy();
        attributes = Json.objectOrEmpty(attributes);
        comments = comments == null ? List.of() : List.copyOf(comments);
        if (authorId == null && !comments.isEmpty()) {
            authorId = comments.get(0).authorId();
        }
    }

    /**
     * @param commentId a comment id
     * @return the comment with that id
     */
    public Optional<CommentData> comment(String commentId) {
        return comments.stream().filter(c -> c.commentId().equals(commentId)).findFirst();
    }
}
