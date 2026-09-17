package com.zifang.z.lc.common.task;

/**
 * 基础数据处理任务 DTO — 蒸馏自 ace-platform-core
 * {@code BaseDataProcessTask} ({@code com.c2f.ace.core.middleware.task}).
 *
 * <p>用于低代码平台"数据导入/导出"模块 — 任务的最小公共父类.
 * 业务方具体的导入/导出任务应继承本类.
 *
 * <p>字段语义:
 * <ul>
 *   <li>{@code id} — 任务 ID</li>
 *   <li>{@code taskType} — 任务类型 (IMPORT / EXPORT)</li>
 *   <li>{@code taskStatus} — 任务状态</li>
 * </ul>
 *
 * @author zifang
 */
public abstract class ZLcBaseDataProcessTask {

    /** 任务 ID. */
    private Long id;

    /** 任务类型 (IMPORT / EXPORT). */
    private String taskType;

    /** 任务状态. */
    private String taskStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
}