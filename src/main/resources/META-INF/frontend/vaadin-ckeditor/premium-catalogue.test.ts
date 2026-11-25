/**
 * premium 插件清单与实际 npm 包一致性：Java 端列为经 ckeditor5-premium-features 加载的名称不能是
 * 开源 ckeditor5 包导出的插件——否则 CustomPlugin.fromPremium(...) 在 premium 包里找不到它（CKBox 曾被错列）。
 * CKEditorPlugin#isPremiumFeature 必须与真实 ckeditor5 包一致：带 licenseFeatureCode 的插件（自身或其 requires 链上），
 * 加上付费服务 CKBox / CKBoxImageEdit / CKFinder（这三个没有 licenseFeatureCode）。
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

    it('isPremiumFeature 与 ckeditor5 49.1.0 的 licenseFeatureCode 一致（另加 CKBox、CKBoxImageEdit、CKFinder）', () => {
        const constants = new Map([...pluginSource.matchAll(/^\s{4}([A-Z0-9_]+)\("([A-Za-z0-9]+)"/gm)].map((m) => [m[1], m[2]]));
        const block = pluginSource.match(/PREMIUM_FEATURES = Collections\.unmodifiableSet\(EnumSet\.of\(([^;]*)\)\);/)![1];
        const flagged = new Set(block.split(',').map((s) => s.trim()).filter(Boolean));
        type Ctor = { licenseFeatureCode?: string; requires?: unknown[] };
        const hasCode = (ctor: Ctor | undefined, seen = new Set<unknown>()): boolean => {
            if (!ctor || seen.has(ctor)) return false;
            seen.add(ctor);
            if (ctor.licenseFeatureCode) return true;
            return (ctor.requires ?? []).some((r) => typeof r === 'function' && hasCode(r as Ctor, seen));
        };
        const expected = new Set(['CKBOX', 'CKBOX_IMAGE_EDIT', 'CKFINDER']);
        for (const [constant, jsName] of constants) {
            const ctor = (umbrella as unknown as Record<string, Ctor | undefined>)[jsName];
            if (hasCode(ctor)) expected.add(constant);
        }
        expect([...flagged].sort()).toEqual([...expected].sort());
        // 18 个带 licenseFeatureCode 的枚举插件（含 6.2.0 新增的 FULL_PAGE）+ CKBox、CKBoxImageEdit、CKFinder
        expect(flagged.size).toBe(21);
        for (const constant of flagged) {
            const jsName = constants.get(constant)!;
            expect(PLUGIN_REGISTRY[jsName], jsName).toBe((umbrella as unknown as Record<string, unknown>)[jsName]);
            expect(premiumDependencyNames).not.toContain(jsName);
            expect(enumNames).not.toContain(jsName);
        }
    });

    it('CKBox / CKBoxImageEdit 由 ckeditor5 包导出（不属于 premium 包清单）', () => {
        expect(PLUGIN_REGISTRY['CKBox']).toBe((umbrella as unknown as Record<string, unknown>).CKBox);
        expect(PLUGIN_REGISTRY['CKBoxImageEdit']).toBe((umbrella as unknown as Record<string, unknown>).CKBoxImageEdit);
        expect(premiumDependencyNames).not.toContain('CKBox');
    });
});
