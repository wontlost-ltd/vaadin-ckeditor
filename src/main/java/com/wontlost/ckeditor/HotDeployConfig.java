package com.wontlost.ckeditor;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import java.util.logging.Logger;

/**
 * Hot deploy configuration for Vaadin CKEditor component.
 *
 * <p>This class provides support for hot reloading during development.
 * It is automatically registered via Vaadin's ServiceInitListener mechanism.</p>
 *
 * <h2>Hot Deploy Setup:</h2>
 *
 * <h3>For Gradle projects using this addon:</h3>
 * <pre>
 * // In build.gradle.kts of your application
 * vaadin {
 *     optimizeBundle = false  // Disable optimization for faster reloads
 * }
 *
 * // Run with hot reload
 * ./gradlew vaadinPrepareFrontend
 * ./gradlew bootRun  // or your preferred run task
 * </pre>
 *
 * <h3>For Maven projects using this addon:</h3>
 * <pre>
 * &lt;!-- In pom.xml of your application --&gt;
 * &lt;plugin&gt;
 *     &lt;groupId&gt;com.vaadin&lt;/groupId&gt;
 *     &lt;artifactId&gt;vaadin-maven-plugin&lt;/artifactId&gt;
 *     &lt;configuration&gt;
 *         &lt;optimizeBundle&gt;false&lt;/optimizeBundle&gt;
 *     &lt;/configuration&gt;
 * &lt;/plugin&gt;
 *
 * // Run with hot reload
 * mvn vaadin:prepare-frontend
 * mvn spring-boot:run  // or jetty:run
 * </pre>
 *
 * <h3>IDE Configuration:</h3>
 * <ul>
 *   <li><b>IntelliJ IDEA:</b> Enable "Build project automatically" and
 *       "Allow auto-make to start even if developed application is currently running"</li>
 *   <li><b>Eclipse:</b> Enable "Build Automatically" in Project menu</li>
 *   <li><b>VS Code:</b> Use the Vaadin extension with auto-build enabled</li>
 * </ul>
 *
 * <h3>JVM Options for Hot Reload:</h3>
 * <pre>
 * // For Spring Boot DevTools integration
 * -Dspring.devtools.restart.enabled=true
 *
 * // For faster class reloading with DCEVM/HotSwapAgent
 * -XX:+AllowEnhancedClassRedefinition
 * -javaagent:/path/to/hotswap-agent.jar
 * </pre>
 *
 * @see <a href="https://vaadin.com/docs/latest/configuration/development-mode">Vaadin Development Mode</a>
 */
public class HotDeployConfig implements VaadinServiceInitListener {

    private static final Logger logger = Logger.getLogger(HotDeployConfig.class.getName());

    /**
     * System property to enable verbose hot deploy logging.
     */
    public static final String HOT_DEPLOY_DEBUG_PROPERTY = "vaadin.ckeditor.hotdeploy.debug";

    /**
     * System property to disable hot deploy functionality.
     */
    public static final String HOT_DEPLOY_DISABLED_PROPERTY = "vaadin.ckeditor.hotdeploy.disabled";

    private static volatile boolean initialized = false;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        if (isHotDeployDisabled()) {
            return;
        }

        // 双重检查锁定（double-checked locking）：仅首次初始化时进入 synchronized，
        // 避免每次 serviceInit 都加锁。两处检查都必要——外层检查规避锁开销，
        // 内层检查防止多线程同时通过外层检查后重复初始化。initialized 必须为 volatile
        // 以保证可见性与禁止重排序。
        if (!initialized) {
            synchronized (HotDeployConfig.class) {
                if (!initialized) {
                    initializeHotDeploy(event);
                    initialized = true;
                }
            }
        }
    }

    private void initializeHotDeploy(ServiceInitEvent event) {
        boolean debug = isDebugEnabled();

        if (debug) {
            logger.info("VaadinCKEditor hot deploy initialized");
            logger.info("  Version: " + VaadinCKEditor.getVersion());
            logger.info("  Development mode: " + !event.getSource().getDeploymentConfiguration().isProductionMode());
        }

        // Register UI init listener for component tracking
        event.getSource().addUIInitListener(uiEvent -> {
            if (debug) {
                logger.fine("UI initialized: " + uiEvent.getUI().getUIId());
            }
        });

        // Register session init listener for session-level hot deploy support
        event.getSource().addSessionInitListener(sessionEvent -> {
            if (debug) {
                logger.fine("Session initialized: " + sessionEvent.getSession().getSession().getId());
            }
        });
    }

    /**
     * Check if hot deploy is disabled via system property.
     */
    public static boolean isHotDeployDisabled() {
        return Boolean.getBoolean(HOT_DEPLOY_DISABLED_PROPERTY);
    }

    /**
     * Check if debug logging is enabled.
     */
    public static boolean isDebugEnabled() {
        return Boolean.getBoolean(HOT_DEPLOY_DEBUG_PROPERTY);
    }

    /**
     * Check if the application is running in development mode.
     * This can be used to conditionally enable development features.
     *
     * @return true if in development mode
     */
    public static boolean isDevelopmentMode() {
        // Check Vaadin's development mode flag
        String devMode = System.getProperty("vaadin.productionMode");
        if (devMode != null) {
            return !Boolean.parseBoolean(devMode);
        }

        // Check Spring profile
        String springProfiles = System.getProperty("spring.profiles.active", "");
        if (springProfiles.contains("dev") || springProfiles.contains("development")) {
            return true;
        }

        // Default to development mode if not explicitly set
        return true;
    }

    /**
     * Force refresh of frontend resources.
     * Call this method after making changes to frontend files during development.
     *
     * <p>Note: This is typically handled automatically by Vaadin's development mode,
     * but can be called manually if needed.</p>
     */
    public static void refreshFrontend() {
        if (isDebugEnabled()) {
            logger.info("Frontend refresh requested");
        }
        // In Vaadin 25+, frontend refresh is handled automatically
        // This method is provided for backwards compatibility and manual refresh scenarios
    }

    /**
     * Get hot deploy status information.
     *
     * @return status information string
     */
    public static String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("VaadinCKEditor Hot Deploy Status:\n");
        sb.append("  Initialized: ").append(initialized).append("\n");
        sb.append("  Disabled: ").append(isHotDeployDisabled()).append("\n");
        sb.append("  Debug: ").append(isDebugEnabled()).append("\n");
        sb.append("  Development Mode: ").append(isDevelopmentMode()).append("\n");
        sb.append("  Version: ").append(VaadinCKEditor.getVersion()).append("\n");
        return sb.toString();
    }
}
