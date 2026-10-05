package com.zifang.z.lc.core.adapter;

import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OSS 适配器: 调用 z-oss 的对象存储 API, 处理 FILE / IMAGE 类型字段.
 * <p>
 * 典型用法:
 * - 用户上传附件 → 走 z-oss 拿到访问 URL → 字段保存 URL 字符串
 * - 流程结束时批量清理临时文件 → 调 z-oss 删除接口
 */
@Component
public class OssAdapter implements Adapter {

    public static final String NAME = "oss";
    private static final Logger log = LogManager.getLogger(OssAdapter.class);

    @Value("${z-lc.adapter.oss.base-url:http://localhost:8888}")
    private String baseUrl;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public void init() {
        log.info("OssAdapter initialized, baseUrl={}", baseUrl);
    }

    /**
     * 获取对象签名 URL（用于前端直传后获取访问地址）
     *
     * @param bucketName 桶名
     * @param objectKey  对象 key
     * @param expiresSec URL 有效期秒数, 默认 3600
     * @return 签名 URL; 失败时返回 null
     */
    public String generateDownloadUrl(String bucketName, String objectKey, Integer expiresSec) {
        if (bucketName == null || objectKey == null) {
            return null;
        }
        int expires = expiresSec == null ? 3600 : expiresSec;
        String url = baseUrl + "/api/v1/object/sign-url?bucket=" + bucketName
                + "&key=" + objectKey + "&expires=" + expires;
        Map<String, String> headers = JwtAwareHttpSupport.currentAuthHeaders();
        log.debug("OssAdapter → GET {}", url);
        HttpExecutionResult res = CtcAdapter.doGet(url, headers);
        if (!CtcAdapter.httpAccepted(res)) {
            log.warn("OssAdapter.generateDownloadUrl failed: status={} err={}",
                    res.getStatus(), res.getError());
            return null;
        }
        try {
            // 逐格读而非 JsonUtil.fromJson(body, Result.class)：实测那份引擎反序列化不出 Result
            // （CtcAdapter.ENVELOPE_NOTE 有完整实测记录）。
            JsonObject envelope = JsonUtil.parseObject(res.getBody());
            if (envelope == null) {
                return null;
            }
            Object data = envelope.get("data");
            if (data == null) {
                return null;
            }
            return data.toString();
        } catch (Exception ex) {
            log.warn("OssAdapter.generateDownloadUrl parse error: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 删除对象.
     *
     * @param bucketName 桶名
     * @param objectKey  对象 key
     * @return true 成功, false 失败
     */
    public boolean deleteObject(String bucketName, String objectKey) {
        if (bucketName == null || objectKey == null) {
            return false;
        }
        String url = baseUrl + "/api/v1/object/" + bucketName + "?key=" + objectKey;
        Map<String, String> headers = new HashMap<>(JwtAwareHttpSupport.currentAuthHeaders());
        headers.put("Content-Type", "application/json; charset=UTF-8");
        log.debug("OssAdapter → DELETE {}", url);
        HttpExecutionResult res = CtcAdapter.doRequest("DELETE", url, headers, null);
        if (!CtcAdapter.httpAccepted(res)) {
            log.warn("OssAdapter.deleteObject failed: status={} err={}",
                    res.getStatus(), res.getError());
            return false;
        }
        return true;
    }

    /**
     * 健康检查.
     */
    public boolean ping() {
        try {
            HttpExecutionResult res = CtcAdapter.doGet(
                    baseUrl + "/api/v1/bucket/list",
                    JwtAwareHttpSupport.currentAuthHeaders());
            return CtcAdapter.httpAccepted(res);
        } catch (Exception ex) {
            return false;
        }
    }
}
