package com.zifang.z.lc.core.adapter;

import com.zifang.util.core.meta.Result;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.json.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Mist 适配器: 调用 z-mist 的密钥服务, 提供字段级加密/解密能力.
 * <p>
 * 设计要点:
 * - z-mist 自身在存储层透明加密 (AES/RSA), HTTP 接口不直接暴露明文加解密,
 * 这里通过保存/读取 secret 记录, 让用户能引用 z-mist 中已托管的密钥作为字段值源.
 * - 适配器对调用方屏蔽 HTTP 细节, 失败返回 null + WARN 日志, 不抛异常阻塞主流程.
 */
@Component
public class MistAdapter implements Adapter {

    public static final String NAME = "mist";
    private static final Logger log = LogManager.getLogger(MistAdapter.class);

    @Value("${z-lc.adapter.mist.base-url:http://localhost:8888}")
    private String baseUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 30;
    }

    @Override
    public void init() {
        log.info("MistAdapter initialized, baseUrl={}", baseUrl);
    }

    /**
     * 从 z-mist 读取指定密钥的解密值.
     *
     * @param secretKey 密钥标识 (例如 "database/password")
     * @param group     分组, 默认 DEFAULT_GROUP
     * @param namespace 命名空间, 默认 default
     * @return 解密后的明文; 失败时返回 null
     */
    public String decrypt(String secretKey, String group, String namespace) {
        if (secretKey == null || secretKey.isEmpty()) {
            return null;
        }
        String url = baseUrl + "/api/secret/" + secretKey
                + "?group=" + (group == null ? "DEFAULT_GROUP" : group)
                + "&namespace=" + (namespace == null ? "default" : namespace);
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        log.debug("MistAdapter → GET {}", url);
        HttpExecutionResult res = CtcAdapter.doGet(url, headers);
        if (!res.isSuccess()) {
            log.warn("MistAdapter.decrypt failed: status={} err={}", res.getStatus(), res.getError());
            return null;
        }
        try {
            Result<?> r = JsonUtil.fromJson(res.getBody(), Result.class);
            if (r == null || r.getData() == null) {
                return null;
            }
            // z-mist 返回的 data 是 Map, 其中 encryptedValue 是加密值, decryptValue 是解密值
            Object data = r.getData();
            if (data instanceof Map) {
                Object plain = ((Map<?, ?>) data).get("decryptValue");
                if (plain == null) {
                    plain = ((Map<?, ?>) data).get("encryptedValue");
                }
                return plain == null ? null : plain.toString();
            }
            return data.toString();
        } catch (Exception ex) {
            log.warn("MistAdapter.decrypt parse error: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 健康检查: 探测 z-mist 是否在线.
     */
    public boolean ping() {
        try {
            HttpExecutionResult res = CtcAdapter.doGet(
                    baseUrl + "/api/secret/list?pageNum=1&pageSize=1",
                    JwtAwareHttpSupport.currentAuthHeaders());
            return res.isSuccess();
        } catch (Exception ex) {
            return false;
        }
    }
}
