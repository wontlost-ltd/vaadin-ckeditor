package com.wontlost.ckeditor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for HotDeployConfig class.
 */
class HotDeployConfigTest {

    @AfterEach
    void cleanUpSystemProperties() {
        System.clearProperty(HotDeployConfig.HOT_DEPLOY_DEBUG_PROPERTY);
        System.clearProperty(HotDeployConfig.HOT_DEPLOY_DISABLED_PROPERTY);
        System.clearProperty("vaadin.productionMode");
        System.clearProperty("spring.profiles.active");
    }

    @Test
    @DisplayName("System property names should be defined")
    void systemPropertyNamesShouldBeDefined() {
        assertThat(HotDeployConfig.HOT_DEPLOY_DEBUG_PROPERTY)
            .isEqualTo("vaadin.ckeditor.hotdeploy.debug");
        assertThat(HotDeployConfig.HOT_DEPLOY_DISABLED_PROPERTY)
            .isEqualTo("vaadin.ckeditor.hotdeploy.disabled");
    }

    @Test
    @DisplayName("isHotDeployDisabled should return false by default")
    void isHotDeployDisabledShouldReturnFalseByDefault() {
        // Ensure property is not set
        System.clearProperty(HotDeployConfig.HOT_DEPLOY_DISABLED_PROPERTY);
        assertThat(HotDeployConfig.isHotDeployDisabled()).isFalse();
    }

    @Test
    @DisplayName("isDebugEnabled should return false by default")
    void isDebugEnabledShouldReturnFalseByDefault() {
        // Ensure property is not set
        System.clearProperty(HotDeployConfig.HOT_DEPLOY_DEBUG_PROPERTY);
        assertThat(HotDeployConfig.isDebugEnabled()).isFalse();
    }

    @Test
    @DisplayName("isDevelopmentMode should return true by default")
    void isDevelopmentModeShouldReturnTrueByDefault() {
        // Clear production mode property
        System.clearProperty("vaadin.productionMode");
        System.clearProperty("spring.profiles.active");
        assertThat(HotDeployConfig.isDevelopmentMode()).isTrue();
    }

    @Test
    @DisplayName("getStatus should return status information")
    void getStatusShouldReturnStatusInfo() {
        String status = HotDeployConfig.getStatus();

        assertThat(status)
            .contains("VaadinCKEditor Hot Deploy Status")
            .contains("Initialized:")
            .contains("Disabled:")
            .contains("Debug:")
            .contains("Development Mode:")
            .contains("Version:");
    }

    @Test
    @DisplayName("refreshFrontend should not throw exception")
    void refreshFrontendShouldNotThrow() {
        assertThatCode(HotDeployConfig::refreshFrontend)
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("HotDeployConfig should implement VaadinServiceInitListener")
    void shouldImplementVaadinServiceInitListener() {
        HotDeployConfig config = new HotDeployConfig();
        assertThat(config).isInstanceOf(com.vaadin.flow.server.VaadinServiceInitListener.class);
    }
}
