package com.zifang.z.lc.design.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.design.collector.LowCodeModelServiceCollector;
import com.zifang.z.lc.design.dto.ServiceListResponse;
import com.zifang.z.lc.design.model.PageTemplate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 低代码设计态最小入口.
 * <p>
 * 设计哲学:
 * 框架层不强制包含"页面模板的 CRUD"业务 (那是 Phase 2B 的事) — 本 Controller 只提供
 * 最少必要接口: 列出已注册的低代码服务 + 按 group/code 取服务.
 * <p>
 * 业务侧如需完整的 PageTemplate CRUD, 应当:
 * <ul>
 *   <li>在 z-meta 模块建 z_lc_page_template 表 + PageTemplateManager</li>
 *   <li>实现 PageTemplateProvider SPI (在 z-lc-sdk 中扩展)</li>
 *   <li>本 Controller 改为委派给 PageTemplateProvider</li>
 * </ul>
 */
@Tag(name = "z-lc 设计态")
@RestController
@RequestMapping("/api/lc/design")
public class PageTemplateController {

    @Autowired
    private LowCodeModelServiceCollector collector;

    /**
     * 列出当前已注册到 {@link LowCodeModelServiceCollector} 的全部低代码服务.
     * <p>
     * 每个服务会附带其声明的 {@code @InterfaceMapping} 注解的 group/code/name 三元组.
     * 主要用于设计态自检与运行时探查.
     *
     * @return 服务总数与每个服务的 class/group/code/name 列表
     */
    @Operation(summary = "健康检查: 列出所有已注册的低代码服务")
    @GetMapping("/services")
    public Result<ServiceListResponse> listServices() {
        java.util.List<ServiceListResponse.ServiceEntry> list = new java.util.ArrayList<>();
        for (Object bean : collector.all()) {
            String className = bean.getClass().getName();
            String group = null;
            String code = null;
            String name = null;
            for (Class<?> iface : bean.getClass().getInterfaces()) {
                com.zifang.z.lc.sdk.annotation.InterfaceMapping m =
                        iface.getAnnotation(com.zifang.z.lc.sdk.annotation.InterfaceMapping.class);
                if (m != null) {
                    group = m.group();
                    code = m.code();
                    name = m.name();
                    break;
                }
            }
            list.add(new ServiceListResponse.ServiceEntry(className, group, code, name));
        }
        return Result.success(new ServiceListResponse(collector.size(), list));
    }

    /**
     * 按 (group, code) 拾取一个已注册的低代码服务.
     * <p>
     * 出于避免内部对象泄漏, 仅返回 bean 的类名与命中状态, 不直接返回 bean 本身.
     *
     * @param group 服务的 group 标识 (来自 {@code @InterfaceMapping.group()})
     * @param code  服务的 code 标识 (来自 {@code @InterfaceMapping.code()})
     * @return 包含 status / group / code / class 的结果, 命中失败时 status = not_found
     */
    @Operation(summary = "按 (group, code) 取一个已注册的低代码服务 (返回 bean 标识, 不返回对象避免泄漏)")
    @GetMapping("/services/pick")
    public Result<Map<String, String>> pickService(@RequestParam String group,
                                                   @RequestParam String code) {
        Object bean = collector.pickByGroupAndCode(group, code);
        Map<String, String> out = new HashMap<>();
        if (bean == null) {
            out.put("status", "not_found");
            out.put("group", group);
            out.put("code", code);
            return Result.success(out);
        }
        out.put("status", "found");
        out.put("group", group);
        out.put("code", code);
        out.put("class", bean.getClass().getName());
        return Result.success(out);
    }

    /**
     * 演示接口: 基于 appCode 与 modelCode 构造一个空白的 {@link PageTemplate} 模板.
     * <p>
     * 前端可基于此模板渲染设计器画布, 页面类型默认为 form, 状态默认为 0, 视图 JSON 为空组件数组.
     *
     * @param appCode   应用编码
     * @param modelCode 模型编码
     * @return 初始化后的空白 PageTemplate
     */
    @Operation(summary = "演示: 返回一个 PageTemplate 空模板 (前端可基于此渲染设计器)")
    @PostMapping("/template/sample")
    public Result<PageTemplate> sampleTemplate(@RequestParam String appCode,
                                               @RequestParam String modelCode) {
        PageTemplate t = new PageTemplate();
        t.setAppCode(appCode);
        t.setModelCode(modelCode);
        t.setPageType("form");
        t.setPageName("未命名页面");
        t.setStatus(0);
        t.setViewJson("{\\\"components\\\":[]}");
        return Result.success(t);
    }
}
