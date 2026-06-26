/**
 * 嵌入媒体缩放插件的按需加载（issue #71）。
 *
 * CKEditor 48.2.0 的 MediaEmbedResize **由 umbrella `ckeditor5` 包导出**
 * （umbrella 通过 `export * from '@ckeditor/ckeditor5-media-embed'` 透传，type 与 runtime 两层均可解析）。
 * 但它是**功能性 premium**：其依赖 `MediaEmbedResizeEditing` 的 `isPremiumPlugin === true`，
 * 在 GPL license 下加载会触发 CKEditor 的 license 校验报错。因此不能并入免费 PLUGIN_REGISTRY，
 * 而是把"是否需要加载"的判断抽成纯函数便于单测，并仅在用户显式 setMediaEmbedResizable(true) 时
 * 才从 umbrella 动态 import——既避免无谓打包，又把 premium license 失败隔离为静默跳过。
 * 从 umbrella（而非子包）import 也消除了"静态 umbrella + 动态子包"双入口可能解析到不同类实例的风险。
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
 * 从 umbrella `ckeditor5` 包动态加载 MediaEmbedResize 插件构造器。
 * 失败时返回 null（调用方据此跳过并可记录告警），不抛出以免阻断编辑器创建——
 * 包括缺少商业 license 触发 premium 校验失败的场景，均静默降级。
 */
export async function loadMediaEmbedResizePlugin(): Promise<unknown | null> {
    try {
        const mod = await import('ckeditor5') as Record<string, unknown>;
        return mod.MediaEmbedResize ?? null;
    } catch {
        return null;
    }
}
