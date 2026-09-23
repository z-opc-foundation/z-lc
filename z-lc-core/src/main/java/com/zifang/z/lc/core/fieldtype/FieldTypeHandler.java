package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.util.List;
import java.util.Map;

/**
 * 字段类型处理器 — 一个逻辑类型 (STRING/INT/.../REF) 的全部类型语义的唯一来源.
 * <p>
 * 取代此前散落在 {@code DynamicSqlBuilder.jdbcType}/{@code coerce}、pipeline processors、
 * 以及三套竞争枚举中的重复类型知识. 每个 handler 声明:
 * <ul>
 *   <li>{@link #fieldType()} — 逻辑/UI 类型名 (与 FieldDefDTO.fieldType 对齐)</li>
 *   <li>{@link #cellValueType()} — String/Number/Boolean/DateTime, 过滤/排序/分组只咨询它</li>
 *   <li>{@link #dbType(Integer, Integer)} — 物理列类型 (与历史 DDL 完全一致, 不改存量表)</li>
 *   <li>{@link #coerce(Object)} — 写入值强转 (保持历史 DATE/DATETIME 解析等行为)</li>
 *   <li>{@link #validate(FieldDefDTO)} — 字段配置校验 (结构化错误列表)</li>
 *   <li>{@link #validateValue(FieldDefDTO, Object)} — 数据值校验 (必填/长度/数值/布尔/日期)</li>
 *   <li>{@link #operators()} — 该类型允许的过滤操作符</li>
 *   <li>能力位: sortable / groupable / filterable / inlineEditable + widget 提示名</li>
 *   <li>{@link #formatForExport(Object)} / {@link #parseFromCell(String)} — CSV 导入导出</li>
 * </ul>
 * <p>
 * handler 必须无状态、线程安全; 由 {@link FieldTypeRegistry} 统一注册与查找.
 *
 * @author zifang
 */
public interface FieldTypeHandler {

    /**
     * 逻辑类型名 (大写), 如 "STRING".
     */
    String fieldType();

    /**
     * 单元格值类型 (过滤/排序/分组的唯一依据).
     */
    CellValueType cellValueType();

    /**
     * UI/展示名 (中文 label, 供 meta 接口输出).
     */
    String label();

    /**
     * 物理列类型 (等价于历史 DynamicSqlBuilder.jdbcType 的输出).
     *
     * @param length fieldLength, 可空
     * @param scale  scale, 可空
     * @return 如 "VARCHAR(255)" / "BIGINT" / "DECIMAL(18,2)"
     */
    String dbType(Integer length, Integer scale);

    /**
     * 写入值强转; 非法值抛 IllegalArgumentException (行为与历史 coerce 完全一致).
     */
    Object coerce(Object raw);

    /**
     * 字段配置合法性校验 (如 STRING 应声明 fieldLength, DECIMAL 可带 scale, REF 必须有 refEntity).
     *
     * @return 结构化错误列表; 空列表 = 配置合法
     */
    List<ValidationError> validate(FieldDefDTO def);

    /**
     * 数据值校验: 必填 / 最大长度 / 数值范围 / 布尔解析 / 日期解析.
     * 不抛首个异常, 返回全部错误的结构化列表.
     *
     * @param def       字段配置
     * @param cellValue 单元格值 (写入前的原始值, 可为 null)
     * @return 结构化错误列表; 空列表 = 值合法
     */
    List<ValidationError> validateValue(FieldDefDTO def, Object cellValue);

    /**
     * 该类型允许的结构化过滤操作符.
     */
    List<String> operators();

    boolean sortable();

    boolean groupable();

    boolean filterable();

    boolean inlineEditable();

    /**
     * 前端控件提示: input / textarea / number / switch / datePicker / select / json / ref.
     */
    String widget();

    /**
     * CSV 导出: 单元格值 → 文本 (null → 空串).
     */
    String formatForExport(Object cellValue);

    /**
     * CSV 导入: 文本 → 值 (与 {@link #coerce(Object)} 同一解析规则; 空串 → null).
     */
    Object parseFromCell(String text);

    /**
     * meta 接口输出的一行描述 (顺序即契约: fieldType/cellValueType/label/dbType/widget/
     * sortable/groupable/filterable/inlineEditable/operators).
     */
    Map<String, Object> describe();
}
