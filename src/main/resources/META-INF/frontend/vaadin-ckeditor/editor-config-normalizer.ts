export type EditorType = 'classic' | 'balloon' | 'inline' | 'decoupled';

export interface RootConfig {
    element?: HTMLElement;
    attachTo?: HTMLElement;
    initialData?: string;
    placeholder?: string;
    label?: string;
}

export interface NormalizedRootConfig {
    rootConfig: RootConfig;
    remainingConfig: Record<string, unknown>;
    warnings: string[];
}

const ROOT_FIELDS = ['initialData', 'placeholder', 'label'] as const;

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function hasOwn(record: Record<string, unknown>, key: string): boolean {
    return Object.prototype.hasOwnProperty.call(record, key);
}

function getOrCreateRecord(parent: Record<string, unknown>, key: string): Record<string, unknown> {
    const value = parent[key];
    if (isRecord(value)) {
        return value;
    }

    const next: Record<string, unknown> = {};
    parent[key] = next;
    return next;
}

export function normalizeRootConfig(
    config: Record<string, unknown>,
    _editorType: EditorType
): NormalizedRootConfig {
    const remainingConfig: Record<string, unknown> = { ...config };
    const rootConfig = (isRecord(config.root) ? { ...config.root } : {}) as RootConfig;
    const warnings: string[] = [];

    for (const field of ROOT_FIELDS) {
        if (!hasOwn(config, field)) {
            continue;
        }

        if (rootConfig[field] === undefined) {
            rootConfig[field] = config[field] as string;
        } else {
            warnings.push(`top-level ${field} ignored — root.${field} takes precedence`);
        }

        delete remainingConfig[field];
    }

    delete remainingConfig.root;

    return {
        rootConfig,
        remainingConfig,
        warnings,
    };
}

export function normalizeAIConfig48(
    config: Record<string, unknown>
): { config: Record<string, unknown>; warnings: string[] } {
    const next = cloneConfig(config);
    const warnings: string[] = [];

    if (!isRecord(next.ai)) {
        return { config: next, warnings };
    }

    normalizeAIChatShortcuts(next.ai, warnings);
    normalizeAIChatModels(next.ai, warnings);
    normalizeAIReviewModeTranslations(next.ai, warnings);
    normalizeAIQuickActions(next.ai, warnings);

    return { config: next, warnings };
}

// structuredClone 在配置含 function/DOM/类实例等不可克隆值时会抛 DataCloneError。
// 仅对 ai 子树做深克隆，其余字段保持引用透传，确保 normalization 不影响其它配置路径。
function cloneConfig(config: Record<string, unknown>): Record<string, unknown> {
    try {
        return structuredClone(config) as Record<string, unknown>;
    } catch {
        const next: Record<string, unknown> = { ...config };
        if (isRecord(config.ai)) {
            next.ai = clonePlainValue(config.ai);
        }
        return next;
    }
}

function clonePlainValue(value: unknown): unknown {
    if (Array.isArray(value)) {
        return value.map(clonePlainValue);
    }
    if (isRecord(value)) {
        return Object.fromEntries(
            Object.entries(value).map(([k, v]) => [k, clonePlainValue(v)])
        );
    }
    return value;
}

function normalizeAIChatShortcuts(ai: Record<string, unknown>, warnings: string[]): void {
    if (!isRecord(ai.chat) || !Array.isArray(ai.chat.shortcuts)) {
        return;
    }

    ai.chat.shortcuts.forEach((shortcut, index) => {
        if (!isRecord(shortcut) || !hasOwn(shortcut, 'check')) {
            return;
        }

        if (!hasOwn(shortcut, 'commandId')) {
            shortcut.commandId = shortcut.check;
            warnings.push(`ai.chat.shortcuts[${index}].check migrated to commandId`);
        }

        delete shortcut.check;
    });
}

function normalizeAIChatModels(ai: Record<string, unknown>, warnings: string[]): void {
    if (!isRecord(ai.chat) || !isRecord(ai.chat.models)) {
        return;
    }

    const chatModels = ai.chat.models;

    if (
        hasOwn(chatModels, 'modelSelectorAlwaysVisible')
        && !hasOwn(chatModels, 'showModelSelector')
    ) {
        chatModels.showModelSelector = chatModels.modelSelectorAlwaysVisible;
        warnings.push(
            'ai.chat.models.modelSelectorAlwaysVisible migrated to ai.models.showModelSelector'
        );
    }

    delete chatModels.modelSelectorAlwaysVisible;

    if (isRecord(ai.models)) {
        ai.models = {
            ...chatModels,
            ...ai.models,
        };
        warnings.push('ai.chat.models merged into ai.models; existing ai.models keys take precedence');
    } else {
        ai.models = { ...chatModels };
        warnings.push('ai.chat.models migrated to ai.models');
    }

    delete ai.chat.models;
}

function normalizeAIReviewModeTranslations(ai: Record<string, unknown>, warnings: string[]): void {
    if (!isRecord(ai.reviewMode) || !hasOwn(ai.reviewMode, 'translations')) {
        return;
    }

    const translate = getOrCreateRecord(ai, 'translate');

    if (!hasOwn(translate, 'languages')) {
        translate.languages = ai.reviewMode.translations;
        warnings.push('ai.reviewMode.translations migrated to ai.translate.languages');
        delete ai.reviewMode.translations;
    }
}

export interface ChannelInitialDataDeps {
    storage: Pick<Storage, 'getItem' | 'setItem'>;
    now: () => number;
    onSeeded?: (channelId: string) => void;
    onAlreadySeeded?: (channelId: string) => void;
    onStorageUnavailable?: (error: unknown) => void;
}

export interface ChannelInitialDataResult {
    config: Record<string, unknown>;
    stripped: boolean;
}

const CHANNEL_SEED_STORAGE_PREFIX = 'ck-channel-seeded:';

/**
 * 协作模式下检查频道是否已被初始化。
 *
 * 当 config 同时包含 cloudServices（协作模式）和 initialData 时，通过 storage 记录已
 * 初始化的频道：
 * - 首次加载：标记 channel 为已 seed，保留 initialData 作为种子数据。
 * - 后续加载：移除 initialData（顶层与 root.* 两种位置），避免
 *   "editor-initial-data-replaced-with-revision-data" 警告。
 *
 * 抽离自 vaadin-ckeditor.ts 以便单测；依赖通过参数注入而非直接访问 localStorage。
 */
export function stripInitialDataIfChannelSeeded(
    config: Record<string, unknown>,
    deps: ChannelInitialDataDeps
): ChannelInitialDataResult {
    const cloudServices = config.cloudServices;
    const collaboration = config.collaboration;
    const root = config.root;

    const tokenUrl = isRecord(cloudServices) ? cloudServices.tokenUrl : undefined;
    const channelId = isRecord(collaboration) ? collaboration.channelId : undefined;

    const hasTopLevelInitialData = hasOwn(config, 'initialData') && config.initialData != null;
    const hasRootInitialData = isRecord(root) && hasOwn(root, 'initialData') && root.initialData != null;
    const hasInitialData = hasTopLevelInitialData || hasRootInitialData;

    if (typeof tokenUrl !== 'string' || typeof channelId !== 'string' || !hasInitialData) {
        return { config, stripped: false };
    }

    const storageKey = `${CHANNEL_SEED_STORAGE_PREFIX}${channelId}`;

    try {
        if (deps.storage.getItem(storageKey)) {
            deps.onAlreadySeeded?.(channelId);
            return { config: removeInitialData(config), stripped: true };
        }
        deps.storage.setItem(storageKey, String(deps.now()));
        deps.onSeeded?.(channelId);
    } catch (error) {
        deps.onStorageUnavailable?.(error);
    }

    return { config, stripped: false };
}

function removeInitialData(config: Record<string, unknown>): Record<string, unknown> {
    const { initialData: _topLevel, ...rest } = config;
    const root = rest.root;
    if (isRecord(root) && hasOwn(root, 'initialData')) {
        const { initialData: _rootInitial, ...rootWithoutInitialData } = root;
        return { ...rest, root: rootWithoutInitialData };
    }
    return rest;
}

/**
 * 按 CKEditor 48 契约构造 create 配置：
 * - ClassicEditor: editorElement → 顶层 config.attachTo（v48 官方）
 * - Balloon/Inline/Decoupled: editorElement → config.root.element
 *
 * 移除 baseConfig 与 rootConfig 中可能残留的 attachTo/element，避免与新值冲突。
 */
export function buildCreateConfig(
    editorElement: HTMLElement,
    baseConfig: Record<string, unknown>,
    rootConfig: RootConfig,
    editorType: EditorType
): Record<string, unknown> {
    const config: Record<string, unknown> = applyPoweredByPosition({ ...baseConfig });
    const root: Record<string, unknown> = { ...rootConfig };

    delete config.attachTo;
    delete root.attachTo;
    delete root.element;

    if (editorType === 'classic') {
        return {
            ...config,
            attachTo: editorElement,
            root,
        };
    }

    return {
        ...config,
        root: { ...root, element: editorElement },
    };
}

/**
 * 默认把 'Powered By CKEditor' 徽标定位改为 'inside'（issue #62）。
 *
 * 徽标默认 position='border'，作为挂在 body 上的 balloon 用 JS 定位；在滚动容器内
 * CKEditor 的重定位会滞后，导致徽标停在屏幕固定位置不随内容滚动消失。'inside' 让徽标
 * 显示在编辑区边界内，随内容自然滚动，规避该问题。仅在消费端未显式配置 poweredBy.position
 * 时设置默认值，尊重用户覆盖。
 */
export function applyPoweredByPosition(config: Record<string, unknown>): Record<string, unknown> {
    const ui = isRecord(config.ui) ? { ...config.ui } : {};
    const poweredBy = isRecord(ui.poweredBy) ? { ...ui.poweredBy } : {};

    if (!hasOwn(poweredBy, 'position')) {
        poweredBy.position = 'inside';
    }

    ui.poweredBy = poweredBy;
    config.ui = ui;
    return config;
}

function normalizeAIQuickActions(ai: Record<string, unknown>, warnings: string[]): void {
    if (!isRecord(ai.quickActions) || !Array.isArray(ai.quickActions.extraCommands)) {
        return;
    }

    ai.quickActions.extraCommands.forEach((command, index) => {
        if (!isRecord(command)) {
            return;
        }

        const type = command.type;
        if (type === 'CHAT') {
            command.type = 'chat';
            warnings.push(`ai.quickActions.extraCommands[${index}].type migrated from CHAT to chat`);
        } else if (type === 'ACTION') {
            command.type = 'action';
            warnings.push(`ai.quickActions.extraCommands[${index}].type migrated from ACTION to action`);
        }

        // v48: label 取代 displayedPrompt 作为按钮文案；action 不再使用 displayedPrompt，chat 仍需保留
        if (hasOwn(command, 'displayedPrompt') && !hasOwn(command, 'label')) {
            command.label = command.displayedPrompt;
            warnings.push(
                `ai.quickActions.extraCommands[${index}].displayedPrompt migrated to label`
            );
        }

        if (command.type === 'action' && hasOwn(command, 'displayedPrompt')) {
            delete command.displayedPrompt;
        }

        if ((command.type === 'chat' || command.type === 'action') && !hasOwn(command, 'label')) {
            warnings.push(
                `ai.quickActions.extraCommands[${index}].label is required in CKEditor 48`
            );
        }

        if (command.type === 'chat' && !hasOwn(command, 'displayedPrompt')) {
            warnings.push(
                `ai.quickActions.extraCommands[${index}].displayedPrompt is required for chat commands in CKEditor 48`
            );
        }
    });
}
