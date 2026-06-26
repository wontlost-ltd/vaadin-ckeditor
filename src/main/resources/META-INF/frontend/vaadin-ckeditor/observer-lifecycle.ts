/**
 * MutationObserver 生命周期管理（纯逻辑，便于单测）。
 *
 * 背景（review 发现）：组件里多处用 MutationObserver，若创建时不存引用、断开时不清理，
 * 会在组件 disconnect 后泄漏（observer 持续持有回调与目标节点）。重复 setup 还会叠加多个
 * observer。本工具把"创建即替换旧的 + 统一 dispose"的生命周期收敛成可测的纯逻辑，
 * 与具体 editor/DOM 解耦。
 *
 * 用法：
 *   this.annotationObs = replaceObserver(this.annotationObs, () => new MutationObserver(cb), o => o.observe(target, opts));
 *   // disconnectedCallback:
 *   this.annotationObs = disposeObserver(this.annotationObs);
 */

/** 最小 observer 契约：只要能 disconnect 即可（MutationObserver / ResizeObserver 等均满足）。 */
export interface Disconnectable {
    disconnect(): void;
}

/**
 * 用新 observer 替换旧的：先断开既有（避免叠加/泄漏），再创建并启动新 observer。
 * @param existing 当前持有的 observer（可能为 undefined）
 * @param create   创建新 observer 的工厂
 * @param start    启动观察（如 o.observe(target, opts)）
 * @returns 新 observer，调用方应把它存回字段
 */
export function replaceObserver<T extends Disconnectable>(
    existing: T | undefined,
    create: () => T,
    start: (observer: T) => void,
): T {
    existing?.disconnect();
    const next = create();
    start(next);
    return next;
}

/**
 * 断开并清理 observer。返回 undefined，便于调用方写 `this.obs = disposeObserver(this.obs)`。
 */
export function disposeObserver<T extends Disconnectable>(existing: T | undefined): undefined {
    existing?.disconnect();
    return undefined;
}
