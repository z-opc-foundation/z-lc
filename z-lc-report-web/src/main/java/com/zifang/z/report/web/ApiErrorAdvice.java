package com.zifang.z.report.web;

import com.zifang.util.expr.obj.ObjException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一错误出口: 契约校验/资源缺失/整形程序写错 → 400, 其余 → 500。
 */
@RestControllerAdvice
public class ApiErrorAdvice {

    // ObjException 是调用方传来的 shape 程序不合法, 属于请求问题, 不该记成服务端错误
    @ExceptionHandler({IllegalArgumentException.class, ObjException.class})
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({IllegalStateException.class, UnsupportedOperationException.class})
    public ResponseEntity<Map<String, Object>> conflict(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> serverError(Exception e) {
        return body(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("ok", false);
        resp.put("status", status.value());
        resp.put("error", message);
        return ResponseEntity.status(status).body(resp);
    }
}
