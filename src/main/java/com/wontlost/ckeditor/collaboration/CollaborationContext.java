package com.wontlost.ckeditor.collaboration;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

/**
 * Server-side context of a store call: the document (channel) and the user on whose behalf the call is made.
 * Both come from the server-side {@link CollaborationBridge} configuration, never from the browser.
 *
 * @param channelId   the document / channel id
 * @param user        the current user
 * @param permissions the current user's permissions
 * @since 6.2.0
 */
public record CollaborationContext(String channelId, CollaborationUser user,
                                   Set<CollaborationPermission> permissions) implements Serializable {

    /**
     * @throws NullPointerException if any argument is null
     */
    public CollaborationContext {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(user, "user");
        permissions = Set.copyOf(permissions);
    }
}
