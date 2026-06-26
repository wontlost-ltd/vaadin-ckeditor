/**
 * 嵌入媒体缩放插件的按需加载（issue #71）。
 *
 * CKEditor 48.2.0 的 MediaEmbedResize 是免费 GPL 插件，但未由 umbrella `ckeditor5` 包导出，
 * 只在同版本 `@ckeditor/ckeditor5-media-embed` 子包里。这里把"是否需要加载"的判断抽成纯函数
 * 便于单测，并提供一个动态 import 的加载器——仅在用户启用 setMediaEmbedResizable(true) 时
 * 才拉取子包，避免无谓打包，也规避静态混用 umbrella+子包的风险。
 */

/**
 * 根据 config.mediaEmbed.resizable 判断是否需要加载 MediaEmbedResize 插件。
 * 仅当 mediaEmbed 为对象且 resizable === true 时返回 true。
 */
export function shouldLoadMediaEmbedResize(config: Record<string, unknown> | null | undefined): boolean {
    if (config == null) {
        return false;
    }
    const mediaEmbed = config.mediaEmbed;
    if (typeof mediaEmbed !== 'object' || mediaEmbed === null) {
        return false;
    }
    return (mediaEmbed as Record<string, unknown>).resizable === true;
}

/**
 * 从同版本子包动态加载 MediaEmbedResize 插件构造器。
 * 失败时返回 null（调用方据此跳过并可记录告警），不抛出以免阻断编辑器创建。
 */
export async function loadMediaEmbedResizePlugin(): Promise<unknown | null> {
    try {
        const mod = await import('@ckeditor/ckeditor5-media-embed') as Record<string, unknown>;
        return mod.MediaEmbedResize ?? null;
    } catch {
        return null;
    }
}
