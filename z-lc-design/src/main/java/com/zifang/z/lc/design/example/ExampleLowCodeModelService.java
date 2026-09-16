package com.zifang.z.lc.design.example;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.design.annotation.LowCodeModelService;
import com.zifang.z.lc.sdk.context.ExtensionServiceContextHolder;
import com.zifang.z.lc.sdk.dto.ExtensionServiceContext;
import com.zifang.z.lc.sdk.spi.form.FormDataInitService;
import com.zifang.z.lc.sdk.spi.form.FormDataSubmitPreHandlerService;
import com.zifang.z.lc.sdk.spi.form.FormDataValidateService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * z-lc 框架演示: 用户如何实现 SPI 并注册为低代码服务.
 * <p>
 * 演示三件事:
 * <ol>
 *   <li>实现 z-lc-sdk 的 3 个表单生命周期 SPI</li>
 *   <li>用 @InterfaceMapping (来自 z-lc-sdk) 标 SPI 接口语义</li>
 *   <li>用 @LowCodeModelService (来自 z-lc-design) 标 Bean 为低代码服务 (启动时 Collector 扫描)</li>
 * </ol>
 * <p>
 * 用法: 业务方把这三个类拷到自己的 z-opc 业务模块, 改成自己的业务逻辑, 即可接入 z-lc 引擎.
 */
public class ExampleLowCodeModelService {

    /**
     * 演示 4: ThreadLocal 上下文用法.
     */
    public static void threadLocalUsageDemo() {
        ExtensionServiceContext ctx = ExtensionServiceContext.builder()
                .appCode("crm")
                .modelCode("customer")
                .pageCode("customer-list")
                .build();
        ExtensionServiceContextHolder.set(ctx);
        try {
            // 业务代码 / SPI 内部可以无侵入地拿到上下文
            String appCode = ExtensionServiceContextHolder.currentAppCode();
            String modelCode = ExtensionServiceContextHolder.currentModelCode();
            // ...
        } finally {
            ExtensionServiceContextHolder.clear();
        }
    }

    /**
     * 演示 1: 表单初始化 — 引擎发起表单时回调.
     */
    @Component
    @LowCodeModelService
    public static class ExampleFormDataInitService implements FormDataInitService {
        private static final Logger log = LogManager.getLogger(ExampleFormDataInitService.class);

        @Override
        public Result<Map<String, Object>> init(ExtensionServiceContext context, Map<String, Object> data) {
            log.info("[Example] FormDataInitService.init 触发: appCode={}, modelCode={}",
                    context.getAppCode(), context.getModelCode());
            // 演示: 给 data 注入默认值
            if (data == null) data = new HashMap<>();
            data.put("createdBy", "z-lc-demo");
            data.put("createdAt", System.currentTimeMillis());
            return Result.success(data);
        }
    }

    /**
     * 演示 2: 表单校验 — 引擎提交表单前回调.
     */
    @Component
    @LowCodeModelService
    public static class ExampleFormDataValidateService implements FormDataValidateService {
        private static final Logger log = LogManager.getLogger(ExampleFormDataValidateService.class);

        @Override
        public Result<Boolean> validate(ExtensionServiceContext context, Map<String, Object> data) {
            log.info("[Example] FormDataValidateService.validate 触发: data keys={}", data == null ? 0 : data.size());
            // 演示: name 字段必填
            if (data == null || data.get("name") == null || data.get("name").toString().isEmpty()) {
                return Result.<Boolean>fail("name 字段必填 (来自 ExampleFormDataValidateService)").code(400);
            }
            return Result.success(true);
        }
    }

    /**
     * 演示 3: 表单提交前置 — 引擎提交表单后, 落库前回调.
     */
    @Component
    @LowCodeModelService
    public static class ExampleFormDataSubmitPreHandlerService implements FormDataSubmitPreHandlerService {
        private static final Logger log = LogManager.getLogger(ExampleFormDataSubmitPreHandlerService.class);

        @Override
        public Result<Map<String, Object>> preHandler(ExtensionServiceContext context, Map<String, Object> data) {
            log.info("[Example] FormDataSubmitPreHandlerService.preHandler 触发");
            // 演示: 自动盖时间戳
            if (data == null) data = new HashMap<>();
            data.put("submittedAt", System.currentTimeMillis());
            return Result.success(data);
        }
    }
}
