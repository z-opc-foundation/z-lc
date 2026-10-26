package com.zifang.z.lc.core.mapper;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.core.datasource.LcDataSourceResolver;
import com.zifang.z.lc.core.schema.SchemaAdminBizService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * DB 表逆向映射服务: 扫描已有物理表 → EntityDefDTO 模型定义
 * <p>
 * 支持:
 * <ul>
 *   <li>指定表名精确映射</li>
 *   <li>按表名前缀批量扫描 (e.g. lc_* 或 oc_*)</li>
 *   <li>读取列注释作为 fieldName</li>
 *   <li>自动识别主键 / 外键关联</li>
 * </ul>
 * <p>
 * 用法:
 * <pre>{@code
 * // 单表映射
 * EntityDefDTO entity = mapper.mapTable("oc_customer");
 *
 * // 批量扫描
 * List<EntityDefDTO> entities = mapper.scanTables("lc_");
 * }</pre>
 */
@Service
public class DbTableMapperService {

    private static final Logger log = LogManager.getLogger(DbTableMapperService.class);
    private final JdbcTemplate jdbcTemplate;
    /**
     * 数据源目录（V1 孵化，2026-10-26）：可选 bean —— 未装配 z-dms 时为 absent，
     * 所有 dataSourceId==null 的老路径行为不变。
     */
    private final ObjectProvider<LcDataSourceResolver> resolverProvider;

    @org.springframework.beans.factory.annotation.Autowired
    public DbTableMapperService(DataSource dataSource, ObjectProvider<LcDataSourceResolver> resolverProvider) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.resolverProvider = resolverProvider;
    }

    /**
     * 兼容构造（目录能力视同未装配）—— 老调用方/老测试不受多源化影响。
     */
    public DbTableMapperService(DataSource dataSource) {
        this(dataSource, new EmptyResolverProvider());
    }

    /** 恒缺席的 provider（免依赖 Spring 上下文的空实现）。 */
    private static final class EmptyResolverProvider implements ObjectProvider<LcDataSourceResolver> {
        @Override
        public LcDataSourceResolver getObject() {
            throw new UnsupportedOperationException("数据源目录未装配");
        }

        @Override
        public LcDataSourceResolver getObject(Object... args) {
            throw new UnsupportedOperationException("数据源目录未装配");
        }

        @Override
        public LcDataSourceResolver getIfAvailable() {
            return null;
        }

        @Override
        public LcDataSourceResolver getIfUnique() {
            return null;
        }
    }

    /**
     * 按目录 id 取连接；id 为 null（或空白）= 主库。
     *
     * @throws IllegalArgumentException id 在目录里不存在
     * @throws IllegalStateException    目录能力未装配（z-dms 未接入）
     */
    private Connection openConnection(String dataSourceId) throws Exception {
        if (dataSourceId == null || dataSourceId.trim().isEmpty()) {
            return jdbcTemplate.getDataSource().getConnection();
        }
        LcDataSourceResolver resolver = resolverProvider.getIfAvailable();
        if (resolver == null || !resolver.available()) {
            throw new IllegalStateException("数据源目录未装配（z-dms 未接入），无法按 id 取连接: " + dataSourceId);
        }
        return resolver.resolve(dataSourceId).getConnection();
    }

    /**
     * JDBC TYPE → 低代码字段类型
     */
    public static String toLcType(int jdbcType, long size, int scale) {
        switch (jdbcType) {
            case Types.BIT:
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.TINYINT:
            case Types.SMALLINT:
                return "INTEGER";
            case Types.INTEGER:
                return "INTEGER";
            case Types.BIGINT:
                return "LONG";
            case Types.FLOAT:
            case Types.REAL:
                return "FLOAT";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.DECIMAL:
            case Types.NUMERIC:
                return "DECIMAL";
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                if (size == 1) return "BOOLEAN"; // TINYINT(1) often used as bool
                if (size < 50) {
                    return "VARCHAR";
                }

                if (size < 2000) {
                    return "TEXT";
                }

                return "TEXT";
            case Types.DATE:
                return "DATE";
            case Types.TIME:
            case Types.TIME_WITH_TIMEZONE:
                return "TIME";
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "DATETIME";
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return "FILE";
            case Types.CLOB:
            case Types.NCLOB:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return "TEXT";
            default:
                return "VARCHAR";
        }
    }

    private static String removePrefix(String name, String[] prefixes) {
        String lower = name.toLowerCase();
        for (String p : prefixes) {
            if (lower.startsWith(p)) {
                return name.substring(p.length());
            }
        }
        return name;
    }

    private static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : name.toCharArray()) {
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 扫描数据库下所有表（支持前缀过滤）。等价 {@code scanTables(null, prefix, schema)}（主库）。
     */
    public List<EntityDefDTO> scanTables(String tableNamePrefix, String schema) {
        return scanTables(null, tableNamePrefix, schema);
    }

    /**
     * 按数据源目录 id 扫描（V1 数据源管理域）。{@code dataSourceId=null} = 主库（历史行为）。
     *
     * @throws IllegalStateException    目录未装配
     * @throws IllegalArgumentException 目录里不存在该 id
     */
    public List<EntityDefDTO> scanTables(String dataSourceId, String tableNamePrefix, String schema) {
        List<EntityDefDTO> result = new ArrayList<>();
        try (Connection conn = openConnection(dataSourceId)) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = schema;
            String searchPattern = tableNamePrefix == null ? "%" : tableNamePrefix + "%";

            try (ResultSet rs = meta.getTables(catalog, null, searchPattern, new String[]{"TABLE"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String remarks = rs.getString("REMARKS");
                    try {
                        EntityDefDTO entity = mapTable(meta, tableName, remarks);
                        if (entity != null) {
                            result.add(entity);
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to map table {}: {}", tableName, ex.getMessage());
                    }
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("scanTables failed", e);
        }
        return result;
    }

    /**
     * 映射单张表为 EntityDefDTO（主库）。等价 {@code mapTable(null, tableName, schema)}。
     */
    public EntityDefDTO mapTable(String tableName, String schema) {
        return mapTable((String) null, tableName, schema);
    }

    /**
     * 按数据源目录 id 映射单张表（V1 数据源管理域）。{@code dataSourceId=null} = 主库（历史行为）。
     *
     * @throws IllegalStateException    目录未装配
     * @throws IllegalArgumentException 目录里不存在该 id
     */
    public EntityDefDTO mapTable(String dataSourceId, String tableName, String schema) {
        try (Connection conn = openConnection(dataSourceId)) {
            return mapTable(conn.getMetaData(), tableName, null);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map table: " + tableName, e);
        }
    }

    /**
     * 内部映射逻辑
     */
    private EntityDefDTO mapTable(DatabaseMetaData meta, String tableName, String remarks) throws Exception {
        EntityDefDTO entity = new EntityDefDTO();
        entity.setTableName(tableName);

        // 从表名推导 entityCode: 去掉前缀分割，下划线转驼峰
        entity.setEntityCode(toCamelCase(removePrefix(tableName, new String[]{"lc_", "oc_", "z_", "t_"})));
        entity.setEntityName(remarks != null && !remarks.isEmpty() ? remarks : entity.getEntityCode());
        entity.setDescription("逆向映射自: " + tableName);

        // 读取列信息
        List<FieldDefDTO> fields = new ArrayList<>();
        List<String> skippedSystemColumns = new ArrayList<>();
        Set<String> primaryKeys = new HashSet<>();

        // 读取主键
        try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
            while (rs.next()) {
                primaryKeys.add(rs.getString("COLUMN_NAME"));
            }
        }

        // 读取列
        try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                FieldDefDTO field = new FieldDefDTO();
                String colName = rs.getString("COLUMN_NAME");
                String colRemarks = rs.getString("REMARKS");
                int dataType = rs.getInt("DATA_TYPE");
                long columnSize = rs.getLong("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                String isNullable = rs.getString("IS_NULLABLE");

                // 这几列引擎会给每张受管表自建, 映射成业务字段就等着建表撞 Duplicate column name。
                // 跳过必须留痕: 逆向映射的表里它们是真存在的列, 不写一句就等于静默丢数据。
                if (colName != null
                        && SchemaAdminBizService.SYSTEM_COLUMN_CODES.contains(colName.toLowerCase(Locale.ROOT))) {
                    skippedSystemColumns.add(colName);
                    continue;
                }

                field.setFieldCode(colName);
                field.setFieldName(colRemarks != null && !colRemarks.isEmpty() ? colRemarks : colName);
                field.setFieldType(toLcType(dataType, columnSize, decimalDigits));
                field.setFieldLength((int) columnSize);
                field.setScale(decimalDigits);
                field.setRequired("NO".equalsIgnoreCase(isNullable) ? true : false);
                // 主键字段自动必填
                if (primaryKeys.contains(colName)) {
                    field.setRequired(true);
                }
                field.setSortOrder(fields.size());
                fields.add(field);
            }
        }

        // 读取外键（作为 refEntity 挂载）
        try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
            Set<String> handled = new HashSet<>();
            while (rs.next()) {
                String fkCol = rs.getString("FKCOLUMN_NAME");
                String pkTable = rs.getString("PKTABLE_NAME");
                String pkCol = rs.getString("PKCOLUMN_NAME");
                String fkName = rs.getString("FK_NAME");

                // 找到对应 field，标记 refEntity
                for (FieldDefDTO f : fields) {
                    if (f.getFieldCode().equals(fkCol) && !handled.contains(fkCol)) {
                        f.setRefEntity(toCamelCase(removePrefix(pkTable, new String[]{"lc_", "oc_", "z_", "t_"})));
                        f.setDescription("外键 → " + pkTable + "." + pkCol + " (" + fkName + ")");
                        handled.add(fkCol);
                        break;
                    }
                }
            }
        }

        if (!skippedSystemColumns.isEmpty()) {
            entity.setDescription("逆向映射自: " + tableName
                    + "; 已跳过引擎自建列: " + String.join(", ", skippedSystemColumns));
        }
        entity.setFields(fields);
        log.info("Mapped table {} → entity={} ({} fields)", tableName, entity.getEntityCode(), fields.size());
        return entity;
    }
}
