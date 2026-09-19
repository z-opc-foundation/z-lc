package com.zifang.z.lc.common.bpmn.listener;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ZLcWorkflowEventListener 单元测试
 *
 * @author zifang
 */
class ZLcWorkflowEventListenerTest {

    @Test
    void shouldBeInterface() {
        assertThat(ZLcWorkflowEventListener.class.isInterface()).isTrue();
    }

    @Test
    void shouldReceiveEventCallback() {
        AtomicInteger counter = new AtomicInteger(0);
        ZLcWorkflowEventListener listener = new ZLcWorkflowEventListener() {
            @Override
            public void onEvent(String eventType, String processInstanceId, String taskId,
                               Map<String, Object> variables) {
                counter.incrementAndGet();
            }
        };

        listener.onEvent("processStarted", "proc-001", null, new HashMap<>());
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldDefaultListenerNameToClassName() {
        ZLcWorkflowEventListener listener = new NamedListener();

        assertThat(listener.listenerName()).isEqualTo("NamedListener");
    }

    private static class NamedListener implements ZLcWorkflowEventListener {
        @Override
        public void onEvent(String eventType, String processInstanceId, String taskId,
                           Map<String, Object> variables) {
        }
    }

    @Test
    void shouldOverrideListenerName() {
        ZLcWorkflowEventListener listener = new ZLcWorkflowEventListener() {
            @Override
            public void onEvent(String eventType, String processInstanceId, String taskId,
                               Map<String, Object> variables) {
            }

            @Override
            public String listenerName() {
                return "CustomListener";
            }
        };

        assertThat(listener.listenerName()).isEqualTo("CustomListener");
    }

    @Test
    void shouldHandleNullVariables() {
        ZLcWorkflowEventListener listener = new ZLcWorkflowEventListener() {
            @Override
            public void onEvent(String eventType, String processInstanceId, String taskId,
                               Map<String, Object> variables) {
                assertThat(variables).isNull();
            }
        };

        listener.onEvent("processStarted", "proc-001", null, null);
    }

    @Test
    void shouldHaveOnEventMethod() throws NoSuchMethodException {
        assertThat(ZLcWorkflowEventListener.class.getMethod("onEvent",
                String.class, String.class, String.class, Map.class)).isNotNull();
    }
}