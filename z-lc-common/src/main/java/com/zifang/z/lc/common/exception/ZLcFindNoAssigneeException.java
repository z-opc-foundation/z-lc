package com.zifang.z.lc.common.exception;

/**
 * 找不到审批人异常 — 蒸馏自 ace-platform-core
 * {@code FindNoAssigneeException} ({@code com.c2f.ace.core.exception}).
 *
 * <p>在流程审批人解析逻辑中，当无法找到合适的审批人时抛出此异常.
 * 典型场景：流程定义中配置的审批人查询条件无匹配结果，
 * 或 Apex 审批人服务返回空列表.
 *
 * @author zifang
 */
public class ZLcFindNoAssigneeException extends RuntimeException {

    public ZLcFindNoAssigneeException(String message) {
        super(message);
    }

    public ZLcFindNoAssigneeException(String message, Throwable cause) {
        super(message, cause);
    }
}
