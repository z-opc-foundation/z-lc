package com.zifang.z.lc.common.enums;

/**
 * 模型类型枚举 — 蒸馏自 ace-platform-core
 * {@code Enums.ModelTypeFlag} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>用于区分低代码平台中模型的存储形态：
 * <ul>
 *   <li>物理模型（{@link #PHYSICAL_TYPE}）— 数据真实存储在业务数据库表</li>
 *   <li>虚拟模型（{@link #VIRTUAL_TYPE}）— 通过 RPC/聚合多表/调用服务拼装的视图</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcModelTypeFlag {

    /** 物理模型（对应真实业务数据库表） */
    PHYSICAL_TYPE(1),

    /** 虚拟模型（数据来自 RPC 聚合或多表关联） */
    VIRTUAL_TYPE(0);

    private final Integer code;

    ZLcModelTypeFlag(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    /**
     * 按 code 数值查找（未匹配返回 null）.
     */
    public static ZLcModelTypeFlag fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcModelTypeFlag f : values()) {
            if (f.code.equals(code)) {
                return f;
            }
        }
        return null;
    }
}
