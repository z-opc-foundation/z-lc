package com.zifang.z.lc.core.vapi;

import com.zifang.z.lc.common.dto.VapiCreateReq;
import com.zifang.z.lc.common.dto.VapiDTO;
import com.zifang.z.lc.common.dto.VapiUpdateReq;

import java.util.List;

/** 虚拟接口注册表服务 (V3)。 */
public interface VapiService {

    VapiDTO createVapi(VapiCreateReq req);

    VapiDTO updateVapi(VapiUpdateReq req);

    void deleteVapi(Long id);

    VapiDTO getVapi(Long id);

    List<VapiDTO> listByApp(String appCode);

    /** 分发前查找: appCode + method + path; 未启用/不存在返回 null。path 归一化由实现负责。 */
    VapiDTO resolve(String appCode, String method, String path);
}
