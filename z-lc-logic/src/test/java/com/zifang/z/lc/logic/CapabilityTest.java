package com.zifang.z.lc.logic;

import com.zifang.z.lc.logic.context.ExecutionContext;
import com.zifang.z.lc.logic.context.ExpressionResolver;
import com.zifang.z.lc.logic.node.DefaultNodes;
import com.zifang.z.lc.logic.node.LogicNodeRegistry;
import com.zifang.z.lc.logic.node.NodeHandlers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** P1 能力补齐测试：表达式 / 8 类作用域 / memento / 副作用节点 / 节点库规模 */
class CapabilityTest {

    @Test
    void shouldRegisterAll21Nodes() {
        LogicNodeRegistry reg = new LogicNodeRegistry();
        DefaultNodes.install(reg);
        assertEquals(21, reg.all().size());
        assertEquals(21, DefaultNodes.types().size());
        for (String t : DefaultNodes.types()) {
            assertTrue(reg.has(t), "缺少节点: " + t);
        }
    }

    @Test
    void expressionShouldResolvePathsAndOperators() {
        ExecutionContext ctx = new ExecutionContext(
                Map.of("x", 5, "name", "zs"),
                Map.of("theme", "dark"));
        ctx.setState("n1", "res", Map.of("total", 100));

        assertEquals(5, ((Number) ExpressionResolver.resolve("${x}", ctx)).intValue());
        assertEquals("dark", ExpressionResolver.resolve("${global.theme}", ctx));
        assertEquals(100, ((Number) ExpressionResolver.resolve("${state.n1.res.total}", ctx)).intValue());
        assertEquals(100, ((Number) ExpressionResolver.resolve("${state('n1').res.total}", ctx)).intValue());
        assertEquals(Boolean.TRUE, ExpressionResolver.resolve("${x > 3}", ctx));
        assertEquals(Boolean.FALSE, ExpressionResolver.resolve("${x == 9}", ctx));
        assertEquals(Boolean.TRUE, ExpressionResolver.resolve("${x > 3 && theme == 'dark'}", ctx));
        assertEquals("你好 zs", ExpressionResolver.resolve("你好 ${name}", ctx));
    }

    @Test
    void contextShouldExposeEightScopes() {
        ExecutionContext ctx = new ExecutionContext(Map.of(), Map.of());
        assertNotNull(ctx.getParams());
        assertNotNull(ctx.getNodeState());
        assertNotNull(ctx.getFlowParams());
        assertNotNull(ctx.getGlobals());
        assertNotNull(ctx.getPropsVars());
        assertNotNull(ctx.getServices());
        assertNotNull(ctx.getStateApi());
        assertNotNull(ctx.getLogicFlowState());
    }

    @Test
    void mementoShouldStageThenCommitOnce() {
        ExecutionContext ctx = new ExecutionContext(Map.of(), Map.of());
        ctx.stageState("a", "v", 1);
        ctx.stageState("a", "v", 2);
        ctx.stageState("b", "v", 3);
        assertTrue(ctx.hasStaged());
        assertTrue(ctx.state("a").isEmpty()); // 提交前不可见
        int committed = ctx.commitState();
        assertEquals(2, committed); // 两个 address
        assertFalse(ctx.hasStaged());
        assertEquals(2, ctx.state("a").get("v"));
        assertEquals(3, ctx.state("b").get("v"));
    }

    @Test
    void mementoRollbackShouldDropStaged() {
        ExecutionContext ctx = new ExecutionContext(Map.of(), Map.of());
        ctx.stageState("a", "v", 1);
        ctx.rollbackState();
        assertFalse(ctx.hasStaged());
        assertTrue(ctx.state("a").isEmpty());
    }

    @Test
    void sideEffectNodesShouldUseInjectedHandlers() {
        ExecutionContext ctx = new ExecutionContext(Map.of("url", "app://home"), Map.of());
        List<String> msgs = new ArrayList<>();
        List<String> jumps = new ArrayList<>();
        ctx.registerService(NodeHandlers.MESSAGE, (NodeHandlers.MessageHandler) (type, content) -> msgs.add(type + ":" + content));
        ctx.registerService(NodeHandlers.NAVIGATE, new NodeHandlers.NavigateHandler() {
            @Override public void jump(String url) { jumps.add(url); }
            @Override public void openModal(String id, Map<String, Object> p) { jumps.add("modal:" + id); }
            @Override public void closeModal(String id) { jumps.add("close:" + id); }
        });
        ctx.registerService(NodeHandlers.REQUEST, (NodeHandlers.RequestHandler) (p, c) -> Map.of("ok", true));

        DefaultNodes.message().execute(Map.of("type", "success", "content", "done"), ctx);
        DefaultNodes.jumpLink().execute(Map.of("url", "${url}"), ctx);
        Object res = DefaultNodes.interfaceRequest().execute(Map.of("url", "/api/x"), ctx);

        assertEquals(List.of("success:done"), msgs);
        assertEquals(List.of("app://home"), jumps);
        assertEquals(Map.of("ok", true), res);
    }

    @Test
    void noHandlerShouldBeSafeNoop() {
        ExecutionContext ctx = new ExecutionContext(Map.of(), Map.of());
        assertDoesNotThrow(() -> DefaultNodes.message().execute(Map.of("content", "x"), ctx));
        assertDoesNotThrow(() -> DefaultNodes.interfaceRequest().execute(Map.of(), ctx));
        assertDoesNotThrow(() -> DefaultNodes.customScript().execute(Map.of("code", "1+1"), ctx));
    }

    @Test
    void endNodeShouldStopFlow() {
        ExecutionContext ctx = new ExecutionContext(Map.of(), Map.of());
        DefaultNodes.end().execute(Map.of(), ctx);
        assertTrue(ctx.isStopped());
    }
}
