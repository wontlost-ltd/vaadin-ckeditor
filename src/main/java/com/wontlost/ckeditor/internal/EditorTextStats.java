package com.wontlost.ckeditor.internal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 按 CKEditor 5 WordCount 插件的规则统计 HTML 的词数与字符数（服务端校验用）。
 *
 * <p>WordCount 在模型上统计（{@code @ckeditor/ckeditor5-word-count} 49.1.0 的 {@code modelElementToPlainText}
 * 与 {@code _getWords} / {@code _getCharacters}）：</p>
 * <ul>
 *   <li>纯文本 = 各文本节点内容；相邻两个子节点中前一个是元素（块、软换行、行内图片）时插入 {@code \n}；</li>
 *   <li>字符数 = 去掉所有 {@code \n} 后的长度（UTF-16 码元，与 JavaScript {@code length} 一致）；</li>
 *   <li>词数 = 正则 {@code ([\p{L}\p{N}]+\S?)+}（全局、Unicode）的匹配次数。</li>
 * </ul>
 *
 * <p>本类从 HTML 近似重建同一份纯文本：块元素与 {@code <br>} / 行内对象分行；非 {@code <pre>} 中的连续空白
 * 折叠为一个空格并去掉行首尾空格（CKEditor 读入数据时的空白处理）；只含一个 {@code &nbsp;} 的块视为空块
 * （CKEditor 的空段落填充符）；{@code script} / {@code style} 等不产生文本。常见内容（段落、标题、列表、表格、
 * 引用、代码块、行内格式、CJK、emoji）与客户端计数一致，见 {@code EditorTextStatsTest}。</p>
 */
public final class EditorTextStats {

    /** CKEditor WordCount 的分词正则；UNICODE_CHARACTER_CLASS 使 \S 与 JavaScript 一样把 NBSP 等视为空白。 */
    private static final Pattern WORDS = Pattern.compile("([\\p{L}\\p{N}]+\\S?)+", Pattern.UNICODE_CHARACTER_CLASS);

    /** 可折叠空白（HTML 空白字符，不含 NBSP）。 */
    private static final Pattern COLLAPSIBLE = Pattern.compile("[ \\t\\n\\r\\f]+");

    /** 行内元素：内容并入当前行。其余元素（块、表格单元、图片等）都会分行。 */
    private static final Set<String> INLINE = Set.of(
        "a", "abbr", "b", "bdi", "bdo", "big", "cite", "code", "data", "del", "dfn", "em", "font", "i",
        "ins", "kbd", "label", "mark", "q", "s", "samp", "small", "span", "strike", "strong", "sub",
        "sup", "time", "tt", "u", "var");

    /** 不产生模型文本的元素。 */
    private static final Set<String> SKIPPED = Set.of("script", "style", "template", "noscript", "head", "title");

    /** 统计结果。 */
    public record Stats(int words, int characters) {
    }

    private EditorTextStats() {
    }

    /**
     * 统计 HTML 的词数与字符数。
     *
     * @param html 编辑器 HTML，可为 null
     * @return 词数与字符数
     */
    public static Stats of(String html) {
        if (html == null || html.isBlank()) {
            return new Stats(0, 0);
        }
        String text = plainText(html);
        int characters = text.length() - (int) text.chars().filter(c -> c == '\n').count();
        int words = 0;
        Matcher matcher = WORDS.matcher(text);
        while (matcher.find()) {
            words++;
        }
        return new Stats(words, characters);
    }

    /**
     * 重建 WordCount 统计所用的纯文本（行之间以 {@code \n} 分隔）。包级可见以便单测。
     */
    static String plainText(String html) {
        Lines lines = new Lines();
        walk(Jsoup.parseBodyFragment(html).body(), lines, false);
        lines.endLine();
        return String.join("\n", lines.done);
    }

    private static void walk(Element element, Lines lines, boolean preformatted) {
        for (Node child : element.childNodes()) {
            if (child instanceof TextNode text) {
                lines.append(text.getWholeText(), preformatted);
            } else if (child instanceof Element el) {
                visitElement(el, lines, preformatted);
            }
        }
    }

    private static void visitElement(Element el, Lines lines, boolean preformatted) {
        String tag = el.normalName();
        if (SKIPPED.contains(tag)) {
            return;
        }
        if (INLINE.contains(tag)) {
            walk(el, lines, preformatted);
            return;
        }
        // 块、<br>、图片等模型元素：前后分行
        lines.endLine();
        walk(el, lines, preformatted || "pre".equals(tag));
        lines.endLine();
    }

    /** 逐行收集文本；行 = 两个模型元素边界之间的连续文本。 */
    private static final class Lines {
        private final List<String> done = new ArrayList<>();
        private final StringBuilder current = new StringBuilder();
        /** 当前行含代码块文本：保留首尾空白，不做空块填充符判断。 */
        private boolean raw;

        void append(String text, boolean preformatted) {
            if (preformatted) {
                // 代码块中的换行在模型里是软换行（元素），同样分行；其余空白原样保留
                raw = true;
                String[] parts = text.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        endLine();
                        raw = true;
                    }
                    current.append(parts[i].replace("\r", ""));
                }
                return;
            }
            String collapsed = COLLAPSIBLE.matcher(text).replaceAll(" ");
            if (collapsed.startsWith(" ") && (current.isEmpty() || current.charAt(current.length() - 1) == ' ')) {
                collapsed = collapsed.substring(1);
            }
            current.append(collapsed);
        }

        void endLine() {
            String line = current.toString();
            current.setLength(0);
            if (raw) {
                raw = false;
                done.add(line);
                return;
            }
            if (line.endsWith(" ")) {
                line = line.substring(0, line.length() - 1);
            }
            if (line.startsWith(" ")) {
                line = line.substring(1);
            }
            if (" ".equals(line)) {
                line = "";
            }
            done.add(line);
        }
    }
}
