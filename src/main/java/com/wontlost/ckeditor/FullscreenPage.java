package com.wontlost.ckeditor;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Width of the page shown in CKEditor's fullscreen mode (the {@code FULLSCREEN} plugin).
 *
 * <ul>
 *   <li>{@link #A4_FIT} (default): an A4-proportioned page whose width follows the available width.
 *       Width = the editing column (fullscreen area minus sidebars that have content, such as the document
 *       outline or comments) minus a gutter on each side
 *       equal to the fullscreen top margin ({@code --ck-fullscreen-editor-top-margin}, 28px);
 *       the page padding (20mm × 12mm of 210mm) and its minimum height (297mm of 210mm) scale with that
 *       width. Window resizing and browser zoom therefore change the page size. The page grows downwards
 *       with the content, as in CKEditor's own layout; its height never limits the width.</li>
 *   <li>{@link #A4}: CKEditor's fixed A4 width, {@code 795.701px} (the look before 6.2.0).</li>
 *   <li>{@link #FULL_WIDTH}: the page fills the editing column, without gutters.</li>
 *   <li>{@link #custom(String)}: any CSS width, such as {@code 1200px}, {@code 80%} or
 *       {@code min(1400px, 90%)}; percentages refer to the editing column.</li>
 * </ul>
 *
 * <p>Applies to the editing view and the source-editing view. An application-wide
 * {@code --ck-fullscreen-editable-width} set on {@code html} overrides this setting for every editor.</p>
 */
public final class FullscreenPage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** A4 proportions, width follows the available width (default). */
    public static final FullscreenPage A4_FIT = new FullscreenPage("a4-fit", null);

    /** CKEditor's fixed A4 width (795.701px). */
    public static final FullscreenPage A4 = new FullscreenPage("a4", null);

    /** Fills the available width. */
    public static final FullscreenPage FULL_WIDTH = new FullscreenPage("full-width", null);

    private static final int MAX_CUSTOM_LENGTH = 100;

    /** 允许的长度单位（含视口与容器查询单位） */
    private static final Set<String> UNITS = Set.of(
            "px", "em", "rem", "ex", "ch", "cm", "mm", "q", "in", "pt", "pc",
            "vw", "vh", "vmin", "vmax", "svw", "lvw", "dvw", "cqw", "cqi");

    /** 允许的 CSS 函数；其余函数（url、var、attr、env、expression 等）一律拒绝 */
    private static final Set<String> FUNCTIONS = Set.of("calc", "min", "max", "clamp");

    /**
     * 白名单字符：数字、小写字母、小数点、百分号、四则运算符、括号、逗号与空格。
     * 不含 ; : { } 引号、反斜杠等字符，因而无法闭合声明或注入其它属性。
     */
    private static final Pattern ALLOWED_CHARS = Pattern.compile("[0-9a-z.%+\\-*/(), ]+");
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z]+");
    /** 不含函数时必须是「数字 + 单位 / %」；函数内允许无单位的系数（calc(100% * 0.8)） */
    private static final Pattern DIMENSION = Pattern.compile("\\d+(\\.\\d+)?([a-z]+|%)");

    private final String mode;
    private final String cssWidth;

    private FullscreenPage(String mode, String cssWidth) {
        this.mode = mode;
        this.cssWidth = cssWidth;
    }

    /**
     * A page of the given CSS width.
     *
     * <p>Accepted: a number with a unit or {@code %} ({@code 1200px}, {@code 90vw}, {@code 80%}),
     * or {@code calc()}, {@code min()}, {@code max()}, {@code clamp()} combining them. Anything else
     * (keywords, {@code var()}, {@code url()}, quotes, semicolons, braces) is rejected, so the value
     * cannot inject CSS.</p>
     *
     * @param cssWidth CSS width, case-insensitive
     * @return the custom page width
     * @throws IllegalArgumentException if the value is not an accepted CSS width
     */
    public static FullscreenPage custom(String cssWidth) {
        return new FullscreenPage("custom", validateCssWidth(cssWidth));
    }

    static String validateCssWidth(String cssWidth) {
        if (cssWidth == null || cssWidth.isBlank()) {
            throw new IllegalArgumentException("Fullscreen page width must not be blank");
        }
        String value = cssWidth.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_CUSTOM_LENGTH || !ALLOWED_CHARS.matcher(value).matches() || !balanced(value)) {
            throw invalid(cssWidth);
        }
        Matcher m = IDENTIFIER.matcher(value);
        while (m.find()) {
            String id = m.group();
            boolean isFunction = m.end() < value.length() && value.charAt(m.end()) == '(' && FUNCTIONS.contains(id);
            boolean isUnit = m.start() > 0 && Character.isDigit(value.charAt(m.start() - 1)) && UNITS.contains(id);
            if (!isFunction && !isUnit) {
                throw invalid(cssWidth);
            }
        }
        boolean usesFunction = value.indexOf('(') >= 0;
        if (usesFunction ? !value.matches(".*[0-9].*") : !DIMENSION.matcher(value).matches()) {
            throw invalid(cssWidth);
        }
        return value;
    }

    private static boolean balanced(String value) {
        int depth = 0;
        for (char c : value.toCharArray()) {
            depth += c == '(' ? 1 : c == ')' ? -1 : 0;
            if (depth < 0) {
                return false;
            }
        }
        return depth == 0;
    }

    private static IllegalArgumentException invalid(String cssWidth) {
        return new IllegalArgumentException("Unsupported fullscreen page width '" + cssWidth
                + "': use a number with a unit or %, optionally inside calc(), min(), max() or clamp(), "
                + "e.g. 1200px, 80% or min(1400px, 90%)");
    }

    /** Client-side mode name: {@code a4-fit}, {@code a4}, {@code full-width} or {@code custom}. */
    public String getMode() {
        return mode;
    }

    /** The CSS width of a {@link #custom(String)} page (lower-cased and trimmed), otherwise {@code null}. */
    public String getCssWidth() {
        return cssWidth;
    }

    /** 反序列化时把预定义值还原为同一实例，保持 == 比较可用 */
    private Object readResolve() {
        if (cssWidth == null) {
            switch (mode) {
                case "a4": return A4;
                case "full-width": return FULL_WIDTH;
                default: return A4_FIT;
            }
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof FullscreenPage other
                && mode.equals(other.mode) && Objects.equals(cssWidth, other.cssWidth));
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, cssWidth);
    }

    @Override
    public String toString() {
        return cssWidth == null ? mode : mode + "(" + cssWidth + ")";
    }
}
