package com.zifang.z.lc.core.workflow;

import com.zifang.z.lc.core.adapter.CamudaAdapter;
import com.zifang.z.lc.core.workflow.entity.WorkflowBindingEntity;
import com.zifang.z.lc.core.workflow.entity.WorkflowFireEntity;
import com.zifang.z.lc.mapper.workflow.WorkflowFireMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 写后触发流程绑定：{@code z_lc_workflow_binding} 从「存起来的一张纸」变成真的发一次单（缺陷 #61）.
 * <p>
 * 这一支之前长什么样（全部实测，不是推理）：{@link WorkflowBindingService#listByEvent} 与
 * {@code CamudaAdapter.startProcess} 在生产代码里<b>零调用者</b>；而 {@code startProcess} 自己也是坏的
 * —— 它 POST 到 {@code /approval-center/process/start}，z-camuda 的真实映射是
 * {@code /api/approval-center/processes/start}（{@code ApprovalCenterController.java:57} + {@code :617}），
 * body 里发的键是 {@code processDefKey} 而 DTO 读的是 {@code processKey}
 * （{@code StartProcessRequestDTO.java:18}），成功时又把整个 {@code data} Map 的 toString 当实例 id。
 * 也就是说"把调用点加回去"是不够的：三处对不上的话，接上只会从"什么都不发生"变成"每次都失败"。
 * <p>
 * 三条口径要记住：
 * <ul>
 *   <li><b>业务写入永远不为外部引擎陪葬。</b>发起失败/超时/并发额度用尽都只落一条 FAILED 的
 *       结局行 + 一句 warn，绝不把异常抛回写路径 —— 与 #42 的"写入口拒、运行期只 warn"同一族：
 *       写入口拒是为了不让一份发不出去的绑定出生，运行期拒会把用户刚写的数据一起带走。</li>
 *   <li><b>结局要能回读。</b>每一次尝试（成功或失败）都在 {@code z_lc_workflow_fire} 里留一行，
 *       {@code GET /api/lc/workflow-binding/fires} 读得到。只写日志的话，"已保存绑定"这句话
 *       没有任何一层能证明它兑现过。</li>
 *   <li><b>等待必须有上限，而且上限要落在持有 socket 的那一层。</b>共享的 z-util-http 客户端读超时
 *       是 60s（{@code HttpExecutor.java:56}），一次挂死的 z-camuda 会把用户的"新建记录"按住 60 秒，
 *       所以这条调用走一个固定 2 槽、不排队的池，并且 {@code future.get(timeoutMs)} 到点就判 FAILED。
 *       <p>
 *       ⚠ 但"放走等的人"并不等于"腾出槽位"：那条 HTTP 调用还压在槽上直到它自己结束。契约层实测过
 *       这个形状 —— 连续两次挂死的发起就把 2 个槽占满，之后<b>每一条新记录的绑定都发不出去</b>
 *       （账上写着「并发发起已达上限」，见 {@code WorkflowTriggerContractTest#timeoutDoesNotPoisonTheNextFires}）。
 *       真正收口这件事的不是这里，而是 {@link CamudaAdapter} 那条短超时传输：预算只有一个键
 *       （{@code z-lc.workflow.dispatch-timeout-ms}），派发侧拿它等多久、传输层拿它加一点余量收尾，
 *       槽位因此在毫秒级自己回来。</li>
 * </ul>
 */
@Component
public class WorkflowTriggerDispatcher {

    private static final Logger log = LogManager.getLogger(WorkflowTriggerDispatcher.class);

    /** 一次发起最多等多久；与 {@link CamudaAdapter} 的传输预算是同一个键、同一个默认值. */
    static final long DEFAULT_TIMEOUT_MS = CamudaAdapter.DEFAULT_TIMEOUT_MS;

    @Resource
    private WorkflowBindingService bindingService;

    @Resource
    private CamudaAdapter camudaAdapter;

    @Resource
    private WorkflowFireMapper fireMapper;

    private long timeoutMs = DEFAULT_TIMEOUT_MS;
    /** 2 槽、不排队：宁可当场判"额度用尽"，也不要让一批写请求排在一条挂死的 HTTP 调用后面. */
    private ExecutorService workers = newWorkers();

    private static ExecutorService newWorkers() {
        final AtomicLong seq = new AtomicLong();
        return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new SynchronousQueue<Runnable>(),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "lc-wf-fire-" + seq.incrementAndGet());
                        t.setDaemon(true);
                        return t;
                    }
                });
    }

    void setBindingService(WorkflowBindingService bindingService) {
        this.bindingService = bindingService;
    }

    void setCamudaAdapter(CamudaAdapter camudaAdapter) {
        this.camudaAdapter = camudaAdapter;
    }

    void setFireMapper(WorkflowFireMapper fireMapper) {
        this.fireMapper = fireMapper;
    }

    /**
     * 类注释里那句"配置项可覆盖"之前只是写着，没有任何地方把它绑进来 —— 这是 #42 那一族的反面
     * （那族是有人绑没人读，这一支是有人写进注释没人绑）。
     * 契约层 {@code WorkflowTriggerContractTest} 用 250ms 起服务并断言 FAILED 行里点名的就是这个值，
     * 摘掉下面这个注解会当场红。
     * <p>
     * 键与默认值都取自 {@link CamudaAdapter}：那一边拿同一个数收住 socket，这里拿它收住等待。
     * 分成两个键就会有一个数没人对齐，而"派发等到 250ms 就放弃、传输却还能占住槽 60s"正是
     * {@link #workers} 被占满的那种走法。
     */
    @Value("${" + CamudaAdapter.TIMEOUT_PROPERTY + ":" + CamudaAdapter.DEFAULT_TIMEOUT_MS + "}")
    void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    void setWorkers(ExecutorService workers) {
        this.workers = workers;
    }

    /**
     * 一条记录写成功之后调用。这个方法<b>承诺不抛</b>：任何意外（查绑定失败、结局行写不进库）
     * 都只留一句 warn。
     *
     * @param tenantCode  这条记录所在租户；绑定的查询用同一个值（管理面今天把绑定落在 "default"，
     *                    运行时记录若属于另一个租户，这里查不到绑定 ⇒ 一条都不发，见类注释与 README 的边界）
     * @param appCode     应用编码
     * @param entityCode  实体编码
     * @param recordId    刚写入的记录主键
     * @param fieldValues 记录的字段值，作为流程变量整份交给引擎
     * @param initiator   发起人（ actor ），z-camuda 用它定 initiator/applicant/审批人
     * @return 尝试发起的条数（0 = 这个实体在这个事件上没有启用的绑定）
     */
    public int afterCreate(String tenantCode, String appCode, String entityCode, Long recordId,
                           Map<String, Object> fieldValues, String initiator) {
        if (bindingService == null || camudaAdapter == null || recordId == null
                || appCode == null || entityCode == null) {
            return 0;
        }
        List<WorkflowBindingEntity> bindings;
        try {
            bindings = bindingService.listByEvent(tenantCode, appCode, entityCode,
                    WorkflowTriggers.AFTER_CREATE);
        } catch (RuntimeException ex) {
            log.warn("流程绑定查询失败 (app={}, entity={}): {}", appCode, entityCode, ex.getMessage());
            return 0;
        }
        if (bindings == null || bindings.isEmpty()) {
            return 0;
        }

        int attempted = 0;
        for (WorkflowBindingEntity binding : bindings) {
            if (binding == null) {
                continue;
            }
            attempted++;
            CamudaAdapter.ProcessStart start = fire(binding, entityCode, recordId, fieldValues, initiator);
            record(binding, start, tenantCode, appCode, entityCode, recordId, initiator);
        }
        return attempted;
    }

    private CamudaAdapter.ProcessStart fire(final WorkflowBindingEntity binding, String entityCode,
                                        final Long recordId, final Map<String, Object> fieldValues,
                                        final String initiator) {
        final String businessKey = binding.getAppCode() + ":" + entityCode + ":" + recordId;
        final Map<String, Object> variables = new LinkedHashMap<String, Object>();
        if (fieldValues != null) {
            variables.putAll(fieldValues);
        }
        variables.put("lcAppCode", binding.getAppCode());
        variables.put("lcEntityCode", entityCode);
        variables.put("lcRecordId", recordId);
        final String processKey = binding.getProcessDefinitionKey().trim();
        final String title = entityCode + "#" + recordId;

        try {
            Future<CamudaAdapter.ProcessStart> future = workers.submit(new Callable<CamudaAdapter.ProcessStart>() {
                @Override
                public CamudaAdapter.ProcessStart call() {
                    return camudaAdapter.startProcess(processKey, businessKey, initiator, title, variables);
                }
            });
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException ex) {
                future.cancel(true);
                return CamudaAdapter.ProcessStart.failed("等待 z-camuda 响应超过 " + timeoutMs + "ms，本次没有确认发起结果");
            } catch (ExecutionException ex) {
                Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                return CamudaAdapter.ProcessStart.failed("发起时抛错: " + cause);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return CamudaAdapter.ProcessStart.failed("等待发起结果时被中断");
            }
        } catch (RejectedExecutionException ex) {
            return CamudaAdapter.ProcessStart.failed("并发发起已达上限（2 个在飞、不排队），这条记录没有发起");
        }
    }

    private void record(WorkflowBindingEntity binding, CamudaAdapter.ProcessStart start, String tenantCode,
                        String appCode, String entityCode, Long recordId, String initiator) {
        if (fireMapper == null) {
            log.info("流程绑定 [id={}] 对 {}#{} 的发起结果: {} {}", binding.getId(), entityCode, recordId,
                    start.isStarted() ? WorkflowFireEntity.STATUS_STARTED : WorkflowFireEntity.STATUS_FAILED,
                    start.isStarted() ? start.getInstanceId() : start.getFailure());
            return;
        }
        WorkflowFireEntity row = new WorkflowFireEntity();
        row.setTenantCode(tenantCode);
        row.setAppCode(appCode);
        row.setEntityCode(entityCode);
        row.setRecordId(recordId);
        row.setBindingId(binding.getId());
        row.setTriggerEvent(binding.getTriggerEvent());
        row.setProcessDefinitionKey(binding.getProcessDefinitionKey());
        row.setCreateTime(new Date());
        row.setUpdateTime(new Date());
        row.setDeleted(0);
        if (start.isStarted()) {
            row.setStatus(WorkflowFireEntity.STATUS_STARTED);
            row.setInstanceId(start.getInstanceId());
        } else {
            row.setStatus(WorkflowFireEntity.STATUS_FAILED);
            row.setDetail(clip(start.getFailure()));
        }
        try {
            fireMapper.insert(row);
        } catch (RuntimeException ex) {
            // 结局账写不进去也不能把用户的写入带走 —— 但这件事必须留下声，否则就成了"没发生过"。
            log.warn("流程发起结局行写入失败 (binding={}, entity={}, record={}, initiator={}, 结果={}): {}",
                    binding.getId(), entityCode, recordId, initiator,
                    start.isStarted() ? "STARTED instance=" + start.getInstanceId()
                            : "FAILED " + start.getFailure(), ex.getMessage());
            return;
        }
        if (!start.isStarted()) {
            log.warn("流程绑定 [id={}] 没有发起成功 (entity={}, record={}, process={}): {}",
                    binding.getId(), entityCode, recordId, binding.getProcessDefinitionKey(),
                    start.getFailure());
        }
    }

    /** detail 列是 VARCHAR(512)；截断要留得出"被截断了"的形状，不能悄悄掉尾. */
    private static String clip(String value) {
        if (value == null) {
            return "失败原因未给出";
        }
        return value.length() <= 500 ? value : value.substring(0, 500) + "…(截断)";
    }
}
