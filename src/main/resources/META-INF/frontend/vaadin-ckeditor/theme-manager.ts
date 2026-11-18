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

    // CKEditor 49 基础 token：组件在未设置旧版 --ck-color-* 变量时回退到这些亮色默认值
    // （如 #f3f4f7 悬停底色），暗色下必须一并覆盖，否则出现亮块（issue #150）。
    '--ck-color-base-border': 'hsl(257, 3%, 43%)',
    '--ck-color-base-border-light': 'hsl(270, 1%, 35%)',
    '--ck-color-base-text-light': 'hsl(0, 0%, 75%)',
    '--ck-color-base-hover': 'hsla(0, 0%, 100%, 0.1)',
    '--ck-color-base-hover-strong': 'hsla(0, 0%, 100%, 0.16)',
    '--ck-color-base-selected': 'hsla(217, 91%, 60%, 0.25)',
    '--ck-color-base-selected-hover': 'hsla(217, 91%, 60%, 0.35)',
    '--ck-color-base-action-muted': 'hsla(217, 91%, 60%, 0.2)',
    '--ck-color-base-error-shadow': 'hsla(0, 70%, 50%, 0.35)',
    '--ck-color-interactive-disabled-surface': 'hsl(270, 1%, 24%)',
    '--ck-color-surface-skeleton': 'hsl(270, 1%, 29%)',
    '--ck-color-surface-skeleton-highlight': 'hsl(270, 1%, 35%)',
    '--ck-color-text-placeholder': 'hsl(0, 0%, 66%)',

    // 按钮默认透明，与亮色主题一致；此前的实心灰底在工具栏上呈现为一个个方块
    '--ck-color-button-default-background': 'transparent',
    '--ck-color-button-default-hover-background': 'hsla(0, 0%, 100%, 0.1)',
    '--ck-color-button-default-active-background': 'hsla(0, 0%, 100%, 0.16)',
    '--ck-color-button-default-active-shadow': 'hsl(270, 2%, 23%)',
    '--ck-color-button-default-disabled-background': 'transparent',
    // split button（insertImage、insertTable 等）悬停 / 展开时未被指向的那一半（issue #150）
    '--ck-color-split-button-hover-background': 'hsla(0, 0%, 100%, 0.1)',
    // 下拉列表选中项：白字需 ≥ 4.5:1（#2563eb 上为 5.2:1）
    '--ck-color-list-button-on-background': 'hsl(221, 83%, 53%)',
    '--ck-color-list-button-on-background-focus': 'hsl(224, 76%, 48%)',
    '--ck-color-list-button-on-text': 'hsl(0, 0%, 100%)',
    '--ck-color-list-button-hover-background': 'hsla(0, 0%, 100%, 0.1)',
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
    // 列表项透明，透出所在面板的底色；否则在组件内（面板被 sticky-toolbar.css 设为 #374151）呈现为灰色方块
    '--ck-color-list-background': 'transparent',
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

    // Style 面板按钮标签底色（issue #145）：CKEditor 把它硬编码为 #f0f0f0，
    // 暗色下与近白的 --ck-color-text 叠在一起导致标签不可读，这里一并改为深色。
    '--ck-style-panel-button-label-background': 'hsl(270, 1%, 22%)',
    '--ck-style-panel-button-hover-label-background': 'hsl(270, 1%, 25%)',
    '--ck-style-panel-button-hover-border-color': 'hsl(257, 3%, 43%)',

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
 * Vaadin 25 `Page#setColorScheme` / `@ColorScheme` 写入的"跟随系统"取值：
 * SYSTEM / LIGHT_DARK → "light-dark"，DARK_LIGHT → "dark-light"。
 * Lumo 对它们按 prefers-color-scheme 切换，编辑器必须同样跟随系统，
 * 而不是因为字符串里含 "dark" 就固定为暗色。
 */
const SYSTEM_COLOR_SCHEME_TOKENS = ['light-dark', 'dark-light'];

function themeTokens(themeAttribute: string | null): string[] {
    return (themeAttribute ?? '').split(/\s+/).filter(Boolean);
}

/**
 * `<html theme>` 是否交由系统偏好决定明暗：未设置、只有与明暗无关的值，或为 light-dark / dark-light。
 */
export function followsSystemColorScheme(themeAttribute: string | null): boolean {
    const tokens = themeTokens(themeAttribute);
    return !tokens.includes('dark') && !tokens.includes('light')
        && (tokens.length === 0 || tokens.some((token) => SYSTEM_COLOR_SCHEME_TOKENS.includes(token)));
}

/**
 * 根据 `<html theme>` 与系统偏好计算编辑器主题。按空白分词匹配（与 Lumo 的 `[theme~=dark]` 一致）。
 * 优先级：dark / light 词 > 跟随系统（未设置或 light-dark / dark-light）> 其他取值视为亮色。
 */
export function resolveVaadinTheme(themeAttribute: string | null, prefersDark: boolean): 'light' | 'dark' {
    const tokens = themeTokens(themeAttribute);
    if (tokens.includes('dark')) {
        return 'dark';
    }
    if (followsSystemColorScheme(themeAttribute)) {
        return prefersDark ? 'dark' : 'light';
    }
    return 'light';
}

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
            if (followsSystemColorScheme(vaadinTheme)) {
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
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        this.applyTheme(resolveVaadinTheme(document.documentElement.getAttribute('theme'), prefersDark));
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
