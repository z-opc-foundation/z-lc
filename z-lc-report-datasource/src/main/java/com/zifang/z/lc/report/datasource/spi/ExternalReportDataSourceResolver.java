package com.zifang.z.lc.report.datasource.spi;

import java.util.Map;

/**
 * 外部数据源目录桥接 SPI：报表域不直接依赖任何目录/凭据底座（z-dms / z-mist），
 * 宿主把"按 id 从外部目录取一个已建池的源"的实现挂进 Spring 容器即可。
 * <p>
 * 约定（对齐 z-lc-core 的 {@code LcDataSourceResolver} 同款分层）：
 * <ul>
 *   <li>{@link #resolve} 返回的源连接已由目录侧建好（池生命周期归目录），实现必须保证
 *       其 {@link ReportDataSource#close()} 不关外部池；</li>
 *   <li>id 不存在 / 类型不支持 → 抛 {@link IllegalArgumentException}（控制器映射为 400）；</li>
 *   <li>目录未装配 → 抛 {@link IllegalStateException}（控制器映射为 503 语义）。</li>
 * </ul>
 * options 传实现约定的非敏感调参（如 {@code max-rows}）；凭据永不经过本接口。
 */
public interface ExternalReportDataSourceResolver {

    /** 目录桥是否在位（目录服务缺席时返回 false，控制器据此报 503 语义） */
    boolean available();

    /**
     * 按目录 id 取一个已建池的报表数据源。
     *
     * @param id      目录里的数据源 id
     * @param options 非敏感调参（可为 null）
     */
    ReportDataSource resolve(String id, Map<String, String> options);
}
