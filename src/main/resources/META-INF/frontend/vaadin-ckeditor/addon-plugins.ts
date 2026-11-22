/**
 * add-on 自带的可选 CKEditor 插件，经自定义插件机制（plugin-resolver.ts 的
 * {@link registerCKEditorPluginLoader}）登记为**按需加载**：Java 端只有在功能启用时才把对应的
 * {@code CustomPlugin} 加入插件列表，插件模块（连同其依赖，如 highlight.js）在 Vite 构建中是独立 chunk，
 * 未启用时不会下载。
 */
import { registerCKEditorPluginLoader } from './plugin-resolver';

/** 代码块编辑时语法高亮（Java：VaadinCKEditor#setCodeBlockHighlighting）。 */
registerCKEditorPluginLoader('VaadinCodeBlockHighlighting', () => import('./code-block-highlighting'));
