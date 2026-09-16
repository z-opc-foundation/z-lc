package com.zifang.z.lc.core.adapter;

import com.zifang.util.core.meta.Result;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.z.lc.common.dto.DictItemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Meta 适配器: 调用 z-meta 的元数据 API.
 * <p>
 * 设计哲学:
 * HTTP 走 z-opc 自己的 z-util-http (Library-First), 通过 {@link CtcAdapter#doGet(String, Map)} 共享
 * GET 工具方法. 缓存策略: dict items 5 分钟 TTL (字典数据变更频率低, 缓存大幅降低 z-meta 出口).
 * entity/field 定义由 EventReplayService 从本地 z_lc_event 直接计算, 不走 HTTP.
 */
@Component
public class MetaAdapter implements Adapter {

    public static final String NAME = "meta";
    private static final Logger log = LogManager.getLogger(MetaAdapter.class);

    private final com.github.benmanes.caffeine.cache.Cache<String, List<DictItemDTO>> dictCache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(5))
                    .maximumSize(500)
                    .build();

    @Value("${z-lc.adapter.meta.base-url:http://localhost:8888}")
    private String baseUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public void init() {
        log.info("MetaAdapter initialized, baseUrl={} (via z-util-http)", baseUrl);
    }

    /**
     * 取字典项 (5 分钟缓存).
     */
    public List<DictItemDTO> listDictItems(String tenantCode, String dictCode) {
        if (dictCode == null || dictCode.isEmpty()) {
            return Collections.emptyList();
        }
        String key = (tenantCode == null ? "" : tenantCode) + "|" + dictCode;
        return dictCache.get(key, k -> fetchDictItems(tenantCode, dictCode));
    }

    private List<DictItemDTO> fetchDictItems(String tenantCode, String dictCode) {
        String url = baseUrl + "/dict/items/get?dictCode=" + dictCode;
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        if (tenantCode != null && !tenantCode.isEmpty()) {
            headers = new HashMap<>(headers);
            headers.put("X-Tenant-Code", tenantCode);
        }
        log.debug("MetaAdapter → GET {}", url);
        HttpExecutionResult res = CtcAdapter.doGet(url, headers);
        if (!res.isSuccess()) {
            log.warn("MetaAdapter.listDictItems failed for dictCode={}: status={} err={}",
                    dictCode, res.getStatus(), res.getError());
            return Collections.emptyList();
        }
        try {
            Result<List<DictItemDTO>> r = JsonUtil.fromJson(
                    res.getBody(),
                    new TypeReference<Result<List<DictItemDTO>>>() {
                    });
            if (r == null || r.getData() == null) {
                return Collections.emptyList();
            }
            if (r.getCode() != 200) {
                log.warn("MetaAdapter.listDictItems business error for dictCode={}: code={} msg={}",
                        dictCode, r.getCode(), r.getMessage());
                return Collections.emptyList();
            }
            return r.getData();
        } catch (Exception ex) {
            log.warn("MetaAdapter.listDictItems parse error for dictCode={}: {}", dictCode, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 健康检查: 探测 z-meta 是否在线.
     */
    public boolean ping() {
        try {
            HttpExecutionResult res = CtcAdapter.doGet(
                    baseUrl + "/app/list?pageNum=1&pageSize=1",
                    JwtAwareHttpSupport.currentAuthHeaders());
            return res.isSuccess();
        } catch (Exception ex) {
            log.warn("MetaAdapter.ping failed: {}", ex.getMessage());
            return false;
        }
    }
}
