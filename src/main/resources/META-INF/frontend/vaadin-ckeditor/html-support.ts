/**
 * General HTML Support 配置中的正则（Java `HtmlSupport` / `CKEditorConfig.setHtmlSupport`）。
 *
 * JSON 没有 RegExp 类型：Java 端把 `Pattern` 序列化为 `{"$regexp": "source", "$flags": "i"}`，这里在创建编辑器前
 * 把 `htmlSupport.allow` / `disallow` 中的此类对象还原为 `RegExp`。CKEditor 只把 `RegExp` 实例当作模式
 * （@ckeditor/ckeditor5-html-support 49.1.0 `testViewName`：字符串按全等比较），因此此前 `setHtmlSupport(true)`
 * 发送的字符串 `"/.*\/"` 只会匹配名为 "/.*\/" 的元素，等于没有生效。
 *
 * 只识别键恰为 `$regexp`（可选 `$flags`）且值为字符串的对象；无效正则抛错，由编辑器创建流程报告，
 * 不会静默放宽或收紧规则。
 */
export const REGEXP_KEY = '$regexp';
export const FLAGS_KEY = '$flags';

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value)
        && Object.getPrototypeOf(value) === Object.prototype;
}

function isRegExpMarker(value: Record<string, unknown>): boolean {
    const keys = Object.keys(value);
    return typeof value[REGEXP_KEY] === 'string'
        && keys.every((k) => k === REGEXP_KEY || (k === FLAGS_KEY && typeof value[FLAGS_KEY] === 'string'));
}

/** 递归还原正则标记，返回新结构（不修改输入） */
export function revivePatterns(value: unknown): unknown {
    if (Array.isArray(value)) {
        return value.map(revivePatterns);
    }
    if (!isPlainObject(value)) {
        return value;
    }
    if (isRegExpMarker(value)) {
        const source = value[REGEXP_KEY] as string;
        const flags = (value[FLAGS_KEY] as string | undefined) ?? '';
        try {
            return new RegExp(source, flags);
        } catch (error) {
            throw new Error(`Invalid htmlSupport pattern /${source}/${flags}: ${(error as Error).message}`);
        }
    }
    const result: Record<string, unknown> = {};
    for (const [key, item] of Object.entries(value)) {
        result[key] = revivePatterns(item);
    }
    return result;
}

/** 还原 `config.htmlSupport.allow` / `disallow` 中的正则；没有 htmlSupport 时原样返回同一对象 */
export function withHtmlSupportPatterns<T extends Record<string, unknown>>(config: T): T {
    const htmlSupport = config.htmlSupport;
    if (!isPlainObject(htmlSupport)) {
        return config;
    }
    const next: Record<string, unknown> = { ...htmlSupport };
    for (const key of ['allow', 'disallow']) {
        if (Array.isArray(next[key])) {
            next[key] = revivePatterns(next[key]);
        }
    }
    return { ...config, htmlSupport: next };
}
