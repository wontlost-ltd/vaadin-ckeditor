import { describe, it, expect, vi } from 'vitest';
import { PLUGIN_CONFIG_EVENT, type PluginConfig } from './plugin-resolver';

/**
 * 编辑器运行中插件列表变化的处理（连接器 applyPluginsChange，生产方法经原型调用）：
 * 与编辑器创建时的列表比较；只有配置变化 → editor.config.set + 触发 vaadin:pluginConfig；
 * 插件集合变化 → 重建编辑器；创建 / 销毁进行中不处理（创建结束时由创建流程再调用）。
 */
async function applyPluginsChange(plugins: PluginConfig[], applied: PluginConfig[] | null, state: Record<string, unknown> = {}) {
    const mod = await import('./vaadin-ckeditor');
    const proto = (mod as unknown as { VaadinCKEditor: { prototype: Record<string, unknown> } }).VaadinCKEditor.prototype;
    const editor = { config: { set: vi.fn() }, fire: vi.fn() };
    const ctx: Record<string, unknown> = {
        editor, plugins, appliedPlugins: applied, isCreating: false, isDestroying: false,
        rebuildEditor: vi.fn(async () => undefined), ...state,
    };
    (proto['applyPluginsChange'] as (this: unknown) => void).call(ctx);
    return { editor, ctx };
}

const base: PluginConfig = { name: 'Bold', premium: false };
const hl = (config: unknown): PluginConfig => ({
    name: 'VaadinCodeBlockHighlighting', premium: false, importPath: './vaadin-ckeditor/code-block-highlighting',
    configKey: 'codeBlockHighlighting', config,
});

describe('运行中的插件列表变化', () => {
    it('只有配置变化：写入 editor.config 并通知插件，不重建；记下已应用的列表', async () => {
        const next = [base, hl({ enabled: false, theme: 'github' })];
        const { editor, ctx } = await applyPluginsChange(next, [base, hl({ enabled: true })]);
        expect(ctx.rebuildEditor).not.toHaveBeenCalled();
        expect(editor.config.set).toHaveBeenCalledWith('codeBlockHighlighting', { enabled: false, theme: 'github' });
        expect(editor.fire).toHaveBeenCalledWith(PLUGIN_CONFIG_EVENT, 'codeBlockHighlighting', { enabled: false, theme: 'github' });
        expect(ctx.appliedPlugins).toBe(next);
    });

    it('配置键被移除：以 undefined 通知', async () => {
        const plain = { ...hl(undefined) };
        delete plain.configKey;
        delete plain.config;
        const { editor } = await applyPluginsChange([plain], [hl({ enabled: true })]);
        expect(editor.config.set).toHaveBeenCalledWith('codeBlockHighlighting', undefined);
        expect(editor.fire).toHaveBeenCalledWith(PLUGIN_CONFIG_EVENT, 'codeBlockHighlighting', undefined);
    });

    it('配置未变或是同一个列表：什么也不做', async () => {
        const same = [hl({ enabled: true })];
        expect((await applyPluginsChange([hl({ enabled: true })], [hl({ enabled: true })])).editor.fire).not.toHaveBeenCalled();
        const { editor, ctx } = await applyPluginsChange(same, same);
        expect(editor.fire).not.toHaveBeenCalled();
        expect(ctx.rebuildEditor).not.toHaveBeenCalled();
    });

    it('新增插件：重建编辑器', async () => {
        const { editor, ctx } = await applyPluginsChange([base, hl({ enabled: true })], [base]);
        expect(ctx.rebuildEditor).toHaveBeenCalledTimes(1);
        expect(editor.fire).not.toHaveBeenCalled();
    });

    it('创建或销毁进行中、或尚无编辑器：不处理（创建结束时再对齐）', async () => {
        for (const state of [{ isCreating: true }, { isDestroying: true }, { editor: null }]) {
            const { ctx } = await applyPluginsChange([base, hl({ enabled: true })], [base], state);
            expect(ctx.rebuildEditor).not.toHaveBeenCalled();
        }
    });

    it('配置键 "__proto__" 也作为普通属性收集', async () => {
        const { pluginConfigEntries } = await import('./plugin-resolver');
        const entries = pluginConfigEntries([{ name: 'X', premium: false, configKey: '__proto__', config: { a: 1 } }]);
        expect(Object.keys(entries)).toEqual(['__proto__']);
    });
});
