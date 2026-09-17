package com.zifang.z.lc.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP 请求工具 — 蒸馏自 ace-platform-core
 * {@code RequestUtils} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供请求参数签名和 JSON 生成能力. 适用于低代码平台与外部系统对接时的
 * HMAC-SHA256 签名验证场景.
 *
 * <p>蒸馏时移除了 ace 对 Gson 的依赖, 改为 Jackson ObjectMapper.
 *
 * <p>典型场景：
 * <ul>
 *   <li>OpenAPI 接口签名验证</li>
 *   <li>第三方回调请求验签</li>
 *   <li>微服务间请求防篡改</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcRequestUtils {

    private ZLcRequestUtils() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AND = "&";
    private static final String EQUAL = "=";

    /**
     * 生成带签名的请求 JSON.
     *
     * @param request 请求参数 (会被添加 signature 字段)
     * @param key     签名密钥
     * @return 签名后的 JSON 字符串
     * @throws Exception 签名失败时抛出
     */
    public static String generateRequestJson(Map<String, String> request, String key) throws Exception {
        String data = generateSignString(request);
        String signature = getHMAC(data.getBytes(StandardCharsets.UTF_8),
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        request.put("signature", signature);
        return MAPPER.writeValueAsString(request);
    }

    /**
     * 生成签名字符串 (按 key 排序拼接).
     *
     * @param props 请求参数
     * @return 签名字符串 (如 "key1=value1&key2=value2")
     */
    public static String generateSignString(Map<String, String> props) {
        StringBuilder sb = new StringBuilder();
        List<String> keys = new ArrayList<>(props.keySet());
        Collections.sort(keys);

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = props.get(key);
            if (value == null || "signature".equals(key)) {
                continue;
            }
            sb.append(key).append(EQUAL).append(value);
            if (i < keys.size() - 1) {
                sb.append(AND);
            }
        }
        return sb.toString();
    }

    /**
     * HMAC 签名.
     *
     * @param data      待签名数据
     * @param key       密钥
     * @param algorithm 算法 (如 "HmacSHA256")
     * @return Base64 编码的签名
     * @throws Exception 签名失败时抛出
     */
    public static String getHMAC(byte[] data, byte[] key, String algorithm) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(key, algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(signingKey);
        return Base64.getEncoder().encodeToString(mac.doFinal(data));
    }
}
