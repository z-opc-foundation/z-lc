package com.zifang.z.lc.core.schema;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.json.JsonMapperFactory;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.core.event.EventService;
import com.zifang.z.lc.core.executor.DynamicSqlBuilder;
import com.zifang.z.lc.core.executor.entity.AppEntity;
import com.zifang.z.lc.core.executor.entity.EntityEntity;
import com.zifang.z.lc.core.executor.entity.FieldEntity;
import com.zifang.z.lc.mapper.executor.EntityMapper;
import com.zifang.z.lc.mapper.executor.FieldMapper;
import com.zifang.z.lc.mapper.executor.LcAppEntityMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Schema 管理服务实现: App / Entity / Field CRUD + DDL 建表
 */
@Service
public class SchemaAdminBizService implements SchemaAdminService {

    private static final Logger log = LogManager.getLogger(SchemaAdminBizService.class);

    /** 列名规则: 字母开头, 仅字母/数字/下划线 —— 要原样进 DDL 的反引号里. */
    public static final Pattern FIELD_CODE_RE = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*$");

    /**
     * 每张受管表都由引擎自带这几列 (见 buildCreateTableDdl). 字段编码撞上它们不是"名字不优雅",
     * 而是建表直接 Duplicate column name —— 更要紧的是撞名的列名全都是合法标识符, 前端的格式校验放不住.
     * 小写比较: MySQL 列名不区分大小写, `ID` 一样撞 `id`.
     */
    public static final Set<String> SYSTEM_COLUMN_CODES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("id", "tenant_code", "deleted", "create_time", "update_time")));

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper jsonMapper = JsonMapperFactory.getDefault();
    @Autowired
    private LcAppEntityMapper appMapper;
    @Autowired
    private EntityMapper entityMapper;
    @Autowired
    private FieldMapper fieldMapper;
    @Autowired
    private EventService eventService;

    public SchemaAdminBizService(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // ===== App CRUD =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppDTO createApp(AppDTO req) {
        if (req.getAppCode() == null || req.getAppCode().isEmpty()) {
            throw new IllegalArgumentException("appCode is required");
        }
        if (req.getTenantCode() == null || req.getTenantCode().isEmpty()) {
            throw new IllegalArgumentException("tenantCode is required");
        }
        // 唯一性校验。uk_app_tenant_code 不含 deleted 列 —— 软删掉的 app 依旧占着这个 code，
        // 所以这里不能加 .eq("deleted", 0)：加了会放过去，insert 撞索引变成 500 且把索引名/列名透给前端。
        List<AppEntity> hits = appMapper.selectList(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", req.getTenantCode())
                        .eq("app_code", req.getAppCode()));
        if (!hits.isEmpty()) {
            boolean occupiedByDeleted = hits.get(0).getDeleted() != null && hits.get(0).getDeleted() == 1;
            throw new IllegalArgumentException("App already exists: " + req.getAppCode()
                    + (occupiedByDeleted ? "（该编码此前已被删除，唯一索引仍占着它，请换一个编码）" : ""));
        }

        AppEntity entity = new AppEntity();
        BeanUtils.copyProperties(req, entity, "id", "createTime", "updateTime");
        entity.setStatus(req.getStatus() != null ? req.getStatus() : "DRAFT");
        entity.setCurrentVersion(0L);
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        appMapper.insert(entity);

        log.info("App created: tenant={} code={}", req.getTenantCode(), req.getAppCode());
        return toAppDTO(entity);
    }

    @Override
    public PageResult<AppDTO> listApps(String tenantCode, int page, int size) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 20;
        }

        size = Math.min(size, 200);

        // COUNT 不能带 ORDER BY (见 AppAdminBizService#pageApps 的同款注释)
        QueryWrapper<AppEntity> qw = new QueryWrapper<AppEntity>()
                .eq("deleted", 0)
                .eq(tenantCode != null, "tenant_code", tenantCode);

        Long total = appMapper.selectCount(qw);
        if (total == null || total == 0) {
            return new PageResult<>(Collections.emptyList(), 0L, page, size);
        }

        qw.orderByDesc("id");
        qw.last("LIMIT " + ((page - 1) * size) + "," + size);
        List<AppEntity> list = appMapper.selectList(qw);
        List<AppDTO> dtos = list.stream().map(this::toAppDTO).collect(Collectors.toList());
        return new PageResult<>(dtos, total, page, size);
    }

    @Override
    public AppDTO getApp(Long id) {
        AppEntity entity = appMapper.selectById(id);
        return entity == null || entity.getDeleted() == 1 ? null : toAppDTO(entity);
    }

    @Override
    public AppDTO getAppByCode(String tenantCode, String appCode) {
        AppEntity entity = appMapper.selectOne(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0));
        return entity == null ? null : toAppDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateApp(Long id, AppDTO req) {
        AppEntity entity = appMapper.selectById(id);
        if (entity == null || entity.getDeleted() == 1) {
            throw new IllegalArgumentException("App not found: " + id);
        }
        if (req.getAppName() != null) {
            entity.setAppName(req.getAppName());
        }

        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }

        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }

        entity.setCurrentVersion(entity.getCurrentVersion() + 1);
        entity.setUpdateTime(new Date());
        return appMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteApp(Long id) {
        AppEntity entity = appMapper.selectById(id);
        if (entity == null) {
            return 0;
        }

        entity.setDeleted(1);
        entity.setUpdateTime(new Date());
        return appMapper.updateById(entity);
    }

    // ===== Entity CRUD =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntityDefDTO createEntity(String tenantCode, String appCode, EntityDefDTO req) {
        if (req.getEntityCode() == null || req.getEntityCode().isEmpty()) {
            throw new IllegalArgumentException("entityCode is required");
        }
        // tableName 默认值: lc_{appCode}_{entityCode}
        if (req.getTableName() == null || req.getTableName().isEmpty()) {
            req.setTableName("lc_" + appCode + "_" + req.getEntityCode());
        }
        // 校验 appCode 存在
        AppEntity app = appMapper.selectOne(
                new QueryWrapper<AppEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0));
        if (app == null) {
            throw new IllegalArgumentException("App not found: " + appCode);
        }
        // 唯一性校验
        EntityEntity existing = entityMapper.selectOne(
                new QueryWrapper<EntityEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("entity_code", req.getEntityCode())
                        .eq("deleted", 0));
        if (existing != null) {
            throw new IllegalArgumentException("Entity already exists: " + req.getEntityCode());
        }
        // 先校验再落库: 建表要等 provision 才发生，元数据一旦写进去就是永久一份坏定义，
        // 而且 provision-all 会在第一个坏实体上抛，把同应用其他实体的表一起挡住。
        validateFieldCodes(req.getFields());

        EntityEntity entity = new EntityEntity();
        BeanUtils.copyProperties(req, entity, "id", "createTime", "updateTime");
        entity.setTenantCode(tenantCode);
        entity.setAppCode(appCode);
        entity.setCurrentVersion(0L);
        entity.setDeleted(0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        entityMapper.insert(entity);

        // 保存字段
        if (req.getFields() != null) {
            for (int i = 0; i < req.getFields().size(); i++) {
                FieldDefDTO fd = req.getFields().get(i);
                FieldEntity fe = toFieldEntity(fd, tenantCode, entity.getId());
                if (fe.getSortOrder() == null) {
                    fe.setSortOrder(i);
                }

                fieldMapper.insert(fe);
            }
        }

        // 追加 CREATE 事件
        emitEvent(tenantCode, appCode, req.getEntityCode(), "CREATE", req);

        log.info("Entity created: app={} entity={}", appCode, req.getEntityCode());
        return getEntity(entity.getId());
    }

    @Override
    public List<EntityDefDTO> listEntities(String tenantCode, String appCode) {
        List<EntityEntity> entities = entityMapper.selectList(
                new QueryWrapper<EntityEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .eq("deleted", 0)
                        .orderByAsc("id"));

        List<EntityDefDTO> result = new ArrayList<>();
        for (EntityEntity e : entities) {
            EntityDefDTO dto = toEntityDTO(e);
            dto.setFields(listFields(e.getId()));
            result.add(dto);
        }
        return result;
    }

    @Override
    public EntityDefDTO getEntity(Long id) {
        EntityEntity e = entityMapper.selectById(id);
        if (e == null || e.getDeleted() == 1) {
            return null;
        }

        EntityDefDTO dto = toEntityDTO(e);
        dto.setFields(listFields(id));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateEntity(Long id, EntityDefDTO req) {
        EntityEntity e = entityMapper.selectById(id);
        if (e == null || e.getDeleted() == 1) {
            throw new IllegalArgumentException("Entity not found: " + id);
        }
        // 同样在任何写入之前: 全量替换会先把旧字段软删掉，坏编码进来后再想退回去就没退路了。
        validateFieldCodes(req.getFields());
        if (req.getEntityName() != null) {
            e.setEntityName(req.getEntityName());
        }

        if (req.getDescription() != null) {
            e.setDescription(req.getDescription());
        }

        e.setCurrentVersion(e.getCurrentVersion() + 1);
        e.setUpdateTime(new Date());
        int n = entityMapper.updateById(e);

        // 更新字段 (全量替换)
        if (req.getFields() != null) {
            // 软删旧字段
            List<FieldEntity> oldFields = fieldMapper.selectList(
                    new QueryWrapper<FieldEntity>()
                            .eq("entity_id", id)
                            .eq("deleted", 0));
            for (FieldEntity of_ : oldFields) {
                of_.setDeleted(1);
                of_.setUpdateTime(new Date());
                fieldMapper.updateById(of_);
            }
            // 插入新字段
            for (int i = 0; i < req.getFields().size(); i++) {
                FieldDefDTO fd = req.getFields().get(i);
                FieldEntity fe = toFieldEntity(fd, e.getTenantCode(), id);
                if (fe.getSortOrder() == null) {
                    fe.setSortOrder(i);
                }

                fieldMapper.insert(fe);
            }
        }

        // 追加 UPDATE 事件
        EntityDefDTO full = getEntity(id);
        emitEvent(e.getTenantCode(), e.getAppCode(), e.getEntityCode(), "UPDATE", full);

        return n;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteEntity(Long id) {
        EntityEntity e = entityMapper.selectById(id);
        if (e == null) {
            return 0;
        }

        e.setDeleted(1);
        e.setUpdateTime(new Date());
        int n = entityMapper.updateById(e);

        // 软删字段
        List<FieldEntity> fields = fieldMapper.selectList(
                new QueryWrapper<FieldEntity>()
                        .eq("entity_id", id)
                        .eq("deleted", 0));
        for (FieldEntity f : fields) {
            f.setDeleted(1);
            f.setUpdateTime(new Date());
            fieldMapper.updateById(f);
        }

        // 追加 DELETE 事件
        EntityDefDTO def = new EntityDefDTO();
        def.setEntityCode(e.getEntityCode());
        emitEvent(e.getTenantCode(), e.getAppCode(), e.getEntityCode(), "DELETE", def);

        return n;
    }

    // ===== DDL Provisioning =====

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String provisionTable(Long entityId) {
        EntityDefDTO def = getEntity(entityId);
        if (def == null) {
            throw new IllegalArgumentException("Entity not found: " + entityId);
        }
        String ddl = buildCreateTableDdl(def);
        log.info("Provisioning table for entity={}: {}", def.getEntityCode(), ddl);
        jdbcTemplate.execute(ddl);
        return ddl;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> provisionAllTables(String tenantCode, String appCode) {
        List<EntityDefDTO> entities = listEntities(tenantCode, appCode);
        Map<String, String> result = new LinkedHashMap<>();
        for (EntityDefDTO def : entities) {
            String ddl = buildCreateTableDdl(def);
            log.info("Provisioning table for entity={}: {}", def.getEntityCode(), ddl);
            jdbcTemplate.execute(ddl);
            result.put(def.getEntityCode(), ddl);
        }
        return result;
    }

    // ===== Internal Helpers =====

    /**
     * 字段编码在写入处就要过这道闸: 撞名要等建表才炸 (裸 500 + 整应用连坐),
     * 不合法的编码更坏 —— 建表会静默跳过它, 于是元数据说"有这一列", 物理表里没有.
     */
    private void validateFieldCodes(List<FieldDefDTO> fields) {
        if (fields == null) {
            return;
        }
        for (FieldDefDTO f : fields) {
            String code = f == null ? null : f.getFieldCode();
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("fieldCode is required");
            }
            if (!FIELD_CODE_RE.matcher(code).matches()) {
                throw new IllegalArgumentException("字段编码不是合法列名: " + code
                        + " (需字母开头, 仅含字母/数字/下划线)");
            }
            if (SYSTEM_COLUMN_CODES.contains(code.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("字段编码撞了引擎自建列: " + code
                        + " (保留列: " + String.join(", ", SYSTEM_COLUMN_CODES) + ")");
            }
        }
    }

    private String buildCreateTableDdl(EntityDefDTO def) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `").append(def.getTableName()).append("` (\n");
        sb.append("  `id` BIGINT NOT NULL AUTO_INCREMENT,\n");

        // 这里以前对撞名/非法编码是 `continue` 静默跳过: 建表照样返回"成功", 少几列没人知道。
        validateFieldCodes(def.getFields());

        if (def.getFields() != null) {
            for (FieldDefDTO f : def.getFields()) {
                String colType = DynamicSqlBuilder.jdbcType(f.getFieldType(), f.getFieldLength(), f.getScale());
                sb.append("  `").append(f.getFieldCode()).append("` ").append(colType);
                if (f.getRequired() != null && f.getRequired()) {
                    sb.append(" NOT NULL");
                }
                if (f.getDefaultValue() != null && !f.getDefaultValue().isEmpty()) {
                    sb.append(" DEFAULT '").append(f.getDefaultValue().replace("'", "\\'")).append("'");
                }
                if (f.getDescription() != null) {
                    sb.append(" COMMENT '").append(f.getDescription().replace("'", "\\'")).append("'");
                }
                sb.append(",\n");
            }
        }

        sb.append("  `tenant_code` VARCHAR(64) COMMENT '租户编码',\n");
        sb.append("  `deleted` TINYINT DEFAULT 0 COMMENT '软删',\n");
        sb.append("  `create_time` DATETIME COMMENT '创建时间',\n");
        sb.append("  `update_time` DATETIME COMMENT '更新时间',\n");
        sb.append("  PRIMARY KEY (`id`)\n");
        sb.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='")
                .append(def.getEntityName() != null ? def.getEntityName() : def.getEntityCode())
                .append("'");
        return sb.toString();
    }

    private void emitEvent(String tenantCode, String appCode, String entityCode,
                           String eventType, EntityDefDTO def) {
        try {
            EventAppendRequest req = new EventAppendRequest();
            req.setTenantCode(tenantCode);
            req.setEventType(eventType);
            req.setEntityCode(entityCode);
            req.setSource("ADMIN");
            // 末次事件 id 作为 parent
            com.zifang.z.lc.common.dto.EventDTO last = eventService.getLastEvent(tenantCode, appCode);
            req.setParentEventId(last == null ? null : last.getEventId());
            req.setEventData(jsonMapper.writeValueAsString(def));
            eventService.append(appCode, req);
        } catch (Exception ex) {
            log.warn("Failed to emit {} event for entity={}: {}", eventType, entityCode, ex.getMessage());
        }
    }

    private List<FieldDefDTO> listFields(Long entityId) {
        List<FieldEntity> fields = fieldMapper.selectList(
                new QueryWrapper<FieldEntity>()
                        .eq("entity_id", entityId)
                        .eq("deleted", 0)
                        .orderByAsc("sort_order"));
        return fields.stream().map(this::toFieldDTO).collect(Collectors.toList());
    }

    private AppDTO toAppDTO(AppEntity e) {
        AppDTO dto = new AppDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }

    private EntityDefDTO toEntityDTO(EntityEntity e) {
        EntityDefDTO dto = new EntityDefDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }

    private FieldDefDTO toFieldDTO(FieldEntity e) {
        FieldDefDTO dto = new FieldDefDTO();
        dto.setId(e.getId());
        dto.setTenantCode(e.getTenantCode());
        dto.setEntityId(e.getEntityId());
        dto.setFieldCode(e.getFieldCode());
        dto.setFieldName(e.getFieldName());
        dto.setFieldType(e.getFieldType());
        dto.setRequired(e.getRequired() != null && e.getRequired() == 1);
        dto.setDefaultValue(e.getDefaultValue());
        dto.setDictCode(e.getDictCode());
        dto.setRefEntity(e.getRefEntity());
        dto.setFieldLength(e.getFieldLength());
        dto.setScale(e.getScale());
        dto.setSortOrder(e.getSortOrder());
        dto.setDescription(e.getDescription());
        return dto;
    }

    private FieldEntity toFieldEntity(FieldDefDTO dto, String tenantCode, Long entityId) {
        FieldEntity fe = new FieldEntity();
        fe.setTenantCode(tenantCode);
        fe.setEntityId(entityId);
        fe.setFieldCode(dto.getFieldCode());
        fe.setFieldName(dto.getFieldName());
        fe.setFieldType(dto.getFieldType());
        fe.setRequired(dto.getRequired() != null && dto.getRequired() ? 1 : 0);
        fe.setDefaultValue(dto.getDefaultValue());
        fe.setDictCode(dto.getDictCode());
        fe.setRefEntity(dto.getRefEntity());
        fe.setFieldLength(dto.getFieldLength());
        fe.setScale(dto.getScale());
        fe.setSortOrder(dto.getSortOrder());
        fe.setDescription(dto.getDescription());
        fe.setDeleted(0);
        fe.setCreateTime(new Date());
        fe.setUpdateTime(new Date());
        return fe;
    }
}
