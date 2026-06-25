/**
 * 服务端 updateData() 在 SourceEditing 源码视图下的刷新决策（纯逻辑，便于单测）。
 *
 * 背景（issue #57）：当 SourceEditing 插件处于源码视图模式时，editor.setData() 只更新
 * 底层 model，而源码视图的 <textarea> 是进入源码模式时填充的快照，不会随之刷新——于是
 * 服务端 setValue() 后用户在源码视图里看到的仍是旧内容。
 *
 * 修复策略：若处于源码视图，setData() 之后需要把源码视图退出再进入（toggle off→on），
 * 强制 textarea 从刚更新的 model 重新填充。本模块只做"是否需要刷新源码视图"的纯判断，
 * 真正的 toggle 由调用方执行，从而把判断与 editor 实例解耦、可测。
 */

export interface SourceEditingState {
    /** 编辑器是否注册了 SourceEditing 插件 */
    hasSourceEditingPlugin: boolean;
    /** SourceEditing.isSourceEditingMode：当前是否处于源码视图 */
    isSourceEditingMode: boolean;
}

/**
 * 判断在 setData() 之后是否需要刷新源码视图。
 * 仅当插件存在且当前正处于源码视图模式时才需要刷新（toggle off→on）。
 */
export function shouldRefreshSourceView(state: SourceEditingState): boolean {
    return state.hasSourceEditingPlugin && state.isSourceEditingMode;
}
