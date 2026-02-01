package com.wontlost.ckeditor;

import com.vaadin.flow.component.HasElement;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public interface HasConfig extends HasElement {

    default void setConfig(Config config) {
        getElement().setPropertyJson("config", config != null ? config.getConfigJson() : new Config().getConfigJson());
    }

    default Config getConfig() {
        String configJson = getElement().getProperty("config");

        JsonMapper mapper = JsonMapper.builder().build();
        ObjectNode jsonObject;

        try {
            jsonObject = (ObjectNode) mapper.readTree(configJson);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid config JSON", e);
        }

        return new Config(jsonObject);
    }
}
