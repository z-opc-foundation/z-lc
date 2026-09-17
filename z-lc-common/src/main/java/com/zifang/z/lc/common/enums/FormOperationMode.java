package com.zifang.z.lc.common.enums;

/**
 * 表单操作模式枚举.
 *
 * <p>蒸馏自 ace-platform-engine {@code ModeEnum}
 * （{@code com.c2f.ace.engine.common}），字段语义完全对齐.
 *
 * <p>与 {@link com.zifang.z.lc.sdk.define.ModelDataSaveMode} 的区别：
 * <ul>
 *   <li>{@code ModelDataSaveMode} — 数据保存模式（普通/暂存/流程中）</li>
 *   <li>{@code FormOperationMode} — 表单操作模式（暂存/提交/审批修改）</li>
 * </ul>
 *
 * <p>业务场景：「表单提交」按钮的下拉选项 — 暂存后用户可继续编辑；提交后
 * 流程启动；审批修改用于流程回退节点.
 *
 * @author xuhf (distilled by zifang)
 */
public enum FormOperationMode {

    /**
     * 暂存（保留为草稿，不进流程）.
     */
    TEMP(1, "暂存"),

    /**
     * 提交（正式启动流程）.
     */
    SUBMIT(2, "提交"),

    /**
     * 审批修改保存（流程回退节点 — 申请人按审批意见修改后保存）.
     */
    AUDIT(3, "审批修改保存");

    private final Integer mode;
    private final String modeName;

    FormOperationMode(Integer mode, String modeName) {
        this.mode = mode;
        this.modeName = modeName;
    }

    public Integer getMode() {
        return mode;
    }

    public String getModeName() {
        return modeName;
    }

    /**
     * 按 mode 数值查找（未匹配返回 null）.
     */
    public static FormOperationMode fromMode(Integer mode) {
        if (mode == null) {
            return null;
        }
        for (FormOperationMode m : values()) {
            if (m.mode.equals(mode)) {
                return m;
            }
        }
        return null;
    }
}
