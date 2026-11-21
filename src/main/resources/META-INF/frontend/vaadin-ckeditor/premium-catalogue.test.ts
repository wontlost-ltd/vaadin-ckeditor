/**
 * premium 插件清单与实际 npm 包一致性：Java 端列为经 ckeditor5-premium-features 加载的名称不能是
 * 开源 ckeditor5 包导出的插件——否则 CustomPlugin.fromPremium(...) 在 premium 包里找不到它（CKBox 曾被错列）。
 * 由 ckeditor5 包导出、但需要商业 license 的插件（CKEditorPlugin#requiresLicense：CKBox、CKBoxImageEdit、
 * TableLayout）必须在 PLUGIN_REGISTRY 里，并在 Java 枚举中标为 requiresLicense。
 */
import { describe, it, expect } from 'vitest';
import * as umbrella from 'ckeditor5';
import premiumSource from '../../../../java/com/wontlost/ckeditor/VaadinCKEditorPremium.java?raw';
import dependenciesSource from '../../../../java/com/wontlost/ckeditor/CKEditorPluginDependencies.java?raw';
import pluginSource from '../../../../java/com/wontlost/ckeditor/CKEditorPlugin.java?raw';
import { PLUGIN_REGISTRY } from './plugin-resolver';

const enumNames = [...premiumSource.matchAll(/^\s+[A-Z_]+\("([A-Za-z]+)"/gm)].map((m) => m[1]);
const premiumDependencyNames = [...dependenciesSource.matchAll(/premDeps\.put\("([A-Za-z]+)"/g)].map((m) => m[1]);

describe('premium 插件清单', () => {
    it('解析到 Java 端的 premium 名单', () => {
        expect(enumNames).toContain('Comments');
        expect(enumNames).toContain('Footnotes');
        expect(premiumDependencyNames).toContain('ExportPdf');
    });

    // 只看插件类：免费包里有同名的非插件导出（如 ckeditor5-ui 的 Template 类），不影响 premium 插件的加载
    it.each([...new Set([...enumNames, ...premiumDependencyNames])])('%s 不是免费 ckeditor5 导出的插件', (name) => {
        const exported = (umbrella as unknown as Record<string, { pluginName?: string } | undefined>)[name];
        expect(exported?.pluginName, `${name} is a plugin exported by the free ckeditor5 package`).toBeUndefined();
    });

    it('需要 license 的 ckeditor5 包插件在 PLUGIN_REGISTRY 中，Java 枚举标为 requiresLicense', () => {
        const licensed = pluginSource.match(/EnumSet\.of\(([^)]*)\)/)![1].split(',').map((s) => s.trim());
        expect(licensed.sort()).toEqual(['CKBOX', 'CKBOX_IMAGE_EDIT', 'TABLE_LAYOUT']);
        for (const name of ['CKBox', 'CKBoxImageEdit', 'TableLayout']) {
            expect(PLUGIN_REGISTRY[name], name).toBe((umbrella as unknown as Record<string, unknown>)[name]);
            expect(premiumDependencyNames).not.toContain(name);
            expect(enumNames).not.toContain(name);
        }
    });

    it('CKBox / CKBoxImageEdit 由 ckeditor5 包导出（不属于 premium 包清单）', () => {
        expect(PLUGIN_REGISTRY['CKBox']).toBe((umbrella as unknown as Record<string, unknown>).CKBox);
        expect(PLUGIN_REGISTRY['CKBoxImageEdit']).toBe((umbrella as unknown as Record<string, unknown>).CKBoxImageEdit);
        expect(premiumDependencyNames).not.toContain('CKBox');
    });
});
