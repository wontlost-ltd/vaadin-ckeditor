# 安全策略 / Security Policy

## 支持的版本 / Supported versions

| 版本 | 状态 |
|---|---|
| 5.4.x | ✅ 接受安全修复 |
| 5.3.x | ⚠️ 仅严重漏洞 |
| < 5.3 | ❌ 不再支持，请升级 |

## 报告漏洞 / Reporting a vulnerability

**请不要通过公开 issue 报告安全问题。**

Please do not report security issues through public issues.

请使用 GitHub 的私密报告通道：
[Security → Report a vulnerability](https://github.com/wontlost-ltd/vaadin-ckeditor/security/advisories/new)

或发送邮件至 service@wontlost.com。

报告中请尽量包含：受影响版本、复现步骤、以及你认为的影响范围。有可运行的复现
示例会显著加快确认速度。

我们会在收到后尽快确认。这是一个开源项目，没有专职安全团队，因此不承诺具体的
响应时限——但会如实告知处理进度，包括「暂时无法处理」这种情况。

## ⚠️ 使用者必读：默认不做 HTML 净化

这是本组件最容易被误用的地方，请务必确认你的用法。

**编辑器默认没有配置净化器。** 富文本编辑器的输出是用户可控的 HTML；如果原样
存库并在其他页面渲染，就是一条标准的存储型 XSS 路径。

组件提供了净化能力，但**需要你显式开启**：

```java
editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT));
```

可选策略：`NONE` / `BASIC` / `RELAXED` / `STRICT`。

需要说明的是，**净化是否在客户端做，不应作为你唯一的防线**。攻击者可以绕过前端
直接向服务端发请求。请在服务端持久化前、以及渲染时同样做净化或转义。本组件的
净化器是纵深防御的一层，不是全部。

## 本项目的安全边界 / Threat model

值得关注的攻击面：

1. **富文本内容** —— 见上文。默认不净化。
2. **文件上传** —— 组件支持图片上传适配器。上传目标、大小限制与类型校验由
   使用方配置；组件不代替你做访问控制，也不校验文件内容是否与其声称的类型相符。
3. **CKEditor 5 自身与其插件** —— 70+ 免费插件与可选的 premium 插件都在浏览器中
   执行。其漏洞会传递给使用本组件的应用。我们通过 Dependabot 跟踪 `ckeditor5`
   的版本。
4. **`ckeditor5` 与 `ckeditor5-premium-features` 必须同版本** —— 版本错配曾导致
   运行时异常。构建时请确认二者一致。

不在本组件职责内：认证、鉴权、CSRF 防护、上传文件的存储安全。这些属于宿主应用。

## 依赖 / Dependencies

前端依赖通过 `@NpmPackage` 声明，随 jar 传递给下游：

- `ckeditor5` — 编辑器本体（精确版本，不用范围）
- `lit` — 宿主元素基类

Java 侧依赖 Jackson（`tools.jackson.core`），由消费端提供版本。

## 已知的历史修复

- **5.3.3** — 修复深度审计发现的 20 项缺陷
- **5.3.3** — 修复 Dependabot 报告的 4 个依赖漏洞
- **5.4.0** — 修复组件重挂载时的销毁递归（issue #122），以及上传适配器的
  工厂未还原导致的泄漏

详见 [CHANGELOG.md](CHANGELOG.md)。
