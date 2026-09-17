package com.zifang.z.lc.common.utils;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA 加解密工具 — 蒸馏自 ace-platform-core
 * {@code RSAUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供 RSA 密钥对生成、公钥加密、私钥解密能力.
 * 蒸馏时移除了 ace 对 BouncyCastle / BusinessException 的依赖,
 * 改为纯 JDK 加密 API.
 *
 * <p>典型场景：
 * <ul>
 *   <li>API 接口密钥交换 (X-ACE-SECRET Header)</li>
 *   <li>敏感数据加密传输 (如密码、Token)</li>
 *   <li>电子签名验签</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcRSAUtil {

    private ZLcRSAUtil() {
    }

    private static final String KEY_ALGORITHM = "RSA";
    private static final String PUBLIC_KEY = "RSAPublicKey";
    private static final String PRIVATE_KEY = "RSAPrivateKey";

    /** 1024 位 RSA 公钥加密最大明文大小. */
    private static final int MAX_ENCRYPT_BLOCK = 117;

    /** 1024 位 RSA 私钥解密最大密文大小. */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 生成 RSA 密钥对.
     *
     * @param keySize 密钥长度 (如 2048)
     * @return 密钥对 Map ("RSAPublicKey" / "RSAPrivateKey")
     */
    public static Map<String, Object> initKey(int keySize) {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyPairGen.initialize(keySize);
            KeyPair keyPair = keyPairGen.generateKeyPair();
            Map<String, Object> keyMap = new HashMap<>(2);
            keyMap.put(PUBLIC_KEY, keyPair.getPublic());
            keyMap.put(PRIVATE_KEY, keyPair.getPrivate());
            return keyMap;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA 密钥生成失败", e);
        }
    }

    /**
     * 获取公钥 Base64 字符串.
     */
    public static String getPublicKeyStr(Map<String, Object> keyMap) {
        Key key = (Key) keyMap.get(PUBLIC_KEY);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 获取私钥 Base64 字符串.
     */
    public static String getPrivateKeyStr(Map<String, Object> keyMap) {
        Key key = (Key) keyMap.get(PRIVATE_KEY);
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * 公钥加密.
     *
     * @param data      待加密字符串
     * @param publicKey Base64 编码的公钥
     * @return Base64 编码的密文
     * @throws Exception 加密失败
     */
    public static String encrypt(String data, String publicKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePublic(new X509EncodedKeySpec(decoded));
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * 私钥解密.
     *
     * @param data       Base64 编码的密文
     * @param privateKey Base64 编码的私钥
     * @return 解密后的字符串
     * @throws Exception 解密失败
     */
    public static String decrypt(String data, String privateKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(privateKey);
        RSAPrivateKey privKey = (RSAPrivateKey) KeyFactory.getInstance(KEY_ALGORITHM)
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
    }
}
