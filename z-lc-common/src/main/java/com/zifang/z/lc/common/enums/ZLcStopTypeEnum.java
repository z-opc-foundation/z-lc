package com.zifang.z.lc.common.enums;

/**
 * 流程停止类型枚举 — 蒸馏自 ace-platform-core
 * {@code StopTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「流程实例」停止操作时区分停止原因：
 * <ul>
 *   <li>{@link #STOP_DELETE} — 删除流程实例（彻底删除）</li>
 *   <li>{@link #STOP_SUSPEND} — 挂起流程实例（暂停）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcStopTypeEnum {

    /** 删除流程实例. */
    STOP_DELETE(1, "删除"),

    /** 挂起流程实例. */
    STOP_SUSPEND(2, "挂起");

    private final Integer stopType;
    private final String stopTypeName;

    ZLcStopTypeEnum(Integer stopType, String stopTypeName) {
        this.stopType = stopType;
        this.stopTypeName = stopTypeName;
    }

    public Integer getStopType() {
        return stopType;
    }

    public String getStopTypeName() {
        return stopTypeName;
    }

    public static ZLcStopTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcStopTypeEnum v : values()) {
            if (v.stopType.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
