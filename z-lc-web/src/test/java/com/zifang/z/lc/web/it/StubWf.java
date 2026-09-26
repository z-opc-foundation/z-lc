package com.zifang.z.lc.web.it;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 集成测试里的 z-wf 桩：把 {@code WfAdapter} 打出来的那一句原样记下来，再按脚本回话.
 * <p>
 * 为什么放在 z-lc-web 而不是复用 {@code z-lc-core} 测试树里的 {@code WfStubServer}：
 * 两个模块的测试类互相不在对方 classpath 上（没有 test-jar 依赖），而为一个小工具去改
 * pom 的构建结构不划算 —— 这一份是刻意做小的（只记请求 + 三种回法）。
 * <p>
 * 为什么自己拿 {@link ServerSocket} 而不是 JDK 的 {@code com.sun.net.httpserver.HttpServer}：
 * 后者在本仓的 JDK8 fork 里有过 {@code stop()} 卡在 {@code preClose0} 把整个 fork 挂死的先例。
 * 这里的收尾是"关掉监听 socket ⇒ accept 立刻抛 ⇒ 守护线程自己结束"，没有等不掉的第三步。
 * <p>
 * 端口由 OS 分配（{@code new ServerSocket(0)}），再通过
 * {@code @DynamicPropertySource} 交给 {@code z-lc.adapter.wf.base-url} ——
 * 固定端口会和同一台机器上别的测试/服务撞，而撞了以后的表现是"桩没收到请求"，
 * 那种红会被误读成"链没接上"。
 */
final class StubWf implements AutoCloseable {

    /** z-wf 真实映射的那条路径（{@code ApprovalCenterController} 的 {@code /api/approval-center} + {@code /processes/start}）。 */
    static final String START_PATH = "/api/approval-center/processes/start";

    static final String OK_BODY =
            "{\"success\":true,\"code\":200,\"message\":null,\"data\":{\"processInstanceId\":\"wf-stub-77\"}}";

    /** 一次被收到的请求。 */
    static final class Recorded {
        final String method;
        final String path;
        final String body;

        Recorded(String method, String path, String body) {
            this.method = method;
            this.path = path;
            this.body = body;
        }

        @Override
        public String toString() {
            return method + " " + path + " " + body;
        }
    }

    private final ServerSocket server;
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<Recorded>());
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Thread acceptor;

    private volatile int status = 200;
    private volatile String body = OK_BODY;
    private volatile long delayMs = 0L;
    private volatile boolean silence = false;

    StubWf() throws IOException {
        this.server = new ServerSocket(0);
        this.acceptor = new Thread(this::serve, "lc-stub-wf-" + server.getLocalPort());
        this.acceptor.setDaemon(true);
        this.acceptor.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getLocalPort();
    }

    void reset() {
        requests.clear();
        status = 200;
        body = OK_BODY;
        delayMs = 0L;
        silence = false;
    }

    /** 引擎说"不"（HTTP 200 但信封里 success=false，z-wf 的拒绝就是这个形状）。 */
    void rejectWith(String message) {
        this.status = 200;
        this.body = "{\"success\":false,\"code\":500,\"message\":\"" + message + "\"}";
    }

    /** 迟迟不回话：给"等待必须有上限"那一支当猎物。 */
    void delay(long ms) {
        this.delayMs = ms;
    }

    /** 连上就断：模拟引擎不可达。 */
    void silent() {
        this.silence = true;
    }

    void status(int value) {
        this.status = value;
    }

    List<Recorded> requests() {
        return new ArrayList<Recorded>(requests);
    }

    int count() {
        return requests.size();
    }

    /** 所有请求的 body 拼在一起，用于"这一格到底发出去了没有"这种整串检查。 */
    String allBodies() {
        StringBuilder sb = new StringBuilder();
        for (Recorded r : requests()) {
            sb.append(r.body).append('\n');
        }
        return sb.toString();
    }

    private void serve() {
        while (!stopped.get() && !server.isClosed()) {
            try {
                final Socket socket = server.accept();
                Thread worker = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        handle(socket);
                    }
                }, "lc-stub-wf-conn");
                worker.setDaemon(true);
                worker.start();
            } catch (IOException ex) {
                return;
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket) {
            InputStream in = s.getInputStream();
            String requestLine = readLine(in);
            if (requestLine == null) {
                return;
            }
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "?";
            String path = parts.length > 1 ? parts[1] : "?";
            int contentLength = 0;
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0 && "Content-Length".equalsIgnoreCase(line.substring(0, colon).trim())) {
                    contentLength = Integer.parseInt(line.substring(colon + 1).trim());
                }
            }
            String payload = contentLength > 0 ? readFully(in, contentLength) : "";
            requests.add(new Recorded(method, path, payload));

            if (silence) {
                return;
            }
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
            byte[] out1 = ("HTTP/1.1 " + status + " " + (status == 200 ? "OK" : "Error")
                    + "\r\nContent-Type: application/json; charset=UTF-8"
                    + "\r\nContent-Length: " + body.getBytes(StandardCharsets.UTF_8).length
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            OutputStream out = s.getOutputStream();
            out.write(out1);
            out.write(body.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException | InterruptedException ex) {
            // 桩自己不参与断言：连接被中断就是"没有回话"，由被测那方判失败。
        }
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\n') {
                break;
            }
            if (b != '\r') {
                buf.write(b);
            }
        }
        if (b == -1 && buf.size() == 0) {
            return null;
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String readFully(InputStream in, int length) throws IOException {
        byte[] buf = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(buf, read, length - read);
            if (n < 0) {
                break;
            }
            read += n;
        }
        return new String(buf, 0, read, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        stopped.set(true);
        try {
            server.close();
        } catch (IOException ignored) {
            // 已经在关了
        }
        acceptor.interrupt();
    }
}
