package com.zifang.z.lc.common.utils;

/**
 * 审批意见解析工具 — 蒸馏自 ace-platform-core
 * {@code ApprovalOpinionUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>从流程审批操作描述中提取审批意见文本.
 * 支持多种操作类型的解析: 普通审批、改派、委派、加签等.
 *
 * <p>典型场景：
 * <ul>
 *   <li>审批历史列表中展示纯审批意见 (去除操作前缀)</li>
 *   <li>审批意见统计分析</li>
 *   <li>消息通知中提取审批意见正文</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcApprovalOpinionUtil {

    private ZLcApprovalOpinionUtil() {
    }

    private static final String PREFIX_APPROVAL = "审批意见：";
    private static final String PREFIX_TURN = "改派流程,";
    private static final String PREFIX_DELEGATE = "委派流程,";
    private static final String PREFIX_ADD_MULTI = "增加会签人：";
    private static final String PREFIX_ADD_MULTI_V2 = "增加加签人：";

    /**
     * 从操作描述中提取审批意见.
     *
     * @param operationDesc 完整操作描述 (如 "审批意见：同意通过")
     * @return 提取后的意见文本; 无法识别时返回 null
     */
    public static String extractOpinion(String operationDesc) {
        if (operationDesc == null || operationDesc.isEmpty()) {
            return null;
        }

        if (operationDesc.contains(PREFIX_APPROVAL)) {
            int idx = operationDesc.indexOf(PREFIX_APPROVAL);
            return operationDesc.substring(idx + PREFIX_APPROVAL.length());
        } else if (operationDesc.contains(PREFIX_TURN)) {
            int idx = operationDesc.indexOf(PREFIX_TURN);
            return operationDesc.substring(idx + PREFIX_TURN.length());
        } else if (operationDesc.contains(PREFIX_DELEGATE)) {
            int idx = operationDesc.indexOf(PREFIX_DELEGATE);
            return operationDesc.substring(idx + PREFIX_DELEGATE.length());
        } else if (operationDesc.contains(PREFIX_ADD_MULTI)) {
            int idx = operationDesc.indexOf(PREFIX_ADD_MULTI);
            return operationDesc.substring(idx);
        } else if (operationDesc.contains(PREFIX_ADD_MULTI_V2)) {
            int idx = operationDesc.indexOf(PREFIX_ADD_MULTI_V2);
            return operationDesc.substring(idx);
        }
        return null;
    }
}
