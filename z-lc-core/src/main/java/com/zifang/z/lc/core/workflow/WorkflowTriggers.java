package com.zifang.z.lc.core.workflow;

import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程绑定的触发事件词表：「引擎在哪些事件上真有挂接点」这件事只在这里说一次.
 * <p>
 * 为什么要有这个类（缺陷 #61，与 #41 同一族）：{@code WorkflowsPage} 把触发时机做成三个选项
 * （创建后/更新后/删除后），而 {@code WorkflowBindingService.listByEvent} 与
 * {@code WfAdapter.startProcess} 在生产代码里<b>零调用者</b> —— 三个选项一个都不会执行。
 * 更糟的是那个"执行器"自己也是坏的：URL 与字段名对不上 z-wf 的真实契约（见
 * {@link com.zifang.z.lc.core.adapter.WfAdapter} 类注释），所以"接上"不是把调用点加回去就完事。
 * <p>
 * 这一族的口径和 {@link com.zifang.z.lc.core.pipeline.config.PipelineStages} 一致：
 * <b>写入口拒掉兑现不了的形态</b>（一份永远不会发的绑定不该出生），运行期只做能兑现的那一件事。
 * "更新后/删除后"不是被禁了，是引擎还没有那个挂接点：z-lc 没有在任何地方存过
 * "这条记录对应哪个流程实例"，而 {@code z_lc_workflow_binding} 只有绑定关系、没有实例账，
 * 所以更新时再发一次就是重复提单，删除时想挂起也无从查起。等真有了实例账再把它加进
 * {@link #IMPLEMENTED}，界面上的选项会跟着这份数据长出来（前端词表测试机械比对这里）。
 */
public final class WorkflowTriggers {

    /** 唯一真有挂接点的事件：记录创建成功之后发起一个流程实例。 */
    public static final String AFTER_CREATE = "AFTER_CREATE";

    private static final List<String> IMPLEMENTED = Collections.singletonList(AFTER_CREATE);

    /** 界面上出现过/注释里提到过但引擎兑现不了的事件 ⇒ 拒绝时要点名原因，不能只说"不支持"。 */
    private static final Map<String, String> UNIMPLEMENTED = unimplemented();

    private static Map<String, String> unimplemented() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("AFTER_UPDATE", "更新后重新发起会让同一条记录提出一堆并行流程，而 z-lc 没有存"
                + "「这条记录对应哪个流程实例」，谈不上「改的是同一个单」");
        m.put("AFTER_DELETE", "删除后既没有可挂起的实例（同上，没有实例账），也没有可终止的入口");
        m.put("status_change", "引擎没有「状态变更」这一类事件：字段的写入不产生状态机回调");
        m.put("BEFORE_CREATE", "写前挂接点属于处理流水线（#41），流程发起必须在拿到记录 id 之后");
        m.put("BEFORE_UPDATE", "同上：写前挂接点是流水线的，不是流程的");
        return Collections.unmodifiableMap(m);
    }

    private WorkflowTriggers() {
    }

    /** 可以在这里登记绑定 ⇒ 就是 {@link #AFTER_CREATE}；前端与接口文档的清单都从这里取。 */
    public static List<String> implemented() {
        return IMPLEMENTED;
    }

    public static boolean isImplemented(String triggerEvent) {
        return triggerEvent != null && IMPLEMENTED.contains(triggerEvent.trim());
    }

    /**
     * 保存一条绑定前的全部要求。抛 {@link IllegalArgumentException} ⇒ 由
     * {@code LcExceptionHandler} 映射成 400，消息里点名要改哪一格。
     */
    public static void validateForWrite(WorkflowBindingEntity binding) {
        if (binding == null) {
            throw new IllegalArgumentException("流程绑定不能为空");
        }
        requireText(binding.getAppCode(), "appCode（哪个应用的实体）");
        requireText(binding.getEntityCode(), "entityCode（哪张表）");
        requireText(binding.getProcessDefinitionKey(), "processDefinitionKey（要发起的流程 KEY）");
        if (!isImplemented(binding.getTriggerEvent())) {
            throw new IllegalArgumentException(rejectTrigger(binding.getTriggerEvent()));
        }
        Integer autoSubmit = binding.getAutoSubmit();
        if (autoSubmit != null && autoSubmit.intValue() != 1) {
            throw new IllegalArgumentException("「自动提单」关掉之后这条绑定没有任何运行时行为："
                    + "记录创建时不发起流程，界面上也没有手动提单的入口。要么打开自动提单，"
                    + "要么不要留这条绑定（autoSubmit=" + autoSubmit + "）");
        }
    }

    private static String rejectTrigger(String triggerEvent) {
        String trimmed = triggerEvent == null ? null : triggerEvent.trim();
        if (trimmed != null && UNIMPLEMENTED.containsKey(trimmed)) {
            return "触发事件 [" + trimmed + "] 引擎兑现不了：" + UNIMPLEMENTED.get(trimmed)
                    + "。当前可登记的只有 " + join(IMPLEMENTED);
        }
        return "不认识的触发事件 [" + triggerEvent + "]，当前可登记的只有 " + join(IMPLEMENTED);
    }

    /** 为什么这个事件被拒（null = 可以登记）。给界面/接口文档用的同一份说法。 */
    public static String reasonUnavailable(String triggerEvent) {
        String trimmed = triggerEvent == null ? null : triggerEvent.trim();
        if (isImplemented(trimmed)) {
            return null;
        }
        return rejectTrigger(triggerEvent);
    }

    /** 界面/文档要展示的「不兑现清单」：事件名 → 原因。顺序稳定。 */
    public static Map<String, String> unimplementedReasons() {
        return UNIMPLEMENTED;
    }

    private static void requireText(String value, String what) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("流程绑定缺少 " + what + "，缺了它的绑定一条都不会发");
        }
    }

    private static String join(List<String> values) {
        return Arrays.toString(values.toArray());
    }
}
