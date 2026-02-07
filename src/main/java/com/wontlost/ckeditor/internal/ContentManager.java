package com.wontlost.ckeditor.internal;

import com.wontlost.ckeditor.handler.HtmlSanitizer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Internal class for managing editor content.
 * Handles content retrieval, setting, sanitization, and transformation.
 *
 * <p>This class is an internal API and should not be used directly by external code.</p>
 */
public class ContentManager {

    private final HtmlSanitizer htmlSanitizer;

    /**
     * Create a content manager.
     *
     * @param htmlSanitizer HTML sanitizer, may be null
     */
    public ContentManager(HtmlSanitizer htmlSanitizer) {
        this.htmlSanitizer = htmlSanitizer;
    }

    /**
     * Get sanitized HTML content.
     *
     * @param html raw HTML
     * @return sanitized HTML, or raw content if no sanitizer is set
     */
    public String getSanitizedValue(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        if (htmlSanitizer != null) {
            return htmlSanitizer.sanitize(html);
        }
        return html;
    }

    /**
     * Convert HTML to plain text.
     *
     * @param html HTML content
     * @return plain text content
     */
    public String getPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    /**
     * Sanitize HTML using relaxed rules.
     *
     * @param html HTML content
     * @return sanitized HTML
     */
    public String getSanitizedHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.clean(html, Safelist.relaxed());
    }

    /**
     * Sanitize HTML using custom rules.
     *
     * @param html HTML content
     * @param safelist sanitization rules
     * @return sanitized HTML
     */
    public String sanitizeHtml(String html, Safelist safelist) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.clean(html, safelist);
    }

    /**
     * Normalize HTML content for comparison.
     *
     * @param html HTML content
     * @return normalized HTML
     */
    public String normalizeForComparison(String html) {
        if (html == null) {
            return "";
        }
        // Remove excess whitespace and normalize line breaks
        return html.trim()
            .replaceAll("\\s+", " ")
            .replaceAll(">\\s+<", "><");
    }

    /**
     * Check whether the content is empty.
     *
     * @param html HTML content
     * @return true if the content is empty or contains only whitespace tags
     */
    public boolean isContentEmpty(String html) {
        if (html == null || html.isEmpty()) {
            return true;
        }
        String text = getPlainText(html);
        return text.trim().isEmpty();
    }

    /**
     * Estimate the character count of the content (excluding HTML tags).
     *
     * @param html HTML content
     * @return character count
     */
    public int getCharacterCount(String html) {
        String text = getPlainText(html);
        return text.length();
    }

    /**
     * Estimate the word count of the content.
     *
     * @param html HTML content
     * @return word count
     */
    public int getWordCount(String html) {
        String text = getPlainText(html);
        if (text.trim().isEmpty()) {
            return 0;
        }
        // Simple word splitting with CJK character counting support
        String[] words = text.trim().split("\\s+");
        int count = 0;
        for (String word : words) {
            if (!word.isEmpty()) {
                // Each CJK character counts as one word
                int cjkChars = 0;
                for (char c : word.toCharArray()) {
                    if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                        cjkChars++;
                    }
                }
                count += cjkChars > 0 ? cjkChars : 1;
            }
        }
        return count;
    }
}
