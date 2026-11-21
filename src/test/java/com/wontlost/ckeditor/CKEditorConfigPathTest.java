package com.wontlost.ckeditor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link CKEditorConfig#set(String, tools.jackson.databind.JsonNode)} 的点号路径与类型化重载。
 */
class CKEditorConfigPathTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private CKEditorConfig config;

    @BeforeEach
    void setUp() {
        config = new CKEditorConfig();
    }

    @Test
    @DisplayName("documented collaboration example compiles and produces nested objects")
    void documentedExampleProducesNestedObjects() {
        config.set("cloudServices.tokenUrl", "https://your-server/token")
              .set("collaboration.channelId", "document-123");

        ObjectNode json = config.toJson();
        assertThat(json.get("cloudServices").get("tokenUrl").asString()).isEqualTo("https://your-server/token");
        assertThat(json.get("collaboration").get("channelId").asString()).isEqualTo("document-123");
        assertThat(json.has("cloudServices.tokenUrl")).isFalse();
    }

    @Test
    @DisplayName("dotted paths merge into existing nested objects")
    void dottedPathsMergeIntoExistingObjects() {
        config.set("cloudServices", Map.of("tokenUrl", "https://a/token"));
        config.set("cloudServices.webSocketUrl", "wss://a/ws");
        config.set("ai.openAI.apiUrl", "https://a/openai");
        config.set("ai.openAI.requestHeaders.Authorization", "Bearer x");
        config.set("ai.chat.context.document.enabled", true);

        ObjectNode json = config.toJson();
        assertThat(json.get("cloudServices").toString())
            .isEqualTo("{\"tokenUrl\":\"https://a/token\",\"webSocketUrl\":\"wss://a/ws\"}");
        assertThat(json.get("ai").toString()).isEqualTo(
            "{\"openAI\":{\"apiUrl\":\"https://a/openai\",\"requestHeaders\":{\"Authorization\":\"Bearer x\"}},"
                + "\"chat\":{\"context\":{\"document\":{\"enabled\":true}}}}");
    }

    @Test
    @DisplayName("dotted path merges into objects built by typed setters")
    void dottedPathMergesIntoTypedSetterObjects() {
        config.setToolbar("bold", "italic");
        config.setAutosave(2000);
        config.set("autosave.foo", 1);
        config.setUiViewportOffset(10, 0, 0, 0);
        config.set("ui.viewportOffset.top", 56.5);

        ObjectNode json = config.toJson();
        assertThat(json.get("autosave").get("waitingTime").asInt()).isEqualTo(2000);
        assertThat(json.get("autosave").get("foo").asInt()).isEqualTo(1);
        assertThat(json.get("ui").get("viewportOffset").get("top").asDouble()).isEqualTo(56.5);
        assertThat(json.get("ui").get("viewportOffset").get("left").asDouble()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("plain key with object value still replaces the whole object (backward compatible)")
    void plainKeyReplacesWholeObject() {
        config.set("cloudServices.tokenUrl", "https://a/token");
        config.set("cloudServices", Map.of("webSocketUrl", "wss://b/ws"));

        assertThat(config.toJson().get("cloudServices").toString()).isEqualTo("{\"webSocketUrl\":\"wss://b/ws\"}");
    }

    @Test
    @DisplayName("leaf value replaces previous value of any type")
    void leafReplacesPreviousValue() {
        config.set("collaboration.channelId", "one");
        config.set("collaboration.channelId", Map.of("x", 1));
        assertThat(config.toJson().get("collaboration").get("channelId").toString()).isEqualTo("{\"x\":1}");
        config.set("collaboration.channelId", "two");
        assertThat(config.getString("collaboration.channelId")).isEqualTo("two");
    }

    @Test
    @DisplayName("non-object parent is replaced by an object, as CKEditor Config does")
    void nonObjectParentIsReplaced() {
        config.set("toolbar", List.of("bold"));
        config.set("toolbar.shouldNotGroupWhenFull", true);
        assertThat(config.toJson().get("toolbar").toString()).isEqualTo("{\"shouldNotGroupWhenFull\":true}");
    }

    @Test
    @DisplayName("typed overloads serialize to the matching JSON types")
    void typedOverloads() {
        config.set("s", "text")
              .set("b", false)
              .set("i", 42)
              .set("l", 9_007_199_254_740_993L)
              .set("d", 1.5)
              .set("n", new BigDecimal("12.50"))
              .set("list", Arrays.asList(1, "two", true, null, Map.of("k", "v"), List.of(3)))
              .set("map", Map.of("k", List.of("a")))
              .set("node", MAPPER.createObjectNode().put("x", 1));

        ObjectNode json = config.toJson();
        assertThat(json.get("s").isString()).isTrue();
        assertThat(json.get("b").isBoolean()).isTrue();
        assertThat(json.get("i").isInt()).isTrue();
        assertThat(json.get("l").asLong()).as("long 不得经 double 丢精度").isEqualTo(9_007_199_254_740_993L);
        assertThat(json.get("d").asDouble()).isEqualTo(1.5);
        assertThat(json.get("n").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(json.get("list").toString()).isEqualTo("[1,\"two\",true,null,{\"k\":\"v\"},[3]]");
        assertThat(json.get("map").toString()).isEqualTo("{\"k\":[\"a\"]}");
        assertThat(json.get("node").toString()).isEqualTo("{\"x\":1}");
    }

    @Test
    @DisplayName("dotted keys inside object values are expanded and merged, arrays are not traversed")
    void dottedKeysInValuesAreExpanded() {
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("openAI.apiUrl", "https://a/openai");
        ai.put("openAI.requestHeaders", Map.of("Authorization", "Bearer x"));
        ai.put("openAI", Map.of("requestParameters", Map.of("model", "gpt")));
        ai.put("quickActions.extraCommands", List.of(Map.of("id.with.dot", "kept")));
        config.set("ai", ai);

        assertThat(config.toJson().get("ai").toString()).isEqualTo(
            "{\"openAI\":{\"apiUrl\":\"https://a/openai\",\"requestHeaders\":{\"Authorization\":\"Bearer x\"},"
                + "\"requestParameters\":{\"model\":\"gpt\"}},"
                + "\"quickActions\":{\"extraCommands\":[{\"id.with.dot\":\"kept\"}]}}");
    }

    @Test
    @DisplayName("existing call shapes still compile and resolve to the JsonNode overload")
    void existingCallShapesStillCompile() {
        // 编译期回归：新增重载不得让 set(key, null) 与 set(key, mapper.valueToTree(x)) 产生歧义
        config.set("plain", null);
        config.set("tree", MAPPER.valueToTree(Map.of("a.b", 1)));

        assertThat(config.toJson().get("plain").isNull()).isTrue();
        assertThat(config.toJson().get("tree").toString()).isEqualTo("{\"a\":{\"b\":1}}");
    }

    @Test
    @DisplayName("values Jackson cannot serialize are rejected with IllegalArgumentException")
    void unserializableValueRejected() {
        assertThatThrownBy(() -> config.set("x", new Boom()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'x'");
    }

    @Test
    @DisplayName("null stores JSON null and keeps the key")
    void nullStoresJsonNull() {
        config.set("a.b", (String) null);
        assertThat(config.hasConfig("a.b")).isTrue();
        assertThat(config.toJson().get("a").get("b").isNull()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "a..b", ".a", "a.", "."})
    @DisplayName("blank paths and empty segments are rejected")
    void invalidPathsAreRejected(String path) {
        assertThatThrownBy(() -> config.set(path, "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("null path and empty segments in map keys are rejected")
    void nullPathAndBadMapKeysRejected() {
        assertThatThrownBy(() -> config.set(null, "x")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.set("ai", Map.of("a..b", 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getters resolve dotted paths")
    void gettersResolveDottedPaths() {
        config.set("cloudServices.tokenUrl", "https://a/token")
              .set("autosave.waitingTime", 3000)
              .set("toolbar.shouldNotGroupWhenFull", true);

        assertThat(config.hasConfig("cloudServices")).isTrue();
        assertThat(config.hasConfig("cloudServices.tokenUrl")).isTrue();
        assertThat(config.hasConfig("cloudServices.webSocketUrl")).isFalse();
        assertThat(config.hasConfig("cloudServices.tokenUrl.deeper")).isFalse();
        assertThat(config.getString("cloudServices.tokenUrl")).isEqualTo("https://a/token");
        assertThat(config.getInt("autosave.waitingTime", 0)).isEqualTo(3000);
        assertThat(config.getBoolean("toolbar.shouldNotGroupWhenFull", false)).isTrue();
        assertThat(config.getJsonNode("cloudServices").get("tokenUrl").asString()).isEqualTo("https://a/token");
        assertThat(config.getAutosaveWaitingTime()).isEqualTo(3000);
    }

    @Test
    @DisplayName("stored values are copies: later changes to the passed node do not leak in")
    void storedValuesAreCopies() {
        ObjectNode node = MAPPER.createObjectNode().put("tokenUrl", "a");
        config.set("cloudServices", node);
        node.put("tokenUrl", "EVIL");
        assertThat(config.getString("cloudServices.tokenUrl")).isEqualTo("a");
    }

    @Test
    @DisplayName("dotted config survives session serialization")
    void dottedConfigSurvivesSerialization() throws Exception {
        config.set("cloudServices.tokenUrl", "https://a/token");
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
            out.writeObject(config);
        }
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(
                new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            CKEditorConfig copy = (CKEditorConfig) in.readObject();
            assertThat(copy.getString("cloudServices.tokenUrl")).isEqualTo("https://a/token");
        }
    }

    /** getter 抛异常的 bean，用于验证无法转换为 JSON 的值。 */
    public static class Boom {
        public String getValue() {
            throw new IllegalStateException("boom");
        }
    }
}
