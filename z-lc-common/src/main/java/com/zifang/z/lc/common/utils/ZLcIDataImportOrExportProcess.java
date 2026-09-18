package com.zifang.z.lc.common.utils;

/**
 * 数据导入导出处理接口 — 蒸馏自 ace-platform-core
 * {@code IDataImportOrExportProcess} ({@code com.c2f.ace.core.middleware.task}).
 *
 * <p>定义数据导入导出任务的生命周期方法: 获取待处理任务、执行任务、异常中断等.
 * 配合 {@link ZLcDataImportExportExecutor} 使用.
 *
 * @param <T> 任务类型
 * @author zifang
 */
public interface ZLcIDataImportOrExportProcess<T> {

    /**
     * 任务异常中断 (防止异常任务长时间处理不成功).
     */
    void taskAbnormalInterrupt();

    /**
     * 执行任务.
     *
     * @param task 任务
     */
    void taskExecute(T task);

    /**
     * 获取下个要执行的任务.
     *
     * @return 待执行任务; 无任务时返回 null
     */
    T nextTask();

    /**
     * 获取处理中的任务数量.
     *
     * @return 正在执行的任务数
     */
    Long getInExecutionTaskCount();

    /**
     * 获取当前执行中的任务.
     *
     * @return 执行中的任务; 无则返回 null
     */
    T getInExecutionTask();

    /**
     * 更新任务状态为执行中.
     *
     * @param task 任务
     */
    void updateTaskStatusToExecute(T task);
}
