package com.wontlost.ckeditor.collaboration;

import tools.jackson.databind.JsonNode;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A track changes suggestion, as stored (CKEditor {@code SuggestionData}).
 *
 * @param id                   the suggestion id
 * @param type                 {@code insertion}, {@code deletion}, {@code formatInline}, {@code formatBlock}
 *                             or {@code attribute} (CKEditor may add a sub-type, e.g. {@code attribute:bold|…})
 * @param authorId             the author's user id, always set by the server
 * @param createdAt            the creation time, always set by the server
 * @param hasComments          whether the suggestion has a comment thread with comments
 * @param data                 additional suggestion data ({@code null} for insertions and deletions)
 * @param attributes           custom attributes ({@code {}} if none)
 * @param originalSuggestionId the suggestion this one was split from, or {@code null}
 * @param state                {@code open}, {@code accepted} or {@code rejected}
 * @since 6.2.0
 */
public record SuggestionData(String id, String type, String authorId, Instant createdAt, boolean hasComments,
                             JsonNode data, JsonNode attributes, String originalSuggestionId,
                             String state) implements Serializable {

    /**
     * @throws NullPointerException if the id, type, author or time is null
     */
    public SuggestionData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(authorId, "authorId");
        Objects.requireNonNull(createdAt, "createdAt");
        data = data == null || data.isNull() ? null : data.deepCopy();
        attributes = Json.objectOrEmpty(attributes);
        state = state == null ? "open" : state;
    }
}
