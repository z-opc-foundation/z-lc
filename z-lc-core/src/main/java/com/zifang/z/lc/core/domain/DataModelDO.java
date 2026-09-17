package com.zifang.z.lc.core.domain;

/**
 * 数据模型 DO — 蒸馏自 ace-platform-core {@code DataModelDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code data_model} 表的内存表示 — 持久化对象包含父类 8 个通用字段 +
 * 模型特有的 6 个字段（appCode / modelName / modelCode / modelDesc / physicalFlag / treeNodeCode）.
 *
 * <p>注意：z-lc 已有的 {@code z-lc-core/executor/entity/EntityEntity} 是 event-sourcing 风格的
 * entity 状态对象，与本 DO 互补 — 本 DO 用于「直接持久化到 MySQL」场景.
 *
 * @author zifang
 */
public class DataModelDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型名称（中文名）.
     */
    private String modelName;

    /**
     * 模型 code（业务唯一标识）.
     */
    private String modelCode;

    /**
     * 模型描述.
     */
    private String modelDesc;

    /**
     * 是否为物理表.
     * <ul>
     *   <li>0 — 虚拟表（数据存 data_instance.data_json）</li>
     *   <li>1 — 物理表（数据存物理库表）</li>
     * </ul>
     */
    private Integer physicalFlag;

    /**
     * 模型树节点 code（用于左侧树展示）.
     */
    private String treeNodeCode;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getModelDesc() {
        return modelDesc;
    }

    public void setModelDesc(String modelDesc) {
        this.modelDesc = modelDesc;
    }

    public Integer getPhysicalFlag() {
        return physicalFlag;
    }

    public void setPhysicalFlag(Integer physicalFlag) {
        this.physicalFlag = physicalFlag;
    }

    public String getTreeNodeCode() {
        return treeNodeCode;
    }

    public void setTreeNodeCode(String treeNodeCode) {
        this.treeNodeCode = treeNodeCode;
    }

    /**
     * 是否物理表.
     */
    public boolean isPhysical() {
        return physicalFlag != null && physicalFlag == 1;
    }

    /**
     * 是否虚拟表.
     */
    public boolean isVirtual() {
        return physicalFlag == null || physicalFlag == 0;
    }
}
