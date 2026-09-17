package com.zifang.z.lc.sdk.spi.sign.adapter;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;
import com.zifang.z.lc.sdk.spi.sign.AbstractAssignService;

import java.util.Map;

/**
 * RPC 签名服务适配器 — 服务端 Provider — 蒸馏自 ace-platform-engine
 * {@code RpcAssignServiceAdapterProvider} （{@code com.c2f.ace.engine.adapter}）.
 *
 * <p>本类是 AssignServiceAdapter 的「服务端实现」 — 调用方 RPC 调用时实际进入这里，
 * 然后委派给本地 {@link AbstractAssignService} 子类.
 *
 * @author zifang
 */
public class ZLcRpcAssignServiceAdapterProvider extends AbstractRpcAssignServiceAdapter {

    private final AbstractAssignService localAssignService;

    public ZLcRpcAssignServiceAdapterProvider(AbstractAssignService localAssignService) {
        this.localAssignService = localAssignService;
    }

    public AbstractAssignService getLocalAssignService() {
        return localAssignService;
    }

    @Override
    public SignJobResultExtendDTO addSignJob(AddSignExtendDTO addSignExtendDTO) {
        return localAssignService == null ? null : localAssignService.addSignJob(addSignExtendDTO);
    }

    @Override
    public ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO querySignResultExtendDTO) {
        return localAssignService == null ? null : localAssignService.querySignResult(querySignResultExtendDTO);
    }

    @Override
    public Boolean verifySignedData(VerifySignExtendDTO verifySignExtendDTO) {
        return localAssignService == null ? Boolean.FALSE : localAssignService.verifySignedData(verifySignExtendDTO);
    }

    @Override
    public Map<String, Object> getSignedData(AssignDataExtendDTO assignDataExtendDTO) {
        return localAssignService == null ? new java.util.HashMap<>() : localAssignService.getSignedData(assignDataExtendDTO);
    }
}
