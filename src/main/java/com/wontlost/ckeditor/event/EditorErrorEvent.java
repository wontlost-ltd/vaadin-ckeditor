package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Editor error event.
 * Fired when the editor encounters an error, including initialization errors, runtime errors, etc.
 *
 * <p>错误码与 {@code recoverable} 契约——{@code recoverable} 表示<b>编辑器实例在该错误之后是否仍可继续使用</b>，
 * 不表示触发错误的那次操作已被重试或补救：</p>
 * <table>
 *   <tr><th>code</th><th>severity</th><th>recoverable</th><th>含义</th></tr>
 *   <tr><td>{@code EDITOR_CREATION_FAILED}</td><td>FATAL</td><td>false</td><td>创建失败，随后进入 fallback 模式</td></tr>
 *   <tr><td>{@code PLUGIN_LOAD_FAILED}</td><td>WARNING</td><td>true</td><td>某插件未能加载，编辑器已不含该插件地创建</td></tr>
 *   <tr><td>{@code EDITOR_RUNTIME_ERROR}</td><td>ERROR</td><td>true</td><td>运行期逃逸出编辑器的异常（CKEditor 49 起无 Watchdog
 *       自动重启；编辑器保留内容与撤销历史并继续运行，但触发异常的那次操作本身未完成）</td></tr>
 * </table>
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
         * @param recoverable 编辑器实例在该错误之后是否仍可继续使用（见类注释的错误码表）
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
