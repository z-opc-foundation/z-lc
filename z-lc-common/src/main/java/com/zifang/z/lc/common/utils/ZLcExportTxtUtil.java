package com.zifang.z.lc.common.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文本导出工具 — 蒸馏自 ace-platform-core
 * {@code ExportTxtUtil} ({@code com.c2f.ace.core.utils}).
 *
 * <p>提供文本文件导出、目录创建、文件名生成等操作.
 * 蒸馏时移除了 ace 对 hutool / MyBatis Plus 的依赖, 改为纯 JDK 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>流程审批记录导出为 TXT 文件</li>
 *   <li>数据校验结果导出</li>
 *   <li>SQL 脚本生成导出</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcExportTxtUtil {

    private ZLcExportTxtUtil() {
    }

    /**
     * 导出字符串内容到本地 TXT 文件.
     *
     * @param content  文件内容
     * @param filePath 文件绝对路径
     * @throws RuntimeException 导出失败时抛出
     */
    public static void exportTxtLocal(String content, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            throw new RuntimeException("文件导出失败: " + filePath, e);
        }
    }

    /**
     * 构建导出文件路径.
     *
     * @param directory 子目录名 (如 "z-lc-export")
     * @param baseName  文件基础名 (不含扩展名)
     * @return 完整文件路径
     */
    public static String buildFilePath(String directory, String baseName) {
        String dirPath = getLocalDirectoryPath(directory);
        createDirectory(dirPath);
        return dirPath + File.separator + baseName + ".txt";
    }

    /**
     * 创建目录 (含父目录).
     *
     * @param directoryPath 目录路径
     */
    public static void createDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        dir.mkdirs();
    }

    /**
     * 获取用户主目录下的子目录路径.
     *
     * @param directory 子目录名
     * @return 完整路径
     */
    public static String getLocalDirectoryPath(String directory) {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + directory;
    }

    /**
     * 生成 TXT 文件名.
     *
     * @param baseName 基础名
     * @return 完整文件名 (含 .txt 扩展名)
     */
    public static String getFileName(String baseName) {
        return baseName + ".txt";
    }
}
