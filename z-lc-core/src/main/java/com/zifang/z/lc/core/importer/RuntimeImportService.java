package com.zifang.z.lc.core.importer;

import com.zifang.z.lc.common.dto.DictItemDTO;
import com.zifang.z.lc.common.dto.EntityDefDTO;
import com.zifang.z.lc.common.dto.FieldDefDTO;
import com.zifang.z.lc.common.dto.RuntimeCrudDTO;
import com.zifang.z.lc.core.dict.DictAdminService;
import com.zifang.z.lc.core.executor.RuntimeCrudExecutor;
import com.zifang.z.lc.core.pipeline.Pipeline;
import com.zifang.z.lc.core.undo.UndoService;
import com.zifang.z.lc.core.undo.entity.DataChangeEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 服务端批量导入：preview（全量校验、零写入）+ commit（先校验、再写）。
 * <p>
 * 校验走的是和单条 create **完全同一条路径**（{@link Pipeline#preWrite}）：
 * 必填、类型转换、字典、引用检查都不另立第二套口径 —— 否则"预览说能过、真写却失败"
 * 这种最难受的偏差一定会出现。
 * <p>
 * ⚠ 关于原子性，说清楚：commit 提供的保证是**"只要有一行不合法就整批不写"**，
 * 这是校验层面的 all-or-nothing，确实成立。它**不是**数据库事务：
 * {@link RuntimeCrudExecutor} 自己 {@code new JdbcTemplate(dataSource)}，
 * 不受 Spring 的 {@code @Transactional} 管辖。因此万一在逐行插入途中出现
 * 意料之外的库错误（约束、连接断开），这里做的是**补偿回滚**：把本批已插入的 id 软删掉，
 * 并在结果里把 rolledBack 标出来。要真正的事务，得让 RuntimeCrudExecutor 走
 * TransactionAwareDataSourceProxy —— 那是另一个改动，不在这里偷偷宣称。
 */
@Service
public class RuntimeImportService {

    private static final Logger log = LogManager.getLogger(RuntimeImportService.class);

    @Autowired
    private Pipeline pipeline;

    @Autowired
    private RuntimeCrudExecutor crudExecutor;

    @Autowired
    private UndoService undoService;

    @Autowired
    private DictAdminService dictAdminService;

    /** 只校验，不写任何东西。 */
    public ImportDto.Result preview(EntityDefDTO entity, ImportDto.Request request) {
        ImportDto.Result result = new ImportDto.Result();
        List<ImportDto.RowError> errors = new ArrayList<ImportDto.RowError>();
        List<ImportDto.RowWarning> warnings = new ArrayList<ImportDto.RowWarning>();
        Map<String, Set<String>> codeCache = new LinkedHashMap<String, Set<String>>();
        // 整批解析一次执行链: 逐行查配置表的话 2000 行就是 2000 次查询, 而且中途改配置会让
        // 一批数据一半按旧链校验、一半按新链校验。preview 与 commit 各自解析, 但两边口径相同,
        // 所以"预览说能过"不会在真写时才翻车。
        Pipeline.Chain chain = pipeline.writeChain(request.getAppCode(), entity, Pipeline.BEFORE_CREATE);
        int valid = 0;
        for (int index = 0; index < request.getRecords().size(); index++) {
            Map<String, Object> record = request.getRecords().get(index);
            try {
                RuntimeCrudDTO dto = dtoFor(entity, request, record);
                chain.run(entity, dto);
                valid++;
                collectDictWarnings(entity, request, index, dto.getFieldValues(), codeCache, warnings);
            } catch (RuntimeException ex) {
                errors.add(new ImportDto.RowError(index, messageOf(ex)));
            }
        }
        result.setTotal(request.getRecords().size());
        result.setValidCount(valid);
        result.setApplied(false);
        result.setErrors(cap(errors));
        result.setWarnings(capWarnings(warnings));
        result.setMessage(describe(result.getTotal(), valid, errors.size(), warnings.size(), false));
        return result;
    }

    /**
     * 落库。任何一行校验不过就整批不写；写途中出错则补偿回滚本批已插入的行。
     */
    public ImportDto.Result commit(EntityDefDTO entity, ImportDto.Request request, String actor) {
        ImportDto.Result result = new ImportDto.Result();
        List<ImportDto.RowError> errors = new ArrayList<ImportDto.RowError>();
        List<ImportDto.RowWarning> warnings = new ArrayList<ImportDto.RowWarning>();
        Map<String, Set<String>> codeCache = new LinkedHashMap<String, Set<String>>();
        List<RuntimeCrudDTO> prepared = new ArrayList<RuntimeCrudDTO>();

        Pipeline.Chain chain = pipeline.writeChain(request.getAppCode(), entity, Pipeline.BEFORE_CREATE);
        for (int index = 0; index < request.getRecords().size(); index++) {
            Map<String, Object> record = request.getRecords().get(index);
            try {
                RuntimeCrudDTO dto = dtoFor(entity, request, record);
                chain.run(entity, dto);
                prepared.add(dto);
                collectDictWarnings(entity, request, index, dto.getFieldValues(), codeCache, warnings);
            } catch (RuntimeException ex) {
                errors.add(new ImportDto.RowError(index, messageOf(ex)));
            }
        }

        result.setTotal(request.getRecords().size());
        result.setValidCount(prepared.size());

        if (!errors.isEmpty()) {
            // 校验层面的 all-or-nothing：有一行坏就整批不写，不留半截数据
            result.setApplied(false);
            result.setErrors(cap(errors));
            result.setMessage("存在 " + errors.size() + " 行校验失败，整批未写入");
            return result;
        }

        List<Long> inserted = new ArrayList<Long>();
        List<RuntimeCrudDTO> writtenDtos = new ArrayList<RuntimeCrudDTO>();
        try {
            // (id, 写入值) 成对收集：分成两个 list 的话，一旦某行没拿到 id 就会整体错位，
            // 日志会记到别的记录上。
            for (RuntimeCrudDTO dto : prepared) {
                Long id = crudExecutor.create(entity, dto, actor);
                if (id != null) {
                    inserted.add(id);
                    writtenDtos.add(dto);
                }
            }
        } catch (RuntimeException ex) {
            result.setRolledBack(compensate(entity, request, inserted));
            result.setErrors(cap(errors));
            result.setApplied(false);
            result.setMessage("写入中断：" + messageOf(ex)
                    + (result.isRolledBack() ? "，已回滚本批 " + inserted.size() + " 行" : "，回滚未完成，请人工核对"));
            log.error("import aborted for entity={}, inserted={}, rolledBack={}",
                    entity.getEntityCode(), inserted.size(), result.isRolledBack(), ex);
            return result;
        }

        // 整批都写完了再记变更日志：日志写在另一个数据源上，不会跟主写入一起回滚，
        // 所以必须等成功之后再记，否则中途失败会留下指向不存在记录的"幽灵日志"。
        for (int i = 0; i < inserted.size(); i++) {
            undoService.record(request.getTenantCode(), request.getAppCode(), entity, inserted.get(i),
                    DataChangeEntity.OP_CREATE, null, writtenDtos.get(i).getFieldValues(), actor);
        }

        result.setApplied(true);
        result.setInsertedCount(inserted.size());
        result.setIds(inserted);
        result.setErrors(cap(errors));
        result.setWarnings(capWarnings(warnings));
        // 说"已导入 N 行"只能用真正拿到 id 的行数：create 返回 null 的行没有 id，
        // 拿校验通过数来报会多报，而多报的那几行用户根本看不见。
        result.setMessage(describe(result.getTotal(), inserted.size(), 0, warnings.size(), true));
        return result;
    }

    private boolean compensate(EntityDefDTO entity, ImportDto.Request request, List<Long> inserted) {
        if (inserted.isEmpty()) {
            return true;
        }
        boolean clean = true;
        for (Long id : inserted) {
            try {
                crudExecutor.delete(entity, id, request.getTenantCode());
            } catch (RuntimeException ex) {
                clean = false;
                log.warn("compensating delete failed for entity={} id={}: {}",
                        entity.getEntityCode(), id, ex.getMessage());
            }
        }
        return clean;
    }

    /**
     * 字典列**不校验值域**是现状（`stage="NOPE"` 能写进去）。这里不改成拒绝：
     * 字典项后来被删会让存量数据瞬间"非法"，存量库导入也常带未登记的码，硬校验的破坏性更大。
     * 所以只在 preview / commit 结果里回一条 warning，让导入的人自己决定要不要补字典。
     * <p>
     * 只提示不阻断；同一 (行, 列) 只报一次；一次调用内按 dictCode 缓存码表，避免每行查库。
     */
    private void collectDictWarnings(EntityDefDTO entity, ImportDto.Request request, int index,
                                     Map<String, Object> record,
                                     Map<String, Set<String>> codeCache,
                                     List<ImportDto.RowWarning> out) {
        if (entity.getFields() == null || record == null) {
            return;
        }
        for (FieldDefDTO field : entity.getFields()) {
            if (field == null || field.getDictCode() == null || field.getDictCode().isEmpty()) {
                continue;
            }
            Object value = record.get(field.getFieldCode());
            if (value == null || String.valueOf(value).trim().isEmpty()) {
                continue;
            }
            Set<String> codes = codeCache.get(field.getDictCode());
            if (codes == null) {
                codes = new HashSet<String>();
                try {
                    for (DictItemDTO item : dictAdminService.listItems(request.getTenantCode(), field.getDictCode())) {
                        if (item.getItemCode() != null) {
                            codes.add(item.getItemCode());
                        }
                    }
                } catch (RuntimeException ex) {
                    // 查不到码表时不拿假 warning 骚扰，也绝不因为这个失败整个预检
                    log.warn("dict {} lookup failed: {}", field.getDictCode(), ex.getMessage());
                    codes = null;
                }
                codeCache.put(field.getDictCode(), codes == null ? new HashSet<String>() : codes);
                if (codes == null) {
                    continue;
                }
            }
            if (!codes.isEmpty() && !codes.contains(String.valueOf(value).trim())) {
                out.add(new ImportDto.RowWarning(index, field.getFieldCode(),
                        "字段 [" + (field.getFieldName() == null ? field.getFieldCode() : field.getFieldName())
                                + "] 的值 " + value + " 不在字典 " + field.getDictCode() + " 里，界面会回落显示原始码"));
            }
        }
    }

    private RuntimeCrudDTO dtoFor(EntityDefDTO entity, ImportDto.Request request, Map<String, Object> record) {
        RuntimeCrudDTO dto = new RuntimeCrudDTO();
        dto.setEntityCode(entity.getEntityCode());
        dto.setAppCode(request.getAppCode());
        dto.setTenantCode(request.getTenantCode());
        // 拷一份：pipeline 会就地改写值（类型转换），不能污染调用方传进来的 map
        dto.setFieldValues(new LinkedHashMap<String, Object>(
                record == null ? new LinkedHashMap<String, Object>() : record));
        return dto;
    }

    private static String messageOf(RuntimeException ex) {
        String message = ex.getMessage();
        Throwable cause = ex.getCause();
        if ((message == null || message.trim().isEmpty()) && cause != null) {
            message = cause.getMessage();
        }
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }

    /** 人话结果：错误说清"没写"，警告说清"写了但要注意什么"。 */
    private static String describe(int total, int valid, int errorCount, int warningCount, boolean wrote) {
        StringBuilder sb = new StringBuilder();
        if (errorCount > 0) {
            sb.append("有 ").append(errorCount).append(" 行不合法，整批未写入");
        } else if (wrote) {
            sb.append("已导入 ").append(valid).append(" 行");
        } else {
            sb.append("校验通过 ").append(valid).append(" 行");
        }
        if (warningCount > 0) {
            sb.append("；").append(warningCount).append(" 处值不在字典中（不阻断，界面会回落显示原始码）");
        }
        return sb.toString();
    }

    private static List<ImportDto.RowWarning> capWarnings(List<ImportDto.RowWarning> warnings) {
        if (warnings.size() <= ImportDto.MAX_ERRORS_RETURNED) {
            return warnings;
        }
        return new ArrayList<ImportDto.RowWarning>(warnings.subList(0, ImportDto.MAX_ERRORS_RETURNED));
    }

    private static List<ImportDto.RowError> cap(List<ImportDto.RowError> errors) {
        if (errors.size() <= ImportDto.MAX_ERRORS_RETURNED) {
            return errors;
        }
        return new ArrayList<ImportDto.RowError>(errors.subList(0, ImportDto.MAX_ERRORS_RETURNED));
    }
}
