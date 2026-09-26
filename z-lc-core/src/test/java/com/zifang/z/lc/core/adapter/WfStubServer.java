package com.zifang.z.lc.core.adapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 测试用的 z-wf 桩服务：把请求原样记下来，再按脚本回话.
 * <p>
 * 为什么自己拿 {@link ServerSocket} 而不复用 JDK 的 {@code com.sun.net.httpserver.HttpServer}：
 * 后者在本仓的 JDK8 fork 里有过 {@code stop()} 卡在 preClose0 把整个 fork 挂死的先例（缺陷记录里
 * 那条"@After 收尾必须有上限"就是这么来的）。这里的收尾是"关掉监听 socket ⇒ accept 立刻抛 ⇒
 * 守护线程自己结束"，不存在等不掉的第三步。
 */
public final class WfStubServer implements AutoCloseable {

    /** 一次被收到的请求（headers 的键统一小写，取值不区分大小写地断言）。 */
    public static final class Recorded {
        public final String method;
        public final String path;
        public final String body;
        public final Map<String, String> headers;

        Recorded(String method, String path, String body, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.body = body;
            this.headers = headers;
        }

        /** 这个头有没有真的上线（CtcAdapter 曾经在建请求时就把头对象 NPE 掉，一个字节都没发出去）。 */
        public String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }

    /** 怎么回这一句。 */
    public static final class Script {
        int status = 200;
        String body = "{\"success\":true,\"code\":200,\"message\":null,\"data\":{\"processInstanceId\":\"42\"}}";
        long delayMs = 0L;
        boolean closeWithoutResponse = false;

        public Script status(int value) {
            this.status = value;
            return this;
        }

        public Script body(String value) {
            this.body = value;
            return this;
        }

        /** 迟迟不回话：给"等待必须有上限"那一支断言当猎物. */
        public Script delayMs(long value) {
            this.delayMs = value;
            return this;
        }

        /** 连上就断：模拟引擎不可达/连接被重置. */
        public Script closeWithoutResponse() {
            this.closeWithoutResponse = true;
            return this;
        }
    }

    private final ServerSocket server;
    private final List<Recorded> requests = Collections.synchronizedList(new ArrayList<Recorded>());
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile Script script = new Script();
    private final Thread acceptor;

    public WfStubServer(Script script) throws IOException {
        this.script = script;
        this.server = new ServerSocket(0);
        this.acceptor = new Thread(this::serve, "wf-stub-" + server.getLocalPort());
        this.acceptor.setDaemon(true);
        this.acceptor.start();
    }

    public static WfStubServer ok() throws IOException {
        return new WfStubServer(new Script());
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getLocalPort();
    }

    public List<Recorded> requests() {
        return new ArrayList<Recorded>(requests);
    }

    public int requestCount() {
        return requests.size();
    }

    public Recorded onlyRequest() {
        List<Recorded> snapshot = requests();
        if (snapshot.size() != 1) {
            throw new AssertionError("期望桩服务正好收到 1 个请求, 实际 " + snapshot.size() + ": " + snapshot);
        }
        return snapshot.get(0);
    }

    private void serve() {
        while (!stopped.get() && !server.isClosed()) {
            try {
                final Socket socket = server.accept();
                Thread worker = new Thread(() -> handle(socket), "wf-stub-conn");
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
            Map<String, String> headers = new LinkedHashMap<String, String>();
            String line;
            while ((line = readLine(in)) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon <= 0) {
                    continue;
                }
                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                headers.put(name, value);
                if ("content-length".equals(name)) {
                    contentLength = Integer.parseInt(value);
                }
            }
            String body = contentLength > 0 ? readFully(in, contentLength) : "";
            requests.add(new Recorded(method, path, body, headers));

            if (script.closeWithoutResponse) {
                return;
            }
            if (script.delayMs > 0) {
                Thread.sleep(script.delayMs);
            }
            byte[] payload = script.body.getBytes(StandardCharsets.UTF_8);
            OutputStream out = s.getOutputStream();
            String head = "HTTP/1.1 " + script.status + " " + reason(script.status)
                    + "\r\nContent-Type: application/json; charset=UTF-8"
                    + "\r\nContent-Length: " + payload.length
                    + "\r\nConnection: close\r\n\r\n";
            out.write(head.getBytes(StandardCharsets.UTF_8));
            out.write(payload);
            out.flush();
        } catch (IOException | InterruptedException ex) {
            // 桩服务自己不参与断言：连接被中断就是"没有回话",由被测方判失败
            // try-with-resources 已经把 socket 收掉了,这里不需要再动
        }
    }

    private static String reason(int status) {
        if (status == 200) {
            return "OK";
        }
        if (status == 404) {
            return "Not Found";
        }
        if (status == 500) {
            return "Internal Server Error";
        }
        return "Status " + status;
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
    public void close() throws IOException {
        stopped.set(true);
        server.close();
        acceptor.interrupt();
    }
}
