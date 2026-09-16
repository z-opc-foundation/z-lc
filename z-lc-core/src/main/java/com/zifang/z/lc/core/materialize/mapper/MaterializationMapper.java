package com.zifang.z.lc.core.materialize.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.materialize.entity.MaterializationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * z_lc_materialization Mapper.
 */
@Mapper
public interface MaterializationMapper extends BaseMapper<MaterializationEntity> {

    @Select("SELECT * FROM z_lc_materialization WHERE tenant_code=#{tenantCode} AND app_code=#{appCode} " +
            "ORDER BY id DESC LIMIT #{limit}")
    List<MaterializationEntity> listByApp(String tenantCode, String appCode, int limit);

    @Select("SELECT * FROM z_lc_materialization WHERE export_version=#{exportVersion} LIMIT 1")
    MaterializationEntity getByExportVersion(String exportVersion);

    @Update("UPDATE z_lc_materialization SET status=#{status}, file_count=#{fileCount}, " +
            "error_message=#{errorMessage}, update_time=NOW() WHERE id=#{id}")
    int updateStatus(Long id, String status, Integer fileCount, String errorMessage);
}
