package com.zifang.z.lc.sdk.spi.sign.adapter;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;

import java.util.Map;

/**
 * 签名任务 RPC 适配器接口 — 蒸馏自 ace-platform-engine {@code AssignServiceAdapter}
 * （{@code com.c2f.ace.engine.adapter}），字段语义完全对齐.
 *
 * <p>本接口是 {@link com.zifang.z.lc.sdk.spi.sign.AssignService} 的 RPC 风格对应物 —
 * 用于「业务方实现 AssignService SPI → 引擎暴露 RPC 接口给跨进程调用方」场景.
 *
 * @author xuhf (distilled by zifang)
 */
public interface AssignServiceAdapter {

    /**
     * 添加电子签名任务（返回签名任务二维码）— 含自动签名.
     */
    SignJobResultExtendDTO addSignJob(AddSignExtendDTO addSignExtendDTO);

    /**
     * 查询签名结果.
     */
    ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO querySignResultExtendDTO);

    /**
     * 验签.
     */
    Boolean verifySignedData(VerifySignExtendDTO verifySignExtendDTO);

    /**
     * 获取签名数据.
     */
    Map<String, Object> getSignedData(AssignDataExtendDTO assignDataExtendDTO);
}
