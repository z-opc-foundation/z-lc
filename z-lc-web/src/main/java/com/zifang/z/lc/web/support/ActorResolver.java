package com.zifang.z.lc.web.support;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 解析"这次改动是谁做的", 只用于**变更归属**（谁能撤销自己的改动、历史里显示谁改的），
 * 不作为任何鉴权依据 —— z-lc 是直连模式, 鉴权统一由上游网关 z-ctc 负责.
 * <p>
 * 顺序: 显式入参 actor -&gt; {@code X-User-Code} -&gt; {@code X-Operator} -&gt; anonymous.
 * 之所以留一个 body 兜底: 网关之外还有内部调用方 (脚本 / 流水线) 会直接打 HTTP.
 */
public final class ActorResolver {

    public static final String ANONYMOUS = "anonymous";

    private ActorResolver() {
    }

    public static String resolve(String explicit) {
        if (isSet(explicit)) {
            return explicit.trim();
        }
        HttpServletRequest request = currentRequest();
        if (request != null) {
            String header = request.getHeader("X-User-Code");
            if (isSet(header)) {
                return header.trim();
            }
            header = request.getHeader("X-Operator");
            if (isSet(header)) {
                return header.trim();
            }
        }
        return ANONYMOUS;
    }

    private static boolean isSet(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static HttpServletRequest currentRequest() {
        Object attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }
}
