package com.wontlost.sample;

import com.vaadin.flow.component.page.AppShellConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot host for Playwright E2E smoke tests.
 *
 * <p>Not published. Used only as a fixture for the e2e/ Playwright suite.
 * Uses the default Vaadin Lumo theme to avoid needing a custom theme folder.</p>
 */
@SpringBootApplication
public class SampleApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
