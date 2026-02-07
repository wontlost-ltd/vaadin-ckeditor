package com.wontlost.ckeditor;

import com.wontlost.ckeditor.internal.ContentManager;
import com.wontlost.ckeditor.handler.HtmlSanitizer;
import org.jsoup.safety.Safelist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContentManager
 */
class ContentManagerTest {

    private ContentManager manager;

    @BeforeEach
    void setUp() {
        manager = new ContentManager(null); // No sanitizer
    }

    @Test
    @DisplayName("getPlainText should strip HTML tags")
    void getPlainTextStripsHtml() {
        String html = "<p>Hello <strong>World</strong>!</p>";
        String text = manager.getPlainText(html);
        assertEquals("Hello World!", text);
    }

    @Test
    @DisplayName("getPlainText should handle null input")
    void getPlainTextHandlesNull() {
        assertEquals("", manager.getPlainText(null));
    }

    @Test
    @DisplayName("getPlainText should handle empty input")
    void getPlainTextHandlesEmpty() {
        assertEquals("", manager.getPlainText(""));
    }

    @Test
    @DisplayName("getSanitizedHtml should use relaxed rules")
    void getSanitizedHtmlUsesRelaxedRules() {
        String html = "<p>Hello</p><script>alert('xss')</script>";
        String sanitized = manager.getSanitizedHtml(html);
        assertTrue(sanitized.contains("<p>Hello</p>"));
        assertFalse(sanitized.contains("<script>"));
    }

    @Test
    @DisplayName("sanitizeHtml should apply custom safelist")
    void sanitizeHtmlAppliesCustomSafelist() {
        String html = "<p>Hello <a href='#'>Link</a></p>";
        String sanitized = manager.sanitizeHtml(html, Safelist.simpleText());
        assertFalse(sanitized.contains("<a"));
        assertTrue(sanitized.contains("Hello"));
    }

    @Test
    @DisplayName("getSanitizedValue should return original when no sanitizer")
    void getSanitizedValueReturnsOriginalWithoutSanitizer() {
        String html = "<p>Hello</p>";
        assertEquals(html, manager.getSanitizedValue(html));
    }

    @Test
    @DisplayName("getSanitizedValue should apply sanitizer when set")
    void getSanitizedValueAppliesSanitizer() {
        HtmlSanitizer sanitizer = html -> html.toUpperCase();
        ContentManager managerWithSanitizer = new ContentManager(sanitizer);
        assertEquals("HELLO", managerWithSanitizer.getSanitizedValue("hello"));
    }

    @Test
    @DisplayName("isContentEmpty should return true for null")
    void isContentEmptyReturnsTrueForNull() {
        assertTrue(manager.isContentEmpty(null));
    }

    @Test
    @DisplayName("isContentEmpty should return true for empty string")
    void isContentEmptyReturnsTrueForEmpty() {
        assertTrue(manager.isContentEmpty(""));
    }

    @Test
    @DisplayName("isContentEmpty should return true for whitespace only HTML")
    void isContentEmptyReturnsTrueForWhitespace() {
        assertTrue(manager.isContentEmpty("<p>   </p>"));
    }

    @Test
    @DisplayName("isContentEmpty should return false for content")
    void isContentEmptyReturnsFalseForContent() {
        assertFalse(manager.isContentEmpty("<p>Hello</p>"));
    }

    @Test
    @DisplayName("getCharacterCount should count text characters")
    void getCharacterCountCountsText() {
        String html = "<p>Hello World</p>";
        assertEquals(11, manager.getCharacterCount(html));
    }

    @Test
    @DisplayName("getWordCount should count words")
    void getWordCountCountsWords() {
        String html = "<p>Hello World from CKEditor</p>";
        assertEquals(4, manager.getWordCount(html));
    }

    @Test
    @DisplayName("getWordCount should return 0 for empty content")
    void getWordCountReturnsZeroForEmpty() {
        assertEquals(0, manager.getWordCount(""));
        assertEquals(0, manager.getWordCount("<p></p>"));
    }

    @Test
    @DisplayName("getWordCount should count Chinese characters individually")
    void getWordCountCountsChineseCharacters() {
        String html = "<p>你好世界</p>";
        assertEquals(4, manager.getWordCount(html)); // Each CJK character counts as one word
    }

    @Test
    @DisplayName("getWordCount should count mixed CJK and Latin correctly")
    void getWordCountMixedCjkAndLatin() {
        // "Hello 你好 World" should count as: "Hello"=1, "你好"=2 CJK chars, "World"=1 = 4
        String html = "<p>Hello 你好 World</p>";
        assertEquals(4, manager.getWordCount(html));
    }

    @Test
    @DisplayName("getWordCount should count mixed token with CJK and non-CJK parts")
    void getWordCountMixedToken() {
        // "test中文test" (no spaces) should count CJK chars (2) + 1 for non-CJK = 3
        String html = "<p>test中文test</p>";
        assertEquals(3, manager.getWordCount(html));
    }

    @Test
    @DisplayName("normalizeForComparison should remove extra whitespace")
    void normalizeForComparisonRemovesWhitespace() {
        String html = "<p>Hello  World</p>  <p>Test</p>";
        String normalized = manager.normalizeForComparison(html);
        assertFalse(normalized.contains("  ")); // No double spaces
    }
}
