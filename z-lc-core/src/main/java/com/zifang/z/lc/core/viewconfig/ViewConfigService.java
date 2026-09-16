package com.zifang.z.lc.core.viewconfig;

import com.zifang.z.lc.common.dto.ViewConfigCreateReq;
import com.zifang.z.lc.common.dto.ViewConfigDTO;
import com.zifang.z.lc.common.dto.ViewConfigUpdateReq;

import java.util.List;

/**
 * 视图配置服务接口 (F035 T5: View Config)
 */
public interface ViewConfigService {

    ViewConfigDTO createViewConfig(ViewConfigCreateReq req);

    ViewConfigDTO updateViewConfig(ViewConfigUpdateReq req);

    int deleteViewConfig(Long id);

    ViewConfigDTO getViewConfig(String tenantCode, String appCode, String entityCode, String viewType);

    List<ViewConfigDTO> listViewConfigs(String tenantCode, String appCode, String entityCode);

    List<ViewConfigDTO> listViewConfigsByApp(String tenantCode, String appCode);
}
