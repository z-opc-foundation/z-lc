package com.zifang.z.lc.datasource;

import com.zifang.z.dms.core.model.DataSourceDef;
import com.zifang.z.dms.core.model.DataSourceType;
import com.zifang.z.dms.core.pool.DataSourcePool;
import com.zifang.z.dms.core.service.DataSourceService;
import com.zifang.z.lc.report.datasource.impl.JdbcReportDataSource;
import com.zifang.z.lc.report.datasource.spi.ExternalReportDataSourceResolver;
import com.zifang.z.lc.report.datasource.spi.ReportDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 报表域外部目录桥（z-dms 实现，2026-10-26 报表域收口）。
 * <p>
 * 链路：报表 REST {@code POST /api/lc/report/datasource/external} → 本实现按 id 查
 * z-dms 目录（{@link DataSourceService#find}）→ 建池面取池（{@link DataSourceService#require}）→
 * 池连接（{@link DataSourcePool#dataSource()}）递给 {@link JdbcReportDataSource} 外部池构造器。
 * <p>
 * 纪律：凭据与池生命周期全归 z-dms —— 报表域只拿到一个能借连接的池句柄，
 * {@code close()} 不关池（换绑/关闭语义在 z-dms 登记链里）。
 */
public class DmsExternalReportDataSourceResolver implements ExternalReportDataSourceResolver {

    private static final Logger log = LoggerFactory.getLogger(DmsExternalReportDataSourceResolver.class);

    /** options 透传：进内存的行数上限 */
    public static final String OPT_MAX_ROWS = "max-rows";

    private final ObjectProvider<DataSourceService> serviceProvider;

    public DmsExternalReportDataSourceResolver(ObjectProvider<DataSourceService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public boolean available() {
        return serviceProvider.getIfAvailable() != null;
    }

    @Override
    public ReportDataSource resolve(String id, Map<String, String> options) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("数据源 id required");
        }
        DataSourceService service = serviceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("数据源目录未装配（z-dms 服务 bean 缺席）: " + id);
        }
        DataSourceDef dmsDef = service.find(id.trim());
        if (dmsDef == null) {
            throw new IllegalArgumentException("数据源目录里不存在 id: " + id);
        }
        if (dmsDef.getType() != DataSourceType.JDBC) {
            throw new IllegalArgumentException(
                    "报表外部目录源仅支持 JDBC 型, [" + id + "] type=" + dmsDef.getType());
        }
        DataSourcePool pool = service.require(id.trim());
        log.info("报表源 [{}] ({}) 经 z-dms 目录建池接入", id, dmsDef.getName());

        Map<String, String> props = new LinkedHashMap<>();
        if (options != null && options.get(OPT_MAX_ROWS) != null) {
            props.put(JdbcReportDataSource.PROP_MAX_ROWS, options.get(OPT_MAX_ROWS));
        }
        com.zifang.z.lc.report.common.schema.DataSourceDef reportDef =
                new com.zifang.z.lc.report.common.schema.DataSourceDef(
                        dmsDef.getId(), dmsDef.getName(),
                        com.zifang.z.lc.report.common.schema.DataSourceType.JDBC, props);
        return new JdbcReportDataSource(reportDef, pool.dataSource());
    }
}
