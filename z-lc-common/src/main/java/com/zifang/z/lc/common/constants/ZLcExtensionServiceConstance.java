package com.zifang.z.lc.common.constants;

/**
 * 扩展服务 SPI code 常量 — 蒸馏自 ace-platform-core
 * {@code ExtensionServiceConstance} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>每个常量对应 {@link com.zifang.z.lc.common.enums.ZLcExtensionServiceEnum} 的一项枚举值，
 * 用作 SPI 标识字符串（{@code @WfSpi(code=...)}）。
 *
 * @author zifang
 */
public final class ZLcExtensionServiceConstance {

    /** 初始化扩展 SPI code. */
    public static final String INIT_EXTENSION = "init";

    /** 暂存前置扩展 SPI code. */
    public static final String TEMP_PRE_EXTENSION = "tempPre";

    /** 暂存后置扩展 SPI code. */
    public static final String TEMP_POST_EXTENSION = "tempPost";

    /** 提交前校验扩展 SPI code. */
    public static final String SUBMIT_VALIDATE_EXTENSION = "submitValidate";

    /** 提交前置扩展 SPI code. */
    public static final String SUBMIT_PRE_EXTENSION = "submitPre";

    /** 提交后置扩展 SPI code. */
    public static final String SUBMIT_POST_EXTENSION = "submitPost";

    /** 查询前置扩展 SPI code. */
    public static final String QUERY_PRE_EXTENSION = "queryPre";

    /** 查询后置扩展 SPI code. */
    public static final String QUERY_POST_EXTENSION = "queryPost";

    /** 删除前校验扩展 SPI code. */
    public static final String REMOVE_VALIDATE_EXTENSION = "removeValidate";

    /** 删除后置扩展 SPI code. */
    public static final String REMOVE_POST_EXTENSION = "removePostValidate";

    /** 修改后置扩展 SPI code. */
    public static final String MODIFY_POST_EXTENSION = "modifyPost";

    /** 审批校验扩展 SPI code. */
    public static final String AGREE_VALIDATE_EXTENSION = "agreeValidate";

    /** 审批前置扩展 SPI code. */
    public static final String AGREE_PRE_EXTENSION = "agreePre";

    /** 审批后置扩展 SPI code. */
    public static final String AGREE_POST_EXTENSION = "agreePost";

    /** 驳回校验扩展 SPI code. */
    public static final String REJECT_VALIDATE_EXTENSION = "rejectValidate";

    /** 暂存前校验扩展 SPI code. */
    public static final String TEMP_VALIDATE_EXTENSION = "tempValidate";

    private ZLcExtensionServiceConstance() {
        // 常量类，禁止实例化
    }
}
