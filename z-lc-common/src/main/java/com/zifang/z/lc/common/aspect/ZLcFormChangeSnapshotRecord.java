package com.zifang.z.lc.common.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表单变更快照记录字段 — 蒸馏自 ace-platform-core
 * {@code FormChangeSnapshotRecord} ({@code com.c2f.ace.core.common.aspect}).
 *
 * <p>标记在表单"新增/修改/删除"方法上, 由
 * {@code ZLcFormChangeSnapshotRecordAspect} 切面拦截,
 * 在方法 {@code @AfterReturning} 时通过 Extractor 提取变更前后差异快照并发布为事件
 * (供变更通知 / 审计 / 同步等下游消费).
 *
 * <p>典型用法:
 * <pre>{@code
 * @ZLcFormChangeSnapshotRecord(
 *     actionType = "FORM_UPDATE",
 *     extractor = MyFormChangeExtractor.class)
 * public Result<Boolean> updateForm(@RequestBody FormDTO dto) {
 *     ...
 * }
 * }</pre>
 *
 * @author zifang
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ZLcFormChangeSnapshotRecord {

    /**
     * 操作动作类型 — 区分新增 / 修改 / 删除等.
     */
    String actionType();

    /**
     * 事件提取器 Class — 实现 {@code FormChangeSnapshotEventExtractor} 接口的 Spring Bean.
     *
     * <p>提取器负责把方法参数 + 返回值转换成 {@code FormChangeSnapshotEventDTO}.
     */
    Class<?> extractor();
}