package com.wontlost.ckeditor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.wontlost.ckeditor.JsonUtil.createArrayNode;
import static com.wontlost.ckeditor.JsonUtil.createObjectNode;

/**
 * Typed configuration of CKEditor's
 * <a href="https://ckeditor.com/docs/ckeditor5/latest/features/html/general-html-support.html">General HTML
 * Support</a> (GHS), the {@code htmlSupport} option. Pass it to {@link CKEditorConfig#setHtmlSupport(HtmlSupport)}.
 *
 * <pre>
 * config.setHtmlSupport(HtmlSupport.create()
 *     // &lt;div&gt;, &lt;section&gt;, &lt;article&gt; with any class
 *     .allow(HtmlSupport.Rule.elements("div", "section", "article").classes(true))
 *     // collapsible blocks
 *     .allow(HtmlSupport.Rule.elements("details", "summary").attributes("open"))
 *     // only YouTube embeds
 *     .allowIframes()
 *     .allow(HtmlSupport.Rule.element("iframe")
 *         .attribute("src", Pattern.compile("^https://www\\.youtube(-nocookie)?\\.com/embed/"))
 *         .attributes("width", "height", "allowfullscreen", "title"))
 *     // classes and two styles on paragraphs
 *     .allow(HtmlSupport.Rule.element("p").classes("lead", "note").styles("color", "text-align")));
 * </pre>
 *
 * <p><b>Safe by default.</b> Unless explicitly enabled with {@link #allowScripts()}, {@link #allowIframes()} or
 * {@link #allowEventHandlers()}, three {@code disallow} rules are always added (GHS applies {@code disallow}
 * after {@code allow}): {@code <script>} elements, {@code <iframe>} elements, and {@code on*} event handler
 * attributes on every element. CKEditor has no full XSS filter: anything allowed here ends up in
 * {@code getValue()}. Always sanitize on the server as well; see the User Guide.</p>
 *
 * <p>Regular expressions are Java {@link Pattern}s using the syntax common to Java and JavaScript (no
 * {@code \Q...\E}, possessive quantifiers or inline flags); they are evaluated by the browser. Supported
 * flags: {@link Pattern#CASE_INSENSITIVE} ({@code i}), {@link Pattern#MULTILINE} ({@code m}),
 * {@link Pattern#DOTALL} ({@code s}).</p>
 */
public final class HtmlSupport implements Serializable {

    /**
     * JSON marker for a regular expression: {@code {"$regexp": "source", "$flags": "i"}}. JSON has no RegExp
     * type; the connector turns such objects into {@code RegExp} inside {@code htmlSupport.allow} /
     * {@code disallow}.
     */
    static final String REGEXP_KEY = "$regexp";
    static final String FLAGS_KEY = "$flags";

    private final List<Rule> allow = new ArrayList<>();
    private final List<Rule> disallow = new ArrayList<>();
    private final List<String> allowEmpty = new ArrayList<>();
    private boolean scripts;
    private boolean iframes;
    private boolean eventHandlers;
    private Boolean preserveEmptyBlocksInEditingView;
    private Boolean fullPageAllowRenderStylesFromHead;
    private JsonNode iframeSandbox;

    private HtmlSupport() {
    }

    /**
     * @return an empty configuration (only the safe-by-default {@code disallow} rules)
     */
    public static HtmlSupport create() {
        return new HtmlSupport();
    }

    /**
     * Allow every element with all attributes, classes and styles, except {@code <script>}, {@code <iframe>}
     * and {@code on*} attributes (enable those with {@link #allowScripts()}, {@link #allowIframes()},
     * {@link #allowEventHandlers()}). This is what {@link CKEditorConfig#setHtmlSupport(boolean)
     * setHtmlSupport(true)} uses.
     *
     * @return the configuration
     */
    public static HtmlSupport allowAll() {
        return create().allow(Rule.anyElement().attributes(true).classes(true).styles(true));
    }

    /**
     * Add an {@code htmlSupport.allow} rule.
     *
     * @param rule the rule
     * @return this
     */
    public HtmlSupport allow(Rule rule) {
        allow.add(Objects.requireNonNull(rule, "rule"));
        return this;
    }

    /**
     * Add an {@code htmlSupport.disallow} rule. A rule with only a name removes the element; a rule with
     * attributes, classes or styles removes just those.
     *
     * @param rule the rule
     * @return this
     */
    public HtmlSupport disallow(Rule rule) {
        disallow.add(Objects.requireNonNull(rule, "rule"));
        return this;
    }

    /**
     * {@code htmlSupport.allowEmpty}: inline elements kept even when empty (e.g. icon {@code <i>}).
     * They also need an {@link #allow(Rule)} rule.
     *
     * @param elementNames element names
     * @return this
     */
    public HtmlSupport allowEmpty(String... elementNames) {
        for (String name : elementNames) {
            allowEmpty.add(Rule.requireElementName(name));
        }
        return this;
    }

    /**
     * Opt in to {@code <script>} elements. <b>Security risk:</b> scripts in the content run wherever the HTML
     * is later displayed.
     *
     * @return this
     */
    public HtmlSupport allowScripts() {
        scripts = true;
        return this;
    }

    /**
     * Opt in to {@code <iframe>} elements (an {@link #allow(Rule)} rule is still needed). Restrict
     * {@code src} with {@link Rule#attribute(String, Pattern)}. In the editing view CKEditor sandboxes
     * iframes ({@link #iframeSandbox(boolean)}); the data returned by {@code getValue()} is not sandboxed.
     *
     * @return this
     */
    public HtmlSupport allowIframes() {
        iframes = true;
        return this;
    }

    /**
     * Opt in to {@code on*} event handler attributes ({@code onclick}, {@code onerror}, ...). <b>Security
     * risk:</b> they run JavaScript wherever the HTML is displayed.
     *
     * @return this
     */
    public HtmlSupport allowEventHandlers() {
        eventHandlers = true;
        return this;
    }

    /**
     * {@code htmlSupport.preserveEmptyBlocksInEditingView}.
     *
     * @param preserve whether empty blocks are kept in the editing view
     * @return this
     */
    public HtmlSupport preserveEmptyBlocksInEditingView(boolean preserve) {
        preserveEmptyBlocksInEditingView = preserve;
        return this;
    }

    /**
     * {@code htmlSupport.htmlIframeSandbox}: {@code true} (CKEditor's default) adds an empty
     * {@code sandbox} to iframes in the editing view, {@code false} leaves them unchanged.
     *
     * @param sandbox whether to sandbox iframes in the editing view
     * @return this
     */
    public HtmlSupport iframeSandbox(boolean sandbox) {
        iframeSandbox = JsonUtil.valueToTree(sandbox);
        return this;
    }

    /**
     * {@code htmlSupport.htmlIframeSandbox}: keep only these sandbox flags on iframes in the editing view,
     * e.g. {@code "allow-scripts", "allow-same-origin"}.
     *
     * @param flags allowed sandbox flags
     * @return this
     */
    public HtmlSupport iframeSandbox(String... flags) {
        iframeSandbox = JsonUtil.toArrayNode(flags);
        return this;
    }

    /**
     * {@code htmlSupport.fullPage.allowRenderStylesFromHead}, for the {@code FullPage} plugin (add it with
     * {@code CustomPlugin}). <b>Security risk:</b> CKEditor recommends a {@code sanitizeCss} callback, which
     * cannot be passed from Java; the styles from {@code <head>} are rendered unsanitized.
     *
     * @param allow whether styles from {@code <head>} are rendered in the editor
     * @return this
     */
    public HtmlSupport fullPageAllowRenderStylesFromHead(boolean allow) {
        fullPageAllowRenderStylesFromHead = allow;
        return this;
    }

    /**
     * @return the {@code htmlSupport} JSON object
     */
    public ObjectNode toJson() {
        ObjectNode json = createObjectNode();
        ArrayNode allowArr = createArrayNode();
        allow.forEach(rule -> allowArr.add(rule.toJson()));
        json.set("allow", allowArr);

        ArrayNode disallowArr = createArrayNode();
        disallow.forEach(rule -> disallowArr.add(rule.toJson()));
        // 安全默认值以 disallow 规则表达（GHS 中 disallow 优先于 allow），而非在 allow 上做特殊判断
        if (!scripts) {
            disallowArr.add(Rule.element("script").toJson());
        }
        if (!iframes) {
            disallowArr.add(Rule.element("iframe").toJson());
        }
        if (!eventHandlers) {
            disallowArr.add(Rule.anyElement()
                .attribute(Pattern.compile("^on", Pattern.CASE_INSENSITIVE)).toJson());
        }
        json.set("disallow", disallowArr);

        if (!allowEmpty.isEmpty()) {
            json.set("allowEmpty", JsonUtil.toArrayNode(allowEmpty.toArray(String[]::new)));
        }
        if (preserveEmptyBlocksInEditingView != null) {
            json.put("preserveEmptyBlocksInEditingView", preserveEmptyBlocksInEditingView);
        }
        if (iframeSandbox != null) {
            json.set("htmlIframeSandbox", iframeSandbox.deepCopy());
        }
        if (fullPageAllowRenderStylesFromHead != null) {
            json.set("fullPage", createObjectNode().put("allowRenderStylesFromHead", fullPageAllowRenderStylesFromHead));
        }
        return json;
    }

    static ObjectNode regexp(Pattern pattern) {
        ObjectNode node = createObjectNode();
        node.put(REGEXP_KEY, pattern.pattern());
        node.put(FLAGS_KEY, jsFlags(pattern.flags()));
        return node;
    }

    private static String jsFlags(int flags) {
        int supported = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL;
        if ((flags & ~supported) != 0) {
            throw new IllegalArgumentException(
                "Only CASE_INSENSITIVE, MULTILINE and DOTALL can be used in browser-side patterns");
        }
        StringBuilder js = new StringBuilder();
        if ((flags & Pattern.CASE_INSENSITIVE) != 0) js.append('i');
        if ((flags & Pattern.MULTILINE) != 0) js.append('m');
        if ((flags & Pattern.DOTALL) != 0) js.append('s');
        return js.toString();
    }

    /**
     * One GHS rule (CKEditor's {@code MatcherObjectPattern}): an element name plus the attributes, classes
     * and styles to match. Attributes and styles are serialized in the array form
     * ({@code [ "name", {key, value} ]}), which accepts names, patterns and values alike.
     */
    public static final class Rule implements Serializable {

        private static final Pattern ELEMENT_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9-]*");

        private final JsonNode name;
        private final Property attributes = new Property();
        private final Property classes = new Property();
        private final Property styles = new Property();

        private Rule(JsonNode name) {
            this.name = name;
        }

        /**
         * @param name element name, e.g. {@code "div"}
         * @return a rule for that element
         */
        public static Rule element(String name) {
            return new Rule(JsonUtil.valueToTree(requireElementName(name)));
        }

        /**
         * @param names element names
         * @return a rule for any of the elements ({@code /^(div|section)$/})
         */
        public static Rule elements(String... names) {
            if (names.length == 0) {
                throw new IllegalArgumentException("At least one element name is required");
            }
            if (names.length == 1) {
                return element(names[0]);
            }
            String alternatives = String.join("|", Arrays.stream(names).map(Rule::requireElementName).toList());
            return new Rule(regexp(Pattern.compile("^(" + alternatives + ")$")));
        }

        /**
         * @param namePattern pattern for the element name
         * @return a rule for the matching elements
         */
        public static Rule element(Pattern namePattern) {
            return new Rule(regexp(Objects.requireNonNull(namePattern, "namePattern")));
        }

        /**
         * @return a rule without a name, matching every element (as GHS does for rules without {@code name})
         */
        public static Rule anyElement() {
            return new Rule(null);
        }

        /** @param all {@code true} for every attribute; {@code false} adds nothing */
        public Rule attributes(boolean all) {
            attributes.all |= all;
            return this;
        }

        /** Allow these attributes with any value. */
        public Rule attributes(String... names) {
            for (String n : names) {
                attributes.items.add(JsonUtil.valueToTree(requireName(n, "attribute")));
            }
            return this;
        }

        /** Attributes whose name matches the pattern, any value (e.g. {@code ^data-}). */
        public Rule attribute(Pattern namePattern) {
            attributes.items.add(regexp(Objects.requireNonNull(namePattern, "namePattern")));
            return this;
        }

        /** The attribute with exactly this value. */
        public Rule attribute(String name, String value) {
            attributes.items.add(keyValue(JsonUtil.valueToTree(requireName(name, "attribute")),
                JsonUtil.valueToTree(Objects.requireNonNull(value, "value"))));
            return this;
        }

        /** The attribute with a value matching the pattern (e.g. an allowed {@code src} prefix). */
        public Rule attribute(String name, Pattern valuePattern) {
            attributes.items.add(keyValue(JsonUtil.valueToTree(requireName(name, "attribute")),
                regexp(Objects.requireNonNull(valuePattern, "valuePattern"))));
            return this;
        }

        /** @param all {@code true} for every class; {@code false} adds nothing */
        public Rule classes(boolean all) {
            classes.all |= all;
            return this;
        }

        /** These classes. */
        public Rule classes(String... names) {
            for (String n : names) {
                classes.items.add(JsonUtil.valueToTree(requireName(n, "class")));
            }
            return this;
        }

        /** Classes matching the pattern (e.g. {@code ^text-}). */
        public Rule classes(Pattern pattern) {
            classes.items.add(regexp(Objects.requireNonNull(pattern, "pattern")));
            return this;
        }

        /** @param all {@code true} for every style; {@code false} adds nothing */
        public Rule styles(boolean all) {
            styles.all |= all;
            return this;
        }

        /** These style properties with any value. */
        public Rule styles(String... properties) {
            for (String p : properties) {
                styles.items.add(JsonUtil.valueToTree(requireName(p, "style")));
            }
            return this;
        }

        /** The style property with exactly this value. */
        public Rule style(String property, String value) {
            styles.items.add(keyValue(JsonUtil.valueToTree(requireName(property, "style")),
                JsonUtil.valueToTree(Objects.requireNonNull(value, "value"))));
            return this;
        }

        /** The style property with a value matching the pattern. */
        public Rule style(String property, Pattern valuePattern) {
            styles.items.add(keyValue(JsonUtil.valueToTree(requireName(property, "style")),
                regexp(Objects.requireNonNull(valuePattern, "valuePattern"))));
            return this;
        }

        /** @return the rule as a GHS pattern object */
        public ObjectNode toJson() {
            ObjectNode json = createObjectNode();
            if (name != null) {
                json.set("name", name.deepCopy());
            }
            attributes.writeTo(json, "attributes");
            classes.writeTo(json, "classes");
            styles.writeTo(json, "styles");
            return json;
        }

        static String requireElementName(String name) {
            if (name == null || !ELEMENT_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid element name: " + name);
            }
            return name;
        }

        private static String requireName(String name, String kind) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("The " + kind + " name must not be blank");
            }
            return name;
        }

        private static ObjectNode keyValue(JsonNode key, JsonNode value) {
            ObjectNode node = createObjectNode();
            node.set("key", key);
            node.set("value", value);
            return node;
        }
    }

    /** 某一类属性的匹配项：{@code true}（全部）或名称 / 正则 / {key, value} 列表。 */
    private static final class Property implements Serializable {
        private boolean all;
        private final List<JsonNode> items = new ArrayList<>();

        void writeTo(ObjectNode rule, String key) {
            if (all) {
                rule.put(key, true);
            } else if (!items.isEmpty()) {
                ArrayNode arr = createArrayNode();
                items.forEach(item -> arr.add(item.deepCopy()));
                rule.set(key, arr);
            }
        }
    }
}
