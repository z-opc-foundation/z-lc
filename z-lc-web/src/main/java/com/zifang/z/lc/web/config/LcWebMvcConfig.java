package com.zifang.z.lc.web.config;

import com.zifang.z.lc.core.adapter.JwtRelayInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Web MVC 配置: 在请求进入 Controller 时绑定 servlet request 到 JwtRelayInterceptor 线程局部.
 * <p>
 * 这样 Adapter 在 outbound HTTP 调用时, 拦截器能读到当前用户的 Authorization + X-Tenant-Code.
 */
@Configuration
public class LcWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                JwtRelayInterceptor.bindCurrentRequest(request);
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception ex) {
                JwtRelayInterceptor.clear();
            }
        }).addPathPatterns("/api/lc/**");
    }
}
