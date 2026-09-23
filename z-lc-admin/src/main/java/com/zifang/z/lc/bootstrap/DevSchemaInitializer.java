package com.zifang.z.lc.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * dev profile 下把 db/schema-h2.sql 灌进内存库, 让 z-lc 可以零外部依赖启动.
 * <p>
 * 为什么不能用 spring.sql.init: 该自动配置带 @ConditionalOnSingleCandidate(DataSource.class),
 * 而 z-lc 同时存在 spring.datasource(Hikari) 与 dataSourceLc(Druid) 两个 DataSource bean,
 * 条件不满足 -> 自动配置直接退避, schema 永远不执行 (表现为 "this database is empty").
 * <p>
 * 两个 DataSource 指向同一个 jdbc:h2:mem:zlc 实例, 所以脚本只需成功执行一次;
 * schema-h2.sql 全量使用 CREATE TABLE IF NOT EXISTS, 重复执行安全.
 */
@Component
@Profile("dev")
@Order(Integer.MIN_VALUE)
public class DevSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevSchemaInitializer.class);

    private static final String REQUIRED = "Z_LC_APP";

    @Autowired
    private List<DataSource> dataSources;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("[z-lc dev] {} DataSource(s) detected, applying db/schema-h2.sql", dataSources.size());
        EncodedResource script = new EncodedResource(new ClassPathResource("db/schema-h2.sql"), StandardCharsets.UTF_8);
        Exception last = null;
        for (DataSource ds : dataSources) {
            try (Connection conn = ds.getConnection()) {
                log.info("[z-lc dev] applying schema on {}", conn.getMetaData().getURL());
                ScriptUtils.executeSqlScript(conn, script);
                List<String> tables = tables(conn);
                log.info("[z-lc dev] schema applied, {} tables: {}", tables.size(), tables);
                if (tables.contains(REQUIRED)) {
                    return;
                }
                last = new IllegalStateException("schema applied but " + REQUIRED + " still missing");
            } catch (Exception ex) {
                last = ex;
                log.error("[z-lc dev] schema apply failed on one DataSource: {}", ex.getMessage(), ex);
            }
        }
        throw new IllegalStateException("db/schema-h2.sql could not be applied",
                last == null ? null : last);
    }

    private List<String> tables(Connection conn) throws Exception {
        List<String> out = new ArrayList<String>();
        DatabaseMetaData md = conn.getMetaData();
        for (String schema : new String[]{"PUBLIC", "public", null}) {
            try (ResultSet rs = md.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    out.add(rs.getString("TABLE_NAME").toUpperCase());
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        return out;
    }
}
