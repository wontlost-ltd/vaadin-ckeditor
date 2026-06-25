/**
 * editor.model.document 的 change:data 事件处理决策（纯逻辑，便于单测）。
 *
 * 抽离自 vaadin-ckeditor.ts 的 dataChangeListener：根据当前内容、上次已知内容、
 * 同步开关以及变更来源深度，决定是否触发 contentChange 事件、是否把数据回写服务端，
 * 以及变更来源标签。把决策从 editor 实例中解耦，使其可在不实例化 CKEditor 的前提下测试。
 */

export interface DataChangeState {
    /** editor.getData() 的最新返回值 */
    newContent: string;
    /** 组件记录的上次已知内容 */
    lastKnownContent: string;
    /** 是否启用实时同步（sync=true 时内容变更即回写服务端） */
    sync: boolean;
    /**
     * API 变更深度：>0 表示当前 change:data 由服务端 updateData() 触发（而非用户输入）。
     * 用于避免把服务端回填的数据再次以 fromClient=true 回写服务端（issue #38：
     * Binder.readBean() 后 hasChanges() 误为 true）。
     */
    apiChangeDepth: number;
    /** 用户态变更来源标签（USER_INPUT / UNDO_REDO / PASTE / COLLABORATION 等） */
    changeSource: string;
}

export interface DataChangeDecision {
    /** 是否触发 fireContentChange */
    fireContentChange: boolean;
    /** fireContentChange 使用的来源标签（fireContentChange=false 时无意义） */
    contentChangeSource: string;
    /** 是否调用 $server.setEditorData() 把数据回写服务端 */
    syncToServer: boolean;
    /** contentChange 触发后，lastKnownContent 应更新为的值（null 表示不更新） */
    nextLastKnownContent: string | null;
    /** 处理完后 changeSource 是否应重置为 USER_INPUT */
    resetChangeSource: boolean;
}

/**
 * 计算一次 change:data 事件应执行的动作。
 *
 * 关键修复（issue #38）：当 apiChangeDepth > 0 时，本次变更是服务端 updateData() 引发的回填，
 * 不应再调用 setEditorData() 回写服务端——否则 Binder.readBean() 会触发一个 fromClient=true 的
 * ValueChangeEvent，导致 Binder.hasChanges() 在没有用户改动时仍返回 true。
 */
export function decideDataChange(state: DataChangeState): DataChangeDecision {
    const isApiOriginated = state.apiChangeDepth > 0;
    const contentChanged = state.newContent !== state.lastKnownContent;

    const fireContentChange = contentChanged;
    const contentChangeSource = isApiOriginated ? 'API' : state.changeSource;

    // 仅在用户态（非 API 回填）且开启同步时回写服务端，避免服务端回填的内容反向污染 Binder。
    const syncToServer = state.sync && !isApiOriginated;

    return {
        fireContentChange,
        contentChangeSource,
        syncToServer,
        nextLastKnownContent: fireContentChange ? state.newContent : null,
        resetChangeSource: fireContentChange,
    };
}
