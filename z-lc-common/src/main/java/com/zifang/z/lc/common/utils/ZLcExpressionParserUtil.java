package com.zifang.z.lc.common.utils;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表达式解析工具 — 蒸馏自 ace-platform-core
 * {@code ExpressionParserUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 BPMN 条件表达式的提取和计算能力.
 * 蒸馏时移除了 ace 对 MVEL 的依赖, 改为 Spring SpEL 实现.
 *
 * <p>支持的表达式格式：
 * <ul>
 *   <li>{@code ${expression}} — 从 BPMN 条件中提取表达式</li>
 *   <li>{@code #variableName} — SpEL 变量引用</li>
 * </ul>
 *
 * <p>典型场景：
 * <ul>
 *   <li>BPMN 排他网关条件解析 (如 {@code ${days > 3}})</li>
 *   <li>表单字段联动规则计算</li>
 *   <li>审批人逻辑表达式求值</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcExpressionParserUtil {

    private ZLcExpressionParserUtil() {
    }

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final Pattern EXPR_PATTERN = Pattern.compile("\\$\\{(.*?)}");

    /**
     * 从 BPMN 条件表达式中提取纯表达式 (去除 ${} 包裹).
     *
     * @param bpmnExpression BPMN 条件表达式 (如 "${days > 3}")
     * @return 纯表达式 (如 "days > 3"); 无法提取时返回 null
     */
    public static String extractExpression(String bpmnExpression) {
        if (bpmnExpression == null || bpmnExpression.isEmpty()) {
            return null;
        }
        Matcher matcher = EXPR_PATTERN.matcher(bpmnExpression);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return bpmnExpression;
    }

    /**
     * 计算 SpEL 表达式并返回 Boolean 结果.
     *
     * @param expression SpEL 表达式
     * @param variables  变量上下文
     * @return 表达式计算结果
     */
    public static boolean evaluateExpression(String expression, Map<String, Object> variables) {
        try {
            Expression exp = PARSER.parseExpression(expression);
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    ctx.setVariable(entry.getKey(), entry.getValue());
                }
            }
            Boolean result = exp.getValue(ctx, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算 SpEL 表达式并返回 Object 结果.
     *
     * @param expression SpEL 表达式
     * @param variables  变量上下文
     * @return 表达式计算结果; 解析失败时返回 null
     */
    public static Object evaluateToObject(String expression, Map<String, Object> variables) {
        try {
            Expression exp = PARSER.parseExpression(expression);
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    ctx.setVariable(entry.getKey(), entry.getValue());
                }
            }
            return exp.getValue(ctx);
        } catch (Exception e) {
            return null;
        }
    }
}
