/**
 * Vite `import.meta.glob` 的最小类型声明（仅测试使用）。
 *
 * 连接器 tsconfig 不含 `vite/client`；translation-loader.test.ts 用它列出 node_modules 中
 * ckeditor5 实际提供的翻译文件。放在 test-mocks/ 下，随其一起排除在 jar 之外。
 */
interface ImportMeta {
    glob(pattern: string): Record<string, () => Promise<unknown>>;
}
