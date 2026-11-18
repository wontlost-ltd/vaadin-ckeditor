/**
 * translation-loader 单元测试（issue #151）。
 *
 * Run with: npx vitest run translation-loader.test.ts
 */
/// <reference path="./test-mocks/import-meta-glob.d.ts" />
import { describe, it, expect } from 'vitest';
import {
    TRANSLATION_LOADERS,
    resolveLanguageCode,
    resolveEditorLanguage,
    loadTranslations,
} from './translation-loader';

/** node_modules 中 ckeditor5 实际提供的翻译语言（不含 UMD 版本）。只取路径，不加载模块。 */
function shippedLanguages(): string[] {
    const files = Object.keys(import.meta.glob('./node_modules/ckeditor5/dist/translations/*.js'));
    return files
        .map((file) => file.slice(file.lastIndexOf('/') + 1, -'.js'.length))
        .filter((language) => !language.endsWith('.umd'))
        .sort();
}

describe('TRANSLATION_LOADERS', () => {
    it('covers exactly the translations shipped by the installed ckeditor5', () => {
        const shipped = shippedLanguages();
        expect(shipped.length).toBeGreaterThan(50);
        expect(Object.keys(TRANSLATION_LOADERS).sort()).toEqual(shipped);
    });

    it('includes Slovak', () => {
        expect(TRANSLATION_LOADERS).toHaveProperty('sk');
    });
});

describe('resolveLanguageCode', () => {
    it.each([
        ['sk', 'sk'],
        ['SK', 'sk'],
        ['sk-SK', 'sk'],
        ['sk_SK', 'sk'],
        [' de ', 'de'],
        ['de-CH', 'de-ch'],
        ['de-AT', 'de'],
        ['pt-BR', 'pt-br'],
        ['pt-PT', 'pt'],
        ['sr-Latn', 'sr-latn'],
        ['sr-Latn-RS', 'sr-latn'],
        ['sr-RS', 'sr'],
        ['zh-CN', 'zh-cn'],
        ['zh-Hans-CN', 'zh-cn'],
        ['zh-SG', 'zh-cn'],
        ['zh-TW', 'zh'],
        ['zh-Hant-TW', 'zh'],
        ['en-US', 'en'],
        ['en-GB', 'en-gb'],
        ['ast', 'ast'],
    ])('%s → %s', (input, expected) => {
        expect(resolveLanguageCode(input)).toBe(expected);
    });

    it.each([undefined, null, '', '   ', 'xx', 'xx-YY', '-'])('returns null for %j', (input) => {
        expect(resolveLanguageCode(input as string | null | undefined)).toBeNull();
    });
});

describe('resolveEditorLanguage', () => {
    it('uses the component language property when config has none', () => {
        expect(resolveEditorLanguage(undefined, 'sk')).toEqual({ language: 'sk', uiLanguage: 'sk', unsupported: null });
    });

    it('prefers config.language string over the property', () => {
        expect(resolveEditorLanguage('sk', 'en').uiLanguage).toBe('sk');
    });

    it('keeps a region-qualified string as the content language', () => {
        expect(resolveEditorLanguage(undefined, 'sk-SK')).toEqual({
            language: { ui: 'sk', content: 'sk-SK' },
            uiLanguage: 'sk',
            unsupported: null,
        });
    });

    it('normalizes ui of the object form and preserves content and textPartLanguage', () => {
        const textPartLanguage = [{ title: 'Slovak', languageCode: 'sk' }];
        const result = resolveEditorLanguage({ ui: 'sk-SK', content: 'sk', textPartLanguage }, 'en');
        expect(result.language).toEqual({ ui: 'sk', content: 'sk', textPartLanguage });
        expect(result.uiLanguage).toBe('sk');
    });

    it('falls back to the property when the object form has no ui', () => {
        expect(resolveEditorLanguage({ content: 'ar' }, 'de').language).toEqual({ ui: 'de', content: 'ar' });
    });

    it('reports unsupported languages and falls back to English', () => {
        const result = resolveEditorLanguage('xx', 'en');
        expect(result.uiLanguage).toBe('en');
        expect(result.unsupported).toBe('xx');
        expect(result.language).toEqual({ ui: 'en', content: 'xx' });
    });

    it('defaults to English without warnings', () => {
        expect(resolveEditorLanguage(undefined, undefined)).toEqual({ language: 'en', uiLanguage: 'en', unsupported: null });
    });
});

describe('loadTranslations', () => {
    it('loads the Slovak dictionary', async () => {
        const translations = await loadTranslations('sk');
        expect(translations).toBeDefined();
        const dictionary = (translations as Record<string, { dictionary: Record<string, unknown> }>).sk.dictionary;
        expect(dictionary.Bold).toBe('Tučné');
    });

    it('skips English, the source language', async () => {
        expect(await loadTranslations('en')).toBeUndefined();
    });

    it('returns undefined for unknown languages', async () => {
        expect(await loadTranslations('xx')).toBeUndefined();
    });
});
