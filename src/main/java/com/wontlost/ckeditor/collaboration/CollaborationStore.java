package com.wontlost.ckeditor.collaboration;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Persistence for comments and suggestions, mirroring CKEditor's {@code CommentsAdapter} and
 * {@code TrackChangesAdapter} (49.x) asynchronously.
 *
 * <p>The {@link CollaborationBridge} calls the store only after it has checked permissions and ownership, and
 * passes data whose {@code authorId} / {@code createdAt} / {@code resolvedBy} / {@code resolvedAt} are already
 * set by the server: a store persists, it does not decide. Data from the browser never reaches a store unchecked.
 * Every method receives the {@link CollaborationContext} (document id and user) for scoping and auditing.</p>
 *
 * <p>Futures may complete on any thread. A future completed exceptionally with a
 * {@link CollaborationException} is reported to the browser with its message; any other failure is reported as
 * a generic error and logged on the server.</p>
 *
 * @see InMemoryCollaborationStore
 * @since 6.2.0
 */
public interface CollaborationStore {

    /**
     * Users to register in the editor ({@code Users#addUser}), at least every author of a stored comment or
     * suggestion. The current user is added by the bridge if missing.
     *
     * @param context the document and current user
     * @return the users
     */
    CompletableFuture<List<CollaborationUser>> loadUsers(CollaborationContext context);

    /**
     * All threads of the document, including resolved and unlinked ones (loaded when the editor starts).
     *
     * @param context the document and current user
     * @return the threads
     */
    CompletableFuture<List<CommentThreadData>> loadCommentThreads(CollaborationContext context);

    /**
     * All suggestions of the document (loaded when the editor starts).
     *
     * @param context the document and current user
     * @return the suggestions
     */
    CompletableFuture<List<SuggestionData>> loadSuggestions(CollaborationContext context);

    /**
     * {@code CommentsAdapter#getCommentThread}.
     *
     * @param context  the document and current user
     * @param threadId the thread id
     * @return the thread, or empty if unknown
     */
    CompletableFuture<Optional<CommentThreadData>> getCommentThread(CollaborationContext context, String threadId);

    /**
     * {@code CommentsAdapter#addCommentThread}: stores a new thread with its first comments.
     *
     * @param context the document and current user
     * @param thread  the thread, authors and times already set
     * @return completes when stored; fails with {@link CollaborationException.Reason#INVALID_REQUEST} if the id exists
     */
    CompletableFuture<Void> addCommentThread(CollaborationContext context, CommentThreadData thread);

    /**
     * {@code CommentsAdapter#updateCommentThread}.
     *
     * @param context  the document and current user
     * @param threadId the thread id
     * @param update   the changed properties
     * @return completes when stored
     */
    CompletableFuture<Void> updateCommentThread(CollaborationContext context, String threadId, CommentThreadUpdate update);

    /**
     * {@code CommentsAdapter#resolveCommentThread}.
     *
     * @param context    the document and current user
     * @param threadId   the thread id
     * @param resolvedAt the server time
     * @param resolvedBy the current user's id
     * @return completes when stored
     */
    CompletableFuture<Void> resolveCommentThread(CollaborationContext context, String threadId,
                                                 Instant resolvedAt, String resolvedBy);

    /**
     * {@code CommentsAdapter#reopenCommentThread}: clears {@code resolvedAt} / {@code resolvedBy}.
     *
     * @param context  the document and current user
     * @param threadId the thread id
     * @return completes when stored
     */
    CompletableFuture<Void> reopenCommentThread(CollaborationContext context, String threadId);

    /**
     * {@code CommentsAdapter#removeCommentThread}.
     *
     * @param context  the document and current user
     * @param threadId the thread id
     * @return completes when removed
     */
    CompletableFuture<Void> removeCommentThread(CollaborationContext context, String threadId);

    /**
     * {@code CommentsAdapter#addComment}. For a suggestion's discussion CKEditor adds the first comment without
     * calling {@code addCommentThread}: the thread has the suggestion's id and must then be created here.
     *
     * @param context  the document and current user
     * @param threadId the thread id
     * @param comment  the comment, author and time already set
     * @return completes when stored
     */
    CompletableFuture<Void> addComment(CollaborationContext context, String threadId, CommentData comment);

    /**
     * {@code CommentsAdapter#updateComment}: replaces the stored comment.
     *
     * @param context  the document and current user
     * @param threadId the thread id
     * @param comment  the updated comment (author and creation time unchanged)
     * @return completes when stored
     */
    CompletableFuture<Void> updateComment(CollaborationContext context, String threadId, CommentData comment);

    /**
     * {@code CommentsAdapter#removeComment}.
     *
     * @param context   the document and current user
     * @param threadId  the thread id
     * @param commentId the comment id
     * @return completes when removed
     */
    CompletableFuture<Void> removeComment(CollaborationContext context, String threadId, String commentId);

    /**
     * {@code TrackChangesAdapter#getSuggestion}.
     *
     * @param context the document and current user
     * @param id      the suggestion id
     * @return the suggestion, or empty if unknown
     */
    CompletableFuture<Optional<SuggestionData>> getSuggestion(CollaborationContext context, String id);

    /**
     * {@code TrackChangesAdapter#addSuggestion}.
     *
     * @param context    the document and current user
     * @param suggestion the suggestion, author and time already set
     * @return completes when stored; fails with {@link CollaborationException.Reason#INVALID_REQUEST} if the id exists
     */
    CompletableFuture<Void> addSuggestion(CollaborationContext context, SuggestionData suggestion);

    /**
     * {@code TrackChangesAdapter#updateSuggestion}.
     *
     * @param context the document and current user
     * @param id      the suggestion id
     * @param update  the changed properties
     * @return completes when stored
     */
    CompletableFuture<Void> updateSuggestion(CollaborationContext context, String id, SuggestionUpdate update);
}
