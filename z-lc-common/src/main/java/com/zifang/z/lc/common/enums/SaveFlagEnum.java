package com.zifang.z.lc.common.enums;

/**
 * 保存结果标记枚举.
 *
 * <p>蒸馏自 ace-platform-engine {@code SaveFlagEnum}
 * （{@code com.c2f.ace.engine.common}），字段语义完全对齐.
 *
 * <p>用途：表单保存 / 提交接口返回时携带标记 — 客户端可按 code 做后续路由：
 * <ul>
 *   <li>{@link #SUCCESS} — 提交成功，跳转到详情页</li>
 *   <li>{@link #FAIL} — 提交失败，停留在表单页</li>
 * </ul>
 *
 * @author zifang
 */
public enum SaveFlagEnum {

    /**
     * 成功.
     */
    SUCCESS("success", "成功"),

    /**
     * 失败.
     */
    FAIL("fail", "失败");

    private final String code;
    private final String desc;

    SaveFlagEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 按 code 字符串查找（未匹配返回 null）.
     */
    public static SaveFlagEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (SaveFlagEnum s : values()) {
            if (s.code.equalsIgnoreCase(code)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 是否成功.
     */
    public static boolean isSuccess(String code) {
        return SUCCESS.code.equalsIgnoreCase(code);
    }

    /**
     * 是否失败.
     */
    public static boolean isFail(String code) {
        return FAIL.code.equalsIgnoreCase(code);
    }
}
