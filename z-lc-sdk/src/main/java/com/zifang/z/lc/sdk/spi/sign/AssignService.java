package com.zifang.z.lc.sdk.spi.sign;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;

import java.util.Map;

/**
 * 签名任务服务 SPI — 蒸馏自 ace-platform-engine {@code AssignService}
 * （{@code com.c2f.ace.engine.define}），字段语义完全对齐.
 *
 * <p>用于 CA 电子签名任务的「发起 / 查询 / 验签 / 获取待签名数据」 —
 * 业务方继承 {@link AbstractAssignService} 实现本接口，内部委托 z-meta
 * 模块或第三方 CA 服务（CFCA / 法大大 / e签宝 等）.
 *
 * @author xuhf (distilled by zifang)
 */
public interface AssignService {

    /**
     * 添加电子签名任务（返回签名任务二维码）— 含自动签名.
     *
     * @param addSignJobDTO 发起签名任务入参
     * @return 签名任务结果（含自动签名标志 + 扫码参数 / 签名结果）
     */
    SignJobResultExtendDTO addSignJob(AddSignExtendDTO addSignJobDTO);

    /**
     * 查询签名结果.
     *
     * @param querySignResultDTO 查询入参（仅含 signDataId）
     * @return 签名结果（{@link ElectronicSignInfoExtendDTO}）
     */
    ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO querySignResultDTO);

    /**
     * 验签 — 验证 {@code signResult} 配合 {@code signCert} 是否真实有效.
     *
     * @param verifySignExtendDTO 验签入参
     * @return true 验签通过 / false 验签失败
     */
    Boolean verifySignedData(VerifySignExtendDTO verifySignExtendDTO);

    /**
     * 获取签名数据 — 查询待签名的表单数据.
     *
     * @param assignDataExtendDTO 查询入参
     * @return 待签名数据（Map 形式 — 通常是表单字段值）
     */
    Map<String, Object> getSignedData(AssignDataExtendDTO assignDataExtendDTO);
}
