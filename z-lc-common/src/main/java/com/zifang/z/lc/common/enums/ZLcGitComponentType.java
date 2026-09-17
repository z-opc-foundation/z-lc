package com.zifang.z.lc.common.enums;

/**
 * Git 文件构建类型 — 蒸馏自 ace-platform-core
 * {@code Enums.GitComponentType} ({@code com.c2f.ace.core.common}).
 *
 * <p>用于低代码平台"git 仓库拉取 / 构建"过程中区分仓库内对象的类型：
 *
 * <ul>
 *   <li>{@link #FOLDER} — 目录</li>
 *   <li>{@link #FILE} — 文件</li>
 *   <li>{@link #UN_SUPPORT} — 不支持的类型（链接 / 子模块 / 设备文件等）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcGitComponentType {

    /** 目录. */
    FOLDER("folder"),

    /** 文件. */
    FILE("file"),

    /** 不支持的类型. */
    UN_SUPPORT("unSupport");

    private final String componentType;

    ZLcGitComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentType() {
        return componentType;
    }

    public static ZLcGitComponentType fromComponentType(String componentType) {
        if (componentType == null) {
            return null;
        }
        for (ZLcGitComponentType v : values()) {
            if (v.componentType.equals(componentType)) {
                return v;
            }
        }
        return null;
    }
}