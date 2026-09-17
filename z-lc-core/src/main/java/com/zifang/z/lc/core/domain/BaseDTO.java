package com.zifang.z.lc.core.domain;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体基础字段 — 蒸馏自 ace-platform-core {@code BaseDO}
 * （{@code com.c2f.boot.starter.datasource.base}），去除 lombok 与 MyBatis Plus 依赖，
 * 改为 z-lc 手写 getter/setter 风格的纯 DTO.
 *
 * <p>每个持久化对象（DataModel / DataField / DictItem / DictItemValue /
 * PageTemplate / WorkflowTemplate / ServiceItem / App）的父类，
 * 提供 8 个通用字段（id + tenantCode + 4 个审计字段 + 删除标志 + 扩展）.
 *
 * @author zifang
 */
public abstract class BaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 id.
     */
    private Long id;

    /**
     * 租户 code.
     */
    private String tenantCode;

    /**
     * 创建时间.
     */
    private Date createTime;

    /**
     * 更新时间.
     */
    private Date updateTime;

    /**
     * 创建人.
     */
    private String createBy;

    /**
     * 更新人.
     */
    private String updateBy;

    /**
     * 软删标记（0 未删 / 1 已删）— 模仿 ace BaseDO 的 deleted 字段.
     */
    private Integer deleted;

    /**
     * 扩展信息（JSON 字符串 — 用于业务方自定义字段）.
     */
    private String extend;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }
}
