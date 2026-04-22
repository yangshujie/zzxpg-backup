package com.ruoyi.service.impl.zhpg;

import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.EvalResult;
import com.ruoyi.domain.zhpg.ReportTemplate;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;
import com.ruoyi.domain.zhpg.dto.ReportDownloadData;
import com.ruoyi.mapper.zhpg.EvalReportInstanceMapper;
import com.ruoyi.mapper.zhpg.EvalResultMapper;
import com.ruoyi.service.zhpg.IReportTemplateService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvalReportInstanceServiceImplTest {

    @Test
    public void generateForResultCreatesVersionedReportInstance() {
        EvalResultMapper evalResultMapper = mock(EvalResultMapper.class);
        EvalReportInstanceMapper reportMapper = mock(EvalReportInstanceMapper.class);
        IReportTemplateService reportTemplateService = mock(IReportTemplateService.class);
        EvalReportFileStorageService fileStorageService = mock(EvalReportFileStorageService.class);

        EvalResult result = new EvalResult();
        result.setId(10L);
        result.setResultCode("RES-1");
        result.setTaskId(99L);
        result.setTaskName("任务A");
        result.setScore(new BigDecimal("88.5"));
        result.setGrade("良好");
        result.setDelFlag(0);
        when(evalResultMapper.selectById(10L)).thenReturn(result);

        ReportTemplate template = new ReportTemplate();
        template.setId(7L);
        template.setTemplateName("模板A");
        template.setDeleted(0);
        when(reportTemplateService.getById(7L)).thenReturn(template);
        when(reportTemplateService.renderDocxDownload(eq(7L), any()))
                .thenReturn(new ReportDownloadData("7.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        new byte[]{1, 2, 3}));

        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(reportMapper.insert(any(EvalReportInstance.class))).thenReturn(1);
        when(evalResultMapper.updateById(any(EvalResult.class))).thenReturn(1);
        when(fileStorageService.uploadEvalReport(eq(99L), eq("RES-1_v1"), eq("docx"), any()))
                .thenReturn("zhpg/evalReport/202604/calc99_RES-1_v1.docx");

        EvalReportInstanceServiceImpl service = new EvalReportInstanceServiceImpl(
                reportMapper, evalResultMapper, reportTemplateService, fileStorageService, null);

        EvalReportGenerateRequest request = new EvalReportGenerateRequest();
        request.setReportTemplateId(7L);
        request.setFields(Collections.singletonMap("extraConclusion", "可用于归档"));

        EvalReportInstance generated = service.generateForResult(10L, request);

        assertEquals(Long.valueOf(10L), generated.getEvalResultId());
        assertEquals(Long.valueOf(99L), generated.getCalcTaskId());
        assertEquals(Long.valueOf(7L), generated.getReportTemplateId());
        assertEquals("模板A", generated.getReportTemplateName());
        assertEquals(Integer.valueOf(1), generated.getGenerationNo());
        assertEquals("SUCCESS", generated.getRenderStatus());
        assertEquals("zhpg/evalReport/202604/calc99_RES-1_v1.docx", generated.getWordUrl());
        assertTrue(generated.getMappingJson().contains("extraConclusion"));

        ArgumentCaptor<EvalReportInstance> captor = ArgumentCaptor.forClass(EvalReportInstance.class);
        verify(reportMapper).insert(captor.capture());
        assertEquals("RES-1_v1", captor.getValue().getReportCode());
    }
}
