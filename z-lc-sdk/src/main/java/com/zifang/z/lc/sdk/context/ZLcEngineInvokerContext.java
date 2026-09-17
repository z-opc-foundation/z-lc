package com.zifang.z.lc.sdk.context;

import java.util.List;
import java.util.Map;

/**
 * 引擎调用上下文 ThreadLocal — 蒸馏自 ace-platform-engine {@code EngineInvokerContext}
 * （{@code com.c2f.ace.engine.filter}），去除 fastjson + dubbo + slf4j 依赖，
 * 改为 z-lc 轻量 ThreadLocal 实现.
 *
 * <p>设计哲学：
 * <ul>
 *   <li>Web 请求开始时（Filter / Interceptor）把上下文信息 set 到 ThreadLocal</li>
 *   <li>引擎回调 SPI / DataModelService / ServiceAdapter 时 get — 无需上游调用链感知</li>
 *   <li>请求结束（finally）清理 ThreadLocal，避免线程复用导致污染</li>
 * </ul>
 *
 * <p>与 z-lc 已有的 {@link ExtensionServiceContextHolder} 的区别：
 * <ul>
 *   <li>{@code ExtensionServiceContextHolder} — 精简 SPI 上下文（appCode + modelCode + customTags）</li>
 *   <li>{@code ZLcEngineInvokerContext} — 完整运行时上下文（含 UUID + mode + saveFlag + currentTaskDefKey + isMobile + tags + businessContext）</li>
 * </ul>
 *
 * <p>字段说明：
 * <ul>
 *   <li>{@link #UUID_CODE} — 请求级 UUID（链路追踪 / 日志关联）</li>
 *   <li>{@link #MODE_CODE} — 操作模式（{@link com.zifang.z.lc.common.enums.FormOperationMode}）</li>
 *   <li>{@link #BUSINESS_CONTEXT} — 业务上下文 Map（业务方自定义）</li>
 *   <li>{@link #APP_CODE} / {@link #MODEL_CODE} / {@link #PAGE_CODE} — 应用 / 模型 / 页面 code</li>
 *   <li>{@link #SAVE_FLAG} — 保存结果标记（{@link com.zifang.z.lc.common.enums.SaveFlagEnum}）</li>
 *   <li>{@link #CURRENT_TASK_DEF_KEY} — Flowable 当前任务定义 key</li>
 *   <li>{@link #IS_MOBILE} — 是否移动端</li>
 *   <li>{@link #TAGS} — 自定义标签（用于灰度 / AB Test）</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcEngineInvokerContext {

    /** 请求级 UUID 字段名 — 用于日志关联 / 链路追踪. */
    public static final String UUID_CODE = "uuid";

    /** 操作模式字段名. */
    public static final String MODE_CODE = "mode";

    /** 业务上下文字段名. */
    public static final String BUSINESS_CONTEXT = "business_context";

    /** 应用 code 字段名. */
    public static final String APP_CODE = "appCode";

    /** 模型 code 字段名. */
    public static final String MODEL_CODE = "modelCode";

    /** 页面 code 字段名. */
    public static final String PAGE_CODE = "pageCode";

    /** 保存标记字段名. */
    public static final String SAVE_FLAG = "SAVE_FLAG";

    /** 当前任务定义 key 字段名. */
    public static final String CURRENT_TASK_DEF_KEY = "currentTaskDefKey";

    /** 是否移动端字段名. */
    public static final String IS_MOBILE = "isMobile";

    /** 标签字段名. */
    private static final String TAGS = "tags";

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    private ZLcEngineInvokerContext() {
        // 工具类，禁止实例化
    }

    /**
     * 设置整个 Map — 用于 Filter / Interceptor 一次性注入.
     */
    public static void setContextMap(Map<String, Object> context) {
        if (context == null) {
            return;
        }
        THREAD_LOCAL.set(context);
    }

    /**
     * 取出整个 Map — 返回内部引用（修改会影响 ThreadLocal，调用方勿改）.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getContextMap() {
        Map<String, Object> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new java.util.HashMap<>(16);
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    public static void setUUID(String uuid) {
        getContextMap().put(UUID_CODE, uuid);
    }

    public static String getUUID() {
        Object v = getContextMap().get(UUID_CODE);
        return v == null ? null : v.toString();
    }

    public static void setMode(Integer mode) {
        getContextMap().put(MODE_CODE, mode);
    }

    public static Integer getMode() {
        Object v = getContextMap().get(MODE_CODE);
        return v == null ? null : (Integer) v;
    }

    public static void setBusinessContext(Map<String, Object> businessContext) {
        getContextMap().put(BUSINESS_CONTEXT, businessContext);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getBusinessContext() {
        return (Map<String, Object>) getContextMap().get(BUSINESS_CONTEXT);
    }

    public static void setAppCode(String appCode) {
        getContextMap().put(APP_CODE, appCode);
    }

    public static String getAppCode() {
        Object v = getContextMap().get(APP_CODE);
        return v == null ? null : v.toString();
    }

    public static void setModelCode(String modelCode) {
        getContextMap().put(MODEL_CODE, modelCode);
    }

    public static String getModelCode() {
        Object v = getContextMap().get(MODEL_CODE);
        return v == null ? null : v.toString();
    }

    public static void setPageCode(String pageCode) {
        getContextMap().put(PAGE_CODE, pageCode);
    }

    public static String getPageCode() {
        Object v = getContextMap().get(PAGE_CODE);
        return v == null ? null : v.toString();
    }

    public static void setSaveFlag(String saveFlag) {
        getContextMap().put(SAVE_FLAG, saveFlag);
    }

    public static String getSaveFlag() {
        Object v = getContextMap().get(SAVE_FLAG);
        return v == null ? null : v.toString();
    }

    public static void setCurrentTaskDefKey(String currentTaskDefKey) {
        getContextMap().put(CURRENT_TASK_DEF_KEY, currentTaskDefKey);
    }

    public static String getCurrentTaskDefKey() {
        Object v = getContextMap().get(CURRENT_TASK_DEF_KEY);
        return v == null ? null : v.toString();
    }

    public static void setIsMobile(Boolean isMobile) {
        getContextMap().put(IS_MOBILE, isMobile);
    }

    public static Boolean getIsMobile() {
        Object v = getContextMap().get(IS_MOBILE);
        return v != null && (Boolean) v;
    }

    public static void setTags(List<String> tags) {
        getContextMap().put(TAGS, tags);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getTags() {
        return (List<String>) getContextMap().get(TAGS);
    }

    /**
     * 清理 ThreadLocal — 必须在请求结束 finally 块调用，避免线程复用污染.
     */
    public static void clean() {
        THREAD_LOCAL.remove();
    }
}
