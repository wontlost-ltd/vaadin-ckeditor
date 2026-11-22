package com.wontlost.ckeditor;

import java.util.Locale;

/**
 * Color theme for code block syntax highlighting
 * ({@link VaadinCKEditor#setCodeBlockHighlighting(boolean)}).
 *
 * <p>Token colors apply to the editing area only; the HTML returned by {@code getValue()} never
 * contains highlight markup. Colors are taken from the highlight.js themes of the same name and
 * exposed as {@code --ck-code-hl-*} CSS variables, so any single color can be overridden.</p>
 *
 * @since 6.2.0
 */
public enum CodeHighlightTheme {

    /**
     * Follows the editor theme: {@link #GITHUB} colors on a light editor, {@link #GITHUB_DARK} colors
     * on a dark editor (Lumo dark, OS dark mode or {@link CKEditorTheme#DARK}). Keeps the editor's own
     * code block background. This is the default.
     */
    AUTO("auto"),

    /** highlight.js "github" colors on a light code block background. */
    GITHUB("github"),

    /** highlight.js "github-dark" colors on a dark code block background, in light and dark editors. */
    GITHUB_DARK("github-dark"),

    /** highlight.js "atom-one-light" colors on a light code block background. */
    ATOM_ONE_LIGHT("atom-one-light"),

    /** highlight.js "atom-one-dark" colors on a dark code block background, in light and dark editors. */
    ATOM_ONE_DARK("atom-one-dark"),

    /**
     * No built-in colors: the editing area only carries highlight.js {@code hljs-*} classes, so an
     * application stylesheet (for example any highlight.js theme CSS) decides the colors.
     */
    NONE("none");

    private final String jsName;

    CodeHighlightTheme(String jsName) {
        this.jsName = jsName;
    }

    /**
     * @return the name used by the client-side connector
     */
    public String getJsName() {
        return jsName;
    }

    /**
     * Parses a theme name as returned by {@link #getJsName()} or the enum constant name.
     *
     * @param name the theme name, case-insensitive
     * @return the matching theme
     * @throws IllegalArgumentException if the name is unknown
     */
    public static CodeHighlightTheme fromJsName(String name) {
        if (name != null) {
            String normalized = name.trim().toLowerCase(Locale.ROOT).replace('_', '-');
            for (CodeHighlightTheme theme : values()) {
                if (theme.jsName.equals(normalized)) {
                    return theme;
                }
            }
        }
        throw new IllegalArgumentException("Unknown code highlight theme: " + name);
    }

    @Override
    public String toString() {
        return jsName;
    }
}
