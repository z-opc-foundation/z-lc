package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;
import java.util.Map;

/**
 * 数据模型树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code DataModelTreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「模型管理」树节点的附加信息 — appCode / modelId / modelCode /
 * physicalFlag（虚拟 vs 物理）/ extend（自定义 Map 扩展）.
 *
 * @author zifang
 */
public class DataModelTreeNodeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型 id（数据库主键）.
     */
    private Long modelId;

    /**
     * 模型标识.
     */
    private String modelCode;

    /**
     * 模型描述.
     */
    private String modelDesc;

    /**
     * 模型名称.
     */
    private String modelName;

    /**
     * 树节点 id.
     */
    private String treeNodeId;

    /**
     * 是否为物理表.
     * <ul>
     *   <li>0：虚拟表（数据存 data_instance.data_json）</li>
     *   <li>1：物理表（数据存物理库表）</li>
     * </ul>
     */
    private Integer physicalFlag;

    /**
     * 扩展信息（自定义 — 例如字段定义快照）.
     */
    private Map<String, Object> extend;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }

    public Integer getPhysicalFlag() {
        return physicalFlag;
    }

    public void setPhysicalFlag(Integer physicalFlag) {
        this.physicalFlag = physicalFlag;
    }

    public Map<String, Object> getExtend() {
        return extend;
    }

    public void setExtend(Map<String, Object> extend) {
        this.extend = extend;
    }
}
