package com.zifang.z.lc.logic.node;

import java.util.Map;

/**
 * 副作用类节点的外部能力接口。
 *
 * <p>逻辑引擎本身不依赖任何具体 RPC/UI/路由实现。宿主按需注册这些 Handler，
 * 节点运行时从 {@code ctx.getServices()} 按约定 key 取用；未注册时走安全的空实现，
 * 从而保证节点库在无宿主环境（单测、服务端）也能跑通不报错。</p>
 */
public final class NodeHandlers {

    private NodeHandlers() {
    }

    public static final String REQUEST = "request";
    public static final String MESSAGE = "message";
    public static final String NAVIGATE = "navigate";
    public static final String SCRIPT = "script";

    /** 接口请求：props 含 url/method/data，返回响应对象 */
    public interface RequestHandler {
        Object request(Map<String, Object> props, com.zifang.z.lc.logic.context.ExecutionContext ctx);
    }

    /** 消息提示：type=info/success/warning/error */
    public interface MessageHandler {
        void show(String type, String content);
    }

    /** 页面跳转 / 弹窗 */
    public interface NavigateHandler {
        void jump(String url);
        void openModal(String modalId, Map<String, Object> props);
        void closeModal(String modalId);
    }

    /** 自定义脚本：可接入 z-script / vapi 沙箱 */
    public interface ScriptHandler {
        Object run(String code, Map<String, Object> vars);
    }
}
