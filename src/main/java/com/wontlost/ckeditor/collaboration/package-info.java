/**
 * Server-side persistence bridge for CKEditor 5 comments and track changes without CKEditor Cloud
 * (the "adapter" integration of the comments and track changes features).
 *
 * <p>{@link com.wontlost.ckeditor.collaboration.CollaborationBridge} is attached to a
 * {@link com.wontlost.ckeditor.VaadinCKEditor}; a custom plugin shipped with the add-on
 * ({@code VaadinCollaborationBridge}) installs a {@code CommentsRepository} adapter and a
 * {@code TrackChanges} adapter in the browser that forward every call to the bridge, which checks
 * permissions, assigns authors and timestamps on the server and persists through a
 * {@link com.wontlost.ckeditor.collaboration.CollaborationStore}.
 * {@link com.wontlost.ckeditor.collaboration.InMemoryCollaborationStore} is a reference implementation
 * for demos and tests.</p>
 *
 * <p>The comments and track changes features are CKEditor premium features and need a licence key with the
 * collaboration add-on. The bridge was built against the documented 49.x adapter API and verified with mock
 * CKEditor plugins only; see {@code docs/PREMIUM_INTEGRATION.md}.</p>
 *
 * @since 6.2.0
 */
package com.wontlost.ckeditor.collaboration;
