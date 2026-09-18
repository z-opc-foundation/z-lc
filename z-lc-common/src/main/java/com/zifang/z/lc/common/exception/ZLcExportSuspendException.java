package com.zifang.z.lc.common.exception;

/**
 * 导出任务挂起续导信号 — 蒸馏自 ace-platform-core
 * {@code ExportSuspendException} ({@code com.c2f.ace.core.utils.excel}).
 *
 * <p>分页重试耗尽、checkpoint 已持久化时抛出, 表示任务应重新入队以续导剩余数据.
 *
 * @author zifang
 */
public class ZLcExportSuspendException extends RuntimeException {

    public ZLcExportSuspendException(String message, Throwable cause) {
        super(message, cause);
    }
}
