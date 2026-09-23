# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Vaadin Platform：25.2.6 → 25.3.0**（根 `pom.xml`、`ckeditor-vaadin-testbench/pom.xml`、
  `examples/spring-boot-sample/pom.xml`）。
- **CKEditor 5（`ckeditor5`、`ckeditor5-premium-features`）：48.5.0 → 48.5.2**。
  patch 版本升级。同步更新全部版本锚点：前端 `package.json` 与 `package-lock.json`、
  `@NpmPackage` 注解（`VaadinCKEditor`、`VaadinCKEditorPremium`）、
  `VaadinCKEditorPremium.getVersion()` 返回值、示例应用 `package.json`
  （由 `vaadin:build-frontend` 重新生成）。
  已复核 issue #122 的修复前提在 48.5.2 下依然成立：逐字节比对
  `@ckeditor/ckeditor5-core` 48.5.0 与 48.5.2 的 `dist/index.js`，二者**完全相同**
  （两版本间仅 `package.json` 与 `LICENSE.md` 有差异），故 `Context#_removeEditor`
  与 `Collection#add` 的行为未变，该修复继续必要且有效。

### Fixed
- **Vaadin 25.3.0 的 `vaadin-bom` 不再 import `tools.jackson:jackson-bom`**，
  导致本项目 `tools.jackson.core:jackson-databind`（不写 version，依赖 BOM 管理）
  失去版本来源、Maven 构建直接失败（`'dependencies.dependency.version' ... is missing`）。
  修复方式为在 `dependencyManagement` 中显式 import `tools.jackson:jackson-bom`，
  版本由新增的 `${jackson3.version}` 属性单点控制，取值 3.1.5 —— 与
  `vaadin-core:25.3.0` 传递引入的 Jackson 版本一致，保持跟随 Vaadin 的安全基线，
  同时延续“不在依赖声明处写死版本”的既有约定（避免把消费端拉回旧版）。
  该问题由 Matti Tahvonen 在 PR #129 中独立指出（"Vaadin 25.3 will stop declaring that"），
  本处采用 BOM import 而非在依赖声明处写死版本，两者解析到的版本一致。
- **`frontendHotdeploy=true` 下 UI 无限转圈**（issue #120）——**由 Vaadin 25.3.0 修复，本库无需改动**。
  根因是 rolldown 会把 TS 装饰器降级为 `@oxc-project/runtime` 的辅助模块 import，
  但该包此前**无人声明**（rolldown 只依赖 `@oxc-project/types`），dev server 模式下
  辅助模块 404。Vaadin 25.3.0 起已在生成的 `package.json` 中自行声明
  `@oxc-project/runtime`，该包现已存在于依赖树中。
  已用示例应用的 `-Photdeploy` 档实机验证（Vite 8.3.0 / rolldown 1.2.9）：
  连接器实际发出的 import 为 `.../@oxc-project+runtime@0.150.0/helpers/esm/decorate.js`，
  返回 **200**；浏览器打开 `/classic` 后编辑器正常渲染并可输入，
  无失败请求、无 console 错误。故先前考虑的 `@NpmPackage("@oxc-project/runtime")`
  固定版本方案**不再需要**——那样反而会把版本钉死、与 Vaadin 自带声明冲突。

## [5.4.0] - 2026-09-06

### Changed
- **CKEditor 5（`ckeditor5`、`ckeditor5-premium-features`）：48.4.0 → 48.5.0**。
  minor 版本升级，官方 changelog 的 48.5.0 段落**未列出任何 breaking change**
  （其余 MINOR BREAKING CHANGES 小节均属更早版本）。新增内容集中在 premium 侧：
  AI Chat 的 GHS 感知与 Context Library、Revision History 的 `showAISource`；
  免费侧仅表格缺陷修复（列宽在有纵向滚动条时缩水、嵌套表最后一列 resize 撑满父表、
  GHS 保留 `content-table`/`layout-table` 类）。
  同步更新四处版本锚点：前端 `package.json`、`@NpmPackage` 注解（`VaadinCKEditor`、
  `VaadinCKEditorPremium`）、`getPremiumVersion()` 返回值、示例应用 `package.json`。
  已复核 issue #122 的修复前提在 48.5.0 下依然成立：`Context#_removeEditor` 实现逐字未变，
  `Collection#add` 仍会为无 `.id` 的实例自动生成 `uid()` 并写回（实测 `has()` 在覆盖 id 后
  仍失配），故该修复继续必要且有效。`npm audit` 报告 0 vulnerabilities。

### Fixed
- **换父容器时 `destroy()` 无限递归卡死标签页**（前端，严重，issue #122）。
  连接器在 `Editor.create()` 完成后覆盖了 editor 实例的 `.id`，但该实例在**构造期间**
  就已注册进自己的私有 Context：`Context#_addEditor` → `Collection#add` →
  `_getItemIdBeforeAdding()` 发现新实例没有 `.id`，会自动生成 `uid()` **写回实例**
  并以该值为键存入 `_itemMap`。事后覆盖 `.id` 使实例与其在 `_itemMap` 中的键脱钩：
  `destroy()` → `Context#_removeEditor()` → `editors.has(editor)` 失配 → `remove()`
  从不执行 → 编辑器永远留在 `context.editors` → `_contextOwner === editor` 成立而
  调用 `Context#destroy()` → 后者遍历仍含该编辑器的 `editors` 再次 `destroy()`
  → 无限递归。该递归跨 `await` 边界（`Context#destroy` 用 `Promise.all`），
  因此表现为微任务饥饿、主线程被占满，而**不是** `RangeError` 栈溢出。
  触发场景：编辑器在**保持连接**的前提下被换父（如 Vaadin 在同一次往返内做
  `SplitLayout` 槽位重分配），此路径会真正调用 `editor.destroy()`。
  修复方式是移除该赋值——`editor.id` 在整个代码库中从无读取方，属死代码；
  组件侧 `editorId`（DOM id、只读锁 id、上传 id）契约完全不变，无迁移步骤。
  已对照 CKEditor 48.4.0 源码逐行核实根因，并新增单测与 E2E 双重回归。
- **上传适配器猴补丁从不还原导致编辑器泄漏**（前端，严重）。
  `setupCustomUploadAdapter()` 把自建工厂写到 CKEditor `FileRepository` 插件的
  `createUploadAdapter` 上，该工厂闭包捕获了 `this`（Lit 元素），形成
  「插件 → 闭包 → 组件 → 整棵 DOM 子树」的引用链，而全仓无任何还原代码。
  由于 `destroyEditor()` 在组件已断开时会跳过 `editor.destroy()`（交给 GC），
  CKEditor 自身不拆卸插件，这条链会把整个编辑器钉住——每次路由往返泄漏一个实例。
  修复：捕获原值并登记还原动作到 `listenerCleanups`，该数组在 `destroyEditor()` 的
  `isDisconnected` 提前 return **之前**被无条件遍历执行。还原成 `undefined` 是安全的：
  CKEditor 的 `createLoader()` 有 `if (!this.createUploadAdapter)` 守卫，仅告警返回 null。
  该缺陷与 issue #122 正交，由全仓扫描顺带查出。

### Documentation
- 修正 `MediaEmbedResize` 可用性的描述（`media-embed-resize.ts`、`CKEditorConfig#setMediaEmbedResizable`）。
  原注释称该插件"由 umbrella `ckeditor5` 包导出，type 与 runtime 两层均可解析"，
  但实测 **48.4.0 与 48.5.0 的 umbrella 产物中均不含该符号**（`dist/index.d.ts` 与
  `dist/ckeditor5.js` 两层皆无）。这意味着 `loadMediaEmbedResizePlugin()` 的
  `?? null` 分支是常态而非异常路径——启用 `setMediaEmbedResizable(true)` 会静默降级为不可缩放。
  这是既有描述失准，非本次升级引入；代码行为本身正确（优雅降级、不阻断编辑器创建），故仅修文档。

### Added
- DevTools 手工排查指引（`docs/USER_GUIDE.md`、`docs/QUICK_REFERENCE.md`）：
  `$0.editorId` / `$0.editor` / `getData()` / 按 id 反查 / 列出页面全部编辑器。
  并说明 `editor` 虽在 TypeScript 中声明为 `private`，但那只是编译期约束，
  运行时（含生产构建）可正常访问；以及 `editor.id` 是 CKEditor 内部 id，
  识别编辑器应使用 `$0.editorId`。全部片段已在生产构建上实机验证。
- 澄清 `window.VAADIN_CKEDITOR_DEBUG` 在模块加载时求值一次，需**先设置再刷新页面**。

## [5.3.3] - 2026-08-26

### Fixed
- **builder 配置的 `ErrorHandler` 从不触发**（严重）。`setErrorHandlerInternal()` 只写字段，
  未像公开 setter 那样把 handler 接到 `EventDispatcher` 上，导致
  `.withErrorHandler(h).build()` 之后 `h` 永远不会被调用——消费端的错误处理静默失效。
  修复方式与 `contentManager`、`uploadManager` 保持一致：在 `initializeManagers()`
  中统一从字段装配。实测：修复前 builder 路径回调 0 次 / setter 路径 1 次，修复后均为 1 次。
- **`getToolbarStyle()` 丢失全部 per-button 样式**（严重）。重建时只读取 9 个标量字段而漏掉
  `buttonStyles`，使 `setToolbarStyle(getToolbarStyle())` 这类「读-改-写」永久销毁按钮样式。
  实测修复后 round-trip 完全保真。
- **三个内置 preset 在 STRICT 依赖模式下无法构建**（严重）。补齐
  `AI_DOCUMENT`（缺 `CLOUD_SERVICES_CORE`）、`EMAIL`（缺 `CLIPBOARD`）、
  `NOTION`（缺 `CLIPBOARD`/`WIDGET`/`WIDGET_TOOLBAR_REPOSITORY`）。
  实测：修复前 9 个 preset 有 3 个在 STRICT 下抛 `IllegalStateException`，修复后全部可构建。
- `validateDependencies()` 改为按**传递闭包**报告缺失项。原实现只检查直接依赖，
  与传递解析的 `resolve()` 在诊断范围上不一致：例如只声明 `SIMPLE_UPLOAD_ADAPTER` 时
  原先仅报 `IMAGE_UPLOAD`，遗漏其再依赖的 `IMAGE`。
  这是**诊断信息的完善，不是判定口径收紧**——已枚举全部插件验证：
  「原先通过、现在被拒」的数量为 0（若直接依赖已满足，其依赖必然也在集合中）。
- **上传去重集合只增不减**。`notifiedUploadIds` 在正常完成路径从不清理，
  长会话下持续泄漏。新增 `retireUpload()` 在终态释放登记信息。
  实测：200 次上传后集合从 200 降为 0，且同一 ID 再次上传可正常回调。
  注意 early-failure 路径**不**释放，以维持既有的「同一 uploadId 只通知一次」契约。
- **编辑器重挂载后永不重建**（前端，严重）。创建逻辑绑在 `firstUpdated()`（Lit 每实例只调一次），
  而 `disconnectedCallback` 会销毁编辑器，导致「移出 DOM 再放回」后只剩空白容器
  （Vaadin 中 `remove()/add()`、布局间移动、Tab 切换等场景常见）。改为在
  `connectedCallback` 中按需重建。
- **异步创建期间断开会留下孤儿编辑器**（前端，严重）。`create()` 解析可能晚于
  `disconnectedCallback`，而结果被无条件赋值给 `this.editor`，此时已无人负责销毁——
  每次路由往返泄漏一个完整编辑器实例。改为 await 之后重新检查连接状态并立即销毁孤儿。
- **标注侧栏 scroll 监听从未注销**（前端）。每次 `onEditorReady` 都会叠加监听器，
  造成滚动卡顿与组件泄漏；相邻的 `MutationObserver` 已有防叠加处理，此处补齐。
- **属性路径写回 `editorData` 会绕过 `apiChangeDepth`**（前端）。直接调用 `setData` 会让
  服务端推送的内容被判定为用户输入并回传服务端（issue #38 的问题经此路径复现），
  且跳过源码视图刷新（issue #57）。改为统一走 `updateData()`。
- `CKEditorConfig.configs` 改为同步 Map。实测 8 线程并发写入会静默丢失约 3 万次写入且不抛异常。
- `toJson()` / `getConfigs()` 改为返回深拷贝。此前返回值与内部配置共享可变子节点，
  调用方修改「快照」会反向污染真实配置。
- `firstUpdated` 中 `createEditor()` 的 unhandled rejection 兜底。

以下三项由交叉审查（Codex）发现，均为上述修复的边界遗漏，已一并解决：
- **同一 tick 内 remove→add 时重建被跳过**。销毁被延迟到 microtask，而实测回调顺序是
  `disconnectedCallback → connectedCallback → destroy microtask`，故 `connectedCallback`
  执行时 `editor` 尚未清空、重建守卫直接返回，随后编辑器被销毁却无人重建。
  改为在销毁完成后补判一次（`recreateEditorOnReconnect()` 由两处共用）。
- **「创建中断开、创建结束前又重连」漏唤醒**。孤儿销毁路径不经过 `destroyEditor()`，
  拿不到上述 microtask 补偿；而 `connectedCallback` 早已在 `isCreating=true` 时返回。
  改为在孤儿销毁后同样补触发一次重建判定。
- **同一 uploadId 重复登记时会互相删除**。`UploadManager` 的清理按裸 `uploadId` 进行，
  一旦同一 ID 上先后存在两个 task（服务端不校验 ID 唯一性，且 `handleUpload` 会
  无条件覆盖登记），先结束的一方会把另一方从 `activeTasks` 中移除，
  导致后者无法取消、状态查询失效。所有 `activeTasks.remove` 改为两参数原子删除
  （仅当映射仍指向本次 task 时才删）；重复通知守卫的键也由裸 uploadId 改为
  **`UploadTask` 实例本身**，按实例隔离——释放时机因此不再需要
  「该 ID 是否还有活跃任务」这类非原子判断，窗口期问题随之消失。

  说明：前端当前**不会**产生重复 ID——`UploadAdapterManager` 实例在组件重挂载后
  被复用（`if (!this.uploadManager)`），其计数器不随 `cleanup()` 重置，
  已实测确认 `cleanup()` 前后为 `upload-ed-1` → `upload-ed-2`。
  因此这些修复属于服务端的**纵深防御**：`handleUpload` 是 `@ClientCallable`，
  ID 由客户端提供、服务端不应假定其唯一。
- **孤儿销毁后的补偿重建时机错误**。原先用 `queueMicrotask` 触发，实测该 microtask
  会排在外层 `async` 函数的 `finally` **之前**，届时 `isCreating` 尚未复位、
  守卫直接返回，补偿失效、组件仍永久空白。改为置标志位、由 `finally` 在释放
  创建锁之后消费。
- **创建锁存在并发窗口**。`canCreateEditor()` 检查与 `isCreating = true` 之间隔着
  `await waitForPreviousEditorCleanup()`，两个并发调用者可能都通过检查并各建一个
  编辑器。改为先占锁再 await，并让守卫只依据 `isCreating`（`createPromise` 直到
  await 之后才赋值，要求两者同时成立会在该窗口内放行第二个调用者）。
- **失败路径仍会强引用整个 `UploadTask`**。handler 返回 null 或同步抛异常时
  `notifyError` 带 task 调用，而该路径不经过 `retireUpload`，守卫集合会一直持有
  该 task。已补充释放。
- **有界等待的挂起被换了个入口**。第一次修复给孤儿销毁加了超时防挂起，但随后
  让 `waitForPreviousEditorCleanup()` 无界 `await` 同一个 promise——detached 下
  CKEditor 的 `destroy()` 可能永不 settle，于是 `isCreating` 被永久占用、重连后
  依旧空白。改为等待也走 `Promise.race` 超时。
- **迟到的销毁会清空新编辑器**（DOM 所有权）。`Promise.race` 只停止等待、**不取消**
  底层 `destroy()`；而 CKEditor 的 `ElementApiMixin.updateSourceElement()` 末尾执行
  `setDataInElement(this.sourceElement, ...)`，其实现是 `el.innerHTML = data`
  （见 `@ckeditor/ckeditor5-utils`）——写回目标正是我们传给 `Editor.create()` 的容器，
  而重连复用同一节点（`editorId` 是 `@property`）。因此超时后新增
  `detachStaleEditorContainer()`：用同 id、同 class 的**新节点**替换容器，
  陈旧实例持有的成为游离节点，其写回不再影响页面。
  新容器的 class 取自常量 `EDITOR_CONTENT_CLASS`（同时用于 render() 的三处模板），
  而非沿用 `stale.className`——后者可能已被 CKEditor 注入运行时 class。
- **换节点后 Lit 未重新绑定**。仅调用 `requestUpdate()` 不够：Lit 3 的模板实例在首次
  克隆时就绑定了 `AttributePart`，不会重新扫描被外部 `replaceChild` 的节点。
  实测此后把 `editorId` 改为新值，Lit 会写到**已游离的旧节点**上。该路径可达
  （`setId()` 是公开 API）。改为先 `render(nothing, renderRoot)` 丢弃旧模板实例
  再触发更新，实测动态更新可正确落到页面节点。
- **`destroyPromise` 的清空未按身份比对**。`destroyEditor()` 中无条件置 null，
  会抹掉后登记的孤儿销毁，重新打开上述所有权漏洞。已改为按身份比对。

### Added
- `setSanitizeOnInput(boolean)` / `withSanitizeOnInput(boolean)`：可选的「客户端输入即净化」。
  背景：本组件继承 `CustomField<String>`，`Binder` 读取的是 `getValue()`，
  而 `setHtmlSanitizer()` 仅作用于 `getSanitizedValue()`——即配置了净化器 + 使用 Binder
  的用户实际得不到任何净化。开启本开关后，客户端内容在写入模型时即被净化，
  `getValue()`、Binder 与 `ValueChangeEvent` 均受保护；服务端自行 `setValue()` 的内容
  视为可信来源，不受影响。**默认关闭以保持向后兼容**，并已在 `getValue()` 的 Javadoc 中
  明确标注其返回未净化内容及对 Binder 的影响。

### Changed
- **CI 现在运行前端测试**：新增 `frontend` job 执行 `npm ci && npm run typecheck && npm test`。
  此前 171 个 vitest 用例与 `tsc --noEmit` 从未在 CI 中运行过（Maven 只把 `.ts` 当资源复制），
  类型错误会直到消费端 Vaadin 构建时才暴露。CI 同时开始编译 testbench 模块
  （该模块不在根 reactor 中，此前完全无覆盖），并在发布 tag 上触发。
- **发布增加 CI 门禁**：`publish.yml` 与 `publish-testbench.yml` 新增 `verify-ci` 前置 job，
  断言待发布 commit 在 `main` 上且其 CI 结论为 success。Maven Central 不可撤回，
  此前 tag 一推即发布，且 `workflow_dispatch` 可从任意分支触发。
  同时把 testbench 工作流中更严格的全串 SemVer 校验回填到主工作流。
- **发布产物不再包含测试资产**：`maven-jar-plugin` 补充排除 `*.test.ts`、`test-mocks/`、
  `vitest.config.ts`（此前约占 jar 的 46%，且 import 消费端不会安装的 vitest）；
  `maven-source-plugin` 补充独立的 excludes——它不继承 jar-plugin 配置，本地构建
  （存在 node_modules）产出的 sources jar 达 49MB/约 2 万个文件。
  实测 sources jar 由 49.6MB 降至 187KB。

### Tests
- 新增 `VersionSyncTest`：把 6 处版本常量的漂移变成构建失败。此前唯一约束是一句注释，
  发布流程也只校验 tag↔pom 一对关系，实践中已发生过漏改。
- 净化器测试补充判别性断言与 XSS 向量覆盖。原断言「`<script>` 被移除」是同义反复——
  jsoup 在任何 Safelist 下都会移除 `<script>`，把 BASIC 误改成 `relaxed()` 也照样全绿；
  已用变异测试确认新断言能捕获该降级。同时补齐 `onerror`/`onclick`/`javascript:`/`onload`
  在三种策略下的剥离断言。
- 上传大小上限与 MIME 白名单补充 `upload()` 层的行为断言（此前这两个校验分支从未被执行，
  删掉也不会有测试失败），已用变异测试确认有效。
- 替换 `theme-manager.test.ts` 中恒成立的 `expect(true).toBe(true)`。
- 新增 `reconnect-decision.ts`：把「重挂载后是否应重建编辑器」的判定抽成纯函数并配 9 个单测
  （含同 tick remove→add 与创建中断开两条完整时序），沿用本仓既有的
  「提取纯函数以便测试」模式，避免为覆盖生命周期而 mock 整个 CKEditor。
- 补充判别性测试：ErrorHandler 真实回调（非仅 getter）、全部 preset 在 STRICT 下可构建、
  传递依赖缺失可被发现、toolbarStyle 读-改-写保真、三个快照方法均为深拷贝、
  上传跨代复用与跨代重叠。多数已用变异测试验证其确能捕获对应缺陷。

### Security
- 修复 Dependabot 报告的 4 个依赖漏洞，其中 2 个属运行时（随包分发、影响消费端），
  2 个属开发期（`vitest` 传递依赖，不进入发布产物）。

- **jsoup 1.18.3 → 1.23.1**（运行时，CVE-2026-71497）。该漏洞使 `Cleaner` 在处理
  自定义 raw-text 元素时可能放行本应被清理的标记。本库 `HtmlSanitizer` 正是通过
  `Jsoup.clean(html, Safelist)` 做 HTML 净化，属于直接受影响的代码路径，
  故必须升级。全部 13 个 sanitizer 相关用例升级后仍通过。

- **jackson-databind 移除写死的 3.1.4**（运行时，CVE-2026-59889）。修复方式不是
  手动改成 3.1.5，而是**删除 `<version>` 声明**改由 `vaadin-bom` 管理——Vaadin 25.2.6
  已透传 `tools.jackson:jackson-bom` 并锁定 3.1.5（即修复版本）。此前写死 3.1.4 会
  覆盖 BOM，反而把消费端从 Vaadin 的安全基线上拉回旧版本。改为跟随 BOM 后，
  后续 Vaadin 升级可自动获得 Jackson 安全补丁，无需再手动跟。

- **postcss 8.5.15 → 8.5.26**（开发期，CVE-2026-73646 / CVE-2026-69153）。经
  `npm ls` 确认其来源是 `vitest → vite` 的传递依赖，仅用于本地测试，不随
  jar 分发，消费端不受影响。通过 `npm update vite`（vite 8.1.0 → 8.2.2，其
  依赖约束为 `postcss ^8.5.26`）在 lockfile 层面修复，`package.json` 未改动，
  `ckeditor5` 仍精确锁定 48.4.0。修复后 `npm audit` 报告 0 vulnerabilities。

## [5.3.2] - 2026-08-25

### Changed
- Vaadin Platform: 25.2.0 → 25.2.6（根 `pom.xml`、`ckeditor-vaadin-testbench/pom.xml`
  与 `examples/spring-boot-sample/pom.xml`）。补丁版升级，无 API 变更；`vaadin-bom`
  继续统一管理 `vaadin-core` 与 `vaadin-testbench-core` 的版本。
- CKEditor 5（`ckeditor5`、`ckeditor5-premium-features`）：48.2.0 → 48.4.0。
  同步更新前端 `package.json` 精确 pin、`VaadinCKEditor` 与 `VaadinCKEditorPremium`
  上的 `@NpmPackage` 注解，以及 `VaadinCKEditorPremium.getVersion()`
  （该值向消费端声明需要自行安装的 premium 包版本，两者必须同版本）。

  验证：`tsc --noEmit` 在 48.4.0 下零报错，说明 `plugin-resolver.ts` 中导入的
  全部插件符号在新版 umbrella 包中依然存在，无需代码适配。

### Fixed
- 修复 `upload-adapter.test.ts` 的偶发失败（既有问题，非本次升级引入；已在升级前的
  48.2.0 基线上复现）。根因：`upload()` 在调用 `server.handleFileUpload` 之前需先
  `await fileToBase64()`（内部是真实 `FileReader`），该耗时由 jsdom 的 I/O 调度决定、
  无上界；而用例用固定 `setTimeout(10)` 等待后即读取 `mock.calls[0][0]`，机器负载高时
  调用尚未发生，遂抛 `Cannot read properties of undefined`。

  修复方式：新增 `waitForUploadCall()` 辅助函数，轮询"调用已发生"这一真实条件
  （带 2s 上限与明确超时信息）替代猜测时长，共替换 8 处固定延时。其中
  `should handle abort during active upload` 一例的固定延时还存在语义问题——延时
  到期时上传可能尚未开始，`abort()` 命中的并非 active upload 分支，一并修正。

  验证：`upload-adapter` 单文件连续 13 次通过（含 4 路 `yes` 占满 CPU 的负载场景
  下 5 次），全量前端套件连续 5 次 171/171 通过。

## [5.3.1] - 2026-06-26

### Added
- Caret/focus control API on `VaadinCKEditor` (issue #52): `setCaretToStart()`,
  `setCaretToEnd()`, `focusEditor()`. Useful when leading block content (e.g. a
  table letterhead) would otherwise be auto-selected on focus — move the caret
  to the start so nothing is highlighted.
- Resizable embedded media (issue #71): `CKEditorConfig.setMediaEmbedResizable(true)`
  enables drag-to-resize handles on embedded media (videos). Backed by CKEditor's
  `MediaEmbedResize` plugin. The frontend loads it on demand from the `ckeditor5`
  umbrella package only when the flag is enabled; load failures (e.g. missing
  commercial license — see below) degrade silently. Requires `CKEditorPlugin.MEDIA_EMBED`.
- Free media-embed companion plugins, exported by the `ckeditor5` umbrella and now
  registered as built-in `CKEditorPlugin` constants:
  - `MEDIA_EMBED_STYLE` (`MediaEmbedStyle`) — alignment/styling for embedded media
    (`mediaEmbed:alignLeft` / `alignCenter` / `alignRight` toolbar items); the sibling
    feature of the resize support above. Not auto-loaded by `MEDIA_EMBED`, so it must
    be selected explicitly.
  - `MEDIA_EMBED_TOOLBAR` (`MediaEmbedToolbar`) — floating toolbar shown when an
    embedded media widget is selected (hosts the style/alignment buttons).
  - `AUTO_MEDIA_EMBED` (`AutoMediaEmbed`) — auto-converts pasted media links into embeds.
- `CKFINDER` (`CKFinder`) — CKFinder file-manager integration plugin. The plugin class
  itself is free (umbrella-exported); using it requires `config.ckfinder.uploadUrl` and
  a CKFinder server backend, so it is treated as a config-required plugin (filtered out
  of auto-select unless `setAllowConfigRequiredPlugins(true)` is set).
- `CKEditorConfig.setMediaEmbedToolbar(String...)` — fluent setter that writes media
  toolbar buttons to `config.mediaEmbed.toolbar` (where `MediaEmbedStyle`'s alignment
  buttons such as `mediaEmbed:alignLeft` must live, not the top-level `config.toolbar`).
  Empty/null input writes nothing (no empty-array noise).
- Optional `ckeditor-vaadin-testbench` module providing `VaadinCKEditorElement` — a
  type-safe Vaadin TestBench page object for the `vaadin-ckeditor` component
  (`getData`/`setData`/`insertText`/`setReadOnly`/`focusEditor`/`setCaretToStart`/`End`
  plus key property getters). Vaadin TestBench is a commercial (Premium) feature: the
  module depends on `vaadin-testbench-core` with `provided` scope and does not bundle
  TestBench at runtime, so the core addon stays free of the Premium dependency. Consumers
  bring their own TestBench dependency and license to run tests.

### Changed
- `media-embed-resize.ts`: the on-demand `MediaEmbedResize` loader now imports from the
  `ckeditor5` umbrella package instead of the `@ckeditor/ckeditor5-media-embed` subpackage.
  Verified against the installed 48.2.0 artifacts: the umbrella re-exports the whole
  media-embed subpackage (`export * from '@ckeditor/ckeditor5-media-embed'`), so
  `import { MediaEmbedResize } from 'ckeditor5'` resolves. Importing from the umbrella
  (rather than mixing umbrella + subpackage entry points) avoids the risk of resolving
  two distinct class instances. Corrects the prior comment that claimed the plugin was
  "free" and "not umbrella-exported": it *is* umbrella-exported, but its
  `MediaEmbedResizeEditing` dependency has `isPremiumPlugin === true`, so loading it under
  a GPL license triggers CKEditor's license check — it is effectively a premium feature.

### Removed
- **Breaking:** `CKEditorPlugin.LINE_HEIGHT` removed from the free built-in plugin enum.
  `LineHeight` is a **premium** feature in CKEditor 48.x — it is not exported by the
  `ckeditor5` umbrella package (only by `ckeditor5-premium-features`, where its class
  reports `isPremiumPlugin === true`) and was never registered in the TypeScript
  `PLUGIN_REGISTRY`, so `withPlugins(CKEditorPlugin.LINE_HEIGHT)` produced a
  "Plugin not found in registry" error at runtime. The premium definition already exists
  as `VaadinCKEditorPremium.PremiumPlugin.LINE_HEIGHT`.
  - **Migration:** replace `withPlugins(CKEditorPlugin.LINE_HEIGHT)` with
    `addCustomPlugin(CustomPlugin.fromPremium("LineHeight"))` and configure a commercial
    license key.

## [5.3.0] - 2026-06-26

### Changed
- Vaadin Platform: 25.1.6 → 25.2.0 (root addon `pom.xml` + `examples/spring-boot-sample/pom.xml`)
  - Companion `provided` dependencies verified against 25.2.0's `flow-server` BOM
    (`jakarta.servlet-api` 6.1.0 matches and is kept).
- CKEditor 5 (`ckeditor5`, `ckeditor5-premium-features`): 48.1.1 → 48.2.0
  - `package-lock.json` regenerated; `tsc --noEmit` clean — no premium AI `.d.ts`
    type-contract drift; no source changes required; frontend vitest 114/114 green
  - `@NpmPackage` annotations on `VaadinCKEditor`/`VaadinCKEditorPremium` and
    `VaadinCKEditorPremium.getVersion()` synced to 48.2.0 (these tell consuming
    apps which npm version to install)
- Jackson databind (`tools.jackson.core`): 3.1.3 → 3.1.4
- `lit`: ^3.3.2 → ^3.3.3 (`package.json` + `@NpmPackage` annotation)
- Frontend test tooling: `vitest` 3.2.4 → 4.1.9, `@vitest/coverage-v8` → ^4.1.9
  (clears the dev-only vitest CVEs — `npm audit` now reports 0 vulnerabilities;
  `vi.fn()` spy types adjusted for vitest 4's stricter `Mock` typing)
- Addon version bumped to 5.3.0 and synced across all version references
  (`pom.xml`, sample `addon.version`, Java `VERSION`, `vaadin-ckeditor.ts` version,
  frontend `package.json`)

### Fixed
- `UploadManager` completion handler refactored: the upload `handle()` lambda
  (previously 5 indent levels) extracted into `processCompletion()` +
  `resolveFailureMessage()` with guard clauses (now ≤3 levels), removing
  duplicated failure-message logic
- `vaadin-ckeditor.ts`: collapsed 4 duplicated `try/catch` listener-removal blocks
  into a single `safeOff()` helper

## [5.2.0] - 2026-05-27

### Changed
- Vaadin Platform: 25.0.5 → 25.1.6
- CKEditor 5 (`ckeditor5`, `ckeditor5-premium-features`): 47.5.0 → 48.1.1
- Jackson databind (`tools.jackson.core`): 3.0.3 → 3.1.3
- Jakarta Servlet API: 6.0.0 → 6.1.0
- JUnit Jupiter: 5.11.4 → 6.0.3 (PR #80 by @mstahv — forward compatible with Vaadin 25.2-SNAPSHOT)
- `EditorConstructor.create(element, config)` migrated to CKEditor 48 single-argument create config:
  - `ClassicEditor` uses top-level `config.attachTo` (v48 official contract)
  - `BalloonEditor` / `InlineEditor` / `DecoupledEditor` use `config.root.element`

### Added
- New `editor-config-normalizer.ts` module: pure-function CKEditor 47 → 48 config compatibility layer
  - `normalizeRootConfig` — auto-migrates top-level `initialData`/`placeholder`/`label` to `root.*`; `root.*` takes precedence over top-level with a dev-mode warning
  - `normalizeAIConfig48` — AI config migration:
    - `ai.chat.shortcuts[].check` → `commandId`
    - `ai.chat.models.modelSelectorAlwaysVisible` → `ai.models.showModelSelector`
    - `ai.chat.models` → `ai.models` (existing `ai.models` keys take precedence)
    - `ai.reviewMode.translations` → `ai.translate.languages`
    - `ai.quickActions.extraCommands[].type` `'CHAT'/'ACTION'` → `'chat'/'action'`
    - `ai.quickActions.extraCommands[].displayedPrompt` → `label` (action type drops `displayedPrompt`; chat type keeps both)
  - `buildCreateConfig` — constructs the v48 create config per `editorType`; defensively clears stale `attachTo`/`element` fields
  - `stripInitialDataIfChannelSeeded` — collaboration channel seed logic, dependency-injected via `ChannelInitialDataDeps` (storage / clock / callbacks)
  - `cloneConfig` — `structuredClone` fallback for configs containing non-cloneable values (functions / DOM nodes)
  - Dev mode (`window.VAADIN_CKEDITOR_DEBUG = true`) prints migration warnings
- `ckeditor5-premium-features.d.ts`: CKEditor 48 AI configuration type contracts
  - `AIChatController`, `AIContextItemType`, `AIChatShortcutType`, `AIQuickActionCommandType`
  - `AIQuickActionsExtraCommandConfig` refactored as discriminated union (`AIQuickActionsChatCommandConfig` + `AIQuickActionsActionCommandConfig`); chat commands require `displayedPrompt`, action commands use `label` only
- `theme-manager.ts` `DARK_THEME_VARS`: 10 new CKEditor 48 official `--ck-color-ai-*` dark-mode tokens
- `vitest.config.ts`: excludes `**/target/**` to avoid scanning Maven-copied frontend sources; regex alias resolves the premium CSS deep import path in test environments
- `test-mocks/empty.css`: stub for the premium CSS deep import under vitest
- End-to-end test suite under `e2e/` (NOT bundled in the published jar):
  - Spring Boot 4 + Vaadin 25.1.6 sample app at `examples/spring-boot-sample/` (7 routes covering 4 EditorTypes, dark theme, upload, collaboration seed)
  - Playwright 1.60.0 test suite — 18 functional tests + 10 visual regression baselines = 28 tests, Chromium + Firefox
  - GitHub Actions `e2e.yml` workflow runs the full suite on every PR + push to `main`

### Fixed
- Cleared 7 transitive npm dev-only vulnerabilities (rollup / vite / minimatch / postcss / picomatch / ws)
- Dependabot alert #41: bump `@playwright/test` 1.50.0 → 1.60.0 (high-severity CVE — Playwright < 1.55.1 downloaded browser binaries without TLS verification)

### Notes
- **Spring Boot 4.0.4+ is recommended** to align Jackson 3.1
- Top-level `initialData`/`placeholder`/`label` config keys are still accepted, but migrating to `root.*` is recommended
- Legacy AI config keys are auto-normalized to v48 field names at runtime — no consumer code changes required
- Test coverage at release: Java JUnit 488/488 + frontend vitest 114/114 + Playwright 28/28 (Chromium + Firefox) — all green

## [5.1.0] - 2026-02-13

### Added
- EMAIL preset - curated plugin set for email composition with Base64 upload adapter
- NOTION preset - Notion-style editing with block toolbar and collaboration-ready plugins
- AI premium plugins: AIChat, AIEditorIntegration, AIQuickActions, AIReviewMode, AITranslate
- AI sidebar with responsive layout, accessibility (inert attribute), and dark theme support
- BLOCK_TOOLBAR dependency on WIDGET and WIDGET_TOOLBAR_REPOSITORY
- CSS custom properties for AI sidebar sizing (`--ck-ai-sidebar-*`)
- Container query height support with `100cqh` fallback

### Changed
- Minimum Vaadin version: 25.0.5
- Slimmed pom.xml: removed legacy assembly profile, OSGi test infrastructure, snapshot repositories
- Release process: Maven Central via `central-publishing-maven-plugin` (replaces legacy Vaadin Directory zip)
- CI workflows streamlined

### Removed
- OSGi integration test (Pax Exam dependencies removed)
- Legacy `directory` Maven profile and assembly descriptors
- Snapshot/prerelease repository declarations

## [5.0.5] - 2025-02-13

### Added
- Premium plugin support and custom plugin builder
- Empty file upload validation - rejects zero-byte files with clear error message
- Upload abort race condition protection - prevents cancellation of wrong upload
- SSRF protection documentation with known limitations (decimal integer IP, DNS rebinding)

### Fixed
- Upload adapter abort() now safely handles edge case when no upload is in progress
- Debug logging added for empty file rejection and abort edge cases
- Code review issues across frontend, backend, and tests

## [5.0.3] - 2025-02-02

### Added
- GitHub Actions CI/CD workflows
  - CI: Build and test on PR/push to main
  - Publish: Automated release to Vaadin Directory on tag

## [5.0.2] - 2025-02-02

### Added
- OSGi integration tests using Pax Exam framework
- Upload timeout mechanism with configurable duration (default 5 minutes)

### Fixed
- VERSION constant synchronized to 5.0.2 in Java and TypeScript
- Internal package no longer exported in OSGi bundle
- Documentation version references updated

## [5.0.1] - 2025-02-02

### Fixed
- Minor documentation corrections

## [5.0.0] - 2025-02-02

### Added
- Complete rewrite of Vaadin CKEditor 5 integration
- New `VaadinCKEditorBuilder` with fluent API for editor configuration
- `withViewOnly()` convenience method for read-only display mode
- `withReadOnly(boolean)` method for toggling edit capability
- `withHideToolbar(boolean)` method for toolbar visibility control
- `EnumParser` utility for safe, locale-independent enum parsing
- `EventDispatcher` for centralized event handling
- `UploadManager` for async file upload with progress tracking and cancellation
- Support for all CKEditor 5 editor types: Classic, Balloon, Inline, Decoupled
- Auto theme support - syncs with Vaadin Lumo light/dark mode
- Comprehensive error handling with `EditorError` and `ErrorSeverity`
- Fallback modes for graceful degradation (READ_ONLY, DISABLED, HIDDEN)
- Builder parameter validation with clear error messages
- Language code validation (ISO 639-1 format)
- Toolbar configuration validation

### Changed
- Minimum Java version: 21
- Minimum Vaadin version: 24.x
- Package renamed from `vaadin-litelement-ckeditor` to `vaadin-ckeditor`
- Simplified API with builder pattern replacing constructor overloads
- Unified `Locale.ROOT` for all case conversions (i18n safe)

### Fixed
- Locale-sensitive string operations now use `Locale.ROOT`
- Upload cancellation properly interrupts underlying CompletableFuture
- CompletionException properly unwrapped to expose root cause

### Removed
- Legacy LitElement-based implementation
- Deprecated configuration methods
- Support for Vaadin 14-23

## [4.x and earlier]

See the [legacy repository](https://github.com/wontlost-ltd/vaadin-ckeditor/tree/v4-archived) for previous versions.

---

## Version Guidelines

- **MAJOR** (x.0.0): Breaking API changes, removed features
- **MINOR** (0.x.0): New features, backward compatible
- **PATCH** (0.0.x): Bug fixes, no API changes

[Unreleased]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.3.3...HEAD
[5.3.3]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.3.2...v5.3.3
[5.3.2]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.3.1...v5.3.2
[5.3.1]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.3.0...v5.3.1
[5.3.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.2.0...v5.3.0
[5.2.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.1.0...v5.2.0
[5.1.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.5...v5.1.0
[5.0.5]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.3...v5.0.5
[5.0.3]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.2...v5.0.3
[5.0.2]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.1...v5.0.2
[5.0.1]: https://github.com/wontlost-ltd/vaadin-ckeditor/compare/v5.0.0...v5.0.1
[5.0.0]: https://github.com/wontlost-ltd/vaadin-ckeditor/releases/tag/v5.0.0
