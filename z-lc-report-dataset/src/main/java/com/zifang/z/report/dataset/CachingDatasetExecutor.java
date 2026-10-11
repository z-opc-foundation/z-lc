package com.zifang.z.report.dataset;

import com.zifang.z.report.common.schema.ComputedField;
import com.zifang.z.report.common.schema.DatasetSchema;
import com.zifang.z.report.common.schema.JoinDef;
import com.zifang.z.report.common.schema.QueryResult;
import com.zifang.z.report.common.schema.WidgetFilter;
// 2026-10-04: 原 com.zifang.util.cache.* —— z-util/pom.xml:141「z-util-cache 已合并到
// z-util-pattern 子模块下 (2026-10-02)」, 老包随之消失, 改指合并后的新包。
import com.zifang.util.core.pattern.cache.Cache;
import com.zifang.util.core.pattern.cache.CacheBuilder;

import java.time.Duration;
import java.util.List;

/**
 * 带 TTL 的数据集结果缓存执行器 (M2): 组合模式包装 DatasetExecutor,
 * 相同 schema 定义在 TTL 窗口内直接返回上次 QueryResult, 不重复执行源读取/join/计算。
 * <p>
 * 缓存实现切换到 z-util-cache (1.0.10):
 * - 原生 LRU 淘汰 (替代旧版"超容量粗暴清空"逻辑)
 * - expireAfterWrite 精确按 Duration 控制 TTL
 * - 命中率统计 + RemovalListener 钩子
 * - schema 内容稳定串作为缓存键, 与 schema.id 解耦 (改定义立即视为新键)
 * <p>
 * 直通约定: ttlMs &lt;= 0 或 maxEntries &lt;= 0 时不缓存, 等价于裸 DatasetExecutor.
 */
public class CachingDatasetExecutor extends DatasetExecutor {

    /** 默认 TTL: 60s */
    public static final long DEFAULT_TTL_MS = 60_000L;
    /** 默认容量上限: 128 个数据集定义 */
    public static final int DEFAULT_MAX_ENTRIES = 128;

    private final long ttlMs;
    private final int maxEntries;
    private final Cache<String, QueryResult> cache;

    public CachingDatasetExecutor(TableSourceResolver sources) {
        this(sources, DEFAULT_TTL_MS, DEFAULT_MAX_ENTRIES);
    }

    public CachingDatasetExecutor(TableSourceResolver sources, long ttlMs, int maxEntries) {
        super(sources);
        this.ttlMs = Math.max(0L, ttlMs);
        this.maxEntries = Math.max(0, maxEntries);
        this.cache = (this.ttlMs <= 0 || this.maxEntries <= 0)
                ? null
                : CacheBuilder.<String, QueryResult>newBuilder()
                        .name("z-report-dataset")
                        .maximumSize(this.maxEntries)
                        .expireAfterWrite(Duration.ofMillis(this.ttlMs))
                        .build();
    }

    @Override
    public QueryResult execute(DatasetSchema schema) {
        if (cache == null) {
            return super.execute(schema);
        }
        String key = cacheKey(schema);
        return cache.get(key, k -> super.execute(schema));
    }

    /** 清空缓存 (源数据变更/测试用) */
    public void invalidateAll() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /** schema 内容稳定键 (与 id/name 解耦) */
    static String cacheKey(DatasetSchema s) {
        StringBuilder sb = new StringBuilder();
        sb.append(s.getSourceId()).append('|').append(s.getBaseTable());
        List<JoinDef> joins = s.getJoins();
        if (joins != null) {
            for (JoinDef j : joins) {
                sb.append("|J:").append(j.getSourceId()).append('.').append(j.getTable())
                        .append(':').append(j.getType())
                        .append(':').append(java.util.Arrays.toString(j.getLeftKeys()))
                        .append('>').append(java.util.Arrays.toString(j.getRightKeys()));
            }
        }
        List<WidgetFilter> filters = s.getFilters();
        if (filters != null) {
            for (WidgetFilter f : filters) {
                sb.append("|F:").append(f.getField()).append(f.getOp())
                        .append('=').append(f.getValue()).append(',').append(f.getValues());
            }
        }
        List<ComputedField> computed = s.getComputedFields();
        if (computed != null) {
            for (ComputedField c : computed) {
                sb.append("|C:").append(c.getName()).append(':').append(c.getType())
                        .append(':').append(c.getExpr());
            }
        }
        return sb.toString();
    }
}