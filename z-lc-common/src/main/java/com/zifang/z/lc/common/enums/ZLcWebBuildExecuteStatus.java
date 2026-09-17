package com.zifang.z.lc.common.enums;

/**
 * Web 构建执行状态枚举 — 蒸馏自 ace-platform-core
 * {@code WebBuildExecuteStatusEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"Web 应用构建"异步执行的状态:
 *
 * <ul>
 *   <li>{@link #EXECUTING} — 执行中</li>
 *   <li>{@link #SUCCESS} — 执行成功</li>
 *   <li>{@link #FAIL} — 执行失败</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebBuildExecuteStatus {

    /** 执行中. */
    EXECUTING("executing", "执行中"),

    /** 成功. */
    SUCCESS("success", "成功"),

    /** 失败. */
    FAIL("fail", "失败");

    private final String type;
    private final String desc;

    ZLcWebBuildExecuteStatus(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebBuildExecuteStatus fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebBuildExecuteStatus v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }

    /** 是否终态 (SUCCESS/FAIL). */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAIL;
    }

    /** 是否成功. */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}