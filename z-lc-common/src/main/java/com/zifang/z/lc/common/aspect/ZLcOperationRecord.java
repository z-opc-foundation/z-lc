package com.zifang.z.lc.common.aspect;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 系统操作日志记录注解 — 蒸馏自 ace-platform-core
 * {@code OperationRecord} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>标记在 Controller/Service 方法或参数上, 由
 * {@code ZLcOperationRecordAspect} 切面拦截, 在方法正常返回和异常抛出时
 * 通过 MQ 异步上报"操作人/URL/请求参数/响应/动作类型"到审计中心.
 *
 * <p>典型用法:
 * <pre>{@code
 * @ZLcOperationRecord(action = "CREATE")
 * public Result<UserDTO> createUser(@RequestBody CreateUserDTO dto) {
 *     ...
 * }
 * }</pre>
 *
 * <p>蒸馏差异：ace 原版要求 {@code action} 为 {@code ActionEnum} 枚举值,
 * 蒸馏版简化为 {@code String} 以减少 z-lc-common 的常量依赖,
 * 但默认推荐值对齐 z-lc 已有的 {@code ZLcActionTypeEnum} (CREATE/UPDATE/DELETE).
 *
 * @author zifang
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZLcOperationRecord {

    /**
     * 操作动作类型 — 与 ace ActionEnum 对齐.
     *
     * <p>推荐值: {@code CREATE} / {@code UPDATE} / {@code DELETE} (对齐 ZLcActionTypeEnum).
     */
    String action();
}