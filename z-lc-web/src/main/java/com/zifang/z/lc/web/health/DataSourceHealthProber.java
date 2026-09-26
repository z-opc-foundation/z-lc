package com.zifang.z.lc.web.health;

import com.zifang.z.lc.web.config.DataSourceConfigGuard;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 逐个连接池真探活，产出健康检查里那一份 {@code sources}.
 * <p>
 * 存在的理由：{@code /api/lc/health} 的 {@code status} 从前是硬编码的 {@code "UP"}，一个字都没问过库
 * （实测：部署件在没有任何可用连接的情况下回 200 {@code "status":"UP"}，而同一个进程上
 * {@code /api/lc/app/list} 回 {@code http=500 处理失败|Connection refused}）。健康检查是部署闸唯一的
 * 判据，它说 UP 而库连不上，闸就变成谎话的放大器。
 * <p>
 * 探活跑在独立线程上并带超时：连接池 {@code maxWait} 可能是 -1（无限等），直接在请求线程里
 * {@code getConnection()} 会让健康检查自己挂死，那比报 DOWN 糟得多。
 */
@Component
public class DataSourceHealthProber {

    private static final long DEFAULT_TIMEOUT_MS = 2000L;
    private static final int DETAIL_MAX_LEN = 200;

    private final long timeoutMs;
    private final ExecutorService executor;

    public DataSourceHealthProber() {
        this(DEFAULT_TIMEOUT_MS);
    }

    DataSourceHealthProber(final long timeoutMs) {
        this.timeoutMs = timeoutMs;
        this.executor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "lc-health-probe");
                t.setDaemon(true);
                return t;
            }
        });
    }

    /**
     * @param pools 上下文里全部 DataSource bean，key 为 bean 名
     * @return {@code {status, sources:[{name,status,database,url,latencyMs|detail}]}}；
     *         {@code status} 只有在至少有一个池且全部池都 UP 时才是 UP
     */
    public Map<String, Object> report(Map<String, DataSource> pools) {
        List<Map<String, Object>> sources = new ArrayList<>();
        boolean allUp = pools != null && !pools.isEmpty();
        if (pools != null) {
            for (Map.Entry<String, DataSource> entry : pools.entrySet()) {
                Map<String, Object> one = probe(entry.getKey(), entry.getValue());
                sources.add(one);
                if (!"UP".equals(one.get("status"))) {
                    allUp = false;
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", allUp ? "UP" : "DOWN");
        out.put("sources", sources);
        if (!allUp) {
            out.put("reason", (pools == null || pools.isEmpty())
                    ? "上下文里没有任何 DataSource bean"
                    : firstDownReason(sources));
        }
        return out;
    }

    private static String firstDownReason(List<Map<String, Object>> sources) {
        for (Map<String, Object> s : sources) {
            if (!"UP".equals(s.get("status"))) {
                return s.get("name") + ": " + s.get("detail");
            }
        }
        return "unknown";
    }

    private Map<String, Object> probe(final String name, final DataSource ds) {
        Future<Map<String, Object>> future = null;
        try {
            future = executor.submit(() -> connect(name, ds));
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            if (future != null) {
                future.cancel(true);
            }
            return down(name, "probe timed out after " + timeoutMs
                    + "ms (池可能借干了或 maxWait=-1 在无限等)");
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() == null ? ee : ee.getCause();
            return down(name, cause.getClass().getSimpleName() + ": " + brief(cause.getMessage()));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return down(name, "probe interrupted");
        } catch (RejectedExecutionException re) {
            return down(name, "probe executor rejected: " + brief(re.getMessage()));
        }
    }

    private Map<String, Object> connect(String name, DataSource ds) throws Exception {
        long startNs = System.nanoTime();
        try (Connection conn = ds.getConnection()) {
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
                if (!rs.next()) {
                    throw new IllegalStateException("SELECT 1 没有返回任何行");
                }
            }
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000L;
            DatabaseMetaData md = conn.getMetaData();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", name);
            out.put("status", "UP");
            out.put("database", brief(md.getDatabaseProductName() + " " + md.getDatabaseProductVersion(), 60));
            out.put("url", DataSourceConfigGuard.mask(md.getURL()));
            out.put("latencyMs", Long.valueOf(latencyMs));
            return out;
        }
    }

    private Map<String, Object> down(String name, String detail) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", name);
        out.put("status", "DOWN");
        out.put("url", "unknown");
        out.put("detail", detail);
        return out;
    }

    private String brief(String message) {
        return brief(message, DETAIL_MAX_LEN);
    }

    private static String brief(String value, int max) {
        if (value == null) {
            return "";
        }
        String flat = value.replace('\n', ' ').replace('\r', ' ').trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
