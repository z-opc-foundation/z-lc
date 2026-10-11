package com.zifang.z.report.dataset.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式字符串解析器 (递归下降): "amount * 2 + 1", "city == 'hangzhou' AND amount > 100" → Expr 对象树。
 * <p>
 * 安全约定 (方案决策①的"白名单受限求值"):
 * - 只生成结构化 Expr 对象树, 求值走 Expr.eval(row), 不做任何字符串代码执行
 * - 语法面即白名单: 列引用 / 字面量 / 算术(+ - * / %) / 比较(= == != > >= < <=) /
 *   逻辑(AND OR NOT, 亦支持 &amp;&amp; ||) / IS [NOT] NULL / IN (...) / LIKE (退化为 contains)
 * - 不支持函数调用与动态标识符 → 不存在注入面; 更复杂表达式 (函数/日期运算) 由 z-util-expr
 *   接入 (M2+, 见 _doc/008_troubleshooting/失败要点与坑.md), 届时必须白名单限定可用函数
 * <p>
 * 语法 (优先级从低到高): OR → AND → 相等 → 比较 → 加减 → 乘除模 → 一元(- ! NOT) → primary。
 * 关键字 AND/OR/NOT/IS/NULL/IN/LIKE/TRUE/FALSE 大小写不敏感; 列名大小写敏感。
 */
public final class ExprParser {

    /** 解析失败 (语法/未知列无关, 纯语法层) */
    public static class ParseException extends RuntimeException {
        public ParseException(String message) {
            super(message);
        }
    }

    private final List<Token> tokens;
    private int pos;

    public ExprParser(String source) {
        this.tokens = tokenize(source);
        this.pos = 0;
    }

    /** 入口: 表达式字符串 → Expr 对象树 */
    public static Expr parse(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new ParseException("empty expression");
        }
        ExprParser p = new ExprParser(source);
        Expr e = p.parseOr();
        if (!p.atEnd()) {
            throw new ParseException("unexpected token at " + p.peek().pos + ": " + p.peek().text);
        }
        return e;
    }

    // ==================== 语法层 ====================

    private Expr parseOr() {
        Expr left = parseAnd();
        while (matchKeyword("OR") || matchOp("||")) {
            left = Expr.or(left, parseAnd());
        }
        return left;
    }

    private Expr parseAnd() {
        Expr left = parseEquality();
        while (matchKeyword("AND") || matchOp("&&")) {
            left = Expr.and(left, parseEquality());
        }
        return left;
    }

    private Expr parseEquality() {
        Expr left = parseComparison();
        while (true) {
            if (matchOp("==") || matchOp("=")) {
                left = Expr.eq(left, parseComparison());
            } else if (matchOp("!=") || matchOp("<>")) {
                left = Expr.ne(left, parseComparison());
            } else {
                return left;
            }
        }
    }

    private Expr parseComparison() {
        Expr left = parseAdditive();
        if (matchOp(">=")) {
            return Expr.ge(left, parseAdditive());
        }
        if (matchOp(">")) {
            return Expr.gt(left, parseAdditive());
        }
        if (matchOp("<=")) {
            return Expr.le(left, parseAdditive());
        }
        if (matchOp("<")) {
            return Expr.lt(left, parseAdditive());
        }
        // IS [NOT] NULL / IN (...) / LIKE 'x' (左操作数为列或任意表达式, 退化为列引用形式)
        if (matchKeyword("IS")) {
            boolean not = matchKeyword("NOT");
            if (!matchKeyword("NULL")) {
                throw new ParseException("expected NULL after IS");
            }
            return not ? Expr.isNotNull(colNameOf(left)) : Expr.isNull(colNameOf(left));
        }
        if (matchKeyword("IN")) {
            expectOp("(");
            List<Object> values = new ArrayList<>();
            do {
                values.add(parseLiteral());
            } while (matchOp(","));
            expectOp(")");
            return Expr.in(colNameOf(left), values);
        }
        if (matchKeyword("LIKE")) {
            Object pat = parseLiteral();
            if (!(pat instanceof String)) {
                throw new ParseException("LIKE pattern must be string");
            }
            return Expr.contains(colNameOf(left), (String) pat);
        }
        return left;
    }

    private Expr parseAdditive() {
        Expr left = parseMultiplicative();
        while (true) {
            if (matchOp("+")) {
                left = Expr.add(left, parseMultiplicative());
            } else if (matchOp("-")) {
                left = Expr.sub(left, parseMultiplicative());
            } else {
                return left;
            }
        }
    }

    private Expr parseMultiplicative() {
        Expr left = parseUnary();
        while (true) {
            if (matchOp("*")) {
                left = Expr.mul(left, parseUnary());
            } else if (matchOp("/")) {
                left = Expr.div(left, parseUnary());
            } else if (matchOp("%")) {
                throw new ParseException("mod(%) not supported yet (z-util-expr handoff)");
            } else {
                return left;
            }
        }
    }

    private Expr parseUnary() {
        if (matchOp("-")) {
            Expr inner = parseUnary();
            return Expr.mul(Expr.lit(-1L), inner);
        }
        if (matchOp("!") || matchKeyword("NOT")) {
            return Expr.not(parseUnary());
        }
        return parsePrimary();
    }

    private Expr parsePrimary() {
        Token t = peek();
        if (t == null) {
            throw new ParseException("unexpected end of expression");
        }
        if (matchOp("(")) {
            Expr e = parseOr();
            expectOp(")");
            return e;
        }
        if (t.kind == Kind.NUMBER) {
            next();
            return Expr.lit(t.value);
        }
        if (t.kind == Kind.STRING) {
            next();
            return Expr.lit(t.value);
        }
        if (t.kind == Kind.IDENT) {
            String upper = t.text.toUpperCase();
            if ("TRUE".equals(upper) || "FALSE".equals(upper)) {
                next();
                return Expr.lit(Boolean.parseBoolean(upper));
            }
            if ("NULL".equals(upper)) {
                next();
                return Expr.lit(null);
            }
            // 函数调用不允许 (安全白名单)
            if (pos + 1 < tokens.size() && "(".equals(tokens.get(pos + 1).text)) {
                throw new ParseException("function call not allowed (whitelist policy): " + t.text);
            }
            next();
            return Expr.col(t.text);
        }
        throw new ParseException("unexpected token at " + t.pos + ": " + t.text);
    }

    private Object parseLiteral() {
        Token t = peek();
        if (t == null) {
            throw new ParseException("expected literal");
        }
        if (t.kind == Kind.NUMBER || t.kind == Kind.STRING) {
            next();
            return t.value;
        }
        if (t.kind == Kind.IDENT) {
            String upper = t.text.toUpperCase();
            if ("TRUE".equals(upper) || "FALSE".equals(upper)) {
                next();
                return Boolean.parseBoolean(upper);
            }
            if ("NULL".equals(upper)) {
                next();
                return null;
            }
        }
        throw new ParseException("expected literal at " + t.pos + ": " + t.text);
    }

    private static Expr toColumn(Expr e) {
        if (e instanceof Expr.Col) {
            return e;
        }
        throw new ParseException("IS NULL/IS NOT NULL requires column operand");
    }

    private static String colNameOf(Expr e) {
        if (e instanceof Expr.Col) {
            return ((Expr.Col) e).name();
        }
        throw new ParseException("IN/LIKE requires column operand");
    }

    // ==================== 词法层 ====================

    private enum Kind { NUMBER, STRING, IDENT, OP }

    private static final class Token {
        final Kind kind;
        final String text;
        final Object value;
        final int pos;

        Token(Kind kind, String text, Object value, int pos) {
            this.kind = kind;
            this.text = text;
            this.value = value;
            this.pos = pos;
        }
    }

    private List<Token> tokenize(String src) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            // 字符串字面量: 'x' 或 "x" (支持 '' 转义)
            if (c == '\'' || c == '"') {
                char quote = c;
                StringBuilder sb = new StringBuilder();
                i++;
                boolean closed = false;
                while (i < n) {
                    char ch = src.charAt(i);
                    if (ch == quote) {
                        if (i + 1 < n && src.charAt(i + 1) == quote) {
                            sb.append(quote);
                            i += 2;
                            continue;
                        }
                        i++;
                        closed = true;
                        break;
                    }
                    sb.append(ch);
                    i++;
                }
                if (!closed) {
                    throw new ParseException("unterminated string literal at " + i);
                }
                out.add(new Token(Kind.STRING, sb.toString(), sb.toString(), i));
                continue;
            }
            // 数字 (含小数)
            if (Character.isDigit(c)) {
                int start = i;
                while (i < n && (Character.isDigit(src.charAt(i)) || src.charAt(i) == '.')) {
                    i++;
                }
                String num = src.substring(start, i);
                Object v = num.indexOf('.') >= 0 ? (Object) Double.parseDouble(num) : (Object) Long.parseLong(num);
                out.add(new Token(Kind.NUMBER, num, v, start));
                continue;
            }
            // 标识符 (列名/关键字)
            if (Character.isJavaIdentifierStart(c)) {
                int start = i;
                while (i < n && Character.isJavaIdentifierPart(src.charAt(i))) {
                    i++;
                }
                out.add(new Token(Kind.IDENT, src.substring(start, i), null, start));
                continue;
            }
            // 运算符 (最长匹配)
            String two = i + 1 < n ? src.substring(i, i + 2) : "";
            if ("==".equals(two) || "!=".equals(two) || "<>".equals(two) || ">=".equals(two)
                    || "<=".equals(two) || "&&".equals(two) || "||".equals(two)) {
                out.add(new Token(Kind.OP, two, null, i));
                i += 2;
                continue;
            }
            if ("+-*/%()<>=,".indexOf(c) >= 0) {
                out.add(new Token(Kind.OP, String.valueOf(c), null, i));
                i++;
                continue;
            }
            if (c == '!') {
                out.add(new Token(Kind.OP, "!", null, i));
                i++;
                continue;
            }
            throw new ParseException("illegal character '" + c + "' at " + i);
        }
        return out;
    }

    // ==================== 游标工具 ====================

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private Token next() {
        Token t = peek();
        pos++;
        return t;
    }

    private boolean atEnd() {
        return pos >= tokens.size();
    }

    private boolean matchOp(String op) {
        Token t = peek();
        if (t != null && t.kind == Kind.OP && t.text.equals(op)) {
            pos++;
            return true;
        }
        return false;
    }

    private boolean matchKeyword(String kw) {
        Token t = peek();
        if (t != null && t.kind == Kind.IDENT && kw.equalsIgnoreCase(t.text)) {
            pos++;
            return true;
        }
        return false;
    }

    private void expectOp(String op) {
        if (!matchOp(op)) {
            Token t = peek();
            throw new ParseException("expected '" + op + "' at "
                    + (t == null ? "end" : t.pos + " (" + t.text + ")"));
        }
    }
}
