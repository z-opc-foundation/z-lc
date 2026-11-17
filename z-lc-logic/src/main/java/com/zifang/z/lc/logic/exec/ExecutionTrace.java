package com.zifang.z.lc.logic.exec;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行轨迹：记录节点进入顺序、分支选择、停止信号，供调试与埋点（对应参考引擎 executionTrace）。
 */
public class ExecutionTrace {

    public static class Step {
        private final String address;
        private final String nodeType;
        private String detail;

        Step(String address, String nodeType) {
            this.address = address;
            this.nodeType = nodeType;
        }

        public String getAddress() { return address; }
        public String getNodeType() { return nodeType; }
        public String getDetail() { return detail; }
    }

    private final List<Step> steps = new ArrayList<>();
    private boolean stopped;

    public void enter(String address, String nodeType) {
        steps.add(new Step(address, nodeType));
    }

    public void record(String address, String detail) {
        Step last = lastOf(address);
        if (last != null) last.detail = detail;
    }

    public void branch(String address, String condition, boolean truthy) {
        Step last = lastOf(address);
        if (last != null) last.detail = "branch=" + condition + " -> " + truthy;
    }

    private Step lastOf(String address) {
        for (int i = steps.size() - 1; i >= 0; i--) {
            if (address == null ? steps.get(i).address == null : address.equals(steps.get(i).address)) {
                return steps.get(i);
            }
        }
        return null;
    }

    public void markStopped() { this.stopped = true; }

    public List<Step> getSteps() { return steps; }
    public boolean isStopped() { return stopped; }

    /** 按顺序返回执行过的节点类型，便于断言 */
    public List<String> nodeTypes() {
        List<String> types = new java.util.ArrayList<>();
        steps.forEach(s -> types.add(s.nodeType));
        return types;
    }
}
