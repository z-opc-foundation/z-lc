package com.zifang.z.lc.common.enums;

/**
 * 单元格对齐方式枚举 — 蒸馏自 ace-platform-core
 * {@code CellStyleEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于导出 Excel 模板 / 报表时标注单元格水平对齐方式 —
 * 对应 Apache POI 的 {@code HorizontalAlignment}.
 *
 * <p>z-lc 蒸馏时不依赖 POI（避免 z-lc-common 引入 poi-ooxml），
 * 仅保留 code 字段和映射方法 — 业务方在做导出时自行映射到 POI API.
 *
 * @author zifang
 */
public enum ZLcCellStyleEnum {

    /** 居左对齐. */
    LEFT(1, "居左"),

    /** 居中对齐. */
    CENTER(2, "居中"),

    /** 居右对齐. */
    RIGHT(3, "居右");

    private final Integer code;
    private final String value;

    ZLcCellStyleEnum(Integer code, String value) {
        this.code = code;
        this.value = value;
    }

    public Integer getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }

    /**
     * 按 code 反查（未匹配返回 null）.
     */
    public static ZLcCellStyleEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcCellStyleEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 业务方在集成 POI 时调用 — 把 code 映射到 POI 枚举.
     * 业务方在自己的 Service 中按 POI 依赖实现:
     * <pre>{@code
     *   HorizontalAlignment align = switch (ZLcCellStyleEnum.fromCode(code)) {
     *       case LEFT -> HorizontalAlignment.LEFT;
     *       case CENTER -> HorizontalAlignment.CENTER;
     *       case RIGHT -> HorizontalAlignment.RIGHT;
     *       default -> throw new IllegalStateException("Unknown code: " + code);
     *   };
     * }</pre>
     */
    public static String getPoiHorizontalAlignmentName(Integer code) {
        ZLcCellStyleEnum e = fromCode(code);
        if (e == null) {
            return null;
        }
        switch (e) {
            case LEFT: return "LEFT";
            case CENTER: return "CENTER";
            case RIGHT: return "RIGHT";
            default: return null;
        }
    }
}
