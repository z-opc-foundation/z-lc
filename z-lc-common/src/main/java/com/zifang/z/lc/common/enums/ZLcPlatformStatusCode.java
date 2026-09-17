package com.zifang.z.lc.common.enums;

import com.zifang.util.core.meta.StatusCode;

/**
 * z-lc 平台状态码 — 蒸馏自 ace-platform-core
 * {@code AceStatusCode} （{@code com.c2f.ace.core.common}}，字段语义完全对齐.
 *
 * <p>实现 {@link StatusCode} 接口，可被 {@code BusinessException(StatusCode, message)} 直接使用.
 *
 * <p>与 {@code ZLcEngineStatusCode} 的关系：
 * <ul>
 *   <li>{@code ZLcEngineStatusCode} — 引擎内部使用（低代码引擎特定的错误码）</li>
 *   <li>{@code ZLcPlatformStatusCode} — 平台层通用错误码（业务方/第三方集成时使用）</li>
 * </ul>
 *
 * @author zifang
 */
public enum ZLcPlatformStatusCode implements StatusCode {

    /** 业务异常（通用 — Service 层抛出的所有可恢复异常用此 code） */
    BIZ_EXCEPTION(1001, "业务异常"),

    /** 参数异常（入参校验失败） */
    PARAM_EXCEPTION(1002, "参数异常"),

    /** 系统繁忙 / 限流触发. */
    TOO_BUSY_EXCEPTION(2001, "系统繁忙，请稍后再试"),

    /** 校验不通过（参数校验 / 业务规则校验） */
    VALIDATE_EXCEPTION(3001, "校验不通过"),

    /** 实体未找到（appCode/modelCode/entityCode 查不到对应定义） */
    ENTITY_NOT_FOUND(1003, "实体未找到"),

    /** 数据源连接失败. */
    DATASOURCE_CONNECT_FAILED(4001, "数据源连接失败"),

    /** 字段类型不匹配. */
    FIELD_TYPE_MISMATCH(5001, "字段类型不匹配"),

    /** 模型定义冲突（同名 entityCode 重复定义 / 字段重复） */
    MODEL_DEFINITION_CONFLICT(6001, "模型定义冲突"),

    /** 权限校验失败. */
    PERMISSION_DENIED(8001, "权限校验失败"),

    /** 未授权访问. */
    UNAUTHORIZED(8002, "未授权访问"),

    /** 资源不存在. */
    NOT_FOUND(8003, "资源不存在");

    private final int code;
    private final String message;

    ZLcPlatformStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public static ZLcPlatformStatusCode fromCode(int code) {
        for (ZLcPlatformStatusCode s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }
}
