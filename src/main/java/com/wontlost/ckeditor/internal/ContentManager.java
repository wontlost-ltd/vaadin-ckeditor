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
        return countWords(getPlainText(html));
    }

    /**
     * 内容统计结果：纯文本字符数与词数。
     *
     * @param characterCount 纯文本字符数
     * @param wordCount 词数（CJK 每字算一词）
     */
    public record ContentStats(int characterCount, int wordCount) {
    }

    /**
     * 一次解析同时计算字符数与词数（review 发现：分别调用 getCharacterCount/getWordCount
     * 会对同一 HTML 重复 Jsoup.parse）。需要同时展示字/词数时用本方法避免重复解析。
     *
     * @param html HTML content
     * @return 字符数与词数
     */
    public ContentStats getContentStats(String html) {
        String text = getPlainText(html);
        return new ContentStats(text.length(), countWords(text));
    }

    /**
     * 从纯文本计算词数（纯函数，便于单测；不触碰 HTML 解析）。
     *
     * <p>分词规则保持不变：按空白切分；CJK（{@link Character.UnicodeScript#HAN}）字符每个计一词，
     * 词内非 CJK 部分整体再计一词。CJK 判定刻意沿用 UnicodeScript.HAN 以保持既有计数行为，
     * 不改用 isIdeographic（二者结果不同，会改变用户的词数）。</p>
     *
     * @param text 纯文本
     * @return 词数
     */
    public static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        int count = 0;
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            // 单次遍历统计 CJK 字符（避免 toCharArray 额外分配）
            int cjkChars = 0;
            for (int i = 0; i < word.length(); i++) {
                if (Character.UnicodeScript.of(word.charAt(i)) == Character.UnicodeScript.HAN) {
                    cjkChars++;
                }
            }
            if (cjkChars > 0) {
                // CJK 每字算一词，词内非 CJK 部分整体再算一词
                boolean hasNonCjk = word.length() > cjkChars;
                count += cjkChars + (hasNonCjk ? 1 : 0);
            } else {
                count += 1;
            }
        }
        return count;
    }
}
