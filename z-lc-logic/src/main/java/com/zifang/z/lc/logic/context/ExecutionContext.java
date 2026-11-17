package com.zifang.z.lc.logic.context;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 逻辑执行上下文：8 类作用域 + 中断/报错信号 + memento 批量状态提交。
 *
 * <p>作用域（与参考引擎 8 类上下文对齐，中性命名）：
 * <ol>
 *   <li>params —— 本次触发参数；</li>
 *   <li>nodeState —— 每个节点地址隔离抛出的值（如 interfaceRequest 的 res）；</li>
 *   <li>flowParams —— 子流程间传递的参数（commonLogicFlowParams）；</li>
 *   <li>globals —— 页面全局上下文变量（contextGlobalVariableMap）；</li>
 *   <li>propsVars —— 外部 → 页面：宿主传入的上下文变量（contextPropsVariableMap）；</li>
 *   <li>services —— 全局服务注册表（contextGlobalServiceMap），如请求器、消息器；</li>
 *   <li>stateApi —— 供节点读写状态的 API 句柄（_stateApi）；</li>
 *   <li>logicFlowState —— 当前逻辑流自身的中转状态。</li>
 * </ol>
 * </p>
 *
 * <p><b>memento 批量提交</b>：{@link #stageState} 只把变更累积到暂存区，
 * {@link #commitState()} 才一次性 reduce 合并进 nodeState。这样一条逻辑流里
 * 对同一地址的多次写入只产生一次最终状态，避免多次渲染/多次通知——对应参考引擎
 * 的 setStateInFlow / updateStateInFlow。</p>
 */
public class ExecutionContext {

    private final Map<String, Object> params;
    private final Map<String, Map<String, Object>> nodeState = new LinkedHashMap<>();
    private final Map<String, Object> flowParams = new HashMap<>();
    private final Map<String, Object> globals;

    // === 补齐的 4 类作用域 ===
    /** 外部 → 页面：宿主传入的上下文变量 */
    private final Map<String, Object> propsVars = new HashMap<>();
    /** 全局服务（请求器/消息器/路由等），由宿主注入 */
    private final Map<String, Object> services = new HashMap<>();
    /** 状态读写 API 句柄 */
    private final Map<String, Object> stateApi = new HashMap<>();
    /** 当前逻辑流自身中转状态 */
    private final Map<String, Object> logicFlowState = new HashMap<>();

    /** memento 暂存区：address → (key → value)，commit 时才落到 nodeState */
    private final Map<String, Map<String, Object>> staged = new LinkedHashMap<>();
    /** 可选：提交时的自定义 reduce 策略（如合并 Map、累加 List） */
    private BiFunction<Object, Object, Object> mergeStrategy = (oldV, newV) -> newV;

    /** 中断信号：与参考引擎 STOP_EXECUTE_LOGIC 等价 */
    private boolean stopped = false;

    public ExecutionContext(Map<String, Object> params, Map<String, Object> globals) {
        this.params = params == null ? new HashMap<>() : new HashMap<>(params);
        this.globals = globals == null ? new HashMap<>() : new HashMap<>(globals);
    }

    public ExecutionContext() {
        this(null, null);
    }

    // === 原有 4 类作用域 getter（保持兼容）===
    public Map<String, Object> getParams() { return params; }
    public Map<String, Object> getGlobals() { return globals; }
    public Map<String, Object> getFlowParams() { return flowParams; }
    public Map<String, Map<String, Object>> getNodeState() { return nodeState; }

    // === 补齐的 4 类作用域 getter ===
    public Map<String, Object> getPropsVars() { return propsVars; }
    public Map<String, Object> getServices() { return services; }
    public Map<String, Object> getStateApi() { return stateApi; }
    public Map<String, Object> getLogicFlowState() { return logicFlowState; }

    /** 直接写入某节点的产出（按 address 隔离），立即生效 */
    public void setState(String address, String key, Object value) {
        nodeState.computeIfAbsent(address, k -> new HashMap<>()).put(key, value);
    }

    /** 读取某节点的产出 */
    public Map<String, Object> state(String address) {
        return nodeState.getOrDefault(address, Map.of());
    }

    /**
     * memento：暂存一次状态变更，不立即生效。
     * 等价于参考引擎的 setStateInFlow —— 只累积。
     */
    public void stageState(String address, String key, Object value) {
        staged.computeIfAbsent(address, k -> new HashMap<>()).put(key, value);
    }

    /**
     * memento：把暂存区一次性 reduce 合并进 nodeState 并清空暂存区。
     * 等价于参考引擎的 updateStateInFlow —— 批量提交，只触发一次状态更新。
     *
     * @return 本次提交的 address 数量，便于调用方判断是否有变更
     */
    public int commitState() {
        int count = staged.size();
        staged.forEach((address, changes) -> {
            Map<String, Object> bucket = nodeState.computeIfAbsent(address, k -> new HashMap<>());
            changes.forEach((key, value) -> {
                Object oldV = bucket.get(key);
                bucket.put(key, oldV == null ? value : mergeStrategy.apply(oldV, value));
            });
        });
        staged.clear();
        return count;
    }

    /** 丢弃暂存区（如逻辑流中断时） */
    public void rollbackState() {
        staged.clear();
    }

    public boolean hasStaged() {
        return !staged.isEmpty();
    }

    public void setMergeStrategy(BiFunction<Object, Object, Object> mergeStrategy) {
        this.mergeStrategy = mergeStrategy == null ? (o, n) -> n : mergeStrategy;
    }

    /** 注册一个全局服务（如 request/message/router），供节点按名取用 */
    public void registerService(String name, Object service) {
        services.put(name, service);
    }

    public void stop() { this.stopped = true; }
    public boolean isStopped() { return stopped; }
}
