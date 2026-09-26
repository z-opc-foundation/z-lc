package com.zifang.z.lc.core.adapter;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonObject;
import org.junit.Test;

/** 一次性探针：这份 JSON 引擎到底能不能把 Result 信封读出来（跑完即删）。 */
public class JsonEnvelopeProbeTest {

    private static final String OK = "{\"success\":true,\"code\":200,\"message\":null,"
            + "\"data\":{\"processInstanceId\":\"42\",\"businessKey\":\"b\"}}";

    private void probe(String label, java.util.concurrent.Callable<Object> c) {
        try {
            System.out.println("PROBE " + label + " => " + c.call());
        } catch (Throwable t) {
            Throwable root = t;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            System.out.println("PROBE " + label + " => THROW " + t + " root=" + root);
        }
    }

    @Test
    public void probe() {
        probe("parseObject", () -> JsonUtil.parseObject(OK));
        probe("getBoolean(success)", () -> JsonUtil.parseObject(OK).getBoolean("success"));
        probe("getJsonObject(data)", () -> JsonUtil.parseObject(OK).getJsonObject("data"));
        probe("data.processInstanceId", () ->
                JsonUtil.parseObject(OK).getJsonObject("data").getString("processInstanceId"));
        probe("missing key getString", () ->
                JsonUtil.parseObject(OK).getJsonObject("nope"));
        probe("getString of absent", () ->
                JsonUtil.parseObject(OK).getString("absent"));
        probe("data as string literal", () ->
                JsonUtil.parseObject("{\"success\":true,\"data\":\"42\"}").getJsonObject("data"));
        probe("garbage", () -> JsonUtil.parseObject("not json at all"));
        probe("empty", () -> JsonUtil.parseObject(""));
        probe("Result.class", () -> JsonUtil.fromJson(OK, com.zifang.util.core.meta.Result.class));
    }
}
