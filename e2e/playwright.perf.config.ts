import { defineConfig, devices } from '@playwright/test';

/**
 * 性能套件配置（issue #137），与功能套件 playwright.config.ts 隔离。
 *
 * - 仅 Chromium：内存与 DOM 计数依赖 CDP（Performance / Memory / HeapProfiler 域）；
 * - 串行、单 worker、不重试：并发和重试都会扭曲计时；
 * - 同样驱动 production jar，计时反映用户实际拿到的产物，而非 dev 模式。
 */
const SAMPLE_APP_DIR = '../examples/spring-boot-sample';
const SAMPLE_JAR = `${SAMPLE_APP_DIR}/target/vaadin-ckeditor-sample-1.0.0-SNAPSHOT.jar`;
const PORT = process.env.E2E_PORT ?? '8080';
const BASE_URL = `http://localhost:${PORT}`;

export default defineConfig({
    testDir: './perf',
    testMatch: '**/*.perf.ts',
    timeout: 300_000,
    expect: { timeout: 30_000 },
    fullyParallel: false,
    workers: 1,
    retries: 0,
    reporter: process.env.CI ? [['list'], ['html', { open: 'never', outputFolder: 'perf-report' }]] : 'list',
    use: {
        baseURL: BASE_URL,
        trace: 'off',
        screenshot: 'only-on-failure',
        video: 'off',
    },
    webServer: {
        command: `java -jar ${SAMPLE_JAR} --server.port=${PORT}`,
        url: `${BASE_URL}/classic`,
        timeout: 180_000,
        reuseExistingServer: !process.env.CI,
        stdout: 'pipe',
        stderr: 'pipe',
    },
    projects: [
        { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    ],
});
