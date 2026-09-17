package com.zifang.z.lc.common.enums;

/**
 * 逻辑文件夹类型 — 蒸馏自 ace-platform-core
 * {@code Enums.LogicFolderType} ({@code com.c2f.ace.core.common}).
 *
 * <p>用于低代码平台中区分逻辑目录的种类：
 *
 * <ul>
 *   <li>{@link #CODE_FOLDER} — 代码目录（存放 Java / 脚本 / 编译产物）</li>
 *   <li>{@link #RESOURCE_FOLDER} — 资源目录（存放 SQL / 配置 / 静态资源）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcLogicFolderType {

    /** 代码目录. */
    CODE_FOLDER("code"),

    /** 资源目录. */
    RESOURCE_FOLDER("resources");

    private final String folderType;

    ZLcLogicFolderType(String folderType) {
        this.folderType = folderType;
    }

    public String getFolderType() {
        return folderType;
    }

    public static ZLcLogicFolderType fromFolderType(String folderType) {
        if (folderType == null) {
            return null;
        }
        for (ZLcLogicFolderType v : values()) {
            if (v.folderType.equals(folderType)) {
                return v;
            }
        }
        return null;
    }
}