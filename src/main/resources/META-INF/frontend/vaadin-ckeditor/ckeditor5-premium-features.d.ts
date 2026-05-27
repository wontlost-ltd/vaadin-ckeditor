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

    // CKEditor 48 AI configuration type placeholders
    // 这些类型只覆盖项目主动消费的配置字段，未列出的字段保持运行时透传
    export type AIContextItemType = 'content' | 'selection' | 'comment' | string;
    export type AIChatShortcutType = 'command' | 'prompt' | string;
    export type AIQuickActionCommandType = 'chat' | 'action';

    export interface AIChatController {
        executePrompt?: (prompt: string) => Promise<unknown>;
        stop?: () => void;
    }

    // v48: AI Quick Actions 自定义命令配置
    // - label 取代 displayedPrompt 作为按钮文案；chat 类型仍保留 displayedPrompt 作为完整提示词
    // - action 类型不再使用 displayedPrompt
    interface AIQuickActionsBaseCommandConfig {
        id: string;
        label: string;
        prompt: string;
        commandId?: string;
        model?: string;
    }

    export interface AIQuickActionsChatCommandConfig extends AIQuickActionsBaseCommandConfig {
        type: 'chat';
        // v48: chat 命令必填 displayedPrompt（参见 CKEditor 48 update guide）
        displayedPrompt: string;
    }

    export interface AIQuickActionsActionCommandConfig extends AIQuickActionsBaseCommandConfig {
        type: 'action';
    }

    export type AIQuickActionsExtraCommandConfig =
        | AIQuickActionsChatCommandConfig
        | AIQuickActionsActionCommandConfig;

    export interface AIQuickActionsConfig {
        extraCommands?: AIQuickActionsExtraCommandConfig[];
    }

    // Allow any other exports
    const _default: Record<string, unknown>;
    export default _default;
}

/** CSS module — dynamic import resolves to void */
declare module 'ckeditor5-premium-features/ckeditor5-premium-features.css' {}
