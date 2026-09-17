package com.zifang.z.lc.common.enums;

/**
 * 请求类型枚举 — 蒸馏自 ace-platform-core
 * {@code RequestTypeEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>用于业务方标注跨进程调用的来源类型 — 业务方做监控 / 限流 / 安全审计时按 type 区分.
 *
 * @author zifang
 */
public enum ZLcRequestTypeEnum {

    /** HTTP 请求（Web 层直接调用） */
    HTTP(1, "http"),

    /** RPC 请求（Dubbo / z-rpc / gRPC 等跨进程调用） */
    RPC(2, "rpc");

    private final Integer type;
    private final String name;

    ZLcRequestTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * 按 type 数值查找（未匹配返回 null）.
     */
    public static ZLcRequestTypeEnum fromType(Integer type) {
        if (type == null) {
            return null;
        }
        for (ZLcRequestTypeEnum t : values()) {
            if (t.type.equals(type)) {
                return t;
            }
        }
        return null;
    }
}
