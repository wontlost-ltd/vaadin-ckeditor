import { describe, it, expect } from 'vitest';
import { isAllowedCssUrl } from './css-url-validator';

describe('isAllowedCssUrl (review: whitelist over substring blocklist)', () => {
    it('allows http/https absolute URLs', () => {
        expect(isAllowedCssUrl('http://example.com/theme.css')).toBe(true);
        expect(isAllowedCssUrl('https://cdn.example.com/a.css')).toBe(true);
        expect(isAllowedCssUrl('HTTPS://EXAMPLE.COM/A.CSS')).toBe(true); // scheme case-insensitive
    });

    it('allows relative paths (no scheme)', () => {
        expect(isAllowedCssUrl('theme.css')).toBe(true);
        expect(isAllowedCssUrl('./assets/theme.css')).toBe(true);
        expect(isAllowedCssUrl('/static/theme.css')).toBe(true);
        expect(isAllowedCssUrl('../up/theme.css')).toBe(true);
    });

    it('allows protocol-relative URLs', () => {
        expect(isAllowedCssUrl('//cdn.example.com/a.css')).toBe(true);
    });

    it('rejects javascript: / data: / file: / vbscript: schemes', () => {
        expect(isAllowedCssUrl('javascript:alert(1)')).toBe(false);
        expect(isAllowedCssUrl('data:text/css,body{}')).toBe(false);
        expect(isAllowedCssUrl('file:///etc/passwd')).toBe(false);
        expect(isAllowedCssUrl('vbscript:msgbox')).toBe(false);
    });

    it('rejects casing-mixed dangerous schemes (blocklist bypass)', () => {
        expect(isAllowedCssUrl('JavaScript:alert(1)')).toBe(false);
        expect(isAllowedCssUrl('DATA:text/css,x')).toBe(false);
    });

    it('rejects null/undefined/empty/whitespace', () => {
        expect(isAllowedCssUrl(null)).toBe(false);
        expect(isAllowedCssUrl(undefined)).toBe(false);
        expect(isAllowedCssUrl('')).toBe(false);
        expect(isAllowedCssUrl('   ')).toBe(false);
    });

    it('trims surrounding whitespace before judging the scheme', () => {
        expect(isAllowedCssUrl('  https://example.com/a.css  ')).toBe(true);
        expect(isAllowedCssUrl('  javascript:alert(1)')).toBe(false);
    });
});
