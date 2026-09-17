package com.zifang.z.lc.sdk.context;

/**
 * 引擎调用过滤器 SPI — 蒸馏自 ace-platform-engine {@code BdpInvokerFilter}
 * （{@code com.c2f.ace.engine.filter}）.
 *
 * <p>在跨进程 RPC 调用前后双向拦截 ThreadLocal 上下文（UUID / appCode / modelCode / pageCode /
 * saveFlag / currentTaskDefKey / isMobile / tags / businessContext）— 让服务端能读到
 * 客户端的链路追踪信息.
 *
 * <p>ace 原实现是 {@code @Activate(group = {CONSUMER, PROVIDER})} 的 Dubbo Filter，
 * 依赖 fastjson + dubbo. z-lc 蒸馏为通用接口，业务方按使用的 RPC 框架（z-rpc / Dubbo /
 * gRPC / HTTP）实现：
 * <ul>
 *   <li>{@link #beforeInvoke()} — 客户端：把当前 ThreadLocal 中的上下文打包到 RPC attachment</li>
 *   <li>{@link #afterInvoke()} — 客户端：清理可能产生的临时上下文</li>
 *   <li>{@link #onReceive()} — 服务端：从 RPC attachment 解包到 ThreadLocal</li>
 * </ul>
 *
 * <p>典型用法（z-rpc 场景）：
 * <pre>{@code
 *   public class ZRpcBdpInvokerFilter implements ZLcBdpInvokerFilter {
 *       public void beforeInvoke() {
 *           String uuid = ZLcEngineInvokerContext.getUUID();
 *           if (uuid != null) ZRpcContext.setAttachment(KEY_UUID, uuid);
 *           // ... 同样处理 appCode/modelCode/...
 *       }
 *       public void afterInvoke() {
 *           ZLcEngineInvokerContext.clean();
 *       }
 *       public void onReceive() {
 *           String uuid = ZRpcContext.getAttachment(KEY_UUID);
 *           if (uuid != null) ZLcEngineInvokerContext.setUUID(uuid);
 *       }
 *   }
 * }</pre>
 *
 * @author zifang
 */
public interface ZLcBdpInvokerFilter {

    /** 客户端 — 调用 RPC 前 — 把 ThreadLocal 上下文写入 RPC attachment. */
    void beforeInvoke();

    /** 客户端 — 调用 RPC 后 — 清理 ThreadLocal（如果调用产生了新的上下文）. */
    void afterInvoke();

    /** 服务端 — 收到 RPC 调用 — 从 attachment 解包到 ThreadLocal. */
    void onReceive();

    // ====== RPC attachment key 常量 — 与 ace BdpInvokerFilter 字段名对齐 ======

    /** 请求级 UUID attachment key. */
    String KEY_UUID = "uuid";

    /** 操作模式 attachment key. */
    String KEY_MODE = "mode";

    /** 业务上下文 attachment key. */
    String KEY_BUSINESS_CONTEXT = "business_context";

    /** appCode attachment key. */
    String KEY_APP_CODE = "appCode";

    /** modelCode attachment key. */
    String KEY_MODEL_CODE = "modelCode";

    /** pageCode attachment key. */
    String KEY_PAGE_CODE = "pageCode";

    /** saveFlag attachment key. */
    String KEY_SAVE_FLAG = "SAVE_FLAG";

    /** currentTaskDefKey attachment key. */
    String KEY_CURRENT_TASK_DEF_KEY = "currentTaskDefKey";

    /** isMobile attachment key. */
    String KEY_IS_MOBILE = "isMobile";

    /** tags attachment key. */
    String KEY_TAGS = "tags";
}
