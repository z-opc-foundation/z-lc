package com.zifang.z.lc.core.deployment;

import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;

import java.util.List;

/**
 * 部署管理服务接口 (F035 T7: Deployment)
 */
public interface DeploymentService {

    DeploymentDTO createDeployment(DeploymentCreateReq req);

    DeploymentDTO updateDeploymentStatus(Long id, String status, String deployLog);

    DeploymentDTO getDeployment(Long id);

    List<DeploymentDTO> listDeploymentsByApp(String tenantCode, String appCode);

    DeploymentDTO getLatestDeployment(String tenantCode, String appCode);
}
