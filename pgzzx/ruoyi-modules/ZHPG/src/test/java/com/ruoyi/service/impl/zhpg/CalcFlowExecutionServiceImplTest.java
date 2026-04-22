package com.ruoyi.service.impl.zhpg;

import com.ruoyi.domain.zhpg.CalcFlowExecution;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionInitRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionSaveRequest;
import com.ruoyi.mapper.zhpg.CalcFlowExecutionMapper;
import com.ruoyi.service.zhpg.ICalcFlowTemplateService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CalcFlowExecutionServiceImplTest {

    @Test
    public void initCreatesDraftExecutionFromTemplatePreset() {
        CalcFlowExecutionMapper mapper = mock(CalcFlowExecutionMapper.class);
        ICalcFlowTemplateService templateService = mock(ICalcFlowTemplateService.class);
        IEvalIndicatorSystemService indicatorSystemService = mock(IEvalIndicatorSystemService.class);

        CalcFlowTemplate template = new CalcFlowTemplate();
        template.setId(11L);
        template.setTemplateName("流程模板A");
        template.setConfigJson("{\"stages\":{\"scheduleConfig\":{\"config\":{\"routeStrategy\":\"FIRST\"}}}}");
        when(templateService.selectTemplateDetail(11L)).thenReturn(template);
        EvalIndicatorSystem indicatorSystem = new EvalIndicatorSystem();
        indicatorSystem.setId(22L);
        when(indicatorSystemService.getById(22L)).thenReturn(indicatorSystem);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(CalcFlowExecution.class))).thenReturn(1);

        CalcFlowExecutionServiceImpl service = new CalcFlowExecutionServiceImpl(mapper, templateService, null, indicatorSystemService);

        CalcFlowExecutionInitRequest request = new CalcFlowExecutionInitRequest();
        request.setTemplateId(11L);
        request.setIndicatorSystemId(22L);
        request.setExecutionName("任务草稿");

        CalcFlowExecution execution = service.initExecution(request);

        assertEquals(Long.valueOf(11L), execution.getTemplateId());
        assertEquals(Long.valueOf(22L), execution.getIndicatorSystemId());
        assertEquals("DRAFT", execution.getStatus());
        assertEquals("scheduleConfig", execution.getCurrentStep());
        assertTrue(execution.getRuntimeConfigJson().contains("routeStrategy"));
        assertEquals(template.getConfigJson(), execution.getTemplateSnapshotJson());

        ArgumentCaptor<CalcFlowExecution> captor = ArgumentCaptor.forClass(CalcFlowExecution.class);
        verify(mapper).insert(captor.capture());
        assertTrue(captor.getValue().getExecutionCode().startsWith("FLOW-"));
    }

    @Test
    public void saveRuntimeConfigPersistsDraftStepState() {
        CalcFlowExecutionMapper mapper = mock(CalcFlowExecutionMapper.class);
        CalcFlowExecution existing = new CalcFlowExecution();
        existing.setId(99L);
        existing.setStatus("DRAFT");
        when(mapper.selectById(99L)).thenReturn(existing);
        when(mapper.updateById(any(CalcFlowExecution.class))).thenReturn(1);

        CalcFlowExecutionServiceImpl service = new CalcFlowExecutionServiceImpl(
                mapper, null, null, mock(IEvalIndicatorSystemService.class));

        CalcFlowExecutionSaveRequest request = new CalcFlowExecutionSaveRequest();
        request.setRuntimeConfigJson("{\"stages\":{\"scheduleConfig\":{\"config\":{\"timeoutSeconds\":60}}}}");
        request.setStepStateJson("{\"scheduleConfig\":{\"status\":\"completed\"}}");
        request.setCurrentStep("weightCalc");

        CalcFlowExecution saved = service.saveRuntimeConfig(99L, request);

        assertEquals("weightCalc", saved.getCurrentStep());
        assertEquals("CONFIGURING", saved.getStatus());
        assertTrue(saved.getRuntimeConfigJson().contains("timeoutSeconds"));
        assertTrue(saved.getStepStateJson().contains("completed"));
        verify(mapper).updateById(saved);
    }
}
