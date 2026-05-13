package com.ruoyi.service.impl.zhpg;

import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.EvalResult;
import com.ruoyi.domain.zhpg.ReportTemplate;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;
import com.ruoyi.domain.zhpg.dto.ReportDownloadData;
import com.ruoyi.domain.zhpg.dto.ReportTemplateRenderRequest;
import com.ruoyi.common.report.IndicatorReportSectionBuilder;
import com.ruoyi.mapper.zhpg.EvalReportInstanceMapper;
import com.ruoyi.mapper.zhpg.EvalResultMapper;
import com.ruoyi.service.zhpg.IReportTemplateService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
        when(evalResultMapper.selectById(10L)).thenReturn(result);

        ReportTemplate template = new ReportTemplate();
        template.setId(7L);
        template.setTemplateName("模板A");
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

    @Test
    public void generateForResultBuildsChartFieldsFromStoredPayload() {
        EvalResultMapper evalResultMapper = mock(EvalResultMapper.class);
        EvalReportInstanceMapper reportMapper = mock(EvalReportInstanceMapper.class);
        IReportTemplateService reportTemplateService = mock(IReportTemplateService.class);
        EvalReportFileStorageService fileStorageService = mock(EvalReportFileStorageService.class);

        EvalResult result = new EvalResult();
        result.setId(11L);
        result.setResultCode("RES-CHART");
        result.setTaskId(100L);
        result.setTaskName("图表任务");
        result.setScore(new BigDecimal("88.5"));
        result.setGrade("good");
        result.setDimensions("[{\"label\":\"通信干扰能力\",\"value\":88.5,\"tone\":\"good\"}]");
        result.setReportPayloadJson("{\"scoredIndicatorTree\":{\"label\":\"root\",\"children\":[{\"label\":\"通信干扰能力\",\"score\":88.5,\"children\":[{\"label\":\"压制成功率\",\"score\":92,\"evalValue\":92,\"referenceValue\":80,\"unit\":\"%\",\"tone\":\"excellent\"}]}]}}");
        when(evalResultMapper.selectById(11L)).thenReturn(result);

        ReportTemplate template = new ReportTemplate();
        template.setId(8L);
        template.setTemplateName("图表模板");
        when(reportTemplateService.getById(8L)).thenReturn(template);
        when(reportTemplateService.renderDocxDownload(eq(8L), any()))
                .thenReturn(new ReportDownloadData("8.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        new byte[]{1, 2, 3}));
        when(reportMapper.selectCount(any())).thenReturn(0L);
        when(reportMapper.insert(any(EvalReportInstance.class))).thenReturn(1);
        when(evalResultMapper.updateById(any(EvalResult.class))).thenReturn(1);
        when(fileStorageService.uploadEvalReport(eq(100L), eq("RES-CHART_v1"), eq("docx"), any()))
                .thenReturn("zhpg/evalReport/202604/calc100_RES-CHART_v1.docx");

        EvalReportInstanceServiceImpl service = new EvalReportInstanceServiceImpl(
                reportMapper, evalResultMapper, reportTemplateService, fileStorageService, null);

        EvalReportGenerateRequest request = new EvalReportGenerateRequest();
        request.setReportTemplateId(8L);
        service.generateForResult(11L, request);

        ArgumentCaptor<ReportTemplateRenderRequest> captor = ArgumentCaptor.forClass(ReportTemplateRenderRequest.class);
        verify(reportTemplateService).renderDocxDownload(eq(8L), captor.capture());
        Map<String, Object> fields = captor.getValue().getFields();
        assertTrue(String.valueOf(fields.get("CapabilityRadarChart")).startsWith("data:image/png;base64,"));
        List<?> sections = (List<?>) fields.get("IndicatorSections");
        assertEquals(2, sections.size());
        IndicatorReportSectionBuilder.Section leaf = (IndicatorReportSectionBuilder.Section) sections.get(1);
        assertTrue(leaf.getChartImg().startsWith("data:image/png;base64,"));
    }

    @Test
    public void selectReportPageFiltersByManagementFields() {
        EvalResultMapper evalResultMapper = mock(EvalResultMapper.class);
        EvalReportInstanceMapper reportMapper = mock(EvalReportInstanceMapper.class);
        IReportTemplateService reportTemplateService = mock(IReportTemplateService.class);
        EvalReportFileStorageService fileStorageService = mock(EvalReportFileStorageService.class);
        EvalReportInstanceServiceImpl service = new EvalReportInstanceServiceImpl(
                reportMapper, evalResultMapper, reportTemplateService, fileStorageService, null);

        EvalReportInstance query = new EvalReportInstance();
        query.setReportCode("RES-1");
        query.setReportTemplateName("模板A");
        query.setRenderStatus("SUCCESS");
        query.setEvalResultId(10L);
        query.setCalcTaskId(99L);

        Page<EvalReportInstance> page = new Page<>(1, 10);
        Page<EvalReportInstance> expected = new Page<>(1, 10);
        when(reportMapper.selectPage(eq(page), any(QueryWrapper.class))).thenReturn(expected);

        Page<EvalReportInstance> actual = service.selectReportPage(page, query);

        assertEquals(expected, actual);
        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(reportMapper).selectPage(eq(page), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("report_code"));
        assertTrue(sqlSegment.contains("report_template_name"));
        assertTrue(sqlSegment.contains("render_status"));
        assertTrue(sqlSegment.contains("eval_result_id"));
        assertTrue(sqlSegment.contains("calc_task_id"));
        assertTrue(sqlSegment.contains("ORDER BY generation_no DESC,create_time DESC"));
    }

    @Test
    public void deleteReportByIdsRemovesInstances() {
        EvalResultMapper evalResultMapper = mock(EvalResultMapper.class);
        EvalReportInstanceMapper reportMapper = mock(EvalReportInstanceMapper.class);
        IReportTemplateService reportTemplateService = mock(IReportTemplateService.class);
        EvalReportFileStorageService fileStorageService = mock(EvalReportFileStorageService.class);
        EvalReportInstanceServiceImpl service = new EvalReportInstanceServiceImpl(
                reportMapper, evalResultMapper, reportTemplateService, fileStorageService, null);

        when(reportMapper.deleteBatchIds(Arrays.asList(1L, 2L))).thenReturn(2);

        assertEquals(2, service.deleteReportByIds(new Long[]{1L, 2L}));
        verify(reportMapper).deleteBatchIds(Arrays.asList(1L, 2L));
    }
}
