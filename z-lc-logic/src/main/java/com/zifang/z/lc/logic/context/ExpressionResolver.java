package com.zifang.z.lc.logic.context;

import java.util.Map;

/**
 * 轻量表达式解析器：把节点配置里的字符串模板求值成运行时值。
 *
 * <p>设计取舍：不引入表达式引擎依赖（如 Aviator/SpEL），用约定优先的解析器覆盖
 * 低代码场景的高频写法，复杂表达式可通过注册自定义节点整段接管。
 * 支持以下语法（均可嵌套在普通文本中做模板替换）：
 * <ul>
 *   <li>{@code ${x}} —— 取 params 里的 x；</li>
 *   <li>{@code ${global.theme}} / {@code ${g.theme}} —— 取全局上下文；</li>
 *   <li>{@code ${flow.total}} —— 取流程参数；</li>
 *   <li>{@code ${state.addr.res}} / {@code ${state('addr').res}} —— 取某节点产出；</li>
 *   <li>{@code ${props.title}} —— 取当前节点 props；</li>
 *   <li>{@code ${a > b}} / {@code ${a == b}} / {@code ${a != b}} / {@code ${a && b}} —— 比较与逻辑；</li>
 *   <li>字面量：数字、true/false、引号字符串；</li>
 *   <li>模板文本：{@code "你好 ${name}"} → 逐段替换后拼接字符串。</li>
 * </ul>
 * </p>
 */
public final class ExpressionResolver {

    private ExpressionResolver() {
    }

    /** 兼容旧调用入口 */
    public static Object resolve(Object value, ExecutionContext ctx) {
        return resolve(value, ctx, null);
    }

    /** 带当前节点 props 的求值（props 可为 null） */
    public static Object resolve(Object value, ExecutionContext ctx, Map<String, Object> props) {
        if (value instanceof String s) {
            return resolveString(s, ctx, props);
        }
        return value;
    }

    private static Object resolveString(String text, ExecutionContext ctx, Map<String, Object> props) {
        // 整串就是一个 ${...}：尽量返回原始类型（Boolean/Number/Map），供条件节点直接使用
        String trimmed = text.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}") && matchingClose(trimmed, 1) == trimmed.length() - 1) {
            return eval(trimmed.substring(2, trimmed.length() - 1).trim(), ctx, props);
        }
        // 模板文本：逐段替换 ${...}，拼成字符串
        StringBuilder out = new StringBuilder();
        int i = 0;
        boolean replaced = false;
        while (i < text.length()) {
            int open = text.indexOf("${", i);
            if (open < 0) {
                out.append(text, i, text.length());
                break;
            }
            int close = matchingClose(text, open + 1);
            if (close < 0) {
                out.append(text, i, text.length());
                break;
            }
            out.append(text, i, open);
            Object v = eval(text.substring(open + 2, close).trim(), ctx, props);
            out.append(v == null ? "" : v);
            i = close + 1;
            replaced = true;
        }
        return replaced ? out.toString() : text;
    }

    /** 找到与起始的 } 配对的位置（start 指向 '{'），支持简单括号嵌套 */
    private static int matchingClose(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static Object eval(String expr, ExecutionContext ctx, Map<String, Object> props) {
        // 逻辑或
        int or = findTopLevel(expr, "||");
        if (or >= 0) {
            boolean l = truthy(eval(expr.substring(0, or), ctx, props));
            if (l) return true;
            return truthy(eval(expr.substring(or + 2), ctx, props));
        }
        // 逻辑与
        int and = findTopLevel(expr, "&&");
        if (and >= 0) {
            boolean l = truthy(eval(expr.substring(0, and), ctx, props));
            if (!l) return false;
            return truthy(eval(expr.substring(and + 2), ctx, props));
        }
        // 比较运算
        for (String op : new String[]{"==", "!=", ">=", "<=", ">", "<"}) {
            int idx = findTopLevel(expr, op);
            if (idx >= 0) {
                Object l = eval(expr.substring(0, idx), ctx, props);
                Object r = eval(expr.substring(idx + op.length()), ctx, props);
                return compare(l, r, op);
            }
        }
        // 字面量与路径
        return atom(expr.trim(), ctx, props);
    }

    /** 查找最外层（不在引号/括号内）的操作符位置 */
    private static int findTopLevel(String s, String op) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i <= s.length() - op.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                continue;
            }
            if (c == '\'' || c == '"') { quote = c; continue; }
            if (c == '(') { depth++; continue; }
            if (c == ')') { depth--; continue; }
            if (depth == 0 && s.regionMatches(i, op, 0, op.length())) return i;
        }
        return -1;
    }

    private static Object atom(String expr, ExecutionContext ctx, Map<String, Object> props) {
        if (expr.isEmpty()) return null;
        // 引号字符串
        if ((expr.startsWith("'") && expr.endsWith("'")) || (expr.startsWith("\"") && expr.endsWith("\""))) {
            return expr.substring(1, expr.length() - 1);
        }
        if ("true".equalsIgnoreCase(expr)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(expr)) return Boolean.FALSE;
        if ("null".equalsIgnoreCase(expr)) return null;
        // 数字
        try {
            if (expr.matches("-?\\d+")) return Long.parseLong(expr);
            if (expr.matches("-?\\d+\\.\\d+")) return Double.parseDouble(expr);
        } catch (NumberFormatException ignore) {
            // fall through
        }
        return path(expr, ctx, props);
    }

    /** 解析变量路径 */
    private static Object path(String expr, ExecutionContext ctx, Map<String, Object> props) {
        String p = expr;
        // state('addr').res → state.addr.res
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^state\\(['\"]([^'\"]+)['\"]\\)\\.(.*)$").matcher(p);
        String[] segs;
        if (m.matches()) {
            segs = splitPath("state." + m.group(1) + "." + m.group(2));
        } else {
            segs = splitPath(p);
        }
        if (segs.length == 0) return null;
        String root = segs[0];
        Object cur;
        int start;
        switch (root) {
            case "global": case "g":
                cur = ctx.getGlobals(); start = 1; break;
            case "flow":
                cur = ctx.getFlowParams(); start = 1; break;
            case "props":
                cur = props; start = 1; break;
            case "state":
                // state.addr.res → 从 nodeState[addr] 开始
                if (segs.length < 2) return null;
                cur = ctx.state(segs[1]); start = 2; break;
            case "params": case "p":
                cur = ctx.getParams(); start = 1; break;
            default:
                // 默认（如 ${x}）：首段就是要找的 key，从 params 找，取不到再退回 globals
                if (ctx.getParams().containsKey(root)) {
                    cur = ctx.getParams().get(root);
                } else {
                    cur = ctx.getGlobals().get(root);
                }
                start = 1; break;
        }
        for (int i = start; i < segs.length; i++) {
            if (!(cur instanceof Map<?, ?> map)) return null;
            cur = map.get(segs[i]);
            if (cur == null) return null;
        }
        return cur;
    }

    private static String[] splitPath(String s) {
        return s.isEmpty() ? new String[0] : s.split("\\.");
    }

    private static boolean truthy(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        return !"false".equalsIgnoreCase(v.toString()) && !"".equals(v.toString());
    }

    private static boolean compare(Object l, Object r, String op) {
        int c = compareTo(l, r);
        return switch (op) {
            case "==" -> c == 0;
            case "!=" -> c != 0;
            case ">" -> c > 0;
            case "<" -> c < 0;
            case ">=" -> c >= 0;
            case "<=" -> c <= 0;
            default -> false;
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareTo(Object l, Object r) {
        if (l == null && r == null) return 0;
        if (l == null) return -1;
        if (r == null) return 1;
        if (l instanceof Number ln && r instanceof Number rn) {
            return Double.compare(ln.doubleValue(), rn.doubleValue());
        }
        if (l instanceof Comparable c && r.getClass().isInstance(l)) {
            return c.compareTo(r);
        }
        return String.valueOf(l).compareTo(String.valueOf(r));
    }
}
