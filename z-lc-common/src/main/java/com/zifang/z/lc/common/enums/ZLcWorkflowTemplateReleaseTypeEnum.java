package com.zifang.z.lc.common.enums;

/**
 * 工作流模板发布类型枚举 — 蒸馏自 ace-platform-core
 * {@code WorkflowTemplateReleaseType} ({@code com.c2f.ace.core.common}}.
 *
 * <p>用于低代码平台「工作流模板」发布管理：
 * <ul>
 *   <li>{@link #TEST} — 测试环境发布（仅供开发/测试联调）</li>
 *   <li>{@link #PROD} — 生产环境发布（正式上线）</li>
 * </ul>
 *
 * <p>注意：ace 原源码字段命名使用 {@code stopType/stopTypeName}（疑似历史遗留笔误，
 * 应为 {@code releaseType/releaseTypeName}）。蒸馏版按业务语义修正字段名。
 *
 * @author zifang
 */
public enum ZLcWorkflowTemplateReleaseTypeEnum {

    /** 测试环境发布. */
    TEST(1, "测试"),

    /** 正式环境发布. */
    PROD(2, "正式");

    private final Integer releaseType;
    private final String releaseTypeName;

    ZLcWorkflowTemplateReleaseTypeEnum(Integer releaseType, String releaseTypeName) {
        this.releaseType = releaseType;
        this.releaseTypeName = releaseTypeName;
    }

    public Integer getReleaseType() {
        return releaseType;
    }

    public String getReleaseTypeName() {
        return releaseTypeName;
    }

    public static ZLcWorkflowTemplateReleaseTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcWorkflowTemplateReleaseTypeEnum v : values()) {
            if (v.releaseType.equals(code)) {
                return v;
            }
        }
        return null;
    }
}