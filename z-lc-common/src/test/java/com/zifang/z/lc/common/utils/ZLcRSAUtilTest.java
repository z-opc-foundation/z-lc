package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcRSAUtil 单元测试
 *
 * @author zifang
 */
class ZLcRSAUtilTest {

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcRSAUtil> constructor = ZLcRSAUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldGenerateKeyPair() {
        Map<String, Object> keyMap = ZLcRSAUtil.initKey(2048);
        assertThat(keyMap).isNotNull();
        assertThat(keyMap).containsKey("RSAPublicKey");
        assertThat(keyMap).containsKey("RSAPrivateKey");
    }

    @Test
    void shouldGetPublicKeyStr() {
        Map<String, Object> keyMap = ZLcRSAUtil.initKey(2048);
        String publicKey = ZLcRSAUtil.getPublicKeyStr(keyMap);
        assertThat(publicKey).isNotNull();
        assertThat(publicKey).isNotEmpty();
    }

    @Test
    void shouldGetPrivateKeyStr() {
        Map<String, Object> keyMap = ZLcRSAUtil.initKey(2048);
        String privateKey = ZLcRSAUtil.getPrivateKeyStr(keyMap);
        assertThat(privateKey).isNotNull();
        assertThat(privateKey).isNotEmpty();
    }

    @Test
    void shouldEncryptAndDecrypt() throws Exception {
        Map<String, Object> keyMap = ZLcRSAUtil.initKey(2048);
        String publicKey = ZLcRSAUtil.getPublicKeyStr(keyMap);
        String privateKey = ZLcRSAUtil.getPrivateKeyStr(keyMap);

        String original = "Hello, World!";
        String encrypted = ZLcRSAUtil.encrypt(original, publicKey);
        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(original);

        String decrypted = ZLcRSAUtil.decrypt(encrypted, privateKey);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void shouldHandleChineseCharacters() throws Exception {
        Map<String, Object> keyMap = ZLcRSAUtil.initKey(2048);
        String publicKey = ZLcRSAUtil.getPublicKeyStr(keyMap);
        String privateKey = ZLcRSAUtil.getPrivateKeyStr(keyMap);

        String original = "你好，世界！";
        String encrypted = ZLcRSAUtil.encrypt(original, publicKey);
        String decrypted = ZLcRSAUtil.decrypt(encrypted, privateKey);
        assertThat(decrypted).isEqualTo(original);
    }
}