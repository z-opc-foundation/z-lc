package com.zifang.z.lc.common.dto;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 主键基类 — 蒸馏自 ace-platform-core
 * {@code PrimaryKeyEntity} ({@code com.c2f.ace.core.common}).
 *
 * <p>提供带主键 id 的基础实体, 所有需要主键的业务实体可继承此类.
 * 蒸馏时移除了 ace 对 Lombok @Data / Swagger @ApiModelProperty 的依赖,
 * 改为手写 getter/setter + Bean Validation.
 *
 * @author zifang
 */
public class ZLcPrimaryKeyEntity implements Serializable {

    private static final long serialVersionUID = 6166625648639800775L;

    @NotNull(message = "id 不能为空")
    private Long id;

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
