package com.zifang.z.lc.common.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 特殊模型字段定义 — 蒸馏自 ace-platform-core
 * {@code Enums.SpecialModelFieldDefinition} ({@code com.c2f.ace.core.common}).
 *
 * <p>用于低代码平台"模型"管理 — 标识系统级特殊字段 (id / create_time / update_time /
 * create_by / update_by / is_deleted / tenant_id / app_code)，业务方不可修改其字段名 /
 * 物理列名 / 默认 JSON 路径.
 *
 * <p>字段语义：
 * <ul>
 *   <li>{@code fieldCode} — 业务字段编码（业务对象上访问用）</li>
 *   <li>{@code columnName} — 数据库物理列名</li>
 *   <li>{@code position} — JSON 序列化时该字段在对象中的默认路径（{@code $.xxx}）</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcSpecialModelFieldDefinition implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务字段编码. */
    private final String fieldCode;

    /** 数据库物理列名. */
    private final String columnName;

    /** JSON 默认路径. */
    private final String position;

    public ZLcSpecialModelFieldDefinition(String fieldCode, String columnName, String position) {
        this.fieldCode = fieldCode;
        this.columnName = columnName;
        this.position = position;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getPosition() {
        return position;
    }

    // ============ 系统级特殊字段定义（业务方只读）============

    /** 主键 ID. */
    public static final ZLcSpecialModelFieldDefinition ID =
            new ZLcSpecialModelFieldDefinition("id", "id", "$.id");

    /** 创建时间. */
    public static final ZLcSpecialModelFieldDefinition CREATE_TIME =
            new ZLcSpecialModelFieldDefinition("createTime", "createTime", "$.createTime");

    /** 更新时间. */
    public static final ZLcSpecialModelFieldDefinition UPDATE_TIME =
            new ZLcSpecialModelFieldDefinition("updateTime", "update_time", "$.updateTime");

    /** 创建人. */
    public static final ZLcSpecialModelFieldDefinition CREATE_BY =
            new ZLcSpecialModelFieldDefinition("createBy", "CREATE_BY", "$.createBy");

    /** 更新人. */
    public static final ZLcSpecialModelFieldDefinition UPDATE_BY =
            new ZLcSpecialModelFieldDefinition("updateBy", "update_by", "$.updateBy");

    /** 逻辑删除标记. */
    public static final ZLcSpecialModelFieldDefinition IS_DELETED =
            new ZLcSpecialModelFieldDefinition("isDeleted", "is_deleted", "$.isDeleted");

    /** 租户 ID. */
    public static final ZLcSpecialModelFieldDefinition TENANT_ID =
            new ZLcSpecialModelFieldDefinition("tenantId", "tenant_id", "$.tenantId");

    /** 应用编码. */
    public static final ZLcSpecialModelFieldDefinition APP_CODE =
            new ZLcSpecialModelFieldDefinition("appCode", "app_code", "$.appCode");

    /** 全部特殊字段定义列表 — 用于"模型管理"页面渲染. */
    public static final List<ZLcSpecialModelFieldDefinition> SPECIAL_MODEL_FIELD_DEFINITIONS =
            new ArrayList<>(Arrays.asList(
                    ID, CREATE_TIME, UPDATE_TIME, CREATE_BY, UPDATE_BY,
                    IS_DELETED, TENANT_ID, APP_CODE
            ));

    /**
     * 根据 {@code fieldCode} 反查定义.
     */
    public static ZLcSpecialModelFieldDefinition getByFieldCode(String fieldCode) {
        if (fieldCode == null) {
            return null;
        }
        for (ZLcSpecialModelFieldDefinition def : SPECIAL_MODEL_FIELD_DEFINITIONS) {
            if (def.fieldCode.equals(fieldCode)) {
                return def;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ZLcSpecialModelFieldDefinition{" +
                "fieldCode='" + fieldCode + '\'' +
                ", columnName='" + columnName + '\'' +
                ", position='" + position + '\'' +
                '}';
    }
}