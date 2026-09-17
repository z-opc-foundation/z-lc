package com.zifang.z.lc.common.validation;

/**
 * 创建操作校验分组 — 蒸馏自 ace-platform-core
 * {@code Create} ({@code com.c2f.ace.core.common.valid}).
 *
 * <p>用于 JSR-303 / javax.validation 分组校验:
 * 创建接口使用 {@code @Validated(ZLcCreateGroup.class)} 时,
 * 仅触发标注了 {@code @Valid(ZLcCreateGroup.class)} 的校验规则.
 *
 * <p>典型场景：
 * <ul>
 *   <li>创建模型时校验必填字段, 编辑时允许为空</li>
 *   <li>创建应用时校验编码唯一性, 编辑时排除自身</li>
 * </ul>
 *
 * @author zifang
 */
public interface ZLcCreateGroup {
}
