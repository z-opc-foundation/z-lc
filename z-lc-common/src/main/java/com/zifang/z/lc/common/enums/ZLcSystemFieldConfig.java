package com.zifang.z.lc.common.enums;

/**
 * 系统字段配置枚举 — 蒸馏自 ace-platform-core
 * {@code SystemFieldConfigEnum} ({@code com.c2f.ace.core.enums}).
 *
 * <p>用于低代码平台"模型管理" — 标识系统级特殊字段 (id / gmt_create / gmt_modify /
 * create_by / modify_by / org_id / is_deleted / model_code / app_code / period_id)
 * 的静态值/默认值配置 (JSON 字符串).
 *
 * <p>字段语义:
 * <ul>
 *   <li>{@code fieldCode} — 物理字段编码</li>
 *   <li>{@code staticValueConfig} — 静态值配置 JSON (如 is_deleted 默认 0)</li>
 *   <li>{@code defaultValueConfig} — 默认值配置 JSON (含 script 取值策略)</li>
 * </ul>
 *
 * <p>业务方可通过 {@link #isSystemField(String)} 判断某字段是否系统字段 (不可编辑).
 *
 * @author zifang
 */
public enum ZLcSystemFieldConfig {

    /** 创建时间. */
    GMT_CREATE("gmt_create", null,
            "{\"value\": null, \"script\": \"${CURRENT_TIMM}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 修改时间. */
    GMT_MODIFY("gmt_modify", null,
            "{\"value\": null, \"script\": \"${CURRENT_TIMM}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 创建人. */
    CREATE_BY("create_by", null,
            "{\"value\": null, \"script\": \"${OPERATOR}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 修改人. */
    MODIFY_BY("modify_by", null,
            "{\"value\": null, \"script\": \"${OPERATOR}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 创建人机构. */
    CREATE_ORG_ID("create_org_id", null,
            "{\"value\": null, \"script\": \"${ORG_ID}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 当前机构. */
    ORG_ID("org_id", null,
            "{\"value\": null, \"script\": \"${ORG_ID}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 修改人机构. */
    MODIFY_ORG_ID("modify_org_id", null,
            "{\"value\": null, \"script\": \"${ORG_ID}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 逻辑删除标记. */
    IS_DELETED("is_deleted",
            "{\"value\": \"0\", \"script\": null, \"fetchType\": null, \"enableFlag\": true, \"enableFilterFlag\": true}",
            null),

    /** 模型编码. */
    MODEL_CODE("model_code",
            "{\"value\": \"ZLc20241018105048\", \"script\": null, \"fetchType\": null, \"enableFlag\": true, \"enableFilterFlag\": false}",
            null),

    /** 应用编码. */
    APP_CODE("app_code", null,
            "{\"value\": null, \"script\": \"${CURRENT_APP_CODE}\", \"fetchType\": \"strategy\", \"enableFlag\": true, \"enableFilterFlag\": false}"),

    /** 主键. */
    ID("id", null, null),

    /** 期间 ID. */
    PERIOD_ID("period_id", null, null);

    private final String fieldCode;
    private final String staticValueConfig;
    private final String defaultValueConfig;

    ZLcSystemFieldConfig(String fieldCode, String staticValueConfig, String defaultValueConfig) {
        this.fieldCode = fieldCode;
        this.staticValueConfig = staticValueConfig;
        this.defaultValueConfig = defaultValueConfig;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getStaticValueConfig() {
        return staticValueConfig;
    }

    public String getDefaultValueConfig() {
        return defaultValueConfig;
    }

    public static ZLcSystemFieldConfig getByCode(String fieldCode) {
        if (fieldCode == null) {
            return null;
        }
        for (ZLcSystemFieldConfig v : values()) {
            if (v.fieldCode.equals(fieldCode)) {
                return v;
            }
        }
        return null;
    }

    /** 是否系统字段 (业务方不可编辑). */
    public static boolean isSystemField(String fieldCode) {
        return getByCode(fieldCode) != null;
    }
}