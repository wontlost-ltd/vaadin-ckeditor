/**
 * error-reporter 单测：CKEditor 49 移除 Watchdog 后，运行期逃逸的编辑器错误改由 onEditorError() 观察。
 * 这里注入 subscribe 桩，验证只上报属于本实例的错误、上报字段齐全、dispose 能退订。
 */
import { describe, it, expect, vi } from 'vitest';
import { bindEditorErrors, type EditorErrorNotice, type EditorErrorSubscriber } from './error-reporter';

function stubSubscriber() {
    let callback: ((data: EditorErrorNotice) => void) | null = null;
    const unsubscribe = vi.fn();
    const subscribe: EditorErrorSubscriber = (cb) => { callback = cb; return unsubscribe; };
    return { subscribe, unsubscribe, emit: (data: EditorErrorNotice) => callback!(data) };
}

describe('bindEditorErrors', () => {
    it('reports a runtime error raised by this editor as a recoverable ERROR with code and stack', () => {
        const editor = {};
        const report = vi.fn();
        const { subscribe, emit } = stubSubscriber();
        bindEditorErrors(editor, report, subscribe);
        const error = Object.assign(new Error('model-selection-set-ranges-not-in-model'), { stack: 'stack-trace' });

        emit({ error, source: editor });

        expect(report).toHaveBeenCalledWith('EDITOR_RUNTIME_ERROR', 'model-selection-set-ranges-not-in-model', 'ERROR', true, 'stack-trace');
    });

    it('ignores errors raised by other editors or contexts', () => {
        const editor = {};
        const report = vi.fn();
        const { subscribe, emit } = stubSubscriber();
        bindEditorErrors(editor, report, subscribe);

        emit({ error: new Error('other'), source: {} });
        emit({ error: new Error('context'), source: null });

        expect(report).not.toHaveBeenCalled();
    });

    it('dispose unsubscribes from the global error reporter', () => {
        const { subscribe, unsubscribe, emit } = stubSubscriber();
        const report = vi.fn();
        const editor = {};
        const dispose = bindEditorErrors(editor, report, subscribe);

        dispose();
        expect(unsubscribe).toHaveBeenCalledTimes(1);
        expect(() => emit({ error: new Error('late'), source: editor })).not.toThrow();
    });
});
