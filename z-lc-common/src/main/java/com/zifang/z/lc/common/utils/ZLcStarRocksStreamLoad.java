package com.zifang.z.lc.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * StarRocks Stream Load 工具 — 蒸馏自 ace-platform-core
 * {@code StarRocksStreamLoad} ({@code com.c2f.ace.core.utils}).
 *
 * <p>通过 HTTP PUT 将 JSON 数据流式加载到 StarRocks 数据库.
 * 蒸馏时移除了 ace 对 hutool DateUtil / Apache HttpClient / Nacos Base64 的依赖,
 * 改为纯 JDK HttpURLConnection 实现.
 *
 * <p>典型场景：
 * <ul>
 *   <li>大数据量批量导入到 StarRocks</li>
 *   <li>实时数据同步到 StarRocks</li>
 * </ul>
 *
 * @author zifang
 */
public final class ZLcStarRocksStreamLoad {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private ZLcStarRocksStreamLoad() {
    }

    /**
     * 将 JSON 数据流式加载到 StarRocks.
     *
     * @param jsonContent JSON 格式的数据内容
     * @param config      StarRocks 连接配置
     * @return Stream Load 响应内容
     * @throws Exception 加载失败时抛出
     */
    public static String sendData(String jsonContent, Map<String, String> config) throws Exception {
        String host = config.get("host");
        int port = Integer.parseInt(config.get("port"));
        String db = config.get("db");
        String table = config.get("table");
        String user = config.get("user");
        String password = config.get("password");

        String loadUrl = String.format("http://%s:%s/api/%s/%s/_stream_load", host, port, db, table);
        String label = table + "_" + LocalDateTime.now().format(LABEL_FMT);

        HttpURLConnection conn = (HttpURLConnection) new URL(loadUrl).openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        // 设置请求头
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Expect", "100-continue");
        conn.setRequestProperty("format", "JSON");
        conn.setRequestProperty("strip_outer_array", "true");
        conn.setRequestProperty("label", label);
        conn.setRequestProperty("Authorization", "Basic " + basicAuth(user, password));

        // 写入数据
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonContent.getBytes(StandardCharsets.UTF_8));
        }

        // 读取响应
        int code = conn.getResponseCode();
        java.io.InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            return "HTTP " + code;
        }
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String basicAuth(String user, String password) {
        String auth = user + ":" + password;
        return Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}
