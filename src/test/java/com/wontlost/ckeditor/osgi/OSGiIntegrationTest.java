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
 * OSGi 集成测试 - 验证 bundle 在 OSGi 容器中的热部署能力。
 *
 * <p>测试内容：</p>
 * <ul>
 *   <li>Bundle 安装（INSTALLED 或 RESOLVED 状态）</li>
 *   <li>导出包的正确性（不导出 internal 包）</li>
 *   <li>Bundle 元数据验证</li>
 * </ul>
 *
 * <p><b>注意：</b>由于 Vaadin 依赖在测试环境中不可用，bundle 无法完全启动（ACTIVE），
 * 但可以验证安装、元数据和导出配置的正确性。</p>
 *
 * <p>运行方式：</p>
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
        // 查找构建的 jar 文件
        File projectJar = new File("target/ckeditor-vaadin-" + EXPECTED_VERSION + ".jar");
        if (!projectJar.exists()) {
            throw new IllegalStateException("Project JAR not found: " + projectJar.getAbsolutePath() +
                ". Run 'mvn package -DskipTests' first.");
        }

        return options(
            // 使用 Felix 作为 OSGi 框架
            frameworkProperty("felix.bootdelegation.implicit").value("false"),

            // 系统包导出（OSGi 需要的基础包）
            systemPackages(
                "javax.annotation;version=1.3.2",
                "javax.inject;version=1"
            ),

            // 安装项目 bundle（使用 file: URL，不自动启动）
            bundle(projectJar.toURI().toURL().toString()).noStart(),

            // 安装依赖 bundle (jsoup)
            mavenBundle("org.jsoup", "jsoup", "1.18.3"),

            // JUnit bundle
            junitBundles(),

            // 清理工作目录
            cleanCaches(true)
        );
    }

    /**
     * 测试 1：验证 bundle 已正确安装
     *
     * <p>由于 Vaadin 依赖不可用，bundle 处于 INSTALLED 状态是正常的。
     * 这验证了 bundle 格式正确，可被 OSGi 容器识别。</p>
     */
    @Test
    public void testBundleIsInstalled() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed: " + BUNDLE_SYMBOLIC_NAME, bundle);
        // Bundle 应该处于 INSTALLED 或 RESOLVED 状态（取决于依赖可用性）
        int state = bundle.getState();
        assertTrue("Bundle should be INSTALLED or RESOLVED, but was: " + state,
            state == Bundle.INSTALLED || state == Bundle.RESOLVED || state == Bundle.ACTIVE);
    }

    /**
     * 测试 2：验证 bundle 版本正确
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
     * 测试 3：验证导出的包（不应包含 internal 包）
     */
    @Test
    public void testExportedPackages() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        String exportPackage = bundle.getHeaders().get("Export-Package");
        assertNotNull("Export-Package header should exist", exportPackage);

        // 验证公共 API 包已导出
        assertTrue("Should export com.wontlost.ckeditor",
            exportPackage.contains("com.wontlost.ckeditor"));
        assertTrue("Should export com.wontlost.ckeditor.event",
            exportPackage.contains("com.wontlost.ckeditor.event"));
        assertTrue("Should export com.wontlost.ckeditor.handler",
            exportPackage.contains("com.wontlost.ckeditor.handler"));

        // 验证 internal 包未导出
        assertFalse("Should NOT export com.wontlost.ckeditor.internal",
            exportPackage.contains("com.wontlost.ckeditor.internal"));
    }

    /**
     * 测试 4：验证 bundle 可以卸载
     */
    @Test
    public void testBundleUninstall() throws Exception {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        long bundleId = bundle.getBundleId();

        // 卸载 bundle
        bundle.uninstall();
        assertEquals("Bundle should be UNINSTALLED", Bundle.UNINSTALLED, bundle.getState());

        // 验证通过符号名找不到了
        Bundle uninstalled = findBundle(BUNDLE_SYMBOLIC_NAME);
        assertNull("Bundle should not be found after uninstall", uninstalled);

        // 但通过 ID 仍能获取（处于 UNINSTALLED 状态）
        Bundle byId = bundleContext.getBundle(bundleId);
        if (byId != null) {
            assertEquals("Bundle by ID should be UNINSTALLED", Bundle.UNINSTALLED, byId.getState());
        }
    }

    /**
     * 测试 5：验证 bundle 可以更新（模拟热部署，不启动）
     */
    @Test
    public void testBundleUpdate() throws Exception {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        long bundleId = bundle.getBundleId();

        // 更新 bundle（使用相同的 jar，模拟热部署）
        bundle.update();

        // 验证 bundle 仍然存在且 ID 不变
        Bundle updatedBundle = bundleContext.getBundle(bundleId);
        assertNotNull("Bundle should still exist after update", updatedBundle);
        assertEquals("Bundle ID should remain the same", bundleId, updatedBundle.getBundleId());

        // 验证状态仍然是 INSTALLED 或 RESOLVED（由于缺少 Vaadin 依赖）
        int state = updatedBundle.getState();
        assertTrue("Bundle should remain INSTALLED/RESOLVED after update",
            state == Bundle.INSTALLED || state == Bundle.RESOLVED);
    }

    /**
     * 测试 6：验证 Vaadin OSGi Extender 标记存在
     */
    @Test
    public void testVaadinOsgiExtender() {
        Bundle bundle = findOrReinstallBundle();
        assertNotNull("Bundle should be installed", bundle);

        String vaadinExtender = bundle.getHeaders().get("Vaadin-OSGi-Extender");
        assertEquals("Vaadin-OSGi-Extender should be true", "true", vaadinExtender);
    }

    /**
     * 测试 7：验证 bundle 可以重新安装（在另一个测试卸载后）
     *
     * <p>注意：此测试依赖于 testBundleUninstall 已执行。
     * 由于 Pax Exam 使用 PerClass 策略，每个测试在同一容器中运行。</p>
     */
    @Test
    public void testBundleReinstall() throws Exception {
        // 如果 bundle 已存在（其他测试未卸载），先卸载
        Bundle existing = findBundle(BUNDLE_SYMBOLIC_NAME);
        if (existing != null && existing.getState() != Bundle.UNINSTALLED) {
            existing.uninstall();
        }

        // 重新安装 bundle
        File projectJar = new File("target/ckeditor-vaadin-" + EXPECTED_VERSION + ".jar");
        Bundle reinstalledBundle = bundleContext.installBundle(projectJar.toURI().toURL().toString());
        assertNotNull("Bundle should be reinstalled", reinstalledBundle);

        // 验证状态
        int state = reinstalledBundle.getState();
        assertTrue("Reinstalled bundle should be INSTALLED or RESOLVED",
            state == Bundle.INSTALLED || state == Bundle.RESOLVED);
    }

    /**
     * 辅助方法：根据符号名查找 bundle
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
     * 辅助方法：查找 bundle，如果不存在或已卸载则重新安装
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
