package com.zifang.z.lc.core.materialize.service;

import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import com.zifang.z.lc.core.materialize.dto.MaterializationReq;
import com.zifang.z.lc.core.materialize.dto.MaterializationResp;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import com.zifang.z.lc.core.materialize.mapper.MaterializationMapper;
import com.zifang.z.lc.core.materialize.template.CodeTemplateEngine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.zifang.util.core.time.DateUtil;

/**
 * 代码物化服务 (FEATURE006 T1).
 * <p>
 * 核心流程:
 * 1. 创建 materialization 记录 (PENDING)
 * 2. 异步生成代码 + 写文件
 * 3. 写 z_lc_event 事件 (event_type=MATERIALIZE)
 * 4. 更新记录为 READY 或 FAILED
 */
@Service
public class MaterializationService {

    private static final Logger log = LogManager.getLogger(MaterializationService.class);

    @Autowired
    private MaterializationMapper materializationMapper;
    @Autowired
    private CodeTemplateEngine templateEngine;
    @Autowired(required = false)
    private MaterializationEventService eventService;

    /**
     * 是否允许写入用户指定的绝对路径, 默认 false (沙箱安全)
     */
    @Value("${z-lc.materialize.allow-absolute-path:false}")
    private boolean allowAbsolutePath;

    /**
     * 默认根目录, 用于相对路径解析
     */
    @Value("${z-lc.materialize.default-root:${user.home}/z-lc-materialized}")
    private String defaultRoot;
    @Autowired(required = false)
    private com.zifang.z.lc.mapper.executor.EntityMapper entityMapper;
    @Autowired(required = false)
    private com.zifang.z.lc.mapper.executor.FieldMapper fieldMapper;

    /**
     * 同步触发 (返回实体, 包含 PENDING 状态)
     */
    public MaterializationResp trigger(MaterializationReq req, String tenantCode) {
        if (req == null || req.getAppCode() == null || req.getAppCode().isEmpty()) {
            throw new IllegalArgumentException("appCode is required");
        }
        if (req.getMaterializationPath() == null || req.getMaterializationPath().isEmpty()) {
            throw new IllegalArgumentException("materializationPath is required");
        }

        String exportVersion = DateUtil.format(new Date(), "yyyyMMddHHmmss")
                + "-" + String.format("%03d", (int) (Math.random() * 1000));
        String source = (req.getTriggerSource() == null || req.getTriggerSource().isEmpty())
                ? MaterializationEntity.SOURCE_USER : req.getTriggerSource();

        MaterializationEntity entity = new MaterializationEntity();
        entity.setTenantCode(tenantCode == null ? "default" : tenantCode);
        entity.setAppCode(req.getAppCode());
        entity.setMaterializationPath(req.getMaterializationPath());
        entity.setEntityCodes(req.getEntityCodes() == null ? null
                : new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(req.getEntityCodes()).toString());
        entity.setExportVersion(exportVersion);
        entity.setStatus(MaterializationEntity.STATUS_PENDING);
        entity.setFileCount(0);
        entity.setDescription(req.getDescription());
        entity.setTriggerSource(source);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        materializationMapper.insert(entity);

        // 异步执行
        runAsync(entity.getId());

        return toResp(entity, null);
    }

    @Async
    public CompletableFuture<Void> runAsync(Long materializationId) {
        try {
            run(materializationId);
        } catch (Exception ex) {
            log.error("Materialization {} failed", materializationId, ex);
            try {
                MaterializationEntity e = materializationMapper.selectById(materializationId);
                if (e != null) {
                    e.setStatus(MaterializationEntity.STATUS_FAILED);
                    e.setErrorMessage(ex.getMessage());
                    e.setUpdateTime(new Date());
                    materializationMapper.updateById(e);
                }
            } catch (Exception ignore) {
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private void run(Long materializationId) {
        MaterializationEntity entity = materializationMapper.selectById(materializationId);
        if (entity == null) return;
        entity.setStatus(MaterializationEntity.STATUS_GENERATING);
        entity.setUpdateTime(new Date());
        materializationMapper.updateById(entity);

        // 解析目标路径
        Path basePath = resolvePath(entity.getMaterializationPath());
        ensureDir(basePath);

        // 解析 entity 列表
        List<String> entityCodes = parseEntityCodes(entity.getEntityCodes());

        // 1. 拉取 entity 列表 (z_lc_entity)
        List<EntityEntity> entities = loadEntities(entity.getTenantCode(), entity.getAppCode(), entityCodes);
        if (entities.isEmpty()) {
            throw new IllegalStateException("No entities found for app=" + entity.getAppCode()
                    + (entityCodes != null ? " codes=" + entityCodes : ""));
        }

        // 2. 逐 entity 生成
        List<MaterializationResp.GeneratedFile> allFiles = new ArrayList<>();
        for (EntityEntity e : entities) {
            List<FieldEntity> fields = loadFields(entity.getTenantCode(), e.getId());
            allFiles.addAll(generateForEntity(e, fields, basePath));
        }

        // 3. 写文件
        for (MaterializationResp.GeneratedFile gf : allFiles) {
            Path target = basePath.resolve(gf.getRelativePath());
            try {
                ensureDir(target.getParent());
                Files.write(target, gf.getContent().getBytes(StandardCharsets.UTF_8));
            } catch (IOException io) {
                throw new RuntimeException("Failed to write " + target, io);
            }
        }

        // 4. 写 z_lc_event
        String eventId = null;
        if (eventService != null) {
            try {
                eventId = eventService.recordMaterializeEvent(entity, allFiles.size());
            } catch (Exception ex) {
                log.warn("Failed to record MATERIALIZE event: {}", ex.getMessage());
            }
        }

        // 5. 更新状态
        entity.setFileCount(allFiles.size());
        entity.setStatus(MaterializationEntity.STATUS_READY);
        entity.setEventId(eventId);
        entity.setUpdateTime(new Date());
        materializationMapper.updateById(entity);

        log.info("Materialization {} completed: {} files written to {}",
                materializationId, allFiles.size(), basePath);
    }

    private List<MaterializationResp.GeneratedFile> generateForEntity(
            EntityEntity entity, List<FieldEntity> fields, Path basePath) {
        List<MaterializationResp.GeneratedFile> files = new ArrayList<>();
        String packagePath = "com/zifang/generated/" + entity.getAppCode();

        // Java
        files.add(new MaterializationResp.GeneratedFile(
                entity.getEntityCode(), "Entity",
                "backend/src/main/java/" + packagePath + "/entity/"
                        + CodeTemplateEngine.toPascalCase(entity.getEntityCode()) + ".java",
                templateEngine.renderEntity(entity, fields)));
        files.add(new MaterializationResp.GeneratedFile(
                entity.getEntityCode(), "Mapper",
                "backend/src/main/java/" + packagePath + "/mapper/"
                        + CodeTemplateEngine.toPascalCase(entity.getEntityCode()) + "Mapper.java",
                templateEngine.renderMapper(entity)));
        files.add(new MaterializationResp.GeneratedFile(
                entity.getEntityCode(), "Service",
                "backend/src/main/java/" + packagePath + "/service/"
                        + CodeTemplateEngine.toPascalCase(entity.getEntityCode()) + "Service.java",
                templateEngine.renderService(entity)));
        files.add(new MaterializationResp.GeneratedFile(
                entity.getEntityCode(), "ServiceImpl",
                "backend/src/main/java/" + packagePath + "/service/impl/"
                        + CodeTemplateEngine.toPascalCase(entity.getEntityCode()) + "ServiceImpl.java",
                templateEngine.renderServiceImpl(entity)));
        files.add(new MaterializationResp.GeneratedFile(
                entity.getEntityCode(), "Controller",
                "backend/src/main/java/" + packagePath + "/controller/"
                        + CodeTemplateEngine.toPascalCase(entity.getEntityCode()) + "Controller.java",
                templateEngine.renderController(entity)));

        // React
        files.add(new MaterializationResp.GeneratedFile(
                entity.getEntityCode(), "ReactPage",
                "frontend/src/pages/" + entity.getAppCode() + "/"
                        + CodeTemplateEngine.toPascalCase(entity.getEntityCode()) + "Page.jsx",
                templateEngine.renderReactPage(entity, fields)));

        return files;
    }

    /**
     * 路径解析: 仅允许沙箱默认根 + 相对路径; 绝对路径需配置开关
     */
    private Path resolvePath(String input) {
        if (Paths.get(input).isAbsolute()) {
            if (!allowAbsolutePath) {
                throw new SecurityException("Absolute path not allowed: " + input
                        + " (set z-lc.materialize.allow-absolute-path=true to enable)");
            }
            return Paths.get(input);
        }
        return Paths.get(defaultRoot, input);
    }

    private void ensureDir(Path p) {
        if (p == null) return;
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create dir: " + p, e);
        }
    }

    private List<String> parseEntityCodes(String json) {
        if (json == null || json.isEmpty() || "null".equals(json)) return null;
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            log.warn("Failed to parse entity_codes: {}", e.getMessage());
            return null;
        }
    }

    private List<EntityEntity> loadEntities(String tenantCode, String appCode, List<String> codes) {
        // 通过 EntityMapper 拉取; 避免直接 SQL, 走标准 MyBatis-Plus
        // 调用方: z-lc-core 内部, 通过 Spring 注入
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<EntityEntity> qw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("tenant_code", tenantCode);
            qw.eq("app_code", appCode);
            qw.eq("deleted", 0);
            if (codes != null && !codes.isEmpty()) qw.in("entity_code", codes);
            return entityMapper.selectList(qw);
        } catch (Exception e) {
            log.warn("loadEntities via mapper failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<FieldEntity> loadFields(String tenantCode, Long entityId) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<FieldEntity> qw =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            qw.eq("tenant_code", tenantCode);
            qw.eq("entity_id", entityId);
            qw.eq("deleted", 0);
            qw.orderByAsc("id");
            return fieldMapper.selectList(qw);
        } catch (Exception e) {
            log.warn("loadFields via mapper failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public MaterializationResp getStatus(Long id) {
        MaterializationEntity e = materializationMapper.selectById(id);
        if (e == null) return null;
        return toResp(e, null);
    }

    public List<MaterializationResp> listByApp(String tenantCode, String appCode, int limit) {
        return materializationMapper.listByApp(tenantCode, appCode, Math.max(1, Math.min(limit, 100)))
                .stream().map(e -> toResp(e, null)).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 列出某次物化实际写出的文件 (扫描文件系统, 配合 export_version 子目录)
     */
    public List<MaterializationResp.GeneratedFile> listFiles(Long id) {
        MaterializationEntity e = materializationMapper.selectById(id);
        if (e == null) return new ArrayList<>();
        Path basePath = resolvePath(e.getMaterializationPath());
        File base = basePath.toFile();
        if (!base.exists() || !base.isDirectory()) return new ArrayList<>();
        List<MaterializationResp.GeneratedFile> result = new ArrayList<>();
        walkFiles(base, base, result);
        return result;
    }

    private void walkFiles(File root, File current, List<MaterializationResp.GeneratedFile> out) {
        File[] children = current.listFiles();
        if (children == null) return;
        for (File f : children) {
            if (f.isDirectory()) {
                walkFiles(root, f, out);
            } else {
                try {
                    String rel = root.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/');
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    out.add(new MaterializationResp.GeneratedFile(null, "File", rel, content));
                } catch (IOException ignore) {
                }
            }
        }
    }

    private MaterializationResp toResp(MaterializationEntity e, List<MaterializationResp.GeneratedFile> files) {
        MaterializationResp r = new MaterializationResp();
        r.setId(e.getId());
        r.setAppCode(e.getAppCode());
        r.setMaterializationPath(e.getMaterializationPath());
        r.setExportVersion(e.getExportVersion());
        r.setStatus(e.getStatus());
        r.setFileCount(e.getFileCount());
        r.setDescription(e.getDescription());
        r.setErrorMessage(e.getErrorMessage());
        r.setTriggerSource(e.getTriggerSource());
        r.setCreateTime(e.getCreateTime() == null ? null
                : DateUtil.format(e.getCreateTime(), "yyyy-MM-dd HH:mm:ss"));
        r.setUpdateTime(e.getUpdateTime() == null ? null
                : DateUtil.format(e.getUpdateTime(), "yyyy-MM-dd HH:mm:ss"));
        if (files != null) r.setFiles(files);
        return r;
    }
}
