package com.zifang.z.lc.logic;

import com.zifang.z.lc.logic.compile.ExecutableNode;
import com.zifang.z.lc.logic.compile.LogicCompiler;
import com.zifang.z.lc.logic.context.ExecutionContext;
import com.zifang.z.lc.logic.exec.ExecutionTrace;
import com.zifang.z.lc.logic.exec.LogicExecutor;
import com.zifang.z.lc.logic.node.DefaultNodes;
import com.zifang.z.lc.logic.node.LogicNode;
import com.zifang.z.lc.logic.node.LogicNodeRegistry;
import com.zifang.z.lc.schema.logic.FlowNode;
import com.zifang.z.lc.schema.logic.LogicFlow;
import com.zifang.z.lc.schema.model.PageSchema;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1 逻辑引擎测试：编译（主干线性化/分支树/子流程内联）+ 执行（顺序/分支/中断/上下文）。
 */
class LogicEngineTest {

    private LogicNodeRegistry registry() {
        LogicNodeRegistry r = new LogicNodeRegistry();
        DefaultNodes.install(r);
        return r;
    }

    private ExecutionContext ctx() {
        return new ExecutionContext(Map.of("x", "hello"), Map.of());
    }

    @Test
    void shouldCompileLinearChain() {
        LogicFlow flow = new LogicFlow("f", "a");
        flow.putNode("a", new FlowNode(FlowNode.Execute.logic("start", null)));
        flow.getFlowNodeMap().get("a").getNext().add(new FlowNode.NextEdge("b"));
        flow.putNode("b", new FlowNode(FlowNode.Execute.logic("setVariables", null)));

        List<ExecutableNode> nodes = LogicCompiler.compile(flow);
        assertEquals(2, nodes.size());
        assertEquals("a", nodes.get(0).getAddress());
        assertEquals("b", nodes.get(1).getAddress());
    }

    @Test
    void shouldCompileConditionAsBranches() {
        LogicFlow flow = new LogicFlow("f", "start-1");
        FlowNode cond = new FlowNode(FlowNode.Execute.logic("rhombCondition", null));
        cond.getNext().add(new FlowNode.NextEdge("yes-1", "ifLine", "trueLine"));
        cond.getNext().add(new FlowNode.NextEdge("no-1", "ifLine", "falseLine"));
        flow.putNode("start-1", new FlowNode(FlowNode.Execute.logic("start", null)));
        flow.getFlowNodeMap().get("start-1").getNext().add(new FlowNode.NextEdge("cond-1"));
        flow.putNode("cond-1", cond);
        flow.putNode("yes-1", new FlowNode(FlowNode.Execute.logic("noop", null)));
        flow.putNode("no-1", new FlowNode(FlowNode.Execute.logic("noop", null)));

        List<ExecutableNode> nodes = LogicCompiler.compile(flow);
        assertEquals(2, nodes.size()); // start + 条件节点（主干到此为止）
        ExecutableNode condNode = nodes.get(1);
        assertTrue(condNode.isConditionNode());
        assertEquals(2, condNode.getCondition().size());
        assertEquals("trueLine", condNode.getCondition().get(0).getCondition());
    }

    @Test
    void shouldInlineSubFlow() {
        PageSchema schema = new PageSchema();
        LogicFlow sub = new LogicFlow("sub", "s1");
        sub.putNode("s1", new FlowNode(FlowNode.Execute.logic("noop", null)));
        schema.getLogic().addFlow(sub);

        LogicFlow main = new LogicFlow("main", "m1");
        main.putNode("m1", new FlowNode(FlowNode.Execute.subFlow("sub", null)));
        schema.getLogic().addFlow(main);

        List<ExecutableNode> nodes = LogicCompiler.compile(schema, "main");
        assertEquals(1, nodes.size());
        assertEquals("flow", nodes.get(0).getType());
        assertEquals(1, nodes.get(0).getFlowNodes().size());
    }

    @Test
    void shouldExecuteChainAndWriteNodeState() {
        LogicFlow flow = new LogicFlow("f", "a");
        flow.putNode("a", new FlowNode(FlowNode.Execute.logic("start", null)));
        flow.getFlowNodeMap().get("a").getNext().add(new FlowNode.NextEdge("b"));
        flow.putNode("b", new FlowNode(FlowNode.Execute.logic("setVariables",
                Map.of("variables", Map.of("greet", "${x}")))));

        ExecutionContext ctx = ctx();
        ExecutionTrace trace = new LogicExecutor(registry()).execute(LogicCompiler.compile(flow), ctx);

        assertEquals(List.of("start", "setVariables"), trace.nodeTypes());
        // setVariables 的产出写入 nodeState["b"].res（产出是一个 map）
        @SuppressWarnings("unchecked")
        Map<String, Object> produced = (Map<String, Object>) ctx.state("b").get("res");
        assertEquals("hello", produced.get("greet"));
    }

    @Test
    void shouldExecuteConditionTrueBranch() {
        LogicFlow flow = new LogicFlow("f", "start-1");
        FlowNode cond = new FlowNode(FlowNode.Execute.logic("rhombCondition", null));
        cond.getNext().add(new FlowNode.NextEdge("yes-1", "ifLine", "trueLine"));
        cond.getNext().add(new FlowNode.NextEdge("no-1", "ifLine", "falseLine"));
        flow.putNode("start-1", new FlowNode(FlowNode.Execute.logic("start", null)));
        flow.getFlowNodeMap().get("start-1").getNext().add(new FlowNode.NextEdge("cond-1"));
        flow.putNode("cond-1", cond);
        flow.putNode("yes-1", new FlowNode(FlowNode.Execute.logic("setGlobal",
                Map.of("variables", Map.of("branch", "YES")))));
        flow.putNode("no-1", new FlowNode(FlowNode.Execute.logic("setGlobal",
                Map.of("variables", Map.of("branch", "NO")))));

        // 条件节点返回 true → 走 trueLine
        LogicNodeRegistry reg = registry();
        reg.register(new LogicNode() {
            @Override public String type() { return "rhombCondition"; }
            @Override public Object execute(Map<String, Object> props, ExecutionContext ctx) { return true; }
        });

        ExecutionContext ctx = ctx();
        ExecutionTrace trace = new LogicExecutor(reg).execute(LogicCompiler.compile(flow), ctx);
        assertEquals("YES", ctx.getGlobals().get("branch"));
        assertTrue(trace.getSteps().stream().anyMatch(s -> "setGlobal".equals(s.getNodeType())));
    }

    @Test
    void shouldStopExecutionOnSignal() {
        LogicNodeRegistry reg = registry();
        reg.register(new LogicNode() {
            @Override public String type() { return "stopper"; }
            @Override public Object execute(Map<String, Object> props, ExecutionContext ctx) {
                ctx.stop();
                return null;
            }
        });
        LogicFlow flow = new LogicFlow("f", "a");
        flow.putNode("a", new FlowNode(FlowNode.Execute.logic("stopper", null)));
        flow.getFlowNodeMap().get("a").getNext().add(new FlowNode.NextEdge("b"));
        flow.putNode("b", new FlowNode(FlowNode.Execute.logic("noop", null)));

        ExecutionTrace trace = new LogicExecutor(reg).execute(LogicCompiler.compile(flow), ctx());
        assertTrue(trace.isStopped());
        assertEquals(List.of("stopper"), trace.nodeTypes()); // b 未执行
    }
}
