package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;

/**
 * CKEditor 文档标为 premium feature、由开源 ckeditor5 包导出的插件（{@link CKEditorPlugin#isPremiumFeature()}）：
 * GPL key 下 builder 记录一次信息日志（不是警告），配置商业 key 时不记录。
 */
class PremiumFeaturePluginTest {

    @Test
    @DisplayName("isPremiumFeature 与 isPremium（加载通道）相互独立")
    void flags() {
        assertThat(CKEditorPlugin.getPremiumFeaturePlugins()).contains(
            CKEditorPlugin.CKBOX, CKEditorPlugin.CKBOX_IMAGE_EDIT, CKEditorPlugin.TABLE_LAYOUT, CKEditorPlugin.WORD_COUNT,
            CKEditorPlugin.IMAGE_RESIZE, CKEditorPlugin.PASTE_FROM_OFFICE, CKEditorPlugin.CKFINDER);
        assertThat(CKEditorPlugin.getPremiumFeaturePlugins()).hasSize(20);
        assertThat(CKEditorPlugin.BOLD.isPremiumFeature()).isFalse();
        for (CKEditorPlugin plugin : CKEditorPlugin.values()) {
            assertThat(plugin.isPremium()).isFalse();
        }
    }

    @Test
    @DisplayName("GPL / 空 key 时列出 premium feature 插件；商业 key 时为空")
    void detection() {
        Set<CKEditorPlugin> plugins = Set.of(CKEditorPlugin.CKBOX, CKEditorPlugin.TABLE_LAYOUT, CKEditorPlugin.BOLD);
        assertThat(VaadinCKEditorBuilder.premiumFeaturesUnderGpl(plugins, "GPL"))
            .containsExactlyInAnyOrder(CKEditorPlugin.CKBOX, CKEditorPlugin.TABLE_LAYOUT);
        assertThat(VaadinCKEditorBuilder.premiumFeaturesUnderGpl(plugins, null)).hasSize(2);
        assertThat(VaadinCKEditorBuilder.premiumFeaturesUnderGpl(plugins, " ")).hasSize(2);
        assertThat(VaadinCKEditorBuilder.premiumFeaturesUnderGpl(plugins, "eyJhbGciOi...")).isEmpty();
        assertThat(VaadinCKEditorBuilder.premiumFeaturesUnderGpl(Set.of(CKEditorPlugin.BOLD), "GPL")).isEmpty();
    }

    @Test
    @DisplayName("build() 记录 INFO（含文档链接），同一组合只记录一次；商业 key 不记录")
    void builderLogsInfoOnce() {
        Logger logger = Logger.getLogger(VaadinCKEditorBuilder.class.getName());
        StringBuilder messages = new StringBuilder();
        Level[] levels = new Level[1];
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                messages.append(record.getMessage()).append('\n');
                levels[0] = record.getLevel();
            }
            @Override public void flush() { }
            @Override public void close() { }
        };
        logger.addHandler(handler);
        try {
            VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).addPlugin(CKEditorPlugin.CKBOX_IMAGE_EDIT).build();
            assertThat(messages.toString()).contains("CKBOX_IMAGE_EDIT").contains("license-key-and-activation");
            assertThat(levels[0]).isEqualTo(Level.INFO);

            messages.setLength(0);
            VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).addPlugin(CKEditorPlugin.CKBOX_IMAGE_EDIT).build();
            assertThat(messages.toString()).doesNotContain("CKBOX_IMAGE_EDIT");

            VaadinCKEditorBuilder.logPremiumFeaturesOnce(EnumSet.noneOf(CKEditorPlugin.class));
            VaadinCKEditor.create().withPreset(CKEditorPreset.BASIC).addPlugin(CKEditorPlugin.CKBOX)
                .withLicenseKey("a-commercial-key").build();
            assertThat(messages.toString()).isEmpty();
        } finally {
            logger.removeHandler(handler);
        }
    }
}
