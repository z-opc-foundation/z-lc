package com.zifang.z.lc.common.enums;

import com.zifang.util.core.meta.StatusCode;

/**
 * 流程运行时状态码 — 蒸馏自 ace-platform-core
 * {@code WorkflowRuntimeStatusCode} （{@code com.c2f.ace.core.common}}，
 * 字段语义完全对齐.
 *
 * <p>实现 {@link StatusCode} 接口，可被 {@code BusinessException(StatusCode, message)}
 * 直接使用，构造统一的流程运行时异常.
 *
 * @author zifang
 */
public enum ZLcWorkflowRuntimeStatusCode implements StatusCode {

    /** 流程业务异常（通用） */
    BIZ_EXCEPTION(1001, "流程业务异常"),

    /** 流程定义未找到（key 不存在） */
    WORKFLOW_DEF_NOT_FOUND(1002, "流程定义未找到"),

    /** 流程实例不存在（processInstanceId 无效） */
    PROCESS_INSTANCE_NOT_FOUND(1003, "流程实例不存在"),

    /** 用户无流程发起权限 */
    WORKFLOW_NO_PERMISSION(1004, "无流程权限"),

    /** 流程变量校验失败 */
    WORKFLOW_VARIABLE_INVALID(1005, "流程变量校验失败"),

    /** 流程流转异常（连线/网关/任务创建失败） */
    WORKFLOW_TRANSITION_ERROR(1006, "流程流转异常"),

    /** 任务分配失败（无候选/候选为空） */
    TASK_ASSIGN_FAILED(1007, "任务分配失败"),

    /** 流程超时 */
    WORKFLOW_TIMEOUT(1008, "流程超时");

    private final int code;
    private final String message;

    ZLcWorkflowRuntimeStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
