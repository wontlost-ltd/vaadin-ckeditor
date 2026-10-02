# Sample app — Vaadin CKEditor E2E fixture

Minimal Spring Boot 4 + Vaadin 25.2 host application used as a fixture for
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
| `/binding` | `BindingView` | 值双向绑定与只读切换；把服务端 `getValue()` / 事件计数投影到 DOM 供断言 |
| `/perf` | `PerfView` | 性能套件夹具（[#137](https://github.com/wontlost-ltd/vaadin-ckeditor/issues/137)）：查询参数控制实例数 / 类型 / 预设 / minimap / 文档体积，见 `e2e/README.md`「性能测试」 |

## Run locally

```bash
# Dev mode (auto-rebuild, but first boot triggers npm install of ckeditor5)
mvn spring-boot:run

# Dev mode with frontendHotdeploy=true (Vite dev server transpiles TS on the fly,
# instead of using a prebuilt dev bundle — enables CSS/TS hot reload)
mvn -Photdeploy spring-boot:run

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
