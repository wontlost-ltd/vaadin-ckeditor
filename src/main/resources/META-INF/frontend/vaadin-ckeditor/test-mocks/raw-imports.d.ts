/**
 * Vite `?raw` 导入的类型声明（仅测试使用）。
 *
 * 连接器 tsconfig 不含 `@types/node` 与 `vite/client`，测试需要读取源文件文本时
 * 改用 `import src from './x.ts?raw'`。放在 test-mocks/ 下，随其一起排除在 jar 之外。
 */
declare module '*?raw' {
    const source: string;
    export default source;
}
