package com.zifang.z.lc.common.dto.sign;

import java.io.Serializable;

/**
 * 查询签名结果 DTO — 蒸馏自 ace-platform-engine {@code QuerySignResultExtendDTO}
 * （{@code com.c2f.ace.engine.dto}），字段语义完全对齐.
 *
 * <p>用途：业务方轮询签名结果时携带的入参 — 签名数据 id.
 *
 * @author zuhf (distilled by zifang)
 */
public class QuerySignResultExtendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 签名数据 id. */
    private String signDataId;

    public String getSignDataId() {
        return signDataId;
    }

    public void setSignDataId(String signDataId) {
        this.signDataId = signDataId;
    }
}
