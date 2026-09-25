package com.zifang.z.lc.core.schema;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.json.JsonMapperFactory;
import com.zifang.util.core.meta.page.PageResult;
import com.zifang.z.lc.common.dto.AppDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.EventAppendRequest;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
        // 物理表名在租户内是一份命名空间。不在这道闸上拦，坏消息要等 provision 才出现，
        // 而且那时 `CREATE TABLE IF NOT EXISTS` 对第二个实体是空操作 —— 接口照样回"建好了"。
        validateTableNameAvailable(tenantCode, req.getTableName());
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
        // 同样在任何写入之前: 坏编码进来后再想退回去就没退路了 (被移除的那些一软删，
        // 原来那行同名记录就成了占着编码的墓碑)。
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

        // 更新字段: 按 field_code 对齐，而不是"全删再全插"。
        // uk_field_entity_code 是 (entity_id, field_code)，**不看 deleted** —— 所以旧写法在每一次
        // 保存上都会撞自己刚留下的那行墓碑: 实体建好字段之后就再也改不动了 (PUT 一律 400，
        // 而且 ExceptionHandler 报的是"同一租户下的编码必须唯一"，指错了约束)。
        // 这条约束真正在说的是"一个实体里每列只有一个名字"，那就照它做: 同码的行原地更新、
        // 被删过又加回来的把那行复活，只有从没出现过的编码才 insert，被移除的才软删。
        if (req.getFields() != null) {
            Map<String, FieldEntity> rows = new HashMap<>();
            for (FieldEntity f : fieldMapper.selectList(
                    new QueryWrapper<FieldEntity>().eq("entity_id", id))) {
                rows.put(f.getFieldCode().toLowerCase(Locale.ROOT), f);
            }
            List<FieldDefDTO> defs = req.getFields();
            Set<String> kept = new HashSet<>();
            for (int i = 0; i < defs.size(); i++) {
                FieldDefDTO fd = defs.get(i);
                String key = fd.getFieldCode().toLowerCase(Locale.ROOT);
                kept.add(key);
                FieldEntity row = rows.get(key);
                if (row == null) {
                    row = toFieldEntity(fd, e.getTenantCode(), id);
                    if (row.getSortOrder() == null) {
                        row.setSortOrder(i);
                    }
                    fieldMapper.insert(row);
                    continue;
                }
                copyFieldAttrs(fd, row);
                if (row.getSortOrder() == null) {
                    row.setSortOrder(i);
                }
                row.setDeleted(0);
                row.setUpdateTime(new Date());
                fieldMapper.updateById(row);
            }
            for (Map.Entry<String, FieldEntity> stale : rows.entrySet()) {
                if (!kept.contains(stale.getKey()) && stale.getValue().getDeleted() == 0) {
                    FieldEntity row = stale.getValue();
                    row.setDeleted(1);
                    row.setUpdateTime(new Date());
                    fieldMapper.updateById(row);
                    // 运行时权威源是事件链，不是这张表: 只墓碑化不吭声，折叠出来的定义里那一栏永远还在。
                    emitFieldRemoved(e.getTenantCode(), e.getAppCode(), e.getEntityCode(),
                            row.getFieldCode());
                }
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

    /**
     * 单个实体建表。
     * <p>
     * 这里**不带** {@code @Transactional}: DDL 在 MySQL 里本来就隐式提交，套一个看起来能回滚的
     * 事务反而骗人。真正的保证是"建完回读物理列"。
     */
    @Override
    public ProvisionReport.Item provisionTable(Long entityId) {
        EntityDefDTO def = getEntity(entityId);
        if (def == null) {
            throw new IllegalArgumentException("Entity not found: " + entityId);
        }
        return provisionOne(def);
    }

    /**
     * 逐个实体建，坏的那支只红自己。
     * <p>
     * 旧写法是整批一个事务 + 直接抛: 第一个坏实体把同应用其他实体的表一起挡住，调用方只拿到一句
     * 500，不知道哪些建成了、哪些没建成。
     */
    @Override
    public ProvisionReport provisionAllTables(String tenantCode, String appCode) {
        List<EntityDefDTO> entities = listEntities(tenantCode, appCode);
        ProvisionReport report = new ProvisionReport();
        report.setAppCode(appCode);
        for (EntityDefDTO def : entities) {
            ProvisionReport.Item item = provisionOne(def);
            report.getItems().add(item);
            if (ProvisionReport.CREATED.equals(item.getStatus())) {
                report.setCreated(report.getCreated() + 1);
            } else if (ProvisionReport.ALTERED.equals(item.getStatus())) {
                report.setAltered(report.getAltered() + 1);
            } else if (ProvisionReport.EXISTS_INTACT.equals(item.getStatus())) {
                report.setUnchanged(report.getUnchanged() + 1);
            }
        }
        report.setTotal(entities.size());
        return report;
    }

    private ProvisionReport.Item provisionOne(EntityDefDTO def) {
        String ddl = buildCreateTableDdl(def);
        Set<String> before = physicalColumns(def.getTableName());
        try {
            log.info("Provisioning table for entity={}: {}", def.getEntityCode(), ddl);
            jdbcTemplate.execute(ddl);
        } catch (Exception ex) {
            log.error("DDL failed for entity={}", def.getEntityCode(), ex);
            return item(def, ProvisionReport.FAILED, ddl, "建表失败: " + briefCause(ex));
        }

        // 建完回读物理列 —— 这一步才是"建成了"的定义。IF NOT EXISTS 对一张已在的表是空操作，
        // 所以"这条 DDL 没报错"完全可以意味着"这份定义一列都没落地"。
        Set<String> after = physicalColumns(def.getTableName());

        // 引擎自建的那五列缺任何一列，都说明这张表**不是这个引擎建的** (或被人改坏了)。
        // 这种表不去补: 给别人的表上加 `deleted`/`tenant_code` 是拿一份定义去改一张不归它管的表，
        // 而且"两个实体抢同一张物理表"那一类缺陷正是靠这里报失败才被看见的 (#43)。
        List<String> missingSystem = new ArrayList<>();
        for (String sys : SYSTEM_COLUMN_CODES) {
            if (!after.contains(sys)) {
                missingSystem.add(sys);
            }
        }
        if (!missingSystem.isEmpty()) {
            ProvisionReport.Item bad = item(def, ProvisionReport.FAILED, ddl,
                    "表 " + def.getTableName() + " 缺引擎自建列 (缺 " + String.join(", ", missingSystem)
                            + ")。这张表不是按本引擎的建表语句建出来的，补列只对**自己建的表**做；"
                            + "先看是不是有别的实体或别的系统占了同一张表。");
            bad.setMissingColumns(missingSystem);
            return bad;
        }

        // 用户列缺 ⇒ 按这份定义**只加列**补上 (缺陷 #47)。旧口径在这里直接判 FAILED 且永远修不好:
        // 定义比表多一栏之后建表是空操作、回读永远缺那一栏，而运行时每一次列表都是裸 500 ——
        // 一个谁都清不掉的"未建成"。
        List<String> missingUser = new ArrayList<>();
        for (String expected : expectedColumns(def)) {
            if (!after.contains(expected)) {
                missingUser.add(expected);
            }
        }
        if (!missingUser.isEmpty()) {
            return reconcileColumns(def, ddl, missingUser);
        }
        return before.isEmpty()
                ? item(def, ProvisionReport.CREATED, ddl, "已按这份定义建表")
                : item(def, ProvisionReport.EXISTS_INTACT, ddl, "表本来就在，列一列不缺，跳过建表");
    }

    /**
     * 把定义里有、表里还没有的那几列补上。逐列执行，坏的一列只红自己并把原因带回去。
     * <p>
     * 补完**再回读一次**物理列: "ALTER 没报错"和"那一栏真在表里了"仍然不是同一件事
     * (比如给一张有行的表加 NOT NULL 且无默认值，MySQL 会拒)。所以最终状态以回读为准。
     */
    private ProvisionReport.Item reconcileColumns(EntityDefDTO def, String createDdl, List<String> missingUser) {
        EntityEntity squatter = otherLiveEntityOnSameTable(def);
        if (squatter != null) {
            // 缺陷 #43 的那一族: 两张定义共用一张物理表时，"补列"会把 A 的表改成 B 想要的样子。
            // 建表闸已经不允许**新建**这种组合，但软删实体留下的旧表、逆向映射导入的表都能把它凑出来，
            // 所以补列之前还要再问一次这张表归谁。这一支同时也是"FAILED 这个形状真的还存在"的证据。
            ProvisionReport.Item bad = item(def, ProvisionReport.FAILED, createDdl,
                    "表 " + def.getTableName() + " 同时被实体 " + squatter.getAppCode() + "/"
                            + squatter.getEntityCode() + " 占着，补列会改到别人的表，这里一列都不动 (缺 "
                            + String.join(", ", missingUser) + ")");
            bad.setMissingColumns(missingUser);
            return bad;
        }

        Map<String, FieldDefDTO> byCode = new LinkedHashMap<>();
        if (def.getFields() != null) {
            for (FieldDefDTO f : def.getFields()) {
                if (f != null && f.getFieldCode() != null) {
                    byCode.put(f.getFieldCode().toLowerCase(Locale.ROOT), f);
                }
            }
        }

        StringBuilder ddl = new StringBuilder(createDdl);
        List<String> added = new ArrayList<>();
        List<String> refused = new ArrayList<>();
        for (String code : missingUser) {
            FieldDefDTO f = byCode.get(code);
            if (f == null) {
                // 期望列里点名了它，定义里却没有这一栏: 那是引擎自建列的口径, 上面已经拦过了。
                refused.add(code + ": 定义里没有这一栏");
                continue;
            }
            String one = buildAddColumnDdl(def.getTableName(), f);
            ddl.append("\n\n").append(one).append(';');
            try {
                log.info("Reconciling missing column for entity={}: {}", def.getEntityCode(), one);
                jdbcTemplate.execute(one);
                added.add(code);
            } catch (Exception ex) {
                log.error("ADD COLUMN failed for entity={} column={}", def.getEntityCode(), code, ex);
                refused.add(code + ": " + briefCause(ex));
            }
        }

        String text = ddl.toString();
        Set<String> now = physicalColumns(def.getTableName());
        List<String> still = new ArrayList<>();
        for (String code : missingUser) {
            if (!now.contains(code)) {
                still.add(code);
            }
        }
        if (!still.isEmpty()) {
            ProvisionReport.Item bad = item(def, ProvisionReport.FAILED, text,
                    "表 " + def.getTableName() + " 补列没补上 (仍缺 " + String.join(", ", still) + ")"
                            + (refused.isEmpty() ? "" : ": " + String.join("; ", refused))
                            + "。已补上的列: " + (added.isEmpty() ? "无" : String.join(", ", added)));
            bad.setMissingColumns(still);
            bad.setAddedColumns(added);
            return bad;
        }
        ProvisionReport.Item ok = item(def, ProvisionReport.ALTERED, text,
                "表本来就在，按这份定义补了 " + added.size() + " 列（只加列，不动已有列、不改类型、不删数据）");
        ok.setAddedColumns(added);
        return ok;
    }

    private ProvisionReport.Item item(EntityDefDTO def, String status, String ddl, String message) {
        return new ProvisionReport.Item(def.getEntityCode(), def.getTableName(), status, ddl, message);
    }

    /** 这份定义期望表里有哪些列: 用户列 + 引擎自建的那五列 (同一份口径，见 SYSTEM_COLUMN_CODES)。 */
    private Set<String> expectedColumns(EntityDefDTO def) {
        Set<String> cols = new LinkedHashSet<>(SYSTEM_COLUMN_CODES);
        if (def.getFields() != null) {
            for (FieldDefDTO f : def.getFields()) {
                if (f != null && f.getFieldCode() != null) {
                    cols.add(f.getFieldCode().toLowerCase(Locale.ROOT));
                }
            }
        }
        return cols;
    }

    /** 物理表里真有哪些列；表不存在或读不到时返回空集 (小写比较，MySQL 列名不分大小写)。 */
    private Set<String> physicalColumns(String tableName) {
        Set<String> cols = new LinkedHashSet<>();
        if (tableName == null || tableName.trim().isEmpty()) {
            return cols;
        }
        try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME");
                    if (name != null) {
                        cols.add(name.toLowerCase(Locale.ROOT));
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to read physical columns of {}: {}", tableName, ex.getMessage());
        }
        return cols;
    }

    /**
     * 回给前端的建表失败摘要: 只留最深一层的原因，且绝不带 SQL 文本 —— JDBC 的 message 里
     * 常有 {@code SQL [CREATE TABLE ...]}，原样透出等于把物理表名和语句结构发给浏览器。
     */
    private static String briefCause(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String raw = root.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return "DDL execution failed";
        }
        String hint = raw.split("(?i)SQL|\\R", 2)[0].trim();
        if (hint.isEmpty()) {
            hint = "DDL execution failed";
        }
        return hint.length() > 120 ? hint.substring(0, 120) : hint;
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
        Set<String> seen = new HashSet<>();
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
            // 同一实体里两列同名: 库里 uk_field_entity_code 也会拒，但那条文案说的是
            // "同一租户下的编码必须唯一" —— 报的是错的约束，用户按它去换实体编码也救不回来。
            if (!seen.add(code.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("同一实体里有重复的字段编码: " + code
                        + " (一个实体里每列的名字必须不同)");
            }
        }
    }

    /**
     * 表名在这份租户内没被别的实体占着。
     * <p>
     * 只比**未删除**的实体: 删实体不会删物理表，把软删的也算进来等于一个表名被永久占死。
     * 大小写不敏感比较 (MySQL/H2 的表名默认不分大小写，`TBL` 和 `tbl` 抢的是同一张表)。
     * 软删实体留下的旧表，由 provision 那一侧处理: 补列只认**这张表没有被别的未删除实体占着**
     * (见 {@link #otherLiveEntityOnSameTable})。
     */
    private void validateTableNameAvailable(String tenantCode, String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("tableName is required");
        }
        List<EntityEntity> live = entityMapper.selectList(
                new QueryWrapper<EntityEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("deleted", 0));
        String wanted = tableName.toLowerCase(Locale.ROOT);
        for (EntityEntity other : live) {
            String held = other.getTableName();
            if (held != null && wanted.equals(held.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("物理表名已被其他实体占用: " + tableName
                        + " (实体 " + other.getAppCode() + "/" + other.getEntityCode() + ")");
            }
        }
    }

    /**
     * 同一租户里还有别的**未删除**实体指着这张物理表吗 (排除自己)。
     * <p>
     * 上面那道闸只拦"新建时抢名"，抢不出来不等于不存在: 软删实体的旧表、逆向映射导入的表都能
     * 让两份定义落在一张表上。建表阶段这无所谓 (IF NOT EXISTS 是空操作)，但**补列**是有副作用的
     * ALTER —— 不做这一问，就等于拿 B 的定义去改 A 的表。
     */
    private EntityEntity otherLiveEntityOnSameTable(EntityDefDTO def) {
        if (def == null || def.getTenantCode() == null || def.getTableName() == null) {
            return null;
        }
        String wanted = def.getTableName().toLowerCase(Locale.ROOT);
        for (EntityEntity other : entityMapper.selectList(new QueryWrapper<EntityEntity>()
                .eq("tenant_code", def.getTenantCode())
                .eq("deleted", 0))) {
            String held = other.getTableName();
            if (held != null && wanted.equals(held.toLowerCase(Locale.ROOT))
                    && (def.getId() == null || !def.getId().equals(other.getId()))) {
                return other;
            }
        }
        return null;
    }

    private String buildCreateTableDdl(EntityDefDTO def) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS `").append(def.getTableName()).append("` (\n");
        sb.append("  `id` BIGINT NOT NULL AUTO_INCREMENT,\n");

        // 这里以前对撞名/非法编码是 `continue` 静默跳过: 建表照样返回"成功", 少几列没人知道。
        validateFieldCodes(def.getFields());

        if (def.getFields() != null) {
            for (FieldDefDTO f : def.getFields()) {
                sb.append("  ").append(columnClause(f)).append(",\n");
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

    /**
     * 一列的建表片段 (不含缩进、不含逗号)。
     * <p>
     * 单独抽出来是因为现在有两个生产方: 整张表的 CREATE，和"定义跑在表前面"时逐列的 ALTER
     * (缺陷 #47)。两处各写一份类型映射，迟早一个改了一个没改 —— 那时候补出来的列会和建表建出来的
     * 列宽度/可空性不一致，而这种不一致只有等下一次数据写歪了才看得见。
     */
    private String columnClause(FieldDefDTO f) {
        StringBuilder sb = new StringBuilder();
        String colType = DynamicSqlBuilder.jdbcType(f.getFieldType(), f.getFieldLength(), f.getScale());
        sb.append("`").append(f.getFieldCode()).append("` ").append(colType);
        if (f.getRequired() != null && f.getRequired()) {
            sb.append(" NOT NULL");
        }
        if (f.getDefaultValue() != null && !f.getDefaultValue().isEmpty()) {
            sb.append(" DEFAULT '").append(quoteLiteral(f.getDefaultValue())).append("'");
        }
        if (f.getDescription() != null) {
            sb.append(" COMMENT '").append(quoteLiteral(f.getDescription())).append("'");
        }
        return sb.toString();
    }

    private static String quoteLiteral(String v) {
        return v.replace("'", "\\'");
    }

    /** 给已存在的表补一列。只加，不动已有列、不改类型、不删任何东西。 */
    private String buildAddColumnDdl(String tableName, FieldDefDTO f) {
        return "ALTER TABLE `" + tableName + "` ADD COLUMN " + columnClause(f);
    }

    private void emitEvent(String tenantCode, String appCode, String entityCode,                           String eventType, EntityDefDTO def) {
        emit(tenantCode, appCode, entityCode, eventType, def);
    }

    /**
     * 追加一个 schema 事件。失败只 warn（沿用既有口径），所以事件链和元数据表是可以分开坏的 ——
     * {@link #updateEntity} 折叠的是事件链，读回来不一致就是缺陷 #46 那一类。
     */
    private void emit(String tenantCode, String appCode, String entityCode,
                      String eventType, Object payload) {
        try {
            EventAppendRequest req = new EventAppendRequest();
            req.setTenantCode(tenantCode);
            req.setEventType(eventType);
            req.setEntityCode(entityCode);
            req.setSource("ADMIN");
            // 末次事件 id 作为 parent
            com.zifang.z.lc.common.dto.EventDTO last = eventService.getLastEvent(tenantCode, appCode);
            req.setParentEventId(last == null ? null : last.getEventId());
            req.setEventData(jsonMapper.writeValueAsString(payload));
            eventService.append(appCode, req);
        } catch (Exception ex) {
            log.warn("Failed to emit {} event for entity={}: {}", eventType, entityCode, ex.getMessage());
        }
    }

    /**
     * 一栏从定义里消失: 补一条 field 级 DELETE 事件。
     * <p>
     * 折叠端 ({@code EventReplayService.applyDelete}) 一直都认得 {@code {fieldCode}} 这个形状，
     * 只是从来没有人发过它 —— 于是 UPDATE 的"按码替换或追加"变成唯一的折叠动作，删掉的栏永远留在
     * 运行时定义里，每一次列表都把物理表里那一列的旧值再端出来一遍（缺陷 #46）。
     */
    private void emitFieldRemoved(String tenantCode, String appCode, String entityCode, String fieldCode) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("entityCode", entityCode);
        payload.put("fieldCode", fieldCode);
        emit(tenantCode, appCode, entityCode, "DELETE", payload);
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
        copyFieldAttrs(dto, fe);
        fe.setDeleted(0);
        fe.setCreateTime(new Date());
        fe.setUpdateTime(new Date());
        return fe;
    }

    /**
     * 一份字段定义里"这一列长什么样"的那些属性。新建 (toFieldEntity) 与原地改 (updateEntity)
     * 共用这一份清单: 各抄一遍的话，以后加一个新属性只会加进其中一遍 —— 于是"新建时挂得上、
     * 改一次就掉"，而这种掉法一个字都不会报错。
     * <p>
     * 不含 fieldCode: 它是那一行的身份 (uk_field_entity_code 拿它当键)，换编码要走
     * "软删旧行 + 插新行"，不是原地覆盖。也不含 id/createTime/deleted —— 那些是行的履历，
     * 不是这一列长什么样。
     */
    private void copyFieldAttrs(FieldDefDTO dto, FieldEntity fe) {
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
    }
}
