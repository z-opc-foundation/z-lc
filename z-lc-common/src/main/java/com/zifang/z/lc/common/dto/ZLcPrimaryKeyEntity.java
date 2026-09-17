package com.zifang.z.lc.common.dto;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 主键实体基类 — 蒸馏自 ace-platform-core
 * {@code PrimaryKeyEntity} ({@code com.c2f.ace.core.common}).
 *
 * <p>仅含 {@code id} 字段 + {@code @NotNull} 校验的最小主键 DTO，用于：
 * <ul>
 *   <li>RPC 接口的"按 ID 操作"参数（getById / deleteById / updateById 等）</li>
 *   <li>MyBatis / Spring Data JPA 的 {@code PrimaryKey} 类型</li>
 *   <li>通用 CRUD API 的入参基础类型</li>
 * </ul>
 *
 * <p>与 {@link BaseDTO} 的区别：
 * <ul>
 *   <li>{@link BaseDTO} 是分页查询入参（含 pageSize/pageNum/sort 等）</li>
 *   <li>{@code ZLcPrimaryKeyEntity} 是单 ID 操作入参（含 {@code id}）</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcPrimaryKeyEntity implements Serializable {

    private static final long serialVersionUID = 6166625648639800775L;

    /** 主键 ID — 不可为空. */
    @NotNull(message = "id 不能为空")
    private Long id;

    public ZLcPrimaryKeyEntity() {
    }

    public ZLcPrimaryKeyEntity(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ZLcPrimaryKeyEntity{id=" + id + "}";
    }
}