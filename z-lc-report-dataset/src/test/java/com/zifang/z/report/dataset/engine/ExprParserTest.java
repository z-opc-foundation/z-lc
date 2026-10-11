package com.zifang.z.report.dataset.engine;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 表达式字符串解析器测试 (白名单受限求值: 对象树, 无代码执行) */
class ExprParserTest {

    private Map<String, Object> row() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", 100);
        m.put("qty", 3);
        m.put("city", "hangzhou");
        m.put("active", Boolean.TRUE);
        m.put("rate", 0.5);
        return m;
    }

    @Test
    void arithmetic_andPrecedence() {
        assertEquals(201.0, ((Number) ExprParser.parse("amount * 2 + 1").eval(row())).doubleValue(), 1e-9);
        assertEquals(101.0, ((Number) ExprParser.parse("1 + amount * 2 / 2").eval(row())).doubleValue(), 1e-9); // 100*2/2=100, +1
        assertEquals(-50L, ((Number) ExprParser.parse("-amount / 2").eval(row())).longValue());
    }

    @Test
    void comparison_andLogic() {
        assertTrue((Boolean) ExprParser.parse("amount > 50 AND city == 'hangzhou'").eval(row()));
        assertTrue((Boolean) ExprParser.parse("city != 'beijing' && qty <= 3").eval(row()));
        assertFalse((Boolean) ExprParser.parse("NOT (amount >= 100) OR city == 'sh'").eval(row()));
        // 比较任一侧 null → false
        assertFalse((Boolean) ExprParser.parse("amount > 50").eval(new LinkedHashMap<>()));
    }

    @Test
    void isNull_in_like() {
        assertTrue((Boolean) ExprParser.parse("memo IS NULL").eval(row()));
        assertTrue((Boolean) ExprParser.parse("city IS NOT NULL").eval(row()));
        assertTrue((Boolean) ExprParser.parse("city IN ('hangzhou', 'shanghai')").eval(row()));
        assertFalse((Boolean) ExprParser.parse("city IN ('beijing')").eval(row()));
        assertTrue((Boolean) ExprParser.parse("city LIKE 'hang'").eval(row()));
    }

    @Test
    void nullPropagation_arithmetic() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("amount", null);
        assertNull(ExprParser.parse("amount * 2").eval(m));
    }

    @Test
    void literals_andParens() {
        assertEquals("it''s", ExprParser.parse("'it''''s'").eval(row())); // '' 转义
        assertEquals(103L, ((Number) ExprParser.parse("(amount + qty) * 100 / 100").eval(row())).longValue()); // (100+3)*100/100
        assertEquals(Boolean.TRUE, ExprParser.parse("true").eval(row()));
        assertNull(ExprParser.parse("null").eval(row()));
    }

    @Test
    void functionCall_rejected() {
        // 安全白名单: 不允许函数调用
        assertThrows(ExprParser.ParseException.class, () -> ExprParser.parse("ABS(amount)"));
        assertThrows(ExprParser.ParseException.class, () ->
                ExprParser.parse("System.getProperty('user.dir')"));
    }

    @Test
    void syntaxErrors() {
        assertThrows(ExprParser.ParseException.class, () -> ExprParser.parse("amount +"));
        assertThrows(ExprParser.ParseException.class, () -> ExprParser.parse("(amount"));
        assertThrows(ExprParser.ParseException.class, () -> ExprParser.parse("city IS"));
        assertThrows(ExprParser.ParseException.class, () -> ExprParser.parse(""));
        assertThrows(ExprParser.ParseException.class, () -> ExprParser.parse("amount @ 2"));
    }
}
