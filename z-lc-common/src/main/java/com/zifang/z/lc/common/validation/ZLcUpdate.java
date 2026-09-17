package com.zifang.z.lc.common.validation;

import javax.validation.groups.Default;

/**
 * Bean Validation 分组 — Update 场景（更新操作）.
 *
 * <p>蒸馏自 ace-platform-core
 * {@code Update} （{@code com.c2f.ace.core.common.valid}}，行为完全对齐.
 *
 * <p>用法见 {@link ZLcCreate} — 在 Controller 上用 {@code @Validated({ZLcUpdate.class, Default.class})} 触发.
 *
 * @author zifang
 */
public interface ZLcUpdate extends Default {
}
