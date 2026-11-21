package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * 由开源 ckeditor5 包导出、但属 premium feature 的插件（CKBox、CKBoxImageEdit、TableLayout）：
 * 未配置 license key 时 builder 记录警告。
 */
class LicenseRequiredPluginTest {

    @Test
    @DisplayName("未配置 license key（空 / GPL）时列出需要 license 的插件；配置后为空")
    void detection() {
        Set<CKEditorPlugin> plugins = Set.of(CKEditorPlugin.CKBOX, CKEditorPlugin.TABLE_LAYOUT, CKEditorPlugin.BOLD);
        assertThat(VaadinCKEditorBuilder.licenseRequiredWithoutKey(plugins, "GPL"))
            .containsExactlyInAnyOrder(CKEditorPlugin.CKBOX, CKEditorPlugin.TABLE_LAYOUT);
        assertThat(VaadinCKEditorBuilder.licenseRequiredWithoutKey(plugins, null)).hasSize(2);
        assertThat(VaadinCKEditorBuilder.licenseRequiredWithoutKey(plugins, " ")).hasSize(2);
        assertThat(VaadinCKEditorBuilder.licenseRequiredWithoutKey(plugins, "eyJhbGciOi...")).isEmpty();
        assertThat(VaadinCKEditorBuilder.licenseRequiredWithoutKey(Set.of(CKEditorPlugin.BOLD), "GPL")).isEmpty();
    }

    @Test
    @DisplayName("build() 在 GPL key + CKBox 时记录警告，配置 license key 后不警告")
    void builderWarns() {
        Logger logger = Logger.getLogger(VaadinCKEditorBuilder.class.getName());
        StringBuilder messages = new StringBuilder();
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) { messages.append(record.getMessage()).append('\n'); }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).addPlugin(CKEditorPlugin.CKBOX).build();
            assertThat(messages.toString()).contains("CKBOX").contains("withLicenseKey");

            messages.setLength(0);
            VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).addPlugin(CKEditorPlugin.CKBOX)
                .withLicenseKey("a-commercial-key").build();
            assertThat(messages.toString()).doesNotContain("CKBOX");
        } finally {
            logger.removeHandler(handler);
        }
    }
}
