package com.zifang.z.report.web.config;

import com.zifang.z.report.book.BookService;
import com.zifang.z.report.book.impl.InMemoryBookService;
import com.zifang.z.report.common.schema.DataSourceDef;
import com.zifang.z.report.common.schema.DataSourceType;
import com.zifang.z.report.dataset.DatasetExecutor;
import com.zifang.z.report.datasource.impl.JdbcReportDataSource;
import com.zifang.z.report.datasource.spi.DataSourceRegistry;
import com.zifang.z.report.render.EChartsRenderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;

/**
 * 核心服务装配 (M1 内存态)。
 * JDBC 自动装配遵循 z-boot 约定: z.base.db.report.* 存在时自动注册,
 * 缺省项 fallback 到 z.base.db.default.* (与 ModuleDataSourceTemplate 语义一致)。
 */
@Configuration
public class ZReportBeanConfig {

    @Bean
    public DataSourceRegistry dataSourceRegistry(Environment env) {
        DataSourceRegistry registry = new DataSourceRegistry();
        String host = env.getProperty("z.base.db.report.host", env.getProperty("z.base.db.default.host"));
        if (host != null && !host.isEmpty()) {
            registry.register(new JdbcReportDataSource(jdbcDefFromZBoot(env, host)));
        }
        return registry;
    }

    /** z.base.db.report.* → DataSourceDef (fallback 链对齐 z-boot ModuleDataSourceTemplate) */
    private static DataSourceDef jdbcDefFromZBoot(Environment env, String host) {
        Map<String, String> props = new HashMap<>();
        props.put(JdbcReportDataSource.PROP_HOST, host);
        props.put(JdbcReportDataSource.PROP_PORT,
                env.getProperty("z.base.db.report.port", env.getProperty("z.base.db.default.port", "3306")));
        props.put(JdbcReportDataSource.PROP_DATABASE, env.getProperty(
                "z.base.db.report.database", env.getProperty("z.base.db.default.database", "")));
        props.put(JdbcReportDataSource.PROP_USERNAME,
                env.getProperty("z.base.db.report.username", env.getProperty("z.base.db.default.username", "root")));
        props.put(JdbcReportDataSource.PROP_PASSWORD,
                env.getProperty("z.base.db.report.password", env.getProperty("z.base.db.default.password", "")));
        return new DataSourceDef("z-base-db-report", "z-boot auto (z.base.db.report)", DataSourceType.JDBC, props);
    }

    @Bean
    public DatasetExecutor datasetExecutor(DataSourceRegistry registry, Environment env) {
        // 依赖倒置: dataset 模块不感知 datasource, 源解析在此注入。
        // M2: 包装 TTL 结果缓存 (同 schema 定义窗口内复用, 避免每次渲染全量执行)
        long ttl = env.getProperty("z.report.cache.ttl-ms", Long.class, 60_000L);
        return new com.zifang.z.report.dataset.CachingDatasetExecutor(
                (sourceId, table) -> registry.require(sourceId).read(table), ttl, 128);
    }

    @Bean
    public EChartsRenderService eChartsRenderService() {
        return new EChartsRenderService();
    }

    @Bean
    public BookService bookService() {
        return new InMemoryBookService();
    }

    /**
     * AI 服务 (M3): LLM 未配置时注入提示型缺省实现 (调用即抛出配置指引),
     * 保证 starter 零配置可启动; 配置 z.report.llm.* 后自动启用 OpenAI 兼容实现。
     */
    @Bean
    public com.zifang.z.report.ai.AiReportService aiReportService(Environment env) {
        String apiKey = env.getProperty("z.report.llm.api-key", env.getProperty("LLM_API_KEY"));
        com.zifang.z.report.ai.LlmClient client;
        if (apiKey != null && !apiKey.isEmpty()) {
            com.zifang.z.report.ai.LlmConfig cfg = new com.zifang.z.report.ai.LlmConfig();
            cfg.setBaseUrl(env.getProperty("z.report.llm.base-url", "https://api.openai.com/v1"));
            cfg.setApiKey(apiKey);
            cfg.setModel(env.getProperty("z.report.llm.model", "gpt-4o-mini"));
            client = new com.zifang.z.report.ai.impl.OpenAiCompatLlmClient(cfg);
        } else {
            client = messages -> {
                throw new IllegalStateException(
                        "LLM 未配置: 请设置 z.report.llm.api-key (或环境变量 LLM_API_KEY) 后重启");
            };
        }
        return new com.zifang.z.report.ai.AiReportService(client);
    }
}
