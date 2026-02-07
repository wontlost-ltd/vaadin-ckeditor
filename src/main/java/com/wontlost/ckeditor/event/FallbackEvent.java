package com.wontlost.ckeditor.event;

import com.vaadin.flow.component.ComponentEvent;
import com.wontlost.ckeditor.VaadinCKEditor;

/**
 * Fallback event.
 * Fired when the editor triggers a fallback mode due to an error.
 *
 * <p>Usage example:</p>
 * <pre>
 * editor.addFallbackListener(event -&gt; {
 *     if (event.getMode() == FallbackMode.TEXTAREA) {
 *         // Editor has fallen back to a plain textarea
 *         Notification.show("Editor failed to load, switched to basic mode",
 *             Notification.Type.WARNING_MESSAGE);
 *     }
 *
 *     // Log the fallback reason
 *     logger.warn("Editor fallback triggered: {}", event.getReason());
 * });
 * </pre>
 */
public class FallbackEvent extends ComponentEvent<VaadinCKEditor> {

    private final FallbackMode mode;
    private final String reason;
    private final String originalError;

    /**
     * Create a fallback event.
     *
     * @param source the editor component that fired the event
     * @param fromClient whether the event originated from the client
     * @param mode the fallback mode
     * @param reason description of the fallback reason
     * @param originalError the original error message
     */
    public FallbackEvent(VaadinCKEditor source, boolean fromClient,
                        FallbackMode mode, String reason, String originalError) {
        super(source, fromClient);
        this.mode = mode;
        this.reason = reason;
        this.originalError = originalError;
    }

    /**
     * Get the fallback mode.
     *
     * @return the current fallback mode
     */
    public FallbackMode getMode() {
        return mode;
    }

    /**
     * Get the fallback reason.
     *
     * @return a human-readable fallback reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Get the original error message.
     *
     * @return the original error that triggered the fallback, may be null
     */
    public String getOriginalError() {
        return originalError;
    }

    /**
     * Fallback mode.
     */
    public enum FallbackMode {
        /**
         * Fall back to a native textarea.
         * Retains basic editing capability but loses rich text features.
         */
        TEXTAREA("textarea"),

        /**
         * Fall back to read-only mode.
         * Content is visible but not editable.
         */
        READ_ONLY("readonly"),

        /**
         * Display an error message.
         * No editing capability provided.
         */
        ERROR_MESSAGE("error"),

        /**
         * Hide the editor.
         * Nothing is displayed.
         */
        HIDDEN("hidden");

        private final String jsName;

        /**
         * Pre-built jsName to FallbackMode lookup map for O(1) lookup.
         */
        private static final java.util.Map<String, FallbackMode> JS_NAME_MAP;
        static {
            java.util.Map<String, FallbackMode> map = new java.util.HashMap<>();
            for (FallbackMode mode : values()) {
                map.put(mode.jsName, mode);
            }
            JS_NAME_MAP = java.util.Collections.unmodifiableMap(map);
        }

        FallbackMode(String jsName) {
            this.jsName = jsName;
        }

        /**
         * Get the JavaScript-side mode name.
         *
         * @return the JS mode name
         */
        public String getJsName() {
            return jsName;
        }

        /**
         * Parse a mode from its JS name.
         *
         * @param jsName the JavaScript-side mode name
         * @return the corresponding enum value, or ERROR_MESSAGE if not found
         */
        public static FallbackMode fromJsName(String jsName) {
            FallbackMode mode = JS_NAME_MAP.get(jsName);
            return mode != null ? mode : ERROR_MESSAGE;
        }
    }
}
