package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Autosave event.
 * Fired when editor content is automatically saved.
 *
 * <p>Usage example:</p>
 * <pre>
 * editor.addAutosaveListener(event -&gt; {
 *     String content = event.getContent();
 *     // Save to database or backend service
 *     documentService.save(documentId, content);
 *
 *     if (event.isSuccess()) {
 *         Notification.show("Auto-saved");
 *     }
 * });
 * </pre>
 *
 * <p>Autosave behavior can be configured via the Builder:</p>
 * <pre>
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPreset(CKEditorPreset.STANDARD)
 *     .withAutosave(content -&gt; saveToBackend(content), 3000) // 3-second delay
 *     .build();
 * </pre>
 */
public class AutosaveEvent extends ComponentEvent<VaadinCKEditor> {

    private final String content;
    private final long timestamp;
    private final boolean success;
    private final String errorMessage;

    /**
     * Create a successful autosave event.
     *
     * @param source the editor component that fired the event
     * @param fromClient whether the event originated from the client
     * @param content the saved content
     */
    public AutosaveEvent(VaadinCKEditor source, boolean fromClient, String content) {
        this(source, fromClient, content, true, null);
    }

    /**
     * Create an autosave event.
     *
     * @param source the editor component that fired the event
     * @param fromClient whether the event originated from the client
     * @param content the saved content
     * @param success whether the save succeeded
     * @param errorMessage error message (on failure)
     */
    public AutosaveEvent(VaadinCKEditor source, boolean fromClient, String content,
                        boolean success, String errorMessage) {
        super(source, fromClient);
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Get the saved content.
     *
     * @return the HTML content
     */
    public String getContent() {
        return content;
    }

    /**
     * Get the save timestamp.
     *
     * @return timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Check whether the save succeeded.
     *
     * @return true if the save was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the error message.
     *
     * @return error message, or null if the save succeeded
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
