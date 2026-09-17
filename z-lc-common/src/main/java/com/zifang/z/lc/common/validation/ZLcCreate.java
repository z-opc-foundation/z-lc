package com.zifang.z.lc.common.validation;

import javax.validation.groups.Default;

/**
 * Bean Validation 分组 — Create 场景（新增操作）.
 *
 * <p>蒸馏自 ace-platform-core
 * {@code Create} （{@code com.c2f.ace.core.common.valid}}，行为完全对齐.
 *
 * <p>用于 JSR-303 校验分组 — 同一 DTO 在 Create / Update 场景下校验规则不同时，
 * 给字段标注 {@code @NotNull(groups = {ZLcCreate.class})} 等，
 * 然后在 Controller 上用 {@code @Validated(Create.class)} 触发该分组校验.
 *
 * <p>典型用法：
 * <pre>{@code
 *   public class UserDTO {
 *       @NotBlank(groups = {ZLcCreate.class})
 *       private String name;
 *
 *       @Null(groups = {ZLcCreate.class})
 *       @NotNull(groups = {ZLcUpdate.class})
 *       private Long id;
 *   }
 *
 *   @PostMapping
 *   public Result createUser(@RequestBody @Validated({ZLcCreate.class, Default.class}) UserDTO dto) { ... }
 *
 *   @PutMapping
 *   public Result updateUser(@RequestBody @Validated({ZLcUpdate.class, Default.class}) UserDTO dto) { ... }
 * }</pre>
 *
 * <p>与 ace 原版的差异：
 * <ul>
 *   <li>ace {@code Create} 是普通空 interface</li>
 *   <li>z-lc {@code ZLcCreate} 继承 {@link Default} — 让分组校验也能触发
 *       未指定 group 的字段（@NotNull 默认属于 Default 分组）</li>
 * </ul>
 *
 * @author zifang
 */
public interface ZLcCreate extends Default {
}
