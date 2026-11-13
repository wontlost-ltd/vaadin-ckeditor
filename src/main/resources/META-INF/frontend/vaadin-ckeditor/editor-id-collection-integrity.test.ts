import { describe, it, expect } from 'vitest';
import { Collection } from '@ckeditor/ckeditor5-utils';

/**
 * editor 实例 id 与 CKEditor Collection 记账一致性的回归测试（issue #122）。
 *
 * 背景：连接器曾在 Editor.create() 完成后执行
 * `(this.editor as unknown as { id: string }).id = this.editorId;`。
 * 但 Editor 在**构造期间**就已被注册进它自己的私有 Context：
 *   Context#_addEditor -> Collection#add -> Collection#_getItemIdBeforeAdding()
 * 该方法发现新实例上没有 .id，于是生成 uid() **写回实例**，并以此为键存入 _itemMap。
 * 事后覆盖 .id，实例就与它在 _itemMap 中的键脱钩，导致：
 *   editor.destroy() -> Context#_removeEditor(editor)
 *     -> this.editors.has(editor) === false   // 按被覆盖的 id 查，查不到
 *     -> remove() 从不执行，editor 永远留在 context.editors
 *     -> 但 _contextOwner === editor 成立，于是调用 Context#destroy()
 *     -> destroy() 遍历仍含该 editor 的 editors，再次 editor.destroy()
 *     -> 无限递归，占满主线程并卡死标签页。
 *
 * 本文件从两个层面锁定该缺陷：
 *  1) 用**真实**的 @ckeditor/ckeditor5-utils Collection 证明"创建后改 id"会破坏记账，
 *     从而说明这条不变量为何必须成立（不依赖任何被测代码，属于对上游行为的钉桩）。
 *  2) 驱动**生产方法** createEditorInstance()，断言它没有在实例上留下任何
 *     破坏 Collection 记账的属性写入 —— 即便将来有人换个属性名重新引入。
 *
 * 注意：与 stale-container-isolation.test.ts 的做法一致，这里直接调用生产代码，
 * 而不是复刻一份同构逻辑；只建模上游行为的测试无法防止缺陷在生产路径上复发。
 */

/** 复刻 ckeditor5-core 的 Context#_removeEditor / Context#destroy（48.5.0 逐行对照）。 */
class ContextLike {
    editors = new Collection<Record<string, unknown>>();
    _contextOwner: unknown = null;

    _addEditor(editor: Record<string, unknown>, isContextOwner: boolean): void {
        this.editors.add(editor);
        if (isContextOwner) this._contextOwner = editor;
    }

    destroy(): Promise<unknown> {
        return Promise.all(Array.from(this.editors, (editor) =>
            (editor as unknown as { destroy: () => Promise<void> }).destroy()
        ));
    }

    _removeEditor(editor: Record<string, unknown>): Promise<unknown> {
        if (this.editors.has(editor)) this.editors.remove(editor);
        if (this._contextOwner === editor) return this.destroy();
        return Promise.resolve();
    }
}

describe('Collection 记账不变量（上游行为钉桩）', () => {
    it('实例加入 Collection 后 .id 被自动写入，且可按对象查回', () => {
        const item: Record<string, unknown> = { name: 'editor-like' };
        const collection = new Collection<Record<string, unknown>>();

        collection.add(item);

        expect(typeof item.id).toBe('string');
        expect(item.id).not.toBe('');
        expect(collection.has(item)).toBe(true);
    });

    it('加入后覆盖 .id 会让 has() 失配，remove() 因此静默失效', () => {
        const item: Record<string, unknown> = { name: 'editor-like' };
        const collection = new Collection<Record<string, unknown>>();
        collection.add(item);
        const generatedId = item.id as string;

        // 这正是 issue #122 中连接器的行为
        item.id = 'editor_deadbeef';

        expect(collection.has(item)).toBe(false);          // 失配
        expect(collection.has(generatedId)).toBe(true);    // 旧键仍在 _itemMap 中
        expect(collection.length).toBe(1);

        // Context#_removeEditor 的第一步在此条件下根本不会调用 remove()
        if (collection.has(item)) collection.remove(item);
        expect(collection.length).toBe(1);                 // 仍未被移除
        expect(Array.from(collection)).toContain(item);
    });

    it('覆盖 id 后 destroy 链进入无限递归；不覆盖则一次收敛', async () => {
        // 取一个远小于 V8 栈深度的阈值：递归一旦成立就由计数器提前中止，
        // 避免真的把栈撑爆（那会在 stderr 打出 RangeError 噪音，掩盖真实失败）。
        const LIMIT = 50;

        async function runDestroy(overwriteId: boolean): Promise<number | 'recursed'> {
            let depth = 0;
            const context = new ContextLike();
            const editor: Record<string, unknown> = {
                async destroy(): Promise<void> {
                    if (++depth > LIMIT) throw new Error('RECURSION');
                    await context._removeEditor(editor);
                },
            };
            // 私有 context：编辑器自己就是 owner（无显式 config.context 时的默认形态）
            context._addEditor(editor, true);
            if (overwriteId) editor.id = 'editor_deadbeef';

            try {
                await (editor as unknown as { destroy: () => Promise<void> }).destroy();
                return depth;
            } catch {
                return 'recursed';
            }
        }

        // 不覆盖 id：destroy() 只被调用一次即收敛
        await expect(runDestroy(false)).resolves.toBe(1);
        // 覆盖 id：递归不收敛（issue #122 的卡死根因）
        await expect(runDestroy(true)).resolves.toBe('recursed');
    });
});

describe('createEditorInstance（生产路径）不破坏 Collection 记账', () => {
    /**
     * 以最小依赖驱动生产方法 createEditorInstance()：
     * 注入一个假的 EditorConstructor，其 create() 返回的实例会像真实 Editor 那样，
     * 在"构造期间"就被注册进自己的私有 Context。随后断言生产代码没有破坏该记账。
     */
    async function runCreateEditorInstance(): Promise<{
        editor: Record<string, unknown>;
        context: ContextLike;
        generatedId: string;
    }> {
        const mod = await import('./vaadin-ckeditor');
        const ctor = (mod as Record<string, unknown>).VaadinCKEditor as {
            prototype: Record<string, unknown>;
        };
        const createEditorInstance = ctor.prototype['createEditorInstance'] as (
            this: unknown,
            el: HTMLElement
        ) => Promise<number>;
        expect(typeof createEditorInstance).toBe('function');

        const context = new ContextLike();
        let editor!: Record<string, unknown>;
        let generatedId!: string;

        const FakeEditorConstructor = {
            create(): Promise<Record<string, unknown>> {
                // 真实 Editor 在构造期间即完成注册；这里如实复刻该时序。
                editor = { state: 'ready', async destroy(): Promise<void> {} };
                context._addEditor(editor, true);
                generatedId = editor.id as string;
                return Promise.resolve(editor);
            },
        };

        const host = document.createElement('div');
        document.body.appendChild(host);

        // 生产方法在其执行路径上访问的最小上下文
        const ctx = {
            editorId: 'editor_abc12345',
            plugins: [],
            editor: null as unknown,
            isDisconnected: false,
            updateComplete: Promise.resolve(true),
            getEditorConstructor: () => FakeEditorConstructor,
            buildConfig: async () => ({}),
            buildEditorCreateConfig: () => ({}),
            // 创建后登记的退订函数（运行期错误上报等）
            listenerCleanups: [] as Array<() => void>,
            // 顶层宿主判定（overlay-host.ts）沿扁平树向上查找：不在任何 popover 内
            assignedSlot: null,
            parentElement: null,
            getRootNode: () => document,
            hasAttribute: () => false,
        };

        try {
            await createEditorInstance.call(ctx, host);
        } finally {
            host.remove();
        }

        // 前置校验：生产方法确实走到了创建成功的分支
        expect(ctx.editor).toBe(editor);
        return { editor, context, generatedId };
    }

    it('创建完成后 editor 仍能按对象在 context.editors 中查回', async () => {
        const { editor, context } = await runCreateEditorInstance();

        // 核心不变量：这一条成立，destroy 链就不会退化成无限递归
        expect(context.editors.has(editor)).toBe(true);
    });

    it('创建完成后未篡改 Collection 自动生成的 .id', async () => {
        const { editor, generatedId } = await runCreateEditorInstance();

        expect(editor.id).toBe(generatedId);
        // 明确断言没有被写成组件侧的 editorId（issue #122 的具体表现）
        expect(editor.id).not.toBe('editor_abc12345');
    });

    it('destroy 链正常收敛，不发生无限递归', async () => {
        const { editor, context } = await runCreateEditorInstance();

        let depth = 0;
        const LIMIT = 2000;
        // 换成会真正走 _removeEditor 的 destroy，模拟 CKEditor 的销毁时序
        editor.destroy = async (): Promise<void> => {
            if (++depth > LIMIT) throw new Error('RECURSION');
            await context._removeEditor(editor);
        };

        await expect(
            (editor as unknown as { destroy: () => Promise<void> }).destroy()
        ).resolves.toBeUndefined();
        expect(depth).toBe(1);
        expect(context.editors.length).toBe(0);
    });

    it('只读锁用的是组件 editorId，而非 Collection 生成的实例 id', async () => {
        // 向后兼容保障：修复只移除了对 CKEditor 实例的写入，组件自身的 editorId 契约不变。
        // 这里驱动生产方法 updateReadOnly()，断言传给 enableReadOnlyMode 的确实是组件 editorId——
        // 两个 id 空间必须保持分离，否则「移除实例 .id 写入」就会连带破坏只读锁。
        const { editor, generatedId } = await runCreateEditorInstance();

        const mod = await import('./vaadin-ckeditor');
        const ctor = (mod as Record<string, unknown>).VaadinCKEditor as {
            prototype: Record<string, unknown>;
        };
        const updateReadOnly = ctor.prototype['updateReadOnly'] as (this: unknown) => void;
        expect(typeof updateReadOnly).toBe('function');

        const locks: string[] = [];
        const ctx = {
            isReadOnly: true,
            editorId: 'editor_abc12345',
            toggleAttribute: (): boolean => true,
            classList: { toggle: (): void => undefined },
            editor: {
                enableReadOnlyMode: (id: string): number => locks.push(id),
                disableReadOnlyMode: (id: string): number => locks.push(id),
            },
        };
        updateReadOnly.call(ctx);

        expect(locks).toEqual(['editor_abc12345']);
        // 明确区分两个 id 空间：锁 id 不得是 Collection 自动生成的实例 id
        expect(locks[0]).not.toBe(generatedId);
        expect(editor.id).toBe(generatedId);
    });
});
