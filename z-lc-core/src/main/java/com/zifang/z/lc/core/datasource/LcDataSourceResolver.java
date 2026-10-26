package com.zifang.z.lc.core.datasource;

import javax.sql.DataSource;
import java.util.List;

/**
 * 低代码数据源目录解析器（V1 数据源管理域的 SPI，2026-10-26 孵化自 009/013 号方案）。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>z-lc-core 只依赖本接口 —— 不直接依赖 z-dms 任何类，保持已发布件的依赖面干净；</li>
 *   <li>实现方（z-lc-datasource 模块，孵化期专用，不进 Central）把 z-dms 的
 *       {@code DataSourceService} 桥接进来：目录来自 z-dms 登记，连接来自 z-dms 建池；</li>
 *   <li>没有实现 bean 时（未装配 z-dms），调用方必须按"目录未装配"降级，不得 NPE。</li>
 * </ul>
 * <p>
 * {@code dataSourceId == null} 语义 = z-lc 自身主库（历史行为，永远保留）。
 */
public interface LcDataSourceResolver {

    /**
     * 数据源目录是否已装配（classpath 里有实现且 z-dms 服务在位）。
     */
    boolean available();

    /**
     * 列出目录里全部数据源（只含非敏感元信息：id / name / type / credentialRef）。
     * 未装配时返回空列表。
     */
    List<LcDataSourceCatalogItem> list();

    /**
     * 按目录 id 取连接池视图。
     *
     * @param id 目录 id（来自 {@link #list()}）
     * @return 非 null 的 DataSource（连接由 z-dms 池供给）
     * @throws IllegalArgumentException 目录里不存在该 id
     * @throws IllegalStateException    目录能力未装配
     */
    DataSource resolve(String id);
}
