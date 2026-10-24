package com.zifang.z.lc.mapper.permission;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.lc.core.permission.entity.PermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * LcPermissionMapper MyBatis Mapper 接口单元测试。
 * <p>
 * ⚠ 接口名带 {@code Lc} 前缀是**生产修复的一部分**（卡 {@code TASK-20261024-113}）：
 * mapper 的 bean 名 = 首字母小写的接口简名 ⇒ 裸名 {@code PermissionMapper} 会注册出
 * bean 名 {@code permissionMapper}，而线上镜像里的 z-config-web 正好有一个
 * {@code @Resource private ZNamespacePermissionMapper permissionMapper;} 字段
 * ⇒ {@code @Resource} 按名命中了本 mapper，类型对不上 ⇒ 主壳启动即挂。
 * 详见 {@link LcPermissionMapper} 的类注释。
 */
public class LcPermissionMapperTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(LcPermissionMapper.class.isInterface());
    }

    @Test
    public void shouldExtendBaseMapper() {
        assertTrue("LcPermissionMapper 应当继承 BaseMapper<PermissionEntity>",
                BaseMapper.class.isAssignableFrom(LcPermissionMapper.class));
    }

    @Test
    public void shouldBeAnnotatedWithMapper() {
        assertNotNull("LcPermissionMapper 应当标注 @Mapper",
                LcPermissionMapper.class.getAnnotation(Mapper.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.mapper.permission",
                LcPermissionMapper.class.getPackage().getName());
    }

    /**
     * 钉住「接口简名 ⇒ bean 名」这条链：MyBatis-Spring 用 Spring 的
     * {@code AnnotationBeanNameGenerator} 命名 ⇒ bean 名 = 首字母小写的接口简名。
     * <p>
     * 这里同时钉住**旧名不得复活**：旧名 {@code PermissionMapper} 会把 bean 名变成
     * {@code permissionMapper} ⇒ 正是线上被抢走的那个名字。
     */
    @Test
    public void beanNameMustNotBeTheGenericPermissionMapper() {
        String beanName = java.beans.Introspector.decapitalize(LcPermissionMapper.class.getSimpleName());
        assertEquals("bean 名 = 首字母小写的接口简名", "lcPermissionMapper", beanName);
        assertTrue("bean 名不得是通用裸名 permissionMapper（会被别的模块的 @Resource 字段抢走）",
                !"permissionMapper".equals(beanName));
    }
}
