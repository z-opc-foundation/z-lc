package com.zifang.z.lc.mapper.workflow;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.workflow.entity.WorkflowFireEntity;

/**
 * z_lc_workflow_fire Mapper（流程发起的结局账）.
 * <p>
 * 注意: 新增 mapper 包必须同时登记到 {@code LcModuleDataSource} 的 @MapperScan basePackages,
 * 否则编译能过、运行时才报 no bean。这里复用已登记的 {@code mapper.workflow} 包。
 */
public interface WorkflowFireMapper extends BaseMapper<WorkflowFireEntity> {
}
