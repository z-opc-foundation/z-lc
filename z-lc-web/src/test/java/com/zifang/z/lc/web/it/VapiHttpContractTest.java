package com.zifang.z.lc.web.it;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * V3 接口/脚本域 HTTP 契约集成测试 (lead 013 §四):
 * vApi 注册 CRUD + 通用分发全链路 (真实 z-script ScriptEngine 经 z-lc-vapi 孵化模块装配)。
 * <p>
 * 覆盖: EL/GROOVY 执行、参数透传 (query+body)、未注册 404、执行失败 500、
 * 非法 DSL/path 400、重复注册 400、停用后 404。
 */
@SpringBootTest(classes = LcTestApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VapiHttpContractTest {

    @Resource
    private MockMvc mockMvc;

    private static String json(String raw) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : raw.toCharArray()) {
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    private static String vapiBody(String code, String method, String path, String dsl, String source) {
        return "{\"appCode\":\"demo\",\"vapiCode\":\"" + code + "\",\"method\":\"" + method
                + "\",\"path\":\"" + path + "\",\"dslType\":\"" + dsl + "\","
                + "\"sourceCode\":" + json(source) + "}";
    }

    @Test
    @Order(1)
    void create_update_flow() throws Exception {
        // EL: 拼接参数
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("hello-el", "POST", "order/greet", "EL",
                                "'Hello, ' + #name + '!'")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.path").value("order/greet"))
                .andExpect(jsonPath("$.data.dslType").value("EL"));

        // GROOVY: 沙箱执行算术
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("sum-groovy", "GET", "math/sum", "GROOVY",
                                "return (a as int) + (b as int);")))
                .andExpect(status().isOk());

        // 重复注册 (同 method+path) → 400
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("dup", "POST", "order/greet", "EL", "1")))
                .andExpect(status().isBadRequest());

        // 非法 DSL → 400
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("bad-dsl", "POST", "x/y", "API_BRIDGE", "1")))
                .andExpect(status().isBadRequest());

        // 非法 path 字符 → 400
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("bad-path", "POST", "x/y?z", "EL", "1")))
                .andExpect(status().isBadRequest());

        // 更新: 停用 math/sum
        mockMvc.perform(get("/api/lc/vapi/list?appCode=demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @Order(2)
    void dispatch_el_with_params() throws Exception {
        // body 参数进脚本
        mockMvc.perform(post("/api/lc/vapi/invoke/demo/order/greet")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"CEO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").value("Hello, CEO!"))
                .andExpect(jsonPath("$.data.dslType").value("EL"));

        // query 参数同样透传
        mockMvc.perform(post("/api/lc/vapi/invoke/demo/order/greet?name=OPC")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").value("Hello, OPC!"));
    }

    @Test
    @Order(3)
    void dispatch_groovy_query_params() throws Exception {
        mockMvc.perform(get("/api/lc/vapi/invoke/demo/math/sum?a=3&b=4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").value(7));
    }

    @Test
    @Order(4)
    void dispatch_semantics() throws Exception {
        // 未注册 → 404
        mockMvc.perform(post("/api/lc/vapi/invoke/demo/nope/path")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());

        // 执行失败 (EL 引用不存在属性抛错) → 500 带原因
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("boom", "POST", "boom/x", "EL", "#missing.deep")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/lc/vapi/invoke/demo/boom/x")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isInternalServerError());

        // 注册时路径尾斜杠归一化: /math/abs/ 等价 math/abs
        mockMvc.perform(post("/api/lc/vapi/create").contentType(MediaType.APPLICATION_JSON)
                        .content(vapiBody("abs-groovy", "GET", "/math/abs/", "GROOVY",
                                "return Math.abs(a as int);")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lc/vapi/invoke/demo/math/abs?a=-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data").value(9));
    }

    @Test
    @Order(5)
    void disable_then_404_and_delete() throws Exception {
        MvcResult list = mockMvc.perform(get("/api/lc/vapi/list?appCode=demo"))
                .andExpect(status().isOk()).andReturn();
        assertTrue(list.getResponse().getContentAsString().contains("order/greet"));

        // 停用 boom (从 list 里按 path 定位 id; DTO 序列化后 id 是字符串形态)
        MvcResult listRaw = mockMvc.perform(get("/api/lc/vapi/list?appCode=demo")).andReturn();
        String body = listRaw.getResponse().getContentAsString();
        String boomId;
        try {
            net.minidev.json.JSONArray hits = com.jayway.jsonpath.JsonPath
                    .read(body, "$.data[?(@.path=='boom/x')].id");
            boomId = String.valueOf(hits.get(0));
        } catch (IndexOutOfBoundsException e) {
            throw new AssertionError("boom/x not found in list", e);
        }
        assertTrue(boomId.matches("\\d+"), "should locate boom id from list");
        mockMvc.perform(post("/api/lc/vapi/update").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + boomId + ",\"status\":0}"))
                .andExpect(status().isOk());
        // 停用后解析不到 → 404
        mockMvc.perform(post("/api/lc/vapi/invoke/demo/boom/x")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());

        // 删除 boom 后注册表里不再可解析 (list 不含)
        mockMvc.perform(post("/api/lc/vapi/delete?id=" + boomId)).andExpect(status().isOk());
        MvcResult after = mockMvc.perform(get("/api/lc/vapi/list?appCode=demo")).andReturn();
        assertTrue(!after.getResponse().getContentAsString().contains("boom/x"));
    }
}
