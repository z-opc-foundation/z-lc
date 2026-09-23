package com.zifang.z.lc.core.fieldtype;

/**
 * DATE → DATE, pattern yyyy-MM-dd (与历史 coerce 一致).
 *
 * @author zifang
 */
public class DateTypeHandler extends AbstractDateTimeTypeHandler {

    @Override
    public String fieldType() {
        return "DATE";
    }

    @Override
    public String label() {
        return "日期";
    }

    @Override
    public String dbType(Integer length, Integer scale) {
        return "DATE";
    }

    @Override
    protected String pattern() {
        return "yyyy-MM-dd";
    }
}
