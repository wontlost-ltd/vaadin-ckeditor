package com.wontlost.ckeditor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 版本号一致性测试。
 *
 * <p>本项目把版本号硬编码在多处（pom、Java 常量、前端 package.json、
 * 前端组件 version 字段、premium 包版本、@NpmPackage 注解），此前唯一的约束
 * 只是一句注释，发布流程也只校验「tag 与 pom 一致」这一对关系。
 * 实践中已经发生过漏改：某次升级后 {@code VaadinCKEditor.VERSION} 与
 * {@code vaadin-ckeditor.ts} 仍停留在上一个版本，而 {@code HotDeployConfig}
 * 会把该常量打进消费方的启动日志，导致线上版本自述失真、故障排查被误导。
 *
 * <p>这里用测试把「漂移」变成构建失败，成本远低于每次发布靠人工记忆核对。
 */
@DisplayName("Version Sync Tests")
class VersionSyncTest {

    /** 项目根目录：测试工作目录即模块根。 */
    private static final Path ROOT = Paths.get("").toAbsolutePath();

    private static String read(String relativePath) throws IOException {
        return Files.readString(ROOT.resolve(relativePath), StandardCharsets.UTF_8);
    }

    private static String firstMatch(String content, String regex, String what) {
        Matcher m = Pattern.compile(regex).matcher(content);
        assertTrue(m.find(), "未能在文件中定位" + what + "，正则：" + regex);
        return m.group(1);
    }

    /** pom.xml 中 <artifactId>ckeditor-vaadin</artifactId> 紧随其后的项目版本。 */
    private static String pomVersion() throws IOException {
        return firstMatch(read("pom.xml"),
            "<artifactId>ckeditor-vaadin</artifactId>\\s*\\R\\s*<packaging>jar</packaging>\\s*\\R\\s*<version>([^<]+)</version>",
            "项目版本");
    }

    @Test
    @DisplayName("VaadinCKEditor.getVersion() 必须与 pom.xml 版本一致")
    void javaVersionMatchesPom() throws IOException {
        assertEquals(pomVersion(), VaadinCKEditor.getVersion(),
            "VaadinCKEditor.VERSION 与 pom.xml 版本不一致：该常量会随 HotDeployConfig "
            + "打进消费方启动日志，漂移会导致线上版本自述错误");
    }

    @Test
    @DisplayName("前端 package.json 版本必须与 pom.xml 一致")
    void frontendPackageJsonMatchesPom() throws IOException {
        String pkg = read("src/main/resources/META-INF/frontend/vaadin-ckeditor/package.json");
        assertEquals(pomVersion(), firstMatch(pkg, "\"version\"\\s*:\\s*\"([^\"]+)\"", "package.json 版本"));
    }

    @Test
    @DisplayName("前端组件 version 字段必须与 pom.xml 一致")
    void frontendComponentVersionMatchesPom() throws IOException {
        String ts = read("src/main/resources/META-INF/frontend/vaadin-ckeditor/vaadin-ckeditor.ts");
        assertEquals(pomVersion(),
            firstMatch(ts, "private readonly version\\s*=\\s*'([^']+)'", "组件 version 字段"));
    }

    @Test
    @DisplayName("testbench 模块版本必须与主模块一致")
    void testbenchModuleVersionMatchesPom() throws IOException {
        String tb = read("ckeditor-vaadin-testbench/pom.xml");
        assertEquals(pomVersion(),
            firstMatch(tb,
                "<artifactId>ckeditor-vaadin-testbench</artifactId>\\s*\\R\\s*<packaging>jar</packaging>\\s*\\R\\s*<version>([^<]+)</version>",
                "testbench 模块版本"),
            "两个模块作为一套整体发布，版本必须同步");
    }

    @Test
    @DisplayName("ckeditor5 的 @NpmPackage 版本必须与 package.json 及 premium 版本一致")
    void ckeditorNpmVersionsAreConsistent() throws IOException {
        String pkg = read("src/main/resources/META-INF/frontend/vaadin-ckeditor/package.json");
        String ckeditorVersion = firstMatch(pkg, "\"ckeditor5\"\\s*:\\s*\"([^\"]+)\"", "package.json 中 ckeditor5 版本");

        String editorJava = read("src/main/java/com/wontlost/ckeditor/VaadinCKEditor.java");
        assertEquals(ckeditorVersion,
            firstMatch(editorJava,
                "@NpmPackage\\(value = \"ckeditor5\", version = \"([^\"]+)\"\\)", "@NpmPackage 中 ckeditor5 版本"),
            "@NpmPackage 与 package.json 声明的 ckeditor5 版本必须一致");

        // premium 包必须与免费包同版本：CKEditor 官方要求二者版本严格匹配，
        // getVersion() 是向消费端声明「你需要自行安装哪个版本」的唯一依据。
        String premiumJava = read("src/main/java/com/wontlost/ckeditor/VaadinCKEditorPremium.java");
        assertEquals(ckeditorVersion,
            firstMatch(premiumJava, "@NpmPackage\\(value = \"ckeditor5-premium-features\", version = \"([^\"]+)\"\\)",
                "premium @NpmPackage 版本"),
            "ckeditor5-premium-features 必须与 ckeditor5 同版本");
        assertEquals(ckeditorVersion, VaadinCKEditorPremium.getVersion(),
            "VaadinCKEditorPremium.getVersion() 必须与实际 npm 版本一致，"
            + "否则消费端据此判断 premium 能力时会做出错误决策");
    }

    @Test
    @DisplayName("highlight.js 的 @NpmPackage 版本必须与 package.json 一致")
    void highlightJsNpmVersionMatchesPackageJson() throws IOException {
        String pkg = read("src/main/resources/META-INF/frontend/vaadin-ckeditor/package.json");
        String editorJava = read("src/main/java/com/wontlost/ckeditor/VaadinCKEditor.java");
        assertEquals(
            firstMatch(pkg, "\"highlight\\.js\"\\s*:\\s*\"([^\"]+)\"", "package.json 中 highlight.js 版本"),
            firstMatch(editorJava, "@NpmPackage\\(value = \"highlight\\.js\", version = \"([^\"]+)\"\\)",
                "@NpmPackage 中 highlight.js 版本"),
            "连接器测试与消费端安装的 highlight.js 必须是同一版本");
    }
}
