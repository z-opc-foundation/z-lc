package com.zifang.z.lc.core.datasource;

import com.zifang.z.lc.common.enums.DataSourceEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据源 SQL 转换策略调度器.
 *
 * <p>蒸馏自 ace-platform-core {@code DataModelConvertSqlDispatch}
 * （{@code com.c2f.ace.core.service.data.source}），但采用 Spring 容器扫描注入 —
 * 业务方新增方言只需：1) 实现 {@link DataModelConvertSqlStrategy} + 标 {@code @Component}；
 * 2) 在策略实现类上标 {@code @ZLcDialect("xxx")} 注解，调度器自动注册.
 *
 * <p>设计哲学：避免 ace 的硬编码 switch（{@code case DATA_SOURCE_MYSQL: ...}）—
 * 新增方言无需修改 dispatcher，符合「开放封闭」原则.
 *
 * @author xuhf (distilled by zifang)
 */
@Component
public class DataModelConvertSqlDispatch {

    private static final Logger log = LogManager.getLogger(DataModelConvertSqlDispatch.class);

    /**
     * 数据源 type → 策略实现的注册表（由 {@link DataModelConvertStrategyRegistrar} 启动时填充）.
     */
    private final Map<String, DataModelConvertSqlStrategy> registry = new HashMap<>();

    /**
     * 注册一个方言策略 — 启动时被 {@link DataModelConvertStrategyRegistrar} 调用.
     *
     * @param type     方言类型字符串（如 {@code "mysql"}）
     * @param strategy 策略实现
     */
    public void register(String type, DataModelConvertSqlStrategy strategy) {
        if (type == null || type.isEmpty() || strategy == null) {
            return;
        }
        DataModelConvertSqlStrategy old = registry.put(type, strategy);
        if (old != null) {
            log.warn("Dialect strategy for '{}' overridden: {} -> {}", type,
                    old.getClass().getSimpleName(), strategy.getClass().getSimpleName());
        } else {
            log.info("Registered dialect strategy: {} -> {}", type, strategy.getClass().getSimpleName());
        }
    }

    /**
     * 按数据源类型获取对应方言策略.
     *
     * @param datasourceType 数据源 type 字符串
     * @return 对应策略实现；未注册时回退到 MySQL（兼容 ace 默认行为）；type 为 null 时返回 null
     */
    public DataModelConvertSqlStrategy getDataModelConvertSqlStrategy(String datasourceType) {
        log.info("Resolving dialect strategy for datasourceType={}", datasourceType);
        if (datasourceType == null) {
            return null;
        }

        DataSourceEnum e = DataSourceEnum.getEnumByType(datasourceType);
        if (e == null) {
            log.warn("Unknown datasource type '{}', fallback to mysql if available", datasourceType);
        }

        // 按 type 查找
        DataModelConvertSqlStrategy strategy = registry.get(datasourceType);
        if (strategy != null) {
            return strategy;
        }

        // 兜底：mysql
        if (!"mysql".equals(datasourceType)) {
            DataModelConvertSqlStrategy mysqlFallback = registry.get("mysql");
            if (mysqlFallback != null) {
                log.warn("data source type '{}' not registered, fallback to mysql strategy", datasourceType);
                return mysqlFallback;
            }
        }
        log.warn("No strategy found for datasourceType={}, including fallback", datasourceType);
        return null;
    }

    /**
     * 当前已注册的所有方言类型 — 用于管理端查询「支持哪些方言」.
     */
    public java.util.Set<String> registeredDialects() {
        return new java.util.LinkedHashSet<>(registry.keySet());
    }
}
