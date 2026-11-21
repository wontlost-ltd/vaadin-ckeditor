import { describe, it, expect, vi } from 'vitest';
import { PLUGIN_CONFIG_EVENT, type PluginConfig } from './plugin-resolver';

/**
 * 编辑器运行中插件列表变化的处理（连接器 applyPluginsChange，生产方法经原型调用）：
 * 只有配置变化 → editor.config.set + 触发 vaadin:pluginConfig；插件集合变化 → 重建编辑器。
 */
async function applyPluginsChange(plugins: PluginConfig[], previous: PluginConfig[]) {
    const mod = await import('./vaadin-ckeditor');
    const proto = (mod as unknown as { VaadinCKEditor: { prototype: Record<string, unknown> } }).VaadinCKEditor.prototype;
    const editor = { config: { set: vi.fn() }, fire: vi.fn() };
    const ctx = { editor, plugins, rebuildEditor: vi.fn(async () => undefined) };
    (proto['applyPluginsChange'] as (this: unknown, p: PluginConfig[]) => void).call(ctx, previous);
    return { editor, ctx };
}

const hl = (enabled: boolean, theme = 'auto'): PluginConfig => ({
    name: 'VaadinCodeBlockHighlighting', premium: false, importPath: './vaadin-ckeditor/code-block-highlighting',
    configKey: 'codeBlockHighlighting', config: { enabled, theme },
});

describe('运行中的插件列表变化', () => {
    it('只有配置变化：写入 editor.config 并通知插件，不重建', async () => {
        const base = { name: 'Bold', premium: false };
        const { editor, ctx } = await applyPluginsChange([base, hl(false, 'github')], [base, hl(true)]);
        expect(ctx.rebuildEditor).not.toHaveBeenCalled();
        expect(editor.config.set).toHaveBeenCalledWith('codeBlockHighlighting', { enabled: false, theme: 'github' });
        expect(editor.fire).toHaveBeenCalledWith(PLUGIN_CONFIG_EVENT, 'codeBlockHighlighting', { enabled: false, theme: 'github' });
    });

    it('配置未变：什么也不做', async () => {
        const { editor, ctx } = await applyPluginsChange([hl(true)], [hl(true)]);
        expect(ctx.rebuildEditor).not.toHaveBeenCalled();
        expect(editor.fire).not.toHaveBeenCalled();
    });

    it('新增插件：重建编辑器', async () => {
        const base = { name: 'Bold', premium: false };
        const { editor, ctx } = await applyPluginsChange([base, hl(true)], [base]);
        expect(ctx.rebuildEditor).toHaveBeenCalledTimes(1);
        expect(editor.fire).not.toHaveBeenCalled();
    });
});
