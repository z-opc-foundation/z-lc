package com.zifang.z.lc.sdk.spi.task.adapter;

import com.zifang.z.lc.common.dto.task.MessageDTO;

/**
 * 任务服务 RPC 适配器接口 — 蒸馏自 ace-platform-engine {@code TaskServiceAdapter}
 * （{@code com.c2f.ace.engine.adapter}），字段语义完全对齐.
 *
 * <p>本接口是 {@link com.zifang.z.lc.sdk.spi.task.TaskService} 的 RPC 风格对应物 —
 * 用于「业务方实现 TaskService SPI → 引擎暴露 RPC 接口给跨进程调用方」场景.
 *
 * <p>两个核心实现类（典型）：
 * <ul>
 *   <li>{@link ZLcRpcTaskServiceAdapterProvider} — 服务端实现，把 TaskService 调用委派给 AbstractTaskService</li>
 *   <li>{@link ZLcRpcTaskServiceAdapterInvoker} — 客户端代理，通过 RPC 跨进程调用</li>
 * </ul>
 *
 * @author xuhf (distilled by zifang)
 */
public interface TaskServiceAdapter {

    /**
     * 发送待办消息.
     */
    void sendTodoMsg(MessageDTO messageDTO);

    /**
     * 发送已办消息.
     */
    void sendDoneMsg(MessageDTO messageDTO);

    /**
     * 发送待办撤销消息.
     */
    void sendRevokeMsg(MessageDTO messageDTO);
}
