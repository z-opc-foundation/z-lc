package com.zifang.z.report.datasource.impl;

import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvDataSourceTest {

    private CsvReportDataSource source(String csv) {
        Map<String, String> props = new HashMap<>();
        props.put("inline", csv);
        return new CsvReportDataSource(new DataSourceDef("csv1", "csv1", DataSourceType.CSV, props));
    }

    @Test
    void inlineCsv_infersTypes_andReads() {
        CsvReportDataSource src = source(
                "city,amount,active\n"
                        + "hangzhou,100,true\n"
                        + "shanghai,250.5,false\n"
                        + "shenzhen,80,true\n");
        assertEquals(java.util.Collections.singletonList("csv1"), src.tables());

        Map<String, FieldType> schema = src.schema("csv1");
        assertEquals(FieldType.STRING, schema.get("city"));
        assertEquals(FieldType.DOUBLE, schema.get("amount")); // 100(INT) 行遇 250.5 升级 DOUBLE
        assertEquals(FieldType.BOOL, schema.get("active"));

        Table t = src.read("csv1");
        assertEquals(3, t.nRows());
        assertEquals("hangzhou", t.colValues("city").get(0));
        assertEquals(100L, t.colValues("amount").get(0));
        assertEquals(Boolean.TRUE, t.colValues("active").get(0));
    }

    @Test
    void quotedFields_withCommaAndEscapedQuote() {
        CsvReportDataSource src = source(
                "name,note\n"
                        + "\"Zhang, San\",\"say \"\"hi\"\"\"\n"
                        + "Li,nice\n");
        Table t = src.read("csv1");
        assertEquals(2, t.nRows());
        assertEquals("Zhang, San", t.colValues("name").get(0));
        assertEquals("say \"hi\"", t.colValues("note").get(0));
        assertEquals("Li", t.colValues("name").get(1));
    }

    @Test
    void emptyCells_becomeNull() {
        CsvReportDataSource src = source("a,b\n1,\n2,3\n");
        Table t = src.read("csv1");
        assertNull(t.colValues("b").get(0));
        assertEquals(3L, t.colValues("b").get(1));
    }

    @Test
    void unknownTable_rejected() {
        CsvReportDataSource src = source("a\n1\n");
        assertThrows(IllegalArgumentException.class, () -> src.read("nope"));
    }

    @Test
    void pathBased_missingFile_rejected() {
        Map<String, String> props = new HashMap<>();
        props.put("path", "/nonexistent/x.csv");
        assertThrows(IllegalArgumentException.class, () ->
                new CsvReportDataSource(new DataSourceDef("csvp", "csvp", DataSourceType.CSV, props)));
        assertTrue(true);
    }
}
