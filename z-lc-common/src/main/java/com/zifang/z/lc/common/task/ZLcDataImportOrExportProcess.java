package com.zifang.z.lc.common.task;

/**
 * 数据导入/导出处理 SPI 接口 — 蒸馏自 ace-platform-core
 * {@code IDataImportOrExportProcess} ({@code com.c2f.ace.core.middleware.task}).
 *
 * <p>用于低代码平台"数据导入/导出"模块 — 业务方实现本接口, 提供"任务执行 /
 * 拉取下一个任务 / 异常中断"等核心能力. z-lc-engine 通过本接口调度任务的执行.
 *
 * <p>典型用法:
 * <pre>{@code
 * public class MyImportProcess
 *         implements ZLcDataImportOrExportProcess<MyTask> {
 *
 *     {@code @Override}
 *     public void taskExecute(MyTask task) { ... }
 *
 *     {@code @Override}
 *     public MyTask nextTask() { ... }
 *     ...
 * }
 *
 * ZLcDataImportOrExportProcess<MyTask> process = new MyImportProcess();
 * process.taskExecute(task);
 * }</pre>
 *
 * @author zifang
 */
public interface ZLcDataImportOrExportProcess<T extends ZLcBaseDataProcessTask> {

    /**
     * 任务异常中断 — 用于兜底策略, 防止异常任务长时间占用执行位.
     */
    void taskAbnormalInterrupt();

    /**
     * 执行任务.
     *
     * @param task 任务对象
     */
    void taskExecute(T task);

    /**
     * 获取下一个要执行的任务.
     *
     * @return 下一个任务; 无任务时返回 {@code null}
     */
    T nextTask();

    /**
     * 获取处理中的任务数量.
     */
    Long getInExecutionTaskCount();

    /**
     * 获取当前执行中的任务.
     */
    T getInExecutionTask();

    /**
     * 更新任务状态为"执行中".
     */
    void updateTaskStatusToExecute(T task);
}