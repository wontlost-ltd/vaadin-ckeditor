package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Editor ready event.
 * Fired when the CKEditor instance is fully initialized and ready to accept user input.
 *
 * <p>Usage example:</p>
 * <pre>
 * editor.addEditorReadyListener(event -&gt; {
 *     // Editor is ready, safe to perform operations
 *     event.getSource().focus();
 * });
 * </pre>
 */
public class EditorReadyEvent extends ComponentEvent<VaadinCKEditor> {

    private final long initializationTimeMs;

    /**
     * Create an editor ready event.
     *
     * @param source the editor component that fired the event
     * @param fromClient whether the event originated from the client
     * @param initializationTimeMs editor initialization time in milliseconds
     */
    public EditorReadyEvent(VaadinCKEditor source, boolean fromClient, long initializationTimeMs) {
        super(source, fromClient);
        this.initializationTimeMs = initializationTimeMs;
    }

    /**
     * Get the editor initialization time.
     *
     * @return initialization time in milliseconds
     */
    public long getInitializationTimeMs() {
        return initializationTimeMs;
    }
}
