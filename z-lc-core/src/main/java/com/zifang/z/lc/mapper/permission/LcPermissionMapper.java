package com.zifang.z.lc.mapper.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.permission.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限项 MyBatis Mapper 接口。
 * <p>
 * ⚠ <b>为什么接口名带 {@code Lc} 前缀（卡 TASK-20261024-113）</b>：
 * MyBatis-Spring 的 {@code ClassPathMapperScanner} 用 Spring 的
 * {@code AnnotationBeanNameGenerator} 给 mapper 命名 ⇒ bean 名 = 首字母小写的接口简名。
 * 本接口早先叫 {@code PermissionMapper} ⇒ bean 名就叫 {@code permissionMapper}
 * —— 一个**极通用的裸名**，会被任何别的模块的 {@code @Resource} 字段名撞上。
 * <p>
 * 生产实证（2026-10-24，z-opc-main CrashLoop）：线上镜像里的 z-config-web
 * （{@code io.github.yuku123:z-config-web:1.0.10}，Central 发布于 2026-10-02）
 * 里的 {@code ZNamespacePermissionController} 写的是
 * {@code @Resource private ZNamespacePermissionMapper permissionMapper;}
 * ⇒ {@code @Resource} **按名优先** ⇒ 命中本模块（z-lc）注册的 {@code permissionMapper}
 * ⇒ 类型对不上 ⇒ {@code BeanNotOfRequiredTypeException: Bean named 'permissionMapper'
 * is expected to be of type '...ZNamespacePermissionMapper' but was actually of type
 * 'jdk.proxy2.$Proxy352'} ⇒ 主壳启动即挂。
 * <p>
 * 本仓 z-config 的改名（{@code cd06726}，2026-10-18）已把字段名改成
 * {@code zNamespacePermissionMapper}，但**那次改名从未发布到 Maven Central**
 * ⇒ 线上镜像吃的仍是改名前的 z-config-web。
 * <p>
 * ⇒ 所以本仓这一侧也要改名：**不给别的模块留一个可以撞的通用 bean 名**。
 * 改名后本 bean 名变成 {@code lcPermissionMapper}：
 * ① 别的模块不可能自然地写出 {@code @Resource LcPermissionMapper lcPermissionMapper}
 * 之外的撞名形状；② 即便线上旧 z-config 仍用 {@code permissionMapper} 这个字段名，
 * **按名落空后 {@code @Resource} 会按类型回退** ⇒ 命中正确的
 * {@code zNamespacePermissionMapper} ⇒ 主壳能起来（已用真实 Spring 容器实测）。
 * <p>
 * 组织级惯例对齐：z-config 的 {@code ZNamespacePermissionMapper}、z-team 的
 * {@code TeamUserPermissionMapper}、z-webide 的 {@code WebIdeUserPermissionMapper}、
 * z-opc 的 {@code FtUserPermissionMapper} —— 都带模块/命名空间前缀，
 * 本仓原先的裸 {@code PermissionMapper} 是唯一的例外。
 *
 * @see com.zifang.z.lc.web.config.LcMapperBeanNameGuardTest
 */
@Mapper
public interface LcPermissionMapper extends BaseMapper<PermissionEntity> {
}
