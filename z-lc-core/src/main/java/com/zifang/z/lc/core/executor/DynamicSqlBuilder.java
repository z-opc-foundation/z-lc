package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.AggregateQueryDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.QueryConditionDTO;
import com.zifang.z.lc.common.dto.QuerySortDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
import com.zifang.z.lc.core.fieldtype.CellValueType;
import com.zifang.z.lc.core.fieldtype.FieldTypeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * DynamicSqlBuilder: 根据实体定义 + 查询/写入条件拼出可执行 SQL
 * <p>
 * 设计要点:
 * <ul>
 *   <li>列名白名单: 任何 SQL 引用只能来自 EntityDefDTO.fields, 禁止拼接用户输入</li>
 *   <li>过滤 key 后缀约定: "name" → eq, "name:like" → LIKE %x%, "name:gt" → &gt;, "name:lt" → &lt;, "name:in" → IN(...)</li>
 *   <li>filter 仅接受 EntityDefDTO.fields 中出现的字段 (白名单校验)</li>
 *   <li>INSERT 时按字段白名单 + 非 null 值写入</li>
 *   <li>结构化查询 (conjunction/conditions/sorts): fieldCode 严格白名单校验, 未知字段直接抛
 *       {@link IllegalArgumentException} (ControllerAdvice → 400), 绝不拼进 SQL;
 *       ORDER BY 只能由「白名单字段 + asc/desc」的安全片段组装</li>
 *   <li>类型语义 (jdbcType / coerce) 全部委托 {@link FieldTypeRegistry} (单一事实来源)</li>
 * </ul>
 */
@Component
public class DynamicSqlBuilder {

    private static final Logger log = LogManager.getLogger(DynamicSqlBuilder.class);

    /**
     * 结构化条件合法操作符全集 (per-type 再按 FieldTypeRegistry.operators() 二次收窄).
     */
    /** 可做数值聚合的逻辑类型. 与 FieldTypeRegistry 里 NUMBER 那一类保持一致. */
    private static final Set<String> NUMERIC_FIELD_TYPES = new java.util.HashSet<String>(
            Arrays.asList("INT", "LONG", "DECIMAL", "FLOAT", "DOUBLE"));

    private static final Set<String> STRUCTURED_OPERATORS = new LinkedHashSet<>(Arrays.asList(
            "eq", "ne", "like", "notLike", "gt", "gte", "lt", "lte", "in", "notIn", "isNull", "isNotNull"));

    /**
     * SQL 标识符白名单正则 (字段 / 表名同规则).
     */
    private static final String IDENT_REGEX = "^[A-Za-z][A-Za-z0-9_]*$";

    /**
     * 把 fieldType (STRING/INT/...) 映射为列定义片段 (用于 CREATE TABLE 或列类型推导).
     * 语义唯一来源是 {@link FieldTypeRegistry}; 此静态入口保留以兼容既有调用方, 输出逐字符不变.
     */
    public static String jdbcType(String fieldType, Integer length, Integer scale) {
        return FieldTypeRegistry.jdbcType(fieldType, length, scale);
    }

    /**
     * 构建 SELECT (含 WHERE / ORDER BY / LIMIT)
     */
    public SqlAndParams buildListSql(EntityDefDTO entity, RuntimeQueryDTO query) {
        validateTable(entity);
        List<Object> params = new ArrayList<>();
        Map<String, FieldDefDTO> fieldIndex = indexFields(entity);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        // 别名 t. 必带: 下面 buildJoinClauses 会 JOIN 进同名列 (id 等)
        sql.append(joinColumns(entity.getFields(), "t."));
        // 字段关联扩展列 (dict -> _label, refEntity -> _name)
        sql.append(buildExtraSelectColumns(entity));
        sql.append(" FROM ").append(quote(entity.getTableName())).append(" ").append("t");
        // 字段关联 JOIN 段 (dict JOIN / refEntity JOIN)
        sql.append(buildJoinClauses(entity));
        sql.append(" WHERE ").append("t").append(".deleted = 0");
        // tenant_code 主表 + JOIN 表都已 deleted=0; 仅主表加 tenant
        appendTenantParam(sql, params, entity, query);
        if (query != null && query.getFilters() != null) {
            for (Map.Entry<String, Object> e : query.getFilters().entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (val == null) {
                    continue;
                }
                String[] parts = key.split(":");
                String fieldCode = parts[0];
                String op = parts.length > 1 ? parts[1].toLowerCase() : "eq";
                FieldDefDTO fd = fieldIndex.get(fieldCode);
                if (fd == null) {
                    log.warn("Filter field not in whitelist, skip: {}", fieldCode);
                    continue;
                }
                appendLegacyFilter(sql, params, fd, op, val, true);
            }
        }
        appendStructuredConditions(sql, params, fieldIndex, query);
        appendOrderBy(sql, fieldIndex, query);

        int page = query == null || query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query == null || query.getSize() == null || query.getSize() < 1 ? 20 : Math.min(query.getSize(), 200);
        int offset = (page - 1) * size;
        sql.append(" LIMIT ").append(offset).append(",").append(size);

        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * COUNT SQL (用于分页)
     */
    public SqlAndParams buildCountSql(EntityDefDTO entity, RuntimeQueryDTO query) {
        validateTable(entity);
        List<Object> params = new ArrayList<>();
        Map<String, FieldDefDTO> fieldIndex = indexFields(entity);
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(quote(entity.getTableName())).append(" ").append("t");
        // count 也要 JOIN (否则含 LEFT JOIN 的 SQL 会漏掉行)
        sql.append(buildJoinClauses(entity));
        sql.append(" WHERE ").append("t").append(".deleted = 0");
        appendTenantParam(sql, params, entity, query);
        if (query != null && query.getFilters() != null) {
            for (Map.Entry<String, Object> e : query.getFilters().entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (val == null) {
                    continue;
                }
                String[] parts = key.split(":");
                String fieldCode = parts[0];
                String op = parts.length > 1 ? parts[1].toLowerCase() : "eq";
                FieldDefDTO fd = fieldIndex.get(fieldCode);
                if (fd == null) {
                    continue;
                }
                // 修复历史 bug: count SQL 此前不支持 gte/lte (退化为 =), 现与 list SQL 完全同源
                appendLegacyFilter(sql, params, fd, op, val, false);
            }
        }
        appendStructuredConditions(sql, params, fieldIndex, query);
        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * 分组聚合: {@code SELECT g, COUNT(*) [, SUM/AVG/MIN/MAX] FROM t <joins> WHERE ... GROUP BY g}.
     * <p>
     * 三处刻意的防护, 都对应这个引擎踩过的坑:
     * <ul>
     *   <li>分组列与聚合列**只能来自实体字段白名单**, 且必须是合法标识符 —— 绝不拼用户传入的字符串,
     *       ORDER BY 那个注入面就是这么来的;</li>
     *   <li>所有输出列都带 {@code t.} 前缀 —— 分组字段若绑了字典会 JOIN 进同名列;</li>
     *   <li>JOIN 的 dictCode 参数必须先于 tenant 参数入队 (复用 {@link #appendTenantParam}),
     *       顺序颠倒会让两者互相绑错、结果恒为空.</li>
     * </ul>
     * 分组字段绑了字典时额外取一列 {@code _label}, 前端看板/统计条直接用可读值.
     *
     * @return SQL + 参数; groupField 为空时退化成一行总计
     */
    public SqlAndParams buildAggregateSql(EntityDefDTO entity, AggregateQueryDTO query) {
        validateTable(entity);
        Map<String, FieldDefDTO> fieldIndex = indexFields(entity);
        List<Object> params = new ArrayList<Object>();
        StringBuilder sql = new StringBuilder();

        List<FieldDefDTO> groups = resolveGroupFields(entity, fieldIndex, query);
        boolean grouped = !groups.isEmpty();
        FieldDefDTO group = grouped ? groups.get(0) : null;
        TimeBucket bucket = resolveTimeBucket(query.getTimeGroup(), group);
        // 分桶表达式在 SELECT / GROUP BY / ORDER BY 各写一遍而不是引用列别名:
        // ORDER BY 引用 select 别名在部分数据库 (含 H2 的一些版本) 不可靠, 而时间图必须按时间正序。
        List<String> bucketExprs = timeBucketExpressions(bucket, group);

        sql.append("SELECT ");
        if (bucket != null) {
            String[] alias = { "bucket_year", "bucket_month", "bucket_day" };
            for (int i = 0; i < bucketExprs.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(bucketExprs.get(i)).append(" AS ").append(alias[i]);
            }
            sql.append(", ");
        } else if (grouped) {
            // 第一维沿用 group_key / group_label 的既有列名, 多出来的维度顺位补 _2 / _3。
            boolean multi = groups.size() > 1;
            for (int i = 0; i < groups.size(); i++) {
                FieldDefDTO g = groups.get(i);
                String suffix = i == 0 ? "" : "_" + (i + 1);
                String column = "t." + quote(g.getFieldCode());
                boolean dict = g.getDictCode() != null && !g.getDictCode().isEmpty();
                sql.append(column).append(" AS group_key").append(suffix);
                if (!multi) {
                    // 单维按原样: 只有字典列才有标签, 老调用方 (看板/页脚/图表) 本来就是"没标签就显示 code"
                    if (dict) {
                        sql.append(", MAX(d_").append(g.getFieldCode()).append(".item_label) AS group_label");
                    }
                } else {
                    // 多维时每一维都保证有一列非空标签: 透视程序要拿它当列名。引用一个不存在的列会让整条
                    // 请求 400; 引用一个可能为 NULL 的列 (字典项被删) 则会把那些记录折进一个叫 null 的假列。
                    sql.append(", COALESCE(")
                            .append(dict ? "MAX(d_" + g.getFieldCode() + ".item_label)" : column)
                            .append(", ").append(column).append(") AS group_label").append(suffix);
                }
                sql.append(", ");
            }
        }
        sql.append("COUNT(*) AS group_count");
        List<String[]> numeric = numericAggregations(entity, fieldIndex, query.getAggregations());
        for (String[] agg : numeric) {
            String column = "t." + quote(agg[0]);
            String expression;
            if ("DISTINCT".equals(agg[1])) {
                expression = "COUNT(DISTINCT " + column + ")";
            } else if ("FILLED".equals(agg[1])) {
                expression = "COUNT(" + column + ")";
            } else {
                expression = agg[1] + "(" + column + ")";
            }
            sql.append(", ").append(expression).append(" AS ").append(agg[1].toLowerCase()).append("_").append(agg[0]);
        }

        sql.append(" FROM ").append(quote(entity.getTableName())).append(" t");
        sql.append(buildJoinClauses(entity));
        sql.append(" WHERE t.deleted = 0");
        if (bucket != null) {
            // 时间轴只统计填了日期的记录: NULL 日期会分出一个"哪年都不是"的桶, 折线图上无处安放。
            // 这个口径要在图表副标题里说出来, 不能让它变成一个静默的数字缺口。
            sql.append(" AND t.").append(quote(group.getFieldCode())).append(" IS NOT NULL");
        }
        appendTenantParam(sql, params, entity, toQuery(query));

        RuntimeQueryDTO filterCarrier = toQuery(query);
        if (filterCarrier.getFilters() != null) {
            for (Map.Entry<String, Object> entry : filterCarrier.getFilters().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                String[] parts = key.split(":");
                FieldDefDTO fd = fieldIndex.get(parts[0]);
                if (fd == null) {
                    continue;
                }
                appendLegacyFilter(sql, params, fd, parts.length > 1 ? parts[1].toLowerCase() : "eq",
                        value, false);
            }
        }
        appendStructuredConditions(sql, params, fieldIndex, filterCarrier);

        if (bucket != null) {
            sql.append(" GROUP BY ").append(String.join(", ", bucketExprs));
            sql.append(" ORDER BY ");
            for (int i = 0; i < bucketExprs.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(bucketExprs.get(i)).append(" ASC");
            }
        } else if (grouped) {
            // group_label 走 MAX() 聚合, 所以 GROUP BY 只需要分组列本身, ONLY_FULL_GROUP_BY 下合法
            sql.append(" GROUP BY");
            for (int i = 0; i < groups.size(); i++) {
                sql.append(i == 0 ? " " : ", ").append("t.").append(quote(groups.get(i).getFieldCode()));
            }
            // 行维度先按频次排、再按行键、再按列键: 交叉表的行序必须有意义, 不能跟着数据库的哈希序走
            sql.append(" ORDER BY group_count DESC, group_key ASC");
            if (groups.size() > 1) {
                sql.append(", group_key_2 ASC");
            }
        }
        int limit = query.getLimit() == null ? 100 : Math.max(1, Math.min(query.getLimit(), 500));
        if (grouped) {
            sql.append(" LIMIT ").append(limit);
        }
        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * 生效的分组维度: {@code groupFields} 非空时整体接管 {@code groupField}, 只有一项时与单维完全等价.
     * <p>
     * 每一维都要过 {@link #resolveGroupField} 那道白名单 —— GROUP BY 的列名不能走占位符,
     * 漏校验一个维度就是开一个注入面。重复维度直接报错: {@code GROUP BY city, city} 会让交叉表
     * 多出一列永远等于行键的假维度, 看着像正常表但整张表是错的。
     */
    private List<FieldDefDTO> resolveGroupFields(EntityDefDTO entity, Map<String, FieldDefDTO> fieldIndex,
                                                 AggregateQueryDTO query) {
        List<String> codes = query.getGroupFields() == null || query.getGroupFields().isEmpty()
                ? java.util.Collections.singletonList(query.getGroupField())
                : query.getGroupFields();
        List<FieldDefDTO> resolved = new ArrayList<FieldDefDTO>();
        for (String code : codes) {
            if (code == null || code.trim().isEmpty()) {
                // 空位 = 这一维还没选 (透视表的列维度常常先空着), 与 groupField 留空同口径
                continue;
            }
            FieldDefDTO field = resolveGroupField(entity, fieldIndex, code);
            for (FieldDefDTO picked : resolved) {
                if (picked.getFieldCode().equals(field.getFieldCode())) {
                    throw new IllegalArgumentException("Duplicate groupField: " + field.getFieldCode()
                            + " (entity=" + entity.getEntityCode() + ")");
                }
            }
            resolved.add(field);
        }
        if (resolved.size() > 1 && query.getTimeGroup() != null && !query.getTimeGroup().trim().isEmpty()) {
            throw new IllegalArgumentException("timeGroup 只能作用于单个分组字段, 不能与 groupFields ("
                    + resolved.size() + " 维) 同时使用");
        }
        return resolved;
    }

    /**
     * 分组字段必须能在实体里查到; 未知字段直接报错而不是静默降级成"无分组",
     * 否则前端会看到一份看似正常其实全量的统计.
     */
    private FieldDefDTO resolveGroupField(EntityDefDTO entity, Map<String, FieldDefDTO> fieldIndex, String groupField) {
        if (groupField == null || groupField.trim().isEmpty()) {
            return null;
        }
        String code = groupField.trim();
        if (!code.matches(IDENT_REGEX)) {
            throw new IllegalArgumentException("Illegal groupField: " + groupField);
        }
        FieldDefDTO field = fieldIndex.get(code);
        if (field == null) {
            throw new IllegalArgumentException("Unknown groupField: " + code
                    + " (entity=" + entity.getEntityCode() + ")");
        }
        return field;
    }

    /** 时间分桶粒度; SELECT 列顺序固定为 年 → 月 → 日. */
    private enum TimeBucket {
        YEAR, MONTH, DAY;

        boolean includesMonth() {
            return this != YEAR;
        }

        boolean includesDay() {
            return this == DAY;
        }
    }

    /**
     * timeGroup 只在"确实按日期列分组"时成立: 缺 groupField、或分组列不是日期类型,
     * 一律抛错而不是静默退化成无分组的总量 —— 后者会让图表显示一个看着正常的假数字。
     */
    private static TimeBucket resolveTimeBucket(String raw, FieldDefDTO group) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String key = raw.trim().toUpperCase();
        if (!"DAY".equals(key) && !"MONTH".equals(key) && !"YEAR".equals(key)) {
            throw new IllegalArgumentException("Unknown timeGroup: " + raw + " (expect DAY / MONTH / YEAR)");
        }
        if (group == null) {
            throw new IllegalArgumentException("timeGroup requires a groupField to bucket on");
        }
        // 分组能力问 CellValueType, 不逐个 switch fieldType 字符串 (FieldTypeRegistry 的既定约定)
        if (FieldTypeRegistry.getDefault().handler(group.getFieldType()).cellValueType()
                != CellValueType.DATETIME) {
            throw new IllegalArgumentException("timeGroup 只支持 DATE / DATETIME 字段, 当前 groupField="
                    + group.getFieldCode() + " (type " + group.getFieldType() + ")");
        }
        return TimeBucket.valueOf(key);
    }

    /**
     * EXTRACT(<unit> FROM ...) 是 SQL 标准写法, H2 与 MySQL 都吃;
     * 刻意不用 DATE_FORMAT (MySQL 专有) / FORMATDATETIME (H2 专有), 分桶标签由前端拼。
     */
    private List<String> timeBucketExpressions(TimeBucket bucket, FieldDefDTO group) {
        if (bucket == null) {
            return Collections.emptyList();
        }
        String col = "t." + quote(group.getFieldCode());
        List<String> exprs = new ArrayList<String>();
        exprs.add("EXTRACT(YEAR FROM " + col + ")");
        if (bucket.includesMonth()) {
            exprs.add("EXTRACT(MONTH FROM " + col + ")");
        }
        if (bucket.includesDay()) {
            exprs.add("EXTRACT(DAY FROM " + col + ")");
        }
        return exprs;
    }

    /**
     * 聚合函数分两档：
     * <ul>
     *   <li>数值专属 SUM/AVG/MIN/MAX —— 落在文本/日期列上要么无意义要么误导；</li>
     *   <li>任意列都可算的 DISTINCT(去重数) / FILLED(非空数)，填充率 = FILLED / group_count，
     *       由前端算，服务端不偷偷做除法。</li>
     * </ul>
     * 列名与函数名都走白名单，绝不拼用户输入。
     */
    private static final Set<String> NUMERIC_FUNCTIONS = new java.util.HashSet<String>(
            Arrays.asList("SUM", "AVG", "MIN", "MAX"));

    private static final Set<String> UNIVERSAL_FUNCTIONS = new java.util.HashSet<String>(
            Arrays.asList("DISTINCT", "FILLED"));

    private List<String[]> numericAggregations(EntityDefDTO entity, Map<String, FieldDefDTO> fieldIndex,
                                               Map<String, List<String>> aggregations) {
        List<String[]> out = new ArrayList<String[]>();
        if (aggregations == null || aggregations.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, List<String>> entry : aggregations.entrySet()) {
            String code = entry.getKey() == null ? "" : entry.getKey().trim();
            FieldDefDTO field = fieldIndex.get(code);
            if (field == null || !code.matches(IDENT_REGEX)) {
                throw new IllegalArgumentException("Unknown aggregation field: " + code);
            }
            if (entry.getValue() == null) {
                continue;
            }
            for (String fn : entry.getValue()) {
                String upper = fn == null ? "" : fn.trim().toUpperCase();
                if (!NUMERIC_FUNCTIONS.contains(upper) && !UNIVERSAL_FUNCTIONS.contains(upper)) {
                    throw new IllegalArgumentException("Unsupported aggregation function: " + fn);
                }
                if (NUMERIC_FUNCTIONS.contains(upper) && !isNumeric(field.getFieldType())) {
                    throw new IllegalArgumentException("Field " + code + " (type " + field.getFieldType()
                            + ") does not support numeric aggregation");
                }
                out.add(new String[] {code, upper});
            }
        }
        return out;
    }

    private boolean isNumeric(String fieldType) {
        return fieldType != null && NUMERIC_FIELD_TYPES.contains(fieldType.toUpperCase());
    }

    private RuntimeQueryDTO toQuery(AggregateQueryDTO query) {
        RuntimeQueryDTO carrier = new RuntimeQueryDTO();
        carrier.setTenantCode(query.getTenantCode());
        carrier.setAppCode(query.getAppCode());
        carrier.setEntityCode(query.getEntityCode());
        carrier.setFilters(query.getFilters());
        carrier.setConjunction(query.getConjunction());
        carrier.setConditions(query.getConditions());
        return carrier;
    }

    /**
     * SELECT by id
     */
    public SqlAndParams buildGetSql(EntityDefDTO entity, Long id, String tenantCode) {
        validateTable(entity);
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(joinColumns(entity.getFields(), null));
        sql.append(" FROM ").append(quote(entity.getTableName()));
        sql.append(" WHERE id = ? AND deleted = 0");
        params.add(id);
        if (entity.getTenantCode() != null) {
            sql.append(" AND tenant_code = ?");
            params.add(tenantCode != null ? tenantCode : entity.getTenantCode());
        }
        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * INSERT — 只插入白名单内 + 非 null 字段, 强制注入 tenant_code/create_time
     */
    public SqlAndParams buildInsertSql(EntityDefDTO entity, RuntimeCrudDTO body, String currentUser) {
        validateTable(entity);
        Map<String, FieldDefDTO> fieldIndex = indexFields(entity);
        Map<String, Object> values = body.getFieldValues() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(body.getFieldValues());

        List<String> cols = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }

            FieldDefDTO fd = fieldIndex.get(e.getKey());
            if (fd == null) {
                log.warn("Insert field not in whitelist, skip: {}", e.getKey());
                continue;
            }
            // 类型转换: 根据 FieldType 强制类型
            Object coerced = coerce(e.getValue(), fd.getFieldType());
            cols.add(quote(e.getKey()));
            params.add(coerced);
        }
        // 强制字段
        if (entity.getTenantCode() != null && !fieldIndex.containsKey("tenant_code")) {
            cols.add(quote("tenant_code"));
            params.add(body.getTenantCode() != null ? body.getTenantCode() : entity.getTenantCode());
        }
        if (!cols.isEmpty() && !fieldIndex.containsKey("create_time")) {
            cols.add(quote("create_time"));
            params.add(new java.util.Date());
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(quote(entity.getTableName()));
        sql.append(" (").append(String.join(",", cols)).append(")");
        sql.append(" VALUES (");
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }

            sql.append("?");
        }
        sql.append(")");
        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * UPDATE by id
     */
    public SqlAndParams buildUpdateSql(EntityDefDTO entity, Long id, RuntimeCrudDTO body, String currentUser) {
        validateTable(entity);
        Map<String, FieldDefDTO> fieldIndex = indexFields(entity);
        Map<String, Object> values = body.getFieldValues() == null
                ? new LinkedHashMap<>() : new LinkedHashMap<>(body.getFieldValues());

        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : values.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }

            FieldDefDTO fd = fieldIndex.get(e.getKey());
            if (fd == null) {
                log.warn("Update field not in whitelist, skip: {}", e.getKey());
                continue;
            }
            Object coerced = coerce(e.getValue(), fd.getFieldType());
            sets.add(quote(e.getKey()) + " = ?");
            params.add(coerced);
        }
        // update_time
        if (!fieldIndex.containsKey("update_time")) {
            sets.add(quote("update_time") + " = ?");
            params.add(new java.util.Date());
        }
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(quote(entity.getTableName()));
        if (sets.isEmpty()) {
            throw new IllegalArgumentException("Update body has no updatable fields");
        }
        sql.append(" SET ").append(String.join(",", sets));
        sql.append(" WHERE id = ?");
        params.add(id);
        sql.append(" AND deleted = 0");
        if (entity.getTenantCode() != null) {
            sql.append(" AND tenant_code = ?");
            params.add(body.getTenantCode() != null ? body.getTenantCode() : entity.getTenantCode());
        }
        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * DELETE by id (软删)
     */
    /**
     * 软删的逆操作: 把 deleted 复位. undo/redo 恢复被删记录用.
     * 与 buildDeleteSql 同构, 保证租户隔离条件一致.
     */
    public SqlAndParams buildRestoreSql(EntityDefDTO entity, Long id, String tenantCode) {
        validateTable(entity);
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(quote(entity.getTableName()));
        sql.append(" SET deleted = 0");
        sql.append(", ").append(quote("update_time")).append(" = ?");
        params.add(new java.util.Date());
        sql.append(" WHERE id = ?");
        params.add(id);
        sql.append(" AND deleted = 1");
        if (entity.getTenantCode() != null) {
            sql.append(" AND tenant_code = ?");
            params.add(tenantCode != null ? tenantCode : entity.getTenantCode());
        }
        return new SqlAndParams(sql.toString(), params);
    }

    public SqlAndParams buildDeleteSql(EntityDefDTO entity, Long id, String tenantCode) {
        validateTable(entity);
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(quote(entity.getTableName()));
        sql.append(" SET deleted = 1");
        sql.append(", ").append(quote("update_time")).append(" = ?");
        params.add(new java.util.Date());
        sql.append(" WHERE id = ?");
        params.add(id);
        sql.append(" AND deleted = 0");
        if (entity.getTenantCode() != null) {
            sql.append(" AND tenant_code = ?");
            params.add(tenantCode != null ? tenantCode : entity.getTenantCode());
        }
        return new SqlAndParams(sql.toString(), params);
    }

    /**
     * 根据字段类型做强类型转换 (失败抛 IllegalArgumentException)
     */
    /**
     * 根据字段配置 (dictCode / refEntity) 生成 LEFT JOIN 子句.
     * 在 buildListSql / buildCountSql 的 FROM 之后, WHERE 之前注入.
     *
     * <p>关联模式:
     * <ul>
     *   <li>dict 字段 (dictCode 不为空): LEFT JOIN z_lc_dict_item 别名 ON item_code = t.field AND dict_code = '<dictCode>'</li>
     *   <li>refEntity 字段 (refEntity 不为空): LEFT JOIN <entity.tableName> 别名 ON id = t.field</li>
     * </ul>
     */
    /**
     * 追加 `t.tenant_code = ?` 条件, 并保证 JDBC 占位符与参数列表顺序一致.
     * <p>
     * buildJoinClauses 里每个 dictCode 字段都会先于 WHERE 吐出一个 `dict_code = ?` 占位符,
     * 所以这些参数必须先入队, 再入队 tenant 参数. 早先两处 (buildListSql / buildCountSql)
     * 都是先 add(tenant) 再 add(dictCode), 于是 dict_code 绑到了租户值、tenant_code 绑到了
     * 字典码 —— 含字典字段的实体分页恒为 0 条, 且租户过滤实际失效.
     */
    private static void appendTenantParam(StringBuilder sql, List<Object> params,
                                          EntityDefDTO entity, RuntimeQueryDTO query) {
        if (entity.getFields() != null) {
            for (FieldDefDTO fld : entity.getFields()) {
                if (fld != null && fld.getDictCode() != null && !fld.getDictCode().isEmpty()) {
                    params.add(fld.getDictCode());
                }
            }
        }
        if (entity.getTenantCode() != null) {
            sql.append(" AND ").append("t").append(".tenant_code = ?");
            params.add(query != null && query.getTenantCode() != null
                    ? query.getTenantCode() : entity.getTenantCode());
        }
    }

    public String buildJoinClauses(EntityDefDTO entity) {
        StringBuilder sb = new StringBuilder();
        if (entity == null || entity.getFields() == null) {
            return sb.toString();
        }
        String alias = "t";
        for (FieldDefDTO f : entity.getFields()) {
            if (f == null || f.getFieldCode() == null) {
                continue;
            }
            if (f.getDictCode() != null && !f.getDictCode().isEmpty()) {
                // 只 join 每个 (dict_code, item_code) 的最小 id 行: 字典项若有重复,
                // 直接 join 物理表会把每条业务记录 fan-out 成 N 行 (分页 total 虚高)。
                String d = "d_" + f.getFieldCode();
                sb.append(" LEFT JOIN (SELECT i.item_code, i.item_label, i.dict_code FROM z_lc_dict_item i")
                        .append(" WHERE i.deleted = 0 AND NOT EXISTS (SELECT 1 FROM z_lc_dict_item x")
                        .append(" WHERE x.deleted = 0 AND x.dict_code = i.dict_code")
                        .append(" AND x.item_code = i.item_code AND x.id < i.id)) ").append(d)
                        .append(" ON ").append(d)
                        .append(".item_code = ").append(alias).append(".")
                        .append(quote(f.getFieldCode()))
                        .append(" AND ").append(d).append(".dict_code = ?");
            } else if (f.getRefEntity() != null && !f.getRefEntity().isEmpty()) {
                sb.append(" LEFT JOIN ").append(quote(f.getRefEntity()))
                        .append(" r_").append(f.getFieldCode())
                        .append(" ON r_").append(f.getFieldCode())
                        .append(".id = ").append(alias).append(".")
                        .append(quote(f.getFieldCode()))
                        .append(" AND r_").append(f.getFieldCode())
                        .append(".deleted = 0");
            }
        }
        return sb.toString();
    }

    /**
     * 根据字段配置 (dictCode / refEntity) 生成额外 SELECT 列.
     *
     * <p>扩展列命名约定:
     * <ul>
     *   <li>dict 字段: {fieldCode}_label (item_label)</li>
     *   <li>refEntity 字段: {fieldCode}_name (entity_name)</li>
     * </ul>
     */
    public String buildExtraSelectColumns(EntityDefDTO entity) {
        StringBuilder sb = new StringBuilder();
        if (entity == null || entity.getFields() == null) {
            return sb.toString();
        }
        for (FieldDefDTO f : entity.getFields()) {
            if (f == null || f.getFieldCode() == null) {
                continue;
            }
            if (f.getDictCode() != null && !f.getDictCode().isEmpty()) {
                sb.append(",d_").append(f.getFieldCode())
                        .append(".item_label AS ")
                        .append(quote(f.getFieldCode() + "_label"));
            } else if (f.getRefEntity() != null && !f.getRefEntity().isEmpty()) {
                sb.append(",r_").append(f.getFieldCode())
                        .append(".entity_name AS ")
                        .append(quote(f.getFieldCode() + "_name"));
            }
        }
        return sb.toString();
    }

    /**
     * 根据字段类型做强类型转换 (失败抛 IllegalArgumentException).
     * 语义唯一来源是 {@link FieldTypeRegistry}; 行为与历史实现完全一致:
     * null → null; fieldType null → 原值; INT/LONG/REF → Long; DECIMAL → Double;
     * BOOLEAN → Boolean.parseBoolean 宽松语义; DATE/DATETIME → SimpleDateFormat 宽松解析;
     * TEXT/JSON/STRING/未知类型 → toString.
     */
    public Object coerce(Object raw, String fieldType) {
        return FieldTypeRegistry.coerceValue(raw, fieldType);
    }

    // ===== WHERE / ORDER BY 组装 =====

    /**
     * legacy filters (":op" 后缀) 单个片段; 未知字段由调用方先行白名单跳过 (向后兼容行为).
     *
     * @param warn 是否对未知操作符输出 warn 日志 (历史 list 路径 warn, count 路径不 warn)
     */
    private void appendLegacyFilter(StringBuilder sql, List<Object> params, FieldDefDTO fd,
                                    String op, Object val, boolean warn) {
        String col = quote(fd.getFieldCode());
        switch (op) {
            case "eq":
                sql.append(" AND ").append(col).append(" = ?");
                params.add(val);
                break;
            case "like":
                sql.append(" AND ").append(col).append(" LIKE ?");
                params.add("%" + val + "%");
                break;
            case "gt":
                sql.append(" AND ").append(col).append(" > ?");
                params.add(val);
                break;
            case "lt":
                sql.append(" AND ").append(col).append(" < ?");
                params.add(val);
                break;
            case "gte":
                sql.append(" AND ").append(col).append(" >= ?");
                params.add(val);
                break;
            case "lte":
                sql.append(" AND ").append(col).append(" <= ?");
                params.add(val);
                break;
            case "in":
                sql.append(" AND ").append(col).append(" IN (");
                if (val instanceof Collection) {
                    Collection<?> coll = (Collection<?>) val;
                    boolean first = true;
                    for (Object v : coll) {
                        if (!first) {
                            sql.append(",");
                        }

                        sql.append("?");
                        params.add(v);
                        first = false;
                    }
                    if (coll.isEmpty()) {
                        sql.append("NULL"); // 永远为空集
                    }
                } else {
                    sql.append("?");
                    params.add(val);
                }
                sql.append(")");
                break;
            default:
                if (warn) {
                    log.warn("Unknown operator '{}' for {}, fallback to eq", op, fd.getFieldCode());
                }

                sql.append(" AND ").append(col).append(" = ?");
                params.add(val);
        }
    }

    /**
     * 结构化 conditions 组装 (严格白名单): 未知字段 / 非法操作符 / 缺值 → IllegalArgumentException
     * (由 LcExceptionHandler 映射为 Result{success:false, code:400}), 绝不拼进 SQL.
     * <p>
     * conjunction=OR 时整组条件括起后与 legacy filters 片段 AND 合并; 默认 AND.
     */
    private void appendStructuredConditions(StringBuilder sql, List<Object> params,
                                            Map<String, FieldDefDTO> fieldIndex, RuntimeQueryDTO query) {
        if (query == null || query.getConditions() == null || query.getConditions().isEmpty()) {
            return;
        }

        boolean or = "OR".equalsIgnoreCase(query.getConjunction() == null ? "AND" : query.getConjunction().trim());
        StringBuilder group = new StringBuilder();
        List<Object> groupParams = new ArrayList<>();
        for (QueryConditionDTO cond : query.getConditions()) {
            if (cond == null) {
                continue;
            }

            if (group.length() > 0) {
                group.append(or ? " OR " : " AND ");
            }

            appendOneCondition(group, groupParams, fieldIndex, cond);
        }
        if (group.length() == 0) {
            return;
        }

        sql.append(" AND (").append(group).append(")");
        params.addAll(groupParams);
    }

    /**
     * 单条结构化条件 → 安全片段. fieldCode 必须: 命中白名单 + 匹配标识符正则; 操作符必须同时满足
     * 全集 (STRUCTURED_OPERATORS) 与该字段类型的 FieldTypeHandler.operators().
     */
    private void appendOneCondition(StringBuilder group, List<Object> groupParams,
                                    Map<String, FieldDefDTO> fieldIndex, QueryConditionDTO cond) {
        String fieldCode = cond.getFieldCode();
        if (fieldCode == null || !fieldCode.matches(IDENT_REGEX) || !fieldIndex.containsKey(fieldCode)) {
            throw new IllegalArgumentException("Unknown fieldCode in query conditions: " + fieldCode);
        }

        FieldDefDTO fd = fieldIndex.get(fieldCode);
        String op = cond.getOperator() == null ? "eq" : cond.getOperator().trim();
        if (!STRUCTURED_OPERATORS.contains(op) || !FieldTypeRegistry.getDefault().supportsOperator(fd.getFieldType(), op)) {
            throw new IllegalArgumentException("Illegal operator '" + op + "' for field " + fieldCode
                    + " (type " + fd.getFieldType() + ")");
        }

        String col = quote(fieldCode);
        Object val = cond.getValue();
        if ("isNull".equals(op)) {
            group.append(col).append(" IS NULL");
            return;
        }

        if ("isNotNull".equals(op)) {
            group.append(col).append(" IS NOT NULL");
            return;
        }

        if ("in".equals(op) || "notIn".equals(op)) {
            if (!(val instanceof Collection)) {
                throw new IllegalArgumentException("Operator '" + op + "' requires array value for field " + fieldCode);
            }

            Collection<?> coll = (Collection<?>) val;
            if (coll.isEmpty()) {
                // 空集合: in → 恒假; notIn → 恒真 (不产生任何用户输入片段)
                group.append("in".equals(op) ? "1 = 0" : "1 = 1");
                return;
            }

            group.append(col).append("in".equals(op) ? " IN (" : " NOT IN (");
            boolean first = true;
            for (Object v : coll) {
                if (!first) {
                    group.append(",");
                }

                group.append("?");
                groupParams.add(v);
                first = false;
            }
            group.append(")");
            return;
        }

        if (val == null) {
            throw new IllegalArgumentException("Operator '" + op + "' requires non-null value for field " + fieldCode
                    + " (use isNull/isNotNull instead)");
        }

        switch (op) {
            case "eq":
                group.append(col).append(" = ?");
                groupParams.add(val);
                break;
            case "ne":
                group.append(col).append(" <> ?");
                groupParams.add(val);
                break;
            case "like":
                group.append(col).append(" LIKE ?");
                groupParams.add("%" + val + "%");
                break;
            case "notLike":
                group.append(col).append(" NOT LIKE ?");
                groupParams.add("%" + val + "%");
                break;
            case "gt":
                group.append(col).append(" > ?");
                groupParams.add(val);
                break;
            case "gte":
                group.append(col).append(" >= ?");
                groupParams.add(val);
                break;
            case "lt":
                group.append(col).append(" < ?");
                groupParams.add(val);
                break;
            case "lte":
                group.append(col).append(" <= ?");
                groupParams.add(val);
                break;
            default:
                // 理论不可达 (STRUCTURED_OPERATORS 已过滤)
                throw new IllegalArgumentException("Unsupported operator: " + op);
        }
    }

    /**
     * ORDER BY 组装 (只允许由「白名单字段 + ASC/DESC」的安全片段构成):
     * <ol>
     *   <li>结构化 sorts 在前 — 严格: 未知字段 / 非标识符 / 非法 dir → IllegalArgumentException (400)</li>
     *   <li>legacy orderBy 在后 — 保持历史宽松行为: 未知字段静默跳过 (向后兼容), 方向非 asc/desc 按 ASC</li>
     *   <li>两者都缺省 → ORDER BY id DESC</li>
     * </ol>
     * orderBy 先按整体正则 ^[A-Za-z0-9_ ]+$ 预检, 任何带分号/括号/引号等字符的输入根本不会被拆分.
     */
    private void appendOrderBy(StringBuilder sql, Map<String, FieldDefDTO> fieldIndex, RuntimeQueryDTO query) {
        List<String> pieces = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        if (query != null && query.getSorts() != null) {
            for (QuerySortDTO s : query.getSorts()) {
                if (s == null) {
                    continue;
                }

                String f = s.getFieldCode();
                // id / create_time / update_time 是引擎给每张受管表都建的系统列, 不在 fields[] 里,
                // 但排序必须允许 (前端表头点排序默认就落在这几列上)
                boolean systemColumn = "id".equals(f) || "create_time".equals(f) || "update_time".equals(f);
                if (f == null || !f.matches(IDENT_REGEX) || (!systemColumn && !fieldIndex.containsKey(f))) {
                    throw new IllegalArgumentException("Unknown fieldCode in query sorts: " + f);
                }

                String dir = s.getDir() == null || s.getDir().trim().isEmpty()
                        ? "ASC" : s.getDir().trim().toUpperCase();
                if (!"ASC".equals(dir) && !"DESC".equals(dir)) {
                    throw new IllegalArgumentException("Illegal sort direction: " + s.getDir() + " (only asc/desc allowed)");
                }

                if (used.add(f)) {
                    pieces.add(quote(f) + " " + dir);
                }
            }
        }

        if (query != null && query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
            String raw = query.getOrderBy().trim();
            // 结构预检: 只允许「标识符 + 空白 + 可选方向」, 含 ; ( ) " ' 等任何特殊字符直接跳过 (不进 SQL)
            if (raw.matches("^[A-Za-z_][A-Za-z0-9_]*(\\s+[A-Za-z]+)?$")) {
                String[] tokens = raw.split("\\s+");
                String obField = tokens[0];
                String obDir = tokens.length > 1 ? tokens[1].toUpperCase() : "ASC";
                if (!"ASC".equals(obDir) && !"DESC".equals(obDir)) {
                    obDir = "ASC";
                }

                if (fieldIndex.containsKey(obField)) {
                    if (used.add(obField)) {
                        pieces.add(quote(obField) + " " + obDir);
                    }
                } else {
                    log.warn("orderBy field not in whitelist: {}", obField);
                }
            } else {
                log.warn("orderBy rejected by structural validation: {}", raw);
            }
        }

        if (!pieces.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", pieces));
        } else if (query == null || query.getOrderBy() == null || query.getOrderBy().isEmpty()) {
            sql.append(" ORDER BY id DESC");
        }
        // 特例保持历史行为: orderBy 给了但字段不在白名单 → 既不排序也不回退默认排序
    }

    private void validateTable(EntityDefDTO entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity is null");
        }

        if (entity.getTableName() == null || entity.getTableName().isEmpty()) {
            throw new IllegalArgumentException("entity.tableName is empty for " + entity.getEntityCode());
        }
        if (!entity.getTableName().matches("^[A-Za-z][A-Za-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid tableName: " + entity.getTableName());
        }
        if (entity.getFields() == null || entity.getFields().isEmpty()) {
            throw new IllegalArgumentException("entity.fields empty for " + entity.getEntityCode());
        }
    }

    private Map<String, FieldDefDTO> indexFields(EntityDefDTO entity) {
        Map<String, FieldDefDTO> idx = new LinkedHashMap<>();
        if (entity.getFields() == null) {
            return idx;
        }

        for (FieldDefDTO f : entity.getFields()) {
            if (f.getFieldCode() == null) {
                continue;
            }

            // 防御性白名单: 字段名只能字母数字下划线
            if (!f.getFieldCode().matches("^[A-Za-z][A-Za-z0-9_]*$")) {
                continue;
            }

            idx.put(f.getFieldCode(), f);
        }
        return idx;
    }

    private String joinColumns(List<FieldDefDTO> fields, String prefix) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (fields != null) {
            for (FieldDefDTO f : fields) {
                if (f.getFieldCode() == null) {
                    continue;
                }

                if (!first) {
                    sb.append(",");
                }

                if (prefix != null) {
                    sb.append(prefix);
                }

                sb.append(quote(f.getFieldCode()));
                first = false;
            }
        }
        if (!first) {
            sb.append(",");
        }

        // 系统列同样要带表别名前缀: list SQL 会 LEFT JOIN z_lc_dict_item / 关联表,
        // 它们都有自己的 id, 裸 `id` 在 H2/PG 上直接 "Ambiguous column name".
        sb.append(prefix == null ? "" : prefix).append(quote("id"));
        if (!fieldHasCode(fields, "create_time")) {
            sb.append(",").append(prefix == null ? "" : prefix).append(quote("create_time"));
        }
        if (!fieldHasCode(fields, "update_time")) {
            sb.append(",").append(prefix == null ? "" : prefix).append(quote("update_time"));
        }
        return sb.toString();
    }

    private boolean fieldHasCode(List<FieldDefDTO> fields, String code) {
        if (fields == null) {
            return false;
        }

        for (FieldDefDTO f : fields) {
            if (code.equals(f.getFieldCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * SQL 标识符加反引号 (防止保留字冲突).
     * 严格要求标识符仅字母数字下划线 (由 validateTable 兜底).
     */
    private String quote(String ident) {
        return "`" + ident + "`";
    }

    /**
     * SQL + 参数列表 封装
     */
    public static class SqlAndParams {
        public final String sql;
        public final List<Object> params;

        public SqlAndParams(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
