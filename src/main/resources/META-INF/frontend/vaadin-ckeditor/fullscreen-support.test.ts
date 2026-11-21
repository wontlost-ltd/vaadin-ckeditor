/**
 * fullscreen-support 单测（issue #163）：BALLOON / INLINE 不加载 Fullscreen 插件、不显示 fullscreen 工具栏项，
 * CLASSIC / DECOUPLED 不变。预设 × 编辑器类型的用例直接解析 Java 端的 CKEditorPreset / CKEditorPlugin 源码，
 * 以后新增或修改预设也会被覆盖。
 */
import { describe, it, expect } from 'vitest';
import presetSource from '../../../../java/com/wontlost/ckeditor/CKEditorPreset.java?raw';
import pluginSource from '../../../../java/com/wontlost/ckeditor/CKEditorPlugin.java?raw';
import {
    supportsFullscreen,
    withoutUnsupportedFullscreenItems,
    withoutUnsupportedFullscreenPlugin,
} from './fullscreen-support';
import type { EditorType } from './editor-config-normalizer';

const TYPES: EditorType[] = ['classic', 'decoupled', 'balloon', 'inline'];
const SUPPORTED = new Set<EditorType>(['classic', 'decoupled']);

/** CKEditorPlugin 枚举名 → JS 插件名，如 FULLSCREEN → Fullscreen */
const PLUGIN_JS_NAMES = new Map(
    [...pluginSource.matchAll(/^\s{4}([A-Z0-9_]+)\("([A-Za-z0-9]+)"/gm)].map((m) => [m[1], m[2]] as const),
);

/** 去掉行注释后的预设源码：注释里可能有引号或位于两个数组之间 */
const PRESET_CODE = presetSource.replace(/\/\/[^\n]*/g, '');

/** 解析 CKEditorPreset 的每个预设：插件（JS 名）与工具栏项 */
const PRESETS = [...PRESET_CODE.matchAll(
    /^\s{4}([A-Z_]+)\(\s*"[^"]*",\s*new CKEditorPlugin\[\]\s*\{([^}]*)\},\s*new String\[\]\s*\{([^}]*)\}/gm,
)].map((m) => ({
    name: m[1],
    plugins: [...m[2].matchAll(/CKEditorPlugin\.([A-Z0-9_]+)/g)].map((p) => PLUGIN_JS_NAMES.get(p[1])!),
    toolbar: [...m[3].matchAll(/"([^"]*)"/g)].map((t) => t[1]),
}));

describe('presets × editor type', () => {
    it('parses every preset of CKEditorPreset.java', () => {
        // 与枚举常量逐一对应：新增预设也会进入下面的矩阵
        const constants = [...PRESET_CODE.matchAll(/^\s{4}([A-Z_]+)\($/gm)].map((m) => m[1]);
        expect(PRESETS.map((p) => p.name)).toEqual(constants);
        expect(constants).toContain('AI_DOCUMENT');
        expect(PRESETS.every((p) => p.plugins.every(Boolean))).toBe(true);
        // 至少一个预设带全屏，否则下面的用例形同虚设
        expect(PRESETS.some((p) => p.plugins.includes('Fullscreen') && p.toolbar.includes('fullscreen'))).toBe(true);
    });

    for (const type of TYPES) {
        it(`${type}: fullscreen ${SUPPORTED.has(type) ? 'kept' : 'removed'} for every preset`, () => {
            for (const preset of PRESETS) {
                const plugins = withoutUnsupportedFullscreenPlugin(preset.plugins.map((name) => ({ name })), type)
                    .map((p) => p.name);
                const config = withoutUnsupportedFullscreenItems({ toolbar: preset.toolbar }, type);
                if (SUPPORTED.has(type)) {
                    expect(plugins, preset.name).toEqual(preset.plugins);
                    expect(config.toolbar, preset.name).toEqual(preset.toolbar);
                } else {
                    expect(plugins, preset.name).not.toContain('Fullscreen');
                    expect(config.toolbar, preset.name).not.toContain('fullscreen');
                    // 其它项与顺序不变
                    expect(config.toolbar, preset.name).toEqual(preset.toolbar.filter((t) => t !== 'fullscreen'));
                    expect(plugins, preset.name).toEqual(preset.plugins.filter((n) => n !== 'Fullscreen'));
                }
            }
        });
    }
});

describe('withoutUnsupportedFullscreenItems', () => {
    const config = {
        toolbar: { items: ['bold', 'fullscreen', '|', 'italic'], shouldNotGroupWhenFull: true },
        balloonToolbar: ['bold', 'fullscreen'],
        blockToolbar: { items: ['heading', 'fullscreen'] },
        placeholder: 'x',
    };

    it.each(['balloon', 'inline'] as const)('%s: strips fullscreen from toolbar, balloonToolbar and blockToolbar', (type) => {
        const result = withoutUnsupportedFullscreenItems(config, type);
        expect(result).toEqual({
            toolbar: { items: ['bold', '|', 'italic'], shouldNotGroupWhenFull: true },
            balloonToolbar: ['bold'],
            blockToolbar: { items: ['heading'] },
            placeholder: 'x',
        });
        // 不修改入参
        expect(config.toolbar.items).toContain('fullscreen');
    });

    it.each(['classic', 'decoupled'] as const)('%s: returns the same config', (type) => {
        expect(withoutUnsupportedFullscreenItems(config, type)).toBe(config);
    });

    it('leaves configs without toolbars or with other toolbar shapes alone', () => {
        expect(withoutUnsupportedFullscreenItems({}, 'inline')).toEqual({});
        expect(withoutUnsupportedFullscreenItems({ toolbar: { shouldNotGroupWhenFull: true } }, 'inline'))
            .toEqual({ toolbar: { shouldNotGroupWhenFull: true } });
    });
});

describe('supportsFullscreen', () => {
    it('matches CKEditor 49.1.0: classic and decoupled only', () => {
        expect(TYPES.filter(supportsFullscreen)).toEqual(['classic', 'decoupled']);
    });
});
