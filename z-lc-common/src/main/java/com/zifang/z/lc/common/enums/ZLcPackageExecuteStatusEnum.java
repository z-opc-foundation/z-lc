package com.zifang.z.lc.common.enums;

/**
 * 应用包执行状态枚举 — 蒸馏自 ace-platform-core
 * {@code PackageExecuteStatusEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于低代码平台「应用包导入/导出」操作的执行状态展示.
 *
 * @author zifang
 */
public enum ZLcPackageExecuteStatusEnum {

    /** 初始化. */
    INIT("init", "初始化"),

    /** 执行中. */
    EXECUTING("executing", "执行中"),

    /** 执行成功. */
    SUCCESS("success", "执行成功"),

    /** 执行失败. */
    FAIL("fail", "执行失败");

    private final String code;
    private final String desc;

    ZLcPackageExecuteStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ZLcPackageExecuteStatusEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcPackageExecuteStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 按 code 反查中文描述（未匹配返回 null）.
     */
    public static String getDescByCode(String code) {
        ZLcPackageExecuteStatusEnum item = getByCode(code);
        return item == null ? null : item.desc;
    }

    /**
     * 是否终态（成功 / 失败）.
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAIL;
    }
}
