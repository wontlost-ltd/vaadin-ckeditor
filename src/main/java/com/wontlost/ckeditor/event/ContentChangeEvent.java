package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Content change event.
 * Fired when the editor content changes, providing the content before and after the change.
 *
 * <p>Usage example:</p>
 * <pre>
 * editor.addContentChangeListener(event -&gt; {
 *     // Calculate difference
 *     int charDiff = event.getNewContent().length() - event.getOldContent().length();
 *     updateCharacterCount(charDiff);
 *
 *     // Mark as unsaved
 *     markAsUnsaved();
 * });
 * </pre>
 *
 * <p>Note: This event differs from Vaadin's ValueChangeListener:</p>
 * <ul>
 *   <li>ContentChangeEvent - fires on every content change (real-time)</li>
 *   <li>ValueChangeListener - fires on blur or sync</li>
 * </ul>
 */
public class ContentChangeEvent extends ComponentEvent<VaadinCKEditor> {

    private final String oldContent;
    private final String newContent;
    private final ChangeSource changeSource;

    /**
     * Create a content change event.
     *
     * @param source the editor component that fired the event
     * @param fromClient whether the event originated from the client
     * @param oldContent the content before the change
     * @param newContent the content after the change
     * @param changeSource the source of the change
     */
    public ContentChangeEvent(VaadinCKEditor source, boolean fromClient,
                              String oldContent, String newContent, ChangeSource changeSource) {
        super(source, fromClient);
        this.oldContent = oldContent;
        this.newContent = newContent;
        this.changeSource = changeSource;
    }

    /**
     * Get the content before the change.
     *
     * @return the old content
     */
    public String getOldContent() {
        return oldContent;
    }

    /**
     * Get the content after the change.
     *
     * @return the new content
     */
    public String getNewContent() {
        return newContent;
    }

    /**
     * Get the source of the change.
     *
     * @return the change source type
     */
    public ChangeSource getChangeSource() {
        return changeSource;
    }

    /**
     * Check whether the content has actually changed.
     *
     * @return true if the content is different
     */
    public boolean hasChanged() {
        if (oldContent == null) {
            return newContent != null;
        }
        return !oldContent.equals(newContent);
    }

    /**
     * Get the content length delta.
     *
     * @return character count change (positive means increase, negative means decrease)
     */
    public int getLengthDelta() {
        int oldLen = oldContent != null ? oldContent.length() : 0;
        int newLen = newContent != null ? newContent.length() : 0;
        return newLen - oldLen;
    }

    /**
     * Content change source.
     */
    public enum ChangeSource {
        /** User input */
        USER_INPUT,
        /** API call (e.g., setValue) */
        API,
        /** Undo/Redo operation */
        UNDO_REDO,
        /** Paste operation */
        PASTE,
        /** Collaborative editing sync */
        COLLABORATION,
        /** Unknown source */
        UNKNOWN
    }
}
