package com.zifang.z.lc.common.enums;

/**
 * 初始化包导入节点处理状态枚举 — 蒸馏自 ace-platform-core
 * {@code InitPackageImportNodeStatusEnum} ({@code com.c2f.ace.core.common}}.
 *
 * <p>用于低代码平台「初始化包导入」单节点异步处理状态：
 * <ul>
 *   <li>{@link #SUCCESS} — 处理成功</li>
 *   <li>{@link #FAIL} — 处理失败</li>
 *   <li>{@link #EXECUTING} — 处理中</li>
 * </ul>
 *
 * <p>对应初始化包导入任务每一条节点的执行结果。
 *
 * @author zifang
 */
public enum ZLcInitPackageImportNodeStatusEnum {

    /** 处理成功. */
    SUCCESS("success"),

    /** 处理失败. */
    FAIL("fail"),

    /** 处理中. */
    EXECUTING("executing");

    private final String status;

    ZLcInitPackageImportNodeStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static ZLcInitPackageImportNodeStatusEnum fromStatus(String status) {
        if (status == null) {
            return null;
        }
        for (ZLcInitPackageImportNodeStatusEnum v : values()) {
            if (v.status.equalsIgnoreCase(status)) {
                return v;
            }
        }
        return null;
    }

    /** 是否终态 (SUCCESS/FAIL 视为终态，EXECUTING 视为非终态). */
    public boolean isTerminal() {
        return this != EXECUTING;
    }
}