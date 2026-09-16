package com.zifang.z.lc.core.executor;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.common.dto.RuntimeQueryDTO;
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
 * </ul>
 */
@Component
public class DynamicSqlBuilder {

    private static final Logger log = LogManager.getLogger(DynamicSqlBuilder.class);

    /**
     * 把 fieldType (STRING/INT/...) 映射为列定义片段 (用于 CREATE TABLE 或列类型推导).
     * 当前引擎不需要建表, 仅供后续 Phase 2 materialization 用.
     */
    public static String jdbcType(String fieldType, Integer length, Integer scale) {
        if (fieldType == null) return "VARCHAR(255)";
        switch (fieldType.toUpperCase()) {
            case "INT":
            case "LONG":
            case "REF":
                return "BIGINT";
            case "DECIMAL":
                int p = length == null ? 18 : Math.max(1, length);
                int s = scale == null ? 2 : Math.max(0, scale);
                return "DECIMAL(" + p + "," + s + ")";
            case "BOOLEAN":
                return "TINYINT(1)";
            case "DATE":
                return "DATE";
            case "DATETIME":
                return "DATETIME";
            case "TEXT":
                return "TEXT";
            case "JSON":
                return "JSON";
            case "STRING":
            default:
                int l = length == null ? 255 : Math.max(1, length);
                return "VARCHAR(" + l + ")";
        }
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
        sql.append(joinColumns(entity.getFields(), null));
        sql.append(" FROM ").append(quote(entity.getTableName()));
        sql.append(" WHERE deleted = 0");
        if (entity.getTenantCode() != null) {
            sql.append(" AND tenant_code = ?");
            params.add(query.getTenantCode() != null ? query.getTenantCode() : entity.getTenantCode());
        }
        if (query != null && query.getFilters() != null) {
            for (Map.Entry<String, Object> e : query.getFilters().entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (val == null) continue;
                String[] parts = key.split(":");
                String fieldCode = parts[0];
                String op = parts.length > 1 ? parts[1].toLowerCase() : "eq";
                FieldDefDTO fd = fieldIndex.get(fieldCode);
                if (fd == null) {
                    log.warn("Filter field not in whitelist, skip: {}", fieldCode);
                    continue;
                }
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
                                if (!first) sql.append(",");
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
                        log.warn("Unknown operator '{}' for {}, fallback to eq", op, fieldCode);
                        sql.append(" AND ").append(col).append(" = ?");
                        params.add(val);
                }
            }
        }
        if (query != null && query.getOrderBy() != null && !query.getOrderBy().isEmpty()) {
            // 仅接受 <field> [asc|desc] 形式; field 必须在白名单中
            String[] tokens = query.getOrderBy().trim().split("\\s+");
            String obField = tokens[0];
            String obDir = tokens.length > 1 ? tokens[1].toUpperCase() : "ASC";
            if (!"ASC".equals(obDir) && !"DESC".equals(obDir)) obDir = "ASC";
            if (fieldIndex.containsKey(obField)) {
                sql.append(" ORDER BY ").append(quote(obField)).append(" ").append(obDir);
            } else {
                log.warn("orderBy field not in whitelist: {}", obField);
            }
        } else {
            sql.append(" ORDER BY id DESC");
        }

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
        sql.append("SELECT COUNT(*) FROM ").append(quote(entity.getTableName()));
        sql.append(" WHERE deleted = 0");
        if (entity.getTenantCode() != null) {
            sql.append(" AND tenant_code = ?");
            params.add(query.getTenantCode() != null ? query.getTenantCode() : entity.getTenantCode());
        }
        if (query != null && query.getFilters() != null) {
            for (Map.Entry<String, Object> e : query.getFilters().entrySet()) {
                String key = e.getKey();
                Object val = e.getValue();
                if (val == null) continue;
                String[] parts = key.split(":");
                String fieldCode = parts[0];
                String op = parts.length > 1 ? parts[1].toLowerCase() : "eq";
                FieldDefDTO fd = fieldIndex.get(fieldCode);
                if (fd == null) continue;
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
                    case "in":
                        sql.append(" AND ").append(col).append(" IN (");
                        if (val instanceof Collection) {
                            Collection<?> coll = (Collection<?>) val;
                            boolean first = true;
                            for (Object v : coll) {
                                if (!first) sql.append(",");
                                sql.append("?");
                                params.add(v);
                                first = false;
                            }
                            if (coll.isEmpty()) sql.append("NULL");
                        } else {
                            sql.append("?");
                            params.add(val);
                        }
                        sql.append(")");
                        break;
                    default:
                        sql.append(" AND ").append(col).append(" = ?");
                        params.add(val);
                }
            }
        }
        return new SqlAndParams(sql.toString(), params);
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
            if (e.getValue() == null) continue;
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
            if (i > 0) sql.append(",");
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
            if (e.getValue() == null) continue;
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
    public Object coerce(Object raw, String fieldType) {
        if (raw == null) return null;
        if (fieldType == null) return raw;
        switch (fieldType.toUpperCase()) {
            case "INT":
            case "LONG":
            case "REF":
                if (raw instanceof Number) return ((Number) raw).longValue();
                try {
                    return Long.parseLong(raw.toString());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Field requires long: " + raw);
                }
            case "DECIMAL":
                if (raw instanceof Number) return ((Number) raw).doubleValue();
                try {
                    return Double.parseDouble(raw.toString());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Field requires decimal: " + raw);
                }
            case "BOOLEAN":
                if (raw instanceof Boolean) return raw;
                return Boolean.parseBoolean(raw.toString());
            case "DATE":
            case "DATETIME":
                if (raw instanceof java.util.Date) return raw;
                try {
                    return new java.text.SimpleDateFormat(
                            "DATE".equalsIgnoreCase(fieldType) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss")
                            .parse(raw.toString());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Field requires date: " + raw);
                }
            case "JSON":
            case "TEXT":
            case "STRING":
            default:
                return raw.toString();
        }
    }

    private void validateTable(EntityDefDTO entity) {
        if (entity == null) throw new IllegalArgumentException("entity is null");
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
        if (entity.getFields() == null) return idx;
        for (FieldDefDTO f : entity.getFields()) {
            if (f.getFieldCode() == null) continue;
            // 防御性白名单: 字段名只能字母数字下划线
            if (!f.getFieldCode().matches("^[A-Za-z][A-Za-z0-9_]*$")) continue;
            idx.put(f.getFieldCode(), f);
        }
        return idx;
    }

    private String joinColumns(List<FieldDefDTO> fields, String prefix) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (fields != null) {
            for (FieldDefDTO f : fields) {
                if (f.getFieldCode() == null) continue;
                if (!first) sb.append(",");
                if (prefix != null) sb.append(prefix);
                sb.append(quote(f.getFieldCode()));
                first = false;
            }
        }
        if (!first) sb.append(",");
        sb.append(quote("id"));
        if (!fieldHasCode(fields, "create_time")) {
            sb.append(",").append(quote("create_time"));
        }
        if (!fieldHasCode(fields, "update_time")) {
            sb.append(",").append(quote("update_time"));
        }
        return sb.toString();
    }

    private boolean fieldHasCode(List<FieldDefDTO> fields, String code) {
        if (fields == null) return false;
        for (FieldDefDTO f : fields) {
            if (code.equals(f.getFieldCode())) return true;
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
