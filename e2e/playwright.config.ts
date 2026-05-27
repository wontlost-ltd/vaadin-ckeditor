import { defineConfig, devices } from '@playwright/test';

const SAMPLE_APP_DIR = '../examples/spring-boot-sample';
const SAMPLE_JAR = `${SAMPLE_APP_DIR}/target/vaadin-ckeditor-sample-1.0.0-SNAPSHOT.jar`;
const PORT = process.env.E2E_PORT ?? '8080';
const BASE_URL = `http://localhost:${PORT}`;

export default defineConfig({
    testDir: './tests',
    timeout: 60_000,
    expect: { timeout: 10_000 },
    fullyParallel: false,
    workers: 1,
    reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
    use: {
        baseURL: BASE_URL,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
    },
    webServer: {
        // Boot the production jar. Tests assume `mvn package -Pproduction` ran
        // ahead of time (see README). Avoids paying the npm install cost per run.
        command: `java -jar ${SAMPLE_JAR}`,
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
