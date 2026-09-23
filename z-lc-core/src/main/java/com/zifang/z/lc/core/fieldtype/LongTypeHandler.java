package com.zifang.z.lc.core.fieldtype;

/**
 * LONG → BIGINT.
 *
 * @author zifang
 */
public class LongTypeHandler extends LongBackedTypeHandler {

    @Override
    public String fieldType() {
        return "LONG";
    }

    @Override
    public String label() {
        return "长整数";
    }
}
