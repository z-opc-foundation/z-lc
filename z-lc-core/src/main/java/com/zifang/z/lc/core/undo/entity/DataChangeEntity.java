package com.zifang.z.lc.core.undo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_data_change 表实体: 记录运行态数据变更的前像/后像, 用来做服务端 undo/redo.
 * <p>
 * 为什么放关系表而不是复用 z_lc_event: z_lc_event 是**设计态**(实体/字段定义)的事件溯源链,
 * 带 parent_event_id 因果校验且由回放折叠; 运行态数据量大、按记录寻址、不需要折叠语义,
 * 混在一张表里会让两条链路互相拖累.
 */
@TableName("z_lc_data_change")
public class DataChangeEntity implements Serializable {

    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_code")
    private String tenantCode;

    @TableField("app_code")
    private String appCode;

    @TableField("entity_code")
    private String entityCode;

    /** 被改动记录的主键 (物理表的 id). */
    @TableField("record_id")
    private Long recordId;

    /** CREATE / UPDATE / DELETE. */
    private String operation;

    /** 变更前的整行快照 (JSON). CREATE 时为 null. */
    @TableField("before_image")
    private String beforeImage;

    /** 变更后的整行快照 (JSON). DELETE 时为 null. */
    @TableField("after_image")
    private String afterImage;

    /** 被 undo 掉时指向那条 undo 记录, 用于防止重复撤销. */
    @TableField("undone_by")
    private Long undoneBy;

    private String actor;

    @TableField("trace_id")
    private String traceId;

    @TableField("create_time")
    private Date createTime;

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

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(String beforeImage) {
        this.beforeImage = beforeImage;
    }

    public String getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(String afterImage) {
        this.afterImage = afterImage;
    }

    public Long getUndoneBy() {
        return undoneBy;
    }

    public void setUndoneBy(Long undoneBy) {
        this.undoneBy = undoneBy;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
