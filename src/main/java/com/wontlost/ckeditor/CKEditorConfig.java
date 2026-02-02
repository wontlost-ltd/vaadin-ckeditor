package com.wontlost.ckeditor;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

import static com.wontlost.ckeditor.JsonUtil.*;

/**
 * CKEditor configuration class.
 * Used to build JSON configuration passed to CKEditor.
 */
public class CKEditorConfig {

    private final Map<String, JsonNode> configs = new LinkedHashMap<>();

    /**
     * 是否允许私有/内网地址作为上传 URL（用于开发环境）
     */
    private boolean allowPrivateNetworks = false;

    public CKEditorConfig() {
        // Initialize defaults
        setPlaceholder("Type your content here...");
        setLanguage("en");
    }

    /**
     * Set placeholder text
     */
    public CKEditorConfig setPlaceholder(String placeholder) {
        configs.put("placeholder", getMapper().valueToTree(placeholder != null ? placeholder : ""));
        return this;
    }

    /**
     * Set UI language
     */
    public CKEditorConfig setLanguage(String language) {
        configs.put("language", getMapper().valueToTree(language != null ? language : "en"));
        return this;
    }

    /**
     * Set complex language configuration (UI language, content language, text part languages)
     */
    public CKEditorConfig setLanguage(String uiLanguage, String contentLanguage, String[] textPartLanguages) {
        ObjectNode langObj = createObjectNode();
        langObj.put("ui", uiLanguage != null ? uiLanguage : "en");
        if (contentLanguage != null) {
            langObj.put("content", contentLanguage);
        }
        ArrayNode textPartArr = toArrayNodeOrNull(textPartLanguages);
        if (textPartArr != null) {
            langObj.set("textPartLanguage", textPartArr);
        }
        configs.put("language", langObj);
        return this;
    }

    /**
     * Set toolbar items
     */
    public CKEditorConfig setToolbar(String... items) {
        if (hasElements(items)) {
            configs.put("toolbar", toArrayNode(items));
        }
        return this;
    }

    /**
     * Set toolbar with grouping option
     */
    public CKEditorConfig setToolbar(String[] items, boolean shouldNotGroupWhenFull) {
        if (hasElements(items)) {
            ObjectNode toolbarObj = createObjectNode();
            toolbarObj.set("items", toArrayNode(items));
            toolbarObj.put("shouldNotGroupWhenFull", shouldNotGroupWhenFull);
            configs.put("toolbar", toolbarObj);
        }
        return this;
    }

    /**
     * Set heading options
     */
    public CKEditorConfig setHeading(HeadingOption... options) {
        if (hasElements(options)) {
            ObjectNode headingObj = createObjectNode();
            ArrayNode optionsArr = createArrayNode();
            for (HeadingOption option : options) {
                optionsArr.add(option.toJson());
            }
            headingObj.set("options", optionsArr);
            configs.put("heading", headingObj);
        }
        return this;
    }

    /**
     * Set font size options
     */
    public CKEditorConfig setFontSize(String... sizes) {
        return setFontSize(false, sizes);
    }

    /**
     * Set font size options with support all values flag
     */
    public CKEditorConfig setFontSize(boolean supportAllValues, String... sizes) {
        ObjectNode fontSizeObj = createObjectNode();
        fontSizeObj.put("supportAllValues", supportAllValues);
        ArrayNode arr = toArrayNodeOrNull(sizes);
        if (arr != null) {
            fontSizeObj.set("options", arr);
        }
        configs.put("fontSize", fontSizeObj);
        return this;
    }

    /**
     * Set font family options
     */
    public CKEditorConfig setFontFamily(String... families) {
        return setFontFamily(false, families);
    }

    /**
     * Set font family options with support all values flag
     */
    public CKEditorConfig setFontFamily(boolean supportAllValues, String... families) {
        ObjectNode fontFamilyObj = createObjectNode();
        fontFamilyObj.put("supportAllValues", supportAllValues);
        ArrayNode arr = toArrayNodeOrNull(families);
        if (arr != null) {
            fontFamilyObj.set("options", arr);
        }
        configs.put("fontFamily", fontFamilyObj);
        return this;
    }

    /**
     * Set alignment options
     */
    public CKEditorConfig setAlignment(String... options) {
        if (hasElements(options)) {
            ObjectNode alignObj = createObjectNode();
            alignObj.set("options", toArrayNode(options));
            configs.put("alignment", alignObj);
        }
        return this;
    }

    /**
     * Set link configuration
     */
    public CKEditorConfig setLink(String defaultProtocol, boolean addTargetToExternalLinks) {
        ObjectNode linkObj = createObjectNode();
        if (defaultProtocol != null) {
            linkObj.put("defaultProtocol", defaultProtocol);
        }
        linkObj.put("addTargetToExternalLinks", addTargetToExternalLinks);
        configs.put("link", linkObj);
        return this;
    }

    /**
     * Set image configuration
     */
    public CKEditorConfig setImage(String[] toolbar, String[] styles) {
        ObjectNode imageObj = createObjectNode();
        ArrayNode toolbarArr = toArrayNodeOrNull(toolbar);
        if (toolbarArr != null) {
            imageObj.set("toolbar", toolbarArr);
        }
        ArrayNode stylesArr = toArrayNodeOrNull(styles);
        if (stylesArr != null) {
            imageObj.set("styles", stylesArr);
        }
        configs.put("image", imageObj);
        return this;
    }

    /**
     * Set table configuration
     */
    public CKEditorConfig setTable(String[] contentToolbar) {
        if (hasElements(contentToolbar)) {
            ObjectNode tableObj = createObjectNode();
            tableObj.set("contentToolbar", toArrayNode(contentToolbar));
            configs.put("table", tableObj);
        }
        return this;
    }

    /**
     * Set code block languages
     */
    public CKEditorConfig setCodeBlock(String indentSequence, CodeBlockLanguage... languages) {
        ObjectNode codeBlockObj = createObjectNode();
        if (indentSequence != null) {
            codeBlockObj.put("indentSequence", indentSequence);
        }
        if (hasElements(languages)) {
            ArrayNode arr = createArrayNode();
            for (CodeBlockLanguage lang : languages) {
                arr.add(lang.toJson());
            }
            codeBlockObj.set("languages", arr);
        }
        configs.put("codeBlock", codeBlockObj);
        return this;
    }

    /**
     * Set media embed configuration
     */
    public CKEditorConfig setMediaEmbed(boolean previewsInData) {
        ObjectNode mediaObj = createObjectNode();
        mediaObj.put("previewsInData", previewsInData);
        configs.put("mediaEmbed", mediaObj);
        return this;
    }

    /**
     * Set mention feeds
     */
    public CKEditorConfig setMention(MentionFeed... feeds) {
        if (hasElements(feeds)) {
            ObjectNode mentionObj = createObjectNode();
            ArrayNode feedsArr = createArrayNode();
            for (MentionFeed feed : feeds) {
                feedsArr.add(feed.toJson());
            }
            mentionObj.set("feeds", feedsArr);
            configs.put("mention", mentionObj);
        }
        return this;
    }

    /**
     * Set simple upload configuration for the SimpleUploadAdapter plugin.
     *
     * <p>SimpleUploadAdapter uses XMLHttpRequest to send files to your server.
     * This is a lightweight solution when you have a simple upload endpoint.</p>
     *
     * <h3>Basic Usage</h3>
     * <pre>
     * VaadinCKEditor editor = VaadinCKEditor.create()
     *     .withPreset(CKEditorPreset.STANDARD)
     *     .removePlugin(CKEditorPlugin.BASE64_UPLOAD_ADAPTER)
     *     .addPlugin(CKEditorPlugin.SIMPLE_UPLOAD_ADAPTER)
     *     .withConfig(config -&gt; config
     *         .setSimpleUpload("https://your-server.com/api/upload")
     *     )
     *     .build();
     * </pre>
     *
     * <h3>With Authentication</h3>
     * <pre>
     * .withConfig(config -&gt; config
     *     .setSimpleUpload(
     *         "https://your-server.com/api/upload",
     *         Map.of(
     *             "Authorization", "Bearer " + jwtToken,
     *             "X-CSRF-TOKEN", csrfToken
     *         )
     *     )
     * )
     * </pre>
     *
     * <h3>Server Response Format</h3>
     * <p>Your server must return JSON in one of these formats:</p>
     *
     * <p><b>Success (single URL):</b></p>
     * <pre>
     * { "url": "https://example.com/images/foo.jpg" }
     * </pre>
     *
     * <p><b>Success (responsive images):</b></p>
     * <pre>
     * {
     *   "urls": {
     *     "default": "https://example.com/images/foo.jpg",
     *     "800": "https://example.com/images/foo-800.jpg",
     *     "1200": "https://example.com/images/foo-1200.jpg"
     *   }
     * }
     * </pre>
     *
     * <p><b>Error:</b></p>
     * <pre>
     * { "error": { "message": "File too large (max 5MB)" } }
     * </pre>
     *
     * @param uploadUrl the URL to upload files to (required)
     * @return this config for chaining
     * @see #setSimpleUpload(String, Map)
     * @see #setSimpleUpload(String, Map, boolean)
     */
    public CKEditorConfig setSimpleUpload(String uploadUrl) {
        return setSimpleUpload(uploadUrl, null, false);
    }

    /**
     * Set simple upload configuration with custom headers.
     *
     * @param uploadUrl the URL to upload files to (required)
     * @param headers custom HTTP headers to send with the upload request (e.g., Authorization)
     * @return this config for chaining
     * @see #setSimpleUpload(String, Map, boolean)
     */
    public CKEditorConfig setSimpleUpload(String uploadUrl, Map<String, String> headers) {
        return setSimpleUpload(uploadUrl, headers, false);
    }

    /**
     * Set simple upload configuration with all options.
     *
     * <h3>Cross-Origin Requests with Credentials</h3>
     * <p>Set {@code withCredentials} to {@code true} when:</p>
     * <ul>
     *   <li>Your upload endpoint is on a different domain (CORS)</li>
     *   <li>You need to send cookies or HTTP authentication</li>
     *   <li>Your server uses session-based authentication</li>
     * </ul>
     *
     * <p><b>Note:</b> When using {@code withCredentials}, your server must:</p>
     * <ul>
     *   <li>Set {@code Access-Control-Allow-Credentials: true}</li>
     *   <li>Set {@code Access-Control-Allow-Origin} to a specific origin (not {@code *})</li>
     * </ul>
     *
     * <h3>Example with Credentials</h3>
     * <pre>
     * .withConfig(config -&gt; config
     *     .setSimpleUpload(
     *         "https://api.example.com/upload",
     *         Map.of("X-CSRF-TOKEN", csrfToken),
     *         true  // Enable credentials for cross-origin requests
     *     )
     * )
     * </pre>
     *
     * <h3>Spring Boot Server Example</h3>
     * <pre>
     * &#64;PostMapping("/api/upload")
     * &#64;CrossOrigin(origins = "https://your-app.com", allowCredentials = "true")
     * public Map&lt;String, Object&gt; upload(&#64;RequestParam("upload") MultipartFile file) {
     *     String url = storageService.store(file);
     *     return Map.of("url", url);
     * }
     * </pre>
     *
     * @param uploadUrl the URL to upload files to (required)
     * @param headers custom HTTP headers to send with the upload request
     * @param withCredentials whether to send cookies and credentials with cross-origin requests
     * @return this config for chaining
     */
    public CKEditorConfig setSimpleUpload(String uploadUrl, Map<String, String> headers, boolean withCredentials) {
        if (uploadUrl == null || uploadUrl.isBlank()) {
            throw new IllegalArgumentException("uploadUrl must not be null or blank");
        }

        // URL 格式验证，防止 SSRF 攻击
        validateUploadUrl(uploadUrl);

        ObjectNode uploadObj = createObjectNode();
        uploadObj.put("uploadUrl", uploadUrl);

        if (headers != null && !headers.isEmpty()) {
            ObjectNode headersObj = createObjectNode();
            headers.forEach(headersObj::put);
            uploadObj.set("headers", headersObj);
        }

        if (withCredentials) {
            uploadObj.put("withCredentials", true);
        }

        configs.put("simpleUpload", uploadObj);
        return this;
    }

    /**
     * 允许的上传 URL 协议白名单
     */
    private static final Set<String> ALLOWED_URL_PROTOCOLS = Set.of("http", "https");

    /**
     * 允许私有/内网地址作为上传 URL。
     *
     * <p>默认情况下，出于安全考虑，禁止使用 localhost、127.0.0.1、192.168.x.x、
     * 10.x.x.x、172.16-31.x.x 等内网地址。在开发环境中，可以启用此选项：</p>
     *
     * <pre>{@code
     * config.allowPrivateNetworks(true)
     *       .setSimpleUpload("/api/upload");
     * }</pre>
     *
     * <p><b>警告：</b>在生产环境中启用此选项可能导致 SSRF 攻击风险。</p>
     *
     * @param allow true 允许内网地址，false 禁止（默认）
     * @return this
     */
    public CKEditorConfig allowPrivateNetworks(boolean allow) {
        this.allowPrivateNetworks = allow;
        return this;
    }

    /**
     * 检查是否允许私有网络地址
     *
     * @return 是否允许私有网络
     */
    public boolean isAllowPrivateNetworks() {
        return allowPrivateNetworks;
    }

    /**
     * 验证上传 URL 格式和协议安全性。
     * 防止 SSRF 攻击，仅允许 http/https 协议。
     *
     * @param uploadUrl 待验证的 URL
     * @throws IllegalArgumentException 如果 URL 格式无效或协议不在白名单中
     */
    private void validateUploadUrl(String uploadUrl) {
        try {
            URI uri = new URI(uploadUrl);
            String scheme = uri.getScheme();

            if (scheme == null) {
                throw new IllegalArgumentException("Upload URL must have a protocol scheme");
            }

            String protocol = scheme.toLowerCase(Locale.ROOT);
            if (!ALLOWED_URL_PROTOCOLS.contains(protocol)) {
                throw new IllegalArgumentException(
                    "Upload URL protocol must be one of " + ALLOWED_URL_PROTOCOLS +
                    ", got: " + protocol);
            }

            // 禁止内网地址（可选的额外安全检查，可通过 allowPrivateNetworks() 禁用）
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Upload URL must have a valid host");
            }

            if (!allowPrivateNetworks && isPrivateNetworkAddress(host)) {
                throw new IllegalArgumentException(
                    "Upload URL must not point to internal/private network addresses: " + host +
                    ". Use allowPrivateNetworks(true) for development environments.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid upload URL format: " + uploadUrl, e);
        }
    }

    /**
     * 检查是否为私有/内网地址，包括 IPv4 和 IPv6 的各种表示形式。
     * 防止 SSRF 绕过攻击。
     *
     * @param host 主机名或 IP 地址
     * @return 是否为私有地址
     */
    private boolean isPrivateNetworkAddress(String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);

        // IPv4 localhost 和私有地址
        if (lowerHost.equals("localhost") || lowerHost.equals("127.0.0.1") ||
            lowerHost.startsWith("192.168.") || lowerHost.startsWith("10.") ||
            isPrivateClassBAddress(lowerHost) ||
            lowerHost.endsWith(".local") || lowerHost.endsWith(".internal")) {
            return true;
        }

        // 0.0.0.0 - 通配地址
        if (lowerHost.equals("0.0.0.0")) {
            return true;
        }

        // 169.254.x.x - IPv4 链路本地地址
        if (lowerHost.startsWith("169.254.")) {
            return true;
        }

        // IPv6 localhost: ::1 或 [::1]
        if (lowerHost.equals("::1") || lowerHost.equals("[::1]")) {
            return true;
        }

        // IPv4-mapped IPv6 地址: ::ffff:127.0.0.1 或 [::ffff:127.0.0.1]
        // 也包括其他私有 IPv4 地址的 IPv6 映射
        if (isIPv4MappedIPv6PrivateAddress(lowerHost)) {
            return true;
        }

        // IPv4 兼容 IPv6 地址: ::127.0.0.1 或 [::127.0.0.1]
        // 注意：这与 ::ffff: 格式不同，是已弃用但仍需防护的格式
        if (isIPv4CompatibleIPv6PrivateAddress(lowerHost)) {
            return true;
        }

        // SIIT (Stateless IP/ICMP Translation) 格式: ::ffff:0:x.x.x.x
        if (isSIITIPv6PrivateAddress(lowerHost)) {
            return true;
        }

        // IPv6 链路本地地址: fe80::
        if (lowerHost.startsWith("fe80:") || lowerHost.startsWith("[fe80:")) {
            return true;
        }

        // IPv6 唯一本地地址 (ULA): fc00::/7 (fc00:: 到 fdff::)
        if (lowerHost.startsWith("fc") || lowerHost.startsWith("fd") ||
            lowerHost.startsWith("[fc") || lowerHost.startsWith("[fd")) {
            return true;
        }

        // 八进制/十六进制 IP 绕过检查 (如 0177.0.0.1 = 127.0.0.1)
        if (isObfuscatedPrivateIPv4(lowerHost)) {
            return true;
        }

        return false;
    }

    /**
     * 检查是否为 IPv4-mapped IPv6 私有地址。
     * 格式: ::ffff:x.x.x.x 或 [::ffff:x.x.x.x]
     */
    private boolean isIPv4MappedIPv6PrivateAddress(String host) {
        String cleanHost = host.replace("[", "").replace("]", "");
        if (!cleanHost.startsWith("::ffff:")) {
            return false;
        }

        String ipv4Part = cleanHost.substring(7); // 去掉 "::ffff:"
        return isPrivateIPv4String(ipv4Part);
    }

    /**
     * 检查是否为 IPv4 兼容 IPv6 私有地址。
     * 格式: ::x.x.x.x 或 [::x.x.x.x]（已弃用但仍需防护）
     * 注意：这与 ::ffff: 映射格式不同
     */
    private boolean isIPv4CompatibleIPv6PrivateAddress(String host) {
        String cleanHost = host.replace("[", "").replace("]", "");
        // 必须以 :: 开头，但不能是 ::ffff: 或 ::ffff:0: 格式
        if (!cleanHost.startsWith("::") || cleanHost.startsWith("::ffff:")) {
            return false;
        }

        // 提取 :: 后的部分
        String remainder = cleanHost.substring(2);
        // 检查是否为 IPv4 地址格式（包含点号）
        if (!remainder.contains(".")) {
            return false;
        }

        return isPrivateIPv4String(remainder);
    }

    /**
     * 检查是否为 SIIT (Stateless IP/ICMP Translation) 格式的私有 IPv6 地址。
     * 格式: ::ffff:0:x.x.x.x 或 [::ffff:0:x.x.x.x]
     * RFC 6052 定义的 IPv4 嵌入式 IPv6 地址
     */
    private boolean isSIITIPv6PrivateAddress(String host) {
        String cleanHost = host.replace("[", "").replace("]", "");
        if (!cleanHost.startsWith("::ffff:0:")) {
            return false;
        }

        String ipv4Part = cleanHost.substring(9); // 去掉 "::ffff:0:"
        return isPrivateIPv4String(ipv4Part);
    }

    /**
     * 检查 IPv4 字符串是否为私有地址。
     * 提取公共逻辑，避免重复。
     */
    private boolean isPrivateIPv4String(String ipv4) {
        return ipv4.equals("127.0.0.1") ||
               ipv4.startsWith("192.168.") ||
               ipv4.startsWith("10.") ||
               isPrivateClassBAddress(ipv4) ||
               ipv4.equals("0.0.0.0") ||
               ipv4.startsWith("169.254.");
    }

    /**
     * 八进制/十六进制 IP 绕过检测正则。
     * 匹配以 0 开头的八进制表示（如 0177.0.0.1）或以 0x 开头的十六进制表示。
     */
    private static final Pattern OBFUSCATED_IP_PATTERN =
        Pattern.compile("^(0[0-7]+|0x[0-9a-f]+)(\\.(0[0-7]+|0x[0-9a-f]+|\\d+)){0,3}$", Pattern.CASE_INSENSITIVE);

    /**
     * 检查是否为混淆的私有 IPv4 地址（八进制/十六进制表示）。
     * 例如：0177.0.0.1 (= 127.0.0.1), 0x7f.0.0.1 (= 127.0.0.1)
     */
    private boolean isObfuscatedPrivateIPv4(String host) {
        if (!OBFUSCATED_IP_PATTERN.matcher(host).matches()) {
            return false;
        }

        // 尝试解析为标准 IPv4 地址
        try {
            String[] parts = host.split("\\.");
            int[] octets = new int[4];
            int partIndex = 0;

            for (String part : parts) {
                if (partIndex >= 4) break;
                int value;
                if (part.startsWith("0x") || part.startsWith("0X")) {
                    value = Integer.parseInt(part.substring(2), 16);
                } else if (part.startsWith("0") && part.length() > 1) {
                    value = Integer.parseInt(part, 8);
                } else {
                    value = Integer.parseInt(part);
                }
                octets[partIndex++] = value;
            }

            // 填充剩余部分为 0
            while (partIndex < 4) {
                octets[partIndex++] = 0;
            }

            // 检查是否为私有地址
            return isPrivateIPv4Octets(octets);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查 IPv4 八位字节数组是否为私有地址。
     */
    private boolean isPrivateIPv4Octets(int[] octets) {
        // 127.x.x.x (loopback)
        if (octets[0] == 127) return true;
        // 10.x.x.x (Class A private)
        if (octets[0] == 10) return true;
        // 192.168.x.x (Class C private)
        if (octets[0] == 192 && octets[1] == 168) return true;
        // 172.16-31.x.x (Class B private)
        if (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31) return true;
        // 0.0.0.0 (wildcard)
        if (octets[0] == 0 && octets[1] == 0 && octets[2] == 0 && octets[3] == 0) return true;
        // 169.254.x.x (link-local)
        if (octets[0] == 169 && octets[1] == 254) return true;
        return false;
    }

    /**
     * 私有 B 类地址正则（172.16.0.0 - 172.31.255.255）
     * 匹配 172.16.x.x 到 172.31.x.x
     */
    private static final Pattern PRIVATE_CLASS_B_PATTERN =
        Pattern.compile("^172\\.(1[6-9]|2[0-9]|3[01])\\.");

    /**
     * 检查是否为 172.16.0.0 - 172.31.255.255 范围的私有地址
     */
    private boolean isPrivateClassBAddress(String host) {
        return PRIVATE_CLASS_B_PATTERN.matcher(host).find();
    }

    /**
     * Autosave 最小等待时间（毫秒）
     */
    private static final int MIN_AUTOSAVE_WAITING_TIME = 100;

    /**
     * Autosave 最大等待时间（毫秒）
     */
    private static final int MAX_AUTOSAVE_WAITING_TIME = 60000;

    /**
     * Set autosave configuration
     *
     * @param waitingTime 等待时间（毫秒），范围 100-60000
     * @throws IllegalArgumentException 如果等待时间超出有效范围
     */
    public CKEditorConfig setAutosave(int waitingTime) {
        if (waitingTime < MIN_AUTOSAVE_WAITING_TIME || waitingTime > MAX_AUTOSAVE_WAITING_TIME) {
            throw new IllegalArgumentException(
                "Autosave waiting time must be between " + MIN_AUTOSAVE_WAITING_TIME +
                " and " + MAX_AUTOSAVE_WAITING_TIME + " milliseconds, got: " + waitingTime);
        }
        ObjectNode autosaveObj = createObjectNode();
        autosaveObj.put("waitingTime", waitingTime);
        configs.put("autosave", autosaveObj);
        return this;
    }

    /**
     * Set license key (for premium features)
     */
    public CKEditorConfig setLicenseKey(String licenseKey) {
        if (licenseKey != null && !licenseKey.isBlank()) {
            configs.put("licenseKey", getMapper().valueToTree(licenseKey));
        }
        return this;
    }

    /**
     * Set style definitions for the Style plugin.
     * The Style plugin allows applying CSS classes to elements via a dropdown.
     * Requires: Style plugin and GeneralHtmlSupport plugin.
     *
     * @param definitions style definitions (block and inline styles)
     * @return this config for chaining
     */
    public CKEditorConfig setStyle(StyleDefinition... definitions) {
        if (hasElements(definitions)) {
            ObjectNode styleObj = createObjectNode();
            ArrayNode definitionsArr = createArrayNode();
            for (StyleDefinition def : definitions) {
                definitionsArr.add(def.toJson());
            }
            styleObj.set("definitions", definitionsArr);
            configs.put("style", styleObj);
        }
        return this;
    }

    /**
     * Set general HTML support configuration
     */
    public CKEditorConfig setHtmlSupport(boolean allowAll) {
        ObjectNode htmlSupportObj = createObjectNode();
        if (allowAll) {
            ArrayNode allowArr = createArrayNode();
            ObjectNode allowAllObj = createObjectNode();
            allowAllObj.put("name", "/.*/");
            allowAllObj.put("attributes", true);
            allowAllObj.put("classes", true);
            allowAllObj.put("styles", true);
            allowArr.add(allowAllObj);
            htmlSupportObj.set("allow", allowArr);
        }
        configs.put("htmlSupport", htmlSupportObj);
        return this;
    }

    /**
     * Set toolbar style configuration.
     * This enables custom styling of the CKEditor toolbar via CSS injection.
     *
     * <p>Usage example:</p>
     * <pre>
     * config.setToolbarStyle(ToolbarStyle.builder()
     *     .background("#f5f5f5")
     *     .borderColor("#ddd")
     *     .borderRadius("8px")
     *     .buttonHoverBackground("rgba(0, 0, 0, 0.1)")
     *     .build());
     * </pre>
     *
     * @param style the toolbar style configuration
     * @return this config for chaining
     */
    public CKEditorConfig setToolbarStyle(ToolbarStyle style) {
        if (style != null) {
            configs.put("toolbarStyle", style.toJson());
        }
        return this;
    }

    /**
     * Set UI viewport offset
     */
    public CKEditorConfig setUiViewportOffset(double top, double right, double bottom, double left) {
        ObjectNode uiObj = createObjectNode();
        ObjectNode viewportOffset = createObjectNode();
        viewportOffset.put("top", top);
        viewportOffset.put("right", right);
        viewportOffset.put("bottom", bottom);
        viewportOffset.put("left", left);
        uiObj.set("viewportOffset", viewportOffset);
        configs.put("ui", uiObj);
        return this;
    }

    /**
     * Set custom configuration value
     */
    public CKEditorConfig set(String key, JsonNode value) {
        configs.put(key, value);
        return this;
    }

    /**
     * Get configuration map
     */
    public Map<String, JsonNode> getConfigs() {
        return Collections.unmodifiableMap(configs);
    }

    /**
     * Convert to JSON object
     */
    public ObjectNode toJson() {
        ObjectNode json = createObjectNode();
        configs.forEach(json::set);
        return json;
    }

    // ==================== Type-safe Getters ====================

    /**
     * Get the placeholder text.
     *
     * @return the placeholder text, or null if not set
     */
    public String getPlaceholder() {
        JsonNode node = configs.get("placeholder");
        return node != null && node.isString() ? node.asString() : null;
    }

    /**
     * Get the UI language.
     *
     * @return the language code, or null if not set
     */
    public String getLanguage() {
        JsonNode node = configs.get("language");
        if (node == null) {
            return null;
        }
        if (node.isString()) {
            return node.asString();
        }
        if (node.isObject() && node.has("ui")) {
            return node.get("ui").asString();
        }
        return null;
    }

    /**
     * Get the toolbar configuration.
     *
     * @return array of toolbar items, or null if not set
     */
    public String[] getToolbar() {
        JsonNode node = configs.get("toolbar");
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            return jsonArrayToStringArray(node);
        }
        if (node.isObject() && node.has("items")) {
            return jsonArrayToStringArray(node.get("items"));
        }
        return null;
    }

    /**
     * Check if a configuration key is set.
     *
     * @param key the configuration key
     * @return true if the key exists
     */
    public boolean hasConfig(String key) {
        return configs.containsKey(key);
    }

    /**
     * Get a string configuration value.
     *
     * @param key the configuration key
     * @return the string value, or null if not set or not a string
     */
    public String getString(String key) {
        JsonNode node = configs.get(key);
        return node != null && node.isString() ? node.asString() : null;
    }

    /**
     * Get a boolean configuration value.
     *
     * @param key the configuration key
     * @param defaultValue the default value if not set
     * @return the boolean value, or defaultValue if not set
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        JsonNode node = configs.get(key);
        return node != null && node.isBoolean() ? node.asBoolean() : defaultValue;
    }

    /**
     * Get an integer configuration value.
     *
     * @param key the configuration key
     * @param defaultValue the default value if not set
     * @return the integer value, or defaultValue if not set
     */
    public int getInt(String key, int defaultValue) {
        JsonNode node = configs.get(key);
        return node != null && node.isNumber() ? node.asInt() : defaultValue;
    }

    /**
     * Get the simple upload URL if configured.
     *
     * @return the upload URL, or null if not configured
     */
    public String getSimpleUploadUrl() {
        JsonNode node = configs.get("simpleUpload");
        if (node != null && node.isObject() && node.has("uploadUrl")) {
            return node.get("uploadUrl").asString();
        }
        return null;
    }

    /**
     * Get the autosave waiting time.
     *
     * @return the autosave waiting time in milliseconds, or -1 if not configured
     */
    public int getAutosaveWaitingTime() {
        JsonNode node = configs.get("autosave");
        if (node != null && node.isObject() && node.has("waitingTime")) {
            return node.get("waitingTime").asInt();
        }
        return -1;
    }

    /**
     * Check if autosave is configured.
     *
     * @return true if autosave is configured
     */
    public boolean hasAutosave() {
        return configs.containsKey("autosave");
    }

    /**
     * Get the license key.
     *
     * @return the license key, or null if not set
     */
    public String getLicenseKey() {
        JsonNode node = configs.get("licenseKey");
        return node != null && node.isString() ? node.asString() : null;
    }

    /**
     * Get the font size options.
     *
     * @return array of font size options, or null if not configured
     */
    public String[] getFontSizeOptions() {
        JsonNode node = configs.get("fontSize");
        if (node != null && node.isObject() && node.has("options")) {
            return jsonArrayToStringArray(node.get("options"));
        }
        return null;
    }

    /**
     * Check if font size supports all values.
     *
     * @return true if supportAllValues is enabled, false otherwise
     */
    public boolean isFontSizeSupportAllValues() {
        JsonNode node = configs.get("fontSize");
        if (node != null && node.isObject() && node.has("supportAllValues")) {
            return node.get("supportAllValues").asBoolean();
        }
        return false;
    }

    /**
     * Get the font family options.
     *
     * @return array of font family options, or null if not configured
     */
    public String[] getFontFamilyOptions() {
        JsonNode node = configs.get("fontFamily");
        if (node != null && node.isObject() && node.has("options")) {
            return jsonArrayToStringArray(node.get("options"));
        }
        return null;
    }

    /**
     * Check if font family supports all values.
     *
     * @return true if supportAllValues is enabled, false otherwise
     */
    public boolean isFontFamilySupportAllValues() {
        JsonNode node = configs.get("fontFamily");
        if (node != null && node.isObject() && node.has("supportAllValues")) {
            return node.get("supportAllValues").asBoolean();
        }
        return false;
    }

    /**
     * Get the alignment options.
     *
     * @return array of alignment options, or null if not configured
     */
    public String[] getAlignmentOptions() {
        JsonNode node = configs.get("alignment");
        if (node != null && node.isObject() && node.has("options")) {
            return jsonArrayToStringArray(node.get("options"));
        }
        return null;
    }

    /**
     * Get the link default protocol.
     *
     * @return the default protocol, or null if not configured
     */
    public String getLinkDefaultProtocol() {
        JsonNode node = configs.get("link");
        if (node != null && node.isObject() && node.has("defaultProtocol")) {
            return node.get("defaultProtocol").asString();
        }
        return null;
    }

    /**
     * Check if links should add target to external links.
     *
     * @return true if addTargetToExternalLinks is enabled, false otherwise
     */
    public boolean isLinkAddTargetToExternalLinks() {
        JsonNode node = configs.get("link");
        if (node != null && node.isObject() && node.has("addTargetToExternalLinks")) {
            return node.get("addTargetToExternalLinks").asBoolean();
        }
        return false;
    }

    /**
     * Get the image toolbar items.
     *
     * @return array of image toolbar items, or null if not configured
     */
    public String[] getImageToolbar() {
        JsonNode node = configs.get("image");
        if (node != null && node.isObject() && node.has("toolbar")) {
            return jsonArrayToStringArray(node.get("toolbar"));
        }
        return null;
    }

    /**
     * Get the image styles.
     *
     * @return array of image styles, or null if not configured
     */
    public String[] getImageStyles() {
        JsonNode node = configs.get("image");
        if (node != null && node.isObject() && node.has("styles")) {
            return jsonArrayToStringArray(node.get("styles"));
        }
        return null;
    }

    /**
     * Get the table content toolbar items.
     *
     * @return array of table toolbar items, or null if not configured
     */
    public String[] getTableContentToolbar() {
        JsonNode node = configs.get("table");
        if (node != null && node.isObject() && node.has("contentToolbar")) {
            return jsonArrayToStringArray(node.get("contentToolbar"));
        }
        return null;
    }

    /**
     * Get the code block indent sequence.
     *
     * @return the indent sequence, or null if not configured
     */
    public String getCodeBlockIndentSequence() {
        JsonNode node = configs.get("codeBlock");
        if (node != null && node.isObject() && node.has("indentSequence")) {
            return node.get("indentSequence").asString();
        }
        return null;
    }

    /**
     * Check if media embed includes previews in data.
     *
     * @return true if previewsInData is enabled, false otherwise
     */
    public boolean isMediaEmbedPreviewsInData() {
        JsonNode node = configs.get("mediaEmbed");
        if (node != null && node.isObject() && node.has("previewsInData")) {
            return node.get("previewsInData").asBoolean();
        }
        return false;
    }

    /**
     * Check if HTML support allows all elements.
     *
     * @return true if HTML support is configured with allow all, false otherwise
     */
    public boolean isHtmlSupportAllowAll() {
        JsonNode node = configs.get("htmlSupport");
        if (node != null && node.isObject() && node.has("allow")) {
            JsonNode allowNode = node.get("allow");
            if (allowNode.isArray() && !allowNode.isEmpty()) {
                JsonNode firstAllow = allowNode.get(0);
                return firstAllow.has("name") && "/.*/".equals(firstAllow.get("name").asString());
            }
        }
        return false;
    }

    /**
     * Get the toolbar style configuration.
     *
     * @return the toolbar style, or null if not configured
     */
    public ToolbarStyle getToolbarStyle() {
        JsonNode node = configs.get("toolbarStyle");
        if (node == null || !node.isObject()) {
            return null;
        }
        ToolbarStyle.Builder builder = ToolbarStyle.builder();
        if (node.has("background")) builder.background(node.get("background").asString());
        if (node.has("borderColor")) builder.borderColor(node.get("borderColor").asString());
        if (node.has("borderRadius")) builder.borderRadius(node.get("borderRadius").asString());
        if (node.has("buttonBackground")) builder.buttonBackground(node.get("buttonBackground").asString());
        if (node.has("buttonHoverBackground")) builder.buttonHoverBackground(node.get("buttonHoverBackground").asString());
        if (node.has("buttonActiveBackground")) builder.buttonActiveBackground(node.get("buttonActiveBackground").asString());
        if (node.has("buttonOnBackground")) builder.buttonOnBackground(node.get("buttonOnBackground").asString());
        if (node.has("buttonOnColor")) builder.buttonOnColor(node.get("buttonOnColor").asString());
        if (node.has("iconColor")) builder.iconColor(node.get("iconColor").asString());
        return builder.build();
    }

    /**
     * Get the UI viewport offset.
     *
     * @return array of [top, right, bottom, left] values, or null if not configured
     */
    public double[] getUiViewportOffset() {
        JsonNode node = configs.get("ui");
        if (node != null && node.isObject() && node.has("viewportOffset")) {
            JsonNode offset = node.get("viewportOffset");
            if (offset.isObject()) {
                return new double[] {
                    offset.has("top") ? offset.get("top").asDouble() : 0,
                    offset.has("right") ? offset.get("right").asDouble() : 0,
                    offset.has("bottom") ? offset.get("bottom").asDouble() : 0,
                    offset.has("left") ? offset.get("left").asDouble() : 0
                };
            }
        }
        return null;
    }

    /**
     * Get the raw JSON node for a configuration key.
     * Use this for complex configurations that don't have dedicated getters.
     *
     * @param key the configuration key
     * @return the JSON node, or null if not set
     */
    public JsonNode getJsonNode(String key) {
        return configs.get(key);
    }

    /**
     * Get heading options configuration.
     *
     * @return the heading node for further inspection, or null if not configured
     */
    public JsonNode getHeadingOptions() {
        JsonNode node = configs.get("heading");
        if (node != null && node.isObject() && node.has("options")) {
            return node.get("options");
        }
        return null;
    }

    /**
     * Get mention feeds configuration.
     *
     * @return the mention feeds node for further inspection, or null if not configured
     */
    public JsonNode getMentionFeeds() {
        JsonNode node = configs.get("mention");
        if (node != null && node.isObject() && node.has("feeds")) {
            return node.get("feeds");
        }
        return null;
    }

    /**
     * Get style definitions configuration.
     *
     * @return the style definitions node for further inspection, or null if not configured
     */
    public JsonNode getStyleDefinitions() {
        JsonNode node = configs.get("style");
        if (node != null && node.isObject() && node.has("definitions")) {
            return node.get("definitions");
        }
        return null;
    }

    /**
     * Check if strict plugin loading is enabled.
     *
     * @return true if strictPluginLoading is set, false otherwise
     */
    public boolean isStrictPluginLoading() {
        JsonNode node = configs.get("strictPluginLoading");
        return node != null && node.asBoolean();
    }

    /**
     * Check if config-required plugins are allowed.
     *
     * @return true if allowConfigRequiredPlugins is set, false otherwise
     */
    public boolean isAllowConfigRequiredPlugins() {
        JsonNode node = configs.get("allowConfigRequiredPlugins");
        return node != null && node.asBoolean();
    }

    /**
     * Convert a JSON array node to a String array.
     */
    private static String[] jsonArrayToStringArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return null;
        }
        String[] result = new String[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            result[i] = arrayNode.get(i).asString();
        }
        return result;
    }

    /**
     * Heading option definition
     */
    public static class HeadingOption {
        private final String model;
        private final String view;
        private final String title;
        private final String className;

        public HeadingOption(String model, String view, String title, String className) {
            this.model = model;
            this.view = view;
            this.title = title;
            this.className = className;
        }

        public static HeadingOption paragraph(String title, String className) {
            return new HeadingOption("paragraph", null, title, className);
        }

        public static HeadingOption heading(int level, String title, String className) {
            return new HeadingOption("heading" + level, "h" + level, title, className);
        }

        public ObjectNode toJson() {
            ObjectNode obj = createObjectNode();
            obj.put("model", model);
            if (view != null) {
                obj.put("view", view);
            }
            obj.put("title", title);
            obj.put("class", className);
            return obj;
        }
    }

    /**
     * Code block language definition
     */
    public static class CodeBlockLanguage {
        private final String language;
        private final String label;
        private final String className;

        public CodeBlockLanguage(String language, String label, String className) {
            this.language = language;
            this.label = label;
            this.className = className;
        }

        public static CodeBlockLanguage of(String language, String label) {
            return new CodeBlockLanguage(language, label, null);
        }

        public static CodeBlockLanguage of(String language, String label, String className) {
            return new CodeBlockLanguage(language, label, className);
        }

        public ObjectNode toJson() {
            ObjectNode obj = createObjectNode();
            obj.put("language", language);
            obj.put("label", label);
            if (className != null) {
                obj.put("class", className);
            }
            return obj;
        }
    }

    /**
     * Mention feed definition
     */
    public static class MentionFeed {
        private final String marker;
        private final String[] feed;
        private final int minimumCharacters;

        public MentionFeed(String marker, String[] feed, int minimumCharacters) {
            this.marker = marker;
            this.feed = feed;
            this.minimumCharacters = minimumCharacters;
        }

        public static MentionFeed users(String... users) {
            return new MentionFeed("@", users, 0);
        }

        public static MentionFeed tags(String... tags) {
            return new MentionFeed("#", tags, 0);
        }

        public ObjectNode toJson() {
            ObjectNode obj = createObjectNode();
            obj.put("marker", marker != null ? marker : "@");
            obj.put("minimumCharacters", minimumCharacters);
            obj.set("feed", toArrayNode(feed));
            return obj;
        }
    }

    /**
     * Style definition for the Style plugin.
     * Defines a style that applies CSS classes to HTML elements.
     *
     * <p>Usage example:</p>
     * <pre>
     * config.setStyle(
     *     // Block styles (applied to block elements like p, h2, blockquote)
     *     StyleDefinition.block("Info box", "p", "info-box"),
     *     StyleDefinition.block("Big heading", "h2", "big-heading"),
     *     StyleDefinition.block("Side quote", "blockquote", "side-quote"),
     *     // Inline styles (applied to inline elements like span)
     *     StyleDefinition.inline("Marker", "marker"),
     *     StyleDefinition.inline("Typewriter", "typewriter"),
     *     StyleDefinition.inline("Spoiler", "spoiler")
     * );
     * </pre>
     */
    public static class StyleDefinition {
        private final String name;
        private final String element;
        private final String[] classes;

        /**
         * Create a style definition
         *
         * @param name display name shown in the dropdown
         * @param element HTML element to apply style to (e.g., "p", "h2", "span", "blockquote")
         * @param classes CSS classes to apply
         */
        public StyleDefinition(String name, String element, String... classes) {
            this.name = name;
            this.element = element;
            this.classes = classes;
        }

        /**
         * Create a block style definition.
         * Block styles are applied to block-level elements like paragraphs, headings, blockquotes.
         *
         * @param name display name
         * @param element block element (e.g., "p", "h2", "h3", "blockquote", "pre")
         * @param classes CSS classes to apply
         * @return style definition
         */
        public static StyleDefinition block(String name, String element, String... classes) {
            return new StyleDefinition(name, element, classes);
        }

        /**
         * Create an inline style definition.
         * Inline styles are applied to text using span elements.
         *
         * @param name display name
         * @param classes CSS classes to apply to span element
         * @return style definition
         */
        public static StyleDefinition inline(String name, String... classes) {
            return new StyleDefinition(name, "span", classes);
        }

        /**
         * Create a code block style definition.
         *
         * @param name display name
         * @param classes CSS classes to apply to pre element
         * @return style definition
         */
        public static StyleDefinition codeBlock(String name, String... classes) {
            return new StyleDefinition(name, "pre", classes);
        }

        /**
         * Get the style name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the HTML element
         */
        public String getElement() {
            return element;
        }

        /**
         * Get the CSS classes
         */
        public String[] getClasses() {
            return classes;
        }

        /**
         * Convert to JSON for CKEditor configuration
         */
        public ObjectNode toJson() {
            ObjectNode obj = createObjectNode();
            obj.put("name", name);
            obj.put("element", element);
            obj.set("classes", toArrayNode(classes));
            return obj;
        }
    }

    /**
     * CSS 值验证正则表达式。
     * 允许：颜色值（#hex, rgb, rgba, hsl, hsla, named colors）、尺寸值（px, em, rem, %）、transparent、inherit 等。
     * 禁止：url()、expression()、javascript:、分号、花括号等可能导致注入的内容。
     */
    private static final Pattern SAFE_CSS_VALUE_PATTERN = Pattern.compile(
        "^(" +
        // 颜色值
        "#[0-9a-fA-F]{3,8}|" +                                          // #RGB, #RRGGBB, #RRGGBBAA
        "rgba?\\s*\\(\\s*[0-9.,\\s%]+\\s*\\)|" +                         // rgb(), rgba()
        "hsla?\\s*\\(\\s*[0-9.,\\s%deg]+\\s*\\)|" +                      // hsl(), hsla()
        // 命名颜色
        "[a-zA-Z]+|" +                                                   // named colors like red, blue, transparent
        // 尺寸值
        "-?[0-9.]+(?:px|em|rem|%|pt|vh|vw|vmin|vmax|ch|ex)?|" +         // 10px, 1.5em, 50%
        // 关键字
        "transparent|inherit|initial|unset|none|auto" +
        ")$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 验证 CSS 值是否安全（防止 CSS 注入攻击）。
     *
     * @param value CSS 值
     * @param propertyName 属性名（用于错误消息）
     * @throws IllegalArgumentException 如果值包含不安全内容
     */
    private static void validateCssValue(String value, String propertyName) {
        if (value == null || value.isEmpty()) {
            return; // null 和空值是允许的
        }

        // 检查危险模式
        String lowerValue = value.toLowerCase(Locale.ROOT);
        if (lowerValue.contains("url(") ||
            lowerValue.contains("expression(") ||
            lowerValue.contains("javascript:") ||
            lowerValue.contains("data:") ||
            value.contains(";") ||
            value.contains("{") ||
            value.contains("}") ||
            value.contains("/*") ||
            value.contains("*/") ||
            value.contains("\\")) {
            throw new IllegalArgumentException(
                "CSS value for '" + propertyName + "' contains potentially dangerous content: " + value);
        }

        // 验证格式
        if (!SAFE_CSS_VALUE_PATTERN.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(
                "CSS value for '" + propertyName + "' has invalid format: " + value +
                ". Allowed: color values (#hex, rgb, rgba, named), sizes (px, em, %), keywords (transparent, inherit).");
        }
    }

    /**
     * Toolbar style configuration for customizing CKEditor toolbar appearance.
     * Uses CSS injection for scoped styling per editor instance.
     *
     * <p>Supports styling:</p>
     * <ul>
     *   <li>Toolbar background, border, and border radius</li>
     *   <li>Button states (default, hover, active, on)</li>
     *   <li>Icon colors</li>
     *   <li>Individual button styles via buttonStyles map</li>
     * </ul>
     *
     * <p><b>Security:</b> All CSS values are validated to prevent CSS injection attacks.
     * Only safe color values, sizes, and keywords are allowed.</p>
     *
     * <p>Usage example:</p>
     * <pre>
     * ToolbarStyle style = ToolbarStyle.builder()
     *     .background("#ffffff")
     *     .borderColor("#e0e0e0")
     *     .borderRadius("4px")
     *     .buttonBackground("transparent")
     *     .buttonHoverBackground("rgba(0, 0, 0, 0.05)")
     *     .buttonOnBackground("#e3f2fd")
     *     .buttonOnColor("#1976d2")
     *     .iconColor("#424242")
     *     .buttonStyle("Bold", ButtonStyle.builder()
     *         .background("#fff3e0")
     *         .iconColor("#e65100")
     *         .build())
     *     .build();
     * </pre>
     */
    public static class ToolbarStyle {
        private final String background;
        private final String borderColor;
        private final String borderRadius;
        private final String buttonBackground;
        private final String buttonHoverBackground;
        private final String buttonActiveBackground;
        private final String buttonOnBackground;
        private final String buttonOnColor;
        private final String iconColor;
        private final Map<String, ButtonStyle> buttonStyles;

        private ToolbarStyle(Builder builder) {
            this.background = builder.background;
            this.borderColor = builder.borderColor;
            this.borderRadius = builder.borderRadius;
            this.buttonBackground = builder.buttonBackground;
            this.buttonHoverBackground = builder.buttonHoverBackground;
            this.buttonActiveBackground = builder.buttonActiveBackground;
            this.buttonOnBackground = builder.buttonOnBackground;
            this.buttonOnColor = builder.buttonOnColor;
            this.iconColor = builder.iconColor;
            this.buttonStyles = builder.buttonStyles;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getBackground() {
            return background;
        }

        public String getBorderColor() {
            return borderColor;
        }

        public String getBorderRadius() {
            return borderRadius;
        }

        public String getButtonBackground() {
            return buttonBackground;
        }

        public String getButtonHoverBackground() {
            return buttonHoverBackground;
        }

        public String getButtonActiveBackground() {
            return buttonActiveBackground;
        }

        public String getButtonOnBackground() {
            return buttonOnBackground;
        }

        public String getButtonOnColor() {
            return buttonOnColor;
        }

        public String getIconColor() {
            return iconColor;
        }

        public Map<String, ButtonStyle> getButtonStyles() {
            return buttonStyles;
        }

        /**
         * Convert to JSON for frontend configuration.
         */
        public ObjectNode toJson() {
            ObjectNode obj = createObjectNode();
            if (background != null) obj.put("background", background);
            if (borderColor != null) obj.put("borderColor", borderColor);
            if (borderRadius != null) obj.put("borderRadius", borderRadius);
            if (buttonBackground != null) obj.put("buttonBackground", buttonBackground);
            if (buttonHoverBackground != null) obj.put("buttonHoverBackground", buttonHoverBackground);
            if (buttonActiveBackground != null) obj.put("buttonActiveBackground", buttonActiveBackground);
            if (buttonOnBackground != null) obj.put("buttonOnBackground", buttonOnBackground);
            if (buttonOnColor != null) obj.put("buttonOnColor", buttonOnColor);
            if (iconColor != null) obj.put("iconColor", iconColor);
            if (buttonStyles != null && !buttonStyles.isEmpty()) {
                ObjectNode stylesObj = createObjectNode();
                buttonStyles.forEach((key, style) -> stylesObj.set(key, style.toJson()));
                obj.set("buttonStyles", stylesObj);
            }
            return obj;
        }

        public static class Builder {
            private String background;
            private String borderColor;
            private String borderRadius;
            private String buttonBackground;
            private String buttonHoverBackground;
            private String buttonActiveBackground;
            private String buttonOnBackground;
            private String buttonOnColor;
            private String iconColor;
            private Map<String, ButtonStyle> buttonStyles;

            private Builder() {}

            public Builder background(String background) {
                this.background = background;
                return this;
            }

            public Builder borderColor(String borderColor) {
                this.borderColor = borderColor;
                return this;
            }

            public Builder borderRadius(String borderRadius) {
                this.borderRadius = borderRadius;
                return this;
            }

            public Builder buttonBackground(String buttonBackground) {
                this.buttonBackground = buttonBackground;
                return this;
            }

            public Builder buttonHoverBackground(String buttonHoverBackground) {
                this.buttonHoverBackground = buttonHoverBackground;
                return this;
            }

            public Builder buttonActiveBackground(String buttonActiveBackground) {
                this.buttonActiveBackground = buttonActiveBackground;
                return this;
            }

            public Builder buttonOnBackground(String buttonOnBackground) {
                this.buttonOnBackground = buttonOnBackground;
                return this;
            }

            public Builder buttonOnColor(String buttonOnColor) {
                this.buttonOnColor = buttonOnColor;
                return this;
            }

            public Builder iconColor(String iconColor) {
                this.iconColor = iconColor;
                return this;
            }

            public Builder buttonStyle(String buttonName, ButtonStyle style) {
                if (this.buttonStyles == null) {
                    this.buttonStyles = new LinkedHashMap<>();
                }
                this.buttonStyles.put(buttonName, style);
                return this;
            }

            public Builder buttonStyles(Map<String, ButtonStyle> buttonStyles) {
                this.buttonStyles = buttonStyles;
                return this;
            }

            public ToolbarStyle build() {
                // 验证所有 CSS 值
                validateCssValue(background, "background");
                validateCssValue(borderColor, "borderColor");
                validateCssValue(borderRadius, "borderRadius");
                validateCssValue(buttonBackground, "buttonBackground");
                validateCssValue(buttonHoverBackground, "buttonHoverBackground");
                validateCssValue(buttonActiveBackground, "buttonActiveBackground");
                validateCssValue(buttonOnBackground, "buttonOnBackground");
                validateCssValue(buttonOnColor, "buttonOnColor");
                validateCssValue(iconColor, "iconColor");
                return new ToolbarStyle(this);
            }
        }
    }

    /**
     * Individual button style configuration.
     * Used within ToolbarStyle to customize specific toolbar buttons.
     *
     * <p>Usage example:</p>
     * <pre>
     * ButtonStyle boldStyle = ButtonStyle.builder()
     *     .background("#fff3e0")
     *     .hoverBackground("#ffe0b2")
     *     .iconColor("#e65100")
     *     .build();
     * </pre>
     */
    public static class ButtonStyle {
        private final String background;
        private final String hoverBackground;
        private final String activeBackground;
        private final String iconColor;

        private ButtonStyle(String background, String hoverBackground, String activeBackground, String iconColor) {
            this.background = background;
            this.hoverBackground = hoverBackground;
            this.activeBackground = activeBackground;
            this.iconColor = iconColor;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getBackground() {
            return background;
        }

        public String getHoverBackground() {
            return hoverBackground;
        }

        public String getActiveBackground() {
            return activeBackground;
        }

        public String getIconColor() {
            return iconColor;
        }

        /**
         * Convert to JSON for frontend configuration.
         */
        public ObjectNode toJson() {
            ObjectNode obj = createObjectNode();
            if (background != null) obj.put("background", background);
            if (hoverBackground != null) obj.put("hoverBackground", hoverBackground);
            if (activeBackground != null) obj.put("activeBackground", activeBackground);
            if (iconColor != null) obj.put("iconColor", iconColor);
            return obj;
        }

        public static class Builder {
            private String background;
            private String hoverBackground;
            private String activeBackground;
            private String iconColor;

            private Builder() {}

            public Builder background(String background) {
                this.background = background;
                return this;
            }

            public Builder hoverBackground(String hoverBackground) {
                this.hoverBackground = hoverBackground;
                return this;
            }

            public Builder activeBackground(String activeBackground) {
                this.activeBackground = activeBackground;
                return this;
            }

            public Builder iconColor(String iconColor) {
                this.iconColor = iconColor;
                return this;
            }

            public ButtonStyle build() {
                // 验证所有 CSS 值
                validateCssValue(background, "background");
                validateCssValue(hoverBackground, "hoverBackground");
                validateCssValue(activeBackground, "activeBackground");
                validateCssValue(iconColor, "iconColor");
                return new ButtonStyle(background, hoverBackground, activeBackground, iconColor);
            }
        }
    }
}
