package com.zifang.z.lc.core.deployment;

import com.zifang.z.lc.common.dto.DeploymentCreateReq;
import com.zifang.z.lc.common.dto.DeploymentDTO;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * DeploymentService 服务接口契约测试
 */
public class DeploymentServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(DeploymentService.class.isInterface());
    }

    @Test
    public void shouldDeclareCreateDeployment() throws NoSuchMethodException {
        Method m = DeploymentService.class.getMethod("createDeployment", DeploymentCreateReq.class);
        assertEquals(DeploymentDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareUpdateDeploymentStatus() throws NoSuchMethodException {
        Method m = DeploymentService.class.getMethod(
                "updateDeploymentStatus", Long.class, String.class, String.class);
        assertEquals(DeploymentDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetDeployment() throws NoSuchMethodException {
        Method m = DeploymentService.class.getMethod("getDeployment", Long.class);
        assertEquals(DeploymentDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareListDeploymentsByApp() throws NoSuchMethodException {
        Method m = DeploymentService.class.getMethod("listDeploymentsByApp", String.class, String.class);
        assertEquals(List.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetLatestDeployment() throws NoSuchMethodException {
        Method m = DeploymentService.class.getMethod("getLatestDeployment", String.class, String.class);
        assertEquals(DeploymentDTO.class, m.getReturnType());
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.core.deployment",
                DeploymentService.class.getPackage().getName());
    }
}