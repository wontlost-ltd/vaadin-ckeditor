package com.wontlost.ckeditor.handler;

import com.wontlost.ckeditor.event.EditorErrorEvent.EditorError;

/**
 * Editor error handler.
 * Used to customize error handling logic such as logging, sending alerts, etc.
 *
 * <h2>Return Value Semantics</h2>
 * <p>The return value of {@link #handleError(EditorError)} determines error propagation behavior:</p>
 * <ul>
 *   <li>{@code true} - Error fully handled, <b>stop propagation</b>, {@code EditorErrorEvent} will not be fired</li>
 *   <li>{@code false} - Error not fully handled, <b>continue propagation</b>, {@code EditorErrorEvent} will be fired</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic usage - log but don't block propagation</h3>
 * <pre>
 * editor.setErrorHandler(error -&gt; {
 *     logger.error("CKEditor error [{}]: {}", error.getCode(), error.getMessage());
 *     return false; // Continue propagation, allow other listeners to handle
 * });
 * </pre>
 *
 * <h3>Conditional handling - only intercept specific errors</h3>
 * <pre>
 * editor.setErrorHandler(error -&gt; {
 *     if ("NETWORK_ERROR".equals(error.getCode())) {
 *         // Auto-retry network errors, don't propagate to user
 *         retryService.scheduleRetry();
 *         return true; // Handled, stop propagation
 *     }
 *     return false; // Continue propagation for other errors
 * });
 * </pre>
 *
 * <h3>Chained handling - compose multiple handlers</h3>
 * <pre>
 * ErrorHandler logger = ErrorHandler.logging(log);
 * ErrorHandler alerter = error -&gt; {
 *     if (error.getSeverity() == ErrorSeverity.FATAL) {
 *         alertService.sendAlert(error.getMessage());
 *         return true;
 *     }
 *     return false;
 * };
 * // Log first, then send alert
 * editor.setErrorHandler(ErrorHandler.compose(logger, alerter));
 * </pre>
 *
 * <h2>Best Practices</h2>
 * <ul>
 *   <li>Loggers should usually return {@code false} to allow other handlers to continue processing</li>
 *   <li>Only return {@code true} when the error is completely resolved (e.g., auto-retry succeeded)</li>
 *   <li>For fatal errors ({@code FATAL}), usually let them propagate so the UI can respond</li>
 *   <li>When using {@link #compose(ErrorHandler...)}, the order of handlers matters</li>
 * </ul>
 *
 * @see EditorError
 * @see com.wontlost.ckeditor.event.EditorErrorEvent
 */
@FunctionalInterface
public interface ErrorHandler {

    /**
     * Handle an editor error.
     *
     * <p>This method is called <b>before</b> {@code EditorErrorEvent} is fired,
     * providing an opportunity to intercept and handle the error.</p>
     *
     * @param error error details including error code, message, severity, etc.
     * @return {@code true} if the error is fully handled, stopping propagation (event not fired);
     *         {@code false} if the error is not handled or needs to continue propagating (fires {@code EditorErrorEvent})
     */
    boolean handleError(EditorError error);

    /**
     * Create a logging error handler.
     *
     * @param logger the logger instance
     * @return an error handler instance
     */
    static ErrorHandler logging(java.util.logging.Logger logger) {
        return error -> {
            switch (error.getSeverity()) {
                case WARNING:
                    logger.warning(() -> String.format("[%s] %s", error.getCode(), error.getMessage()));
                    break;
                case ERROR:
                    logger.severe(() -> String.format("[%s] %s", error.getCode(), error.getMessage()));
                    break;
                case FATAL:
                    logger.severe(() -> String.format("FATAL [%s] %s\n%s",
                        error.getCode(), error.getMessage(), error.getStackTrace()));
                    break;
            }
            return false; // Continue propagation
        };
    }

    /**
     * Compose multiple error handlers.
     *
     * @param handlers the handler list
     * @return a composed handler
     */
    static ErrorHandler compose(ErrorHandler... handlers) {
        return error -> {
            for (ErrorHandler handler : handlers) {
                if (handler.handleError(error)) {
                    return true; // Error handled
                }
            }
            return false;
        };
    }
}
