package com.zifang.z.lc.common.bpmn.model;

import java.io.Serializable;

/**
 * BPMN 网关节点 DTO — 蒸馏自 ace-platform-core
 * {@code GateWayInfo} ({@code com.c2f.ace.core.bpmn.model}).
 *
 * <p>用于低代码平台"流程引擎"建模 — 描述 BPMN 中一个 Gateway 网关节点
 * (排他网关 / 并行网关 / 包容网关 / 事件网关 等).
 *
 * <p>蒸馏说明：ace 原 {@code GateWayInfo} 与 {@code EndEventInfo} 误用相同
 * {@code serialVersionUID}, 蒸馏版修正.
 *
 * @author zifang
 */
public class ZLcGateWayInfo implements Serializable {

    private static final long serialVersionUID = -2779726330941635797L;

    /** 网关唯一标识 (BPMN element id). */
    private String id;

    /** 网关名称 (BPMN element name). */
    private String name;

    /** 网关解释/文档 (BPMN documentation). */
    private String documentation;

    public ZLcGateWayInfo() {
    }

    public ZLcGateWayInfo(String id, String name, String documentation) {
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
        return "ZLcGateWayInfo{id='" + id + "', name='" + name + "'}";
    }
}