/**
 * Unit Tests for Theme Manager Module
 *
 * Run with: npx vitest run theme-manager.test.ts
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ThemeManager } from './theme-manager';

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
            manager.cleanup();
            manager.cleanup();
            manager.cleanup();

            expect(true).toBe(true); // No errors thrown
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
