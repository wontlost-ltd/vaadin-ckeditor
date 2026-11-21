package com.wontlost.ckeditor.collaboration;

import com.wontlost.ckeditor.JsonUtil;
import tools.jackson.databind.JsonNode;

/** JSON helpers of this package. */
final class Json {

    private Json() {
    }

    /** A defensive copy of an object node, or an empty object for null / non-objects. */
    static JsonNode objectOrEmpty(JsonNode node) {
        return node != null && node.isObject() ? node.deepCopy() : JsonUtil.createObjectNode();
    }
}
