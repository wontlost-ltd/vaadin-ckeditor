import { defineConfig, devices } from '@playwright/test';

const SAMPLE_APP_DIR = '../examples/spring-boot-sample';
const SAMPLE_JAR = `${SAMPLE_APP_DIR}/target/vaadin-ckeditor-sample-1.0.0-SNAPSHOT.jar`;
const PORT = process.env.E2E_PORT ?? '8080';
const BASE_URL = `http://localhost:${PORT}`;
// Pass the port through to Spring Boot so E2E_PORT actually controls the server.
const SAMPLE_COMMAND = `java -jar ${SAMPLE_JAR} --server.port=${PORT}`;

export default defineConfig({
    testDir: './tests',
    timeout: 60_000,
    expect: { timeout: 10_000 },
    fullyParallel: false,
    workers: 1,
    reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
    // Pin visual-regression baselines per platform + browser so font / AA
    // differences between macOS dev and Linux CI don't cause false positives.
    snapshotPathTemplate: '{testDir}/__screenshots__/{testFilePath}/{arg}-{projectName}-{platform}{ext}',
    use: {
        baseURL: BASE_URL,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
    },
    webServer: {
        // Boot the production jar. Tests assume `mvn package -Pproduction` ran
        // ahead of time (see README). Avoids paying the npm install cost per run.
        // E2E_PORT is forwarded to Spring Boot via --server.port.
        command: SAMPLE_COMMAND,
        url: `${BASE_URL}/classic`,
        timeout: 180_000,
        reuseExistingServer: !process.env.CI,
        stdout: 'pipe',
        stderr: 'pipe',
    },
    projects: [
        { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
        { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    ],
});
