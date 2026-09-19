package com.zifang.z.lc.sdk.spi.sign;

import com.zifang.z.lc.common.dto.sign.AddSignExtendDTO;
import com.zifang.z.lc.common.dto.sign.AssignDataExtendDTO;
import com.zifang.z.lc.common.dto.sign.ElectronicSignInfoExtendDTO;
import com.zifang.z.lc.common.dto.sign.QuerySignResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.SignJobResultExtendDTO;
import com.zifang.z.lc.common.dto.sign.VerifySignExtendDTO;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * AbstractAssignService 抽象基类单元测试
 */
public class AbstractAssignServiceTest {

    @Test
    public void shouldBeAbstractClass() {
        assertTrue("AbstractAssignService 应当是 abstract",
                Modifier.isAbstract(AbstractAssignService.class.getModifiers()));
    }

    @Test
    public void shouldImplementAssignService() {
        assertTrue("AbstractAssignService 应当实现 AssignService",
                AssignService.class.isAssignableFrom(AbstractAssignService.class));
    }

    @Test
    public void shouldBeInCorrectPackage() {
        assertEquals("com.zifang.z.lc.sdk.spi.sign",
                AbstractAssignService.class.getPackage().getName());
    }

    @Test
    public void defaultAddSignJobShouldReturnNull() {
        AbstractAssignService svc = new AbstractAssignService() {};
        SignJobResultExtendDTO result = svc.addSignJob(new AddSignExtendDTO());
        assertNull(result);
    }

    @Test
    public void defaultAddSignJobShouldHandleNullDTO() {
        AbstractAssignService svc = new AbstractAssignService() {};
        SignJobResultExtendDTO result = svc.addSignJob(null);
        assertNull(result);
    }

    @Test
    public void defaultQuerySignResultShouldReturnNull() {
        AbstractAssignService svc = new AbstractAssignService() {};
        ElectronicSignInfoExtendDTO result = svc.querySignResult(new QuerySignResultExtendDTO());
        assertNull(result);
    }

    @Test
    public void defaultQuerySignResultShouldHandleNullDTO() {
        AbstractAssignService svc = new AbstractAssignService() {};
        ElectronicSignInfoExtendDTO result = svc.querySignResult(null);
        assertNull(result);
    }

    @Test
    public void defaultVerifySignedDataShouldReturnFalse() {
        AbstractAssignService svc = new AbstractAssignService() {};
        Boolean result = svc.verifySignedData(new VerifySignExtendDTO());
        assertNotNull(result);
        assertFalse(result);
    }

    @Test
    public void defaultVerifySignedDataShouldHandleNullDTO() {
        AbstractAssignService svc = new AbstractAssignService() {};
        Boolean result = svc.verifySignedData(null);
        assertNotNull(result);
        assertFalse(result);
    }

    @Test
    public void defaultGetSignedDataShouldReturnEmptyMap() {
        AbstractAssignService svc = new AbstractAssignService() {};
        Map<String, Object> result = svc.getSignedData(new AssignDataExtendDTO());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void defaultGetSignedDataShouldHandleNullDTO() {
        AbstractAssignService svc = new AbstractAssignService() {};
        Map<String, Object> result = svc.getSignedData(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void subclassCanOverrideAddSignJob() {
        AbstractAssignService svc = new AbstractAssignService() {
            @Override
            public SignJobResultExtendDTO addSignJob(AddSignExtendDTO dto) {
                SignJobResultExtendDTO r = new SignJobResultExtendDTO();
                r.setAutoSignFlag(Boolean.TRUE);
                return r;
            }
        };
        SignJobResultExtendDTO r = svc.addSignJob(new AddSignExtendDTO());
        assertNotNull(r);
        assertEquals(Boolean.TRUE, r.getAutoSignFlag());
    }

    @Test
    public void subclassCanOverrideVerifySignedData() {
        AbstractAssignService svc = new AbstractAssignService() {
            @Override
            public Boolean verifySignedData(VerifySignExtendDTO dto) {
                return Boolean.TRUE;
            }
        };
        assertTrue(svc.verifySignedData(new VerifySignExtendDTO()));
    }
}