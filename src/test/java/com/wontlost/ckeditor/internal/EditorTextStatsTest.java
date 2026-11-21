package com.wontlost.ckeditor.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link EditorTextStats}：按 CKEditor WordCount 的规则统计。期望值按 ckeditor5-word-count 49.1.0 的算法推得
 * （纯文本去掉块边界后的长度；正则 ([\p{L}\p{N}]+\S?)+ 的匹配数）；浏览器中与插件的一致性由
 * e2e word-count.spec.ts 对同一批内容逐条比对。
 */
class EditorTextStatsTest {

    @ParameterizedTest(name = "{0} → {1} words, {2} chars")
    @CsvSource(delimiter = '|', value = {
        "''                                                  | 0 | 0",
        "<p></p>                                             | 0 | 0",
        "<p>&nbsp;</p>                                       | 0 | 0",
        "<p>Hello world</p>                                  | 2 | 11",
        "<p>Hello</p><p>world</p>                            | 2 | 10",
        "<p>a  b   c</p>                                     | 3 | 5",
        "<p>  leading and trailing  </p>                     | 3 | 20",
        "<p>a <strong> b</strong> c</p>                      | 3 | 5",
        "<p>bold<strong>er</strong></p>                      | 1 | 6",
        "<p>line<br>break</p>                                | 2 | 9",
        "<p>a&nbsp;&nbsp;b</p>                               | 2 | 4",
        "<ul><li>one</li><li>two three</li></ul>             | 3 | 12",
        "<h2>Title</h2><blockquote><p>quote</p></blockquote> | 2 | 10",
        "<table><tr><td>a</td><td>b</td></tr></table>        | 2 | 2",
        "<p>don't stop-me now.</p>                           | 3 | 18",
        "<p>你好世界</p>                                       | 1 | 4",
        "<p>héllo wörld 123</p>                              | 3 | 15",
        "<p>emoji 😀 here</p>                                | 2 | 13",
        "<p>... --- !!!</p>                                  | 0 | 11",
        "<p>x<img src='a.png'>y</p>                          | 2 | 2",
        "<p>a</p><script>var x = 1;</script>                 | 1 | 1",
    })
    @DisplayName("counts like CKEditor's WordCount plugin")
    void countsLikeWordCount(String html, int words, int characters) {
        assertThat(EditorTextStats.of(html)).isEqualTo(new EditorTextStats.Stats(words, characters));
    }

    @Test
    @DisplayName("code blocks keep indentation; their line breaks are not counted")
    void codeBlockKeepsWhitespace() {
        String html = "<pre><code class=\"language-java\">  int x;\n  x++;</code></pre>";
        assertThat(EditorTextStats.plainText(html)).contains("  int x;\n  x++;");
        // "  int x;" + "  x++;"：字符 8 + 6；词 int、x;、x+
        assertThat(EditorTextStats.of(html)).isEqualTo(new EditorTextStats.Stats(3, 14));
    }

    @Test
    @DisplayName("null and blank input count as empty")
    void nullAndBlank() {
        assertThat(EditorTextStats.of(null)).isEqualTo(new EditorTextStats.Stats(0, 0));
        assertThat(EditorTextStats.of("   ")).isEqualTo(new EditorTextStats.Stats(0, 0));
    }
}
