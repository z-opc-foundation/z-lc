package com.zifang.z.lc.common.enums.dataasset;

/**
 * 标签发布结果枚举 — 蒸馏自 ace-platform-core
 * {@code LabelReleaseResultEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中发布任务的结果:
 *
 * <ul>
 *   <li>{@link #UN_RELEASE} — 未发布</li>
 *   <li>{@link #RELEASE_ING} — 发布中</li>
 *   <li>{@link #RELEASED} — 已发布</li>
 *   <li>{@link #RELEASED_FAIL} — 发布失败</li>
 * </ul>
 *
 * @author gewenjie (zifang distillation)
 */
public enum ZLcLabelReleaseResult {

    /** 未发布. */
    UN_RELEASE(0, "未发布"),

    /** 发布中. */
    RELEASE_ING(1, "发布中"),

    /** 已发布. */
    RELEASED(2, "已发布"),

    /** 发布失败. */
    RELEASED_FAIL(3, "发布失败");

    private final Integer code;
    private final String description;

    ZLcLabelReleaseResult(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcLabelReleaseResult fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcLabelReleaseResult v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /** 是否终态 (RELEASED/RELEASED_FAIL). */
    public boolean isTerminal() {
        return this == RELEASED || this == RELEASED_FAIL;
    }

    /** 是否成功. */
    public boolean isSuccess() {
        return this == RELEASED;
    }

    /** 是否失败. */
    public boolean isFail() {
        return this == RELEASED_FAIL;
    }
}