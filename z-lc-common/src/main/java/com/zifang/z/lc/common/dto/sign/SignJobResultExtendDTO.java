package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;

/**
 * 签名任务结果 DTO — 蒸馏自 ace-platform-engine {@code SignJobResultExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：{@code AssignService.addSignJob} 的返回类型 — 区分自动签名 vs 人工签名：
 * <ul>
 *   <li>{@link #autoSignFlag=true} — 自动签名结果在 {@link #electronicSignInfoDTO}</li>
 *   <li>{@link #autoSignFlag=false} — 人工签名扫码参数在 {@link #addSignJobResultDTO}</li>
 * </ul>
 *
 * @author xuhf (distilled by zifang)
 */
public class SignJobResultExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否自动签名. */
    private Boolean autoSignFlag;

    /** 扫码任务参数 — autoSignFlag=false 时取这个. */
    private AddSignJobResultExtendDTO addSignJobResultDTO;

    /** 签名结果任务 — autoSignFlag=true 时取这个. */
    private ElectronicSignInfoExtendDTO electronicSignInfoDTO;

    public Boolean getAutoSignFlag() {
        return autoSignFlag;
    }

    public void setAutoSignFlag(Boolean autoSignFlag) {
        this.autoSignFlag = autoSignFlag;
    }

    public AddSignJobResultExtendDTO getAddSignJobResultDTO() {
        return addSignJobResultDTO;
    }

    public void setAddSignJobResultDTO(AddSignJobResultExtendDTO addSignJobResultDTO) {
        this.addSignJobResultDTO = addSignJobResultDTO;
    }

    public ElectronicSignInfoExtendDTO getElectronicSignInfoDTO() {
        return electronicSignInfoDTO;
    }

    public void setElectronicSignInfoDTO(ElectronicSignInfoExtendDTO electronicSignInfoDTO) {
        this.electronicSignInfoDTO = electronicSignInfoDTO;
    }

    /**
     * 取最终签名结果 — 优先返回自动签名结果，否则返回 null（人工签名需后续轮询）.
     */
    public ElectronicSignInfoExtendDTO resolveFinalSignInfo() {
        if (Boolean.TRUE.equals(autoSignFlag)) {
            return electronicSignInfoDTO;
        }
        return null;
    }
}
