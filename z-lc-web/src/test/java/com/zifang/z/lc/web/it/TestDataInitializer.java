package com.zifang.z.lc.web.it;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

/**
 * 集成测试的建表与种子数据.
 * <p>
 * 不能用 spring.sql.init: 该自动配置带 @ConditionalOnSingleCandidate(DataSource.class),
 * 而 z-lc 同时有 spring.datasource(Hikari) 与 dataSourceLc(Druid) 两个 DataSource, 条件不满足
 * 会直接退避 —— 与 z-lc-admin dev profile 里 {@code DevSchemaInitializer} 遇到的是同一个坑.
 * <p>
 * @PostConstruct 而不是 ApplicationRunner: @SpringBootTest 不走 SpringApplication.run,
 * ApplicationRunner/CommandLineRunner 在测试里根本不会被调用.
 * <p>
 * 两个 DataSource 指向同一个 jdbc:h2:mem:zlcweb 实例, 所以脚本跑一次即可; schema.sql 全量
 * IF NOT EXISTS、data.sql 全量 MERGE, 重复执行安全.
 */
@Component
public class TestDataInitializer {

    @Autowired
    private List<DataSource> dataSources;

    @PostConstruct
    public void initialize() throws Exception {
        EncodedResource schema = new EncodedResource(new ClassPathResource("schema.sql"), StandardCharsets.UTF_8);
        EncodedResource data = new EncodedResource(new ClassPathResource("data.sql"), StandardCharsets.UTF_8);
        IllegalStateException last = null;
        for (DataSource dataSource : dataSources) {
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, schema);
                ScriptUtils.executeSqlScript(connection, data);
                return;
            } catch (Exception ex) {
                last = ex instanceof IllegalStateException ? (IllegalStateException) ex
                        : new IllegalStateException(ex.getMessage(), ex);
            }
        }
        throw new IllegalStateException("z-lc-web 集成测试建表失败", last);
    }
}
