package com.zifang.z.lc.common.utils;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * 表达式计算工具 — 蒸馏自 ace-platform-core
 * {@code QlExpressionUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>执行基于表达式的规则计算.
 * 蒸馏时移除了 ace 对 QLExpress / Lombok @Slf4j / commons-lang3 的依赖,
 * 改为 Spring SpEL 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>动态规则引擎表达式计算</li>
 *   <li>低代码平台的条件判断</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcQlExpressionUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    private ZLcQlExpressionUtil() {
    }

    /**
     * 执行规则表达式计算.
     *
     * @param paramMap 参数映射 (变量名 → 值)
     * @param rule     SpEL 规则表达式
     * @return 表达式计算结果; 表达式为空或异常时返回 false
     */
    public static boolean executeRule(Map<String, Object> paramMap, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return false;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramMap != null) {
                paramMap.forEach(context::setVariable);
            }
            Object result = PARSER.parseExpression(rule).getValue(context, Boolean.class);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // 规则执行异常时返回 false
            return false;
        }
    }
}
