/**
 * CSS 副作用 import 的环境声明。
 *
 * 连接器用 `import './x.css'` 让打包器（Vite）把样式注入产物，这是 Vaadin 前端的标准做法，
 * 运行时行为一直正常。但 TypeScript 在 `moduleResolution: "bundler"` 下要求副作用 import
 * 同样能解析到模块或类型声明，否则报 TS2882。
 *
 * 该检查在 TS 6 中收紧：本仓 devDependencies 锁定的 5.9.3 不会报错，而 IDE 若选用了更高版本
 * （例如示例应用 node_modules 中的 6.0.3）就会在 vaadin-ckeditor.ts 上报三处 TS2882。
 * 实测用 6.0.3 编译连接器可稳定复现，用 5.9.3 则零报错——即版本差异而非代码缺陷。
 *
 * 这里用通配声明一次性覆盖全部 `.css` 副作用 import，避免每新增一个样式文件就要补一行。
 * 既有的 `ckeditor5-premium-features.css` 单条声明保留在
 * `ckeditor5-premium-features.d.ts` 中，与该文件的其余 premium 声明放在一起。
 */

declare module '*.css';
