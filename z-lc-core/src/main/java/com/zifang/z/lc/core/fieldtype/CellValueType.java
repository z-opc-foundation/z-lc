package com.zifang.z.lc.core.fieldtype;

/**
 * 单元格值类型 (cell value type) — 逻辑字段类型归一化后的 4 个分类.
 * <p>
 * 过滤 / 排序 / 分组只应咨询该类型 (而非逐个 switch fieldType 字符串).
 * {@link FieldTypeRegistry} 是它唯一的注册来源.
 *
 * @author zifang
 */
public enum CellValueType {

    /**
     * 文本类: STRING / TEXT / JSON
     */
    STRING("String"),

    /**
     * 数值类: INT / LONG / DECIMAL / REF
     */
    NUMBER("Number"),

    /**
     * 布尔类: BOOLEAN
     */
    BOOLEAN("Boolean"),

    /**
     * 日期时间类: DATE / DATETIME
     */
    DATETIME("DateTime");

    private final String code;

    CellValueType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
