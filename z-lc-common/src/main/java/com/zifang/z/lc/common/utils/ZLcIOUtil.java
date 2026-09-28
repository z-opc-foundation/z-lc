package com.zifang.z.lc.common.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * IO 工具 — 蒸馏自 ace-platform-core
 * {@code IOUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供文件读写、资源加载、目录删除等常用 IO 操作.
 * 蒸馏时移除了 ace 对 hutool / commons-compress 的依赖, 改为纯 JDK 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>BPMN 流程定义文件读取</li>
 *   <li>Excel 导入导出时的文件操作</li>
 *   <li>临时文件生成与清理</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcIOUtil {

    private ZLcIOUtil() {
    }

    /**
     * 从 classpath 读取资源文件内容.
     *
     * @param name 资源文件路径 (如 "processes/leaveProcess.bpmn")
     * @return 文件内容; 读取失败时返回 null
     */
    public static String fetchFromClasspath(String name) {
        try (InputStream is = ClassLoader.getSystemResourceAsStream(name)) {
            if (is == null) {
                return null;
            }
            return readStream(is);
        } catch (IOException e) {
            throw new RuntimeException("读取资源文件失败: " + name, e);
        }
    }

    /**
     * 从 InputStream 读取全部内容为字符串.
     *
     * @param inputStream 输入流
     * @return 字符串内容; 读取失败时返回 null
     */
    public static String readStream(InputStream inputStream) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                output.write(buf, 0, len);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 读取文件内容为字符串.
     *
     * @param fileName 文件路径
     * @return 文件内容; 读取失败时返回空串
     */
    public static String readFile(String fileName) {
        try {
            Path path = Paths.get(fileName);
            if (Files.exists(path)) {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // fall through
        }
        return "";
    }

    /**
     * 从 classpath 读取文件内容 (使用 BufferedReader).
     *
     * @param fileName classpath 下的文件名
     * @return 文件内容
     */
    public static String readResource(String fileName) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            return buffer.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 生成文件.
     *
     * @param filePath    文件绝对路径
     * @param fileContent 文件内容
     */
    public static void generateFile(String filePath, String fileContent) {
        try {
            Path path = Paths.get(filePath);
            Files.write(path, fileContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("生成文件失败: " + filePath, e);
        }
    }

    /**
     * 递归删除目录.
     *
     * @param dir 目录
     * @return 是否删除成功
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }
}
