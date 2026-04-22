package com.ruoyi.service.impl.zhpg;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.CalcExecutionRequest;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.service.zhpg.ICalcFlowTemplateService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.service.zhpg.IEvalResultService;
import com.ruoyi.service.zhpg.IIndicatorTreeWeightService;
import com.ruoyi.service.zhpg.IObjectiveWeightService;
import com.ruoyi.service.zhpg.IReportTemplateService;
import com.ruoyi.mapper.zhpg.CalcTaskStageLogMapper;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CalcTaskServiceImplTest {

    @Test
    public void resolveIndicatorSystemSupportsRequirementIdOnly() throws Exception {
        IEvalIndicatorSystemService indicatorSystemService = mock(IEvalIndicatorSystemService.class);
        EvalIndicatorSystem system = new EvalIndicatorSystem();
        system.setId(14L);
        system.setRequirementId(40L);
        system.setSystemName("通信对抗试验评估指标体系");
        when(indicatorSystemService.getByRequirementId(40L)).thenReturn(system);

        CalcTaskServiceImpl service = new CalcTaskServiceImpl(
                mock(CalcTaskStageLogMapper.class),
                mock(ICalcFlowTemplateService.class),
                indicatorSystemService,
                mock(IIndicatorTreeWeightService.class),
                mock(IObjectiveWeightService.class),
                mock(IAlgorithmInfoService.class),
                mock(IEvalResultService.class),
                mock(IReportTemplateService.class),
                mock(XxlJobAdminClient.class)
        );

        CalcExecutionRequest request = new CalcExecutionRequest();
        request.setRequirementId(40L);

        Method method = CalcTaskServiceImpl.class.getDeclaredMethod("resolveIndicatorSystem", CalcExecutionRequest.class);
        method.setAccessible(true);
        EvalIndicatorSystem resolved = (EvalIndicatorSystem) method.invoke(service, request);

        assertEquals(Long.valueOf(14L), resolved.getId());
        assertEquals(Long.valueOf(14L), request.getIndicatorSystemId());
    }

    @Test
    public void resolveIndicatorSystemRejectsMismatchedRequirementId() throws Exception {
        IEvalIndicatorSystemService indicatorSystemService = mock(IEvalIndicatorSystemService.class);
        EvalIndicatorSystem system = new EvalIndicatorSystem();
        system.setId(14L);
        system.setRequirementId(40L);
        when(indicatorSystemService.getById(14L)).thenReturn(system);

        CalcTaskServiceImpl service = new CalcTaskServiceImpl(
                mock(CalcTaskStageLogMapper.class),
                mock(ICalcFlowTemplateService.class),
                indicatorSystemService,
                mock(IIndicatorTreeWeightService.class),
                mock(IObjectiveWeightService.class),
                mock(IAlgorithmInfoService.class),
                mock(IEvalResultService.class),
                mock(IReportTemplateService.class),
                mock(XxlJobAdminClient.class)
        );

        CalcExecutionRequest request = new CalcExecutionRequest();
        request.setIndicatorSystemId(14L);
        request.setRequirementId(41L);

        Method method = CalcTaskServiceImpl.class.getDeclaredMethod("resolveIndicatorSystem", CalcExecutionRequest.class);
        method.setAccessible(true);
        try {
            method.invoke(service, request);
            fail("expected ServiceException");
        } catch (InvocationTargetException ex) {
            assertTrue(ex.getTargetException() instanceof ServiceException);
            assertEquals("所选指标体系与需求ID不匹配", ex.getTargetException().getMessage());
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            fail("condition was false");
        }
    }
}
