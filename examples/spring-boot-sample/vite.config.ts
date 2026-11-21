import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated.ts';

/**
 * CKEDITOR_PREMIUM_STUB=1 时把 ckeditor5-premium-features 换成 src/main/frontend/premium-stub/
 * （按公开 API 形状仿写的 Users / Permissions / CommentsRepository / Comments / TrackChanges），
 * 供 e2e/tests/collaboration-bridge.spec.ts 在没有 CKEditor 商业 license 的情况下验证持久化桥的 JS↔Java 往返。
 * 未设置时与 Vaadin 生成的默认配置相同（真实 premium 包）。
 */
const premiumStub = process.env.CKEDITOR_PREMIUM_STUB === '1';

const customConfig: UserConfigFn = () => ({
  resolve: premiumStub
    ? {
        alias: [
          {
            find: /^ckeditor5-premium-features\/ckeditor5-premium-features\.css$/,
            replacement: new URL('./src/main/frontend/premium-stub/empty.css', import.meta.url).pathname,
          },
          {
            find: /^ckeditor5-premium-features$/,
            replacement: new URL('./src/main/frontend/premium-stub/index.ts', import.meta.url).pathname,
          },
        ],
      }
    : {},
});

export default overrideVaadinConfig(customConfig);
