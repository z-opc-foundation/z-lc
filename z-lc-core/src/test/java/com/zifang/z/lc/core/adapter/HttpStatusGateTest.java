package com.zifang.z.lc.core.adapter;

import com.zifang.util.http.client.HttpExecutionResult;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link CtcAdapter#httpAccepted(HttpExecutionResult)} 这一道状态闸自己的测试（缺陷 #62）.
 * <p>
 * <b>这一类先钉"闸门本身有效"，再用它去判别十个调用点。</b>
 * 如果不钉这一条，下面十个 adapter 的负向用例就只是"若干个碰巧绿的断言"——
 * 万一哪天 z-util-http 升级后把 5xx 也算成 {@code success=false}，
 * 那十个用例会集体变绿，而它们要防的缺陷压根不在了（闸门烂掉而不是缺陷被修掉）。
 *
 * <h3>前提的来源（实测，不是推测）</h3>
 * z-util-http 1.0.18 源码两处：
 * <ul>
 *   <li>{@code HttpExecutionResult.ok()} 里写死 {@code r.success = true}，注释自陈
 *       "success 始终为 true，5xx 不会让 isSuccess()=false"；</li>
 *   <li>{@code HttpExecutor.doCall()} 对任何 response 无条件调 {@code ok(res.code(), ...)}
 *       ——OkHttp 不对 4xx/5xx 抛异常，所以只有 IOException 才走 {@code fail()}。</li>
 * </ul>
 * 合起来就是：<b>凡是拿到了响应，{@code isSuccess()} 就为 true，与状态码无关</b>。
 * {@link #libraryMarksEveryResponseSuccessRegardlessOfStatus()} 用一次真实往返把这句话钉住。
 */
public class HttpStatusGateTest {

    @After
    public void cleanup() {
        JwtRelayInterceptor.clear();
    }

    /**
     * 判据自证：对同一个真实 500 响应，{@code isSuccess()} 说是成功、状态闸说是拒绝。
     * <p>
     * 这条红了说明<b>前提变了</b>（库升级了），此时下面十个 adapter 的负向用例不再有意义，
     * 要重新评估——不是把断言改绿了事。
     */
    @Test
    public void libraryMarksEveryResponseSuccessRegardlessOfStatus() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(new CamudaStubServer.Script()
                .status(500)
                .body("{\"success\":true,\"code\":200,\"data\":{\"decryptValue\":\"s3cr3t\"}}"))) {
            HttpExecutionResult res = CtcAdapter.doGet(stub.baseUrl() + "/whatever",
                    Collections.<String, String>emptyMap());

            assertEquals("先确认这一趟真的打到了 500", 500, res.getStatus());
            assertTrue("z-util-http 拿到响应就给 success=true，与状态码无关（这条是缺陷 #62 的前提）",
                    res.isSuccess());
            assertFalse("因此状态闸不能省：同一个响应上闸门必须拒绝", CtcAdapter.httpAccepted(res));
        }
    }

    /** 传输层失败（连不上）走的是 {@code fail()}，status 落在 0——闸门照样拒绝，两条路都堵。 */
    @Test
    public void transportFailureIsRejected() throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().closeWithoutResponse())) {
            HttpExecutionResult res = CtcAdapter.doGet(stub.baseUrl() + "/whatever",
                    Collections.<String, String>emptyMap());

            assertFalse("连不上就不能算受理", res.isSuccess());
            assertFalse(CtcAdapter.httpAccepted(res));
        }
    }

    @Test
    public void nullResultIsRejected() {
        assertFalse("null 不能再解引用炸掉", CtcAdapter.httpAccepted(null));
    }

    /** 2xx 整段放行——200/201/204/299 都是"远端受理了"。 */
    @Test
    public void every2xxIsAccepted() throws Exception {
        assertTrue("200", accepted(200, "{}"));
        assertTrue("201", accepted(201, "{}"));
        assertTrue("202", accepted(202, "{}"));
        assertTrue("204 是 2xx 段内的合法应答（无内容也算受理）", accepted(204, ""));
        assertTrue("299 是 2xx 段的上界内", accepted(299, "{}"));
    }

    /**
     * 3xx/4xx/5xx 全部拒绝。3xx 特意在列：OkHttp 默认跟随重定向，跟不到时原样回一个 3xx，
     * 把它算成"受理"没有道理。
     */
    @Test
    public void non2xxIsRejected() throws Exception {
        assertFalse("301 重定向没被跟到底", accepted(301, "{}"));
        assertFalse("302", accepted(302, "{}"));
        assertFalse("304 缓存命中也不是本次应答", accepted(304, ""));
        assertFalse("400", accepted(400, "{}"));
        assertFalse("401 未认证", accepted(401, "{}"));
        assertFalse("403", accepted(403, "{}"));
        assertFalse("404", accepted(404, "{}"));
        assertFalse("500", accepted(500, "{}"));
        assertFalse("502 网关错误", accepted(502, "{}"));
        assertFalse("503", accepted(503, "{}"));
    }

    /**
     * 跑一次真实往返，问闸门放不放行。
     * <p>
     * <b>body 参数不能省</b>：204/205 按 HTTP 规范不得带 body，而 OkHttp 见到
     * {@code "HTTP 204 had non-zero Content-Length: 2"} 会把整个响应判为非法
     * （实测 {@code isSuccess()=false}、{@code status=0}、{@code err=Execute failed: ...}）。
     * 那样红的不是闸门，是桩服务发了畸形报文——所以这两个码必须用空 body。
     */
    private static boolean accepted(final int status, final String body) throws Exception {
        try (CamudaStubServer stub = new CamudaStubServer(
                new CamudaStubServer.Script().status(status).body(body))) {
            HttpExecutionResult res = CtcAdapter.doGet(stub.baseUrl() + "/whatever",
                    Collections.<String, String>emptyMap());
            assertEquals("桩服务要真的回出 " + status + "（err=" + res.getError() + "）", status, res.getStatus());
            return CtcAdapter.httpAccepted(res);
        }
    }
}
