package com.zifang.z.lc.core.fieldtype;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段类型注册表 — 引擎内所有 fieldType 语义的唯一入口 (single source of truth).
 * <p>
 * 取代历史上三处竞争知识:
 * <ul>
 *   <li>{@code DynamicSqlBuilder.jdbcType} 的 switch</li>
 *   <li>{@code DynamicSqlBuilder.coerce} 的 switch</li>
 *   <li>{@code ZLcModelFieldType / ZLcExternalFieldType / ZLcColumnTypeEnum} 等枚举的重复映射</li>
 * </ul>
 * <p>
 * 兼容约束 (由 FieldTypeRegistryTest 守护):
 * <ul>
 *   <li>dbType 输出与历史 jdbcType 逐字符一致 (已建物理表 DDL 不变)</li>
 *   <li>coerce 行为与历史完全一致: raw==null→null; fieldType==null→原值; 未知类型按 STRING 处理 (toString)</li>
 * </ul>
 * 既是 Spring bean (MetaController / pipeline 注入), 也提供静态入口 (历史静态方法委托).
 *
 * @author zifang
 */
@Component
public class FieldTypeRegistry {

    /**
     * 全局默认实例 (静态路径使用; handler 均无状态, 安全).
     */
    private static final FieldTypeRegistry DEFAULT = new FieldTypeRegistry();

    private final Map<String, FieldTypeHandler> byType = new LinkedHashMap<>();
    private final List<FieldTypeHandler> ordered = new ArrayList<>();
    private final FieldTypeHandler fallback = new StringTypeHandler();

    public FieldTypeRegistry() {
        register(new StringTypeHandler());
        register(new IntTypeHandler());
        register(new LongTypeHandler());
        register(new DecimalTypeHandler());
        register(new BooleanTypeHandler());
        register(new DateTypeHandler());
        register(new DateTimeTypeHandler());
        register(new TextTypeHandler());
        register(new JsonTypeHandler());
        register(new RefTypeHandler());
    }

    private void register(FieldTypeHandler h) {
        byType.put(h.fieldType().toUpperCase(), h);
        ordered.add(h);
    }

    /**
     * 全局默认注册表.
     */
    public static FieldTypeRegistry getDefault() {
        return DEFAULT;
    }

    /**
     * required 判空语义唯一入口 (null / 空串 / 空集合 视为空).
     * 供 pipeline 的 RequiredCheckProcessor 与 handler.validateValue 共用, 避免语义漂移.
     */
    public static boolean isEmptyValue(Object v) {
        return AbstractFieldTypeHandler.isEmptyValue(v);
    }

    /**
     * 按 fieldType 查找 handler; null / 未注册类型回退为 STRING 语义 (与历史 switch default 分支一致).
     */
    public FieldTypeHandler handler(String fieldType) {
        if (fieldType == null) {
            return fallback;
        }

        FieldTypeHandler h = byType.get(fieldType.trim().toUpperCase());
        return h == null ? fallback : h;
    }

    /**
     * 物理列类型 (与历史 DynamicSqlBuilder.jdbcType 完全一致).
     */
    public static String jdbcType(String fieldType, Integer length, Integer scale) {
        return DEFAULT.handler(fieldType).dbType(length, scale);
    }

    /**
     * 写入值强转 (与历史 DynamicSqlBuilder.coerce 完全一致):
     * raw==null → null; fieldType==null → 原值直返; 未知类型 → STRING 语义 (toString).
     */
    public static Object coerceValue(Object raw, String fieldType) {
        if (raw == null) {
            return null;
        }

        if (fieldType == null) {
            return raw;
        }

        return DEFAULT.handler(fieldType).coerce(raw);
    }

    /**
     * 所有已注册 handler (注册顺序 = meta 接口输出顺序).
     */
    public List<FieldTypeHandler> handlers() {
        return Collections.unmodifiableList(ordered);
    }

    /**
     * 该类型是否允许某操作符 (结构化查询白名单校验之一).
     */
    public boolean supportsOperator(String fieldType, String operator) {
        if (operator == null) {
            return false;
        }

        return DEFAULT.handler(fieldType).operators().contains(operator.trim());
    }

    /**
     * /api/lc/meta/field-types 与 /api/lc/meta/bundle.fieldTypes 的统一 payload.
     * 每行 key 顺序即契约: fieldType/cellValueType/label/dbType/widget/sortable/groupable/filterable/inlineEditable/operators.
     */
    public List<Map<String, Object>> describe() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (FieldTypeHandler h : ordered) {
            rows.add(h.describe());
        }

        return rows;
    }
}
