package com.zifang.z.lc.common.utils;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * SpEL 表达式工具 — 蒸馏自 ace-platform-core
 * {@code SpelUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 Spring Expression Language (SpEL) 表达式的便捷解析和计算.
 * 适用于低代码平台的流程条件判断、表单字段联动规则、数据校验规则等场景.
 *
 * <p>典型场景：
 * <ul>
 *   <li>BPMN 排他网关条件表达式计算</li>
 *   <li>表单字段值联动规则 (如: 天数 > 3 → 需要总经理审批)</li>
 *   <li>数据校验规则 (如: 金额 > 10000 → 触发风控)</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcSpelUtil {

    private ZLcSpelUtil() {
    }

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 计算 SpEL 表达式并返回 Boolean 结果.
     *
     * @param expression SpEL 表达式 (如 "#amount > 10000")
     * @param variables  变量上下文 (变量名 → 值)
     * @return 表达式计算结果; 解析失败时返回 false
     */
    public static boolean evaluate(String expression, Map<String, Object> variables) {
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
     * 计算 SpEL 表达式并返回 String 结果.
     *
     * @param expression SpEL 表达式 (如 "#name + '_' + #type")
     * @param variables  变量上下文
     * @return 表达式计算结果; 解析失败时返回 null
     */
    public static String evaluateToString(String expression, Map<String, Object> variables) {
        try {
            Expression exp = PARSER.parseExpression(expression);
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            if (variables != null) {
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    ctx.setVariable(entry.getKey(), entry.getValue());
                }
            }
            return exp.getValue(ctx, String.class);
        } catch (Exception e) {
            return null;
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
