package com.zifang.z.lc.logic.node;

import com.zifang.z.lc.logic.context.ExecutionContext;
import com.zifang.z.lc.logic.context.ExpressionResolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 内置逻辑节点库：21 类，分 6 组（与参考引擎节点分类对齐，命名中性化）。
 *
 * <pre>
 * 起始结构：start / noop / end / subflow
 * 条件分支：condition / rhombCondition / switchCase
 * 数据状态：setVariables / setGlobal / setState / batchUpdate(memento) / appendList
 * 交互反馈：message / openModal / closeModal / jumpLink
 * 方法调用：interfaceRequest / customScript / setMaterialProperty
 * 扩展调试：debug / timer
 * </pre>
 *
 * <p>纯上下文节点开箱即用；副作用节点（请求/消息/跳转/脚本）从
 * {@code ctx.getServices()} 取宿主注入的 Handler，未注入时走安全空实现。</p>
 */
public final class DefaultNodes {

    private DefaultNodes() {
    }

    /** 简式节点：type + (props, ctx) → 产出 */
    private record SimpleNode(String type, BiFunction<Map<String, Object>, ExecutionContext, Object> fn)
            implements LogicNode {
        @Override
        public Object execute(Map<String, Object> props, ExecutionContext ctx) {
            return fn.apply(props, ctx);
        }
    }

    private static LogicNode node(String type, BiFunction<Map<String, Object>, ExecutionContext, Object> fn) {
        return new SimpleNode(type, fn);
    }

    /** 安装全部内置节点到注册表 */
    public static void install(LogicNodeRegistry registry) {
        // 起始结构
        registry.register(start());
        registry.register(noop());
        registry.register(end());
        registry.register(subflow());
        // 条件分支
        registry.register(condition());
        registry.register(rhombCondition());
        registry.register(switchCase());
        // 数据状态
        registry.register(setVariables());
        registry.register(setGlobal());
        registry.register(setState());
        registry.register(batchUpdate());
        registry.register(appendList());
        // 交互反馈
        registry.register(message());
        registry.register(openModal());
        registry.register(closeModal());
        registry.register(jumpLink());
        // 方法调用
        registry.register(interfaceRequest());
        registry.register(customScript());
        registry.register(setMaterialProperty());
        // 扩展调试
        registry.register(debug());
        registry.register(timer());
    }

    /** 内置节点类型清单（供设计器/文档枚举） */
    public static List<String> types() {
        return List.of(
                "start", "noop", "end", "subflow",
                "condition", "rhombCondition", "switchCase",
                "setVariables", "setGlobal", "setState", "batchUpdate", "appendList",
                "message", "openModal", "closeModal", "jumpLink",
                "interfaceRequest", "customScript", "setMaterialProperty",
                "debug", "timer");
    }

    // ==================== 起始结构 ====================

    public static LogicNode start() {
        return node("start", (p, c) -> null);
    }

    public static LogicNode noop() {
        return node("noop", (p, c) -> null);
    }

    /** 结束节点：发出中断信号，后续节点不再执行 */
    public static LogicNode end() {
        return node("end", (p, c) -> {
            c.stop();
            return null;
        });
    }

    /** 子流程占位节点：实际子流程由编译器内联展开，此处为兜底 */
    public static LogicNode subflow() {
        return node("subflow", (p, c) -> null);
    }

    // ==================== 条件分支 ====================

    /** 通用条件节点：props.expression 求值成布尔 */
    public static LogicNode condition() {
        return node("condition", (p, c) -> evalCondition(p, c));
    }

    /** 菱形条件：与 condition 语义一致，供设计态区分图形 */
    public static LogicNode rhombCondition() {
        return node("rhombCondition", (p, c) -> evalCondition(p, c));
    }

    /** switch：按 expression 求值结果在 cases 里匹配，返回命中分支标识 */
    public static LogicNode switchCase() {
        return node("switchCase", (p, c) -> {
            Object value = resolve(p.get("expression"), c, p);
            Object casesObj = p.get("cases");
            if (!(casesObj instanceof Map<?, ?> cases)) return null;
            for (Map.Entry<?, ?> e : cases.entrySet()) {
                Object expected = resolve(e.getKey(), c, p);
                if (expected != null && expected.equals(value)) {
                    return resolve(e.getValue(), c, p);
                }
            }
            return resolve(p.get("defaultCase"), c, p);
        });
    }

    private static Object evalCondition(Map<String, Object> props, ExecutionContext ctx) {
        Object raw = props.get("expression");
        if (raw == null) raw = props.get("condition");
        if (raw == null) return Boolean.FALSE;
        Object v = resolve(raw, ctx, props);
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        return v != null && !"false".equalsIgnoreCase(v.toString()) && !"".equals(v.toString());
    }

    // ==================== 数据状态 ====================

    /** 设置节点局部变量：返回 name→value 映射，由执行器写入 nodeState[address].res */
    public static LogicNode setVariables() {
        return node("setVariables", (p, c) -> resolveVars(p.get("variables"), c, p));
    }

    /** 设置全局变量 */
    public static LogicNode setGlobal() {
        return node("setGlobal", (p, c) -> {
            Map<String, Object> vars = resolveVars(p.get("variables"), c, p);
            c.getGlobals().putAll(vars);
            return vars;
        });
    }

    /** 写节点状态：props 含 address/key/value（value 支持表达式） */
    public static LogicNode setState() {
        return node("setState", (p, c) -> {
            String address = str(p.get("address"));
            String key = str(p.get("key"));
            if (address == null || key == null) return null;
            Object value = resolve(p.get("value"), c, p);
            c.setState(address, key, value);
            return value;
        });
    }

    /** memento 批量更新：props.states = [{address,key,value}]，一次性 stage + commit */
    public static LogicNode batchUpdate() {
        return node("batchUpdate", (p, c) -> {
            Object arr = p.get("states");
            if (!(arr instanceof List<?> list)) return null;
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                String address = str(m.get("address"));
                String key = str(m.get("key"));
                if (address == null || key == null) continue;
                c.stageState(address, key, resolve(m.get("value"), c, p));
            }
            return c.commitState();
        });
    }

    /** 追加到列表变量：props.target(变量名)/value */
    public static LogicNode appendList() {
        return node("appendList", (p, c) -> {
            String name = str(p.get("target"));
            if (name == null) return null;
            Object value = resolve(p.get("value"), c, p);
            Object existing = c.getGlobals().get(name);
            List<Object> list = new ArrayList<>();
            if (existing instanceof List<?> l) list.addAll(l);
            list.add(value);
            c.getGlobals().put(name, list);
            return list;
        });
    }

    // ==================== 交互反馈 ====================

    public static LogicNode message() {
        return node("message", (p, c) -> {
            Object h = c.getServices().get(NodeHandlers.MESSAGE);
            if (h instanceof NodeHandlers.MessageHandler mh) {
                mh.show(str(resolve(p.get("type"), c, p)), str(resolve(p.get("content"), c, p)));
            }
            return null;
        });
    }

    public static LogicNode openModal() {
        return node("openModal", (p, c) -> {
            Object h = c.getServices().get(NodeHandlers.NAVIGATE);
            if (h instanceof NodeHandlers.NavigateHandler nh) {
                nh.openModal(str(resolve(p.get("modalId"), c, p)), p);
            }
            return null;
        });
    }

    public static LogicNode closeModal() {
        return node("closeModal", (p, c) -> {
            Object h = c.getServices().get(NodeHandlers.NAVIGATE);
            if (h instanceof NodeHandlers.NavigateHandler nh) nh.closeModal(str(resolve(p.get("modalId"), c, p)));
            return null;
        });
    }

    public static LogicNode jumpLink() {
        return node("jumpLink", (p, c) -> {
            Object h = c.getServices().get(NodeHandlers.NAVIGATE);
            if (h instanceof NodeHandlers.NavigateHandler nh) nh.jump(str(resolve(p.get("url"), c, p)));
            return null;
        });
    }

    // ==================== 方法调用 ====================

    /** 接口请求：从 services.request 取 handler，产出响应写入 nodeState[address].res */
    public static LogicNode interfaceRequest() {
        return node("interfaceRequest", (p, c) -> {
            Object h = c.getServices().get(NodeHandlers.REQUEST);
            if (h instanceof NodeHandlers.RequestHandler rh) {
                return rh.request(p, c);
            }
            return null;
        });
    }

    /** 自定义脚本：从 services.script 取 handler（可接 z-script / vapi 沙箱） */
    public static LogicNode customScript() {
        return node("customScript", (p, c) -> {
            Object h = c.getServices().get(NodeHandlers.SCRIPT);
            if (h instanceof NodeHandlers.ScriptHandler sh) {
                return sh.run(str(p.get("code")), c.getParams());
            }
            return null;
        });
    }

    /** 设置物料属性：写入 propsVars，供渲染侧订阅 */
    public static LogicNode setMaterialProperty() {
        return node("setMaterialProperty", (p, c) -> {
            String target = str(p.get("target"));
            if (target == null) return null;
            Object value = resolve(p.get("value"), c, p);
            c.getPropsVars().put(target + "-variable", value);
            return value;
        });
    }

    // ==================== 扩展调试 ====================

    public static LogicNode debug() {
        return node("debug", (p, c) -> {
            Object value = resolve(p.get("value"), c, p);
            c.getLogicFlowState().put("debug-" + System.nanoTime(), value);
            return value;
        });
    }

    /** 定时器节点：记录延迟意图到 logicFlowState，实际调度由宿主执行器处理 */
    public static LogicNode timer() {
        return node("timer", (p, c) -> {
            Object ms = resolve(p.get("ms"), c, p);
            c.getLogicFlowState().put("timer-ms", ms);
            return ms;
        });
    }

    // ==================== 工具 ====================

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveVars(Object varsObj, ExecutionContext ctx, Map<String, Object> props) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (!(varsObj instanceof Map)) return out;
        ((Map<String, Object>) varsObj).forEach((k, v) -> out.put(k, resolve(v, ctx, props)));
        return out;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /** 兼容旧调用入口 */
    public static Object resolve(Object value, ExecutionContext ctx) {
        return ExpressionResolver.resolve(value, ctx);
    }

    /** 带当前节点 props 的求值（props 参与 ${props.xxx} 解析） */
    public static Object resolve(Object value, ExecutionContext ctx, Map<String, Object> props) {
        return ExpressionResolver.resolve(value, ctx, props);
    }
}
