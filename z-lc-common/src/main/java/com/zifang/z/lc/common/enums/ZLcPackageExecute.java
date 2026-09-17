package com.zifang.z.lc.common.enums;

/**
 * 应用包执行状态枚举 — 蒸馏自 ace-platform-core
 * {@code PackageExecuteEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"初始化包 / 应用包"在异步执行任务中的状态:
 *
 * <ul>
 *   <li>{@link #UN_EXECUTE} — 待执行</li>
 *   <li>{@link #EXECUTING} — 执行中</li>
 *   <li>{@link #EXECUTED} — 执行成功</li>
 *   <li>{@link #FAIL} — 执行失败</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcPackageExecute {

    /** 待执行. */
    UN_EXECUTE(0, "待执行"),

    /** 执行中. */
    EXECUTING(1, "执行中"),

    /** 执行成功. */
    EXECUTED(2, "执行成功"),

    /** 执行失败. */
    FAIL(3, "执行失败");

    private final Integer code;
    private final String name;

    ZLcPackageExecute(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ZLcPackageExecute findByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcPackageExecute v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否终态 (EXECUTED/FAIL). */
    public boolean isTerminal() {
        return this == EXECUTED || this == FAIL;
    }

    /** 是否成功. */
    public boolean isSuccess() {
        return this == EXECUTED;
    }

    /** 是否失败. */
    public boolean isFail() {
        return this == FAIL;
    }
}