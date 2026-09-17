package com.zifang.z.lc.common.dto.tree;

import java.io.Serializable;

/**
 * 部门-人员树节点附件.
 *
 * <p>蒸馏自 ace-platform-client {@code DeptStaffAttachment}
 * （{@code com.c2f.ace.client.dto.tree}），字段语义完全对齐（ace 原生类也仅含默认实现）.
 *
 * <p>用途：「部门人员」树节点 — 部门 / 人员两种节点复用同一附件类，运行时按
 * {@code instanceOf} 区分.
 *
 * <p>当前 z-lc 蒸馏保持空字段集 — 业务方需要时可继承本类扩展属性（如
 * staffId / deptId / leader / memberCount 等）.
 *
 * @author zifang
 */
public class DeptStaffAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    // 占位 — ace 原生类也仅含默认实现，业务方继承扩展
}
