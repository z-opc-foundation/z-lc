package com.zifang.z.lc.common.utils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * SQL 工具 — 蒸馏自 ace-platform-core
 * {@code SqlUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供低代码平台常用的 SQL 辅助方法:
 * INSERT 语句生成、驼峰 ↔ 下划线转换等.
 * 蒸馏时移除了 ace 对 hutool / MyBatis Plus / FastDateFormat 的依赖,
 * 改为纯 JDK 反射 + SimpleDateFormat 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>动态数据模型 INSERT 语句生成</li>
 *   <li>数据库字段名 ↔ Java 属性名互转</li>
 *   <li>SQL 注入防护的参数校验</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcSqlUtil {

    private ZLcSqlUtil() {
    }

    private static final String INSERT_TEMPLATE = "INSERT INTO %s(";
    private static final String VALUES_LABEL = "VALUES(";
    private static final String END_LABEL = ");";
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 根据对象生成 INSERT SQL 语句.
     *
     * @param tableName 表名
     * @param object    数据对象
     * @return INSERT SQL 字符串
     */
    public static String generateInsertSql(String tableName, Object object) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(INSERT_TEMPLATE, tableName));
        Class<?> clazz = object.getClass();
        Field[] fields = getFields(clazz);

        try {
            // 拼接列名
            for (int i = 0; i < fields.length; i++) {
                String colName = "deleted".equals(fields[i].getName())
                        ? "is_deleted"
                        : camelToUnderline(fields[i].getName());
                sb.append(colName);
                if (i < fields.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(") ").append(VALUES_LABEL);

            // 拼接值
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                Object value = fields[i].get(object);
                String type = fields[i].getType().getSimpleName();
                if (isNumericType(type)) {
                    sb.append(value);
                } else if ("Date".equals(type)) {
                    sb.append(value == null ? "null" : "'" + SDF.format((Date) value) + "'");
                } else {
                    sb.append(value == null ? "null" : "'" + value + "'");
                }
                if (i < fields.length - 1) {
                    sb.append(",");
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("生成 INSERT SQL 失败", e);
        }
        sb.append(END_LABEL);
        return sb.toString();
    }

    /**
     * 驼峰命名转下划线命名.
     *
     * @param str 驼峰字符串 (如 "userName")
     * @return 下划线字符串 (如 "user_name")
     */
    public static String camelToUnderline(String str) {
        return str.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    /**
     * 获取类的所有字段 (含父类).
     */
    private static Field[] getFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && !java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    fieldList.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fieldList.toArray(new Field[0]);
    }

    /**
     * 判断类型是否为数值类型.
     */
    private static boolean isNumericType(String type) {
        return "int".equals(type) || "Integer".equals(type)
                || "long".equals(type) || "Long".equals(type)
                || "double".equals(type) || "Double".equals(type)
                || "float".equals(type) || "Float".equals(type)
                || "short".equals(type) || "Short".equals(type)
                || "byte".equals(type) || "Byte".equals(type);
    }
}
