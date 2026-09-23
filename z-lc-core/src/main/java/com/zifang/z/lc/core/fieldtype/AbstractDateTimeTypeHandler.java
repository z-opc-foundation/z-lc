package com.zifang.z.lc.core.fieldtype;

import com.zifang.z.lc.common.dto.FieldDefDTO;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 日期时间基类: DATE / DATETIME 共用逻辑, 只差 pattern 与物理类型.
 * <p>
 * coerce 走历史宽松解析 (SimpleDateFormat lenient, 与 DynamicSqlBuilder.coerce 完全一致);
 * validateValue / parseFromCell 走严格解析 (lenient=false) — 这是对历史「2026-01-32 也吞下」
 * 类 bug 的修复: 校验路径拒绝非法日期, 写入路径行为不变 (兼容存量).
 *
 * @author zifang
 */
public abstract class AbstractDateTimeTypeHandler extends AbstractFieldTypeHandler {

    /**
     * SimpleDateFormat pattern.
     */
    protected abstract String pattern();

    @Override
    public CellValueType cellValueType() {
        return CellValueType.DATETIME;
    }

    @Override
    public Object coerce(Object raw) {
        if (raw instanceof java.util.Date) {
            return raw;
        }

        try {
            return new SimpleDateFormat(pattern()).parse(raw.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Field requires date: " + raw);
        }
    }

    @Override
    protected List<ValidationError> checkValue(FieldDefDTO def, Object cellValue) {
        List<ValidationError> errors = new ArrayList<>();
        if (cellValue instanceof java.util.Date) {
            return errors;
        }

        if (!canParseStrict(String.valueOf(cellValue))) {
            errors.add(new ValidationError(def == null ? null : def.getFieldCode(), "invalidDate",
                    "字段 (" + (def == null ? "" : def.getFieldCode()) + ") 需要日期格式 " + pattern() + ", 实际: " + cellValue));
        }

        return errors;
    }

    /**
     * 严格日期解析 (不接受 2026-01-32 / 2026-1-1 这类宽松输入).
     */
    protected boolean canParseStrict(String text) {
        SimpleDateFormat f = new SimpleDateFormat(pattern());
        f.setLenient(false);
        try {
            Date d = f.parse(text);
            return d != null;
        } catch (ParseException e) {
            return false;
        }
    }

    @Override
    public List<String> operators() {
        return DATE_OPS;
    }

    @Override
    public boolean sortable() {
        return true;
    }

    @Override
    public boolean groupable() {
        return true;
    }

    @Override
    public boolean filterable() {
        return true;
    }

    @Override
    public boolean inlineEditable() {
        return true;
    }

    @Override
    public String widget() {
        return "datePicker";
    }

    @Override
    public String formatForExport(Object cellValue) {
        if (cellValue == null) {
            return "";
        }

        if (cellValue instanceof java.util.Date) {
            return new SimpleDateFormat(pattern()).format((java.util.Date) cellValue);
        }

        return String.valueOf(cellValue);
    }

    @Override
    public Object parseFromCell(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        if (!canParseStrict(text)) {
            throw new IllegalArgumentException("Field requires date (" + pattern() + "): " + text);
        }

        return coerce(text);
    }
}
