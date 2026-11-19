package com.zifang.z.lc.core.undo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * z_lc_data_change 表实体: 记录运行态数据变更的前像/后像, 用来做服务端 undo/redo.
 * <p>
 * 为什么放关系表而不是复用 z_lc_event: z_lc_event 是**设计态**(实体/字段定义)的事件溯源链,
 * 带 parent_event_id 因果校验且由回放折叠; 运行态数据量大、按记录寻址、不需要折叠语义,
 * 混在一张表里会让两条链路互相拖累.
 */
@BizModel(name = "z_lc_data_change 表实体: 记录运行态数据变更的前像/后像, 用来做服务端 undo/redo", module = "z-lc", description = "z_lc_data_change 表实体: 记录运行态数据变更的前像/后像, 用来做服务端 undo/redo. 为什么放关系表而不是复用 z_lc_event: z_lc_event 是设计态(实体/字段定义)的事件溯源链, 带 parent_event_id 因果校验且由回放折叠; 运行态数据量大、按记录寻址、不需要折叠语义, 混在一张表里会让两条链路互相拖累.")
@TableName("z_lc_data_change")
public class DataChangeEntity implements Serializable {

    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;

    @TableField("app_code")
    @BizField(name = "appCode", type = BizField.FieldType.STRING)
    private String appCode;

    @TableField("entity_code")
    @BizField(name = "entityCode", type = BizField.FieldType.STRING)
    private String entityCode;

    /** 被改动记录的主键 (物理表的 id). */
    @TableField("record_id")
    @BizField(name = "被改动记录的主键 (物理表的 id)", type = BizField.FieldType.INTEGER)
    private Long recordId;

    /** CREATE / UPDATE / DELETE. */
    @BizField(name = "CREATE / UPDATE / DELETE", type = BizField.FieldType.STRING)
    private String operation;

    /** 变更前的整行快照 (JSON). CREATE 时为 null. */
    @TableField("before_image")
    @BizField(name = "变更前的整行快照 (JSON). CREATE 时为 null", type = BizField.FieldType.STRING)
    private String beforeImage;

    /** 变更后的整行快照 (JSON). DELETE 时为 null. */
    @TableField("after_image")
    @BizField(name = "变更后的整行快照 (JSON). DELETE 时为 null", type = BizField.FieldType.STRING)
    private String afterImage;

    /** 被 undo 掉时指向那条 undo 记录, 用于防止重复撤销. */
    @TableField("undone_by")
    @BizField(name = "被 undo 掉时指向那条 undo 记录, 用于防止重复撤销", type = BizField.FieldType.INTEGER)
    private Long undoneBy;

    @BizField(name = "actor", type = BizField.FieldType.STRING)
    private String actor;

    @TableField("trace_id")
    @BizField(name = "traceId", type = BizField.FieldType.STRING)
    private String traceId;

    @TableField("create_time")
    @BizField(name = "createTime", type = BizField.FieldType.DATETIME)
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



