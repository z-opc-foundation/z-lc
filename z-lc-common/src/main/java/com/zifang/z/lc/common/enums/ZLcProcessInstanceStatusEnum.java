package com.zifang.z.lc.common.enums;

/**
 * 流程实例状态枚举 — 蒸馏自 ace-platform-core
 * {@code ProcessInstanceStatusEnum} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义流程实例的运行状态: 已结束、运行中、暂停、未知.
 *
 * @author zifang
 */
public enum ZLcProcessInstanceStatusEnum {

    FINISHED(0, "已结束"),
    UNFINISHED(1, "运行中"),
    SUSPEND(2, "暂停"),
    UNKNOWN(3, "未知");

    private final Integer code;
    private final String description;

    ZLcProcessInstanceStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcProcessInstanceStatusEnum of(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (ZLcProcessInstanceStatusEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return UNKNOWN;
    }
}
