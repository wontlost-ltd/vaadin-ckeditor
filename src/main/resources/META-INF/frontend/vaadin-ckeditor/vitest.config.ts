/**
 * Vitest Configuration for Vaadin CKEditor 5
 *
 * Run tests with: npm test
 * Run in watch mode: npm run test:watch
 * Run with coverage: npm run test:coverage
 */

import { defineConfig } from 'vitest/config';

export default defineConfig({
    test: {
        // Use jsdom for DOM testing
        environment: 'jsdom',

        // Global test utilities (describe, it, expect, etc.)
        globals: true,

        // Test file patterns
        include: ['**/*.test.ts'],

        // Exclude node_modules, build outputs, and Maven target/ copies (mvn package 会复制源码副本)
        exclude: ['node_modules', 'dist', 'build', '**/target/**'],

        // Coverage configuration
        coverage: {
            provider: 'v8',
            reporter: ['text', 'json', 'html'],
            include: [
                'plugin-resolver.ts',
                'upload-adapter.ts',
                'theme-manager.ts',
                'fallback-renderer.ts',
                'editor-config-normalizer.ts',
                'comment-permission-enforcer.ts',
            ],
            exclude: [
                '**/*.test.ts',
                '**/node_modules/**',
            ],
        },

        // Reporter configuration
        reporters: ['default'],

        // Timeout for async tests (5 seconds)
        testTimeout: 5000,
    },

    // 测试以 ?raw 读取 Java 预设 / 插件枚举源码（fullscreen-support.test.ts 的「预设 × 编辑器类型」用例），
    // 只放行这一个目录
    server: {
        fs: {
            allow: [
                new URL('.', import.meta.url).pathname,
                new URL('../../../../java/com/wontlost/ckeditor/', import.meta.url).pathname,
            ],
        },
    },

    // Resolve aliases for testing
    resolve: {
        alias: [
            // premium CSS 子路径（必须放在通用别名之前，避免被前缀匹配吞掉）
            {
                find: /^ckeditor5-premium-features\/ckeditor5-premium-features\.css$/,
                replacement: new URL('./test-mocks/empty.css', import.meta.url).pathname,
            },
            // premium 模块本身：vitest 环境替换为 stub
            {
                find: /^ckeditor5-premium-features$/,
                replacement: new URL('./test-mocks/ckeditor5-premium-features.ts', import.meta.url).pathname,
            },
        ],
    },
});
