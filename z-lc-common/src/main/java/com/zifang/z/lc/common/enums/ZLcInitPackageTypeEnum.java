package com.zifang.z.lc.common.enums;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 初始化包类型枚举 — 蒸馏自 ace-platform-core
 * {@code InitPackageTypeEnum} ({@code com.c2f.ace.core.common}}.
 *
 * <p>用于低代码平台「初始化包」导出/导入 — 标识被打包对象的类型及在初始化包中的
 * 索引位置，便于导入端按顺序还原业务对象（模型/字典/服务/流程/页面/系统配置等）。
 *
 * <p>字段语义完全对齐：
 * <ul>
 *   <li>{@code code} — 类型编码（与业务对象 type 字段对应）</li>
 *   <li>{@code name} — 类型名称（中文）</li>
 *   <li>{@code groupCode} — 分组编码（用于把同组类型聚合展示/导入）</li>
 *   <li>{@code index} — 组内顺序索引（同一 {@code groupCode} 内按 index 升序处理）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcInitPackageTypeEnum {

    /** 模型树. */
    MODEL_TREE("modelTree", "模型树", "model", 1),

    /** 模型. */
    MODEL("model", "模型", "model", 2),

    /** 模型字段. */
    MODEL_FIELD("modelField", "模型字段", "model", 3),

    /** 字典树. */
    DICT_TREE("dictTree", "字典树", "dict", 1),

    /** 字典. */
    DICT("dict", "字典", "dict", 2),

    /** 字典值. */
    DICT_VALUE("dictValue", "字典值", "dict", 3),

    /** 服务树. */
    SERVICE_TREE("serviceTree", "服务树", "service", 1),

    /** 服务. */
    SERVICE("service", "服务", "service", 2),

    /** 流程树. */
    WORKFLOW_TREE("workflowTree", "流程树", "workflow", 1),

    /** 流程. */
    WORKFLOW("workflow", "流程", "workflow", 2),

    /** 页面树. */
    PAGE_TREE("pageTree", "页面树", "page", 1),

    /** 页面. */
    PAGE("page", "页面", "page", 2),

    /** 页面分组. */
    PAGE_GROUP("pageGroup", "页面分组", "page", 3),

    /** 系统配置. */
    SYSTEM_CONFIG("systemConfig", "系统配置", "config", 1),

    /** 数据源配置. */
    DATA_SOURCE_CONFIG("dataSourceConfig", "数据源配置", "config", 2);

    private final String code;
    private final String name;
    private final String groupCode;
    private final Integer index;

    ZLcInitPackageTypeEnum(String code, String name, String groupCode, Integer index) {
        this.code = code;
        this.name = name;
        this.groupCode = groupCode;
        this.index = index;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public Integer getIndex() {
        return index;
    }

    /**
     * 根据 code 反查枚举项 — code 匹配失败时返回 {@code null}.
     */
    public static ZLcInitPackageTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ZLcInitPackageTypeEnum v : values()) {
            if (v.code.equals(code)) {
                return v;
            }
        }
        return null;
    }

    /**
     * 根据 groupCode 获取该分组下所有枚举项（按 index 升序）.
     *
     * @return 若 groupCode 未匹配则返回空列表
     */
    public static List<ZLcInitPackageTypeEnum> getByGroupCode(String groupCode) {
        List<ZLcInitPackageTypeEnum> result = new ArrayList<>();
        if (groupCode == null) {
            return result;
        }
        for (ZLcInitPackageTypeEnum v : values()) {
            if (groupCode.equals(v.groupCode)) {
                result.add(v);
            }
        }
        result.sort(Comparator.comparingInt(ZLcInitPackageTypeEnum::getIndex));
        return result;
    }

    /**
     * 获取所有分组编码（按各组首次出现的顺序）.
     */
    public static List<String> getSortedGroupCodeList() {
        Map<String, String> seen = new LinkedHashMap<>();
        for (ZLcInitPackageTypeEnum v : values()) {
            seen.putIfAbsent(v.groupCode, v.groupCode);
        }
        return new ArrayList<>(seen.keySet());
    }
}