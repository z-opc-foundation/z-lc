package com.zifang.z.lc.common.enums;

import java.util.Objects;

/**
 * 数据处理任务状态枚举 — 蒸馏自 ace-platform-core
 * {@code DataProcessingTaskStatusEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「数据导入/导出」任务的生命周期状态展示.
 *
 * @author zifang
 */
public enum ZLcDataProcessingTaskStatusEnum {

    /** 待执行. */
    UNEXECUTED(1, "待执行"),

    /** 执行中. */
    IN_EXECUTION(2, "执行中"),

    /** 执行完成. */
    EXECUTION_COMPLETE(3, "执行完成"),

    /** 执行失败. */
    EXECUTION_FAIL(4, "执行失败");

    private final Integer code;
    private final String value;

    ZLcDataProcessingTaskStatusEnum(Integer code, String value) {
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
    public static ZLcDataProcessingTaskStatusEnum getByCode(Integer code) {
        for (ZLcDataProcessingTaskStatusEnum v : values()) {
            if (Objects.equals(v.getCode(), code)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 是否处于终态（执行完成 / 执行失败）.
     */
    public boolean isTerminal() {
        return this == EXECUTION_COMPLETE || this == EXECUTION_FAIL;
    }

    /**
     * 是否处于进行中（执行中）.
     */
    public boolean isRunning() {
        return this == IN_EXECUTION;
    }
}
