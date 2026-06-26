/**
 * overrideCssUrl 白名单校验（纯逻辑，便于单测）。
 *
 * 背景（review 发现）：原校验用子串黑名单（包含 'javascript:'/'data:' 就拒绝），可被大小写
 * 混写、换行等绕过。改为白名单：解析 URL，只允许 http/https 绝对地址与相对路径，其余一律拒绝。
 * 即便该属性由 Java 后端控制（风险较低），白名单也提供纵深防御。
 */

/**
 * 判断 overrideCssUrl 是否允许作为外部样式表加载。
 *
 * 允许：
 * - http: / https: 绝对 URL
 * - 相对路径（无协议，如 ./theme.css、/assets/x.css、theme.css）
 * 拒绝：
 * - 含协议但非 http/https（javascript:、data:、file:、vbscript: 等）
 * - 空白/空串
 *
 * @param rawUrl overrideCssUrl 原值
 * @returns 是否允许加载
 */
export function isAllowedCssUrl(rawUrl: string | null | undefined): boolean {
    if (rawUrl == null) {
        return false;
    }
    const url = rawUrl.trim();
    if (url === '') {
        return false;
    }

    // 探测是否带 scheme：scheme 形如 a-z/数字/+/-/. 开头后接 ':'。
    // 注意要在去除可能的前导空白后判断，且不能被换行绕过——用从串首开始的严格匹配。
    const schemeMatch = /^([a-zA-Z][a-zA-Z0-9+.-]*):/.exec(url);
    if (schemeMatch) {
        const scheme = schemeMatch[1].toLowerCase();
        return scheme === 'http' || scheme === 'https';
    }

    // 无 scheme → 视为相对路径，允许。但 protocol-relative（//host/x）也无 scheme，
    // 浏览器会按当前页协议加载，安全可接受；明确允许。
    return true;
}
