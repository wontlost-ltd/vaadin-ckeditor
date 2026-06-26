/**
 * Theme Manager Module for VaadinCKEditor
 *
 * Handles theme management including:
 * - Dark theme CSS variable injection
 * - Vaadin theme synchronization
 * - OS dark mode detection
 * - Theme reference counting for multi-instance support
 */
import { createRefcount } from './dark-theme-refcount';

/**
 * Dark theme CSS variables for CKEditor
 */
export const DARK_THEME_VARS: Record<string, string> = {
    '--ck-color-base-foreground': 'hsl(270, 1%, 29%)',
    '--ck-color-base-background': 'hsl(270, 1%, 18%)',
    '--ck-color-focus-border': 'hsl(208, 90%, 62%)',
    '--ck-color-text': 'hsl(0, 0%, 98%)',
    '--ck-color-shadow-drop': 'hsla(0, 0%, 0%, 0.2)',
    '--ck-color-shadow-inner': 'hsla(0, 0%, 0%, 0.1)',
    '--ck-color-button-default-background': 'hsl(270, 1%, 29%)',
    '--ck-color-button-default-hover-background': 'hsl(270, 1%, 22%)',
    '--ck-color-button-default-active-background': 'hsl(270, 2%, 20%)',
    '--ck-color-button-default-active-shadow': 'hsl(270, 2%, 23%)',
    '--ck-color-button-default-disabled-background': 'hsl(270, 1%, 29%)',
    '--ck-color-button-on-background': 'hsl(255, 3%, 18%)',
    '--ck-color-button-on-hover-background': 'hsl(255, 4%, 16%)',
    '--ck-color-button-on-active-background': 'hsl(255, 4%, 14%)',
    '--ck-color-button-on-active-shadow': 'hsl(240, 3%, 19%)',
    '--ck-color-button-on-disabled-background': 'hsl(255, 3%, 18%)',
    '--ck-color-button-action-background': 'hsl(168, 76%, 42%)',
    '--ck-color-button-action-hover-background': 'hsl(168, 76%, 38%)',
    '--ck-color-button-action-active-background': 'hsl(168, 76%, 36%)',
    '--ck-color-button-action-active-shadow': 'hsl(168, 75%, 34%)',
    '--ck-color-button-action-disabled-background': 'hsl(168, 76%, 42%)',
    '--ck-color-button-action-text': 'hsl(0, 0%, 100%)',
    '--ck-color-button-save': 'hsl(120, 100%, 46%)',
    '--ck-color-button-cancel': 'hsl(15, 100%, 56%)',
    '--ck-color-dropdown-panel-background': 'hsl(270, 1%, 29%)',
    '--ck-color-dropdown-panel-border': 'hsl(255, 3%, 18%)',
    '--ck-color-split-button-hover-border': 'hsl(255, 3%, 18%)',
    '--ck-color-input-background': 'hsl(255, 3%, 18%)',
    '--ck-color-input-border': 'hsl(257, 3%, 43%)',
    '--ck-color-input-text': 'hsl(0, 0%, 98%)',
    '--ck-color-input-disabled-background': 'hsl(255, 4%, 21%)',
    '--ck-color-input-disabled-border': 'hsl(250, 3%, 38%)',
    '--ck-color-input-disabled-text': 'hsl(0, 0%, 46%)',
    '--ck-color-list-background': 'hsl(270, 1%, 29%)',
    '--ck-color-panel-background': 'hsl(270, 1%, 29%)',
    '--ck-color-panel-border': 'hsl(300, 1%, 22%)',
    '--ck-color-toolbar-background': 'hsl(270, 1%, 29%)',
    '--ck-color-toolbar-border': 'hsl(300, 1%, 22%)',
    '--ck-color-tooltip-background': 'hsl(252, 7%, 14%)',
    '--ck-color-tooltip-text': 'hsl(0, 0%, 93%)',
    '--ck-color-image-caption-background': 'hsl(270, 1%, 25%)',
    '--ck-color-image-caption-text': 'hsl(0, 0%, 90%)',
    '--ck-color-widget-blurred-border': 'hsl(270, 1%, 40%)',
    '--ck-color-widget-hover-border': 'hsl(43, 100%, 68%)',
    '--ck-color-widget-editable-focus-background': 'hsl(270, 1%, 22%)',
    '--ck-color-link-default': 'hsl(190, 100%, 75%)',
    '--ck-color-editable-blur-selection': 'hsl(270, 1%, 35%)',

    // CKEditor 48 AI 官方 token（暗色映射），与项目自定义 --ck-ai-bg-*/text-*/border-* 语义层互不冲突
    '--ck-color-ai-chat-primary-button-background': 'hsl(208, 90%, 45%)',
    '--ck-color-ai-chat-primary-button-hover-background': 'hsl(208, 90%, 40%)',
    '--ck-color-ai-chat-primary-button-text': 'hsl(0, 0%, 100%)',
    '--ck-color-ai-chat-input-background': 'hsl(255, 3%, 18%)',
    '--ck-color-ai-chat-input-border': 'hsl(257, 3%, 43%)',
    '--ck-color-ai-chat-border-main': 'hsl(257, 3%, 30%)',
    '--ck-color-ai-header-icon': 'hsl(0, 0%, 88%)',
    '--ck-color-ai-notification-error-background': 'hsl(355, 70%, 40%)',
    '--ck-color-ai-suggestion-marker-insertion-background': 'hsla(140, 70%, 45%, 0.25)',
    '--ck-color-ai-suggestion-marker-deletion-background': 'hsla(355, 70%, 45%, 0.25)',
};

/**
 * Global dark theme reference counter.
 * Tracks how many editor instances are using dark theme to ensure
 * CSS variables are only removed when the last dark theme instance is destroyed.
 */
let darkThemeRefcount = createRefcount();

/**
 * Dark theme style element ID
 */
const DARK_STYLE_ID = 'vaadin-ckeditor-dark-theme';

/**
 * Theme Manager class for handling CKEditor theme operations.
 * Manages dark theme CSS variables with reference counting for multi-instance support.
 */
export class ThemeManager {
    private isDarkThemeActive = false;
    private themeObserver?: MutationObserver;
    private darkModeMediaQuery?: MediaQueryList;
    private darkModeMediaListener?: (e: MediaQueryListEvent) => void;
    private currentTheme: 'light' | 'dark' = 'light';
    private onThemeChangeCallback?: (theme: 'light' | 'dark') => void;

    /**
     * Initialize theme system based on themeType setting.
     * @param themeType - 'auto' to watch Vaadin/OS, or 'light'/'dark' for explicit setting
     * @param onThemeChange - Callback when theme changes
     */
    initialize(themeType: 'auto' | 'light' | 'dark', onThemeChange?: (theme: 'light' | 'dark') => void): void {
        this.onThemeChangeCallback = onThemeChange;

        if (themeType === 'auto') {
            this.setupVaadinThemeObserver();
            this.setupOSDarkModeDetection();
            this.syncThemeWithVaadin();
        } else {
            this.applyTheme(themeType);
        }
    }

    /**
     * Handle themeType property changes.
     */
    handleThemeTypeChange(themeType: 'auto' | 'light' | 'dark'): void {
        if (themeType !== 'auto') {
            this.cleanupObservers();
            this.applyTheme(themeType);
        } else {
            this.initialize(themeType, this.onThemeChangeCallback);
        }
    }

    /**
     * Get the current theme.
     */
    getCurrentTheme(): 'light' | 'dark' {
        return this.currentTheme;
    }

    /**
     * Setup MutationObserver to watch Vaadin's [theme] attribute on <html>.
     */
    private setupVaadinThemeObserver(): void {
        this.cleanupVaadinObserver();

        this.themeObserver = new MutationObserver((mutations) => {
            for (const mutation of mutations) {
                if (mutation.attributeName === 'theme') {
                    this.syncThemeWithVaadin();
                }
            }
        });

        this.themeObserver.observe(document.documentElement, {
            attributes: true,
            attributeFilter: ['theme']
        });
    }

    /**
     * Setup OS-level dark mode detection via prefers-color-scheme.
     */
    private setupOSDarkModeDetection(): void {
        this.cleanupOSModeListener();

        this.darkModeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        this.darkModeMediaListener = (e: MediaQueryListEvent) => {
            const vaadinTheme = document.documentElement.getAttribute('theme');
            if (!vaadinTheme || (!vaadinTheme.includes('dark') && !vaadinTheme.includes('light'))) {
                this.applyTheme(e.matches ? 'dark' : 'light');
            }
        };

        this.darkModeMediaQuery.addEventListener('change', this.darkModeMediaListener);
    }

    /**
     * Sync CKEditor theme with Vaadin's current theme state.
     * Priority: Vaadin [theme] attribute > OS preference > light (default)
     */
    private syncThemeWithVaadin(): void {
        const vaadinTheme = document.documentElement.getAttribute('theme') || '';
        let targetTheme: 'light' | 'dark';

        if (vaadinTheme.includes('dark')) {
            targetTheme = 'dark';
        } else if (vaadinTheme.includes('light') || vaadinTheme) {
            targetTheme = 'light';
        } else {
            const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
            targetTheme = prefersDark ? 'dark' : 'light';
        }

        this.applyTheme(targetTheme);
    }

    /**
     * Apply a specific theme (light or dark).
     */
    applyTheme(theme: 'light' | 'dark'): void {
        this.currentTheme = theme;

        if (theme === 'dark') {
            this.initDarkTheme();
        } else {
            this.removeDarkTheme();
        }

        this.onThemeChangeCallback?.(theme);
    }

    /**
     * Initialize dark theme CSS variables.
     * Uses reference counting for multi-instance support.
     */
    private initDarkTheme(): void {
        if (this.isDarkThemeActive) {
            return;
        }
        this.isDarkThemeActive = true;
        darkThemeRefcount = darkThemeRefcount.acquire();

        if (darkThemeRefcount.justApplied) {
            const rootStyle = document.documentElement.style;
            Object.entries(DARK_THEME_VARS).forEach(([key, value]) => {
                rootStyle.setProperty(key, value);
            });

            if (!document.getElementById(DARK_STYLE_ID)) {
                const style = document.createElement('style');
                style.id = DARK_STYLE_ID;
                style.textContent = this.getDarkThemeStyles();
                document.head.appendChild(style);
            }
        }
    }

    /**
     * Remove dark theme CSS variables and styles.
     * Uses reference counting to only remove when last instance is destroyed.
     */
    removeDarkTheme(): void {
        if (!this.isDarkThemeActive) {
            return;
        }
        this.isDarkThemeActive = false;
        darkThemeRefcount = darkThemeRefcount.release();

        if (darkThemeRefcount.justRemoved) {
            const rootStyle = document.documentElement.style;
            Object.keys(DARK_THEME_VARS).forEach((key) => {
                rootStyle.removeProperty(key);
            });

            const darkStyle = document.getElementById(DARK_STYLE_ID);
            if (darkStyle) {
                darkStyle.remove();
            }
        }
    }

    /**
     * Get dark theme CSS styles for editable content area.
     */
    private getDarkThemeStyles(): string {
        return `
            .ck.ck-editor__editable:not(.ck-editor__nested-editable) {
                background: hsl(270, 1%, 18%) !important;
                color: hsl(0, 0%, 95%) !important;
            }
            .ck.ck-editor__editable:not(.ck-editor__nested-editable).ck-focused {
                background: hsl(270, 1%, 20%) !important;
            }
            .ck.ck-content p {
                color: hsl(0, 0%, 95%) !important;
            }
            .ck.ck-content h1, .ck.ck-content h2, .ck.ck-content h3,
            .ck.ck-content h4, .ck.ck-content h5, .ck.ck-content h6 {
                color: hsl(0, 0%, 98%) !important;
            }
            .ck.ck-content a {
                color: hsl(190, 100%, 75%) !important;
            }
            .ck.ck-content blockquote {
                border-left-color: hsl(270, 1%, 40%) !important;
                color: hsl(0, 0%, 80%) !important;
            }
            .ck.ck-content code {
                background: hsl(270, 1%, 25%) !important;
                color: hsl(0, 0%, 90%) !important;
            }
            .ck.ck-content pre {
                background: hsl(270, 1%, 15%) !important;
                color: hsl(0, 0%, 90%) !important;
            }
        `;
    }

    /**
     * Clean up Vaadin theme observer.
     */
    private cleanupVaadinObserver(): void {
        if (this.themeObserver) {
            this.themeObserver.disconnect();
            this.themeObserver = undefined;
        }
    }

    /**
     * Clean up OS dark mode listener.
     */
    private cleanupOSModeListener(): void {
        if (this.darkModeMediaQuery && this.darkModeMediaListener) {
            this.darkModeMediaQuery.removeEventListener('change', this.darkModeMediaListener);
            this.darkModeMediaListener = undefined;
            this.darkModeMediaQuery = undefined;
        }
    }

    /**
     * Clean up all theme observers.
     */
    cleanupObservers(): void {
        this.cleanupVaadinObserver();
        this.cleanupOSModeListener();
    }

    /**
     * Clean up all resources including dark theme.
     */
    cleanup(): void {
        this.cleanupObservers();
        if (this.isDarkThemeActive) {
            this.removeDarkTheme();
        }
    }
}
