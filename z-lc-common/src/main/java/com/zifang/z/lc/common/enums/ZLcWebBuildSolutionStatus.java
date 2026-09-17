package com.zifang.z.lc.common.enums;

/**
 * Web 构建方案状态枚举 — 蒸馏自 ace-platform-core
 * {@code WebBuildSolutionStatusEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于标识低代码平台"Web 应用构建方案"的执行进度状态:
 *
 * <ul>
 *   <li>{@link #TO_BE_START} — 待开始</li>
 *   <li>{@link #IN_PROGRESS} — 进行中</li>
 *   <li>{@link #COMPLETED} — 已完成</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcWebBuildSolutionStatus {

    /** 待开始. */
    TO_BE_START("toBeStart", "待开始"),

    /** 进行中. */
    IN_PROGRESS("inProgress", "进行中"),

    /** 已完成. */
    COMPLETED("completed", "已完成");

    private final String type;
    private final String desc;

    ZLcWebBuildSolutionStatus(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public String getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcWebBuildSolutionStatus fromType(String type) {
        if (type == null) {
            return null;
        }
        for (ZLcWebBuildSolutionStatus v : values()) {
            if (v.type.equals(type)) {
                return v;
            }
        }
        return null;
    }

    /** 获取中文描述 (按 type 反查). */
    public static String getDescByType(String type) {
        ZLcWebBuildSolutionStatus v = fromType(type);
        return v == null ? null : v.desc;
    }

    /** 是否终态 (COMPLETED). */
    public boolean isTerminal() {
        return this == COMPLETED;
    }
}