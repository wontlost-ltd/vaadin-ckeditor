/**
 * 界面语言解析与 CKEditor 5 翻译包按需加载（issue #151）。
 *
 * 旧实现只静态打包了 10 种语言，其余语言（如 "sk"）静默回退为英文；"sk-SK" 这类带地区的
 * 写法也因与翻译包键名不一致而失效。这里改为：
 * - {@link TRANSLATION_LOADERS} 覆盖 `ckeditor5/translations/` 下的全部语言，每项是一个
 *   字面量 `import()`，由 Vite 拆分为独立 chunk，仅在实际使用该语言时下载，不增加主包体积；
 * - {@link resolveLanguageCode} 把 BCP 47 / Java `Locale` 风格的代码规范化并逐级回退到
 *   CKEditor 实际提供的翻译键（"sk-SK" → "sk"、"sr-Latn-RS" → "sr-latn"）。
 *
 * 升级 ckeditor5 时，translation-loader.test.ts 会比对 node_modules 中的翻译文件清单，
 * 新增或删除语言都会使测试失败，从而提醒同步本表。
 */
import type { Translations } from 'ckeditor5';

type TranslationModule = { default: Translations };

/** CKEditor 的源语言，自带英文文案，无需加载翻译包。 */
export const SOURCE_LANGUAGE = 'en';

/** 语言代码 → 翻译包的懒加载函数。键与 ckeditor5/translations/*.js 文件名一一对应。 */
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
    'en': () => import('ckeditor5/translations/en.js'),
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
 * 把语言代码规范化为 CKEditor 翻译键：小写、下划线转连字符，再逐级去掉末尾子标签直到命中。
 *
 * @returns 命中的翻译键；输入为空或任何前缀都不受支持时返回 null
 */
export function resolveLanguageCode(code: string | null | undefined): string | null {
    if (typeof code !== 'string') {
        return null;
    }
    const subtags = code.trim().toLowerCase().replace(/_/g, '-').split('-').filter(Boolean);
    for (let length = subtags.length; length > 0; length--) {
        const prefix = subtags.slice(0, length).join('-');
        const candidate = LANGUAGE_ALIASES[prefix] ?? prefix;
        if (Object.prototype.hasOwnProperty.call(TRANSLATION_LOADERS, candidate)) {
            return candidate;
        }
    }
    return null;
}

/**
 * 逐级回退无法正确表达的别名：CKEditor 的 "zh" 是繁体、"zh-cn" 是简体，
 * 因此按文字（Hans）或新加坡地区识别的简体必须显式映射，否则会回退成繁体。
 */
const LANGUAGE_ALIASES: Readonly<Record<string, string>> = {
    'zh-hans': 'zh-cn',
    'zh-sg': 'zh-cn',
};

/** CKEditor `language` 配置的对象形态（`ui` / `content` / `textPartLanguage`）。 */
type LanguageObject = Record<string, unknown> & { ui?: unknown };

export interface ResolvedLanguage {
    /** 传给 CKEditor 的 `language` 配置；`ui` 已替换为规范化后的翻译键。 */
    language: string | LanguageObject;
    /** 规范化后的界面语言（翻译键）。 */
    uiLanguage: string;
    /** 请求的界面语言不受支持时为原始值，供调用方告警；否则为 null。 */
    unsupported: string | null;
}

/**
 * 计算最终的 `language` 配置。优先级：`config.language`（字符串或对象的 `ui`）>
 * 组件的 `language` 属性 > 英文。`content` 与 `textPartLanguage` 原样保留——
 * 内容语言只决定 `lang` 属性与书写方向，不需要翻译包。
 */
export function resolveEditorLanguage(configLanguage: unknown, propertyLanguage: string | null | undefined): ResolvedLanguage {
    const isObject = typeof configLanguage === 'object' && configLanguage !== null && !Array.isArray(configLanguage);
    const languageObject = isObject ? configLanguage as LanguageObject : null;

    let requested: string | undefined;
    if (typeof configLanguage === 'string' && configLanguage.trim()) {
        requested = configLanguage;
    } else if (typeof languageObject?.ui === 'string' && languageObject.ui.trim()) {
        requested = languageObject.ui;
    } else if (propertyLanguage && propertyLanguage.trim()) {
        requested = propertyLanguage;
    }

    const resolved = resolveLanguageCode(requested);
    const uiLanguage = resolved ?? SOURCE_LANGUAGE;
    const unsupported = requested !== undefined && resolved === null ? requested : null;
    return {
        language: languageObject
            ? { ...languageObject, ui: uiLanguage }
            : stringLanguage(requested, uiLanguage),
        uiLanguage,
        unsupported,
    };
}

/**
 * 字符串形态：界面语言换成翻译键，但若原值更具体（"sk-SK"）则保留为内容语言，
 * 让编辑区的 `lang` 属性仍反映调用方给出的地区。
 */
function stringLanguage(requested: string | undefined, uiLanguage: string): string | LanguageObject {
    const content = requested?.trim().replace(/_/g, '-');
    if (!content || content.toLowerCase() === uiLanguage) {
        return uiLanguage;
    }
    return { ui: uiLanguage, content };
}

/**
 * 加载界面语言对应的翻译包。英文或加载失败时返回 undefined，编辑器以英文界面继续初始化。
 */
export async function loadTranslations(uiLanguage: string): Promise<Translations | undefined> {
    const loader = uiLanguage === SOURCE_LANGUAGE ? undefined : TRANSLATION_LOADERS[uiLanguage];
    if (!loader) {
        return undefined;
    }
    try {
        return (await loader()).default;
    } catch {
        return undefined;
    }
}
