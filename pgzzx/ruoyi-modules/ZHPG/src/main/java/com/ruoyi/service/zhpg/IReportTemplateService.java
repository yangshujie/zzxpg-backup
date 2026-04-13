package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.ReportTemplate;
import com.ruoyi.domain.zhpg.dto.*;

import java.util.List;
import java.util.Map;

/**
 * 报告模板服务接口（简化版）
 */
public interface IReportTemplateService extends IService<ReportTemplate> {

    /**
     * 创建报告模板
     */
    ReportTemplateDetailResponse createTemplate(ReportTemplateCreateRequest request);

    /**
     * 获取模板列表
     */
    List<ReportTemplate> listTemplates();

    /**
     * 获取模板详情
     */
    ReportTemplateDetailResponse getDetail(Long templateId);

    /**
     * 更新模板基本信息
     */
    ReportTemplateDetailResponse updateMeta(Long templateId, ReportTemplateUpdateRequest request);

    /**
     * 删除模板
     */
    void deleteTemplate(Long templateId);

    /**
     * 更新模板内容
     */
    ReportTemplateDetailResponse updateTemplateContent(Long templateId, String htmlContent);

    /**
     * 校验模板HTML
     */
    Map<String, Object> validateTemplate(ReportTemplateValidateRequest request);

    /**
     * 预览模板
     */
    String preview(Long templateId, ReportTemplateRenderRequest request);

    /**
     * 渲染HTML下载数据
     */
    ReportDownloadData renderHtmlDownload(Long templateId, ReportTemplateRenderRequest request);

    /**
     * 渲染DOCX下载数据
     */
    ReportDownloadData renderDocxDownload(Long templateId, ReportTemplateRenderRequest request);
}
