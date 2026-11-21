package com.wontlost.ckeditor.collaboration;

import java.util.EnumSet;
import java.util.Set;

/**
 * Permissions of the CKEditor {@code Permissions} plugin
 * (<a href="https://ckeditor.com/docs/ckeditor5/latest/features/collaboration/users.html#user-permissions">User permissions</a>).
 * The bridge sends them to the editor (which adapts its UI) and enforces them on the server for every
 * adapter call, so a modified client cannot bypass them.
 *
 * @since 6.2.0
 */
public enum CollaborationPermission {

    /** Modify the document content (and thereby add, accept and discard suggestions), resolve any thread. */
    DOCUMENT_WRITE("document:write"),

    /** Create, edit and remove own comments, create and remove own threads, resolve any thread. */
    COMMENT_WRITE("comment:write"),

    /** Resolve and remove threads created by other users; implies {@link #COMMENT_WRITE}. */
    COMMENT_ADMIN("comment:admin"),

    /** Edit and remove comments created by other users; implies {@link #COMMENT_WRITE}. */
    COMMENT_MODIFY_ALL("comment:modify_all");

    /** CKEditor's default for a user without explicit permissions: document:write, comment:write, comment:admin. */
    public static final Set<CollaborationPermission> DEFAULTS =
        Set.copyOf(EnumSet.of(DOCUMENT_WRITE, COMMENT_WRITE, COMMENT_ADMIN));

    private final String jsName;

    CollaborationPermission(String jsName) {
        this.jsName = jsName;
    }

    /**
     * @return the permission name used by CKEditor, such as {@code comment:write}
     */
    public String getJsName() {
        return jsName;
    }

    /**
     * Whether a permission set grants this permission, taking the CKEditor implications into account
     * ({@code comment:admin} and {@code comment:modify_all} include {@code comment:write}).
     *
     * @param granted the granted permissions
     * @return true if granted
     */
    public boolean isGrantedBy(Set<CollaborationPermission> granted) {
        if (granted.contains(this)) {
            return true;
        }
        return this == COMMENT_WRITE && (granted.contains(COMMENT_ADMIN) || granted.contains(COMMENT_MODIFY_ALL));
    }
}
