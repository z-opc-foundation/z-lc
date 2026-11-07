package com.zifang.z.lc.web.controller;

import com.zifang.util.core.meta.Result;
import com.zifang.z.lc.core.ai.AiModelingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * AI 对话式产品制造 API (F035 T9-T13)
 */
@Tag(name = "低代码-AI产品制造")
@RestController
@RequestMapping("/api/lc/ai")
public class AiModelingController {

    @Resource
    private AiModelingService aiModelingService;

    @Operation(summary = "AI建模建议 - 根据自然语言描述生成实体定义")
    @PostMapping("/model-suggestion")
    public Result<Map<String, Object>> generateModelSuggestion(@RequestBody Map<String, String> request) {
        String appCode = request.get("appCode");
        String description = request.get("description");
        return Result.success(aiModelingService.generateModelSuggestion(appCode, description));
    }

    @Operation(summary = "应用AI建模建议 - 将建议实际创建到系统",
            description = "把 entities / suggestedEntities 与 suggestedRelations 真正落库到 "
                    + "z_lc_entity / z_lc_field / z_lc_relation（不 provision 物理表）。"
                    + "tenantCode 取请求体，缺省 default。success 仅在全部真实落库（或幂等跳过）时为 true；"
                    + "createdXxx 只含真正创建成功的项，skippedXxx 为幂等跳过的项；"
                    + "失败返回 success=false + reason / failedItem，且不返回任何 createdXxx。")
    @PostMapping("/apply-suggestion")
    public Result<Map<String, Object>> applyModelSuggestion(@RequestBody Map<String, Object> suggestion) {
        String appCode = (String) suggestion.get("appCode");
        return Result.success(aiModelingService.applyModelSuggestion(appCode, suggestion));
    }

    @Operation(summary = "智能表单生成")
    @PostMapping("/generate-form")
    public Result<Map<String, Object>> generateFormLayout(@RequestBody Map<String, String> request) {
        return Result.success(aiModelingService.generateFormLayout(request.get("appCode"), request.get("entityCode")));
    }

    @Operation(summary = "数据洞察分析")
    @PostMapping("/analyze-data")
    public Result<Map<String, Object>> analyzeData(@RequestBody Map<String, String> request) {
        return Result.success(aiModelingService.analyzeData(request.get("appCode"), request.get("entityCode")));
    }

    @Operation(summary = "一键产品化")
    @PostMapping("/productize")
    public Result<Map<String, Object>> oneClickProductize(@RequestBody Map<String, String> request) {
        return Result.success(aiModelingService.oneClickProductize(request.get("appCode")));
    }

    @Operation(summary = "MCP Tool 定义 - 供 z-agent 注册")
    @GetMapping("/mcp-tools")
    public Result<List<Map<String, Object>>> getMcpToolDefinitions() {
        return Result.success(aiModelingService.getMcpToolDefinitions());
    }
}
