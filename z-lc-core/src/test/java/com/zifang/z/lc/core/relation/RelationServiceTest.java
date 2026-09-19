package com.zifang.z.lc.core.relation;

import com.zifang.z.lc.common.dto.RelationCreateReq;
import com.zifang.z.lc.common.dto.RelationDTO;
import com.zifang.z.lc.common.dto.RelationUpdateReq;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * RelationService 服务接口契约测试
 */
public class RelationServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(RelationService.class.isInterface());
    }

    @Test
    public void shouldDeclareCreateRelation() throws NoSuchMethodException {
        Method m = RelationService.class.getMethod("createRelation", RelationCreateReq.class);
        assertEquals(RelationDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateRelation() throws NoSuchMethodException {
        Method m = RelationService.class.getMethod("updateRelation", RelationUpdateReq.class);
        assertEquals(RelationDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareDeleteRelation() throws NoSuchMethodException {
        Method m = RelationService.class.getMethod("deleteRelation", Long.class);
        assertEquals(int.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListRelationsByEntity() throws NoSuchMethodException {
        Method m = RelationService.class.getMethod(
                "listRelationsByEntity", String.class, String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListRelationsByApp() throws NoSuchMethodException {
        Method m = RelationService.class.getMethod("listRelationsByApp", String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetRelation() throws NoSuchMethodException {
        Method m = RelationService.class.getMethod("getRelation", Long.class);
        assertEquals(RelationDTO.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.relation",
                RelationService.class.getPackage().getName());
    }
}