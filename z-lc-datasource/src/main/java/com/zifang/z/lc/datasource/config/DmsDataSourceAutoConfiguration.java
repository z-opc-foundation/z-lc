package com.zifang.z.lc.datasource.config;

import com.zifang.z.dms.core.service.DataSourceService;
import com.zifang.z.lc.core.datasource.LcDataSourceResolver;
import com.zifang.z.lc.datasource.DmsDataSourceResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * z-lc 数据源管理域自动装配（V1 孵化，2026-10-26）。
 * <p>
 * 经本模块 {@code META-INF/spring.factories} 挂 EnableAutoConfiguration —— z-lc-admin
 * 与 main-starter 都无需手工 import。两层开关：
 * <ul>
 *   <li>目录桥接：classpath 有 z-dms（{@code DataSourceService} 在位）即生效；</li>
 *   <li>种子自检：{@code z.lc.dms.seed=true} 才注册 dev 种子源（仅开发自检用，
 *       生产宿主绝不开启）。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class DmsDataSourceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(DataSourceService.class)
    @ConditionalOnMissingBean(LcDataSourceResolver.class)
    public static class DmsResolverConfiguration {

        @Bean
        public LcDataSourceResolver lcDataSourceResolver(ObjectProvider<DataSourceService> serviceProvider) {
            return new DmsDataSourceResolver(serviceProvider);
        }
    }

    /**
     * 报表域外部目录桥（2026-10-26 收口）：report-datasource 的
     * {@code ExternalReportDataSourceResolver} SPI 落到 z-dms。
     * 条件与目录桥一致（classpath 有 z-dms）；报表 REST 面向 SPI，
     * 宿主想换目录底座时提供自己的实现即可覆盖。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({DataSourceService.class,
            com.zifang.z.lc.report.datasource.spi.ExternalReportDataSourceResolver.class})
    @ConditionalOnMissingBean(
            com.zifang.z.lc.report.datasource.spi.ExternalReportDataSourceResolver.class)
    public static class DmsReportBridgeConfiguration {

        @Bean
        public com.zifang.z.lc.report.datasource.spi.ExternalReportDataSourceResolver
        dmsExternalReportDataSourceResolver(ObjectProvider<DataSourceService> serviceProvider) {
            return new com.zifang.z.lc.datasource.DmsExternalReportDataSourceResolver(serviceProvider);
        }
    }

    /** 开发自检种子：往 z-dms 目录注册一个与 dev 主库同实例的 H2 源（dev-h2）。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "z.lc.dms", name = "seed", havingValue = "true")
    public static class DmsSeedConfiguration {

        @Bean
        public DmsDevSeed dmsDevSeed(ObjectProvider<DataSourceService> serviceProvider, Environment environment) {
            return new DmsDevSeed(serviceProvider, environment);
        }
    }
}
