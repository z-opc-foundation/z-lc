package com.zifang.z.lc.common.enums;

/**
 * 扩展服务枚举 — 蒸馏自 ace-platform-core
 * {@code ExtensionServiceEnum} ({@code com.c2f.ace.core.common}).
 *
 * <p>定义低代码平台各业务环节的扩展点标识和描述.
 *
 * @author zifang
 */
public enum ZLcExtensionServiceEnum {

    INIT_EXTENSION("init", "初始化扩展"),
    TEMP_PRE_EXTENSION("tempPre", "暂存前置扩展"),
    TEMP_POST_EXTENSION("tempPost", "暂存后置扩展"),
    SUBMIT_VALIDATE_EXTENSION("submitValidate", "提交前校验扩展"),
    SUBMIT_PRE_EXTENSION("submitPre", "提交前置扩展"),
    SUBMIT_POST_EXTENSION("submitPost", "提交后置扩展"),
    QUERY_PRE_EXTENSION("queryPre", "查询前置扩展"),
    QUERY_POST_EXTENSION("queryPost", "查询后置扩展"),
    REMOVE_VALIDATE_EXTENSION("removeValidate", "删除前校验扩展"),
    REMOVE_POST_EXTENSION("removePost", "删除后置扩展"),
    MODIFY_POST_EXTENSION("modifyPost", "修改后置扩展"),
    AGREE_VALIDATE_EXTENSION("agreeValidate", "审批前校验扩展"),
    AGREE_PRE_EXTENSION("agreePre", "审批前置扩展"),
    AGREE_POST_EXTENSION("agreePost", "审批后置扩展"),
    REJECT_VALIDATE_EXTENSION("rejectValidate", "驳回前校验扩展"),
    TEMP_VALIDATE_EXTENSION("tempValidate", "暂存校验扩展");

    private final String code;
    private final String description;

    ZLcExtensionServiceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcExtensionServiceEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcExtensionServiceEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }
}
