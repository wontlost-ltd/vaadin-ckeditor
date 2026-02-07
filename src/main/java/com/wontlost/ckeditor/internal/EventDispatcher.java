package com.wontlost.ckeditor.internal;

import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.event.*;
import com.wontlost.ckeditor.event.ContentChangeEvent.ChangeSource;
import com.wontlost.ckeditor.event.EditorErrorEvent.EditorError;
import com.wontlost.ckeditor.event.FallbackEvent.FallbackMode;
import com.wontlost.ckeditor.handler.ErrorHandler;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Internal class for managing editor event dispatching.
 * Provides type-safe event registration and dispatch mechanism.
 *
 * <p>This class is an internal API and should not be used directly by external code.</p>
 */
public class EventDispatcher {

    private static final Logger logger = Logger.getLogger(EventDispatcher.class.getName());

    private final VaadinCKEditor source;
    private ErrorHandler errorHandler;

    // Use CopyOnWriteArrayList for thread safety
    private final List<ComponentEventListener<EditorReadyEvent>> readyListeners = new CopyOnWriteArrayList<>();
    private final List<ComponentEventListener<EditorErrorEvent>> errorListeners = new CopyOnWriteArrayList<>();
    private final List<ComponentEventListener<AutosaveEvent>> autosaveListeners = new CopyOnWriteArrayList<>();
    private final List<ComponentEventListener<ContentChangeEvent>> contentChangeListeners = new CopyOnWriteArrayList<>();
    private final List<ComponentEventListener<FallbackEvent>> fallbackListeners = new CopyOnWriteArrayList<>();

    /**
     * Create an event dispatcher.
     *
     * @param source the event source component
     */
    public EventDispatcher(VaadinCKEditor source) {
        this.source = source;
    }

    /**
     * Set the error handler.
     *
     * @param errorHandler the error handler
     */
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Get the error handler.
     *
     * @return the error handler
     */
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    // ==================== Listener Registration ====================

    /**
     * Add an editor ready listener.
     *
     * @param listener the listener
     * @return a registration handle for removing the listener
     */
    public Registration addEditorReadyListener(ComponentEventListener<EditorReadyEvent> listener) {
        readyListeners.add(listener);
        return () -> readyListeners.remove(listener);
    }

    /**
     * Add an error listener.
     *
     * @param listener the listener
     * @return a registration handle
     */
    public Registration addEditorErrorListener(ComponentEventListener<EditorErrorEvent> listener) {
        errorListeners.add(listener);
        return () -> errorListeners.remove(listener);
    }

    /**
     * Add an autosave listener.
     *
     * @param listener the listener
     * @return a registration handle
     */
    public Registration addAutosaveListener(ComponentEventListener<AutosaveEvent> listener) {
        autosaveListeners.add(listener);
        return () -> autosaveListeners.remove(listener);
    }

    /**
     * Add a content change listener.
     *
     * @param listener the listener
     * @return a registration handle
     */
    public Registration addContentChangeListener(ComponentEventListener<ContentChangeEvent> listener) {
        contentChangeListeners.add(listener);
        return () -> contentChangeListeners.remove(listener);
    }

    /**
     * Add a fallback mode listener.
     *
     * @param listener the listener
     * @return a registration handle
     */
    public Registration addFallbackListener(ComponentEventListener<FallbackEvent> listener) {
        fallbackListeners.add(listener);
        return () -> fallbackListeners.remove(listener);
    }

    // ==================== Event Firing ====================

    /**
     * Fire an editor ready event.
     *
     * @param initTimeMs initialization time in milliseconds
     */
    public void fireEditorReady(long initTimeMs) {
        EditorReadyEvent event = new EditorReadyEvent(source, true, initTimeMs);
        dispatchEvent(readyListeners, event, "EditorReady");
    }

    /**
     * Fire an error event.
     *
     * @param error the error information
     * @return true if the error was handled by the error handler
     */
    public boolean fireEditorError(EditorError error) {
        // Invoke error handler first
        if (errorHandler != null) {
            try {
                if (errorHandler.handleError(error)) {
                    return true; // Error handled
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in error handler", e);
            }
        }

        // Fire event
        EditorErrorEvent event = new EditorErrorEvent(source, true, error);
        dispatchEvent(errorListeners, event, "EditorError");
        return false;
    }

    /**
     * Fire an autosave event.
     *
     * @param content the saved content
     * @param success whether the save succeeded
     * @param errorMessage error message (null on success)
     */
    public void fireAutosave(String content, boolean success, String errorMessage) {
        AutosaveEvent event = new AutosaveEvent(source, true, content, success, errorMessage);
        dispatchEvent(autosaveListeners, event, "Autosave");
    }

    /**
     * Fire a content change event.
     *
     * @param oldContent the old content
     * @param newContent the new content
     * @param changeSource the source of the change
     */
    public void fireContentChange(String oldContent, String newContent, ChangeSource changeSource) {
        ContentChangeEvent event = new ContentChangeEvent(source, true, oldContent, newContent, changeSource);
        dispatchEvent(contentChangeListeners, event, "ContentChange");
    }

    /**
     * Fire a fallback mode event.
     *
     * @param mode the fallback mode
     * @param reason the reason for fallback
     * @param originalError the original error
     */
    public void fireFallback(FallbackMode mode, String reason, String originalError) {
        FallbackEvent event = new FallbackEvent(source, true, mode, reason, originalError);
        dispatchEvent(fallbackListeners, event, "Fallback");
    }

    // ==================== Internal Methods ====================

    private <E extends ComponentEvent<?>> void dispatchEvent(List<ComponentEventListener<E>> listeners, E event, String eventName) {
        for (ComponentEventListener<E> listener : listeners) {
            try {
                listener.onComponentEvent(event);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in " + eventName + " listener", e);
            }
        }
    }

    /**
     * Clean up all listeners.
     */
    public void cleanup() {
        readyListeners.clear();
        errorListeners.clear();
        autosaveListeners.clear();
        contentChangeListeners.clear();
        fallbackListeners.clear();
    }

    /**
     * Get statistics about registered listeners.
     *
     * @return listener statistics
     */
    public ListenerStats getListenerStats() {
        return new ListenerStats(
            readyListeners.size(),
            errorListeners.size(),
            autosaveListeners.size(),
            contentChangeListeners.size(),
            fallbackListeners.size()
        );
    }

    /**
     * Listener statistics.
     */
    public static class ListenerStats {
        public final int ready;
        public final int error;
        public final int autosave;
        public final int contentChange;
        public final int fallback;

        ListenerStats(int ready, int error, int autosave, int contentChange, int fallback) {
            this.ready = ready;
            this.error = error;
            this.autosave = autosave;
            this.contentChange = contentChange;
            this.fallback = fallback;
        }

        public int total() {
            return ready + error + autosave + contentChange + fallback;
        }
    }
}
