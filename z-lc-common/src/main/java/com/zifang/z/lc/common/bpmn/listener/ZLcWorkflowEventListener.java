package com.zifang.z.lc.common.bpmn.listener;

import java.util.Map;

/**
 * 流程事件监听器接口 — 蒸馏自 ace-platform-core
 * {@code DataModelSyncMqConsumer / FormChangeSnapshotMqConsumer / OperationRecordMqConsumer}
 * ({@code com.c2f.ace.core.middleware.mq}).
 *
 * <p>ace 平台通过 MQ 消费者监听流程事件 (启动/完成/任务创建/任务完成等).
 * z-lc 将此抽象为统一的事件监听器接口, 业务方实现此接口即可在流程生命周期
 * 关键节点注入自定义逻辑.
 *
 * <p>支持的事件类型 (对齐 ace {@link com.zifang.z.lc.common.constance.ZLcConstance}):
 * <ul>
 *   <li>{@code processStarted} — 流程实例启动</li>
 *   <li>{@code processCompleted} — 流程实例完成</li>
 *   <li>{@code taskCreated} — 任务创建</li>
 *   <li>{@code taskCompleted} — 任务完成</li>
 *   <li>{@code taskAgree} — 任务审批通过</li>
 *   <li>{@code taskRefused} — 任务审批拒绝</li>
 * </ul>
 *
 * @author zifang
 */
public interface ZLcWorkflowEventListener {

    /**
     * 流程事件回调.
     *
     * @param eventType         事件类型 (processStarted/taskCreated 等)
     * @param processInstanceId 流程实例 ID
     * @param taskId            任务 ID (任务级事件时有值, 流程级事件时为 null)
     * @param variables         流程变量快照 (可为空)
     */
    void onEvent(String eventType, String processInstanceId, String taskId,
                 Map<String, Object> variables);

    /**
     * 监听器名称 (用于日志/调试).
     */
    default String listenerName() {
        return this.getClass().getSimpleName();
    }
}
