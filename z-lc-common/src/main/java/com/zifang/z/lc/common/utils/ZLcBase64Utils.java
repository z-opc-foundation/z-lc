package com.zifang.z.lc.common.utils;

import java.util.Base64;

/**
 * Base64 编解码工具 — 蒸馏自 ace-platform-core
 * {@code Base64Utils} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 Base64 编码/解码能力. 蒸馏时移除了 ace 对自定义 Base62/Base64 实现的依赖,
 * 改为 JDK 8 原生 {@link Base64} API.
 *
 * <p>典型场景：
 * <ul>
 *   <li>附件/图片 Base64 编码传输</li>
 *   <li>Token / 密钥编码</li>
 *   <li>请求签名 Base64 编码</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcBase64Utils {

    private ZLcBase64Utils() {
    }

    /**
     * Base64 编码.
     *
     * @param data 待编码字节数组
     * @return Base64 编码字符串
     */
    public static String encode(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 编码字符串.
     *
     * @param str 待编码字符串 (UTF-8)
     * @return Base64 编码字符串
     */
    public static String encode(String str) {
        if (str == null) {
            return null;
        }
        return encode(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Base64 解码.
     *
     * @param encoded Base64 编码字符串
     * @return 解码后的字节数组
     * @throws IllegalArgumentException 编码格式非法时抛出
     */
    public static byte[] decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        return Base64.getDecoder().decode(encoded);
    }

    /**
     * Base64 解码为字符串 (UTF-8).
     *
     * @param encoded Base64 编码字符串
     * @return 解码后的字符串
     * @throws IllegalArgumentException 编码格式非法时抛出
     */
    public static String decodeToString(String encoded) {
        if (encoded == null) {
            return null;
        }
        return new String(decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * URL 安全的 Base64 编码.
     *
     * @param data 待编码字节数组
     * @return URL 安全的 Base64 编码字符串
     */
    public static String encodeUrlSafe(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    /**
     * URL 安全的 Base64 解码.
     *
     * @param encoded URL 安全的 Base64 编码字符串
     * @return 解码后的字节数组
     */
    public static byte[] decodeUrlSafe(String encoded) {
        if (encoded == null) {
            return null;
        }
        return Base64.getUrlDecoder().decode(encoded);
    }
}
