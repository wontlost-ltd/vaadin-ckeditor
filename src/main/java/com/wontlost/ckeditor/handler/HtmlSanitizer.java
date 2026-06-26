package com.wontlost.ckeditor.handler;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * HTML content sanitizer.
 * Used to clean dangerous HTML tags and attributes before saving or displaying content.
 *
 * <p>Usage example:</p>
 * <pre>
 * // Use a predefined policy
 * editor.setHtmlSanitizer(HtmlSanitizer.withPolicy(SanitizationPolicy.STRICT));
 *
 * // Custom sanitization logic
 * editor.setHtmlSanitizer(html -&gt; {
 *     // Remove all script tags
 *     return html.replaceAll("&lt;script[^&gt;]*&gt;.*?&lt;/script&gt;", "");
 * });
 * </pre>
 *
 * @see SanitizationPolicy
 */
@FunctionalInterface
public interface HtmlSanitizer {

    /**
     * Sanitize HTML content.
     *
     * @param html the raw HTML content
     * @return the sanitized safe HTML
     */
    String sanitize(String html);

    /**
     * Sanitization policy.
     */
    enum SanitizationPolicy {
        /**
         * No sanitization, retain original content.
         */
        NONE,

        /**
         * Basic sanitization: remove scripts and dangerous tags.
         */
        BASIC,

        /**
         * Relaxed sanitization: allow most formatting tags.
         */
        RELAXED,

        /**
         * Strict sanitization: keep only basic text formatting.
         */
        STRICT
    }

    /**
     * Create a policy-based sanitizer.
     *
     * @param policy the sanitization policy
     * @return a sanitizer instance
     */
    static HtmlSanitizer withPolicy(SanitizationPolicy policy) {
        return html -> {
            if (html == null || html.isEmpty()) {
                return "";
            }

            switch (policy) {
                case NONE:
                    return html;

                case BASIC:
                    return Jsoup.clean(html, Safelist.basic());

                case RELAXED:
                    return Jsoup.clean(html, Safelist.relaxed());

                case STRICT:
                    // Allow only basic formatting
                    Safelist strict = new Safelist()
                        .addTags("p", "br", "b", "i", "u", "strong", "em")
                        .addTags("h1", "h2", "h3", "h4", "h5", "h6")
                        .addTags("ul", "ol", "li")
                        .addTags("blockquote", "pre", "code");
                    return Jsoup.clean(html, strict);

                default:
                    return Jsoup.clean(html, Safelist.basic());
            }
        };
    }

    /**
     * Create a sanitizer with a custom safelist.
     *
     * @param safelist the Jsoup safelist configuration
     * @return a sanitizer instance
     */
    static HtmlSanitizer withSafelist(Safelist safelist) {
        return html -> {
            if (html == null || html.isEmpty()) {
                return "";
            }
            return Jsoup.clean(html, safelist);
        };
    }

    /**
     * Compose sanitizers (chain execution).
     *
     * @param other another sanitizer
     * @return a composed sanitizer
     */
    default HtmlSanitizer andThen(HtmlSanitizer other) {
        return html -> other.sanitize(this.sanitize(html));
    }

    /**
     * A no-op sanitizer that passes content through unchanged.
     *
     * <p>透传语义：原样返回输入，包括 {@code null}。与 {@link #withPolicy} / {@link #withSafelist}
     * 不同（后者把 null/空串归一为 {@code ""}）；这是有意为之，以保持 passthrough 的透明性。
     * 标准调用路径在 {@code ContentManager.getSanitizedValue} 中已先行守卫 null，因此实际不会
     * 把 null 传入此处。</p>
     *
     * @return a passthrough sanitizer that returns its input verbatim (null in, null out)
     */
    static HtmlSanitizer passthrough() {
        return html -> html;
    }
}
