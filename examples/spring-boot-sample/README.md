# Sample app — Vaadin CKEditor E2E fixture

Minimal Spring Boot 4 + Vaadin 25.1 host application used as a fixture for
Playwright E2E smoke tests under [`e2e/`](../../e2e/). **Not published.**

## Routes

| Path | View | Purpose |
|------|------|---------|
| `/classic` | `ClassicView` | ClassicEditor smoke test |
| `/balloon` | `BalloonView` | BalloonEditor smoke test |
| `/inline` | `InlineView` | InlineEditor smoke test |
| `/decoupled` | `DecoupledView` | DecoupledEditor smoke test |
| `/dark` | `DarkThemeView` | Forces Lumo dark theme; verifies v48 AI token injection |
| `/upload` | `UploadView` | Wires a `StubUploadHandler` that returns base64 data URLs |
| `/collab-seed` | `CollabSeedView` | Exercises `stripInitialDataIfChannelSeeded` with `cloudServices` + `collaboration.channelId` + `initialData` |

## Run locally

```bash
# Dev mode (auto-rebuild, but first boot triggers npm install of ckeditor5)
mvn spring-boot:run

# Production build (frontend bundle baked into the jar)
mvn -Pproduction -DskipTests package
java -jar target/vaadin-ckeditor-sample-1.0.0-SNAPSHOT.jar
```

Then open <http://localhost:8080/classic>.

## Notes

- Java 21+ required (Vaadin 25 baseline).
- Depends on the addon `com.wontlost:ckeditor-vaadin` from the local Maven repo;
  run `mvn install` in the repo root first.
- Spring Boot 4.0.4+ is required to align with Jackson 3.1.
