package com.zifang.z.lc.core.event;

/**
 * 事件冲突异常 (parent_event_id 不匹配当前末次事件 → 409)
 * 实现: 不允许出现冲突 (设计决策)
 */
public class EventConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String expectedParentEventId;
    private final String actualLastEventId;

    public EventConflictException(String expectedParentEventId, String actualLastEventId) {
        super("Event conflict: parent_event_id=" + expectedParentEventId
                + " but last event_id=" + actualLastEventId);
        this.expectedParentEventId = expectedParentEventId;
        this.actualLastEventId = actualLastEventId;
    }

    public String getExpectedParentEventId() {
        return expectedParentEventId;
    }

    public String getActualLastEventId() {
        return actualLastEventId;
    }
}
