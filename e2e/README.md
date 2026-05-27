# E2E Tests

Playwright smoke tests for the Vaadin CKEditor addon. Drives the Spring Boot
sample app at [`examples/spring-boot-sample/`](../examples/spring-boot-sample/)
in a real browser to catch issues that unit tests miss.

## Coverage

| Spec | Verifies |
|------|----------|
| `editor-types.spec.ts` | 4 EditorTypes (classic / balloon / inline / decoupled) all mount under CKEditor 48 and render seed HTML |
| `theme-switch.spec.ts` | Lumo dark attribute applied; v48 `--ck-color-ai-*` tokens injected by `theme-manager.ts` |
| `upload-adapter.spec.ts` | Image upload via toolbar reaches Java `UploadHandler` and editor displays inserted `<img>` |
| `ai-config-migration.spec.ts` | Top-level `initialData` migrates to `root.initialData` and editor reports `state === 'ready'` |
| `collab-strip-initial-data.spec.ts` | `stripInitialDataIfChannelSeeded` writes localStorage seed key on first visit |
| `visual-regression.spec.ts` | Pixel-level baselines for the 4 EditorTypes + dark theme (Linux/CI only) |

Each spec runs against Chromium and Firefox.

## Local run

Prerequisites: Java 21+, Maven, Node 22+, ~300 MB of disk for Playwright browsers.

```bash
# 1. Install the addon locally so the sample can resolve it
mvn -DskipTests install

# 2. Build the sample app (frontend bundle baked into the jar)
cd examples/spring-boot-sample
mvn -Pproduction -DskipTests package

# 3. Install Playwright dependencies (one-time)
cd ../../e2e
npm install
npx playwright install chromium firefox

# 4. Run the suite
npm test
```

The sample app jar is started/stopped automatically by Playwright's `webServer`
configuration (`playwright.config.ts`). To run the server on a non-default
port, set `E2E_PORT=9090 npm test`; Playwright forwards it to Spring Boot via
`--server.port`.

## Visual regression baselines

Baselines live under [`tests/__screenshots__/`](tests/__screenshots__/) and are
pinned to **Linux x86_64** (the CI runner platform). The `visual-regression.spec.ts`
file is skipped on every other platform/arch via
`test.skip(!(process.platform === 'linux' && process.arch === 'x64'), …)` so
local development on macOS / Windows / Linux-arm64 stays green.

### Regenerate baselines

When CKEditor, Lumo, or the addon changes the rendered pixels, regenerate the
PNGs inside a Linux/amd64 container so the output matches CI:

```bash
podman run --rm --platform linux/amd64 \
    -v "$PWD":/work:Z \
    -w /work \
    mcr.microsoft.com/playwright:v1.50.0-jammy bash -c '
        apt-get update -qq && apt-get install -y -qq openjdk-21-jdk-headless curl
        curl -sL https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz | tar xz -C /opt
        export PATH=/opt/apache-maven-3.9.9/bin:$PATH
        mvn -B -ntp install -DskipTests
        cd examples/spring-boot-sample && mvn -B -ntp -DskipTests package -Pproduction
        cd ../../e2e && npm ci
        npx playwright test visual-regression --update-snapshots
    '
```

Then commit the changed PNGs.

## CI

GitHub Actions runs the full pipeline (including visual regression) on every
PR and push to `main`. See [`.github/workflows/e2e.yml`](../.github/workflows/e2e.yml).
