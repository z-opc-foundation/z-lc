package com.zifang.z.lc.common.utils;

import java.util.concurrent.ExecutorService;

/**
 * 数据导入导出任务执行器 — 蒸馏自 ace-platform-core
 * {@code DataImportOrExportExecutor} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>负责从逻辑队列中取出待处理任务并异步执行.
 * 使用数据库模拟队列机制, 保证串行执行: 同一时刻只有一个任务在运行.
 * 蒸馏时移除了 ace 对 Spring @Component / ThreadPoolUtil 的依赖,
 * 改为纯 JDK + 接口注入实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>大数据量导出任务的异步串行执行</li>
 *   <li>数据导入任务的队列调度</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcDataImportExportExecutor {

    private final ExecutorService executorService;

    /**
     * 构造执行器.
     *
     * @param executorService 用于异步执行任务的线程池
     */
    public ZLcDataImportExportExecutor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    /**
     * 任务出队并异步执行.
     *
     * @param process 导入导出处理接口
     */
    @SuppressWarnings("unchecked")
    public void taskQueuePop(ZLcIDataImportOrExportProcess process) {
        // 判断是否有已在处理中的任务
        Long count = process.getInExecutionTaskCount();
        if (count == 0) {
            // 获取下个要执行的任务
            Object task = process.nextTask();
            if (task != null) {
                // 更新任务状态
                process.updateTaskStatusToExecute(task);
                // 异步触发任务
                executorService.execute(() -> process.taskExecute(task));
            }
        }
    }
}
