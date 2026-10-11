package com.zifang.z.report.datasource.spi;

import com.zifang.z.report.common.schema.DataSourceDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源注册中心: id → 实例 (M1 内存态, M2 落库 + 凭证加密)。
 */
public class DataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRegistry.class);

    private final Map<String, ReportDataSource> sources = new ConcurrentHashMap<>();

    /** 注册源; 同 id 覆盖时关闭被替换的源, 否则连接池会随每次重复注册泄漏一个 */
    public void register(ReportDataSource source) {
        DataSourceDef def = source.def();
        if (def == null || def.getId() == null) {
            throw new IllegalArgumentException("dataSource def/id required");
        }
        closeQuietly(sources.put(def.getId(), source));
    }

    private static void closeQuietly(ReportDataSource source) {
        if (!(source instanceof AutoCloseable)) {
            return;
        }
        try {
            ((AutoCloseable) source).close();
        } catch (Exception e) {
            log.warn("关闭被替换的数据源 [{}] 失败: {}", source.def().getId(), e.getMessage());
        }
    }

    public ReportDataSource get(String id) {
        return sources.get(id);
    }

    /** 取数据源, 不存在时抛出 (执行链路要求快速失败) */
    public ReportDataSource require(String id) {
        ReportDataSource ds = sources.get(id);
        if (ds == null) {
            throw new IllegalArgumentException("dataSource not registered: " + id);
        }
        return ds;
    }

    /** 全部已注册源 (清单端点/AI 元数据枚举用) */
    public java.util.List<ReportDataSource> all() {
        return new java.util.ArrayList<>(sources.values());
    }
}
