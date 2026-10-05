package com.zifang.z.lc.core.adapter;

import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.lc.common.dto.DictItemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
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
        if (!CtcAdapter.httpAccepted(res)) {
            log.warn("MetaAdapter.listDictItems failed for dictCode={}: status={} err={}",
                    dictCode, res.getStatus(), res.getError());
            return Collections.emptyList();
        }
        try {
            // 逐格读而非 JsonUtil.fromJson(body, new TypeReference<Result<List<DictItemDTO>>>(){})：
            // 实测那份引擎反序列化不出 Result 与泛型（CtcAdapter.ENVELOPE_NOTE 有完整记录），
            // 旧写法恒抛 ClassCastException 并被吞成空列表 —— 而本方法是全仓唯一有真实
            // 调用方的适配器方法（DictResolveProcessor:57/:89），等于字典标签从来没解析出来过。
            JsonObject envelope = JsonUtil.parseObject(res.getBody());
            if (envelope == null) {
                return Collections.emptyList();
            }
            Integer code = envelope.getInt("code");
            if (code != null && code != 200) {
                log.warn("MetaAdapter.listDictItems business error for dictCode={}: code={} msg={}",
                        dictCode, code, envelope.getString("message"));
                return Collections.emptyList();
            }
            Object data = envelope.get("data");
            if (!(data instanceof JsonArray)) {
                return Collections.emptyList();
            }
            JsonArray arr = (JsonArray) data;
            List<DictItemDTO> out = new ArrayList<>(arr.size());
            for (Object item : arr) {
                if (item instanceof JsonObject) {
                    DictItemDTO dto = toDictItem((JsonObject) item);
                    if (dto != null) {
                        out.add(dto);
                    }
                }
            }
            return out;
        } catch (Exception ex) {
            log.warn("MetaAdapter.listDictItems parse error for dictCode={}: {}", dictCode, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 逐格读回 {@link DictItemDTO}。
     * <p>
     * {@code sortOrder} 用 {@code getInt}（内部 {@code instanceof Number}）而不是 {@code getString}：
     * 后者对非 String 一律返回 null，JSON 里的 {@code 1} 是数字，会被悄悄读成 null。
     * <b>itemValue 缺省回落到 itemCode</b>：字典项没显式配存储值时按编码用，与
     * {@code DictResolveProcessor.matches} 的取值口径一致（它先看 itemValue 再看 itemCode）。
     */
    private static DictItemDTO toDictItem(JsonObject o) {
        DictItemDTO dto = new DictItemDTO();
        Long id = o.getLong("id");
        dto.setId(id);
        dto.setTenantCode(o.getString("tenantCode"));
        dto.setDictCode(o.getString("dictCode"));
        dto.setItemCode(o.getString("itemCode"));
        dto.setItemLabel(o.getString("itemLabel"));
        String value = o.getString("itemValue");
        dto.setItemValue(value == null ? o.getString("itemCode") : value);
        dto.setSortOrder(o.getInt("sortOrder"));
        dto.setDescription(o.getString("description"));
        return dto;
    }

    /**
     * 健康检查: 探测 z-meta 是否在线.
     */
    public boolean ping() {
        try {
            HttpExecutionResult res = CtcAdapter.doGet(
                    baseUrl + "/app/list?pageNum=1&pageSize=1",
                    JwtAwareHttpSupport.currentAuthHeaders());
            return CtcAdapter.httpAccepted(res);
        } catch (Exception ex) {
            log.warn("MetaAdapter.ping failed: {}", ex.getMessage());
            return false;
        }
    }
}
