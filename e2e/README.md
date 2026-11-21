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
| `value-binding.spec.ts` | 值双向绑定（客户端输入回传服务端、`setValue` 下发、`setValue(null)` 归一化）与只读切换 |
| `ui-languages.spec.ts` | 每个 ckeditor5 翻译包（及 `zh-tw`、大小写混写代码、`sk-SK` / `sr-Latn-RS` / `zh-Hans` / `zh-Hant-TW` 等逐级回退代码）：add-on 接受代码、工具栏标签与字典一致、运行期 `locale.t()` 生效、`lang`/`dir`（ar/fa/he/ku/ug/ur 为 RTL）正确、无控制台错误；另覆盖 `withLocale`、`CKEditorConfig` 设置语言以及显式语言保留内容语言（#151）；期望值取自 e2e 自带的同版本 `ckeditor5` |
| `dark-theme-states.spec.ts` | Lumo 明暗主题下 split button 两半悬停底色一致、激活按钮悬停保持激活态、书签图标对比度 ≥ 3:1 且颜色可配置（#149 / #150） |
| `fullscreen-sidebars.spec.ts` | `/fullscreen-doc` 夹具：文档编辑器 × {无侧栏、minimap、大纲、两者} × {A4_FIT、A4、FULL_WIDTH} × 两种视口——编辑列 = 全屏宽度 − 可见侧栏，页面不与侧栏重叠、侧栏不被压缩，minimap 在全屏中跟随并驱动滚动，大纲可跳转，退出后布局复原；CLASSIC 在 `setWidthFull()` 下的页宽；BALLOON / INLINE 上 `setFullscreenPage` 无作用。大纲是付费插件：布局用例需 `E2E_PREMIUM=1`（sample 安装 ckeditor5-premium-features），跳转另需 `CKEDITOR_LICENSE_KEY`（#156） |
| `fullscreen-width.spec.ts` | `/fullscreen` 夹具（`?page=` / `?page2=` / `?width=`）：默认 A4_FIT 在 1024 / 1600 / 2400px 视口与 200 % 缩放（`deviceScaleFactor: 2`）下页宽 = 编辑列 − 56px 且页边距、最小高度保持 A4 比例，窗口缩放即时生效；A4 = 795.701px；FULL_WIDTH 铺满编辑列；`custom(1000px / 60%)` 含源码视图；两个编辑器各自生效；运行期切换；`html` 覆盖优先；不影响非全屏编辑器（#156） |
| `fullscreen-editor-types.spec.ts` | `/fullscreen-types` 夹具：经 `AI_DOCUMENT` 预设、`withToolbar`、`CKEditorConfig.setToolbar` 三种途径请求全屏时，BALLOON / INLINE 工具栏无全屏按钮、未加载 Fullscreen 插件、`Ctrl+Shift+F` 不进入全屏；CLASSIC / DECOUPLED 的全屏按钮照常进入与退出（#163） |
| `sticky-toolbar.spec.ts` | AppLayout + 铺满 TabSheet（滚动容器在 shadow DOM 中）的 `/sticky-toolbar` 夹具：聚焦后滚动，工具栏贴住滚动容器顶部，滚动事件当帧与稳定后都没有 CKEditor 粘性占位块、工具栏不被设为 `fixed`（#157） |
| `heading-options.spec.ts` | `/heading` 夹具：同级别两个标题选项（独立 model + view 类名）时编辑器正常初始化，`<h1 class>` / `<h4 class>` 往返不丢类名，从下拉应用变体时内容带上类名（#158 / #159） |
| `fullscreen-overflow.spec.ts` | `/fullscreen-doc` 夹具（1366×768）：`custom(1200px)` 与 `html` 覆盖 1200px（A4_FIT / FULL_WIDTH）× {无侧栏、minimap、大纲、两者}——页面与侧栏按边界框互不重叠，放不下时全屏区域横向滚动、两侧侧栏滚到可见，minimap 在可横向滚动时仍同步并驱动滚动；放得下的 `custom(60%)` / A4_FIT / FULL_WIDTH 不出现横向滚动（#156） |
| `code-block-highlighting.spec.ts` | `/code-highlight` 夹具：代码块着色、纯文本块不着色，输入后新代码着色且服务端 `getValue()` 不含高亮 span，撤销 / 重做，切换代码块语言重新着色，运行期开关与换配色，暗色编辑器下 `AUTO` 用 github-dark；Chromium 上测量 100 / 500 / 1500 行代码块的首次着色、按键与重新着色耗时（与未启用对比） |
| `visual-regression.spec.ts` | Pixel-level baselines for the 4 EditorTypes + dark theme (Linux/CI only) |

Each spec runs against Chromium and Firefox.

## 开发模式诊断工具

Playwright 套件跑的是 **production jar**，覆盖不到 dev 模式特有的前端问题
（如 [#120](https://github.com/wontlost-ltd/vaadin-ckeditor/issues/120)：
`frontendHotdeploy=true` 时 UI 无限加载）。`tools/devmode-probe.mjs` 补上这一层：
它依次以三种前端模式启动 sample，验证 4 种编辑器能否挂载并接受输入。

```bash
npm run probe:devmode                  # 依次跑 hotdeploy / prebuilt / default
npm run probe:devmode -- hotdeploy     # 只跑指定模式
npm run probe:devmode -- --json        # 机器可读输出
```

| 模式 | 含义 | 预期 Vite |
|------|------|-----------|
| `hotdeploy` | `frontendHotdeploy=true`，Vite 实时转译 TS | 启动 |
| `prebuilt` | 使用预编译 `dev.bundle` | 不启动 |
| `default` | Vaadin 25 默认行为 | 视版本而定 |

判读要点：`/VAADIN/@id/` 是 Vite dev server 专属路径前缀。

- Vite 未启动时该路径返回 **404 属于预期**，不是缺陷
- **Vite 已启动但该路径仍 404** 才是异常，说明请求没被 dev server 接住
  （常见成因：context path 代理未匹配、Vite 中途退出、前端产物与运行模式错配）

工具会显式标记这一矛盾，并在编辑器不可用时以退出码 1 结束。

> 前置条件：先在仓库根执行 `mvn -DskipTests install`。工具直接用
> `mvn spring-boot:run` 启动 sample，首次运行会触发 npm install，可能耗时数分钟。

## 性能测试

[#137](https://github.com/wontlost-ltd/vaadin-ckeditor/issues/137) 建立的性能套件位于
`perf/`，与功能套件隔离（独立配置 `playwright.perf.config.ts`，仅 Chromium，串行、不重试），
驱动 sample 的 `/perf` 夹具视图（查询参数控制实例数、编辑器类型、预设、minimap、文档体积）。

| Spec | 测量 |
|------|------|
| `init.perf.ts` | 四种类型 × BASIC / STANDARD / FULL 的初始化耗时（`EditorReadyEvent` 的 initTimeMs 与端到端） |
| `multi-instance.perf.ts` | 同页 N = 1 / 5 / 10 / 20 个实例的就绪耗时、GC 后堆与 DOM 节点、边际成本比例 |
| `large-document.perf.ts` | 1 MB 文档 `setValue` 下发、按键回传、报文与文档体积之比、minimap 开关对比、有 ContentChange 监听器时的回传 |
| `lifecycle-leak.perf.ts` | 反复移出 / 放回布局、反复新建实例后的 DOM 节点、事件监听器、堆的每轮增量 |
| `first-load.perf.ts` | 冷启动 JS 传输体积、LCP、TBT（长任务近似）、首屏 UIDL 报文体积 |

服务端开销（会话序列化体积、属性 JSON 体积、序列化往返）由 Java 测试
`ServerFootprintTest` / `SerializationTest` 覆盖，随 `mvn test` 运行。

```bash
npm run test:perf            # 按 perf/budgets.json 判定
npm run test:perf:baseline   # 只记录不判定，用于采集新基线后调整预算
```

每个指标写入 `perf-results/results.json`（可用 `PERF_RESULTS` 覆盖路径），CI 中作为 artifact 上传。
预算集中在 [`perf/budgets.json`](perf/budgets.json)：计时类按本机基线放大 3–6 倍以容纳 CI 机器差异；
泄漏、报文倍数、包体积、扩展比例与机器无关，设得较严。

### 首轮测量发现并已修复的问题

| 指标 | 修复前 | 修复后 | 原因 |
|------|--------|--------|------|
| 每轮卸载残留 DOM 节点 / 监听器（3 个实例） | 264 / 264 | 0 / 0 | 组件断开时跳过 `editor.destroy()`，CKEditor 在 window / document 上的定时器与监听器使整个编辑器无法回收 |
| 每轮每实例堆增长 | 813 KB | 16–20 KB | 同上 |
| 1 MB `setValue` 耗时 | 5466 ms | 1436 ms | 属性与 `executeJs` 各下发一份，客户端 `setData` 执行两次 |
| 下发报文 / 文档体积 | 2.00× | 1.00× | 同上 |
| 回传报文 / 文档体积 | 4.11× | 1.37× | 每次变更分两次 RPC 发送旧内容 + 新内容 + 新内容；超出 1 的部分为 JSON 转义。注册 ContentChange 监听器时，服务端改值后的第一次输入仍显式携带旧内容（约 2.74×），以免被丢弃的上报让服务端镜像过期 |
| 服务端改值后客户端回传 | 4.3 MB | 0（无 ContentChange 监听器时） | contentChange 无论有无监听器都上报整份文档 |
| minimap | 未加载 | 正常渲染 | Minimap 被「需要特殊配置」规则剔除，而该配置正由连接器注入 |
| 会话序列化 | `NotSerializableException` | 可往返 | `CKEditorConfig` 等内部类与处理器接口未实现 `Serializable` |

数值来自 Apple Silicon 本机（Chromium，production jar）；CI 运行器上计时类数值会更大。

## Local run

Prerequisites: Java 21+, Maven, Node 24+, ~300 MB of disk for Playwright browsers.

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
    mcr.microsoft.com/playwright:v1.60.0-jammy bash -c '
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

性能套件由 [`.github/workflows/perf.yml`](../.github/workflows/perf.yml) 运行：
改动连接器、Java 组件或性能套件的 PR、每周一定时、以及手动触发。
