package com.wontlost.ckeditor;

import com.wontlost.ckeditor.event.FallbackEvent.FallbackMode;
import com.wontlost.ckeditor.handler.ErrorHandler;
import com.wontlost.ckeditor.handler.HtmlSanitizer;
import com.wontlost.ckeditor.handler.UploadHandler;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Builder for creating VaadinCKEditor instances.
 *
 * <p>This builder provides a fluent API for configuring CKEditor 5 instances
 * with support for presets, custom plugins, dependency resolution, and more.</p>
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Simplest usage - use a preset
 * VaadinCKEditor editor = VaadinCKEditor.withPreset(CKEditorPreset.STANDARD);
 *
 * // Or use the builder directly
 * VaadinCKEditor editor = VaadinCKEditorBuilder.create()
 *     .withPreset(CKEditorPreset.STANDARD)
 *     .build();
 * }</pre>
 *
 * <h2>Custom Plugins</h2>
 * <p>Plugin dependencies are automatically resolved by default:</p>
 * <pre>{@code
 * // IMAGE_CAPTION requires IMAGE - it will be added automatically
 * VaadinCKEditor editor = VaadinCKEditorBuilder.create()
 *     .withPlugins(CKEditorPlugin.BOLD, CKEditorPlugin.IMAGE_CAPTION)
 *     .withToolbar("bold", "|", "insertImage")
 *     .build();
 * }</pre>
 *
 * <h2>Customizing Presets</h2>
 * <pre>{@code
 * VaadinCKEditor editor = VaadinCKEditorBuilder.create()
 *     .withPreset(CKEditorPreset.BASIC)
 *     .addPlugin(CKEditorPlugin.TABLE)           // Add table support
 *     .removePlugin(CKEditorPlugin.MEDIA_EMBED)  // Remove media embed
 *     .withLanguage("zh-cn")                     // Chinese UI
 *     .withTheme(CKEditorTheme.DARK)             // Dark theme
 *     .build();
 * }</pre>
 *
 * <h2>View-Only Mode</h2>
 * <pre>{@code
 * // For displaying content without editing
 * VaadinCKEditor viewer = VaadinCKEditorBuilder.create()
 *     .withPreset(CKEditorPreset.BASIC)
 *     .withValue("<p>Content to display</p>")
 *     .withViewOnly()  // Read-only + hidden toolbar
 *     .build();
 * }</pre>
 *
 * <h2>Enterprise Features</h2>
 * <pre>{@code
 * VaadinCKEditor editor = VaadinCKEditorBuilder.create()
 *     .withPreset(CKEditorPreset.FULL)
 *     .withErrorHandler(error -> {
 *         // Custom error handling
 *         logger.error("Editor error: " + error.getMessage());
 *         return false; // Let event propagate
 *     })
 *     .withHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT))
 *     .withUploadHandler((context, stream) -> {
 *         // Handle file uploads
 *         String url = saveFile(context.getFileName(), stream);
 *         return CompletableFuture.completedFuture(new UploadResult(url));
 *     })
 *     .withFallbackMode(FallbackMode.TEXTAREA)
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>Builder instances are not thread-safe. Create a new builder for each editor.</p>
 *
 * @see VaadinCKEditor
 * @see CKEditorPreset
 * @see CKEditorPluginDependencies
 * @see DependencyMode
 */
public class VaadinCKEditorBuilder {

    /**
     * 语言代码正则表达式（ISO 639-1 或带区域的格式，如 "en", "zh-cn", "pt-br"）
     */
    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}(-[a-z]{2})?$");

    /**
     * 工具栏分隔符
     */
    private static final String TOOLBAR_SEPARATOR = "|";

    private final VaadinCKEditor editor;
    private CKEditorPreset preset;
    private final Set<CKEditorPlugin> additionalPlugins = new LinkedHashSet<>();
    private final Set<CKEditorPlugin> removedPlugins = new HashSet<>();
    private final Set<CustomPlugin> customPlugins = new LinkedHashSet<>();
    private String[] toolbar;
    private CKEditorConfig config;
    private DependencyMode dependencyMode = DependencyMode.AUTO_RESOLVE;
    private boolean hideToolbar = false;

    /**
     * Dependency resolution mode for plugins
     */
    public enum DependencyMode {
        /**
         * Automatically resolve all plugin dependencies (default).
         * Missing dependencies are automatically added.
         */
        AUTO_RESOLVE,

        /**
         * Resolve dependencies and include recommended plugins.
         * Provides the most complete feature set.
         */
        AUTO_RESOLVE_WITH_RECOMMENDED,

        /**
         * Validate dependencies but don't auto-add them.
         * Throws exception if dependencies are missing.
         */
        STRICT,

        /**
         * No dependency checking. Use plugins exactly as specified.
         * May cause runtime errors if dependencies are missing.
         */
        MANUAL
    }

    /**
     * Private constructor, use {@link #create()} to get a builder instance.
     */
    VaadinCKEditorBuilder() {
        this.editor = new VaadinCKEditor();
    }

    /**
     * Create a new builder instance.
     *
     * @return a new builder
     */
    public static VaadinCKEditorBuilder create() {
        return new VaadinCKEditorBuilder();
    }

    /**
     * Use preset configuration
     *
     * @param preset the preset to use
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withPreset(CKEditorPreset preset) {
        this.preset = preset;
        return this;
    }

    /**
     * Set plugin list (override preset)
     *
     * @param plugins the plugins to use
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withPlugins(CKEditorPlugin... plugins) {
        editor.clearPlugins();
        editor.addPluginsInternal(Arrays.asList(plugins));
        return this;
    }

    /**
     * Add plugin
     *
     * @param plugin the plugin to add
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder addPlugin(CKEditorPlugin plugin) {
        additionalPlugins.add(plugin);
        return this;
    }

    /**
     * Add multiple plugins
     *
     * @param plugins the plugins to add
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder addPlugins(CKEditorPlugin... plugins) {
        additionalPlugins.addAll(Arrays.asList(plugins));
        return this;
    }

    /**
     * Remove plugin
     *
     * @param plugin the plugin to remove
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder removePlugin(CKEditorPlugin plugin) {
        removedPlugins.add(plugin);
        return this;
    }

    /**
     * Add custom plugin
     *
     * @param plugin the custom plugin to add
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder addCustomPlugin(CustomPlugin plugin) {
        customPlugins.add(plugin);
        return this;
    }

    /**
     * Set toolbar items.
     *
     * <p>Toolbar items can include:</p>
     * <ul>
     *   <li>Plugin command names (e.g., "bold", "italic", "insertTable")</li>
     *   <li>Separator "|" to create visual groupings</li>
     *   <li>Line break "-" to wrap to next line</li>
     * </ul>
     *
     * @param items the toolbar items (must not be null or empty)
     * @return this builder for chaining
     * @throws IllegalArgumentException if items is null or empty
     */
    public VaadinCKEditorBuilder withToolbar(String... items) {
        validateToolbar(items);
        this.toolbar = items;
        return this;
    }

    /**
     * Set editor type
     *
     * @param type the editor type
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withType(CKEditorType type) {
        editor.setEditorTypeInternal(type);
        return this;
    }

    /**
     * Set theme mode.
     *
     * <p>Available modes:</p>
     * <ul>
     *   <li>{@link CKEditorTheme#AUTO} (default) - Automatically syncs with Vaadin's Lumo theme.
     *       When Vaadin switches between light/dark mode, CKEditor follows automatically.
     *       Also respects OS-level dark mode preference if Vaadin doesn't have an explicit theme.</li>
     *   <li>{@link CKEditorTheme#LIGHT} - Forces light theme regardless of Vaadin/OS settings.</li>
     *   <li>{@link CKEditorTheme#DARK} - Forces dark theme regardless of Vaadin/OS settings.</li>
     * </ul>
     *
     * @param theme the theme mode to use
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withTheme(CKEditorTheme theme) {
        editor.setThemeInternal(theme);
        return this;
    }

    /**
     * Set language for editor UI and spell checking.
     *
     * <p>Supported formats:</p>
     * <ul>
     *   <li>ISO 639-1 two-letter code: "en", "zh", "ja", "ko"</li>
     *   <li>With region: "en-us", "zh-cn", "pt-br"</li>
     * </ul>
     *
     * @param language the language code (e.g., "en", "zh-cn")
     * @return this builder for chaining
     * @throws IllegalArgumentException if language format is invalid
     */
    public VaadinCKEditorBuilder withLanguage(String language) {
        validateLanguageCode(language);
        editor.setLanguageInternal(language);
        return this;
    }

    /**
     * Set fallback mode for graceful degradation.
     *
     * @param mode 降级模式
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withFallbackMode(FallbackMode mode) {
        editor.setFallbackModeInternal(mode);
        return this;
    }

    /**
     * Set error handler for custom error handling.
     *
     * @param handler 错误处理器
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withErrorHandler(ErrorHandler handler) {
        editor.setErrorHandlerInternal(handler);
        return this;
    }

    /**
     * Set HTML sanitizer for content cleaning.
     *
     * @param sanitizer HTML 清理器
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withHtmlSanitizer(HtmlSanitizer sanitizer) {
        editor.setHtmlSanitizerInternal(sanitizer);
        return this;
    }

    /**
     * Set upload handler for file uploads.
     *
     * @param handler 上传处理器
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withUploadHandler(UploadHandler handler) {
        editor.setUploadHandlerInternal(handler);
        return this;
    }

    /**
     * Set upload configuration for file uploads.
     * Configures file size limits and allowed MIME types.
     *
     * <p>Example:</p>
     * <pre>
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withUploadHandler(myHandler)
     *     .withUploadConfig(new UploadHandler.UploadConfig()
     *         .setMaxFileSize(5 * 1024 * 1024) // 5MB
     *         .setAllowedMimeTypes("image/jpeg", "image/png", "image/gif"))
     *     .build();
     * </pre>
     *
     * @param config 上传配置
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withUploadConfig(UploadHandler.UploadConfig config) {
        editor.setUploadConfigInternal(config);
        return this;
    }

    /**
     * Set initial content
     *
     * @param value the initial HTML content
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withValue(String value) {
        editor.setEditorDataInternal(value);
        return this;
    }

    /**
     * Set read-only mode.
     *
     * @param readOnly whether the editor should be read-only
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder readOnly(boolean readOnly) {
        editor.setReadOnlyInternal(readOnly);
        return this;
    }

    /**
     * Convenience method for setting read-only mode.
     * Alias for {@link #readOnly(boolean)}.
     *
     * @param readOnly whether the editor should be read-only
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withReadOnly(boolean readOnly) {
        return readOnly(readOnly);
    }

    /**
     * Configure editor for view-only mode.
     *
     * <p>This is a convenience method that sets:</p>
     * <ul>
     *   <li>Read-only mode enabled</li>
     *   <li>Toolbar hidden</li>
     *   <li>Fallback mode to READ_ONLY</li>
     * </ul>
     *
     * <p>Use this for displaying content without editing capabilities.</p>
     *
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withViewOnly() {
        editor.setReadOnlyInternal(true);
        this.hideToolbar = true;
        editor.setFallbackModeInternal(FallbackMode.READ_ONLY);
        return this;
    }

    /**
     * Set whether to hide the toolbar.
     *
     * @param hide true to hide toolbar, false to show
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withHideToolbar(boolean hide) {
        this.hideToolbar = hide;
        return this;
    }

    /**
     * Enable autosave with callback and waiting time.
     *
     * @param callback the callback to invoke when autosave triggers
     * @param waitingTime waiting time in milliseconds (100-60000)
     * @return this builder
     * @throws IllegalArgumentException if waitingTime is out of range
     */
    public VaadinCKEditorBuilder withAutosave(Consumer<String> callback, int waitingTime) {
        // 使用与 CKEditorConfig.setAutosave 相同的校验逻辑
        if (waitingTime < 100 || waitingTime > 60000) {
            throw new IllegalArgumentException(
                "Autosave waiting time must be between 100 and 60000 milliseconds, got: " + waitingTime);
        }
        editor.setAutosaveInternal(true, waitingTime, callback);
        return this;
    }

    /**
     * Set configuration
     *
     * @param config the editor configuration
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withConfig(CKEditorConfig config) {
        this.config = config;
        return this;
    }

    /**
     * Set width
     *
     * @param width the width (e.g., "100%", "500px")
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withWidth(String width) {
        editor.setWidth(width);
        return this;
    }

    /**
     * Set height
     *
     * @param height the height (e.g., "400px", "100%")
     * @return this builder for chaining
     */
    public VaadinCKEditorBuilder withHeight(String height) {
        editor.setHeight(height);
        return this;
    }

    /**
     * Set dependency resolution mode.
     *
     * <p>Available modes:</p>
     * <ul>
     *   <li>{@link DependencyMode#AUTO_RESOLVE} (default) - Automatically add missing dependencies</li>
     *   <li>{@link DependencyMode#AUTO_RESOLVE_WITH_RECOMMENDED} - Add dependencies and recommended plugins</li>
     *   <li>{@link DependencyMode#STRICT} - Fail if dependencies are missing</li>
     *   <li>{@link DependencyMode#MANUAL} - No dependency checking</li>
     * </ul>
     *
     * @param mode the dependency resolution mode
     * @return this builder
     */
    public VaadinCKEditorBuilder withDependencyMode(DependencyMode mode) {
        this.dependencyMode = mode;
        return this;
    }

    /**
     * Set CKEditor license key for premium features.
     *
     * <p>Premium features require a valid license key from CKEditor.
     * Get your license key at: <a href="https://ckeditor.com/pricing">https://ckeditor.com/pricing</a></p>
     *
     * <p>Usage example:</p>
     * <pre>
     * VaadinCKEditor editor = VaadinCKEditorBuilder.create()
     *     .withPreset(CKEditorPreset.STANDARD)
     *     .withLicenseKey("your-license-key-here")
     *     .addCustomPlugin(CustomPlugin.fromPremium("ExportPdf"))
     *     .build();
     * </pre>
     *
     * <p>Note: You must also install the premium features package in your frontend:</p>
     * <pre>
     * npm install ckeditor5-premium-features
     * </pre>
     *
     * @param licenseKey the CKEditor license key
     * @return this builder for chaining
     * @see CustomPlugin#fromPremium(String)
     */
    public VaadinCKEditorBuilder withLicenseKey(String licenseKey) {
        editor.setLicenseKeyInternal(licenseKey);
        return this;
    }

    /**
     * Build and initialize the configured VaadinCKEditor instance.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *   <li>Collects plugins from preset and additional plugins</li>
     *   <li>Removes any plugins marked for removal</li>
     *   <li>Resolves plugin dependencies based on {@link DependencyMode}</li>
     *   <li>Configures toolbar (custom or from preset)</li>
     *   <li>Applies configuration settings</li>
     *   <li>Initializes the editor component</li>
     * </ol>
     *
     * <p><b>Note:</b> The builder can only be used once. After calling {@code build()},
     * create a new builder for additional editors.</p>
     *
     * @return the fully configured and initialized VaadinCKEditor instance
     * @throws IllegalStateException if {@link DependencyMode#STRICT} is used
     *         and plugin dependencies are not satisfied
     */
    public VaadinCKEditor build() {
        // Collect initial plugins
        Set<CKEditorPlugin> initialPlugins = new LinkedHashSet<>();
        if (preset != null && !editor.hasPlugins()) {
            initialPlugins.addAll(preset.getPlugins());
        }
        initialPlugins.addAll(editor.getPluginsInternal());
        initialPlugins.addAll(additionalPlugins);
        initialPlugins.removeAll(removedPlugins);

        // Process plugins based on dependency mode
        Set<CKEditorPlugin> finalPlugins = processPluginDependencies(initialPlugins);

        // Set final plugins
        editor.clearPlugins();
        editor.addPluginsInternal(finalPlugins);
        editor.addCustomPluginsInternal(customPlugins);

        // Process toolbar
        if (toolbar != null) {
            editor.setToolbarInternal(toolbar);
        } else if (preset != null) {
            editor.setToolbarInternal(preset.getDefaultToolbar());
        }

        // Process configuration
        if (config != null) {
            editor.setConfigInternal(config);
        }
        editor.getConfigInternal().setLanguage(editor.getLanguageInternal());

        // Initialize
        editor.initialize();

        // Apply post-initialization settings
        if (hideToolbar) {
            editor.setHideToolbar(true);
        }

        return editor;
    }

    /**
     * Process plugin dependencies based on the configured mode.
     */
    private Set<CKEditorPlugin> processPluginDependencies(Set<CKEditorPlugin> plugins) {
        switch (dependencyMode) {
            case AUTO_RESOLVE:
                return CKEditorPluginDependencies.resolve(plugins, true);

            case AUTO_RESOLVE_WITH_RECOMMENDED:
                return CKEditorPluginDependencies.resolveWithRecommended(plugins);

            case STRICT:
                validateDependenciesStrict(plugins);
                // Add core plugins even in strict mode
                Set<CKEditorPlugin> withCore = new LinkedHashSet<>(plugins);
                withCore.add(CKEditorPlugin.ESSENTIALS);
                withCore.add(CKEditorPlugin.PARAGRAPH);
                return withCore;

            case MANUAL:
            default:
                return plugins;
        }
    }

    /**
     * Validate that all dependencies are satisfied, throw exception if not.
     */
    private void validateDependenciesStrict(Set<CKEditorPlugin> plugins) {
        Map<CKEditorPlugin, Set<CKEditorPlugin>> missing =
            CKEditorPluginDependencies.validateDependencies(plugins);

        if (!missing.isEmpty()) {
            StringBuilder message = new StringBuilder("Missing plugin dependencies:\n");
            for (Map.Entry<CKEditorPlugin, Set<CKEditorPlugin>> entry : missing.entrySet()) {
                message.append("  - ")
                       .append(entry.getKey().getJsName())
                       .append(" requires: ");
                entry.getValue().forEach(dep ->
                    message.append(dep.getJsName()).append(", "));
                message.setLength(message.length() - 2); // Remove trailing ", "
                message.append("\n");
            }
            throw new IllegalStateException(message.toString());
        }
    }

    /**
     * Get the resolved plugins that would be used with current configuration.
     * Useful for debugging or inspection before building.
     *
     * @return set of plugins that would be included
     */
    public Set<CKEditorPlugin> getResolvedPlugins() {
        Set<CKEditorPlugin> initialPlugins = new LinkedHashSet<>();
        if (preset != null && !editor.hasPlugins()) {
            initialPlugins.addAll(preset.getPlugins());
        }
        initialPlugins.addAll(editor.getPluginsInternal());
        initialPlugins.addAll(additionalPlugins);
        initialPlugins.removeAll(removedPlugins);
        return processPluginDependencies(initialPlugins);
    }

    /**
     * Get missing dependencies for current plugin configuration.
     * Useful for understanding what dependencies would be auto-resolved.
     *
     * @return map of plugins to their missing dependencies
     */
    public Map<CKEditorPlugin, Set<CKEditorPlugin>> getMissingDependencies() {
        Set<CKEditorPlugin> initialPlugins = new LinkedHashSet<>();
        if (preset != null && !editor.hasPlugins()) {
            initialPlugins.addAll(preset.getPlugins());
        }
        initialPlugins.addAll(editor.getPluginsInternal());
        initialPlugins.addAll(additionalPlugins);
        initialPlugins.removeAll(removedPlugins);
        return CKEditorPluginDependencies.validateDependencies(initialPlugins);
    }

    // ==================== Validation Methods ====================

    /**
     * Validate toolbar items.
     *
     * @param items the toolbar items to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateToolbar(String[] items) {
        if (items == null) {
            throw new IllegalArgumentException("Toolbar items must not be null");
        }
        if (items.length == 0) {
            throw new IllegalArgumentException("Toolbar items must not be empty");
        }
        for (String item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Toolbar item must not be null");
            }
            // Allow separators and line breaks
            if (item.equals(TOOLBAR_SEPARATOR) || item.equals("-")) {
                continue;
            }
            // Check for empty or whitespace-only items
            if (item.trim().isEmpty()) {
                throw new IllegalArgumentException("Toolbar item must not be empty or whitespace");
            }
        }
    }

    /**
     * Validate language code format.
     *
     * @param language the language code to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateLanguageCode(String language) {
        if (language == null || language.isEmpty()) {
            throw new IllegalArgumentException("Language code must not be null or empty");
        }
        String normalized = language.toLowerCase(java.util.Locale.ROOT);
        if (!LANGUAGE_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Invalid language code format: '" + language + "'. " +
                "Expected ISO 639-1 format (e.g., 'en', 'zh-cn', 'pt-br')");
        }
    }
}
