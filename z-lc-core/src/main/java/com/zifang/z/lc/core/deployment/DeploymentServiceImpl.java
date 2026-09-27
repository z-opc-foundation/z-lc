package com.zifang.z.lc.core.deployment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import com.zifang.z.lc.common.dto.ProvisionReport;
import com.zifang.z.lc.core.app.AppAdminService;
import com.zifang.z.lc.core.deployment.entity.DeploymentEntity;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import com.zifang.z.lc.core.materialize.mapper.MaterializationMapper;
import com.zifang.z.lc.core.schema.SchemaAdminService;
import com.zifang.z.lc.mapper.deployment.DeploymentMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger log = LogManager.getLogger(DeploymentServiceImpl.class);

    /** {@code deploy_log} 里最多逐支点名多少支没建成 —— 计数永远是全量，逐支只列前若干条。 */
    private static final int LOG_ITEM_LIMIT = 20;

    @Resource
    private DeploymentMapper deploymentMapper;

    @Resource
    private AppAdminService appAdminService;

    @Resource
    private SchemaAdminService schemaAdminService;

    @Resource
    private MaterializationMapper materializationMapper;

    @Override
    public DeploymentDTO createDeployment(DeploymentCreateReq req) {
        if (req == null) {
            throw new IllegalArgumentException("部署请求不能为空");
        }
        DeploymentEntity entity = new DeploymentEntity();
        BeanUtils.copyProperties(req, entity);
        if (entity.getTenantCode() == null) {
            entity.setTenantCode("default");
        }
        requireExecutable(entity);

        entity.setStatus(DeploymentEntity.STATUS_PENDING);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        deploymentMapper.insert(entity);
        log.info("Deployment created: app={} type={}", entity.getAppCode(), entity.getDeployType());
        return toDTO(execute(entity.getId()));
    }

    /**
     * 当场把这次部署执行掉，并把结局写回这一行。
     * <p>
     * 为什么不照 {@code MaterializationService.trigger} 那个"先 insert PENDING 再调 @Async 方法"的形状抄：
     * 全仓没有 {@code @EnableAsync}，而 {@code trigger} 又是在自己方法里直接调 {@code runAsync(...)}
     * （自调用不走代理），于是那句 @Async 是装饰 —— 实测 08:00:10 一次物化触发，
     * 响应体写 {@code PENDING} 而同一秒库里已经是 {@code READY}/6 个文件（缺陷 #71）。
     * 这里既然没有第二个能干的执行器，就<strong>同步干完再回答</strong>，回答的是库里的真状态。
     */
    private DeploymentEntity execute(Long id) {
        DeploymentEntity row = deploymentMapper.selectById(id);
        try {
            ProvisionReport report = schemaAdminService.provisionAllTables(row.getTenantCode(), row.getAppCode());
            row.setStatus(report.isAllOk() ? DeploymentEntity.STATUS_SUCCESS : DeploymentEntity.STATUS_FAILED);
            row.setDeployLog(summarize(row.getAppCode(), report));
        } catch (RuntimeException ex) {
            row.setStatus(DeploymentEntity.STATUS_FAILED);
            row.setDeployLog("应用 [" + row.getAppCode() + "] 的部署没跑完：provision 调用抛了 " + ex);
            log.warn("Deployment {} (app={}) failed", id, row.getAppCode(), ex);
        }
        row.setUpdateTime(new Date());
        deploymentMapper.updateById(row);
        return row;
    }

    /**
     * 这条部署有没有可能被执行。三件事缺一件就不要留这一行：方式没有执行器、应用不存在、
     * 指向的物化批次不是这个应用这个租户的（缺陷 #70：原先这三样全收，返回 200 + 一行永远 PENDING 的账）。
     */
    private void requireExecutable(DeploymentEntity target) {
        String appCode = target.getAppCode();
        if (appCode == null || appCode.trim().isEmpty()) {
            throw new IllegalArgumentException("缺少 appCode：部署得说出部署哪个应用");
        }
        DeploymentTypes.validateForWrite(target.getDeployType());
        if (appAdminService.getAppByCode(target.getTenantCode(), appCode) == null) {
            throw new IllegalArgumentException("租户 [" + target.getTenantCode() + "] 下没有应用 ["
                    + appCode + "]，没有定义可以部署");
        }
        if (target.getMaterializationId() != null) {
            requireBatchOfThisApp(target);
        }
    }

    private void requireBatchOfThisApp(DeploymentEntity target) {
        MaterializationEntity batch = materializationMapper.selectById(target.getMaterializationId());
        if (batch == null) {
            throw new IllegalArgumentException("物化批次 " + target.getMaterializationId()
                    + " 不存在：这条部署指向一个没有的批次");
        }
        if (!target.getTenantCode().equals(batch.getTenantCode())
                || !target.getAppCode().equals(batch.getAppCode())) {
            throw new IllegalArgumentException("物化批次 " + target.getMaterializationId() + " 属于租户 ["
                    + batch.getTenantCode() + "] 的应用 [" + batch.getAppCode()
                    + "]，不能挂在租户 [" + target.getTenantCode() + "] 的应用 [" + target.getAppCode() + "] 的部署上");
        }
    }

    private String summarize(String appCode, ProvisionReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("应用 [").append(appCode).append("] 的定义已应用到运行时库：")
                .append(report.getTotal()).append(" 个实体 —— 新建 ").append(report.getCreated())
                .append(" 张，补列 ").append(report.getAltered())
                .append(" 张，本来就是对的 ").append(report.getUnchanged())
                .append(" 张，没建成 ").append(report.getFailedCount()).append(" 张");
        int shown = 0;
        int hidden = 0;
        for (ProvisionReport.Item item : report.getItems()) {
            if (ProvisionReport.CREATED.equals(item.getStatus())
                    || ProvisionReport.EXISTS_INTACT.equals(item.getStatus())) {
                continue;
            }
            if (shown++ >= LOG_ITEM_LIMIT) {
                hidden++;
                continue;
            }
            sb.append("\n- ").append(item.getEntityCode()).append(" → ").append(item.getTableName())
                    .append(" [").append(item.getStatus()).append("] ");
            if (ProvisionReport.ALTERED.equals(item.getStatus())) {
                sb.append("补了 ").append(item.getAddedColumns());
            } else {
                sb.append(item.getMessage());
            }
        }
        if (hidden > 0) {
            sb.append("\n- …另有 ").append(hidden).append(" 支未逐条列出");
        }
        return sb.toString();
    }

    @Override
    public DeploymentDTO updateDeploymentStatus(Long id, String status, String deployLog) {
        DeploymentEntity entity = deploymentMapper.selectById(id);
        if (entity == null) {
            return null;
        }

        entity.setStatus(status);
        if (deployLog != null) {
            entity.setDeployLog(deployLog);
        }

        entity.setUpdateTime(new Date());
        deploymentMapper.updateById(entity);
        return toDTO(entity);
    }

    @Override
    public DeploymentDTO getDeployment(Long id) {
        DeploymentEntity entity = deploymentMapper.selectById(id);
        return entity == null ? null : toDTO(entity);
    }

    @Override
    public List<DeploymentDTO> listDeploymentsByApp(String tenantCode, String appCode) {
        List<DeploymentEntity> list = deploymentMapper.selectList(
                new QueryWrapper<DeploymentEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .orderByDesc("id"));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public DeploymentDTO getLatestDeployment(String tenantCode, String appCode) {
        DeploymentEntity entity = deploymentMapper.selectOne(
                new QueryWrapper<DeploymentEntity>()
                        .eq("tenant_code", tenantCode)
                        .eq("app_code", appCode)
                        .orderByDesc("id")
                        .last("LIMIT 1"));
        return entity == null ? null : toDTO(entity);
    }

    private DeploymentDTO toDTO(DeploymentEntity e) {
        DeploymentDTO dto = new DeploymentDTO();
        BeanUtils.copyProperties(e, dto);
        return dto;
    }
}
