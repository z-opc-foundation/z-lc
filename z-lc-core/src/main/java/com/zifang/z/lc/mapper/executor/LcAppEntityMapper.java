package com.zifang.z.lc.mapper.executor;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.executor.entity.AppEntity;

/**
 * z-lc 的 AppEntity Mapper.
 * <p>
 * 命名说明: 原名 {@code AppMapper} 与 z-ctc 的
 * {@code com.zifang.ctc.core.domain.mapper.AppMapper} 同名, 触发 Spring bean
 * 命名冲突 ({@code ConflictingBeanDefinitionException}), 详见
 * {@code _feature/008_z-script 脚本平台 HTTP→MCP/feature008.md} P3. 改名为 {@code LcAppEntityMapper}
 * 后, 默认 bean 名变为 {@code lcAppEntityMapper}, 与 z-ctc 的 {@code appMapper} 不再冲突.
 * <p>
 * Spring bean name: {@code lcAppEntityMapper} (按 MyBatis-Plus 规则: 类名首字母小写).
 */
public interface LcAppEntityMapper extends BaseMapper<AppEntity> {
}
