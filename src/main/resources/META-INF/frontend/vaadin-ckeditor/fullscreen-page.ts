/**
 * 全屏页面宽度（issue #156）。
 *
 * CKEditor 全屏模式把编辑区固定为 A4 纸宽 795.701px（ckeditor5-fullscreen theme/fullscreen.css），
 * 没有配置项。Java 端 `FullscreenPage` 下发 `fullscreenPage`（模式）与 `fullscreenPageWidth`（自定义宽度），
 * 本模块把它们写成属性 / CSS 变量，样式在 sticky-toolbar.css「Fullscreen page width」一节：
 *
 * - 写到宿主上：按实例区分，Dialog 内全屏容器挂在宿主里时也能继承；
 * - 进入全屏时再写到 CKEditor 的全屏容器上：light DOM 宿主下该容器挂在 document.body，继承不到宿主。
 *
 * 优先级：html 上的 `--ck-fullscreen-editable-width`（应用级覆盖）> 本实例的设置 > CKEditor 默认。
 */

export type FullscreenPageMode = 'a4-fit' | 'a4' | 'full-width' | 'custom';

export interface FullscreenPage {
    mode: FullscreenPageMode;
    /** 仅 custom 模式有值 */
    width: string | null;
}

export const FULLSCREEN_PAGE_ATTR = 'data-ck-fullscreen-page';
export const FULLSCREEN_PAGE_WIDTH_VAR = '--vaadin-ckeditor-fullscreen-page-width';
/** custom 页面宽度中与编辑列无关的下限写入此变量，编辑列以它为最小宽度，放不下时横向滚动而不压到侧栏 */
export const FULLSCREEN_PAGE_MIN_WIDTH_VAR = '--vaadin-ckeditor-fullscreen-page-min-width';
export const DEFAULT_FULLSCREEN_PAGE: FullscreenPage = { mode: 'a4-fit', width: null };

const MODES: ReadonlySet<string> = new Set(['a4-fit', 'a4', 'full-width', 'custom']);
/**
 * 与 Java 端 FullscreenPage 的校验一致：白名单字符；字母串只能是数字后的长度单位或 calc / min / max / clamp 函数名。
 * 服务端已校验，这里只做纵深防御（var、url、attr、env 等一律拒绝）。
 */
const SAFE_WIDTH = /^[0-9a-z.%+\-*/(), ]{1,100}$/;
const UNITS = new Set(['px', 'em', 'rem', 'ex', 'ch', 'cm', 'mm', 'q', 'in', 'pt', 'pc',
    'vw', 'vh', 'vmin', 'vmax', 'svw', 'lvw', 'dvw', 'cqw', 'cqi']);
const FUNCTIONS = new Set(['calc', 'min', 'max', 'clamp']);

function isSafeWidth(value: string): boolean {
    if (!SAFE_WIDTH.test(value)) return false;
    for (const m of value.matchAll(/[a-z]+/g)) {
        const start = m.index ?? 0;
        const end = start + m[0].length;
        const isFunction = value[end] === '(' && FUNCTIONS.has(m[0]);
        const isUnit = /[0-9]/.test(value[start - 1] ?? '') && UNITS.has(m[0]);
        if (!isFunction && !isUnit) return false;
    }
    return true;
}

type WidthSupports = (value: string) => boolean;

/** 没有 CSS.supports 的环境里只依赖白名单（值已通过），不因缺少 API 而回退 */
const cssSupportsWidth: WidthSupports = (value) =>
    typeof CSS === 'undefined' || typeof CSS.supports !== 'function' || CSS.supports('width', value);

/**
 * 把组件属性归一化为页面设置。未知模式、缺宽度或浏览器不认的自定义宽度回退为默认 A4_FIT。
 */
export function resolveFullscreenPage(
    mode: string | undefined,
    width: string | undefined,
    supports: WidthSupports = cssSupportsWidth,
    warn: (message: string) => void = () => {},
): FullscreenPage {
    if (!mode || !MODES.has(mode)) {
        if (mode) warn(`Unknown fullscreen page mode "${mode}", using a4-fit`);
        return DEFAULT_FULLSCREEN_PAGE;
    }
    if (mode !== 'custom') {
        return { mode: mode as FullscreenPageMode, width: null };
    }
    const value = (width ?? '').trim().toLowerCase();
    if (!isSafeWidth(value) || !supports(value)) {
        warn(`Unsupported fullscreen page width "${width}", using a4-fit`);
        return DEFAULT_FULLSCREEN_PAGE;
    }
    return { mode: 'custom', width: value };
}

/** 把页面设置写到元素上：模式属性 + 自定义宽度变量（其它模式移除变量，交给 CSS 计算）。 */
/**
 * custom 页面宽度对编辑列宽度的下限：把相对编辑列的项（百分比、cqw / cqi）替换为 0px。
 * 固定宽度原样返回（1200px → 1200px），纯相对宽度得到 0px（60% → 0px），混合表达式得到其固定部分：
 * max(50cqw, 1200px) → max(0px, 1200px)，clamp(600px, 80%, 1400px) → clamp(600px, 0px, 1400px)；
 * 含减去相对项的表达式（calc(1200px - 10%)）不设下限（0px）。
 * 页面另由 CSS 限制为不超过编辑列（max-width: min(..., 100cqw)），二者合起来保证页面既不压到侧栏，
 * 固定部分放不下时又能让全屏区域横向滚动。
 */
export function pageMinWidth(width: string): string {
    // 减去相对项（calc(1200px - 10%)）时页面随编辑列变窄，把它记为 0 会高估下限、造成不必要的横向滚动；
    // 这类表达式不设下限，由页面不超过编辑列（max-width: min(..., 100cqw)）保证不重叠
    // calc() 的二元减号两侧必须有空白；含减法且含相对项时（含嵌套，如 calc(1200px - min(10%, 20px))）一律不设下限
    const relative = /\d*\.?\d+(?:%|cq[a-z]+)/;
    if (/\s-\s/.test(width) && relative.test(width)) {
        return '0px';
    }
    return width.replace(new RegExp(relative.source, 'g'), '0px');
}

/** 把页面设置写到元素上：模式属性 + 自定义宽度变量（其它模式移除变量，交给 CSS 计算）。 */
export function applyFullscreenPage(element: HTMLElement, page: FullscreenPage): void {
    element.setAttribute(FULLSCREEN_PAGE_ATTR, page.mode);
    if (page.width) {
        element.style.setProperty(FULLSCREEN_PAGE_WIDTH_VAR, page.width);
    } else {
        element.style.removeProperty(FULLSCREEN_PAGE_WIDTH_VAR);
    }
    if (page.width) {
        element.style.setProperty(FULLSCREEN_PAGE_MIN_WIDTH_VAR, pageMinWidth(page.width));
    } else {
        element.style.removeProperty(FULLSCREEN_PAGE_MIN_WIDTH_VAR);
    }
}

type FullscreenCallback = (wrapper: HTMLElement) => void;

/**
 * 包装 `fullscreen.onEnterCallback` / `onLeaveCallback`：进入时把当前页面设置写到全屏容器并记下它，
 * 退出时清掉引用。保留调用方或 overlay-host 先行包装的回调。
 * `currentPage` 在回调时求值，因此运行期改设置后再次进入全屏会用新值。
 */
export function wrapFullscreenPageCallbacks(
    config: Record<string, unknown>,
    currentPage: () => FullscreenPage,
    onWrapper: (wrapper: HTMLElement | null) => void,
): void {
    const fullscreen = (config.fullscreen ?? {}) as Record<string, unknown>;
    const previousEnter = fullscreen.onEnterCallback as FullscreenCallback | undefined;
    const previousLeave = fullscreen.onLeaveCallback as FullscreenCallback | undefined;
    config.fullscreen = {
        ...fullscreen,
        onEnterCallback: (wrapper: HTMLElement) => {
            applyFullscreenPage(wrapper, currentPage());
            onWrapper(wrapper);
            previousEnter?.(wrapper);
        },
        onLeaveCallback: (wrapper: HTMLElement) => {
            previousLeave?.(wrapper);
            onWrapper(null);
        },
    };
}
