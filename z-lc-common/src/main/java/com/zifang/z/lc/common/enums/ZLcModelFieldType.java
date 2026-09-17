package com.zifang.z.lc.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 模型字段类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.ModelFieldType} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于标注模型字段的数据类型 — 与 DataFieldDO.fieldType 对应.
 *
 * <ul>
 *   <li>{@link #TEXT} — 文本类型（默认 String）</li>
 *   <li>{@link #NUMBER} — 数字类型（Integer/Long/Decimal）</li>
 *   <li>{@link #TIME} — 时间类型（Date/LocalDateTime）</li>
 *   <li>{@link #OBJECT} — 1-1 子模型类型（嵌套对象）</li>
 *   <li>{@link #ARRAY} — 列表型数据类型（1-N）</li>
 * </ul>
 *
 * <p>{@link #sysFieldType} 列出了「系统字段类型」—
 * 引擎识别到这 3 种类型时直接生成对应 DB 列；其余类型由业务方自定义映射.
 *
 * @author zifang
 */
public enum ZLcModelFieldType {

    /** 时间类型 */
    TIME("Time"),

    /** 数字类型 */
    NUMBER("Number"),

    /** 文本类型 */
    TEXT("Text"),

    /** 1-1 子模型类型 */
    OBJECT("Object"),

    /** 列表型数据类型 */
    ARRAY("Array");

    private final String code;

    ZLcModelFieldType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** 系统内置字段类型 — 引擎识别的基本类型. */
    public static final List<String> SYS_FIELD_TYPES = Arrays.asList(
            TIME.getCode(), NUMBER.getCode(), TEXT.getCode());

    /**
     * 判断给定字符串是否是系统字段类型.
     */
    public static boolean isSysFieldType(String typeCode) {
        return typeCode != null && SYS_FIELD_TYPES.contains(typeCode);
    }

    public static ZLcModelFieldType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcModelFieldType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
