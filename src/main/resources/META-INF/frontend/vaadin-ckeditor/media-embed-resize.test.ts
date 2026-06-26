import { describe, it, expect } from 'vitest';
import { shouldLoadMediaEmbedResize } from './media-embed-resize';

describe('shouldLoadMediaEmbedResize (issue #71)', () => {
    it('returns true when mediaEmbed.resizable === true', () => {
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: { resizable: true } })).toBe(true);
    });

    it('returns true even with other mediaEmbed keys present', () => {
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: { previewsInData: true, resizable: true } })).toBe(true);
    });

    it('returns false when resizable is false', () => {
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: { resizable: false } })).toBe(false);
    });

    it('returns false when resizable is absent', () => {
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: { previewsInData: true } })).toBe(false);
    });

    it('returns false when mediaEmbed is missing', () => {
        expect(shouldLoadMediaEmbedResize({ toolbar: ['bold'] })).toBe(false);
    });

    it('returns false for null/undefined/empty config', () => {
        expect(shouldLoadMediaEmbedResize(null)).toBe(false);
        expect(shouldLoadMediaEmbedResize(undefined)).toBe(false);
        expect(shouldLoadMediaEmbedResize({})).toBe(false);
    });

    it('returns false when resizable is a truthy non-true value (strict equality)', () => {
        // 仅严格 === true 才加载，避免 'true'/1 等意外值误触发
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: { resizable: 'true' } })).toBe(false);
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: { resizable: 1 } })).toBe(false);
    });

    it('returns false when mediaEmbed is not an object', () => {
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: true })).toBe(false);
        expect(shouldLoadMediaEmbedResize({ mediaEmbed: 'x' })).toBe(false);
    });
});
