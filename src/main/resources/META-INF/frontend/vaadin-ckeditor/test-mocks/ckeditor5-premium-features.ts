/**
 * Mock for ckeditor5-premium-features
 * Used only for testing plugin-resolver.ts
 */

// Mock premium plugin classes
class MockPlugin {
    static pluginName = 'MockPlugin';
}

export const CKBox = class extends MockPlugin { static pluginName = 'CKBox'; };
export const CKBoxImageEdit = class extends MockPlugin { static pluginName = 'CKBoxImageEdit'; };
export const Comments = class extends MockPlugin { static pluginName = 'Comments'; };
export const ExportPdf = class extends MockPlugin { static pluginName = 'ExportPdf'; };
export const ExportWord = class extends MockPlugin { static pluginName = 'ExportWord'; };
export const ImportWord = class extends MockPlugin { static pluginName = 'ImportWord'; };
export const PasteFromOfficeEnhanced = class extends MockPlugin { static pluginName = 'PasteFromOfficeEnhanced'; };
export const RealTimeCollaborativeComments = class extends MockPlugin { static pluginName = 'RealTimeCollaborativeComments'; };
export const RealTimeCollaborativeEditing = class extends MockPlugin { static pluginName = 'RealTimeCollaborativeEditing'; };
export const RealTimeCollaborativeTrackChanges = class extends MockPlugin { static pluginName = 'RealTimeCollaborativeTrackChanges'; };
export const RealTimeCollaborativeRevisionHistory = class extends MockPlugin { static pluginName = 'RealTimeCollaborativeRevisionHistory'; };
export const RevisionHistory = class extends MockPlugin { static pluginName = 'RevisionHistory'; };
export const PresenceList = class extends MockPlugin { static pluginName = 'PresenceList'; };
export const TrackChanges = class extends MockPlugin { static pluginName = 'TrackChanges'; };
export const TrackChangesData = class extends MockPlugin { static pluginName = 'TrackChangesData'; };
export const TrackChangesPreview = class extends MockPlugin { static pluginName = 'TrackChangesPreview'; };
export const Pagination = class extends MockPlugin { static pluginName = 'Pagination'; };
export const SlashCommand = class extends MockPlugin { static pluginName = 'SlashCommand'; };
export const Template = class extends MockPlugin { static pluginName = 'Template'; };
export const FormatPainter = class extends MockPlugin { static pluginName = 'FormatPainter'; };
export const TableOfContents = class extends MockPlugin { static pluginName = 'TableOfContents'; };
export const DocumentOutline = class extends MockPlugin { static pluginName = 'DocumentOutline'; };
export const CaseChange = class extends MockPlugin { static pluginName = 'CaseChange'; };
export const MultiLevelList = class extends MockPlugin { static pluginName = 'MultiLevelList'; };
export const MergeFields = class extends MockPlugin { static pluginName = 'MergeFields'; };
