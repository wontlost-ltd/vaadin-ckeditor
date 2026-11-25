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
| `/language` | `LanguageView` | 界面语言（[#151](https://github.com/wontlost-ltd/vaadin-ckeditor/issues/151)）：`?lang=` → `withLanguage`；`&via=locale` → `withLocale`；`&via=config` → 只用 `CKEditorConfig.setLanguage`；`&content=` 额外设置内容语言与 Text Part Language 选项 |
| `/theme-showcase` | `ThemeShowcaseView` | 加载 Lumo 样式表，`?theme=dark` 切暗色；split button、书签等交互态（[#149](https://github.com/wontlost-ltd/vaadin-ckeditor/issues/149) / [#150](https://github.com/wontlost-ltd/vaadin-ckeditor/issues/150)） |
| `/collab-bridge` | `CollaborationBridgeView` | 评论 / 修订建议持久化桥：`?user=alice\|bob`、`?perm=comment:write,…`、`?reset=1` 重新播种共享内存 store；`#btn-refresh` 把 store 投影到 `#store-state`。premium 插件需 license 或以 `CKEDITOR_PREMIUM_STUB=1` 构建（替身） |
| `/code-highlight` | `CodeHighlightView` | 代码块语法高亮：`?theme=` 配色、`?dark=1` 暗色编辑器、`?off=1` 初始关闭、`?lines=N` 追加 N 行代码块（性能测量）；`#server-value` 投影服务端 `getValue()` |
| `/ckeditor5-export` | `CKEditor5ExportView` | `CustomPlugin.fromCKEditor5` / `of(name, "ckeditor5")` 加载不在静态插件表中的 `ckeditor5` 导出（`Font`） |
| `/full-page` | `FullPageView` | Full page HTML：`fullPageAllowRenderStylesFromHead(true)` 与 `CKEditorPlugin.FULL_PAGE` |
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
