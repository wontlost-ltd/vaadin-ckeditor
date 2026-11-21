package com.wontlost.ckeditor.collaboration;

import tools.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.Instant;

/**
 * Changed properties of a comment thread ({@code CommentsAdapter#updateCommentThread}); {@code null} means unchanged.
 *
 * @param context           new context, or {@code null}
 * @param attributes        new attributes, or {@code null}
 * @param unlinkedAtChanged whether {@code unlinkedAt} changed (it may change to {@code null})
 * @param unlinkedAt        the new {@code unlinkedAt}
 * @since 6.2.0
 */
public record CommentThreadUpdate(JsonNode context, JsonNode attributes, boolean unlinkedAtChanged,
                                  Instant unlinkedAt) implements Serializable {
}
