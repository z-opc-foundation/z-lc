package com.zifang.z.lc.core.mapper;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    public DbTableMapperService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
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
     * 扫描数据库下所有表（支持前缀过滤）
     */
    public List<EntityDefDTO> scanTables(String tableNamePrefix, String schema) {
        List<EntityDefDTO> result = new ArrayList<>();
        try {
            DatabaseMetaData meta = jdbcTemplate.getDataSource().getConnection().getMetaData();
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
        } catch (Exception e) {
            log.error("scanTables failed", e);
        }
        return result;
    }

    /**
     * 映射单张表为 EntityDefDTO
     */
    public EntityDefDTO mapTable(String tableName, String schema) {
        try {
            DatabaseMetaData meta = jdbcTemplate.getDataSource().getConnection().getMetaData();
            return mapTable(meta, tableName, null);
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

        entity.setFields(fields);
        log.info("Mapped table {} → entity={} ({} fields)", tableName, entity.getEntityCode(), fields.size());
        return entity;
    }
}
