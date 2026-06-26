/**
 * 全局 dark 主题引用计数（纯逻辑，便于单测）。
 *
 * 背景（review 发现）：dark 主题的 CSS 变量与 <style> 是全局共享的，多个编辑器实例
 * 需要"第一个进来时应用、最后一个离开时移除"。原实现用模块级可变变量 + 散落的
 * 增减判断，难以单测且对异常/HMR 脆弱。这里把计数状态与"是否应用/是否移除"的判断
 * 收敛为一个不可变的纯计数器，逻辑可测、不依赖 DOM。
 *
 * 用法：
 *   refcount = refcount.acquire(); if (refcount.justApplied) applyGlobalStyle();
 *   refcount = refcount.release(); if (refcount.justRemoved) removeGlobalStyle();
 */

export interface RefcountResult {
    /** 当前计数 */
    readonly count: number;
    /** 本次 acquire 是否使计数从 0 升到 1（应应用全局样式） */
    readonly justApplied: boolean;
    /** 本次 release 是否使计数降到 0（应移除全局样式） */
    readonly justRemoved: boolean;
    acquire(): RefcountResult;
    release(): RefcountResult;
}

function make(count: number, justApplied: boolean, justRemoved: boolean): RefcountResult {
    return {
        count,
        justApplied,
        justRemoved,
        acquire() {
            const next = count + 1;
            return make(next, next === 1, false);
        },
        release() {
            // 不允许降到负数；只有真正归零的那次 release 才触发移除
            const next = Math.max(0, count - 1);
            return make(next, false, count > 0 && next === 0);
        },
    };
}

/** 创建初始引用计数（count=0）。 */
export function createRefcount(initial = 0): RefcountResult {
    return make(Math.max(0, initial), false, false);
}
