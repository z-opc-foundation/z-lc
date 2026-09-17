package com.zifang.z.lc.common.bpmn.model;

import java.io.Serializable;

/**
 * BPMN 结束事件 DTO — 蒸馏自 ace-platform-core
 * {@code EndEventInfo} ({@code com.c2f.ace.core.bpmn.model}).
 *
 * <p>用于低代码平台"流程引擎"建模 — 描述 BPMN 中一个 EndEvent 结束事件节点.
 *
 * @author zifang
 */
public class ZLcEndEventInfo implements Serializable {

    private static final long serialVersionUID = -2779726330941635796L;

    /** 结束事件唯一标识 (BPMN element id). */
    private String id;

    /** 结束事件名称 (BPMN element name). */
    private String name;

    /** 结束事件解释/文档 (BPMN documentation). */
    private String documentation;

    public ZLcEndEventInfo() {
    }

    public ZLcEndEventInfo(String id, String name, String documentation) {
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
        return "ZLcEndEventInfo{id='" + id + "', name='" + name + "'}";
    }
}