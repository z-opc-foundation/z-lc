package com.zifang.z.lc.core.domain;

/**
 * 字典 DO — 蒸馏自 ace-platform-core {@code DictItemDO}
 * （{@code com.c2f.ace.core.domain.entity}），去除 lombok + MyBatis Plus + BaseDO 依赖.
 *
 * <p>对应 {@code dict_item} 表 — 字典定义主表.
 *
 * @author zifang
 */
public class DictItemDO extends BaseDTO {

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
     * 字典 code（业务唯一）.
     */
    private String dictCode;

    /**
     * 字典描述.
     */
    private String dictDesc;

    /**
     * 字典树节点 code.
     */
    private String dictTreeNodeCode;

    /**
     * 是否远程字典（0 本地 / 1 远程 — 远程字典走 RPC / HTTP 拉取）.
     */
    private Integer remote;

    /**
     * 远程拉取 URL（remote=1 时使用）.
     */
    private String bondUrl;

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

    public String getDictTreeNodeCode() {
        return dictTreeNodeCode;
    }

    public void setDictTreeNodeCode(String dictTreeNodeCode) {
        this.dictTreeNodeCode = dictTreeNodeCode;
    }

    public Integer getRemote() {
        return remote;
    }

    public void setRemote(Integer remote) {
        this.remote = remote;
    }

    public String getBondUrl() {
        return bondUrl;
    }

    public void setBondUrl(String bondUrl) {
        this.bondUrl = bondUrl;
    }

    public boolean isRemote() {
        return remote != null && remote == 1;
    }
}
