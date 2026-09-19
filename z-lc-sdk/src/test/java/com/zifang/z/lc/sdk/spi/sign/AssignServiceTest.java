package com.zifang.z.lc.sdk.spi.sign;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AssignService (sign) SPI 接口单元测试
 */
public class AssignServiceTest {

    @Test
    public void shouldBeInterface() {
        assertTrue(AssignService.class.isInterface());
    }

    @Test
    public void shouldDeclareAddSignJobMethod() throws NoSuchMethodException {
        Method m = AssignService.class.getMethod("addSignJob", AddSignExtendDTO.class);
        assertEquals(SignJobResultExtendDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareQuerySignResultMethod() throws NoSuchMethodException {
        Method m = AssignService.class.getMethod("querySignResult", QuerySignResultExtendDTO.class);
        assertEquals(ElectronicSignInfoExtendDTO.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareVerifySignedDataMethod() throws NoSuchMethodException {
        Method m = AssignService.class.getMethod("verifySignedData", VerifySignExtendDTO.class);
        assertEquals(Boolean.class, m.getReturnType());
    }

    @Test
    public void shouldDeclareGetSignedDataMethod() throws NoSuchMethodException {
        Method m = AssignService.class.getMethod("getSignedData", AssignDataExtendDTO.class);
        assertEquals(Map.class, m.getReturnType());
    }

    @Test
    public void anonymousImplShouldSatisfyInterface() {
        AssignService impl = new AssignService() {
            @Override
            public SignJobResultExtendDTO addSignJob(AddSignExtendDTO dto) {
                return new SignJobResultExtendDTO();
            }

            @Override
            public ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO dto) {
                return new ElectronicSignInfoExtendDTO();
            }

            @Override
            public Boolean verifySignedData(VerifySignExtendDTO dto) {
                return true;
            }

            @Override
            public Map<String, Object> getSignedData(AssignDataExtendDTO dto) {
                return new HashMap<>();
            }
        };
        assertNotNull(impl);
        assertTrue(impl instanceof AssignService);
    }

    @Test
    public void anonymousImplShouldReturnTrueOnVerify() {
        AssignService impl = new AssignService() {
            @Override
            public SignJobResultExtendDTO addSignJob(AddSignExtendDTO dto) { return null; }
            @Override
            public ElectronicSignInfoExtendDTO querySignResult(QuerySignResultExtendDTO dto) { return null; }
            @Override
            public Boolean verifySignedData(VerifySignExtendDTO dto) { return true; }
            @Override
            public Map<String, Object> getSignedData(AssignDataExtendDTO dto) { return null; }
        };
        Boolean r = impl.verifySignedData(new VerifySignExtendDTO());
        assertNotNull(r);
        assertTrue(r);
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.sign",
                AssignService.class.getPackage().getName());
    }
}