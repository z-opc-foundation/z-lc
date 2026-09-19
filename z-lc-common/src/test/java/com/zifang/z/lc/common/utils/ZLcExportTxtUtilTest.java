package com.zifang.z.lc.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcExportTxtUtil 单元测试
 *
 * @author zifang
 */
class ZLcExportTxtUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<ZLcExportTxtUtil> constructor = ZLcExportTxtUtil.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }

    @Test
    void shouldExportTxtToFile() throws Exception {
        String content = "Hello, World!";
        Path filePath = tempDir.resolve("test.txt");

        ZLcExportTxtUtil.exportTxtLocal(content, filePath.toString());

        assertThat(Files.exists(filePath)).isTrue();
        assertThat(Files.readString(filePath, StandardCharsets.UTF_8)).isEqualTo(content);
    }

    @Test
    void shouldBuildFilePath() {
        String path = ZLcExportTxtUtil.buildFilePath("test-dir", "test-file");
        assertThat(path).endsWith("test-dir" + File.separator + "test-file.txt");
    }

    @Test
    void shouldCreateDirectory() {
        Path testDir = tempDir.resolve("nested-dir/sub-dir");
        ZLcExportTxtUtil.createDirectory(testDir.toString());
        assertThat(Files.exists(testDir)).isTrue();
    }

    @Test
    void shouldGetLocalDirectoryPath() {
        String path = ZLcExportTxtUtil.getLocalDirectoryPath("test-dir");
        String userHome = System.getProperty("user.home");
        assertThat(path).isEqualTo(userHome + File.separator + "test-dir");
    }

    @Test
    void shouldGetFileName() {
        assertThat(ZLcExportTxtUtil.getFileName("test")).isEqualTo("test.txt");
    }
}