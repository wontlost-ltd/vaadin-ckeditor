/// <reference types="vite/client" />
import { describe, it, expect, vi } from 'vitest';
import {
    TRANSLATION_LOADERS, effectiveUiLanguage, resolveUiLanguage, loadTranslations, withUiLanguage,
} from './translations';

/**
 * 当前 node_modules 中 ckeditor5 实际发布的非英文翻译包代码。
 * 用 import.meta.glob 枚举（不 eager，只取路径），免去 node:fs 与 jsdom 下 URL 的限制。
 */
function bundledLanguageCodes(): string[] {
    return Object.keys(import.meta.glob('./node_modules/ckeditor5/dist/translations/*.js'))
        .map(path => path.slice(path.lastIndexOf('/') + 1, -3))
        .filter(code => code !== 'en' && !code.endsWith('.umd'))
        .sort();
}

/** CKEditor 官方专业翻译列表（ui-language 文档）中的代码 */
const PROFESSIONAL = [
    'ar', 'bn', 'bg', 'ca', 'zh', 'zh-tw', 'cs', 'da', 'nl', 'et', 'fi', 'fr', 'de', 'el', 'he', 'hi',
    'hu', 'id', 'it', 'ja', 'ko', 'lv', 'lt', 'ms', 'no', 'pl', 'pt-br', 'pt', 'ro', 'ru', 'sr', 'sk',
    'es', 'sv', 'th', 'tr', 'uk', 'vi',
];

describe('TRANSLATION_LOADERS', () => {
    it('covers exactly the translation bundles shipped by the pinned ckeditor5', () => {
        expect(Object.keys(TRANSLATION_LOADERS).sort()).toEqual(bundledLanguageCodes());
    });

    it('covers every professional translation code after alias resolution', () => {
        const missing = PROFESSIONAL.map(resolveUiLanguage).filter(code => !TRANSLATION_LOADERS[code]);
        expect(missing).toEqual([]);
    });

    it.each(bundledLanguageCodes())('loads a dictionary keyed by its own code: %s', async code => {
        const translations = await loadTranslations(code);
        expect(translations).toBeDefined();
        const entry = (translations as Record<string, { dictionary: Record<string, unknown> }>)[code];
        expect(Object.keys(entry.dictionary).length).toBeGreaterThan(0);
    });
});

describe('resolveUiLanguage', () => {
    it('maps zh-tw (professional list code) to the zh bundle (Traditional Chinese)', () => {
        expect(resolveUiLanguage('zh-tw')).toBe('zh');
        expect(resolveUiLanguage('zh-TW')).toBe('zh');
    });

    it('keeps zh-cn and zh distinct', () => {
        expect(resolveUiLanguage('zh-cn')).toBe('zh-cn');
        expect(resolveUiLanguage('zh')).toBe('zh');
    });

    it('normalizes case, whitespace and underscores', () => {
        expect(resolveUiLanguage('pt-BR')).toBe('pt-br');
        expect(resolveUiLanguage(' DE ')).toBe('de');
        expect(resolveUiLanguage('sr_Latn')).toBe('sr-latn');
    });

    it('falls back to en for empty input', () => {
        expect(resolveUiLanguage('')).toBe('en');
        expect(resolveUiLanguage(null)).toBe('en');
        expect(resolveUiLanguage(undefined)).toBe('en');
    });

    it('passes unknown codes through unchanged', () => {
        expect(resolveUiLanguage('xx')).toBe('xx');
    });
});

describe('loadTranslations', () => {
    it('does not load anything for en and reports nothing', async () => {
        const onError = vi.fn();
        expect(await loadTranslations('en', onError)).toBeUndefined();
        expect(onError).not.toHaveBeenCalled();
    });

    it('reports an unknown language and returns undefined', async () => {
        const onError = vi.fn();
        expect(await loadTranslations('xx', onError)).toBeUndefined();
        expect(onError).toHaveBeenCalledWith(expect.stringContaining("'xx'"));
    });
});

describe('effectiveUiLanguage (config.language 优先于 language 属性)', () => {
    it('config string wins over the property', () => {
        expect(effectiveUiLanguage('pt', 'en')).toBe('pt');
    });

    it('object form .ui wins over the property', () => {
        expect(effectiveUiLanguage({ ui: 'ar', content: 'he' }, 'en')).toBe('ar');
    });

    it('falls back to the property when config has no language', () => {
        expect(effectiveUiLanguage(undefined, 'de')).toBe('de');
        expect(effectiveUiLanguage(null, 'de')).toBe('de');
        expect(effectiveUiLanguage('', 'de')).toBe('de');
        expect(effectiveUiLanguage({ content: 'ar' }, 'de')).toBe('de');
    });

    it('object form keeps content after resolving and writing back the ui', () => {
        const configLanguage = { ui: 'ZH-TW', content: 'ar', textPartLanguage: [{ title: 'Arabic', languageCode: 'ar' }] };
        const ui = resolveUiLanguage(effectiveUiLanguage(configLanguage, 'en'));
        expect(ui).toBe('zh');
        expect(withUiLanguage(configLanguage, ui)).toEqual({ ...configLanguage, ui: 'zh' });
    });
});

describe('loadTranslations failure handling', () => {
    it('resolves to undefined and reports a warning when the loader rejects', async () => {
        const onError = vi.fn();
        const loaders = { de: () => Promise.reject(new Error('chunk load failed')) };
        await expect(loadTranslations('de', onError, loaders)).resolves.toBeUndefined();
        expect(onError).toHaveBeenCalledTimes(1);
        expect(onError).toHaveBeenCalledWith(expect.stringContaining('chunk load failed'));
    });
});

describe('withUiLanguage', () => {
    it('replaces a string or missing language with the resolved UI language', () => {
        expect(withUiLanguage('pt-BR', 'pt-br')).toBe('pt-br');
        expect(withUiLanguage(undefined, 'de')).toBe('de');
    });

    it('keeps content and textPartLanguage of the object form, replacing only ui', () => {
        expect(withUiLanguage({ ui: 'zh-tw', content: 'ar', textPartLanguage: [] }, 'zh'))
            .toEqual({ ui: 'zh', content: 'ar', textPartLanguage: [] });
    });
});
