/**
 * premium 插件清单与实际 npm 包一致性：Java 端列为 premium（经 ckeditor5-premium-features 加载）的名称
 * 不能由免费 ckeditor5 umbrella 导出——否则 CustomPlugin.fromPremium(...) 在 premium 包里找不到它
 * （CKBox 曾被错列为 premium）。free umbrella 的插件反过来必须在 PLUGIN_REGISTRY 里。
 */
import { describe, it, expect } from 'vitest';
import * as umbrella from 'ckeditor5';
import premiumSource from '../../../../java/com/wontlost/ckeditor/VaadinCKEditorPremium.java?raw';
import dependenciesSource from '../../../../java/com/wontlost/ckeditor/CKEditorPluginDependencies.java?raw';
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

    it('CKBox / CKBoxImageEdit 由免费包导出，并登记在免费插件表中', () => {
        expect(PLUGIN_REGISTRY['CKBox']).toBe((umbrella as unknown as Record<string, unknown>).CKBox);
        expect(PLUGIN_REGISTRY['CKBoxImageEdit']).toBe((umbrella as unknown as Record<string, unknown>).CKBoxImageEdit);
        expect(premiumDependencyNames).not.toContain('CKBox');
    });
});
