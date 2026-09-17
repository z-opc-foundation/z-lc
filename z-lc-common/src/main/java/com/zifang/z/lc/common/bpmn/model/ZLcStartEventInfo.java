package com.zifang.z.lc.common.bpmn.model;

import java.io.Serializable;

/**
 * BPMN 开始事件 DTO — 蒸馏自 ace-platform-core
 * {@code StartEventInfo} ({@code com.c2f.ace.core.bpmn.model}).
 *
 * <p>用于低代码平台"流程引擎"建模 — 描述 BPMN 中一个 StartEvent 开始事件节点.
 *
 * @author zifang
 */
public class ZLcStartEventInfo implements Serializable {

    private static final long serialVersionUID = -8252409100187728582L;

    /** 开始事件唯一标识 (BPMN element id). */
    private String id;

    /** 开始事件名称 (BPMN element name). */
    private String name;

    /** 开始事件解释/文档 (BPMN documentation). */
    private String documentation;

    public ZLcStartEventInfo() {
    }

    public ZLcStartEventInfo(String id, String name, String documentation) {
        this.id = id;
        this.name = name;
        this.documentation = documentation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    @Override
    public String toString() {
        return "ZLcStartEventInfo{id='" + id + "', name='" + name + "'}";
    }
}