package com.wontlost.ckeditor.collaboration;

import java.io.Serializable;
import java.util.Objects;

/**
 * A user known to the CKEditor {@code Users} plugin.
 *
 * @param id     the user id, used as {@code authorId} of comments and suggestions
 * @param name   the display name
 * @param avatar URL of the avatar image, or {@code null} for CKEditor's initials avatar
 * @since 6.2.0
 */
public record CollaborationUser(String id, String name, String avatar) implements Serializable {

    /**
     * @throws NullPointerException if {@code id} or {@code name} is null
     */
    public CollaborationUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (id.isBlank()) {
            throw new IllegalArgumentException("User id must not be blank");
        }
    }

    /**
     * Creates a user without an avatar.
     *
     * @param id   the user id
     * @param name the display name
     */
    public CollaborationUser(String id, String name) {
        this(id, name, null);
    }
}
