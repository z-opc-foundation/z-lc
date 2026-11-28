/**
 * z-lc 全局入口菜单（2026-11-28 CEO「全部补充回来」）。
 *
 * 为什么导出到组件包里：宿主侧栏（z-team 研发工作台侧栏、产研面「研发」组）都只做
 * 「挂载前缀 + 图标」两件事，菜单真相留在这里 —— 宿主手抄一份路径必然漂移
 * （本包改一次路由，宿主侧栏就指空）。
 *
 * 收录口径：**不依赖应用上下文**的全局入口。带 appCode 的能力（建模/数据字典/
 * 流水线/部署记录…）是「应用之内」的事，由宿主在选定应用后按上下文给，
 * 不在这里铺成半成品入口（2026-11-11 CEO 口径：很多东西应该在一个 app 之下）。
 *
 * path 相对 z-lc 根（/lc）：宿主拼前缀即可。
 */
export interface LcGlobalMenuItem {
    /** 稳定标识（宿主做菜单 key 用） */
    key: string;
    label: string;
    /** 相对 z-lc 根的路径段 */
    path: string;
    title?: string;
}

export const GLOBAL_MENU_ITEMS: LcGlobalMenuItem[] = [
    {key: 'apps', label: '应用列表', path: 'apps', title: '低代码应用清单'},
    {key: 'designer', label: 'Schema 设计', path: 'designer', title: '应用数据模型设计'},
    {key: 'db-import', label: '数据导入', path: 'db-import', title: '从既有数据库导入生成应用'},
];
