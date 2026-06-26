package com.wontlost.ckeditor.testbench;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * Vaadin TestBench element（page object）for the CKEditor 5 integration component
 * （前端标签 {@code vaadin-ckeditor}）。
 *
 * <p>在 Vaadin TestBench 端到端测试中以类型安全的方式操作编辑器，例如：</p>
 * <pre>{@code
 * VaadinCKEditorElement editor = $(VaadinCKEditorElement.class).first();
 * editor.setData("<p>Hello</p>");
 * assertEquals("<p>Hello</p>", editor.getData());
 * }</pre>
 *
 * <p><b>Premium 说明</b>：Vaadin TestBench 是商业（Premium）功能。本类仅在编译期依赖
 * TestBench API（{@code vaadin-testbench-core}，scope=provided），不在运行时强加 TestBench。
 * 运行测试的使用者须自备 TestBench 依赖与 license。</p>
 *
 * <p><b>方法映射</b>：</p>
 * <ul>
 *   <li>读属性 → {@code getPropertyString/getPropertyBoolean}（编辑器同步到 server 的属性值）</li>
 *   <li>调前端方法 → {@code callFunction}（对应 {@code VaadinCKEditor} 上的公开方法）</li>
 *   <li>取实时内容 → {@code executeScript} 调用 CKEditor 实例的 {@code getData()}</li>
 * </ul>
 */
@Element("vaadin-ckeditor")
public class VaadinCKEditorElement extends TestBenchElement {

    // ==================== 内容读写 ====================

    /**
     * 获取编辑器的实时 HTML 内容。
     *
     * <p>优先调用 CKEditor 实例的 {@code getData()} 取最新内容；编辑器尚未就绪时
     * 回退到已同步的 {@code editorData} 属性。这比抓取渲染 DOM 更稳定。</p>
     *
     * <p>实现依赖前端元素上运行时可达的 {@code editor} 字段。若前端日后将其改为
     * ECMAScript {@code #private} 或重命名，此方法需相应调整（或前端补一个 public getter）。</p>
     *
     * @return 当前 HTML 内容
     */
    public String getData() {
        Object result = executeScript(
            "return arguments[0].editor ? arguments[0].editor.getData() : arguments[0].editorData;",
            this);
        return result != null ? result.toString() : "";
    }

    /**
     * 设置编辑器的 live 内容（调用前端 {@code updateData}，写入 CKEditor 实例）。
     *
     * <p><b>注意语义边界</b>：该方法只改变浏览器端 CKEditor 的 live 内容，<b>不保证</b>同步到
     * server 端的 {@code VaadinCKEditor#getValue()}。前端 {@code updateData} 走 API 变更路径
     * （{@code apiChangeDepth>0}），按设计不会回写 {@code $server.setEditorData()}，以免反向污染
     * Binder。因此：用 {@link #getData()} 断言浏览器端内容是可靠的；若要断言 server 端 value /
     * Binder / valueChange，应通过真实用户输入路径（如点击编辑区后键入），而非本方法。</p>
     *
     * @param html 要设置的 HTML 内容
     */
    public void setData(String html) {
        callFunction("updateData", html);
    }

    /**
     * 在当前/上次记录的光标位置插入文本（调用前端 {@code insertText}）。
     *
     * @param text 要插入的文本
     */
    public void insertText(String text) {
        callFunction("insertText", text);
    }

    // ==================== 状态控制 ====================

    /**
     * 设置只读状态（调用前端 {@code setReadOnly}）。
     *
     * @param readOnly true 设为只读
     */
    public void setReadOnly(boolean readOnly) {
        callFunction("setReadOnly", readOnly);
    }

    /**
     * 是否只读。读取本组件同步的 {@code isReadOnly} 属性，而非通用 readonly attribute，
     * 更贴近 CKEditor integration 的状态。
     *
     * @return true 表示只读
     */
    public boolean isReadOnly() {
        return getPropertyBoolean("isReadOnly");
    }

    /**
     * 是否启用。
     *
     * <p>覆盖 {@link TestBenchElement} 继承自 Selenium {@code WebElement} 的 {@code isEnabled()}：
     * 返回本组件同步的 {@code isEnabled} 属性（与 Java 侧 {@code onEnabledStateChanged} 一致），
     * 而非通用 WebElement 可交互性判断。</p>
     *
     * @return true 表示启用
     */
    @Override
    public boolean isEnabled() {
        return getPropertyBoolean("isEnabled");
    }

    // ==================== 焦点 / 光标 ====================

    /**
     * 聚焦编辑器可编辑区（调用前端 {@code focusEditor}）。
     */
    public void focusEditor() {
        callFunction("focusEditor");
    }

    /**
     * 将光标移到内容开头（调用前端 {@code setCaretToStart}）。
     */
    public void setCaretToStart() {
        callFunction("setCaretToStart");
    }

    /**
     * 将光标移到内容末尾（调用前端 {@code setCaretToEnd}）。
     */
    public void setCaretToEnd() {
        callFunction("setCaretToEnd");
    }

    // ==================== 关键属性读取 ====================

    /**
     * 编辑器类型：classic / balloon / inline / decoupled。
     *
     * @return {@code editorType} 属性值
     */
    public String getEditorType() {
        return getPropertyString("editorType");
    }

    /**
     * 主题类型：auto / light / dark。
     *
     * @return {@code themeType} 属性值
     */
    public String getThemeType() {
        return getPropertyString("themeType");
    }

    /**
     * UI 语言代码。
     *
     * @return {@code language} 属性值
     */
    public String getLanguage() {
        return getPropertyString("language");
    }

    /**
     * 回退模式：textarea / readonly / error / hidden。
     *
     * @return {@code fallbackMode} 属性值
     */
    public String getFallbackMode() {
        return getPropertyString("fallbackMode");
    }
}
