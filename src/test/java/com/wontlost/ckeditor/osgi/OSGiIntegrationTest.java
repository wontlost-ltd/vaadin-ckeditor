package com.wontlost.ckeditor.osgi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;

/**
 * OSGi integration test - verifies hot deployment capability in an OSGi container.
 *
 * <p>Test coverage:</p>
 * <ul>
 *   <li>Bundle installation (INSTALLED or RESOLVED state)</li>
 *   <li>Exported packages correctness (internal package not exported)</li>
 *   <li>Bundle metadata validation</li>
 * </ul>
 *
 * <p><b>Note:</b> Since Vaadin dependencies are not available in the test environment,
 * the bundle cannot fully start (ACTIVE), but installation, metadata, and export
 * configuration correctness can be verified.</p>
 *
 * <p>Run with:</p>
 * <pre>
 * mvn verify -Posgi-test
 * </pre>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OSGiIntegrationTest {

    private static final String BUNDLE_SYMBOLIC_NAME = "com.wontlost.vaadin-ckeditor";
    private static final String EXPECTED_VERSION = "5.0.3";

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() throws MalformedURLException {
        // Locate the built jar file
        File projectJar = new File("target/ckeditor-vaadin-" + EXPECTED_VERSION + ".jar");
        if (!projectJar.exists()) {
            throw new IllegalStateException("Project JAR not found: " + projectJar.getAbsolutePath() +
                ". Run 'mvn package -DskipTests' first.");
        }

        return options(
            // Use Felix as the OSGi framework
            frameworkProperty("felix.bootdelegation.implicit").value("false"),

            // System package exports (base packages required by OSGi)
            systemPackages(
                "javax.annotation;version=1.3.2",
                "javax.inject;version=1"
            ),

            // Install project bundle (using file: URL, do not auto-start)
            bundle(projectJar.toURI().toURL().toString()).noStart(),

            // Install dependency bundle (jsoup)
            mavenBundle("org.jsoup", "jsoup", "1.18.3"),

            // JUnit bundle
            junitBundles(),

            // Clean working directory
            cleanCaches(true)
        );
    }

    /**
     * Test 1: Verify bundle is correctly installed.
     *
     * <p>Since Vaadin dependencies are not available, the bundle being in INSTALLED
     * state is expected. This verifies the bundle format is correct and recognized
     * by the OSGi container.</p>
     */
    @Test
    public void testBundleIsInstalled() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed: " + BUNDLE_SYMBOLIC_NAME, bundle);
        // Bundle should be in INSTALLED or RESOLVED state (depends on dependency availability)
        int state = bundle.getState();
        assertTrue("Bundle should be INSTALLED or RESOLVED, but was: " + state,
            state == Bundle.INSTALLED || state == Bundle.RESOLVED || state == Bundle.ACTIVE);
    }

    /**
     * Test 2: Verify bundle version is correct.
     */
    @Test
    public void testBundleVersion() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        String version = bundle.getVersion().toString();
        assertTrue("Bundle version should be " + EXPECTED_VERSION + ", but was: " + version,
            version.startsWith(EXPECTED_VERSION));
    }

    /**
     * Test 3: Verify exported packages (should not include internal package).
     */
    @Test
    public void testExportedPackages() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        String exportPackage = bundle.getHeaders().get("Export-Package");
        assertNotNull("Export-Package header should exist", exportPackage);

        // Verify public API packages are exported
        assertTrue("Should export com.wontlost.ckeditor",
            exportPackage.contains("com.wontlost.ckeditor"));
        assertTrue("Should export com.wontlost.ckeditor.event",
            exportPackage.contains("com.wontlost.ckeditor.event"));
        assertTrue("Should export com.wontlost.ckeditor.handler",
            exportPackage.contains("com.wontlost.ckeditor.handler"));

        // Verify internal package is NOT exported
        assertFalse("Should NOT export com.wontlost.ckeditor.internal",
            exportPackage.contains("com.wontlost.ckeditor.internal"));
    }

    /**
     * Test 4: Verify bundle can be uninstalled.
     */
    @Test
    public void testBundleUninstall() throws Exception {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        long bundleId = bundle.getBundleId();

        // Uninstall bundle
        bundle.uninstall();
        assertEquals("Bundle should be UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());

        // Verify it can no longer be found by symbolic name
        Bundle uninstalled = findBundle(BUNDLE_SYMBOLIC_NAME);
        assertNull("Bundle should not be found after uninstall", uninstalled);

        // But it can still be retrieved by ID (in UNINSTALLED state)
        Bundle byId = bundleContext.getBundle(bundleId);
        if (byId != null) {
            assertEquals("Bundle by ID should be UNINSTALLED", Bundle.UNINSTALLED, byId.getState());
        }
    }

    /**
     * Test 5: Verify bundle can be updated (simulates hot deployment without starting).
     */
    @Test
    public void testBundleUpdate() throws Exception {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        long bundleId = bundle.getBundleId();

        // Update bundle (using same jar to simulate hot deployment)
        bundle.update();

        // Verify bundle still exists with the same ID
        Bundle updatedBundle = bundleContext.getBundle(bundleId);
        assertNotNull("Bundle should still exist after update", updatedBundle);
        assertEquals("Bundle ID should remain the same", bundleId, updatedBundle.getBundleId());

        // Verify state remains INSTALLED or RESOLVED (due to missing Vaadin dependencies)
        int state = updatedBundle.getState();
        assertTrue("Bundle should remain INSTALLED/RESOLVED after update",
            state == Bundle.INSTALLED || state == Bundle.RESOLVED);
    }

    /**
     * Test 6: Verify Vaadin OSGi Extender marker is present.
     */
    @Test
    public void testVaadinOsgiExtender() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        String vaadinExtender = bundle.getHeaders().get("Vaadin-OSGi-Extender");
        assertEquals("Vaadin-OSGi-Extender should be true", "true", vaadinExtender);
    }

    /**
     * Test 7: Verify bundle can be reinstalled (after another test uninstalls it).
     *
     * <p>Note: This test depends on testBundleUninstall having been executed.
     * Since Pax Exam uses PerClass strategy, all tests run in the same container.</p>
     */
    @Test
    public void testBundleReinstall() throws Exception {
        // If bundle already exists (other test didn't uninstall), uninstall it first
        Bundle existing = findBundle(BUNDLE_SYMBOLIC_NAME);
        if (existing != null && existing.getState() != Bundle.UNINSTALLED) {
            existing.uninstall();
        }

        // Reinstall bundle
        File projectJar = new File("target/ckeditor-vaadin-" + EXPECTED_VERSION + ".jar");
        Bundle reinstalledBundle = bundleContext.installBundle(projectJar.toURI().toURL().toString());
        assertNotNull("Bundle should be reinstalled", reinstalledBundle);

        // Verify state
        int state = reinstalledBundle.getState();
        assertTrue("Reinstalled bundle should be INSTALLED or RESOLVED",
            state == Bundle.INSTALLED || state == Bundle.RESOLVED);
    }

    /**
     * Helper method: find bundle by symbolic name.
     */
    private Bundle findBundle(String symbolicName) {
        for (Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName()) &&
                bundle.getState() != Bundle.UNINSTALLED) {
                return bundle;
            }
        }
        return null;
    }

    /**
     * Helper method: find bundle, reinstall if missing or uninstalled.
     */
    private Bundle findOrReinstallBundle() {
        Bundle bundle = findBundle(BUNDLE_SYMBOLIC_NAME);
        if (bundle == null) {
            try {
                File projectJar = new File("target/ckeditor-vaadin-" + EXPECTED_VERSION + ".jar");
                bundle = bundleContext.installBundle(projectJar.toURI().toURL().toString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to reinstall bundle", e);
            }
        }
        return bundle;
    }
}
