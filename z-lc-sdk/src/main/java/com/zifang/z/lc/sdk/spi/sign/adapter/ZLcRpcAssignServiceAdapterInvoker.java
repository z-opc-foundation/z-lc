package com.zifang.z.lc.sdk.spi.sign.adapter;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;
import com.zifang.z.lc.sdk.spi.sign.AssignServiceInfo;

import java.util.Map;

/**
 * RPC 签名服务适配器 — 客户端 Invoker 抽象 — 蒸馏自 ace-platform-engine
 * {@code RpcAssignServiceAdapterInvoker} （{@code com.c2f.ace.engine.adapter}）.
 *
 * <p>本类是 AssignServiceAdapter 的「客户端代理」 — 本地业务代码调用签名方法时，
 * 实际进入这里，由子类（具体 RPC 协议）执行跨进程调用.
 *
 * @author zifang
 */
public abstract class ZLcRpcAssignServiceAdapterInvoker extends AbstractRpcAssignServiceAdapter {

    private final String identityCode;

    protected ZLcRpcAssignServiceAdapterInvoker(String identityCode) {
        this.identityCode = identityCode;
    }

    public String getIdentityCode() {
        return identityCode;
    }

    @Override
    public final SignJobResultExtendDTO addSignJob(AddSignExtendDTO addSignExtendDTO) {
        return (SignJobResultExtendDTO) invokeRemote("addSignJob", new Object[]{addSignExtendDTO});
    }

    @Override
    public final ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO querySignResultExtendDTO) {
        return (ElectronicSignInfoExtendDTO) invokeRemote("querySignResult", new Object[]{querySignResultExtendDTO});
    }

    @Override
    public final Boolean verifySignedData(VerifySignExtendDTO verifySignExtendDTO) {
        return (Boolean) invokeRemote("verifySignedData", new Object[]{verifySignExtendDTO});
    }

    @Override
    public final Map<String, Object> getSignedData(AssignDataExtendDTO assignDataExtendDTO) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) invokeRemote("getSignedData", new Object[]{assignDataExtendDTO});
        return result;
    }

    /**
     * 子类实现 — 执行实际 RPC 调用.
     */
    protected abstract Object invokeRemote(String methodName, Object[] args);

    /**
     * 子类实现 — 获取远程版本号（默认从 identityCode 派生，子类可覆盖）.
     */
    protected String deriveVersion() {
        return version(identityCode);
    }
}
