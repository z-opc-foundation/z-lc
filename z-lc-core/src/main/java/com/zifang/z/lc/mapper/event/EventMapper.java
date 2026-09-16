package com.zifang.z.lc.mapper.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.event.entity.EventEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * z_lc_event 表 Mapper
 */
public interface EventMapper extends BaseMapper<EventEntity> {

    /**
     * 取某个 (tenant, app) 下最大的 apply_seq (用于事件溯源序号分配)
     */
    @Select("SELECT IFNULL(MAX(apply_seq), 0) FROM z_lc_event " +
            "WHERE tenant_code = #{tenantCode} AND app_code = #{appCode}")
    Long maxApplySeq(@Param("tenantCode") String tenantCode,
                     @Param("appCode") String appCode);
}