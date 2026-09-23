package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * BOOLEAN coerce 的行为锁定测试.
 * <p>
 * 历史实现是 {@code Boolean.valueOf(raw.toString())}, 于是 checkValue 认为合法的
 * 1 / 0 / Y / N 在写入时全被 coerce 成 false —— 校验放行、数据却被篡改
 * (实测 fieldValues {@code "done":1} 存进去读回来是 0). 这里把修正后的口径钉住.
 */
public class BooleanTypeHandlerCoerceTest {

    /** 直接测 handler 本体, 不绕注册表查找 API. */
    private final BooleanTypeHandler handler = new BooleanTypeHandler();

    @Test
    public void keepsBooleanInstancesAsIs() {
        assertEquals(Boolean.TRUE, handler.coerce(Boolean.TRUE));
        assertEquals(Boolean.FALSE, handler.coerce(Boolean.FALSE));
    }

    /** 数字 1/0 以前一律被 parseBoolean 判成 false. */
    @Test
    public void coercesNumbersToTruthiness() {
        assertEquals(Boolean.TRUE, handler.coerce(1));
        assertEquals(Boolean.FALSE, handler.coerce(0));
        assertEquals(Boolean.TRUE, handler.coerce(2));
        assertEquals(Boolean.TRUE, handler.coerce(1L));
        assertEquals(Boolean.TRUE, handler.coerce(0.5));
        assertEquals(Boolean.FALSE, handler.coerce(0.0));
    }

    /** 与 checkValue 认可的那套写法保持一致. */
    @Test
    public void coercesCommonStringForms() {
        for (String truthy : new String[]{"true", "TRUE", "1", "Y", "yes", "on", "t"}) {
            assertEquals(truthy + " should coerce to true", Boolean.TRUE, handler.coerce(truthy));
        }
        for (String falsy : new String[]{"false", "FALSE", "0", "N", "no", "off", "f"}) {
            assertEquals(falsy + " should coerce to false", Boolean.FALSE, handler.coerce(falsy));
        }
    }

    /** null 的短路在 {@link FieldTypeRegistry} 那一层, 真正的写路径走 coerceValue. */
    @Test
    public void registryWritePathHandlesWhitespaceNullAndGarbage() {
        assertEquals(Boolean.TRUE, FieldTypeRegistry.coerceValue("  true  ", "BOOLEAN"));
        assertEquals(Boolean.TRUE, FieldTypeRegistry.coerceValue(" 1 ", "BOOLEAN"));
        assertEquals(Boolean.TRUE, FieldTypeRegistry.coerceValue(1, "BOOLEAN"));
        assertEquals(Boolean.FALSE, FieldTypeRegistry.coerceValue(0, "BOOLEAN"));
        assertNull("null 必须还是 null, 不能变成 false", FieldTypeRegistry.coerceValue(null, "BOOLEAN"));
        assertFalse("无法识别的串维持历史的 false 兜底",
                (Boolean) FieldTypeRegistry.coerceValue("banana", "BOOLEAN"));
    }

    /**
     * 核心不变式: coerce 的产物必须能过自己的 checkValue.
     * 之前的实现正是违背了这一点才出现"校验放行 + 写入篡改".
     */
    @Test
    public void coerceResultAlwaysPassesItsOwnValidation() {
        FieldDefDTO def = new FieldDefDTO();
        def.setFieldCode("flag");
        Object[] inputs = {1, 0, "1", "0", "Y", "N", true, false, "true", "false"};
        for (Object input : inputs) {
            Object coerced = handler.coerce(input);
            List<ValidationError> errors = handler.validateValue(def, coerced);
            assertTrue("coerce(" + input + ") 产出 " + coerced + " 却被自己的校验拒绝",
                    errors == null || errors.isEmpty());
        }
    }
}
