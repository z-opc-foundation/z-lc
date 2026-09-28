package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcIOUtil 单元测试
 *
 * @author zifang
 */
class ZLcIOUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        java.lang.reflect.Constructor<ZLcIOUtil> constructor = ZLcIOUtil.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldReadStream() throws Exception {
        String content = "Hello, World!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = ZLcIOUtil.readStream(inputStream);

        assertThat(result).isEqualTo(content);
    }

    @Test
    void shouldThrowExceptionForNullInputStream() {
        // readStream方法没有处理null inputStream，会抛出NullPointerException
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ZLcIOUtil.readStream(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldReadFile() throws Exception {
        Path filePath = tempDir.resolve("test.txt");
        Files.write(filePath, "test content".getBytes(StandardCharsets.UTF_8));

        String result = ZLcIOUtil.readFile(filePath.toString());

        assertThat(result).isEqualTo("test content");
    }

    @Test
    void shouldReturnEmptyStringForNonExistentFile() {
        String result = ZLcIOUtil.readFile("/non/existent/file.txt");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldGenerateFile() throws Exception {
        Path filePath = tempDir.resolve("generated.txt");
        String content = "generated content";

        ZLcIOUtil.generateFile(filePath.toString(), content);

        assertThat(Files.exists(filePath)).isTrue();
        assertThat(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)).isEqualTo(content);
    }

    @Test
    void shouldDeleteDirectory() throws Exception {
        // 创建测试目录结构
        File dir = new File(tempDir.toFile(), "testDir");
        dir.mkdir();
        File subDir = new File(dir, "subDir");
        subDir.mkdir();
        File file = new File(subDir, "test.txt");
        file.createNewFile();

        boolean result = ZLcIOUtil.deleteDir(dir);

        assertThat(result).isTrue();
        assertThat(dir.exists()).isFalse();
    }

    @Test
    void shouldDeleteSingleFile() throws Exception {
        File file = new File(tempDir.toFile(), "single.txt");
        file.createNewFile();

        boolean result = ZLcIOUtil.deleteDir(file);

        assertThat(result).isTrue();
        assertThat(file.exists()).isFalse();
    }
}
