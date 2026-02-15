/**
 * Type declarations for ckeditor5-premium-features
 * This module is optional - only needed when using premium CKEditor features
 */
declare module 'ckeditor5-premium-features' {
    // Export all plugins as unknown to support dynamic loading
    export const ExportPdf: unknown;
    export const ExportWord: unknown;
    export const ImportWord: unknown;
    export const Pagination: unknown;
    export const FormatPainter: unknown;
    export const SlashCommand: unknown;
    export const TableOfContents: unknown;
    export const DocumentOutline: unknown;
    export const Template: unknown;
    export const CaseChange: unknown;
    export const MergeFields: unknown;
    export const MultiLevelList: unknown;
    export const AIAssistant: unknown;
    export const AIChat: unknown;
    export const AIEditorIntegration: unknown;
    export const AIQuickActions: unknown;
    export const AIReviewMode: unknown;
    export const AITranslate: unknown;
    export const Comments: unknown;
    export const TrackChanges: unknown;
    export const RevisionHistory: unknown;
    export const RealTimeCollaboration: unknown;
    export const PresenceList: unknown;
    export const EmailConfigurationHelper: unknown;
    export const SourceEditingEnhanced: unknown;
    export const ExportInlineStyles: unknown;
    export const TableLayout: unknown;

    // Allow any other exports
    const _default: Record<string, unknown>;
    export default _default;
}

/** CSS module — dynamic import resolves to void */
declare module 'ckeditor5-premium-features/ckeditor5-premium-features.css' {}
