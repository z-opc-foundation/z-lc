package com.zifang.z.lc.core.workflow;

import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * WorkflowTriggers 词表测试：界面能给出的触发时机 = 这里承诺兑现的那一份.
 * <p>
 * 缺陷 #61 的形状是「界面上三个选项，引擎一个都不执行」。这一族的修法（同 #41 流水线阶段、
 * #48 权限词表）是把"能兑现"这件事收在一个地方说，然后让三头都对齐它：写入口拒绝、
 * 接口 {@code /vocabulary} 回报、前端下拉从同一份数据长出来。这里钉的就是那"一个地方"。
 */
public class WorkflowTriggersTest {

    private WorkflowBindingEntity binding(String event, Integer autoSubmit) {
        WorkflowBindingEntity e = new WorkflowBindingEntity();
        e.setTenantCode("default");
        e.setAppCode("crm");
        e.setEntityCode("order");
        e.setTriggerEvent(event);
        e.setProcessDefinitionKey("expense-approval");
        e.setAutoSubmit(autoSubmit);
        return e;
    }

    private void expectReject(WorkflowBindingEntity e, String mustMention) {
        try {
            WorkflowTriggers.validateForWrite(e);
            fail("写入口应当拒掉这份绑定（要能点名 [" + mustMention + "]）");
        } catch (IllegalArgumentException ex) {
            assertTrue("拒绝消息要点名 [" + mustMention + "]，实际: " + ex.getMessage(),
                    ex.getMessage() != null && ex.getMessage().contains(mustMention));
        }
    }

    @Test
    public void afterCreateIsTheOnlyHookTheEngineReallyHas() {
        List<String> implemented = WorkflowTriggers.implemented();
        assertEquals("今天真有挂接点的只有写后发起这一档；加了新档必须同时接上执行路径，"
                + "并让前端词表测试红下来: ", java.util.Collections.singletonList("AFTER_CREATE"), implemented);
        assertTrue(WorkflowTriggers.isImplemented("AFTER_CREATE"));
        assertTrue("事件名两边都该按 trim 处理: ", WorkflowTriggers.isImplemented("  AFTER_CREATE  "));
    }

    @Test
    public void vocabularyIsConsistentWithTheWriteGate() {
        // 词表说能登记的，写入口必须放行；说不兑现的，写入口必须拒 —— 两边不能各说一套。
        for (String event : WorkflowTriggers.implemented()) {
            WorkflowTriggers.validateForWrite(binding(event, null));
            assertNull("[" + event + "] 在 implemented 里却给出了拒绝理由: ",
                    WorkflowTriggers.reasonUnavailable(event));
        }
        for (Map.Entry<String, String> entry : WorkflowTriggers.unimplementedReasons().entrySet()) {
            String event = entry.getKey();
            assertFalse("[" + event + "] 既在 implemented 又在拒绝清单里（两份名单混了）: ",
                    WorkflowTriggers.isImplemented(event));
            expectReject(binding(event, null), event);
            assertNotNull("[" + event + "] 的拒绝要带原因，光说「不支持」等于没回答: ",
                    WorkflowTriggers.reasonUnavailable(event));
            assertTrue("原因里要点名事件本身: " + WorkflowTriggers.reasonUnavailable(event),
                    WorkflowTriggers.reasonUnavailable(event).contains(event));
        }
    }

    /** 阳性对照：这三个词是界面曾经给过用户的选项，必须逐个还在拒绝清单里。 */
    @Test
    public void everyOptionTheUiOnceOfferedIsStillRefusedByName() {
        for (String fromTheOldDropdown : new String[]{"AFTER_CREATE", "AFTER_UPDATE", "AFTER_DELETE"}) {
            boolean known = WorkflowTriggers.isImplemented(fromTheOldDropdown)
                    || WorkflowTriggers.unimplementedReasons().containsKey(fromTheOldDropdown);
            assertTrue("界面上出现过的 [" + fromTheOldDropdown + "] 必须在词表里有个说法，"
                    + "否则用户提交时得到的是「不认识的事件」这种没有信息量的话", known);
        }
    }

    @Test
    public void rejectionReasonsSayWhyNotJustNo() {
        // 每条原因都要给出"为什么"的可操作线索，而不是一句"不支持"。
        assertTrue(WorkflowTriggers.reasonUnavailable("AFTER_UPDATE")
                .contains("这条记录对应哪个流程实例"));
        assertTrue(WorkflowTriggers.reasonUnavailable("AFTER_DELETE")
                .contains("没有实例账"));
        assertTrue(WorkflowTriggers.reasonUnavailable("status_change")
                .contains("状态"));
        assertTrue(WorkflowTriggers.reasonUnavailable("AFTER_UPDATE")
                .contains(WorkflowTriggers.AFTER_CREATE));
    }

    @Test
    public void unknownEventsAreRejectedAndToldWhatIsAvailable() {
        expectReject(binding("ON_SUNDAY", null), "ON_SUNDAY");
        expectReject(binding(null, null), "不认识的触发事件");
        expectReject(binding("   ", null), "不认识的触发事件");
        assertTrue(WorkflowTriggers.reasonUnavailable("ON_SUNDAY").contains("AFTER_CREATE"));
    }

    @Test
    public void missingIdentityIsRefusedBeforeAnythingElse() {
        WorkflowBindingEntity noApp = binding(WorkflowTriggers.AFTER_CREATE, null);
        noApp.setAppCode(null);
        expectReject(noApp, "appCode");
        WorkflowBindingEntity noEntity = binding(WorkflowTriggers.AFTER_CREATE, null);
        noEntity.setEntityCode("  ");
        expectReject(noEntity, "entityCode");
        WorkflowBindingEntity noKey = binding(WorkflowTriggers.AFTER_CREATE, null);
        noKey.setProcessDefinitionKey("");
        expectReject(noKey, "processDefinitionKey");
        try {
            WorkflowTriggers.validateForWrite(null);
            fail("null 绑定也要拒绝而不是 NPE");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("不能为空"));
        }
    }

    @Test
    public void autoSubmitOffIsRefusedBecauseTheBindingWouldBeDecorative() {
        expectReject(binding(WorkflowTriggers.AFTER_CREATE, 0), "自动提单");
        // 阳性对照：1 与留空都放行 —— 否则"关掉才拒"这一支可能是常数拒绝造出来的假绿。
        WorkflowTriggers.validateForWrite(binding(WorkflowTriggers.AFTER_CREATE, 1));
        WorkflowTriggers.validateForWrite(binding(WorkflowTriggers.AFTER_CREATE, null));
    }

    @Test
    public void unimplementedListCoversTheEventsMentionedInTheEntityComment() {
        // WorkflowBindingEntity 的字段注释写着 AFTER_CREATE / AFTER_UPDATE / status_change；
        // 注释里出现过的词都必须在这份词表里有说法，否则注释就是在教用户填一个被拒的值。
        for (String mentioned : new String[]{"AFTER_CREATE", "AFTER_UPDATE", "status_change"}) {
            assertTrue("[" + mentioned + "] 在实体注释里被当作可选值提到，却没进词表: ",
                    WorkflowTriggers.isImplemented(mentioned)
                            || WorkflowTriggers.unimplementedReasons().containsKey(mentioned));
        }
    }
}
