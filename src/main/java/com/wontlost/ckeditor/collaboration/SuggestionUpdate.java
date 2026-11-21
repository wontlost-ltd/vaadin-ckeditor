package com.wontlost.ckeditor.collaboration;

import tools.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * Changed properties of a suggestion ({@code TrackChangesAdapter#updateSuggestion}); {@code null} means unchanged.
 *
 * @param hasComments new {@code hasComments}, or {@code null}
 * @param attributes  new attributes, or {@code null}
 * @param state       new state ({@code open}, {@code accepted}, {@code rejected}), or {@code null}
 * @since 6.2.0
 */
public record SuggestionUpdate(Boolean hasComments, JsonNode attributes, String state) implements Serializable {
}
