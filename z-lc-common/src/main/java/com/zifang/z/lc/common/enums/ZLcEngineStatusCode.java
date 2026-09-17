package com.zifang.z.lc.common.enums;

import com.zifang.util.core.meta.StatusCode;

/**
 * z-lc 引擎状态码.
 *
 * <p>蒸馏自 ace-platform-engine {@code AceEngineStatusCode}
 * （{@code com.c2f.ace.engine}），字段语义完全对齐.
 *
 * <p>用于 {@code z-lc-core} 各 Service / Controller 抛出业务异常时携带状态码 —
 * 客户端可按 code 做国际化文案提示 / 错误分流.
 *
 * <p>当前仅保留最常用的 3 个状态码；业务方继承此枚举或新增 {@code StatusCode} 实现
 * 即可扩展（与 z-util-core 的 Result 体系兼容）.
 *
 * @author zifang
 */
public enum ZLcEngineStatusCode implements StatusCode {

    /**
     * 业务异常（通用 — Service 层抛出的所有可恢复异常用此 code）.
     */
    BIZ_EXCEPTION(1001, "业务异常"),

    /**
     * 系统繁忙 / 限流触发.
     */
    TOO_BUSY_EXCEPTION(2001, "系统繁忙，请稍后再试"),

    /**
     * 校验不通过（参数校验 / 业务规则校验）.
     */
    VALIDATE_EXCEPTION(3001, "校验不通过"),

    /**
     * 实体未找到（appCode/modelCode/entityCode 查不到对应定义）.
     */
    ENTITY_NOT_FOUND(1002, "实体未找到"),

    /**
     * 数据源连接失败（保存数据源时校验失败 / 扫描时断连）.
     */
    DATASOURCE_CONNECT_FAILED(4001, "数据源连接失败"),

    /**
     * 字段类型不匹配（如 INT 字段传入非数字字符串）.
     */
    FIELD_TYPE_MISMATCH(5001, "字段类型不匹配"),

    /**
     * 模型定义冲突（同名 entityCode 重复定义 / 字段重复）.
     */
    MODEL_DEFINITION_CONFLICT(6001, "模型定义冲突"),

    /**
     * 流程定义错误（Flowable BPMN 解析失败 / 节点配置错误）.
     */
    WORKFLOW_DEFINITION_ERROR(7001, "流程定义错误"),

    /**
     * 权限校验失败（无访问权限 / 数据范围越权）.
     */
    PERMISSION_DENIED(8001, "权限校验失败");

    private final int code;
    private final String message;

    ZLcEngineStatusCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 按 code 查找（未匹配返回 null）.
     */
    public static ZLcEngineStatusCode fromCode(int code) {
        for (ZLcEngineStatusCode c : values()) {
            if (c.code == code) {
                return c;
            }
        }
        return null;
    }
}
