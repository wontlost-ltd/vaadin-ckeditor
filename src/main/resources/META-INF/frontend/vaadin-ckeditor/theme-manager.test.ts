/**
 * Unit Tests for Theme Manager Module
 *
 * Run with: npx vitest run theme-manager.test.ts
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ThemeManager, DARK_THEME_VARS } from './theme-manager';

describe('ThemeManager', () => {
    let manager: ThemeManager;
    let originalMatchMedia: typeof window.matchMedia;

    beforeEach(() => {
        manager = new ThemeManager();
        originalMatchMedia = window.matchMedia;

        // Mock matchMedia
        window.matchMedia = vi.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: vi.fn(),
            removeListener: vi.fn(),
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));
    });

    afterEach(() => {
        manager.cleanup();
        window.matchMedia = originalMatchMedia;

        // Clean up any theme attributes
        document.documentElement.removeAttribute('theme');
    });

    describe('initialization', () => {
        it('should default to light theme', () => {
            expect(manager.getCurrentTheme()).toBe('light');
        });

        it('should initialize with auto mode', () => {
            const callback = vi.fn();
            manager.initialize('auto', callback);

            // Should detect theme from document
            expect(['light', 'dark']).toContain(manager.getCurrentTheme());
        });

        it('should initialize with explicit light mode', () => {
            const callback = vi.fn();
            manager.initialize('light', callback);

            expect(manager.getCurrentTheme()).toBe('light');
            expect(callback).toHaveBeenCalledWith('light');
        });

        it('should initialize with explicit dark mode', () => {
            const callback = vi.fn();
            manager.initialize('dark', callback);

            expect(manager.getCurrentTheme()).toBe('dark');
            expect(callback).toHaveBeenCalledWith('dark');
        });
    });

    describe('theme detection', () => {
        it('should detect dark theme from html[theme~="dark"]', () => {
            document.documentElement.setAttribute('theme', 'dark');

            const callback = vi.fn();
            manager.initialize('auto', callback);

            expect(manager.getCurrentTheme()).toBe('dark');
        });

        it('should detect light theme from html[theme~="light"]', () => {
            document.documentElement.setAttribute('theme', 'light');

            const callback = vi.fn();
            manager.initialize('auto', callback);

            expect(manager.getCurrentTheme()).toBe('light');
        });

        it('should detect dark theme from OS preference', () => {
            window.matchMedia = vi.fn().mockImplementation((query: string) => ({
                matches: query === '(prefers-color-scheme: dark)',
                media: query,
                onchange: null,
                addListener: vi.fn(),
                removeListener: vi.fn(),
                addEventListener: vi.fn(),
                removeEventListener: vi.fn(),
                dispatchEvent: vi.fn(),
            }));

            const callback = vi.fn();
            manager.initialize('auto', callback);

            expect(manager.getCurrentTheme()).toBe('dark');
        });
    });

    describe('theme type changes', () => {
        it('should handle theme type change from auto to dark', () => {
            const callback = vi.fn();
            manager.initialize('auto', callback);

            manager.handleThemeTypeChange('dark');

            expect(manager.getCurrentTheme()).toBe('dark');
        });

        it('should handle theme type change from dark to light', () => {
            const callback = vi.fn();
            manager.initialize('dark', callback);

            manager.handleThemeTypeChange('light');

            expect(manager.getCurrentTheme()).toBe('light');
        });

        it('should handle theme type change to auto', () => {
            const callback = vi.fn();
            manager.initialize('dark', callback);

            manager.handleThemeTypeChange('auto');

            // Should detect from document/OS
            expect(['light', 'dark']).toContain(manager.getCurrentTheme());
        });
    });

    describe('cleanup', () => {
        it('should clean up without errors', () => {
            const callback = vi.fn();
            manager.initialize('auto', callback);

            expect(() => manager.cleanup()).not.toThrow();
        });

        it('should be safe to call cleanup multiple times', () => {
            // expect(true).toBe(true) 恒成立、等于没有断言：
            // 即便 cleanup() 变成抛异常或状态残留也照样通过。
            // 改为断言「重复 cleanup 不抛」+「最终状态确实已清理」。
            expect(() => {
                manager.cleanup();
                manager.cleanup();
                manager.cleanup();
            }).not.toThrow();

            // 断言可观测的最终状态：dark 主题注入的 CSS 变量与 style 节点已被移除。
            expect(document.documentElement.style.getPropertyValue('--ck-color-base-background')).toBe('');
            expect(document.getElementById('vaadin-ckeditor-dark-theme')).toBeNull();
        });
    });

    describe('reference counting', () => {
        it('should handle multiple initializations', () => {
            const callback1 = vi.fn();
            const callback2 = vi.fn();

            const manager1 = new ThemeManager();
            const manager2 = new ThemeManager();

            manager1.initialize('dark', callback1);
            manager2.initialize('dark', callback2);

            expect(manager1.getCurrentTheme()).toBe('dark');
            expect(manager2.getCurrentTheme()).toBe('dark');

            manager1.cleanup();
            manager2.cleanup();
        });
    });
});

describe('ThemeManager CSS variable injection', () => {
    it('should inject dark theme CSS variables', () => {
        const manager = new ThemeManager();
        const callback = vi.fn();

        manager.initialize('dark', callback);

        // Theme manager injects CSS variables for dark mode
        expect(callback).toHaveBeenCalledWith('dark');

        manager.cleanup();
    });
});

describe('DARK_THEME_VARS — CKEditor 48 AI tokens', () => {
    const REQUIRED_V48_AI_TOKENS = [
        '--ck-color-ai-chat-primary-button-background',
        '--ck-color-ai-chat-primary-button-hover-background',
        '--ck-color-ai-chat-primary-button-text',
        '--ck-color-ai-chat-input-background',
        '--ck-color-ai-chat-input-border',
        '--ck-color-ai-chat-border-main',
        '--ck-color-ai-header-icon',
        '--ck-color-ai-notification-error-background',
        '--ck-color-ai-suggestion-marker-insertion-background',
        '--ck-color-ai-suggestion-marker-deletion-background',
    ] as const;

    it('should include all CKEditor 48 AI dark-mode tokens', async () => {
        const { DARK_THEME_VARS } = await import('./theme-manager');

        for (const token of REQUIRED_V48_AI_TOKENS) {
            expect(DARK_THEME_VARS).toHaveProperty(token);
            expect(DARK_THEME_VARS[token]).toMatch(/^hsla?\(/);
        }
    });
});

describe('ThemeManager edge cases', () => {
    let originalMatchMedia: typeof window.matchMedia;

    beforeEach(() => {
        originalMatchMedia = window.matchMedia;
        // Mock matchMedia for edge case tests
        window.matchMedia = vi.fn().mockImplementation((query: string) => ({
            matches: false,
            media: query,
            onchange: null,
            addListener: vi.fn(),
            removeListener: vi.fn(),
            addEventListener: vi.fn(),
            removeEventListener: vi.fn(),
            dispatchEvent: vi.fn(),
        }));
    });

    afterEach(() => {
        window.matchMedia = originalMatchMedia;
    });

    it('should handle invalid theme type gracefully', () => {
        const manager = new ThemeManager();
        const callback = vi.fn();

        // TypeScript would prevent this, but test runtime behavior
        // The implementation takes the 'else' branch for non-'auto' values
        // and passes the value to applyTheme, which sets it as currentTheme
        // This is intentional - TypeScript enforces valid values at compile time
        manager.initialize('invalid' as any, callback);

        // The callback is called with whatever was passed in (no runtime validation)
        expect(callback).toHaveBeenCalled();

        // After initialize, cleanup should work without errors
        expect(() => manager.cleanup()).not.toThrow();
    });

    it('should handle auto mode with mocked matchMedia', () => {
        const manager = new ThemeManager();
        const callback = vi.fn();

        // Should not throw even in edge cases
        expect(() => manager.initialize('auto', callback)).not.toThrow();

        manager.cleanup();
    });
});

// issue #145：CKEditor 把 Style 面板按钮标签底色硬编码为 #f0f0f0（浅灰），
// 暗色主题下 --ck-color-text 变为近白，标签文字完全不可读。暗色变量必须覆盖这组 token。
describe('DARK_THEME_VARS style panel tokens (issue #145)', () => {
    it('overrides the style panel label background so labels stay readable', () => {
        expect(DARK_THEME_VARS['--ck-style-panel-button-label-background']).toBeDefined();
        expect(DARK_THEME_VARS['--ck-style-panel-button-hover-label-background']).toBeDefined();
        expect(DARK_THEME_VARS['--ck-style-panel-button-hover-border-color']).toBeDefined();
    });

    it('applies the style panel label background to <html> when dark theme is active', () => {
        const manager = new ThemeManager();
        manager.applyTheme('dark');
        const value = document.documentElement.style.getPropertyValue('--ck-style-panel-button-label-background');
        manager.cleanup();
        expect(value).not.toBe('');
        expect(value).not.toBe('#f0f0f0');
    });
});
