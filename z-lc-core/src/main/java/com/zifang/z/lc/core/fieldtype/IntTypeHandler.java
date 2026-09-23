package com.zifang.z.lc.core.fieldtype;

/**
 * INT → BIGINT (历史映射即 BIGINT, 保持不变).
 *
 * @author zifang
 */
public class IntTypeHandler extends LongBackedTypeHandler {

    @Override
    public String fieldType() {
        return "INT";
    }

    @Override
    public String label() {
        return "整数";
    }
}
