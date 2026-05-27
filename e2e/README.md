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
configuration (`playwright.config.ts`). Override the port via `E2E_PORT=9090`.

## CI

GitHub Actions runs the full pipeline on every PR and push to `main`. See
[`.github/workflows/e2e.yml`](../.github/workflows/e2e.yml).
