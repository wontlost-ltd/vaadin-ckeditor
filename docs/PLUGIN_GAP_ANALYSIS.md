# CKEditor 插件差集分析报告（ckeditor5 48.2.0）

> 本报告核对 CKEditor 5 官方 `ckeditor5`（免费）与 `ckeditor5-premium-features`（付费）
> 两个 umbrella 包在 **48.2.0** 版本的插件全集，与本库（vaadin-ckeditor）当前支持范围做差集，
> 给出补全决策与实施结果。
>
> **核验方法**：双链交叉验证。
> 1. **一手 import 证据** — 本地安装 `ckeditor5@48.2.0` + `ckeditor5-premium-features@48.2.0`，
>    在 jsdom 提供 DOM 后真实 ESM `import`，读取 **1473 个 umbrella 导出键**与 premium 包导出，
>    并逐个核验插件类的静态 `isPremiumPlugin` gate。
> 2. **Workflow 65-agent 网络核验** — 对抗性验证插件 tier/source，结论与一手证据完全一致。
>
> ⚠️ 过程教训：对 minify 后的 runtime bundle 做 `grep` / `es-module-lexer` 均产生**伪证据**
> （类名被改写、`export *` 转发不展开），唯有**真实 import 解析**给出真相。涉及 CKEditor 包导出
> 结构的判断，必须以真实 import 为准，不可凭 bundle 文本或文档转述。

---

## 1. 核心架构回顾：本库的三条插件加载路径

| 类型 | Java 入口 | TS 加载方式 | 是否需逐个登记 |
|------|-----------|-------------|----------------|
| **标准免费插件** | `withPlugins(CKEditorPlugin.X)` | 静态 `PLUGIN_REGISTRY` 查表 | **是**（枚举 + registry 双侧） |
| **Premium 插件** | `addCustomPlugin(CustomPlugin.fromPremium("X"))` | 运行时 `import('ckeditor5-premium-features')` 按名取 | **否**（generic channel，任意名生效） |
| **第三方/自研** | `addCustomPlugin(CustomPlugin.withImportPath("..."))` | 动态 `import(importPath)` | **否** |

关键差异：
- **免费插件**走静态 registry，必须在 `CKEditorPlugin` 枚举 + `plugin-resolver.ts` 的 `PLUGIN_REGISTRY`
  各登记一行才能用。这是"免费差集"的实际含义。
- **Premium 插件**走 generic channel，`fromPremium(anyName)` 对 `ckeditor5-premium-features` 的
  任意导出生效。因此"premium 差集"在**功能层面不存在**。

---

## 2. 关键裁决（一手证据锁定）

| 插件 | umbrella 导出 | 类 `isPremiumPlugin` | 依赖链 premium gate | 真实 tier | 本库原认知 |
|------|:---:|:---:|:---:|------|------|
| `MediaEmbedStyle` | ✅ | false | `StyleEditing` = false | **真免费** | 未收录 |
| `MediaEmbedToolbar` | ✅ | false | — | **真免费** | 未收录 |
| `AutoMediaEmbed` | ✅ | false | — | **真免费** | 未收录 |
| `MediaEmbedResize` | ✅ | false | **`ResizeEditing` = TRUE** | **功能 premium** | ❌ 注释称"免费、未由 umbrella 导出" |
| `TableLayout` | ✅ | false | **`LayoutEditing` = TRUE** | **功能 premium** | ✅ 已正确归 premium |
| `LineHeight` | ❌ | premium 包 = TRUE | — | **premium** | ❌ 免费枚举里错误登记 |

### 2.1 "功能性 premium" 概念

部分插件的 **glue 类**本身 `isPremiumPlugin === false`（看起来免费），但其 `requires` 依赖链上的
`*Editing` 子插件 `isPremiumPlugin === true`。CKEditor 的 license 校验按依赖链整体判定：在 GPL license 下
加载这类插件会触发 license 报错。`MediaEmbedResize`、`TableLayout` 即属此类——**umbrella 可 import，但功能需商业 license**。

### 2.2 `MediaEmbedResize` 真相

本库 `media-embed-resize.ts` 旧注释断言"48.2.0 未由 umbrella 导出，只在 `@ckeditor/ckeditor5-media-embed` 子包"。
**这是错的**：

- `ckeditor5@48.2.0/dist/index.d.ts` 与 `dist/ckeditor5.js` 通过 `export * from '@ckeditor/ckeditor5-media-embed'`
  把整个子包（含 `MediaEmbedResize`/`MediaEmbedStyle`）透传到 umbrella，`import { MediaEmbedResize } from 'ckeditor5'` 可解析。
- 但它是功能性 premium（`MediaEmbedResizeEditing.isPremiumPlugin === true`），不是免费 GPL。

**结论**：保留"按需隔离加载 + 失败静默"的设计（正确，避免无 license 用户崩溃），但
（a）订正注释，（b）import 源由子包改为 umbrella `ckeditor5`，消除"静态 umbrella + 动态子包"双入口可能解析到
不同类实例的风险。**不**并入免费 registry。

---

## 3. 免费插件差集

### 3.1 已实施补全

| 优先级 | 插件 | Java 枚举常量 | 说明 | 加载特性 |
|:---:|------|------|------|------|
| **P0** | `MediaEmbedStyle` | `MEDIA_EMBED_STYLE` | resize 的姊妹特性，媒体对齐/排版样式。**不被 `MediaEmbed` 自动加载**，需显式选择 | 标准 registry |
| **P1** | `MediaEmbedToolbar` | `MEDIA_EMBED_TOOLBAR` | 选中媒体时的浮动工具栏，承载 style/对齐按钮 | 标准 registry |
| **P1** | `AutoMediaEmbed` | `AUTO_MEDIA_EMBED` | 粘贴链接自动转嵌入 | 标准 registry |
| **P2** | `CKFinder` | `CKFINDER` | 文件管理集成；类免费但需 `config.ckfinder.uploadUrl` + 服务端 | 标准 registry + **require-config** |

`CKFinder` 已加入 `plugin-resolver.ts` 的 `PLUGINS_REQUIRING_CONFIG`，自动全选时被过滤，
显式配置后通过 `setAllowConfigRequiredPlugins(true)` 放行。

### 3.2 不予补全的项（及理由）

Workflow confirmedGaps 列出 60 个"umbrella 有、本库 registry 无"的导出，但绝大多数**不应**单独登记：

| 类别 | 示例 | 不补的理由 |
|------|------|------|
| glue plugin 的内部子组件 | `BoldEditing`、`BoldUI`、`MediaEmbedEditing`、`MediaEmbedUI`、`AlignmentEditing` 等 | 已被对应 glue plugin（registry 已有 `Bold`/`MediaEmbed`/`Alignment`）通过 `requires` **自动加载**，单独登记纯属噪音 |
| 包自动加载的依赖 | `TableClipboard`、`TableKeyboard`、`TableMouse`、`TableSelection` | `Table` 自动加载，无需用户感知 |
| Essentials 成员 | `Enter`、`ShiftEnter`、`Delete`、`Input`、`Typing` | `Essentials`（registry 已有）自动加载二者 |
| ContextPlugin / 工具类 | `PendingActions`、`ClipboardPipeline`、`ClipboardMarkersUtils`、`*Utils` | 多被其他插件依赖自动加载，单独登记价值低 |
| Legacy 废弃类 | `LegacyList`、`LegacyListProperties`、`LegacyTodoList` | 废弃，不应暴露 |
| 功能性 premium | `CKBox`、`CKBoxImageEdit` 系列 | 类在 umbrella，但功能需商业 license + 另装 `ckbox` 包；走 premium/custom 通道，不进免费 registry |

> 设计原则：免费 registry 只登记**用户需显式选择的 glue plugin**，不登记会被自动加载的子组件
> （"消灭特殊情况"，避免 registry 膨胀）。

### 3.3 真正需独立包的免费插件

- **`Mermaid`**（流程图/图表）：唯一确凿的"官方免费但不在 umbrella、需独立 npm 包"案例
  （`@ckeditor/ckeditor5-mermaid`），且官方标注 **experimental，不推荐生产**。
- **建议**：不收进库。需要的用户用 `CustomPlugin.withImportPath("@ckeditor/ckeditor5-mermaid")` 自助加载。

---

## 4. 付费插件差集

### 4.1 功能层面：无差集

`CustomPlugin.fromPremium(jsName)` 走 `loadPremiumPlugins`（`plugin-resolver.ts`），执行
`(await import('ckeditor5-premium-features'))[jsName]` —— 纯运行时具名查找，对 premium 包的
**任意导出名生效**，与 `.d.ts` 是否声明无关（`.d.ts` 末尾 `export default Record<string, unknown>` 兜底）。

因此本库对**全部 premium 插件已具备完整运行时支持**，付费差集在功能层面不存在。

### 4.2 类型层面：`.d.ts` 补全（仅影响 DX，可选）

`ckeditor5-premium-features.d.ts` 声明了 27 个名字用于 TS 类型补全。premium 包实际有 151 个有 `pluginName`
的插件类（含 65 个顶层 glue plugin），故 `.d.ts` 存在 type-completion 差集。但补全它**只改善 IDE 自动完成**，
不改变运行时能力。如需提升 DX，可补充常用导出（如 `LineHeight`、`Footnotes`、`Uploadcare` 等）为
`export const X: unknown;`。**此为可选优化，本次未做**。

### 4.3 边界情况：CKBox

`CKBox` / `CKBoxImageEdit` 是"类在 umbrella `ckeditor5`、但功能 premium 且运行时另需 `ckbox` 服务包"的特性。
它们**不**从 `ckeditor5-premium-features` 导出，所以 `fromPremium` 对其**无效**。正确用法：
`CustomPlugin.of("CKBox", ...)`（从 umbrella `ckeditor5` 取）+ 用户自行 `npm i ckbox` + 商业 license。
此为文档澄清问题，非代码差集。

---

## 5. 实施清单与验证

### 5.1 改动文件

| 文件 | 改动 |
|------|------|
| `CKEditorPlugin.java` | 新增 `AUTO_MEDIA_EMBED` / `MEDIA_EMBED_STYLE` / `MEDIA_EMBED_TOOLBAR` / `CKFINDER`；移除免费 `LINE_HEIGHT` |
| `plugin-resolver.ts` | import 块 + `PLUGIN_REGISTRY` 各加 4 项；`CKFinder` 加入 `PLUGINS_REQUIRING_CONFIG` |
| `media-embed-resize.ts` | 订正注释；动态 import 源 `@ckeditor/ckeditor5-media-embed` → `ckeditor5` |
| `CKEditorPluginTest.java` | 新增 4 个测试方法（媒体配套插件、CKFinder、LineHeight 移除断言） |
| `CHANGELOG.md` | Added / Changed / Removed（含 LINE_HEIGHT **Breaking** 迁移说明）；订正旧 issue #71 措辞 |

### 5.2 破坏性变更

`CKEditorPlugin.LINE_HEIGHT` 移除是 breaking change。

**迁移**：
```java
// 旧（编译失败）
.withPlugins(CKEditorPlugin.LINE_HEIGHT)

// 新
.addCustomPlugin(CustomPlugin.fromPremium("LineHeight"))
// 并配置商业 license key
```

### 5.3 验证结果（本地可重复）

- **TS**：`npm run typecheck` 通过；`npm test` → **169/169 通过**
- **Java**：`mvn clean test` → **542/542 通过**（clean recompile，非陈旧字节码）

---

## 附录：核验依据（本机 48.2.0 产物）

- `ckeditor5/dist/index.d.ts` 第 47 行：`export * from '@ckeditor/ckeditor5-media-embed';`
- `@ckeditor/ckeditor5-media-embed/dist/index.d.ts`：`export { MediaEmbedResize }`、`export { MediaEmbedStyle }` 等
- `MediaEmbedResizeEditing.isPremiumPlugin === true`（一手 import 核验）
- `TableLayoutEditing.isPremiumPlugin === true`（一手 import 核验）
- `ckeditor5` umbrella：1473 个导出键、293 个有 `pluginName` 的插件类
- `ckeditor5-premium-features`：151 个有 `pluginName` 的插件类（65 个顶层 glue）
- `LineHeight`：umbrella 不导出，premium 包导出且 `isPremiumPlugin === true`
