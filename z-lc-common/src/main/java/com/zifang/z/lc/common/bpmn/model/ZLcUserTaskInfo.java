package com.zifang.z.lc.common.bpmn.model;

import java.io.Serializable;

/**
 * 用户任务节点 DTO — 蒸馏自 ace-platform-core
 * {@code UserTaskInfo} ({@code com.c2f.ace.core.bpmn.model}).
 *
 * <p>用于低代码平台"流程引擎"建模 — 描述 BPMN 中一个 UserTask 节点的基本属性.
 * 业务方可通过此 DTO 把建模结果存入数据库或跨节点传递.
 *
 * <p>字段语义:
 * <ul>
 *   <li>{@code id} — 任务唯一标识 (BPMN element id)</li>
 *   <li>{@code name} — 任务名称 (BPMN element name)</li>
 *   <li>{@code assignee} — 任务受理人 (BPMN camunda:assignee)</li>
 *   <li>{@code documentation} — 任务解释/文档 (BPMN documentation)</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcUserTaskInfo implements Serializable {

    private static final long serialVersionUID = 8953131735210957447L;

    /** 任务唯一标识 (BPMN element id). */
    private String id;

    /** 任务名称 (BPMN element name). */
    private String name;

    /** 任务受理人 (BPMN camunda:assignee). */
    private String assignee;

    /** 任务解释/文档 (BPMN documentation). */
    private String documentation;

    public ZLcUserTaskInfo() {
    }

    public ZLcUserTaskInfo(String id, String name, String assignee, String documentation) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.documentation = documentation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String toString() {
        return "ZLcUserTaskInfo{id='" + id + "', name='" + name + "', assignee='" + assignee + "'}";
    }
}