package com.zifang.z.lc.core.domain;

/**
 * 物料 DO — 蒸馏自 ace-platform-core
 * {@code MaterialDO} （{@code com.c2f.ace.core.domain.entity}}，
 * 字段语义完全对齐.
 *
 * <p>对应 {@code material} 表 — 文件/附件元数据存储（OSS 引用），
 * 用于：
 * <ul>
 *   <li>表单附件上传（图片/文档/视频）</li>
 *   <li>公私分明细 — {@code belongType=public}（公共素材库）/ {@code private}（应用私有）</li>
 *   <li>OSS URL 持久化 — 业务方按需接入 z-oss 或第三方存储</li>
 * </ul>
 *
 * @author zifang
 */
public class MaterialDO extends BaseDTO {

    private static final long serialVersionUID = 1L;

    /**
     * 租户 code.
     */
    private String tenantCode;

    /**
     * 归属类型 — {@code "public"}（公共素材库）/ {@code "private"}（应用私有）.
     */
    private String belongType;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 物料名称.
     */
    private String name;

    /**
     * 物料描述.
     */
    private String description;

    /**
     * 物料地址（OSS URL）.
     */
    private String ossUrl;

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getBelongType() {
        return belongType;
    }

    public void setBelongType(String belongType) {
        this.belongType = belongType;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOssUrl() {
        return ossUrl;
    }

    public void setOssUrl(String ossUrl) {
        this.ossUrl = ossUrl;
    }
}
