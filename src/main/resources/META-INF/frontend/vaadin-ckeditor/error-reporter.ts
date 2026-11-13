/**
 * 运行期编辑器错误上报 —— CKEditor 49 移除 Watchdog 及崩溃自动重启后，
 * 逃逸出编辑器的错误改由 onEditorError() 统一观察（全局注册，回调携带 source 标识来源实例）。
 * 连接器把属于本实例的错误转成既有的 EditorErrorEvent（code EDITOR_RUNTIME_ERROR、severity ERROR、
 * recoverable=true：49 起编辑器在错误后保留内容与撤销历史，不再被销毁重建）。
 * subscribe 可注入，便于单测不依赖 CKEditor 全局状态。
 */
import { onEditorError } from 'ckeditor5';

export type EditorErrorReport = (code: string, message: string, severity: string, recoverable: boolean, stackTrace: string) => void;

/** onEditorError 回调收到的数据（只取本模块用到的字段） */
export interface EditorErrorNotice {
    error: Error;
    source: unknown;
}

/** 与 ckeditor5 的 onEditorError 同形：注册回调，返回退订函数 */
export type EditorErrorSubscriber = (callback: (data: EditorErrorNotice) => void) => () => void;

const RUNTIME_ERROR_CODE = 'EDITOR_RUNTIME_ERROR';

/** 把属于 editor 实例的运行期错误交给 report；返回退订函数，须在编辑器销毁时调用。 */
export function bindEditorErrors(
    editor: unknown,
    report: EditorErrorReport,
    subscribe: EditorErrorSubscriber = onEditorError as unknown as EditorErrorSubscriber,
): () => void {
    return subscribe(({ error, source }) => {
        if (source !== editor) {
            return;
        }
        report(RUNTIME_ERROR_CODE, error.message, 'ERROR', true, error.stack ?? '');
    });
}
