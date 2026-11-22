package com.wontlost.ckeditor.collaboration;

/**
 * A rejected collaboration request. The message is sent to the browser, where the adapter promise rejects
 * and CKEditor reports the error; it must not contain sensitive data.
 *
 * @since 6.2.0
 */
public class CollaborationException extends RuntimeException {

    /** Why a request was rejected. */
    public enum Reason {
        /** The current user lacks the permission or does not own the comment / thread. */
        PERMISSION_DENIED,
        /** The thread, comment or suggestion does not exist. */
        NOT_FOUND,
        /** The request is malformed or conflicts with stored data (for example a duplicate id). */
        INVALID_REQUEST
    }

    private final Reason reason;

    /**
     * @param reason  why the request was rejected
     * @param message a message safe to show to the user
     */
    public CollaborationException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * @return why the request was rejected
     */
    public Reason getReason() {
        return reason;
    }

    static CollaborationException notFound(String what) {
        return new CollaborationException(Reason.NOT_FOUND, what + " not found");
    }

    static CollaborationException denied(String what) {
        return new CollaborationException(Reason.PERMISSION_DENIED, "Permission denied: " + what);
    }

    static CollaborationException invalid(String what) {
        return new CollaborationException(Reason.INVALID_REQUEST, "Invalid request: " + what);
    }
}
