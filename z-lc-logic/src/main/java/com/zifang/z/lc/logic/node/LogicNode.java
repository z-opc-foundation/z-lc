package com.zifang.z.lc.logic.node;

import com.zifang.z.lc.logic.context.ExecutionContext;

import java.util.Map;

/**
 * 可插拔逻辑节点：运行时按 logicType 查注册表调用。
 *
 * <p>四件套思想（对应参考引擎 LogicNode 的 view/model/logic/formNode）：
 * 这里只保留运行态最核心的 type + execute；设计态的 view/formNode 由设计器侧提供。
 * 外部可通过 {@link LogicNodeRegistry#register} 覆盖或新增节点类型。</p>
 */
public interface LogicNode {

    /** 节点类型 key，如 condition / setVariables / customScript */
    String type();

    /**
     * 执行节点逻辑。
     *
     * @param props 节点参数
     * @param ctx   执行上下文（读写变量、发中断信号）
     * @return 节点产出（会被写入 nodeState[address]，供下游引用）；条件节点返回命中的分支索引或布尔值
     */
    Object execute(Map<String, Object> props, ExecutionContext ctx);
}
