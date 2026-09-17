package com.zifang.z.lc.common.enums.dataasset;

/**
 * 标签发布状态枚举 — 蒸馏自 ace-platform-core
 * {@code LabelReleaseStateEnum} ({@code com.c2f.ace.core.enums.dataAsset}).
 *
 * <p>用于低代码平台"数据资产 / 标签"模块中发布任务的状态:
 * 与 {@link ZLcLabelReleaseResult} 字段值一致, 但语义不同 —
 * 本枚举用于"调度状态机", {@code ZLcLabelReleaseResult} 用于"最终结果".
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
public enum ZLcLabelReleaseState {

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

    ZLcLabelReleaseState(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ZLcLabelReleaseState fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ZLcLabelReleaseState v : values()) {
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
}