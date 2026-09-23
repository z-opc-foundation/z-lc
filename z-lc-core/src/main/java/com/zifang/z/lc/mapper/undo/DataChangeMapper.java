package com.zifang.z.lc.mapper.undo;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.undo.entity.DataChangeEntity;

/**
 * z_lc_data_change Mapper (变更日志 / undo-redo).
 * <p>
 * 注意: 新增 mapper 包必须同时登记到 {@code LcModuleDataSource} 的 @MapperScan basePackages,
 * 否则编译能过、运行时才报 no bean.
 */
public interface DataChangeMapper extends BaseMapper<DataChangeEntity> {
}
