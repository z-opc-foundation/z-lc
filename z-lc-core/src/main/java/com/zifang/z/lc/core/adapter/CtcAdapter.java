package com.zifang.z.lc.core.adapter;

import com.zifang.util.core.meta.Result;
import com.zifang.util.http.base.define.RequestMethod;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.base.pojo.HttpRequestHeader;
import com.zifang.util.http.base.pojo.HttpRequestLine;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.z.lc.common.dto.AuthContextDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.*;

/**
 * Ctc 适配器: 调用 z-ctc 的鉴权 API.
 * <p>
 * 设计哲学:
 * HTTP 走 z-opc 自己的 z-util-http (Library-First), 用 HttpExecutor.execute(HttpRequestDefinition) 调远端,
 * 权限缓存 1 分钟 TTL 降低 z-ctc 出口. 任何需要"换 z-ctc 实现"的改造只需替换 baseUrl,
 * 本类不感知具体实现.
 */
@Component
public class CtcAdapter implements Adapter {

    public static final String NAME = "ctc";
    private static final Logger log = LogManager.getLogger(CtcAdapter.class);

    private final com.github.benmanes.caffeine.cache.Cache<String, AuthContextDTO> ctxCache =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofMinutes(1))
                    .maximumSize(2000)
                    .build();

    @Value("${z-lc.adapter.ctc.base-url:http://localhost:8888}")
    private String baseUrl;

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static List<String> asStringList(Object o) {
        if (o instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object item : (List<?>) o) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }

            }
            return out;
        }
        return Collections.emptyList();
    }

    /**
     * GET 请求封装. 公共方法, 供 4 个 Adapter 共享.
     */
    static HttpExecutionResult doGet(String url, Map<String, String> headers) {
        return doRequest("GET", url, headers, null);
    }

    /**
     * POST JSON 请求封装. 公共方法, 供 4 个 Adapter 共享.
     */
    static HttpExecutionResult doPostJson(String url, Map<String, String> headers, String jsonBody) {
        Map<String, String> all = headers == null ? new HashMap<>() : new HashMap<>(headers);
        all.put("Content-Type", "application/json; charset=UTF-8");
        return doRequest("POST", url, all, jsonBody);
    }

    static HttpExecutionResult doRequest(String method, String url, Map<String, String> headers, String body) {
        HttpRequestLine line = new HttpRequestLine();
        line.setRequestMethod(RequestMethod.valueOf(method));
        line.setUrl(url);
        HttpRequestDefinition def = new HttpRequestDefinition();
        def.setHttpRequestLine(line);
        // 请求头对象必须由这里 new 出来：HttpRequestDefinition 是个裸 POJO，
        // getHttpRequestHeader() 在没有 set 过时返回 null（z-util-http 1.0.12 实测）。早先直接
        // def.getHttpRequestHeader().put(...) ⇒ 任何带头的调用（doPostJson 恒带 Content-Type、
        // fetchContext 恒带 Authorization）都在这一行 NPE —— 出站链路一次都没真的通过，
        // 而 ping() 把 NPE 咽成"ctc 不可达"，看起来就像对端挂了。
        HttpRequestHeader header = new HttpRequestHeader();
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    header.put(e.getKey(), e.getValue());
                }
            }
        }
        def.setHttpRequestHeader(header);
        if (body != null && !body.isEmpty()) {
            com.zifang.util.http.base.pojo.HttpRequestBody reqBody = new com.zifang.util.http.base.pojo.HttpRequestBody();
            reqBody.setBody(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            def.setHttpRequestBody(reqBody);
        }
        return HttpExecutor.getDefault().execute(def);
    }

    /**
     * 判"远端到底答没答应"。
     * <p>
     * 不能只看 {@link HttpExecutionResult#isSuccess()}：那一格只表示"这一趟没有传输层错误"，
     * 库自己在 {@code HttpExecutionResult:47} 写明了 ⇒
     * "因此 success 始终为 true，5xx 不会让 isSuccess()=false"。
     * 于是 404/500 会从这一族所有调用方脚下溜过去：{@code ping()} 直接报 UP（健康检查缺陷 #52
     * 的同一形状），读 body 的那几处则把一个网关错误页当成"远端说成功"继续解析。
     * <p>
     * 现状（如实记，别当成已经修完）：本批只把 {@link WfAdapter} 这一条链路接进来了。
     * 同一个错误形状还剩 10 处（行号会漂，重取用
     * {@code grep -n "res.isSuccess()" z-lc-core/src/main/java/com/zifang/z/lc/core/adapter/*.java}，
     * 09-26 23:1x 实测 10 处）：{@code CtcAdapter} 的 fetchContext/ping、{@code MetaAdapter} 两处、
     * {@code ScriptAdapter} 一处、{@code MistAdapter} 两处、{@code OssAdapter} 三处，
     * 各自要配自己的测试，单列为缺陷 #62（见
     * {@code _doc/003_待办事项/feature002_http_status_not_checked/TASK.md}）。
     */
    static boolean httpAccepted(HttpExecutionResult res) {
        if (res == null || !res.isSuccess()) {
            return false;
        }
        int status = res.getStatus();
        return status >= 200 && status < 300;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public int priority() {
        return 20;
    }

    @Override
    public void init() {
        log.info("CtcAdapter initialized, baseUrl={} (via z-util-http)", baseUrl);
    }

    /**
     * 拉取当前请求的鉴权上下文 (1 分钟缓存).
     */
    public AuthContextDTO currentContext(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }
        return ctxCache.get(token, k -> fetchContext(token));
    }

    private AuthContextDTO fetchContext(String token) {
        String url = baseUrl + "/api/auth/verify";
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", token);
        HttpExecutionResult res = doGet(url, headers);
        if (!res.isSuccess()) {
            log.warn("CtcAdapter.fetchContext failed: status={} err={}", res.getStatus(), res.getError());
            return null;
        }
        try {
            Result<Map<String, Object>> r = JsonUtil.fromJson(
                    res.getBody(),
                    new TypeReference<Result<Map<String, Object>>>() {
                    });
            if (r == null || r.getData() == null) {
                return null;
            }
            Map<String, Object> data = r.getData();
            AuthContextDTO ctx = new AuthContextDTO();
            Object userId = data.get("userId");
            ctx.setUserId(userId == null ? null : String.valueOf(userId));
            ctx.setUserName(asString(data.get("username")));
            ctx.setTenantCode(asString(data.get("tenantCode")));
            ctx.setRoles(asStringList(data.get("roles")));
            ctx.setPermissions(Collections.emptyList());
            return ctx;
        } catch (Exception ex) {
            log.warn("CtcAdapter.fetchContext parse error: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 简易权限检查 (本地缓存上下文, 不再二次调用).
     */
    public boolean checkAuth(HttpServletRequest request, String permission) {
        if (permission == null) {
            return true;
        }

        AuthContextDTO ctx = currentContext(request);
        if (ctx == null || ctx.getRoles() == null) {
            return false;
        }

        if (ctx.getRoles().contains("admin") || ctx.getRoles().contains("ADMIN")) {
            return true;
        }
        return ctx.getPermissions() != null && ctx.getPermissions().contains(permission);
    }

    /**
     * 健康检查 (不抛异常).
     */
    public boolean ping() {
        try {
            HttpExecutionResult res = doGet(baseUrl + "/api/app/list", JwtAwareHttpSupport.currentAuthHeaders());
            return res.isSuccess();
        } catch (Exception ex) {
            log.warn("CtcAdapter.ping failed: {}", ex.getMessage());
            return false;
        }
    }
}
