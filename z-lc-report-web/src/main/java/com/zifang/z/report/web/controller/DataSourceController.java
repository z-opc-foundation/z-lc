package com.zifang.z.report.web.controller;

import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.datasource.impl.CsvReportDataSource;
import com.zifang.z.report.datasource.impl.InMemoryReportDataSource;
import com.zifang.z.report.datasource.impl.JdbcReportDataSource;
import com.zifang.z.report.datasource.impl.MemoryDataSource;
import com.zifang.z.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.report.common.schema.FieldType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据源板块 API: 内存源/JDBC 源/CSV 源注册 + 元数据探测。
 * JDBC 也可不经 REST, 由 z.base.db.report.* 配置自动装配 (z-boot 约定)。
 */
@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    private final DataSourceRegistry registry;

    public DataSourceController(DataSourceRegistry registry) {
        this.registry = registry;
    }

    /** 内存源注册请求体 */
    public static class MemorySourceRequest {
        private DataSourceDef def;
        private Map<String, List<Map<String, Object>>> tables;
        private Map<String, Map<String, FieldType>> colTypes;

        public DataSourceDef getDef() {
            return def;
        }

        public void setDef(DataSourceDef def) {
            this.def = def;
        }

        public Map<String, List<Map<String, Object>>> getTables() {
            return tables;
        }

        public void setTables(Map<String, List<Map<String, Object>>> tables) {
            this.tables = tables;
        }

        public Map<String, Map<String, FieldType>> getColTypes() {
            return colTypes;
        }

        public void setColTypes(Map<String, Map<String, FieldType>> colTypes) {
            this.colTypes = colTypes;
        }
    }

    @PostMapping("/memory")
    public Map<String, Object> registerMemory(@RequestBody MemorySourceRequest req) {
        if (req.getDef() == null) {
            req.setDef(new DataSourceDef());
            req.getDef().setType(DataSourceType.MEMORY);
        }
        registry.register(new MemoryDataSource(req.getDef(), req.getTables(), req.getColTypes()));
        return ok(req.getDef().getId());
    }

    /** JDBC(Mysql) 源: properties = host/port/database/username/password/max-rows (对齐 z-boot 连接参数) */
    @PostMapping("/jdbc")
    public Map<String, Object> registerJdbc(@RequestBody DataSourceDef def) {
        registry.register(new JdbcReportDataSource(def));
        return ok(def.getId());
    }

    /** CSV 源: properties = path 或 inline, max-rows */
    @PostMapping("/csv")
    public Map<String, Object> registerCsv(@RequestBody DataSourceDef def) {
        registry.register(new CsvReportDataSource(def));
        return ok(def.getId());
    }

    /** 内存 SQL 源注册请求体 */
    public static class MemorySqlRequest {
        private DataSourceDef def;
        /** 源 id → 要搬进内存的基表 */
        private Map<String, List<String>> baseTables;
        /** 数据单元 → SELECT */
        private Map<String, String> sqls;
        private Map<String, Map<String, FieldType>> colTypes;

        public DataSourceDef getDef() {
            return def;
        }

        public void setDef(DataSourceDef def) {
            this.def = def;
        }

        public Map<String, List<String>> getBaseTables() {
            return baseTables;
        }

        public void setBaseTables(Map<String, List<String>> baseTables) {
            this.baseTables = baseTables;
        }

        public Map<String, String> getSqls() {
            return sqls;
        }

        public void setSqls(Map<String, String> sqls) {
            this.sqls = sqls;
        }

        public Map<String, Map<String, FieldType>> getColTypes() {
            return colTypes;
        }

        public void setColTypes(Map<String, Map<String, FieldType>> colTypes) {
            this.colTypes = colTypes;
        }
    }

    /**
     * 内存 SQL 源: 基表来自已注册源 (JDBC/CSV/内存均可), 数据单元是一条跨源 join/聚合的 SELECT。
     * 基表必须先注册, 之后每次取数现读, 不需要重注册。
     */
    @PostMapping("/memory-sql")
    public Map<String, Object> registerMemorySql(@RequestBody MemorySqlRequest req) {
        DataSourceDef def = req.getDef();
        if (def == null) {
            throw new IllegalArgumentException("def required");
        }
        def.setType(DataSourceType.SQL);
        registry.register(new InMemoryReportDataSource(def, req.getBaseTables(), req.getSqls(),
                req.getColTypes(), registry));
        return ok(def.getId());
    }

    /** 已注册源清单 (前端 AI 页/源管理下拉用) */
    @org.springframework.web.bind.annotation.GetMapping
    public java.util.List<Map<String, Object>> list() {
        java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (com.zifang.z.report.datasource.spi.ReportDataSource s : registry.all()) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", s.def().getId());
            item.put("name", s.def().getName());
            item.put("type", s.def().getType() == null ? null : s.def().getType().name());
            out.add(item);
        }
        return out;
    }

    /** 源内数据单元清单 (元数据探测) */
    @org.springframework.web.bind.annotation.GetMapping("/{id}/tables")
    public List<String> tables(@org.springframework.web.bind.annotation.PathVariable("id") String id) {
        return registry.require(id).tables();
    }

    private static Map<String, Object> ok(String sourceId) {
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("ok", true);
        resp.put("sourceId", sourceId);
        return resp;
    }
}
