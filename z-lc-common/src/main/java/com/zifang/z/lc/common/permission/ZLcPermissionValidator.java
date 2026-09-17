package com.zifang.z.lc.common.permission;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解 — 蒸馏自 ace-platform-core
 * {@code PermissionValidator} ({@code com.c2f.ace.core.common.permission}).
 *
 * <p>标记在 Controller 方法上, 由权限校验切面拦截,
 * 在方法被调用前验证当前登录用户是否具备 {@link #permissions()} 中的权限.
 *
 * <p>典型用法:
 * <pre>{@code
 * @ZLcPermissionValidator(permissions = {"ace:app:leave:create"})
 * public Result<LeaveDTO> createLeave(@RequestBody LeaveDTO dto) {
 *     ...
 * }
 * }</pre>
 *
 * @author zifang
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ZLcPermissionValidator {

    /**
     * 校验类型 — 默认 {@link ZLcPermissionValidatorType#ALL} (全部命中).
     */
    String validateType() default ZLcPermissionValidatorType.ALL;

    /**
     * 权限编码列表 — 业务方根据当前用户权限集合判断是否通过.
     */
    String[] permissions();
}