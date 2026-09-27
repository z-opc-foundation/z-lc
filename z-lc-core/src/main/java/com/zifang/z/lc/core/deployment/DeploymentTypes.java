package com.zifang.z.lc.core.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部署方式的词表：「哪一种 deployType 在服务器这边真有执行器」只在这里说一次（缺陷 #70，与
 * {@link com.zifang.z.lc.core.workflow.WorkflowTriggers} /
 * {@link com.zifang.z.lc.core.pipeline.config.PipelineStages} 同一族）。
 * <p>
 * 为什么要有这个类：{@code DeploymentController.create} 原先是 insert 一行 PENDING 然后
 * {@code // TODO: 异步执行物化/部署逻辑} 直接返回，而全仓没有任何执行器 ——
 * {@code updateDeploymentStatus} 在生产代码里零调用者，所以 PENDING 是**终态**：
 * 界面弹「部署已创建」、状态列永远灰、{@code deploy_log} 抽屉永远写「（暂无日志）」。
 * 同一趟还收下不存在的 appCode、不存在的 materializationId、以及一个 NOT NULL 列不给值时的裸 500。
 * <p>
 * 口径与本仓既有两族一致：<b>写入口拒掉兑现不了的形态</b>（一条永远不会执行的部署记录不该出生），
 * 能兑现的那一种就<b>真的执行</b>并把结局写回那一行。
 * "Docker 镜像/Git 推送"不是被禁了，是服务器没有那个能力：等真有了执行器再把它加进
 * {@link #IMPLEMENTED}，界面上的选项会跟着这份数据长出来（前端词表测试机械比对这里）。
 */
public final class DeploymentTypes {

    /**
     * 唯一有执行器的一种：把应用当前的实体定义应用到运行时库（逐实体建表 / 只补缺的列），
     * 并把每一支的结局汇总写进这条记录的 {@code deploy_log}。
     * <p>
     * 它兑现的是"让定义落地"这件事本身 —— 运行时读写的是物理表，定义没落到表上就等于没生效。
     */
    public static final String HOT_LOAD = "HOT_LOAD";

    private static final List<String> IMPLEMENTED = Collections.singletonList(HOT_LOAD);

    /** 界面上出现过或测试里用过、而服务器兑现不了的方式 ⇒ 拒绝时点名原因，不能只说"不支持"。 */
    private static final Map<String, String> UNIMPLEMENTED = unimplemented();

    private static Map<String, String> unimplemented() {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("DOCKER", "服务器不做镜像构建：仓内没有 docker client、没有 Dockerfile，也没有镜像仓库的凭据"
                + "—— 收下这条只会留下一行永远 PENDING 的账");
        m.put("GIT_PUSH", "应用定义存在 z-lc 自己的元数据表里，不在 git 仓里；z-lc 也不持有远端与凭据，"
                + "push 之后没有任何服务会被触发");
        m.put("SQL", "部署记录里没有「一段 SQL」这一格，也没有执行器去读它 —— "
                + "要按份 DDL 落库，走 schema 那一侧的建表接口，别挂在一个跑不动的部署任务上");
        return Collections.unmodifiableMap(m);
    }

    private DeploymentTypes() {
    }

    /** 可以登记的部署方式 ⇒ 就是 {@link #HOT_LOAD}；界面清单与接口文档都从这里取。 */
    public static List<String> implemented() {
        return IMPLEMENTED;
    }

    public static boolean isImplemented(String deployType) {
        return deployType != null && IMPLEMENTED.contains(deployType.trim());
    }

    /**
     * 创建一条部署任务前的门槛。抛 {@link IllegalArgumentException} ⇒ 由 {@code LcExceptionHandler}
     * 映射成 400，消息里点名要改哪一格。
     */
    public static void validateForWrite(String deployType) {
        if (deployType == null || deployType.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少 deployType（部署方式）：这一列 NOT NULL，"
                    + "当前可登记的只有 " + join(IMPLEMENTED));
        }
        if (!isImplemented(deployType)) {
            throw new IllegalArgumentException(reject(deployType));
        }
    }

    /** 这种方式会不会真的被执行（null = 会）。给界面/接口文档用的同一份说法。 */
    public static String reasonUnavailable(String deployType) {
        if (isImplemented(deployType)) {
            return null;
        }
        return deployType == null || deployType.trim().isEmpty()
                ? "缺少 deployType（部署方式）" : reject(deployType);
    }

    /** 界面/文档要展示的「不执行清单」：方式名 → 原因。顺序稳定。 */
    public static Map<String, String> unimplementedReasons() {
        return UNIMPLEMENTED;
    }

    private static String reject(String deployType) {
        String trimmed = deployType.trim();
        String known = UNIMPLEMENTED.get(trimmed);
        if (known != null) {
            return "部署方式 [" + trimmed + "] 服务器执行不了：" + known + "。当前可登记的只有 " + join(IMPLEMENTED);
        }
        return "不认识的部署方式 [" + deployType + "]，当前可登记的只有 " + join(IMPLEMENTED);
    }

    private static String join(List<String> values) {
        return Arrays.toString(values.toArray());
    }
}
