package com.zifang.z.lc.sdk.spi.task;

import com.zifang.z.lc.common.dto.task.MessageDTO;

/**
 * 任务消息发送服务 SPI — 蒸馏自 ace-platform-engine {@code TaskService}
 * （{@code com.c2f.ace.engine.define}），字段语义完全对齐.
 *
 * <p>用于「待办 / 已办 / 撤销」三类任务消息的发送.
 * 业务方继承 {@link AbstractTaskService} 实现本接口 — 内部委托 z-task 模块
 * 或第三方 IM / 邮件服务.
 *
 * <p>三种动作类型（{@link MessageDTO#TODO} / {@link MessageDTO#DONE} /
 * {@link MessageDTO#REVOKE}）分别对应 {@link #sendTodoMsg} /
 * {@link #sendDoneMsg} / {@link #sendRevokeMsg}.
 *
 * @author xuhf (distilled by zifang)
 */
public interface TaskService {

    /**
     * 发送待办消息.
     *
     * @param messageDTO 消息上下文
     */
    void sendTodoMsg(MessageDTO messageDTO);

    /**
     * 发送已办消息.
     */
    void sendDoneMsg(MessageDTO messageDTO);

    /**
     * 发送待办撤销消息（撤回 / 撤回审批）.
     */
    void sendRevokeMsg(MessageDTO messageDTO);
}
