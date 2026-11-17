/**
 * 可视化设计器入口（薄壳）。
 *
 * 实现已上收到共享包 @yuku123/z-frontend-render/designer：
 * 设计器、物料库、落点解算、属性面板都在包里，z-lc 控制台与 z-opc 主壳引用同一份，
 * 避免两边各留一套「看起来差不多但拖拽行为不一样」的实现。
 *
 * 本文件只做两件事：转出组件、引入包内样式（lib 构建把 CSS 抽成了独立文件）。
 */
import '@yuku123/z-frontend-render/designer.css';

export { DesignerPage } from '@yuku123/z-frontend-render/designer';
export type { DesignerPageProps } from '@yuku123/z-frontend-render/designer';
export { useDesignerStore, resetDesigner } from '@yuku123/z-frontend-render/designer';
