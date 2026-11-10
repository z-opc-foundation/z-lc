package com.zifang.z.lc.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * z-lc 响应字段命名固定为 lowerCamelCase, 不随宿主的全局 Jackson 命名策略漂移.
 * <p>
 * 背景: 独立部署时 z-lc 走 Boot 默认 camelCase 契约 —— 前端组件与后端 IT 用例都按它写
 * ({@code appCode} / {@code appName} / {@code entityCount}). z-lc-web 内嵌进 all-in-one 主壳后,
 * z-team-core 的 JacksonConfig 把全局 ObjectMapper 设成 SNAKE_CASE ⇒ {@code /api/lc/**} 响应
 * 静默变成 {@code app_code} / {@code app_name} / {@code entity_count}, 前端按 camelCase 读 ⇒
 * 字段全空: 应用列表卡片显示「实体 — 字段 —」, 卡片要 appCode 去查实体清单时 appCode 是
 * undefined, 请求里就没有 appCode ⇒ 后端 400「缺少参数: appCode」.
 * <p>
 * 做法: 只对 lc 包内控制器 (basePackages=com.zifang.z.lc), 用宿主 ObjectMapper 的拷贝
 * 把响应体先序列化成树 (字段名此时已按 camelCase 定死), 再交给原转换器写出 ——
 * 宿主的 snake_case 命名策略在写出树时不再参与. 独立部署时 camelCase → camelCase 为恒等变换.
 *
 * @see LcRequestBodyAdvice 入参侧同款适配 (宿主 snake_case 下 camelCase 请求体同样解析不出来)
 */
@ControllerAdvice(basePackages = "com.zifang.z.lc")
public class LcResponseNamingAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper camelCaseMapper;

    public LcResponseNamingAdvice(ObjectMapper objectMapper) {
        this.camelCaseMapper = objectMapper.copy()
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return AbstractJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || selectedContentType == null
                || !MediaType.APPLICATION_JSON.includes(selectedContentType)) {
            return body;
        }
        return camelCaseMapper.valueToTree(body);
    }
}
