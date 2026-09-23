package com.zifang.z.lc.core.fieldtype;

/**
 * DATETIME → DATETIME, pattern yyyy-MM-dd HH:mm:ss (与历史 coerce 一致).
 *
 * @author zifang
 */
public class DateTimeTypeHandler extends AbstractDateTimeTypeHandler {

    @Override
    public String fieldType() {
        return "DATETIME";
    }

    @Override
    public String label() {
        return "日期时间";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        return "DATETIME";
    }

    @Override
    protected String pattern() {
        return "yyyy-MM-dd HH:mm:ss";
    }
}
