/**
 * CKEditor 5 UI 翻译包的按需加载。
 *
 * ckeditor5 随包发布 `dist/translations/<code>.js`（49.1.0 共 72 个非英文包）。
 * 早期实现只静态 import 了 10 个，其余语言即使设置了 language 也只显示英文；
 * 全量静态 import 又会把几 MB 字典打进首屏。这里改为显式的动态 import 表：
 * Vite 为每个语言生成独立 chunk，只有实际使用的语言才会被下载。
 *
 * 表必须写成字面量 `import('ckeditor5/translations/xx.js')`——Vite 不支持对裸模块说明符
 * 做模板字符串动态导入。新增 / 删除语言随 ckeditor5 升级而变，translations.test.ts
 * 会把本表与 node_modules 中实际的包文件逐一比对，升级后漏改会直接失败。
 */
import type { Translations } from 'ckeditor5';

type TranslationModule = { default: Translations };

/** 语言代码（与包文件名一致，小写）→ 翻译包加载器 */
export const TRANSLATION_LOADERS: Readonly<Record<string, () => Promise<TranslationModule>>> = {
    'af': () => import('ckeditor5/translations/af.js'),
    'ar': () => import('ckeditor5/translations/ar.js'),
    'ast': () => import('ckeditor5/translations/ast.js'),
    'az': () => import('ckeditor5/translations/az.js'),
    'be': () => import('ckeditor5/translations/be.js'),
    'bg': () => import('ckeditor5/translations/bg.js'),
    'bn': () => import('ckeditor5/translations/bn.js'),
    'bs': () => import('ckeditor5/translations/bs.js'),
    'ca': () => import('ckeditor5/translations/ca.js'),
    'cs': () => import('ckeditor5/translations/cs.js'),
    'da': () => import('ckeditor5/translations/da.js'),
    'de-ch': () => import('ckeditor5/translations/de-ch.js'),
    'de': () => import('ckeditor5/translations/de.js'),
    'el': () => import('ckeditor5/translations/el.js'),
    'en-au': () => import('ckeditor5/translations/en-au.js'),
    'en-gb': () => import('ckeditor5/translations/en-gb.js'),
    'eo': () => import('ckeditor5/translations/eo.js'),
    'es-co': () => import('ckeditor5/translations/es-co.js'),
    'es': () => import('ckeditor5/translations/es.js'),
    'et': () => import('ckeditor5/translations/et.js'),
    'eu': () => import('ckeditor5/translations/eu.js'),
    'fa': () => import('ckeditor5/translations/fa.js'),
    'fi': () => import('ckeditor5/translations/fi.js'),
    'fr': () => import('ckeditor5/translations/fr.js'),
    'gl': () => import('ckeditor5/translations/gl.js'),
    'gu': () => import('ckeditor5/translations/gu.js'),
    'he': () => import('ckeditor5/translations/he.js'),
    'hi': () => import('ckeditor5/translations/hi.js'),
    'hr': () => import('ckeditor5/translations/hr.js'),
    'hu': () => import('ckeditor5/translations/hu.js'),
    'hy': () => import('ckeditor5/translations/hy.js'),
    'id': () => import('ckeditor5/translations/id.js'),
    'it': () => import('ckeditor5/translations/it.js'),
    'ja': () => import('ckeditor5/translations/ja.js'),
    'jv': () => import('ckeditor5/translations/jv.js'),
    'kk': () => import('ckeditor5/translations/kk.js'),
    'km': () => import('ckeditor5/translations/km.js'),
    'kn': () => import('ckeditor5/translations/kn.js'),
    'ko': () => import('ckeditor5/translations/ko.js'),
    'ku': () => import('ckeditor5/translations/ku.js'),
    'lt': () => import('ckeditor5/translations/lt.js'),
    'lv': () => import('ckeditor5/translations/lv.js'),
    'ms': () => import('ckeditor5/translations/ms.js'),
    'nb': () => import('ckeditor5/translations/nb.js'),
    'ne': () => import('ckeditor5/translations/ne.js'),
    'nl': () => import('ckeditor5/translations/nl.js'),
    'no': () => import('ckeditor5/translations/no.js'),
    'oc': () => import('ckeditor5/translations/oc.js'),
    'pl': () => import('ckeditor5/translations/pl.js'),
    'pt-br': () => import('ckeditor5/translations/pt-br.js'),
    'pt': () => import('ckeditor5/translations/pt.js'),
    'ro': () => import('ckeditor5/translations/ro.js'),
    'ru': () => import('ckeditor5/translations/ru.js'),
    'si': () => import('ckeditor5/translations/si.js'),
    'sk': () => import('ckeditor5/translations/sk.js'),
    'sl': () => import('ckeditor5/translations/sl.js'),
    'sq': () => import('ckeditor5/translations/sq.js'),
    'sr-latn': () => import('ckeditor5/translations/sr-latn.js'),
    'sr': () => import('ckeditor5/translations/sr.js'),
    'sv': () => import('ckeditor5/translations/sv.js'),
    'th': () => import('ckeditor5/translations/th.js'),
    'ti': () => import('ckeditor5/translations/ti.js'),
    'tk': () => import('ckeditor5/translations/tk.js'),
    'tr': () => import('ckeditor5/translations/tr.js'),
    'tt': () => import('ckeditor5/translations/tt.js'),
    'ug': () => import('ckeditor5/translations/ug.js'),
    'uk': () => import('ckeditor5/translations/uk.js'),
    'ur': () => import('ckeditor5/translations/ur.js'),
    'uz': () => import('ckeditor5/translations/uz.js'),
    'vi': () => import('ckeditor5/translations/vi.js'),
    'zh-cn': () => import('ckeditor5/translations/zh-cn.js'),
    'zh': () => import('ckeditor5/translations/zh.js'),
};

/**
 * 官方文档使用、但与包文件名不一致的代码 → 包文件名。
 * 专业翻译列表把繁体中文（台湾）写作 `zh-tw`，而包里的繁体文件是 `zh.js`
 * （简体为 `zh-cn.js`）。不做映射时 `zh-tw` 会静默退回英文。
 */
const LANGUAGE_ALIASES: Readonly<Record<string, string>> = {
    'zh-tw': 'zh',
};

/**
 * 把用户给的 UI 语言代码归一化为 CKEditor 使用的代码：去空白、转小写、`_` 视作 `-`，
 * 再套用别名。空值退回 `en`。未知代码原样返回（CKEditor 会以英文显示）。
 */
export function resolveUiLanguage(language: string | null | undefined): string {
    const normalized = (language ?? '').trim().toLowerCase().replace(/_/g, '-');
    if (!normalized) {
        return 'en';
    }
    return LANGUAGE_ALIASES[normalized] ?? normalized;
}

/**
 * 取生效的 UI 语言（未归一化）：沿用原有优先级，`config.language` 优先于组件 `language` 属性。
 * config.language 可以是字符串，也可以是 `{ ui, content, textPartLanguage }` 对象（取其 ui）；
 * 两者都缺省时返回属性值。
 */
export function effectiveUiLanguage(configLanguage: unknown, propertyLanguage: string | null | undefined): string | null | undefined {
    if (typeof configLanguage === 'string' && configLanguage.trim() !== '') {
        return configLanguage;
    }
    if (typeof configLanguage === 'object' && configLanguage !== null) {
        const ui = (configLanguage as Record<string, unknown>).ui;
        if (typeof ui === 'string' && ui.trim() !== '') {
            return ui;
        }
    }
    return propertyLanguage;
}

/**
 * 加载已归一化语言代码的翻译包。英文无需加载；未知语言或加载失败返回 undefined
 * （编辑器以英文 UI 继续创建，不阻断），失败原因交给 onError 记录。
 */
export async function loadTranslations(
    uiLanguage: string,
    onError?: (message: string) => void,
    loaders: Readonly<Record<string, () => Promise<TranslationModule>>> = TRANSLATION_LOADERS,
): Promise<Translations | undefined> {
    const loader = uiLanguage === 'en' ? undefined : loaders[uiLanguage];
    if (!loader) {
        if (uiLanguage !== 'en') {
            onError?.(`No CKEditor translation bundle for UI language '${uiLanguage}'; falling back to English`);
        }
        return undefined;
    }
    try {
        return (await loader()).default;
    } catch (error) {
        onError?.(`Failed to load CKEditor translations for '${uiLanguage}': ${String(error)}`);
        return undefined;
    }
}

/**
 * 把解析后的 UI 语言写回 CKEditor 的 `language` 配置。
 * 字符串（或缺省）形式直接替换；对象形式 `{ ui, content, textPartLanguage }` 只替换 ui，
 * 保留内容语言等其余字段。
 */
export function withUiLanguage(configLanguage: unknown, uiLanguage: string): string | Record<string, unknown> {
    if (typeof configLanguage === 'object' && configLanguage !== null && !Array.isArray(configLanguage)) {
        return { ...(configLanguage as Record<string, unknown>), ui: uiLanguage };
    }
    return uiLanguage;
}
