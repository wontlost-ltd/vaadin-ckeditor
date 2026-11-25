package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            CKEditorPlugin.IMAGE_RESIZE, CKEditorPlugin.PASTE_FROM_OFFICE, CKEditorPlugin.CKFINDER, CKEditorPlugin.FULL_PAGE);
        assertThat(CKEditorPlugin.getPremiumFeaturePlugins()).hasSize(21);
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

    // W4：README 曾写 "70+ Free Plugins"。按维护者的裁定陈述两点事实：都能在 GPL key 下运行，但 CKEditor 把其中若干个标为 premium
    @Test
    @DisplayName("README states the number of plugins CKEditor marks as premium (licence feature code) and the GPL fact")
    void readmeStatesPremiumCount() throws IOException {
        String readme = Files.readString(Path.of(System.getProperty("basedir", "."), "README.md"));
        Matcher m = Pattern.compile("CKEditor marks (\\d+) of them as premium features").matcher(readme);
        assertThat(m.find()).as("README feature line").isTrue();
        // isPremiumFeature 的集合 = 带 licenceFeatureCode 的插件（premium-catalogue.test.ts 对照真实包）+ 三个付费服务插件
        Set<CKEditorPlugin> services = EnumSet.of(CKEditorPlugin.CKBOX, CKEditorPlugin.CKBOX_IMAGE_EDIT, CKEditorPlugin.CKFINDER);
        assertThat(Integer.parseInt(m.group(1))).isEqualTo(CKEditorPlugin.getPremiumFeaturePlugins().size() - services.size());
        assertThat(readme).contains("run under the default `GPL` licence key").doesNotContain("Free Plugins");
    }
}
