package com.zifang.z.lc.core.relation;

import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.RelationUpdateReq;

import java.util.List;

/**
 * 实体关系服务接口 (F035 T2: Entity Relation)
 */
public interface RelationService {

    RelationDTO createRelation(RelationCreateReq req);

    RelationDTO updateRelation(RelationUpdateReq req);

    int deleteRelation(Long id);

    List<RelationDTO> listRelationsByEntity(String tenantCode, String appCode, String entityCode);

    List<RelationDTO> listRelationsByApp(String tenantCode, String appCode);

    RelationDTO getRelation(Long id);
}
