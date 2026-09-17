package com.zifang.z.lc.sdk.reflect;

import java.util.Map;

/**
 * QL 表达式求值工具 — 蒸馏自 ace-platform-engine {@code QlExpressionUtil}
 * （{@code com.c2f.ace.engine}），但避免 alibaba QL / spring-expression 依赖，
 * 实现一个极简子集，仅支持业务路由最常用的操作符.
 *
 * <p>设计哲学：
 * <ul>
 *   <li>支持 {@code DataModelServiceInfo.expression} 字段做轻量路由评估 —
 *       决定某个服务实现是否命中当前 (appCode, modelCode, context) 上下文</li>
 *   <li>只支持业务路由所需的最小操作符集：
 *       <ul>
 *         <li>比较：{@code ==} / {@code !=} / {@code >} / {@code <} / {@code >=} / {@code <=}</li>
 *         <li>逻辑：{@code &&} / {@code ||} / {@code !}</li>
 *         <li>方法调用：{@code contains} / {@code startsWith} / {@code endsWith}</li>
 *       </ul>
 *   </li>
 *   <li>支持字符串字面量（{@code '...'} 或 {@code "..."}）和数字字面量</li>
 *   <li>解析失败时返回 false（fail-safe）</li>
 * </ul>
 *
 * <p>典型用法（{@code DataModelServiceInfo.expression} 求值）：
 * <pre>{@code
 *   boolean hit = QlExpressionUtil.executeRule(
 *       Map.of("tenantCode", "a", "modelCode", "user", "version", 2),
 *       "tenantCode == 'a' && version >= 1");
 *   // → true
 *
 *   // 方法调用形式：
 *   boolean hit2 = QlExpressionUtil.executeRule(
 *       Map.of("modelCode", "order_detail"),
 *       "modelCode.startsWith('order_')");
 *   // → true
 * }</pre>
 *
 * @author zifang
 */
public final class QlExpressionUtil {

    private QlExpressionUtil() {
        // 工具类，禁止实例化
    }

    /**
     * 求值 QL 表达式 — 用于 {@code DataModelServiceInfo.expression} 字段.
     *
     * @param paramMap 参数上下文（业务字段 / 模型字段 / 当前用户信息）
     * @param rule     QL 表达式字符串；为空时默认返回 false
     * @return 求值结果（true = 命中 / false = 未命中）；解析异常时返回 false
     */
    public static boolean executeRule(Map<String, Object> paramMap, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return false;
        }
        try {
            Object result = evaluate(paramMap, rule);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return isTruthy(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 求值表达式并返回 Object 结果.
     *
     * @param paramMap 参数上下文
     * @param rule     QL 表达式
     * @return 求值结果（可能是 Boolean / String / Number 等）；异常时返回 null
     */
    public static Object evaluate(Map<String, Object> paramMap, String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return null;
        }
        // 支持外层括号
        String trimmed = rule.trim();
        if (trimmed.startsWith("(") && trimmed.endsWith(")") && isBalanced(trimmed)) {
            return evaluate(paramMap, trimmed.substring(1, trimmed.length() - 1));
        }
        // 逻辑 OR
        int orIdx = findTopLevelBinaryOp(trimmed, "||");
        if (orIdx >= 0) {
            String left = trimmed.substring(0, orIdx);
            String right = trimmed.substring(orIdx + 2);
            return toBool(evaluate(paramMap, left)) || toBool(evaluate(paramMap, right));
        }
        // 逻辑 AND
        int andIdx = findTopLevelBinaryOp(trimmed, "&&");
        if (andIdx >= 0) {
            String left = trimmed.substring(0, andIdx);
            String right = trimmed.substring(andIdx + 2);
            return toBool(evaluate(paramMap, left)) && toBool(evaluate(paramMap, right));
        }
        // NOT
        if (trimmed.startsWith("!")) {
            return !toBool(evaluate(paramMap, trimmed.substring(1)));
        }
        // 比较运算
        for (String op : new String[]{"==", "!=", ">=", "<=", ">", "<"}) {
            int idx = findTopLevelBinaryOp(trimmed, op);
            if (idx >= 0) {
                Object lv = evaluate(paramMap, trimmed.substring(0, idx));
                Object rv = evaluate(paramMap, trimmed.substring(idx + op.length()));
                return compare(lv, rv, op);
            }
        }
        // 方法调用（varName.method(args)）
        int parenIdx = findTopLevelMethodCall(trimmed);
        if (parenIdx > 0) {
            int dotIdx = trimmed.lastIndexOf('.', parenIdx);
            if (dotIdx > 0) {
                String varName = trimmed.substring(0, dotIdx);
                String methodAndArgs = trimmed.substring(dotIdx + 1);
                int paren = methodAndArgs.indexOf('(');
                String methodName = methodAndArgs.substring(0, paren);
                String argsPart = methodAndArgs.substring(paren + 1, methodAndArgs.lastIndexOf(')'));
                Object target = resolveVar(paramMap, varName);
                String[] args = splitArgs(argsPart);
                return invokeMethod(target, methodName, args, paramMap);
            }
        }
        // 字符串字面量
        if ((trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2)
                || (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2)) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        // 数字字面量
        if (trimmed.matches("-?\\d+(\\.\\d+)?")) {
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            }
            return Long.parseLong(trimmed);
        }
        // 布尔字面量
        if ("true".equals(trimmed)) {
            return Boolean.TRUE;
        }
        if ("false".equals(trimmed)) {
            return Boolean.FALSE;
        }
        if ("null".equals(trimmed)) {
            return null;
        }
        // 变量引用
        return resolveVar(paramMap, trimmed);
    }

    private static Object resolveVar(Map<String, Object> paramMap, String varName) {
        if (paramMap == null) {
            return null;
        }
        return paramMap.get(varName);
    }

    private static Object compare(Object lv, Object rv, String op) {
        if (lv == null || rv == null) {
            // null 比较
            if ("==".equals(op)) {
                return lv == rv;
            }
            if ("!=".equals(op)) {
                return lv != rv;
            }
            return false;
        }
        Comparable l = toComparable(lv);
        Comparable r = toComparable(rv);
        int cmp;
        try {
            cmp = l.compareTo(r);
        } catch (Exception e) {
            // 类型不一致：转为字符串比较
            cmp = l.toString().compareTo(r.toString());
        }
        switch (op) {
            case "==":
                return cmp == 0;
            case "!=":
                return cmp != 0;
            case ">":
                return cmp > 0;
            case "<":
                return cmp < 0;
            case ">=":
                return cmp >= 0;
            case "<=":
                return cmp <= 0;
            default:
                return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable toComparable(Object v) {
        if (v instanceof Number && v instanceof Comparable) {
            // 数字统一为 Double 比较
            return ((Number) v).doubleValue();
        }
        return (Comparable) v;
    }

    private static Object invokeMethod(Object target, String methodName, String[] rawArgs, Map<String, Object> paramMap) {
        Object[] args = new Object[rawArgs.length];
        for (int i = 0; i < rawArgs.length; i++) {
            args[i] = evaluate(paramMap, rawArgs[i].trim());
        }
        if (target == null) {
            return false;
        }
        switch (methodName) {
            case "contains":
                if (target instanceof CharSequence && args.length == 1) {
                    return ((CharSequence) target).toString().contains(String.valueOf(args[0]));
                }
                if (target instanceof java.util.Collection && args.length == 1) {
                    return ((java.util.Collection<?>) target).contains(args[0]);
                }
                return false;
            case "startsWith":
                if (target instanceof CharSequence && args.length == 1) {
                    return ((CharSequence) target).toString().startsWith(String.valueOf(args[0]));
                }
                return false;
            case "endsWith":
                if (target instanceof CharSequence && args.length == 1) {
                    return ((CharSequence) target).toString().endsWith(String.valueOf(args[0]));
                }
                return false;
            case "equals":
                if (args.length == 1) {
                    return target.equals(args[0]);
                }
                return false;
            case "toString":
                return target.toString();
            default:
                return false;
        }
    }

    private static boolean toBool(Object v) {
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        return isTruthy(v);
    }

    private static boolean isTruthy(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof Number) {
            return ((Number) v).doubleValue() != 0;
        }
        if (v instanceof CharSequence) {
            return ((CharSequence) v).length() > 0;
        }
        if (v instanceof java.util.Collection) {
            return !((java.util.Collection<?>) v).isEmpty();
        }
        return true;
    }

    /**
     * 在顶层（不在括号内）查找二元运算符位置.
     */
    private static int findTopLevelBinaryOp(String s, String op) {
        int depth = 0;
        for (int i = 0; i <= s.length() - op.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && s.startsWith(op, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 找到顶层方法调用的左括号位置.
     */
    private static int findTopLevelMethodCall(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                if (depth == 0) {
                    return i;
                }
                depth++;
            } else if (c == ')') {
                if (depth > 0) {
                    depth--;
                }
            }
        }
        return -1;
    }

    /**
     * 按顶层逗号切分参数列表（不在括号内的逗号）.
     */
    private static String[] splitArgs(String argsPart) {
        java.util.List<String> result = new java.util.ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean inString = false;
        char stringChar = 0;
        for (int i = 0; i < argsPart.length(); i++) {
            char c = argsPart.charAt(i);
            if (inString) {
                if (c == stringChar) {
                    inString = false;
                }
            } else if (c == '\'' || c == '"') {
                inString = true;
                stringChar = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                result.add(argsPart.substring(start, i));
                start = i + 1;
            }
        }
        result.add(argsPart.substring(start));
        return result.toArray(new String[0]);
    }

    /**
     * 括号是否平衡（用于外层括号剥离）.
     */
    private static boolean isBalanced(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }
}
