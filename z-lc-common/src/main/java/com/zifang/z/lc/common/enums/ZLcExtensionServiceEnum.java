package com.zifang.z.lc.common.enums;

import com.zifang.z.lc.common.constants.ZLcExtensionServiceConstance;

/**
 * 扩展服务 SPI 枚举 — 蒸馏自 ace-platform-core
 * {@code ExtensionServiceEnum} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>每个枚举项对应一个 SPI code，与 {@link ZLcExtensionServiceConstance} 一一对应.
 * 业务方在 SPI 实现类上用 {@code @WfSpi(code=INIT_EXTENSION.getCode())}
 * （或直接引用对应常量）即可注册到引擎.
 *
 * <p>15 个 SPI code 涵盖低代码平台全生命周期的扩展点：
 * <ul>
 *   <li>数据：init / tempPre / tempPost / submitValidate / submitPre / submitPost</li>
 *   <li>查询：queryPre / queryPost</li>
 *   <li>修改：modifyPost</li>
 *   <li>删除：removeValidate / removePost</li>
 *   <li>审批：agreeValidate / agreePre / agreePost / rejectValidate</li>
 *   <li>暂存校验：tempValidate</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcExtensionServiceEnum {

    /** 初始化扩展. */
    INIT_EXTENSION(ZLcExtensionServiceConstance.INIT_EXTENSION, "初始化扩展"),

    /** 暂存前置扩展. */
    TEMP_PRE_EXTENSION(ZLcExtensionServiceConstance.TEMP_PRE_EXTENSION, "暂存前置扩展"),

    /** 暂存后置扩展. */
    TEMP_POST_EXTENSION(ZLcExtensionServiceConstance.TEMP_POST_EXTENSION, "暂存后置扩展"),

    /** 提交前校验扩展. */
    SUBMIT_VALIDATE_EXTENSION(ZLcExtensionServiceConstance.SUBMIT_VALIDATE_EXTENSION, "提交前校验扩展"),

    /** 提交前置扩展. */
    SUBMIT_PRE_EXTENSION(ZLcExtensionServiceConstance.SUBMIT_PRE_EXTENSION, "提交前置扩展"),

    /** 提交后置扩展. */
    SUBMIT_POST_EXTENSION(ZLcExtensionServiceConstance.SUBMIT_POST_EXTENSION, "提交后置扩展"),

    /** 查询前置扩展. */
    QUERY_PRE_EXTENSION(ZLcExtensionServiceConstance.QUERY_PRE_EXTENSION, "查询前置扩展"),

    /** 查询后置扩展. */
    QUERY_POST_EXTENSION(ZLcExtensionServiceConstance.QUERY_POST_EXTENSION, "查询后置扩展"),

    /** 删除前校验扩展. */
    REMOVE_VALIDATE_EXTENSION(ZLcExtensionServiceConstance.REMOVE_VALIDATE_EXTENSION, "删除前校验扩展"),

    /** 删除后置扩展. */
    REMOVE_POST_EXTENSION(ZLcExtensionServiceConstance.REMOVE_POST_EXTENSION, "删除后置扩展"),

    /** 修改后置扩展. */
    MODIFY_POST_EXTENSION(ZLcExtensionServiceConstance.MODIFY_POST_EXTENSION, "修改后置扩展"),

    /** 审批校验扩展. */
    AGREE_VALIDATE_EXTENSION(ZLcExtensionServiceConstance.AGREE_VALIDATE_EXTENSION, "审批校验扩展"),

    /** 审批前置扩展. */
    AGREE_PRE_EXTENSION(ZLcExtensionServiceConstance.AGREE_PRE_EXTENSION, "审批前置扩展"),

    /** 审批后置扩展. */
    AGREE_POST_EXTENSION(ZLcExtensionServiceConstance.AGREE_POST_EXTENSION, "审批后置扩展"),

    /** 驳回校验扩展. */
    REJECT_VALIDATE_EXTENSION(ZLcExtensionServiceConstance.REJECT_VALIDATE_EXTENSION, "驳回校验扩展"),

    /** 暂存校验扩展. */
    TEMP_VALIDATE_EXTENSION(ZLcExtensionServiceConstance.TEMP_VALIDATE_EXTENSION, "暂存校验扩展");

    private final String code;
    private final String name;

    ZLcExtensionServiceEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ZLcExtensionServiceEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcExtensionServiceEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
