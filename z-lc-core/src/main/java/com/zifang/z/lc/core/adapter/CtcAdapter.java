package com.zifang.z.lc.core.adapter;

import com.zifang.util.http.base.define.RequestMethod;
import com.zifang.util.http.base.pojo.HttpRequestDefinition;
import com.zifang.util.http.base.pojo.HttpRequestHeader;
import com.zifang.util.http.base.pojo.HttpRequestLine;
import com.zifang.util.http.client.HttpExecutionResult;
import com.zifang.util.http.client.HttpExecutor;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonArray;
import com.zifang.util.json.model.JsonObject;
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

    /**
     * 取一个字符串列表。
     * <p>
     * <b>{@code JsonArray} 分支不是可有可无的</b>：逐格读信封时，JSON 数组解析出来是
     * {@code JsonArray}，而它<b>只实现 {@code Iterable}、不实现 {@code List}</b>。
     * 只判 {@code instanceof List} 的话，一份形状完全正确的 {@code "roles":["admin"]}
     * 会安静地变成空列表——角色判空、权限判不过，且日志上什么异常都没有。
     */
    private static List<String> asStringList(Object o) {
        if (o instanceof JsonArray) {
            List<String> out = new ArrayList<>();
            for (Object item : (JsonArray) o) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
            return out;
        }
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
     * 本仓所有出站适配器共同的读信封约定（缺陷 #63 的根因，集中写在这里）。
     * <p>
     * z-util-parser-json 1.0.14 实测（JDK 8 与 JDK 17 结果一致，三条路径全部失败）：
     * <ul>
     *   <li>{@code JsonUtil.fromJson(body, Result.class)} ⇒ {@code RuntimeException: deserializePojo failed:
     *       com.zifang.util.core.meta.Result}（{@code Result} 只有 private 构造，引擎建对象走
     *       {@code getDeclaredConstructor().newInstance()} 且没有 setAccessible）；</li>
     *   <li>{@code JsonUtil.fromJson(body, new TypeReference<Result<Map<String,Object>>>(){})} ⇒
     *       {@code ClassCastException: ParameterizedTypeImpl cannot be cast to Class}；</li>
     *   <li>{@code JsonUtil.fromJson(body, Map.class)} ⇒ {@code deserializePojo failed: java.util.Map}
     *       （接口没有无参构造）。</li>
     * </ul>
     * 唯一走得通的是 {@link JsonUtil#parseObject(String)} 逐格读——
     * {@code get("k") / getString("k") / getJsonObject("k") / getJsonArray("k")}。
     * <p>
     * 后果不是"解析失败"这么轻：这些调用点都把异常吞成 null/空列表，
     * 于是<b>远端明明答得好好的，适配器一律报"没有数据"</b>——
     * 静默降级，没有任何一行 WARN 说得清是它挂了还是我们没读出来。
     */
    static final String ENVELOPE_NOTE =
            "出站信封一律 parseObject 逐格读：这份 JSON 引擎反序列化不出 Result/泛型/Map 接口";

    /**
     * 把 {@link JsonUtil#parseObject(String)} 读出来的值递归还原成普通 JDK 类型。
     * <p>
     * <b>为什么要这一步：</b>{@link JsonObject} 与 {@link JsonArray} 都<b>不实现</b>
     * {@code java.util.Map} / {@code java.util.List}（前者只 {@code implements Iterable}）。
     * 旧写法（若它曾经能工作）交出去的是 fastjson 的 {@code JSONObject}，那是实打实的
     * {@code Map}——调用方 {@code ((Map) result).get("x")} 拿得到东西。
     * 直接把 {@code JsonObject} 递出去的话，同样的调用方当场 ClassCastException。
     * 所以这里把边界收干净：<b>适配器的出参永远是 {@code Map}/{@code List}/标量</b>，
     * JSON 库的类型不外泄。
     */
    static Object toPlainJava(Object value) {
        if (value instanceof JsonObject) {
            // JsonObject 没有 entrySet()/keySet()，只有 getAllKeyValue()（实测其返回
            // List<Map.Entry<String,Object>>）——迭代入口只有这一个。
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((JsonObject) value).getAllKeyValue()) {
                out.put(e.getKey(), toPlainJava(e.getValue()));
            }
            return out;
        }
        if (value instanceof JsonArray) {
            List<Object> out = new ArrayList<>();
            for (Object item : (JsonArray) value) {
                out.add(toPlainJava(item));
            }
            return out;
        }
        return value;
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
        return doPostJson(HttpExecutor.getDefault(), url, headers, jsonBody);
    }

    /**
     * 同一个请求，但由调用方指定执行器 —— 因为"这一次调用最多允许占住一个线程多久"只有
     * {@link HttpExecutor} 构造时给的 OkHttpClient 说得出来（z-util-http 1.0.12 实测：
     * {@code HttpRequestDefinition} 没有任何超时字段，{@code HttpClientFactory} 注释里那个
     * {@code contextParams.timeout} 在整包里只出现在注释里，没有任何一行代码读它）。
     * <p>
     * 现在只有一个调用方用它：{@link CamudaAdapter} 的写后发起。见那里的 {@link #TRANSPORT_BUDGET_NOTE}。
     */
    static HttpExecutionResult doPostJson(HttpExecutor executor, String url,
                                          Map<String, String> headers, String jsonBody) {
        Map<String, String> all = headers == null ? new HashMap<>() : new HashMap<>(headers);
        all.put("Content-Type", "application/json; charset=UTF-8");
        return doRequest(executor, "POST", url, all, jsonBody);
    }

    static HttpExecutionResult doRequest(String method, String url, Map<String, String> headers, String body) {
        return doRequest(HttpExecutor.getDefault(), method, url, headers, body);
    }

    static HttpExecutionResult doRequest(HttpExecutor executor, String method, String url,
                                         Map<String, String> headers, String body) {
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
        return (executor == null ? HttpExecutor.getDefault() : executor).execute(def);
    }

    /** 指给 {@link #doPostJson(HttpExecutor, String, Map, String)} 的存在理由，避免两处各写一遍。 */
    static final String TRANSPORT_BUDGET_NOTE =
            "一次挂死的发起会把派发池的槽位按住到共享客户端的 60s 读超时（缺陷 #61 契约层实测）";

    /**
     * 判"远端到底答没答应"。
     * <p>
     * 不能只看 {@link HttpExecutionResult#isSuccess()}：那一格只表示"这一趟没有传输层错误"，
     * 库自己在 {@code HttpExecutionResult:47} 写明了 ⇒
     * "因此 success 始终为 true，5xx 不会让 isSuccess()=false"。
     * 于是 404/500 会从这一族所有调用方脚下溜过去：{@code ping()} 直接报 UP（健康检查缺陷 #52
     * 的同一形状），读 body 的那几处则把一个网关错误页当成"远端说成功"继续解析。
     * <p>
     * 现状（如实记，别当成已经修完）：本批只把 {@link CamudaAdapter} 这一条链路接进来了。
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
        if (!httpAccepted(res)) {
            log.warn("CtcAdapter.fetchContext failed: status={} err={}", res.getStatus(), res.getError());
            return null;
        }
        try {
            // 信封必须逐格读，不能用 JsonUtil.fromJson(body, new TypeReference<Result<Map<String,Object>>>(){})：
            // 这份 JSON 引擎反序列化不出 Result 与泛型（z-util-parser-json 1.0.14 实测，见类注释
            // ENVELOPE_NOTE）。改用 parseObject + JsonObject 逐格取，CamudaAdapter 已用同一手法。
            JsonObject envelope = JsonUtil.parseObject(res.getBody());
            if (envelope == null) {
                return null;
            }
            Object data = envelope.get("data");
            if (!(data instanceof JsonObject)) {
                return null;
            }
            JsonObject d = (JsonObject) data;
            AuthContextDTO ctx = new AuthContextDTO();
            ctx.setUserId(asString(d.get("userId")));
            ctx.setUserName(asString(d.get("username")));
            ctx.setTenantCode(asString(d.get("tenantCode")));
            ctx.setRoles(asStringList(d.get("roles")));
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
            return httpAccepted(res);
        } catch (Exception ex) {
            log.warn("CtcAdapter.ping failed: {}", ex.getMessage());
            return false;
        }
    }
}
