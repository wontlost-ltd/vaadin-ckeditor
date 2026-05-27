import { describe, it, expect, vi } from 'vitest';
import {
    buildCreateConfig,
    normalizeAIConfig48,
    normalizeRootConfig,
    stripInitialDataIfChannelSeeded,
    type ChannelInitialDataDeps,
    type EditorType,
} from './editor-config-normalizer';

function createMemoryStorage(seed: Record<string, string> = {}): Pick<Storage, 'getItem' | 'setItem'> {
    const data = new Map(Object.entries(seed));
    return {
        getItem: vi.fn((key: string) => data.get(key) ?? null),
        setItem: vi.fn((key: string, value: string) => { data.set(key, value); }),
    };
}

function createDeps(overrides: Partial<ChannelInitialDataDeps> = {}): ChannelInitialDataDeps {
    return {
        storage: overrides.storage ?? createMemoryStorage(),
        now: overrides.now ?? (() => 1_700_000_000_000),
        onSeeded: overrides.onSeeded ?? vi.fn(),
        onAlreadySeeded: overrides.onAlreadySeeded ?? vi.fn(),
        onStorageUnavailable: overrides.onStorageUnavailable ?? vi.fn(),
    };
}

describe('normalizeRootConfig', () => {
    it('should accept every editor type without changing current behavior', () => {
        const editorTypes: EditorType[] = ['classic', 'balloon', 'inline', 'decoupled'];

        for (const editorType of editorTypes) {
            const result = normalizeRootConfig({ initialData: '<p>Hello</p>' }, editorType);

            expect(result.rootConfig).toEqual({ initialData: '<p>Hello</p>' });
            expect(result.remainingConfig).toEqual({});
            expect(result.warnings).toEqual([]);
        }
    });

    it('should migrate top-level root fields into rootConfig', () => {
        const result = normalizeRootConfig({
            initialData: '<p>Seed</p>',
            placeholder: 'Start typing',
            label: 'Body',
            toolbar: ['bold'],
        }, 'classic');

        expect(result.rootConfig).toEqual({
            initialData: '<p>Seed</p>',
            placeholder: 'Start typing',
            label: 'Body',
        });
        expect(result.remainingConfig).toEqual({
            toolbar: ['bold'],
        });
        expect(result.warnings).toEqual([]);
    });

    it('should prefer root fields over top-level fields and warn', () => {
        const result = normalizeRootConfig({
            root: {
                initialData: '<p>Root</p>',
                placeholder: 'Root placeholder',
                label: 'Root label',
            },
            initialData: '<p>Top</p>',
            placeholder: 'Top placeholder',
            label: 'Top label',
            language: 'en',
        }, 'inline');

        expect(result.rootConfig).toEqual({
            initialData: '<p>Root</p>',
            placeholder: 'Root placeholder',
            label: 'Root label',
        });
        expect(result.remainingConfig).toEqual({
            language: 'en',
        });
        expect(result.warnings).toEqual([
            'top-level initialData ignored — root.initialData takes precedence',
            'top-level placeholder ignored — root.placeholder takes precedence',
            'top-level label ignored — root.label takes precedence',
        ]);
    });

    it('should not mutate input config or nested root object', () => {
        const config = {
            root: {
                initialData: '<p>Root</p>',
            },
            initialData: '<p>Top</p>',
            toolbar: ['bold'],
        };

        const result = normalizeRootConfig(config, 'balloon');

        result.rootConfig.initialData = '<p>Changed</p>';

        expect(config).toEqual({
            root: {
                initialData: '<p>Root</p>',
            },
            initialData: '<p>Top</p>',
            toolbar: ['bold'],
        });
    });

    it('should exclude root and migrated top-level fields from remainingConfig', () => {
        const result = normalizeRootConfig({
            root: {
                label: 'Existing label',
            },
            initialData: '<p>Seed</p>',
            placeholder: 'Placeholder',
            label: 'Top label',
            extra: true,
        }, 'decoupled');

        expect(result.remainingConfig).toEqual({
            extra: true,
        });
        expect(result.rootConfig).toEqual({
            initialData: '<p>Seed</p>',
            placeholder: 'Placeholder',
            label: 'Existing label',
        });
        expect(result.warnings).toEqual([
            'top-level label ignored — root.label takes precedence',
        ]);
    });
});

describe('normalizeAIConfig48', () => {
    it('should return a cloned config without warnings when ai is absent', () => {
        const config = {
            toolbar: ['bold'],
            nested: {
                enabled: true,
            },
        };

        const result = normalizeAIConfig48(config);

        expect(result.config).toEqual(config);
        expect(result.config).not.toBe(config);
        expect(result.config.nested).not.toBe(config.nested);
        expect(result.warnings).toEqual([]);
    });

    it('should migrate ai.chat.shortcuts[].check to commandId', () => {
        const result = normalizeAIConfig48({
            ai: {
                chat: {
                    shortcuts: [
                        { label: 'Review', check: 'reviewCommand' },
                    ],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                chat: {
                    shortcuts: [
                        { label: 'Review', commandId: 'reviewCommand' },
                    ],
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.chat.shortcuts[0].check migrated to commandId',
        ]);
    });

    it('should keep existing shortcut commandId when check is also present', () => {
        const result = normalizeAIConfig48({
            ai: {
                chat: {
                    shortcuts: [
                        { check: 'legacyCommand', commandId: 'newCommand' },
                    ],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                chat: {
                    shortcuts: [
                        { commandId: 'newCommand' },
                    ],
                },
            },
        });
        expect(result.warnings).toEqual([]);
    });

    it('should migrate modelSelectorAlwaysVisible to showModelSelector', () => {
        const result = normalizeAIConfig48({
            ai: {
                chat: {
                    models: {
                        modelSelectorAlwaysVisible: true,
                        defaultModel: 'gpt-4.1',
                    },
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                chat: {},
                models: {
                    defaultModel: 'gpt-4.1',
                    showModelSelector: true,
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.chat.models.modelSelectorAlwaysVisible migrated to ai.models.showModelSelector',
            'ai.chat.models migrated to ai.models',
        ]);
    });

    it('should merge ai.chat.models into ai.models and prefer existing ai.models keys', () => {
        const result = normalizeAIConfig48({
            ai: {
                chat: {
                    models: {
                        defaultModel: 'legacy-model',
                        showModelSelector: false,
                    },
                },
                models: {
                    defaultModel: 'current-model',
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                chat: {},
                models: {
                    defaultModel: 'current-model',
                    showModelSelector: false,
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.chat.models merged into ai.models; existing ai.models keys take precedence',
        ]);
    });

    it('should migrate reviewMode translations to translate languages', () => {
        const result = normalizeAIConfig48({
            ai: {
                reviewMode: {
                    translations: ['en', 'de'],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                reviewMode: {},
                translate: {
                    languages: ['en', 'de'],
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.reviewMode.translations migrated to ai.translate.languages',
        ]);
    });

    it('should not overwrite existing translate languages', () => {
        const result = normalizeAIConfig48({
            ai: {
                reviewMode: {
                    translations: ['en', 'de'],
                },
                translate: {
                    languages: ['fr'],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                reviewMode: {
                    translations: ['en', 'de'],
                },
                translate: {
                    languages: ['fr'],
                },
            },
        });
        expect(result.warnings).toEqual([]);
    });

    it('should lowercase quick action command types and migrate displayedPrompt to label', () => {
        const result = normalizeAIConfig48({
            ai: {
                quickActions: {
                    extraCommands: [
                        { type: 'CHAT', displayedPrompt: 'Summarize' },
                        { type: 'ACTION', displayedPrompt: 'Add quote' },
                    ],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                quickActions: {
                    extraCommands: [
                        { type: 'chat', label: 'Summarize', displayedPrompt: 'Summarize' },
                        { type: 'action', label: 'Add quote' },
                    ],
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.quickActions.extraCommands[0].type migrated from CHAT to chat',
            'ai.quickActions.extraCommands[0].displayedPrompt migrated to label',
            'ai.quickActions.extraCommands[1].type migrated from ACTION to action',
            'ai.quickActions.extraCommands[1].displayedPrompt migrated to label',
        ]);
    });

    it('should warn when chat extraCommand is missing displayedPrompt or label', () => {
        const result = normalizeAIConfig48({
            ai: {
                quickActions: {
                    extraCommands: [
                        { type: 'chat', prompt: 'Summarize selection' },
                    ],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                quickActions: {
                    extraCommands: [
                        { type: 'chat', prompt: 'Summarize selection' },
                    ],
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.quickActions.extraCommands[0].label is required in CKEditor 48',
            'ai.quickActions.extraCommands[0].displayedPrompt is required for chat commands in CKEditor 48',
        ]);
    });

    it('should drop displayedPrompt from action commands and keep existing label', () => {
        const result = normalizeAIConfig48({
            ai: {
                quickActions: {
                    extraCommands: [
                        { type: 'ACTION', label: 'Existing', displayedPrompt: 'Old prompt' },
                    ],
                },
            },
        });

        expect(result.config).toEqual({
            ai: {
                quickActions: {
                    extraCommands: [
                        { type: 'action', label: 'Existing' },
                    ],
                },
            },
        });
        expect(result.warnings).toEqual([
            'ai.quickActions.extraCommands[0].type migrated from ACTION to action',
        ]);
    });

    it('should clone via fallback when config contains non-cloneable values', () => {
        const noopFn = (): void => { /* noop */ };
        const config: Record<string, unknown> = {
            onReady: noopFn,
            ai: {
                chat: {
                    shortcuts: [{ label: 'Review', check: 'reviewCommand' }],
                },
            },
        };

        const result = normalizeAIConfig48(config);

        expect(result.config.onReady).toBe(noopFn);
        expect(result.config.ai).toEqual({
            chat: {
                shortcuts: [{ label: 'Review', commandId: 'reviewCommand' }],
            },
        });
        expect(config.ai).toEqual({
            chat: {
                shortcuts: [{ label: 'Review', check: 'reviewCommand' }],
            },
        });
    });

    it('should preserve unknown fields', () => {
        const result = normalizeAIConfig48({
            ai: {
                customFeature: {
                    enabled: true,
                },
                chat: {
                    customChatField: 'value',
                    shortcuts: [
                        { label: 'Review', check: 'reviewCommand', customShortcutField: 123 },
                    ],
                },
            },
            outsideAI: 'kept',
        });

        expect(result.config).toEqual({
            ai: {
                customFeature: {
                    enabled: true,
                },
                chat: {
                    customChatField: 'value',
                    shortcuts: [
                        {
                            label: 'Review',
                            commandId: 'reviewCommand',
                            customShortcutField: 123,
                        },
                    ],
                },
            },
            outsideAI: 'kept',
        });
    });

    it('should not mutate input config', () => {
        const config = {
            ai: {
                chat: {
                    shortcuts: [
                        { label: 'Review', check: 'reviewCommand' },
                    ],
                    models: {
                        modelSelectorAlwaysVisible: true,
                    },
                },
            },
        };

        const result = normalizeAIConfig48(config);

        expect(result.config).not.toBe(config);
        expect(config).toEqual({
            ai: {
                chat: {
                    shortcuts: [
                        { label: 'Review', check: 'reviewCommand' },
                    ],
                    models: {
                        modelSelectorAlwaysVisible: true,
                    },
                },
            },
        });
    });
});

describe('buildCreateConfig', () => {
    const editorElement = { tagName: 'DIV' } as unknown as HTMLElement;

    it('should place attachTo at top level for ClassicEditor and not inside root', () => {
        const result = buildCreateConfig(
            editorElement,
            { toolbar: ['bold'] },
            { initialData: '<p>Seed</p>' },
            'classic'
        );

        expect(result).toEqual({
            toolbar: ['bold'],
            attachTo: editorElement,
            root: { initialData: '<p>Seed</p>' },
        });
        expect((result.root as Record<string, unknown>).attachTo).toBeUndefined();
        expect((result.root as Record<string, unknown>).element).toBeUndefined();
    });

    it('should place root.element for non-classic editors', () => {
        const editorTypes: EditorType[] = ['balloon', 'inline', 'decoupled'];

        for (const editorType of editorTypes) {
            const result = buildCreateConfig(
                editorElement,
                { toolbar: ['bold'] },
                { initialData: '<p>Seed</p>' },
                editorType
            );

            expect(result).toEqual({
                toolbar: ['bold'],
                root: { initialData: '<p>Seed</p>', element: editorElement },
            });
            expect(result.attachTo).toBeUndefined();
        }
    });

    it('should override stale attachTo/element fields from baseConfig and rootConfig', () => {
        const stale = { tagName: 'STALE' } as unknown as HTMLElement;
        const result = buildCreateConfig(
            editorElement,
            { attachTo: stale, toolbar: ['bold'] },
            { initialData: '<p>Seed</p>', element: stale, attachTo: stale } as Record<string, unknown> as never,
            'classic'
        );

        expect(result.attachTo).toBe(editorElement);
        expect((result.root as Record<string, unknown>).element).toBeUndefined();
        expect((result.root as Record<string, unknown>).attachTo).toBeUndefined();
    });
});

describe('stripInitialDataIfChannelSeeded', () => {
    const collaborativeConfig = {
        cloudServices: { tokenUrl: 'https://example.com/token' },
        collaboration: { channelId: 'channel-1' },
    };

    it('should be a no-op when not in collaboration mode', () => {
        const config = {
            initialData: '<p>Seed</p>',
            cloudServices: { tokenUrl: 'https://example.com/token' },
            // collaboration missing → not collaboration mode
        };
        const deps = createDeps();

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(false);
        expect(result.config).toBe(config);
        expect(deps.onSeeded).not.toHaveBeenCalled();
        expect(deps.onAlreadySeeded).not.toHaveBeenCalled();
    });

    it('should be a no-op when neither top-level nor root.initialData is set', () => {
        const deps = createDeps();

        const result = stripInitialDataIfChannelSeeded(collaborativeConfig, deps);

        expect(result.stripped).toBe(false);
        expect(result.config).toBe(collaborativeConfig);
    });

    it('should seed storage and preserve top-level initialData on first load', () => {
        const storage = createMemoryStorage();
        const deps = createDeps({ storage, now: () => 1_700_000_000_000 });
        const config = { ...collaborativeConfig, initialData: '<p>Seed</p>' };

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(false);
        expect(result.config.initialData).toBe('<p>Seed</p>');
        expect(storage.setItem).toHaveBeenCalledWith('ck-channel-seeded:channel-1', '1700000000000');
        expect(deps.onSeeded).toHaveBeenCalledWith('channel-1');
    });

    it('should strip top-level initialData when channel is already seeded', () => {
        const storage = createMemoryStorage({ 'ck-channel-seeded:channel-1': '1234' });
        const deps = createDeps({ storage });
        const config = { ...collaborativeConfig, initialData: '<p>Seed</p>' };

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(true);
        expect(result.config).not.toHaveProperty('initialData');
        expect(deps.onAlreadySeeded).toHaveBeenCalledWith('channel-1');
    });

    it('should strip root.initialData (v48) when channel is already seeded', () => {
        const storage = createMemoryStorage({ 'ck-channel-seeded:channel-1': '1234' });
        const deps = createDeps({ storage });
        const config = {
            ...collaborativeConfig,
            root: { initialData: '<p>Root seed</p>', placeholder: 'Type here' },
        };

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(true);
        expect(result.config.root).toEqual({ placeholder: 'Type here' });
        expect(result.config).not.toHaveProperty('initialData');
    });

    it('should strip both top-level and root.initialData simultaneously when present', () => {
        const storage = createMemoryStorage({ 'ck-channel-seeded:channel-1': '1234' });
        const deps = createDeps({ storage });
        const config = {
            ...collaborativeConfig,
            initialData: '<p>Top</p>',
            root: { initialData: '<p>Root</p>', label: 'Body' },
        };

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(true);
        expect(result.config).not.toHaveProperty('initialData');
        expect(result.config.root).toEqual({ label: 'Body' });
    });

    it('should seed storage and preserve root.initialData on first load', () => {
        const storage = createMemoryStorage();
        const deps = createDeps({ storage });
        const config = {
            ...collaborativeConfig,
            root: { initialData: '<p>Root seed</p>' },
        };

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(false);
        expect((result.config.root as Record<string, unknown>).initialData).toBe('<p>Root seed</p>');
        expect(storage.setItem).toHaveBeenCalled();
    });

    it('should keep initialData when storage throws (e.g. private mode)', () => {
        const storage: Pick<Storage, 'getItem' | 'setItem'> = {
            getItem: vi.fn(() => { throw new Error('localStorage unavailable'); }),
            setItem: vi.fn(),
        };
        const deps = createDeps({ storage });
        const config = { ...collaborativeConfig, initialData: '<p>Seed</p>' };

        const result = stripInitialDataIfChannelSeeded(config, deps);

        expect(result.stripped).toBe(false);
        expect(result.config.initialData).toBe('<p>Seed</p>');
        expect(deps.onStorageUnavailable).toHaveBeenCalled();
    });

    it('should not mutate input config when stripping', () => {
        const storage = createMemoryStorage({ 'ck-channel-seeded:channel-1': '1234' });
        const deps = createDeps({ storage });
        const config = {
            ...collaborativeConfig,
            initialData: '<p>Seed</p>',
            root: { initialData: '<p>Root</p>' },
        };

        stripInitialDataIfChannelSeeded(config, deps);

        expect(config.initialData).toBe('<p>Seed</p>');
        expect(config.root.initialData).toBe('<p>Root</p>');
    });
});
