/**
 * html-support 单测：Java HtmlSupport 序列化的 {"$regexp"} 标记还原为 RegExp，
 * 只作用于 htmlSupport.allow / disallow，不修改输入，无效正则报错。
 */
import { describe, it, expect } from 'vitest';
import { revivePatterns, withHtmlSupportPatterns } from './html-support';

describe('withHtmlSupportPatterns', () => {
    it('turns pattern markers in allow / disallow into RegExp, at any depth', () => {
        const config = {
            htmlSupport: {
                allow: [
                    { name: { $regexp: '^(div|section|article)$', $flags: '' }, classes: true },
                    {
                        name: 'iframe',
                        attributes: [{ key: 'src', value: { $regexp: '^https://www\\.youtube\\.com/embed/', $flags: '' } }, 'width'],
                    },
                ],
                disallow: [{ attributes: [{ $regexp: '^on', $flags: 'i' }] }],
                allowEmpty: ['i'],
            },
        };
        const result = withHtmlSupportPatterns(config);
        const allow = (result.htmlSupport as any).allow;
        expect(allow[0].name).toEqual(/^(div|section|article)$/);
        expect(allow[0].name.test('section')).toBe(true);
        expect(allow[0].name.test('sections')).toBe(false);
        expect(allow[0].classes).toBe(true);
        expect(allow[1].attributes[0].value).toBeInstanceOf(RegExp);
        expect(allow[1].attributes[0].value.test('https://www.youtube.com/embed/x')).toBe(true);
        expect(allow[1].attributes[1]).toBe('width');
        const onPattern = (result.htmlSupport as any).disallow[0].attributes[0] as RegExp;
        expect(onPattern.flags).toBe('i');
        expect(onPattern.test('ONCLICK')).toBe(true);
        expect((result.htmlSupport as any).allowEmpty).toEqual(['i']);
    });

    it('does not modify the input', () => {
        const config = { htmlSupport: { allow: [{ name: { $regexp: 'x' } }] } };
        withHtmlSupportPatterns(config);
        expect(config.htmlSupport.allow[0].name).toEqual({ $regexp: 'x' });
    });

    it('returns the same object without htmlSupport', () => {
        const config = { toolbar: ['bold'] };
        expect(withHtmlSupportPatterns(config)).toBe(config);
    });

    it('leaves other objects alone, including record-form attributes', () => {
        const value = [{ name: 'div', attributes: { 'data-x': true, $regexp: 1 }, styles: { color: 'red' } }];
        expect(revivePatterns(value)).toEqual(value);
    });

    it('throws a descriptive error for an invalid pattern', () => {
        expect(() => revivePatterns({ $regexp: '(' })).toThrow(/Invalid htmlSupport pattern \/\(\//);
    });
});
