package com.zifang.z.lc.logic.exec;

import com.zifang.z.lc.logic.compile.ExecutableNode;
import com.zifang.z.lc.logic.context.ExecutionContext;
import com.zifang.z.lc.logic.context.ExpressionResolver;
import com.zifang.z.lc.logic.node.LogicNode;
import com.zifang.z.lc.logic.node.LogicNodeRegistry;

import java.util.List;
import java.util.Map;

/**
 * 逻辑执行器 —— Java 版「Generator + run() 驱动器」的等价实现。
 *
 * <p>参考引擎用 function* + yield 统一同步/异步/中断；Java 无原生 Generator，
 * 这里用「顺序循环 + 分支递归 + 统一的 sync/async 归一化」达到同等语义：
 * <ul>
 *   <li>executeNode 返回 Object：若结果是 {@link java.util.concurrent.CompletableFuture} 则等待其完成；</li>
 *   <li>每执行一步检查 {@link ExecutionContext#isStopped()}，实现断流；</li>
 *   <li>节点产出按 address 写入 nodeState，供下游引用；</li>
 *   <li>条件节点根据布尔结果选择命中的 Branch，递归执行其子树；</li>
 *   <li>子流程节点递归执行内联的 flowNodes。</li>
 * </ul>
 * 执行耗时/执行轨迹通过 {@link ExecutionTrace} 留痕，供调试与埋点。</p>
 */
public class LogicExecutor {

    private final LogicNodeRegistry registry;

    public LogicExecutor(LogicNodeRegistry registry) {
        this.registry = registry;
    }

    /** 执行一条编译后的节点链，返回执行轨迹 */
    public ExecutionTrace execute(List<ExecutableNode> nodes, ExecutionContext ctx) {
        ExecutionTrace trace = new ExecutionTrace();
        executeNodes(nodes, ctx, trace);
        // 兜底提交：节点内未显式 commit 的暂存变更在整条流结束时统一落地（memento）
        ctx.commitState();
        return trace;
    }

    private void executeNodes(List<ExecutableNode> nodes, ExecutionContext ctx, ExecutionTrace trace) {
        if (nodes == null) {
            return;
        }
        for (ExecutableNode node : nodes) {
            if (ctx.isStopped()) {
                trace.markStopped();
                return;
            }
            executeNode(node, ctx, trace);
            if (ctx.isStopped()) {
                trace.markStopped();
                return;
            }
        }
    }

    private void executeNode(ExecutableNode node, ExecutionContext ctx, ExecutionTrace trace) {
        if ("flow".equals(node.getType())) {
            trace.enter(node.getAddress(), "flow");
            executeNodes(node.getFlowNodes(), ctx, trace);
            return;
        }

        LogicNode impl = registry.get(node.getLogicType());
        trace.enter(node.getAddress(), node.getLogicType());
        Object result;
        if (impl == null) {
            trace.record(node.getAddress(), "未注册节点:" + node.getLogicType());
            result = null;
        } else {
            result = impl.execute(node.getProps(), ctx);
        }
        result = awaitIfAsync(result);

        // 产出写入节点隔离作用域
        if (result != null && node.getAddress() != null) {
            ctx.setState(node.getAddress(), "res", result);
        }

        // 条件节点：解析分支
        if (node.isConditionNode() && !node.getCondition().isEmpty()) {
            boolean truthy = toBoolean(result);
            ExecutableNode.Branch branch = pickBranch(node, truthy, ctx);
            if (branch != null) {
                trace.branch(node.getAddress(), branch.getCondition(), truthy);
                executeNodes(branch.getFlowNodes(), ctx, trace);
            }
        }
    }

    /** 从条件节点的分支里挑命中的一条 */
    private ExecutableNode.Branch pickBranch(ExecutableNode node, boolean truthy, ExecutionContext ctx) {
        // 优先按 ifLine 的 trueLine/falseLine 语义选
        for (ExecutableNode.Branch b : node.getCondition()) {
            String cond = b.getCondition();
            if ("trueLine".equals(cond) && truthy) return b;
            if ("falseLine".equals(cond) && !truthy) return b;
        }
        // expression 类型：取第一个条件表达式求值为真的（这里用轻量解析，复杂表达式交给自定义节点）
        for (ExecutableNode.Branch b : node.getCondition()) {
            if ("ifLine".equals(b.getConditionType())) continue;
            cond: {
                Object v = ExpressionResolver.resolve(b.getCondition(), ctx, node.getProps());
                if (v instanceof Boolean bo) {
                    if (bo) return b;
                } else if (v != null && !v.toString().isEmpty() && !"false".equalsIgnoreCase(v.toString())) {
                    return b;
                }
            }
        }
        // 兜底：布尔真取第一条，假取最后一条
        return truthy ? node.getCondition().get(0) : node.getCondition().get(node.getCondition().size() - 1);
    }

    @SuppressWarnings("unchecked")
    private Object awaitIfAsync(Object result) {
        if (result instanceof java.util.concurrent.CompletableFuture<?> f) {
            try {
                return f.join();
            } catch (Exception e) {
                return null;
            }
        }
        return result;
    }

    private boolean toBoolean(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.doubleValue() != 0;
        return !"false".equalsIgnoreCase(v.toString()) && !"".equals(v.toString());
    }
}
