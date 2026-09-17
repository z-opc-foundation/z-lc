package com.zifang.z.lc.common.dto.task;

import java.io.Serializable;

/**
 * 消息 DTO — 蒸馏自 ace-platform-engine {@code MessageDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：任务消息通知 / 流程触发时携带的上下文 — taskInfo + actionType +
 * appCode + modelCode + pageCode + pkId + businessKey + initiator.
 *
 * <p>{@link #actionType} 取值常量：
 * <ul>
 *   <li>{@link #TODO} — 待办（发送待办消息）</li>
 *   <li>{@link #DONE} — 已办（发送已办消息）</li>
 *   <li>{@link #REVOKE} — 撤销（发送撤销消息）</li>
 * </ul>
 *
 * @author xuhf (distilled by zifang)
 */
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 动作类型：待办. */
    public static final String TODO = "todo";

    /** 动作类型：已办. */
    public static final String DONE = "done";

    /** 动作类型：撤销. */
    public static final String REVOKE = "revoke";

    /**
     * 任务信息.
     */
    private TaskInfoDTO taskInfo;

    /**
     * 动作类型（{@link #TODO} / {@link #DONE} / {@link #REVOKE}）.
     */
    private String actionType;

    /**
     * 应用 code.
     */
    private String appCode;

    /**
     * 模型 code.
     */
    private String modelCode;

    /**
     * 页面 code.
     */
    private String pageCode;

    /**
     * 主键 id（数据行 id）.
     */
    private Long pkId;

    /**
     * 业务 key（流程启动用的 businessKey）.
     */
    private String businessKey;

    /**
     * 发起人 staffId.
     */
    private Long initiator;

    public TaskInfoDTO getTaskInfo() {
        return taskInfo;
    }

    public void setTaskInfo(TaskInfoDTO taskInfo) {
        this.taskInfo = taskInfo;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }

    public String getModelCode() {
        return modelCode;
    }

    public void setModelCode(String modelCode) {
        this.modelCode = modelCode;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public Long getPkId() {
        return pkId;
    }

    public void setPkId(Long pkId) {
        this.pkId = pkId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public Long getInitiator() {
        return initiator;
    }

    public void setInitiator(Long initiator) {
        this.initiator = initiator;
    }

    /**
     * 工厂方法 — 待办消息.
     */
    public static MessageDTO todo(TaskInfoDTO taskInfo, String appCode, String modelCode, Long initiator) {
        MessageDTO m = new MessageDTO();
        m.setTaskInfo(taskInfo);
        m.setActionType(TODO);
        m.setAppCode(appCode);
        m.setModelCode(modelCode);
        m.setInitiator(initiator);
        return m;
    }

    /**
     * 工厂方法 — 已办消息.
     */
    public static MessageDTO done(TaskInfoDTO taskInfo, String appCode, String modelCode, Long initiator) {
        MessageDTO m = new MessageDTO();
        m.setTaskInfo(taskInfo);
        m.setActionType(DONE);
        m.setAppCode(appCode);
        m.setModelCode(modelCode);
        m.setInitiator(initiator);
        return m;
    }

    /**
     * 工厂方法 — 撤销消息.
     */
    public static MessageDTO revoke(TaskInfoDTO taskInfo, String appCode, String modelCode, Long initiator) {
        MessageDTO m = new MessageDTO();
        m.setTaskInfo(taskInfo);
        m.setActionType(REVOKE);
        m.setAppCode(appCode);
        m.setModelCode(modelCode);
        m.setInitiator(initiator);
        return m;
    }
}
