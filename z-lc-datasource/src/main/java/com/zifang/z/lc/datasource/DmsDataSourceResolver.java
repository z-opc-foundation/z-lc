package com.zifang.z.lc.datasource;

import com.zifang.z.dms.core.model.DataSourceDef;
import com.zifang.z.dms.core.pool.DataSourcePool;
import com.zifang.z.dms.core.service.DataSourceService;
import com.zifang.z.lc.core.datasource.LcDataSourceCatalogItem;
import com.zifang.z.lc.core.datasource.LcDataSourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;

/**
 * z-dms 桥接实现（孵化模块，见 lead 009/013 §六拍板记录）。
 * <p>
 * 目录（{@link #list()}）来自 {@code DataSourceService.list()}（z-dms 登记面），
 * 连接（{@link #resolve(String)}）来自 {@code DataSourceService.require(id)}（z-dms 建池面）——
 * z-lc 侧不建池、不碰凭据。{@code DataSourceService} 缺席时按"目录未装配"降级。
 */
public class DmsDataSourceResolver implements LcDataSourceResolver {

    private static final Logger log = LoggerFactory.getLogger(DmsDataSourceResolver.class);

    private final ObjectProvider<DataSourceService> serviceProvider;

    public DmsDataSourceResolver(ObjectProvider<DataSourceService> serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public boolean available() {
        return serviceProvider.getIfAvailable() != null;
    }

    @Override
    public List<LcDataSourceCatalogItem> list() {
        DataSourceService service = serviceProvider.getIfAvailable();
        List<LcDataSourceCatalogItem> out = new ArrayList<>();
        if (service == null) {
            return out;
        }
        for (DataSourceDef def : service.list()) {
            out.add(new LcDataSourceCatalogItem(def.getId(), def.getName(),
                    def.getType() == null ? null : def.getType().name(), def.getCredentialRef()));
        }
        return out;
    }

    @Override
    public DataSource resolve(String id) {
        DataSourceService service = serviceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("数据源目录未装配（z-dms 服务 bean 缺席）: " + id);
        }
        DataSourceDef def = service.find(id);
        if (def == null) {
            throw new IllegalArgumentException("数据源目录里不存在 id: " + id);
        }
        DataSourcePool pool = service.require(id);
        log.debug("按目录 id={} 取得 z-dms 池连接（type={}）", id, def.getType());
        return new PoolDataSourceAdapter(pool);
    }

    /**
     * 把 z-dms 的 {@link DataSourcePool} 适配成 {@link DataSource}。
     * 只有 {@link #getConnection()} 是真的；其余池管理操作（印字/日志/包装）不属于
     * z-lc 的职责面，直接拒绝。适配器不持有关闭权 —— 池的生命周期归 z-dms。
     */
    static final class PoolDataSourceAdapter implements DataSource {

        private final DataSourcePool pool;

        PoolDataSourceAdapter(DataSourcePool pool) {
            this.pool = pool;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return pool.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new UnsupportedOperationException("z-lc 数据源目录不受理指定账号的连接（凭据归 z-dms）");
        }

        @Override
        public PrintWriter getLogWriter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLoginTimeout(int seconds) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getLoginTimeout() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Not a wrapper for " + iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}
