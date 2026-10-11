package com.zifang.z.report.dataset.engine;

import com.zifang.z.report.common.schema.FieldType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * typed 列: 名称 + 类型 + 值序列 (允许 null)。
 * 值与声明类型不做强校验 (宽松存取), 语义校验由上层契约负责。
 */
public final class Column {

    private final String name;
    private final FieldType type;
    private final List<Object> values;

    Column(String name, FieldType type) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.values = new ArrayList<>();
    }

    private Column(String name, FieldType type, List<Object> values) {
        this.name = name;
        this.type = type;
        this.values = values;
    }

    Column copy() {
        return new Column(name, type, new ArrayList<>(values));
    }

    public String name() {
        return name;
    }

    public FieldType type() {
        return type;
    }

    List<Object> values() {
        return values;
    }
}
