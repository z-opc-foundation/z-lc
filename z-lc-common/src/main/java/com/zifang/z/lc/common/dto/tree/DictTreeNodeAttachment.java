package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;

/**
 * 字典树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code DictTreeNodeAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐.
 *
 * <p>用途：「字典管理」树节点的附加信息 — dictCode / dictName / dictDesc /
 * treeNodeId / extend.
 *
 * @author zifang
 */
public class DictTreeNodeAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 字典名称.
     */
    private String dictName;

    /**
     * 字典编码（业务唯一标识）.
     */
    private String dictCode;

    /**
     * 字典描述.
     */
    private String dictDesc;

    /**
     * 树节点 id.
     */
    private String treeNodeId;

    /**
     * 扩展（JSON 字符串 — 保留 ace 原生字符串语义）.
     */
    private String extend;

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictDesc() {
        return dictDesc;
    }

    public void setDictDesc(String dictDesc) {
        this.dictDesc = dictDesc;
    }

    public String getTreeNodeId() {
        return treeNodeId;
    }

    public void setTreeNodeId(String treeNodeId) {
        this.treeNodeId = treeNodeId;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }
}
