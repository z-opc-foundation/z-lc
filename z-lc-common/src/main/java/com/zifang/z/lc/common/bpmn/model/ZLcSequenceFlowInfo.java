package com.zifang.z.lc.common.bpmn.model;

import java.io.Serializable;

/**
 * BPMN 连线 DTO — 蒸馏自 ace-platform-core
 * {@code SequenceFlowInfo} ({@code com.c2f.ace.core.bpmn.model}).
 *
 * <p>用于低代码平台"流程引擎"建模 — 描述 BPMN 中一个 SequenceFlow 连线的基本属性.
 *
 * <p>字段语义:
 * <ul>
 *   <li>{@code id} — 连线唯一标识 (BPMN element id)</li>
 *   <li>{@code sourceRef} — 起始节点 id (BPMN sourceRef)</li>
 *   <li>{@code targetRef} — 目标节点 id (BPMN targetRef)</li>
 * </ul>
 *
 * @author zifang
 */
public class ZLcSequenceFlowInfo implements Serializable {

    private static final long serialVersionUID = -3255066890670214578L;

    /** 连线唯一标识 (BPMN element id). */
    private String id;

    /** 起始节点 id (BPMN sourceRef). */
    private String sourceRef;

    /** 目标节点 id (BPMN targetRef). */
    private String targetRef;

    public ZLcSequenceFlowInfo() {
    }

    public ZLcSequenceFlowInfo(String id, String sourceRef, String targetRef) {
        this.id = id;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    @Override
    public String toString() {
        return "ZLcSequenceFlowInfo{id='" + id + "', " +
                "sourceRef='" + sourceRef + "', targetRef='" + targetRef + "'}";
    }
}