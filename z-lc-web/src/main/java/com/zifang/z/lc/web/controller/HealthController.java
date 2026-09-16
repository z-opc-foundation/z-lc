package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.adapter.AdapterRegistry;
import com.zifang.z.lc.core.adapter.CtcAdapter;
import com.zifang.z.lc.core.adapter.MetaAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema 健康检查 + Adapter 自检控制器.
 * <p>
 * API 基础路径: /api/lc/health
 * 所属模块: z-lc-web
 * 鉴权: 无 (健康检查端点, 运维探活使用)
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /api/lc/health         — 总体 UP 状态 + 已注册 adapter 概览</li>
 *   <li>GET /api/lc/health/adapters — 列出全部 adapter 的 name/priority/class</li>
 *   <li>GET /api/lc/health/meta-ping — 探活 z-meta 适配器</li>
 *   <li>GET /api/lc/health/ctc-ping  — 探活 z-ctc 适配器</li>
 * </ul>
 */
@Tag(name = "低代码-健康检查")
@RestController
@RequestMapping("/api/lc/health")
public class HealthController {

    @Autowired
    private AdapterRegistry adapterRegistry;

    @Autowired
    private MetaAdapter metaAdapter;

    @Autowired
    private CtcAdapter ctcAdapter;

    /**
     * 总体健康检查: 返回 UP 状态与所有已注册 adapter 的 name/priority 概览.
     *
     * @return 包含 status 与 adapters 数组的结果
     */
    @Operation(summary = "总体健康检查")
    @GetMapping("")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("adapters", adapterRegistry.all().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", a.name());
            m.put("priority", a.priority());
            return m;
        }).toArray());
        return Result.success(data);
    }

    /**
     * 列出全部已注册 adapter 的详细信息: name, priority, 完整类名.
     *
     * @return 每个 adapter 一个 Map, 字段为 name/priority/class
     */
    @Operation(summary = "列出全部适配器")
    @GetMapping("/adapters")
    public Result<List<Map<String, Object>>> adapters() {
        return Result.success(adapterRegistry.all().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", a.name());
            m.put("priority", a.priority());
            m.put("class", a.getClass().getName());
            return m;
        }).collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 探活 z-meta adapter.
     *
     * @return true 表示 z-meta 后端可达, false 表示不可达
     */
    @Operation(summary = "探活 z-meta 适配器")
    @GetMapping("/meta-ping")
    public Result<Boolean> metaPing() {
        return Result.success(metaAdapter.ping());
    }

    /**
     * 探活 z-ctc adapter.
     *
     * @return true 表示 z-ctc 后端可达, false 表示不可达
     */
    @Operation(summary = "探活 z-ctc 适配器")
    @GetMapping("/ctc-ping")
    public Result<Boolean> ctcPing() {
        return Result.success(ctcAdapter.ping());
    }
}
