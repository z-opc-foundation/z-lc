package com.zifang.z.lc.core.vapi;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.VapiCreateReq;
import com.zifang.z.lc.common.dto.VapiDTO;
import com.zifang.z.lc.common.dto.VapiUpdateReq;
import com.zifang.z.lc.core.vapi.entity.VapiEntity;
import com.zifang.z.lc.mapper.vapi.VapiMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 虚拟接口注册表服务实现 (V3)。
 * <p>
 * 方法/路径/DSL 在写入口做归一化与白名单校验，保证注册表里的条目可被
 * 分发 Controller 确定性路由（path 无首尾斜杠、method 大写、dsl 大写）。
 */
@Service
public class VapiBizService implements VapiService {

    private static final Logger log = LogManager.getLogger(VapiBizService.class);

    private static final String DEFAULT_TENANT = "default";

    /** z-script ScriptEngine 支持的 DSL 白名单 (API_BRIDGE 属转发形态, 孵化期不放开) */
    private static final List<String> ALLOWED_DSL = java.util.Arrays
            .asList("EL", "GROOVY", "LUA", "SQL", "MOCK");
    private static final List<String> ALLOWED_METHOD = java.util.Arrays
            .asList("GET", "POST", "PUT", "DELETE");

    @Resource
    private VapiMapper vapiMapper;

    @Override
    public VapiDTO createVapi(VapiCreateReq req) {
        String appCode = required(req.getAppCode(), "appCode required");
        String method = normalizeMethod(req.getMethod());
        String path = normalizePath(req.getPath());
        String dslType = normalizeDsl(req.getDslType());
        if (req.getSourceCode() == null || req.getSourceCode().trim().isEmpty()) {
            throw new IllegalArgumentException("sourceCode required");
        }

        VapiEntity existing = vapiMapper.selectOne(new QueryWrapper<VapiEntity>()
                .eq("tenant_code", DEFAULT_TENANT)
                .eq("app_code", appCode)
                .eq("method", method)
                .eq("path", path)
                .eq("deleted", 0));
        if (existing != null) {
            throw new IllegalArgumentException("接口已存在: " + method + " /api/lc/vapi/" + appCode + "/" + path);
        }

        VapiEntity entity = new VapiEntity();
        BeanUtils.copyProperties(req, entity);
        entity.setTenantCode(DEFAULT_TENANT);
        entity.setAppCode(appCode);
        entity.setMethod(method);
        entity.setPath(path);
        entity.setDslType(dslType);
        entity.setSourceCode(req.getSourceCode());
        entity.setStatus(firstNonNull(entity.getStatus(), 1));
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        vapiMapper.insert(entity);
        log.info("vApi created: app={} {} /{}", appCode, method, path);
        return toDTO(entity);
    }

    @Override
    public VapiDTO updateVapi(VapiUpdateReq req) {
        if (req.getId() == null) {
            throw new IllegalArgumentException("id required");
        }
        VapiEntity entity = vapiMapper.selectById(req.getId());
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new IllegalArgumentException("接口不存在: id=" + req.getId());
        }
        if (req.getVapiCode() != null) {
            entity.setVapiCode(req.getVapiCode());
        }
        if (req.getMethod() != null) {
            entity.setMethod(normalizeMethod(req.getMethod()));
        }
        if (req.getPath() != null) {
            entity.setPath(normalizePath(req.getPath()));
        }
        if (req.getDslType() != null) {
            entity.setDslType(normalizeDsl(req.getDslType()));
        }
        if (req.getSourceCode() != null) {
            entity.setSourceCode(req.getSourceCode());
        }
        if (req.getParamSchema() != null) {
            entity.setParamSchema(req.getParamSchema());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }
        entity.setUpdateTime(new Date());
        vapiMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public void deleteVapi(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id required");
        }
        VapiEntity entity = vapiMapper.selectById(id);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
            throw new IllegalArgumentException("接口不存在: id=" + id);
        }
        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        vapiMapper.updateById(entity);
    }

    @Override
    public VapiDTO getVapi(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id required");
        }
        VapiEntity entity = vapiMapper.selectById(id);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
            throw new IllegalArgumentException("接口不存在: id=" + id);
        }
        return toDTO(entity);
    }

    @Override
    public List<VapiDTO> listByApp(String appCode) {
        return vapiMapper.selectList(new QueryWrapper<VapiEntity>()
                        .eq("tenant_code", DEFAULT_TENANT)
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByAsc("path"))
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public VapiDTO resolve(String appCode, String method, String path) {
        if (appCode == null || method == null || path == null) {
            return null;
        }
        VapiEntity entity = vapiMapper.selectOne(new QueryWrapper<VapiEntity>()
                .eq("tenant_code", DEFAULT_TENANT)
                .eq("app_code", appCode)
                .eq("method", method.toUpperCase(Locale.ROOT))
                .eq("path", normalizePath(path))
                .eq("deleted", 0));
        if (entity == null || entity.getStatus() == null || entity.getStatus() != 1) {
            return null;
        }
        return toDTO(entity);
    }

    private String normalizeMethod(String raw) {
        String m = required(raw, "method required").trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_METHOD.contains(m)) {
            throw new IllegalArgumentException("不支持的 HTTP 方法: " + m);
        }
        return m;
    }

    private String normalizeDsl(String raw) {
        String d = required(raw, "dslType required").trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_DSL.contains(d)) {
            throw new IllegalArgumentException("不支持的 DSL 类型: " + d + " (支持 EL/GROOVY/LUA/SQL/MOCK)");
        }
        return d;
    }

    /** 归一化: 去首尾斜杠, 压缩重复斜杠, 校验字符集 (字母数字-/_) */
    private String normalizePath(String raw) {
        String p = required(raw, "path required").trim();
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        p = p.replaceAll("/{2,}", "/");
        if (p.isEmpty()) {
            throw new IllegalArgumentException("path required");
        }
        if (!p.matches("[A-Za-z0-9_\\-/]+")) {
            throw new IllegalArgumentException("path 仅允许字母数字与 - _ /: " + p);
        }
        return p;
    }

    private String required(String v, String msg) {
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
        return v.trim();
    }

    private Integer firstNonNull(Integer a, Integer b) {
        return a != null ? a : b;
    }

    private VapiDTO toDTO(VapiEntity e) {
        VapiDTO dto = new VapiDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
