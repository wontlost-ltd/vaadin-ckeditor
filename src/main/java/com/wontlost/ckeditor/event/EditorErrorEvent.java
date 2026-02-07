package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Editor error event.
 * Fired when the editor encounters an error, including initialization errors, runtime errors, etc.
 *
 * <p>Usage example:</p>
 * <pre>
 * editor.addEditorErrorListener(event -&gt; {
 *     EditorError error = event.getError();
 *     logger.error("Editor error [{}]: {}", error.getCode(), error.getMessage());
 *     if (error.isRecoverable()) {
 *         // Attempt recovery
 *     }
 * });
 * </pre>
 */
public class EditorErrorEvent extends ComponentEvent<VaadinCKEditor> {

    private final EditorError error;

    /**
     * Create an editor error event.
     *
     * @param source the editor component that fired the event
     * @param fromClient whether the event originated from the client
     * @param error the error details
     */
    public EditorErrorEvent(VaadinCKEditor source, boolean fromClient, EditorError error) {
        super(source, fromClient);
        this.error = error;
    }

    /**
     * Get the error details.
     *
     * @return the error object
     */
    public EditorError getError() {
        return error;
    }

    /**
     * Editor error details.
     */
    public static class EditorError {
        private final String code;
        private final String message;
        private final ErrorSeverity severity;
        private final boolean recoverable;
        private final String stackTrace;

        /**
         * Create error details.
         *
         * @param code the error code
         * @param message the error message
         * @param severity the severity level
         * @param recoverable whether the error is recoverable
         * @param stackTrace the stack trace (optional)
         */
        public EditorError(String code, String message, ErrorSeverity severity,
                          boolean recoverable, String stackTrace) {
            this.code = code;
            this.message = message;
            this.severity = severity;
            this.recoverable = recoverable;
            this.stackTrace = stackTrace;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public ErrorSeverity getSeverity() {
            return severity;
        }

        public boolean isRecoverable() {
            return recoverable;
        }

        public String getStackTrace() {
            return stackTrace;
        }

        @Override
        public String toString() {
            return String.format("EditorError[code=%s, severity=%s, message=%s]",
                code, severity, message);
        }
    }

    /**
     * Error severity level.
     */
    public enum ErrorSeverity {
        /** Warning level, does not affect editor functionality */
        WARNING,
        /** Error level, some functionality may be affected */
        ERROR,
        /** Fatal level, editor cannot function properly */
        FATAL
    }
}
