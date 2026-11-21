/**
 * 全屏功能与编辑器类型（issue #163）。
 *
 * CKEditor 5 49.1.0 的 FullscreenCommand 只为 ClassicEditor / DecoupledEditor 创建处理器，
 * 其它编辑器用 FullscreenAbstractEditorHandler，其 defaultOnEnter() 不移动编辑区和工具栏，
 * 但 enable() 仍把 <body> 下其它元素设为 display:none——BALLOON / INLINE 上点全屏按钮或按
 * Ctrl+Shift+F（FullscreenEditing 注册的快捷键）都会让整页变成空白。官方文档也只支持这两种类型。
 *
 * 因此对不支持的类型：不加载 Fullscreen 插件（连同快捷键一起去掉），并从所有工具栏配置里移除
 * `fullscreen` 项。与 minimap / 大纲 / AI 侧栏等「仅 DECOUPLED」功能一致，按类型静默忽略，只记调试日志。
 * 这是唯一的处理点：预设、withToolbar、CKEditorConfig.setToolbar 与 addPlugin(FULLSCREEN) 都经过这里。
 */
import type { EditorType } from './editor-config-normalizer';

export const FULLSCREEN_PLUGIN = 'Fullscreen';
export const FULLSCREEN_TOOLBAR_ITEM = 'fullscreen';

/** CKEditor 官方支持全屏的编辑器类型 */
export function supportsFullscreen(editorType: EditorType): boolean {
    return editorType === 'classic' || editorType === 'decoupled';
}

/** 不支持全屏的类型去掉 Fullscreen 插件；其余原样返回同一数组 */
export function withoutUnsupportedFullscreenPlugin<T extends { name: string }>(plugins: T[], editorType: EditorType): T[] {
    return supportsFullscreen(editorType) ? plugins : plugins.filter((p) => p.name !== FULLSCREEN_PLUGIN);
}

/** 可能列出工具栏项的配置键：主工具栏、气泡工具栏、块工具栏（数组或 { items } 对象） */
const TOOLBAR_KEYS = ['toolbar', 'balloonToolbar', 'blockToolbar'] as const;

function withoutItem(value: unknown): unknown {
    const strip = (items: unknown[]) => items.filter((item) => item !== FULLSCREEN_TOOLBAR_ITEM);
    if (Array.isArray(value)) {
        return strip(value);
    }
    if (value && typeof value === 'object' && Array.isArray((value as { items?: unknown }).items)) {
        return { ...(value as Record<string, unknown>), items: strip((value as { items: unknown[] }).items) };
    }
    return value;
}

/**
 * 不支持全屏的类型从各工具栏配置中移除 `fullscreen` 项，返回新的配置对象（不修改入参）；
 * 支持的类型原样返回。
 */
export function withoutUnsupportedFullscreenItems<C extends Record<string, unknown>>(config: C, editorType: EditorType): C {
    if (supportsFullscreen(editorType)) {
        return config;
    }
    const next: Record<string, unknown> = { ...config };
    for (const key of TOOLBAR_KEYS) {
        if (key in next) {
            next[key] = withoutItem(next[key]);
        }
    }
    return next as C;
}
