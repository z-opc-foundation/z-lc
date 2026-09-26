package com.zifang.z.lc.core.undo;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.executor.RuntimeCrudExecutor;
import com.zifang.z.lc.core.undo.entity.DataChangeEntity;
import com.zifang.z.lc.mapper.undo.DataChangeMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行态数据的 undo / redo —— 前像后像存在 z_lc_data_change, 撤销动作在服务端完成.
 * <p>
 * 为什么值得做: z-lc 已经有一条设计态事件溯源链, 数据层却完全没有回头路, 误删/误改只能靠
 * DBA 捞 binlog. 对标下来 Teable 把 undo/redo 做成了开源能力, 而 NocoDB 把它锁在企业版
 * (CE 里 useUndoRedo 是个空壳), 所以"有事件日志就顺手做掉 undo"是本项目性价比最高的差异化.
 * <p>
 * 语义: 按 (tenant, app, entity) 作用域倒序撤销, 并且带一个冲突保护 —— 如果目标记录在这条
 * 变更之后又被改过, 撤销会被拒绝而不是悄悄覆盖. 宁可不撤销, 也不撤销出错误的数据.
 */
@Service
public class UndoService {

    private static final Logger log = LogManager.getLogger(UndoService.class);

    @Autowired
    private DataChangeMapper changeMapper;

    @Autowired
    private RuntimeCrudExecutor crudExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 记一条变更日志. 记录失败绝不能把主写路径带崩, 所以这里吞掉异常只告警.
     */
    public void record(String tenantCode, String appCode, EntityDefDTO entity, Long recordId,
                       String operation, Map<String, Object> before, Map<String, Object> after,
                       String actor) {
        if (recordId == null || entity == null) {
            return;
        }
        DataChangeEntity row = new DataChangeEntity();
        row.setTenantCode(tenantCode);
        row.setAppCode(appCode);
        row.setEntityCode(entity.getEntityCode());
        row.setRecordId(recordId);
        row.setOperation(operation);
        row.setActor(actor);
        row.setCreateTime(new Date());
        try {
            row.setBeforeImage(write(entity, before));
            row.setAfterImage(write(entity, after));
            changeMapper.insert(row);
        } catch (Exception ex) {
            log.warn("failed to record {} on entity={} record={}: {}",
                    operation, entity.getEntityCode(), recordId, ex.getMessage());
        }
    }

    private String write(EntityDefDTO entity, Map<String, Object> row) throws IOException {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return objectMapper.writeValueAsString(persistable(entity, row));
    }

    /**
     * 快照只留真正落库的列: 丢掉 `*_label` / `*_name` 这些查询期 JOIN 出来的派生列,
     * 否则 redo 时会把它们当字段写进物理表.
     */
    private Map<String, Object> persistable(EntityDefDTO entity, Map<String, Object> row) {
        Map<String, String> types = fieldTypes(entity);
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String fieldType = types.get(entry.getKey());
            if (fieldType == null) {
                continue;
            }
            out.put(entry.getKey(), wireFormat(entry.getValue(), fieldType));
        }
        return out;
    }

    /**
     * 把快照里的值转成"重新写回时能被接受"的形态.
     * <p>
     * JDBC 读回来的 DATETIME 是 java.sql.Timestamp, Jackson 默认序列化成
     * {@code 2026-09-20T02:30:00.000+00:00}; 而 FieldTypeRegistry 的日期解析只认
     * {@code yyyy-MM-dd HH:mm:ss} / {@code yyyy-MM-dd}. 不换算的话, 快照能存但不能重放 ——
     * 撤销删除时会以 "Field requires date: ..." 直接失败, 凡是带日期/时间字段的实体都中招.
     * <p>
     * 但"读回来的是什么类型"本身随驱动变: H2 给 java.sql.Timestamp, 而 MySQL 8 的 Connector/J
     * 给 java.time.LocalDateTime —— 它既不是 Date 也不是 String, 上面两支全落空, 于是快照里存进
     * 了带 T 的 ISO 串 (250 上真 MySQL 实测: {@code "last_visit":"1937-11-05T12:00:00"}),
     * 重放当场失败。java.time 这一族必须在**写快照时**就归一, 不能指望读的那头.
     */
    private Object wireFormat(Object value, String fieldType) {
        if (value == null || fieldType == null) {
            return value;
        }
        String type = fieldType.toUpperCase();
        if (value instanceof Date && ("DATE".equals(type) || "DATETIME".equals(type))) {
            String pattern = "DATE".equals(type) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
            return new java.text.SimpleDateFormat(pattern).format((Date) value);
        }
        if (value instanceof java.time.LocalDateTime && "DATETIME".equals(type)) {
            return ((java.time.LocalDateTime) value).format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT));
        }
        if (value instanceof java.time.LocalDate && ("DATE".equals(type) || "DATETIME".equals(type))) {
            return value.toString();
        }
        if (value instanceof java.time.LocalTime && "TIME".equals(type)) {
            // LocalTime.toString() 在整分时给的是 "12:00" —— 秒不是可有可无的装饰，
            // 快照要和写路径认的形态一字不差。
            return ((java.time.LocalTime) value).format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss", java.util.Locale.ROOT));
        }
        if (value instanceof String && ("DATE".equals(type) || "DATETIME".equals(type))) {
            String text = ((String) value).trim();
            int tIndex = text.indexOf('T');
            if (tIndex > 0) {
                String datePart = text.substring(0, tIndex);
                // 快照里的 ISO 串带的是 UTC 偏移 (Jackson 按 UTC 序列化 Timestamp),
                // 而驱动把不带偏移的裸串按**本地时区**解释 —— 这个不对称意味着"直接把 +08:00 截掉"
                // 会把时刻整体平移 (实测 03:04:05 变成前一天 19:04:05). 必须按瞬时换算回本地墙钟.
                java.time.OffsetDateTime instant = null;
                try {
                    instant = java.time.OffsetDateTime.parse(text);
                } catch (java.time.format.DateTimeParseException ignore) {
                    // 形如 2026-01-02T03:04:05 (无偏移) 的串没法判定原始时区, 按本地墙钟处理
                }
                if (instant == null) {
                    return "DATE".equals(type) ? datePart : datePart + " " + text.substring(tIndex + 1).substring(0, 8);
                }
                java.time.LocalDateTime local = instant.atZoneSameInstant(
                        java.time.ZoneId.systemDefault()).toLocalDateTime();
                if ("DATE".equals(type)) {
                    return local.toLocalDate().toString();
                }
                return local.toLocalDate().toString() + " "
                        + local.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        }
        return value;
    }

    private Map<String, String> fieldTypes(EntityDefDTO entity) {
        Map<String, String> types = new java.util.HashMap<String, String>();
        if (entity.getFields() != null) {
            for (FieldDefDTO field : entity.getFields()) {
                if (field != null && field.getFieldCode() != null) {
                    types.put(field.getFieldCode(), field.getFieldType() == null ? "STRING" : field.getFieldType());
                }
            }
        }
        return types;
    }

    public List<DataChangeEntity> history(String tenantCode, String appCode, String entityCode, int limit) {
        return history(tenantCode, appCode, entityCode, limit, null);
    }

    /**
     * @param actor 非空时只返回这个人的改动. 列表默认给全量 —— 历史是协作信息,
     *              但"撤销"只作用于自己的栈, 这两件事刻意分开.
     */
    public List<DataChangeEntity> history(String tenantCode, String appCode, String entityCode,
                                          int limit, String actor) {
        int capped = limit <= 0 ? 50 : Math.min(limit, 500);
        return changeMapper.selectList(scoped(new QueryWrapper<DataChangeEntity>(),
                tenantCode, appCode, entityCode)
                .eq(actor != null, "actor", actor)
                .orderByDesc("id")
                .last("LIMIT " + capped));
    }

    /**
     * 撤销最近一次未被撤销的变更.
     *
     * @param ownerOnly 只撤这个 actor 自己的改动. 多人共用一个实体时必须是 true ——
     *                  否则 A 点"撤销"会把 B 刚写的改动回滚掉.
     */
    @Transactional(rollbackFor = Exception.class)
    public UndoOutcome undo(String tenantCode, String appCode, EntityDefDTO entity,
                            String actor, boolean ownerOnly) {
        DataChangeEntity target = latestReversible(tenantCode, appCode, entity.getEntityCode(),
                ownerOnly ? actor : null);
        if (target == null) {
            return UndoOutcome.nothing(actor == null || !ownerOnly
                    ? "没有可撤销的变更" : "你在这个实体上没有可撤销的变更");
        }
        applyInverted(entity, target, tenantCode, appCode, actor);
        markUndone(target, null);
        return new UndoOutcome(target.getId(), target.getOperation(), target.getRecordId(), true, null);
    }

    /** 重做最近一次被自己撤销的变更. */
    @Transactional(rollbackFor = Exception.class)
    public UndoOutcome redo(String tenantCode, String appCode, EntityDefDTO entity,
                            String actor, boolean ownerOnly) {
        DataChangeEntity target = latestUndone(tenantCode, appCode, entity.getEntityCode(),
                ownerOnly ? actor : null);
        if (target == null) {
            return UndoOutcome.nothing("没有可重做的变更");
        }
        applyForward(entity, target, tenantCode, appCode, actor);
        clearUndoneMarker(target.getId());
        return new UndoOutcome(target.getId(), target.getOperation(), target.getRecordId(), true, null);
    }

    /**
     * 重做后要把 undone_by 置回 NULL, 这条记录才重新可撤销.
     * <p>
     * 不能用 {@code updateById}: MyBatis-Plus 默认 FieldStrategy.NOT_NULL 会**跳过 null 字段**,
     * 于是 undone_by 保持非空, redo 表面上成功、实际记录仍被标成"已撤销", 再次 undo 就会跳过它.
     */
    private void clearUndoneMarker(Long changeId) {
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DataChangeEntity> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<DataChangeEntity>()
                        .set("undone_by", null)
                        .set("update_time", new Date())
                        .eq("id", changeId);
        changeMapper.update(null, wrapper);
    }

    private DataChangeEntity latestReversible(String tenantCode, String appCode, String entityCode,
                                              String actor) {
        List<DataChangeEntity> list = reversibleList(tenantCode, appCode, entityCode, actor);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<DataChangeEntity> reversibleList(String tenantCode, String appCode, String entityCode,
                                                  String actor) {
        return changeMapper.selectList(scoped(new QueryWrapper<DataChangeEntity>(),
                tenantCode, appCode, entityCode)
                .isNull("undone_by")
                .eq(actor != null, "actor", actor)
                .orderByDesc("id"));
    }

    private DataChangeEntity latestUndone(String tenantCode, String appCode, String entityCode,
                                          String actor) {
        List<DataChangeEntity> list = changeMapper.selectList(scoped(new QueryWrapper<DataChangeEntity>(),
                tenantCode, appCode, entityCode)
                .isNotNull("undone_by")
                .eq(actor != null, "actor", actor)
                .orderByDesc("id"));
        return list.isEmpty() ? null : list.get(0);
    }

    private QueryWrapper<DataChangeEntity> scoped(QueryWrapper<DataChangeEntity> wrapper,
                                                  String tenantCode, String appCode, String entityCode) {
        return wrapper.eq("tenant_code", tenantCode)
                .eq("app_code", appCode)
                .eq(entityCode != null && !entityCode.isEmpty(), "entity_code", entityCode);
    }

    private void markUndone(DataChangeEntity target, Long marker) {
        target.setUndoneBy(marker == null ? -1L : marker);
        changeMapper.updateById(target);
    }

    /** 目标记录在这条变更之后是否又被改过 (且没被撤销). 是的话禁止撤销, 避免悄悄覆盖. */
    private void assertNotSuperseded(DataChangeEntity target) {
        List<DataChangeEntity> later = changeMapper.selectList(new QueryWrapper<DataChangeEntity>()
                .eq("tenant_code", target.getTenantCode())
                .eq("app_code", target.getAppCode())
                .eq("entity_code", target.getEntityCode())
                .eq("record_id", target.getRecordId())
                .gt("id", target.getId())
                .isNull("undone_by")
                .orderByDesc("id"));
        // later 已经是"id 严格大于 target"的集合, 里面任何一条都意味着目标记录又被改过.
        // (之前写成 later.size() > 1, 恰好放过"只有一次后续变更"这种最常见的情况.)
        if (!later.isEmpty()) {
            throw new IllegalStateException("记录 " + target.getRecordId()
                    + " 在这之后还有 " + later.size() + " 次未撤销的变更，请先撤销更新的");
        }
    }

    private void applyInverted(EntityDefDTO entity, DataChangeEntity target,
                               String tenantCode, String appCode, String actor) {
        assertNotSuperseded(target);
        // 老快照 (修复之前写的) 里存的就是带 T 的 ISO 串，重放这一头也要归一次 ——
        // 不然"修了写快照的代码"等于"只对新数据有效，历史日志里的删除永远撤不回来"。
        Map<String, Object> before = persistable(entity, read(target.getBeforeImage()));
        String op = target.getOperation() == null ? "" : target.getOperation();
        if (DataChangeEntity.OP_CREATE.equals(op)) {
            // 撤销新建 = 软删这条记录
            crudExecutor.delete(entity, target.getRecordId(), tenantCode);
            return;
        }
        if (DataChangeEntity.OP_DELETE.equals(op)) {
            // 撤销删除 = 复位 deleted, 再把前像写回去 (防止删除与之后又被改过混在一起)
            crudExecutor.restore(entity, target.getRecordId(), tenantCode);
            if (!before.isEmpty()) {
                crudExecutor.update(entity, target.getRecordId(), dto(entity, appCode, tenantCode, before), actor);
            }
            return;
        }
        if (before.isEmpty()) {
            throw new IllegalStateException("变更日志缺少前像, 无法安全撤销 (id=" + target.getId() + ")");
        }
        crudExecutor.update(entity, target.getRecordId(), dto(entity, appCode, tenantCode, before), actor);
    }

    private void applyForward(EntityDefDTO entity, DataChangeEntity target,
                              String tenantCode, String appCode, String actor) {
        Map<String, Object> after = persistable(entity, read(target.getAfterImage()));
        String op = target.getOperation() == null ? "" : target.getOperation();
        if (DataChangeEntity.OP_CREATE.equals(op)) {
            if (after.isEmpty()) {
                throw new IllegalStateException("变更日志缺少后像, 无法重做新建 (id=" + target.getId() + ")");
            }
            // 重做新建会拿到新的自增主键 —— 这是自增 id 下无法回避的, 日志里保留原 recordId 以便追溯
            crudExecutor.create(entity, dto(entity, appCode, tenantCode, after), actor);
            return;
        }
        if (DataChangeEntity.OP_DELETE.equals(op)) {
            crudExecutor.delete(entity, target.getRecordId(), tenantCode);
            return;
        }
        if (after.isEmpty()) {
            throw new IllegalStateException("变更日志缺少后像, 无法重做 (id=" + target.getId() + ")");
        }
        crudExecutor.update(entity, target.getRecordId(), dto(entity, appCode, tenantCode, after), actor);
    }

    private RuntimeCrudDTO dto(EntityDefDTO entity, String appCode, String tenantCode, Map<String, Object> values) {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setEntityCode(entity.getEntityCode());
        dto.setAppCode(appCode);
        dto.setTenantCode(tenantCode);
        dto.setFieldValues(new HashMap<String, Object>(values));
        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> read(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return map == null ? new LinkedHashMap<String, Object>() : map;
        } catch (Exception ex) {
            log.warn("failed to parse change image: {}", ex.getMessage());
            return new LinkedHashMap<String, Object>();
        }
    }

    /** 撤销 / 重做的结果. */
    public static class UndoOutcome {
        private final Long changeId;
        private final String operation;
        private final Long recordId;
        private final boolean applied;
        private final String reason;

        public UndoOutcome(Long changeId, String operation, Long recordId, boolean applied, String reason) {
            this.changeId = changeId;
            this.operation = operation;
            this.recordId = recordId;
            this.applied = applied;
            this.reason = reason;
        }

        static UndoOutcome nothing(String reason) {
            return new UndoOutcome(null, null, null, false, reason);
        }

        public Long getChangeId() {
            return changeId;
        }

        public String getOperation() {
            return operation;
        }

        public Long getRecordId() {
            return recordId;
        }

        public boolean isApplied() {
            return applied;
        }

        public String getReason() {
            return reason;
        }

        /** 给接口层看的简述. */
        public String describe() {
            return applied ? ("已撤销 " + operation + " (记录 " + recordId + ")") : reason;
        }

        public List<String> toLogLine() {
            List<String> out = new ArrayList<String>();
            out.add(String.valueOf(changeId));
            out.add(operation);
            out.add(String.valueOf(recordId));
            return out;
        }
    }
}
