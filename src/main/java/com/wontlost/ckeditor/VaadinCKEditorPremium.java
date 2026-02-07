package com.wontlost.ckeditor;

import com.vaadin.flow.component.dependency.NpmPackage;

/**
 * Premium features enabler for VaadinCKEditor.
 *
 * <p>This class triggers the installation of the {@code ckeditor5-premium-features} npm package
 * when referenced in your application. Premium features require a valid license key from
 * <a href="https://ckeditor.com/pricing">ckeditor.com/pricing</a>.</p>
 *
 * <h2>Usage:</h2>
 * <pre>
 * // Step 1: Enable premium features (call once at application startup)
 * VaadinCKEditorPremium.enable();
 *
 * // Step 2: Use premium plugins in your editor
 * VaadinCKEditor editor = VaadinCKEditor.create()
 *     .withPreset(CKEditorPreset.FULL)
 *     .withLicenseKey("your-license-key")  // Required for premium features
 *     .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
 *     .addCustomPlugin(CustomPlugin.fromPremium("ExportWord"))
 *     .addCustomPlugin(CustomPlugin.fromPremium("FormatPainter"))
 *     .withToolbar("exportPdf", "exportWord", "formatPainter", "|", "bold", "italic")
 *     .build();
 * </pre>
 *
 * <h2>Available Premium Plugins:</h2>
 * <table>
 *   <tr><th>Plugin</th><th>Toolbar Item</th><th>Description</th></tr>
 *   <tr><td>ExportPdf</td><td>exportPdf</td><td>Export content to PDF</td></tr>
 *   <tr><td>ExportWord</td><td>exportWord</td><td>Export content to Word (DOCX)</td></tr>
 *   <tr><td>ImportWord</td><td>importWord</td><td>Import Word documents</td></tr>
 *   <tr><td>FormatPainter</td><td>formatPainter</td><td>Copy and apply formatting</td></tr>
 *   <tr><td>CaseChange</td><td>caseChange</td><td>Change text case</td></tr>
 *   <tr><td>SlashCommand</td><td>-</td><td>Slash command menu (type /)</td></tr>
 *   <tr><td>TableOfContents</td><td>tableOfContents</td><td>Auto-generated TOC</td></tr>
 *   <tr><td>DocumentOutline</td><td>-</td><td>Document structure outline</td></tr>
 *   <tr><td>Template</td><td>insertTemplate</td><td>Content templates</td></tr>
 *   <tr><td>MergeFields</td><td>insertMergeField</td><td>Mail merge fields</td></tr>
 *   <tr><td>Pagination</td><td>-</td><td>Page breaks and pagination</td></tr>
 *   <tr><td>AIAssistant</td><td>aiCommands, aiAssistant</td><td>AI-powered writing assistant</td></tr>
 *   <tr><td>Comments</td><td>comment</td><td>Inline commenting</td></tr>
 *   <tr><td>TrackChanges</td><td>trackChanges</td><td>Track document changes</td></tr>
 *   <tr><td>RevisionHistory</td><td>revisionHistory</td><td>Version history</td></tr>
 *   <tr><td>LineHeight</td><td>lineHeight</td><td>Line height adjustment</td></tr>
 * </table>
 *
 * <h2>License:</h2>
 * <p>Premium features require a commercial license. You can:</p>
 * <ul>
 *   <li>Start a 14-day free trial at <a href="https://ckeditor.com/pricing">ckeditor.com/pricing</a></li>
 *   <li>Purchase a subscription (Essential, Professional, or Enterprise plans)</li>
 * </ul>
 *
 * <h2>Best Practices:</h2>
 * <ul>
 *   <li>Store the license key in environment variables, not in source code</li>
 *   <li>Configure domain whitelist in the CKEditor Portal for production</li>
 *   <li>Call {@link #enable()} in your Application class or main view</li>
 * </ul>
 *
 * <pre>
 * // Recommended: Load license key from environment
 * String licenseKey = System.getenv("CKEDITOR_LICENSE_KEY");
 * editor.withLicenseKey(licenseKey);
 * </pre>
 *
 * @see VaadinCKEditor
 * @see CustomPlugin#fromPremium(String)
 * @since 5.0.0
 */
@NpmPackage(value = "ckeditor5-premium-features", version = "47.4.0")
public final class VaadinCKEditorPremium {

    private static volatile boolean enabled = false;

    /**
     * Private constructor to prevent instantiation.
     */
    private VaadinCKEditorPremium() {
        // Utility class
    }

    /**
     * Ensures the premium npm package is included in the Vaadin build.
     *
     * <p>The actual npm installation is handled by the {@code @NpmPackage} annotation on this
     * class. Calling this method forces the class to be loaded, which makes Vaadin's
     * annotation scanner pick up the annotation. Without this call the class may be
     * pruned by dead-code elimination since nothing else references it.</p>
     *
     * <p>This method is idempotent - calling it multiple times has no additional effect.</p>
     *
     * <h3>Example:</h3>
     * <pre>
     * &#64;SpringBootApplication
     * public class Application implements AppShellConfigurator {
     *     public static void main(String[] args) {
     *         VaadinCKEditorPremium.enable();  // Trigger class loading for @NpmPackage
     *         SpringApplication.run(Application.class, args);
     *     }
     * }
     * </pre>
     *
     * @return {@code true} if this call enabled premium features, {@code false} if already enabled
     */
    public static boolean enable() {
        if (!enabled) {
            synchronized (VaadinCKEditorPremium.class) {
                if (!enabled) {
                    enabled = true;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if premium features have been enabled.
     *
     * @return {@code true} if {@link #enable()} has been called, {@code false} otherwise
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Resets the enabled state. Visible for testing only.
     */
    static synchronized void resetForTesting() {
        enabled = false;
    }

    /**
     * Returns the version of the ckeditor5-premium-features package.
     *
     * @return the npm package version
     */
    public static String getVersion() {
        return "47.4.0";
    }

    /**
     * Enumeration of all available premium plugins.
     *
     * <p>Use with {@link CustomPlugin#fromPremium(String)} to add premium plugins to your editor.</p>
     */
    public enum PremiumPlugin {
        /**
         * Export content to PDF format.
         */
        EXPORT_PDF("ExportPdf", "exportPdf"),

        /**
         * Export content to Word (DOCX) format.
         */
        EXPORT_WORD("ExportWord", "exportWord"),

        /**
         * Import Word documents into the editor.
         */
        IMPORT_WORD("ImportWord", "importWord"),

        /**
         * Copy and apply text formatting.
         */
        FORMAT_PAINTER("FormatPainter", "formatPainter"),

        /**
         * Change text case (upper, lower, title).
         */
        CASE_CHANGE("CaseChange", "caseChange"),

        /**
         * Slash command menu for quick actions.
         * Triggered by typing "/" in the editor.
         */
        SLASH_COMMAND("SlashCommand"),

        /**
         * Auto-generated table of contents.
         */
        TABLE_OF_CONTENTS("TableOfContents", "tableOfContents"),

        /**
         * Document structure outline panel.
         */
        DOCUMENT_OUTLINE("DocumentOutline"),

        /**
         * Content templates for quick insertion.
         */
        TEMPLATE("Template", "insertTemplate"),

        /**
         * Mail merge fields for personalized content.
         */
        MERGE_FIELDS("MergeFields", "insertMergeField"),

        /**
         * Page breaks and pagination support.
         */
        PAGINATION("Pagination"),

        /**
         * AI-powered writing assistant.
         */
        AI_ASSISTANT("AIAssistant", "aiCommands", "aiAssistant"),

        /**
         * Inline commenting and discussions.
         */
        COMMENTS("Comments", "comment"),

        /**
         * Track changes in documents.
         */
        TRACK_CHANGES("TrackChanges", "trackChanges"),

        /**
         * Revision history and version management.
         */
        REVISION_HISTORY("RevisionHistory", "revisionHistory"),

        /**
         * Real-time collaborative editing.
         */
        REAL_TIME_COLLABORATION("RealTimeCollaboration"),

        /**
         * Presence list showing active collaborators.
         */
        PRESENCE_LIST("PresenceList"),

        /**
         * Multi-level list formatting.
         */
        MULTI_LEVEL_LIST("MultiLevelList"),

        /**
         * Line height adjustment for paragraphs.
         */
        LINE_HEIGHT("LineHeight", "lineHeight");

        private final String pluginName;
        private final String[] toolbarItems;

        PremiumPlugin(String pluginName, String... toolbarItems) {
            this.pluginName = pluginName;
            this.toolbarItems = toolbarItems;
        }

        /**
         * Returns the CKEditor plugin class name.
         *
         * @return the plugin name as used in CKEditor configuration
         */
        public String getPluginName() {
            return pluginName;
        }

        /**
         * Returns the toolbar items provided by this plugin.
         *
         * @return array of toolbar item names, or empty array if none
         */
        public String[] getToolbarItems() {
            return toolbarItems.clone();
        }

        /**
         * Creates a CustomPlugin instance for this premium plugin.
         *
         * @return a configured CustomPlugin ready to add to the editor
         */
        public CustomPlugin toCustomPlugin() {
            CustomPlugin.Builder builder = CustomPlugin.builder(pluginName).premium();
            if (toolbarItems.length > 0) {
                builder.withToolbarItems(toolbarItems);
            }
            return builder.build();
        }
    }
}
