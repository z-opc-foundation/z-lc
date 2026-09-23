package com.zifang.z.lc.core.deleter;

import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.core.executor.RuntimeCrudExecutor;
import com.zifang.z.lc.core.undo.UndoService;
import com.zifang.z.lc.core.undo.entity.DataChangeEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端批量软删：**先整批预检，有一行不能删就一行都不删**。
 * <p>
 * 预检顺手把每条的前像捞出来（本来也要查存在性），成功后再逐条写 undo 日志，
 * 所以「批量删除」和「单条删除」撤销起来是同一套语义。
 * <p>
 * ⚠ 关于原子性，说清楚（和批量导入同一个口径）：这里给的是**预检层面的 all-or-nothing**，
 * 不是数据库事务 —— {@link RuntimeCrudExecutor} 自己 {@code new JdbcTemplate(dataSource)}，
 * 不受 Spring {@code @Transactional} 管辖。所以写入途中真炸了（约束、连接断开、
 * 或某条在预检和写入之间被并发删掉导致受影响行数为 0）走的是**补偿回滚**：
 * 把本批已软删的 id 用 {@link RuntimeCrudExecutor#restore} 还原，并把 rolledBack 标出来。
 */
@Service
public class RuntimeBatchDeleteService {

    private static final Logger log = LogManager.getLogger(RuntimeBatchDeleteService.class);

    @Autowired
    private RuntimeCrudExecutor crudExecutor;

    @Autowired
    private UndoService undoService;

    public BatchDeleteDto.Result deleteBatch(EntityDefDTO entity, BatchDeleteDto.Request request, String actor) {
        BatchDeleteDto.Result result = new BatchDeleteDto.Result();
        List<Long> ids = dedupe(request.getIds());
        result.setTotal(ids.size());

        if (ids.isEmpty()) {
            result.setApplied(false);
            result.setMessage("没有要删除的记录");
            return result;
        }

        List<BatchDeleteDto.RowError> errors = new ArrayList<BatchDeleteDto.RowError>();
        Map<Long, Map<String, Object>> beforeImages = new LinkedHashMap<Long, Map<String, Object>>();
        for (int index = 0; index < ids.size(); index++) {
            Long id = ids.get(index);
            try {
                Map<String, Object> row = crudExecutor.get(entity, id, request.getTenantCode());
                if (row == null) {
                    errors.add(new BatchDeleteDto.RowError(index, id, "记录 " + id + " 不存在或已被删除"));
                } else {
                    beforeImages.put(id, row);
                }
            } catch (RuntimeException ex) {
                errors.add(new BatchDeleteDto.RowError(index, id, messageOf(ex)));
            }
        }

        if (!errors.isEmpty()) {
            result.setApplied(false);
            result.setErrors(cap(errors));
            result.setMessage("有 " + errors.size() + " 条记录不能删除，整批未删除");
            return result;
        }

        List<Long> deleted = new ArrayList<Long>();
        try {
            for (Long id : ids) {
                int affected = crudExecutor.delete(entity, id, request.getTenantCode());
                if (affected != 1) {
                    // 预检刚读到过这条，此刻却删不动 —— 只可能是中间被人并发删了。
                    // 不能装成成功：那会让"已删除 N 条"里的 N 比实际多，多出来的那几条根本查不到。
                    throw new IllegalStateException("记录 " + id + " 未被删除（可能已被他人删除）");
                }
                deleted.add(id);
            }
        } catch (RuntimeException ex) {
            result.setRolledBack(compensate(entity, request, deleted));
            result.setApplied(false);
            result.setDeletedCount(0);
            result.setMessage("删除中断：" + messageOf(ex)
                    + (result.isRolledBack() ? "，已回滚本批 " + deleted.size() + " 条" : "，回滚未完成，请人工核对"));
            log.error("batch delete aborted for entity={}, deleted={}, rolledBack={}",
                    entity.getEntityCode(), deleted.size(), result.isRolledBack(), ex);
            return result;
        }

        // 和批量导入同一条理由：变更日志在另一个数据源，不跟主写入一起回滚，
        // 所以必须整批成功之后再记，否则回滚了会留下指向没删成的记录的幽灵日志。
        for (Long id : deleted) {
            undoService.record(request.getTenantCode(), request.getAppCode(), entity, id,
                    DataChangeEntity.OP_DELETE, beforeImages.get(id), null, actor);
        }

        result.setApplied(true);
        result.setDeletedCount(deleted.size());
        result.setIds(deleted);
        result.setMessage("已删除 " + deleted.size() + " 条");
        return result;
    }

    /** 还原本批已软删的 id。全部还原成功才返回 true —— 部分失败必须让人看见。 */
    private boolean compensate(EntityDefDTO entity, BatchDeleteDto.Request request, List<Long> deleted) {
        boolean clean = true;
        for (Long id : deleted) {
            try {
                if (crudExecutor.restore(entity, id, request.getTenantCode()) != 1) {
                    clean = false;
                    log.warn("compensating restore changed no rows for entity={} id={}", entity.getEntityCode(), id);
                }
            } catch (RuntimeException ex) {
                clean = false;
                log.warn("compensating restore failed for entity={} id={}: {}",
                        entity.getEntityCode(), id, ex.getMessage());
            }
        }
        return clean;
    }

    /** 去重且保持提交顺序：重复 id 会让第二条 delete 命中 0 行，把整批无辜地拖进回滚。 */
    private static List<Long> dedupe(List<Long> ids) {
        List<Long> out = new ArrayList<Long>();
        if (ids == null) {
            return out;
        }
        Map<Long, Boolean> seen = new LinkedHashMap<Long, Boolean>();
        for (Long id : ids) {
            if (id == null || seen.containsKey(id)) {
                continue;
            }
            seen.put(id, Boolean.TRUE);
            out.add(id);
        }
        return out;
    }

    private static String messageOf(RuntimeException ex) {
        String message = ex.getMessage();
        Throwable cause = ex.getCause();
        if ((message == null || message.trim().isEmpty()) && cause != null) {
            message = cause.getMessage();
        }
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }

    private static List<BatchDeleteDto.RowError> cap(List<BatchDeleteDto.RowError> errors) {
        if (errors.size() <= BatchDeleteDto.MAX_ERRORS_RETURNED) {
            return errors;
        }
        return new ArrayList<BatchDeleteDto.RowError>(errors.subList(0, BatchDeleteDto.MAX_ERRORS_RETURNED));
    }
}
