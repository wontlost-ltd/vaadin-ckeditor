/**
 * 嵌入媒体缩放插件的按需加载（issue #71）。
 *
 * MediaEmbedResize 由开源 `ckeditor5` umbrella 包导出（49.1.0 的 `@ckeditor/ckeditor5-media-embed` 导出它；
 * 48.4/48.5 的 umbrella 产物中曾缺少该符号，`?? null` 分支保留作兼容降级）。
 * CKEditor 文档把它标为 premium feature（licence feature code `MER`）：内核对 `'GPL'` key 跳过插件 license 检查，
 * 所以它在 GPL 下可运行；商业 key 的 plan 不含它时会被拦截，商业使用需要 CKEditor 标明的 plan
 * （https://ckeditor.com/docs/ckeditor5/latest/features/media-embed/media-embed-resize.html）。
 * "是否需要加载"的判断抽成纯函数便于单测，仅在用户显式 setMediaEmbedResizable(true) 时才从 umbrella
 * 动态 import，避免无谓打包；加载失败时静默跳过。
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
