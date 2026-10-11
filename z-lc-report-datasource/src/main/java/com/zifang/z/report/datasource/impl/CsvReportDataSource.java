package com.zifang.z.report.datasource.impl;

import com.zifang.util.parser.csv.CsvCharsetDetector;
import com.zifang.util.parser.csv.CsvParser;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.common.schema.FieldType;
import com.zifang.z.report.dataset.engine.Table;
import com.zifang.z.report.datasource.spi.ReportDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV 数据源 (M1.5): 单文件单表, 首行表头, 类型按内容尽力推断。
 * <p>
 * properties:
 * - path: 本地 CSV 文件路径 (二选一)
 * - inline: 内嵌 CSV 文本 (演示/测试, 优先于 path)
 * - max-rows: 截断保护 (默认 100000)
 * <p>
 * 解析: 走 z-util-parser-csv 1.0.10 的 CsvParser (RFC4180, 引号内换行/逗号/"" 转义);
 * 路径读取经 CsvCharsetDetector.decode 探测编码 (UTF-8 BOM / UTF-8 / GB18030 兜底),
 * 解决硬编码 UTF-8 误读 Excel 中文导出 CSV 的问题。空单元 → null + 类型升级推断仍由 z-report 自维护。
 */
public class CsvReportDataSource implements ReportDataSource {

    private final DataSourceDef def;
    private final String tableId;
    private final LinkedHashMap<String, FieldType> colTypes;
    private final List<List<Object>> rows;

    public CsvReportDataSource(DataSourceDef def) {
        Objects.requireNonNull(def, "def");
        if (def.getType() != DataSourceType.CSV) {
            throw new IllegalArgumentException("CsvReportDataSource requires type=CSV, got: " + def.getType());
        }
        Map<String, String> p = def.getProperties() == null
                ? new LinkedHashMap<String, String>() : def.getProperties();
        String maxRows = p.getOrDefault("max-rows", "100000");
        String csv = p.get("inline");
        if (csv == null || csv.isEmpty()) {
            String path = requireProp(p, "path");
            try {
                String text = CsvCharsetDetector.decode(Files.readAllBytes(Paths.get(path)));
                this.tableId = Paths.get(path).getFileName().toString().replaceAll("\\.csv$", "");
                csv = text;
            } catch (IOException e) {
                throw new IllegalArgumentException("cannot read csv file: " + path + " (" + e.getMessage() + ")", e);
            }
        } else {
            this.tableId = def.getId() == null ? "csv" : def.getId();
        }
        this.def = def;

        Parsed parsed = parse(csv, Integer.parseInt(maxRows));
        this.colTypes = parsed.colTypes;
        this.rows = parsed.rows;
    }

    private static String requireProp(Map<String, String> p, String key) {
        String v = p.get(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException("dataSource property required: " + key);
        }
        return v;
    }

    @Override
    public DataSourceDef def() {
        return def;
    }

    @Override
    public List<String> tables() {
        return Collections.singletonList(tableId);
    }

    @Override
    public Map<String, FieldType> schema(String table) {
        requireTable(table);
        return new LinkedHashMap<>(colTypes);
    }

    @Override
    public Table read(String table) {
        requireTable(table);
        return Table.of(new ArrayList<>(colTypes.keySet()), new ArrayList<>(colTypes.values()), rows);
    }

    private void requireTable(String table) {
        if (!tableId.equals(table)) {
            throw new IllegalArgumentException("table not found in csv source [" + def.getId() + "]: " + table);
        }
    }

    // ==================== z-util-parser-csv 适配 + 类型升级推断 ====================

    private static Parsed parse(String csv, int maxRows) {
        CsvParser parser = CsvParser.builder()
                .delimiter(',')
                .firstLineAsHeader(true)
                .skipEmptyLines(false)
                .trimFields(false)
                .build();
        CsvParser.ParseResult result = parser.parseWithHeader(csv);
        String[] headerArr = result.getHeaders();
        List<String[]> data = result.getData();
        if (headerArr == null || headerArr.length == 0) {
            throw new IllegalArgumentException("empty csv (no header)");
        }
        List<String> header = new ArrayList<>(headerArr.length);
        for (String h : headerArr) {
            header.add(h == null ? "" : h);
        }
        LinkedHashMap<String, FieldType> cols = new LinkedHashMap<>();
        for (String h : header) {
            cols.put(h, FieldType.STRING);
        }
        List<List<Object>> rows = new ArrayList<>();
        for (String[] record : data) {
            if (rows.size() >= maxRows) {
                break;
            }
            List<Object> row = new ArrayList<>(header.size());
            for (int i = 0; i < header.size(); i++) {
                String raw = i < record.length ? record[i] : null;
                String v = raw == null ? null : (raw.isEmpty() ? null : raw);
                Object parsed = ValueInference.parse(v);
                row.add(parsed);
                if (parsed != null) {
                    FieldType cur = cols.get(header.get(i));
                    FieldType inf = ValueInference.infer(parsed);
                    if (shouldUpgrade(cur, inf)) {
                        cols.put(header.get(i), inf);
                    }
                }
            }
            rows.add(row);
        }
        return new Parsed(cols, rows);
    }

    /** 类型升级规则: 仅向更宽数值/具体类型单向提升 */
    private static boolean shouldUpgrade(FieldType cur, FieldType inf) {
        if (cur == FieldType.STRING) {
            return inf == FieldType.INT || inf == FieldType.DOUBLE || inf == FieldType.BOOL;
        }
        return cur == FieldType.INT && inf == FieldType.DOUBLE;
    }

    private static final class Parsed {
        final LinkedHashMap<String, FieldType> colTypes;
        final List<List<Object>> rows;

        Parsed(LinkedHashMap<String, FieldType> colTypes, List<List<Object>> rows) {
            this.colTypes = colTypes;
            this.rows = rows;
        }
    }
}