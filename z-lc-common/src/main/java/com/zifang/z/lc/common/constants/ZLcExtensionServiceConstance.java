package com.zifang.z.lc.common.constants;

/**
 * 扩展服务常量 — 蒸馏自 ace-platform-core
 * {@code ExtensionServiceConstance} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义低代码平台各业务环节的扩展点标识,
 * 用于 SPI 注册中心按扩展点名称路由到对应的业务实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>表单提交前/后的扩展点路由</li>
 *   <li>查询/删除/审批等环节的前置/后置拦截</li>
 *   <li>扩展点配置中心的 key 注册</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcExtensionServiceConstance {

    private ZLcExtensionServiceConstance() {
    }

    /** 初始化扩展. */
    public static final String INIT_EXTENSION = "init";

    /** 暂存前置扩展. */
    public static final String TEMP_PRE_EXTENSION = "tempPre";

    /** 暂存后置扩展. */
    public static final String TEMP_POST_EXTENSION = "tempPost";

    /** 提交前校验扩展. */
    public static final String SUBMIT_VALIDATE_EXTENSION = "submitValidate";

    /** 提交前置扩展. */
    public static final String SUBMIT_PRE_EXTENSION = "submitPre";

    /** 提交后置扩展. */
    public static final String SUBMIT_POST_EXTENSION = "submitPost";

    /** 查询前置扩展. */
    public static final String QUERY_PRE_EXTENSION = "queryPre";

    /** 查询后置扩展. */
    public static final String QUERY_POST_EXTENSION = "queryPost";

    /** 删除前校验扩展. */
    public static final String REMOVE_VALIDATE_EXTENSION = "removeValidate";

    /** 删除后置扩展. */
    public static final String REMOVE_POST_EXTENSION = "removePostValidate";

    /** 修改后置扩展. */
    public static final String MODIFY_POST_EXTENSION = "modifyPost";

    /** 审批校验扩展. */
    public static final String AGREE_VALIDATE_EXTENSION = "agreeValidate";

    /** 审批前置扩展. */
    public static final String AGREE_PRE_EXTENSION = "agreePre";

    /** 审批后置扩展. */
    public static final String AGREE_POST_EXTENSION = "agreePost";

    /** 驳回校验扩展. */
    public static final String REJECT_VALIDATE_EXTENSION = "rejectValidate";

    /** 暂存前校验扩展. */
    public static final String TEMP_VALIDATE_EXTENSION = "tempValidate";
}
