package com.zifang.z.lc.sdk.spi.sign;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 签名任务服务 SPI 抽象基类 — 蒸馏自 ace-platform-engine {@code AbstractAssignService}
 * （{@code com.c2f.ace.engine.define}），行为完全对齐.
 *
 * <p>业务方继承本类并标 {@link AssignServiceInfo} 注解 — 默认实现仅打日志 + 返回 null/空 map，
 * 业务方按需覆盖 4 个签名方法.
 *
 * <p>典型用法（业务方接入 CFCA）：
 * <pre>{@code
 *   &#64;Service
 *   &#64;AssignServiceInfo(identityCode = "cfca", exportRpc = true)
 *   public class CfcaAssignService extends AbstractAssignService {
 *       &#64;Override
 *       public SignJobResultExtendDTO addSignJob(AddSignExtendDTO dto) {
 *           return cfcaClient.createSignJob(dto);
 *       }
 *   }
 * }</pre>
 *
 * @author xuhf (distilled by zifang)
 */
public abstract class AbstractAssignService implements AssignService {

    private static final Logger log = LogManager.getLogger(AbstractAssignService.class);

    @Override
    public SignJobResultExtendDTO addSignJob(AddSignExtendDTO addSignJobDTO) {
        log.info("AbstractAssignService.addSignJob default impl — workflowInstanceId={}, taskId={}",
                addSignJobDTO != null ? addSignJobDTO.getWorkflowInstanceId() : null,
                addSignJobDTO != null ? addSignJobDTO.getTaskId() : null);
        return null;
    }

    @Override
    public ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO querySignResultDTO) {
        log.info("AbstractAssignService.querySignResult default impl — signDataId={}",
                querySignResultDTO != null ? querySignResultDTO.getSignDataId() : null);
        return null;
    }

    @Override
    public Boolean verifySignedData(VerifySignExtendDTO verifySignExtendDTO) {
        log.info("AbstractAssignService.verifySignedData default impl — signDataId={}",
                verifySignExtendDTO != null ? verifySignExtendDTO.getSignDataId() : null);
        return Boolean.FALSE;
    }

    @Override
    public Map<String, Object> getSignedData(AssignDataExtendDTO assignDataExtendDTO) {
        log.info("AbstractAssignService.getSignedData default impl — workflowInstanceId={}, taskId={}",
                assignDataExtendDTO != null ? assignDataExtendDTO.getWorkflowInstanceId() : null,
                assignDataExtendDTO != null ? assignDataExtendDTO.getTaskId() : null);
        return new HashMap<>();
    }
}
