import { describe, it, expect } from 'vitest';
import { shouldRefreshSourceView } from './source-editing-refresh';

describe('shouldRefreshSourceView (issue #57)', () => {
    it('refreshes when SourceEditing plugin is present AND in source view mode', () => {
        expect(shouldRefreshSourceView({ hasSourceEditingPlugin: true, isSourceEditingMode: true })).toBe(true);
    });

    it('does NOT refresh when SourceEditing plugin is present but NOT in source view', () => {
        expect(shouldRefreshSourceView({ hasSourceEditingPlugin: true, isSourceEditingMode: false })).toBe(false);
    });

    it('does NOT refresh when SourceEditing plugin is absent (normal editor)', () => {
        expect(shouldRefreshSourceView({ hasSourceEditingPlugin: false, isSourceEditingMode: false })).toBe(false);
    });

    it('does NOT refresh when plugin absent even if a stale mode flag is true', () => {
        expect(shouldRefreshSourceView({ hasSourceEditingPlugin: false, isSourceEditingMode: true })).toBe(false);
    });
});
