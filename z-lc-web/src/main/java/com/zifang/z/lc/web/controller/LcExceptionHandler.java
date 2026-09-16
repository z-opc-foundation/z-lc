package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.event.EventConflictException;
import com.zifang.z.lc.core.pipeline.Pipeline.PipelineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * z-lc 控制器全局异常处理:
 * <ul>
 *   <li>EventConflictException → 409 (设计: 不允许冲突)</li>
 *   <li>PipelineException / IllegalArgumentException → 400 (字段校验失败)</li>
 *   <li>其它 RuntimeException → 500 (兜底)</li>
 * </ul>
 */
@RestControllerAdvice(basePackages = "com.zifang.z.lc.web")
public class LcExceptionHandler {

    private static final Logger log = LogManager.getLogger(LcExceptionHandler.class);

    @ExceptionHandler(EventConflictException.class)
    public ResponseEntity<Result<Object>> handleConflict(EventConflictException ex) {
        log.warn("EventConflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Result.fail(ex.getMessage()).code(409));
    }

    @ExceptionHandler(PipelineException.class)
    public ResponseEntity<Result<Object>> handlePipeline(PipelineException ex) {
        log.warn("Pipeline[{}]: {}", ex.getProcessorName(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(ex.getMessage()).code(400));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Object>> handleBadArg(IllegalArgumentException ex) {
        log.warn("BadArg: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(ex.getMessage()).code(400));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Object>> handleAny(RuntimeException ex) {
        log.error("Unhandled runtime exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(ex.getMessage() == null ? "internal error" : ex.getMessage()).code(500));
    }
}
