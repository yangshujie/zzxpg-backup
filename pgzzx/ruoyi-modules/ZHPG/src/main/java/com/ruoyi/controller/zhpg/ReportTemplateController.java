package com.ruoyi.controller.zhpg;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.domain.zhpg.dto.*;
import com.ruoyi.service.zhpg.IReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 报告模板Controller（简化版：无版本管理）
 */
@RestController
@RequestMapping("/zhpg/report/templates")
public class ReportTemplateController extends BaseController {

    @Autowired
    private IReportTemplateService templateService;

    @PostMapping
    public AjaxResult create(@Validated @RequestBody ReportTemplateCreateRequest request) {
        return AjaxResult.success(templateService.createTemplate(request));
    }

    @GetMapping("/list")
    public AjaxResult list() {
        return AjaxResult.success(templateService.listTemplates());
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable("id") Long id) {
        return AjaxResult.success(templateService.getDetail(id));
    }

    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable("id") Long id,
                             @Validated @RequestBody ReportTemplateUpdateRequest request) {
        return AjaxResult.success(templateService.updateMeta(id, request));
    }

    @PutMapping("/{id}/content")
    public AjaxResult updateContent(@PathVariable("id") Long id, @RequestBody Map<String, String> request) {
        String htmlContent = request.get("htmlContent");
        return AjaxResult.success(templateService.updateTemplateContent(id, htmlContent));
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        templateService.deleteTemplate(id);
        return AjaxResult.success();
    }

    @PostMapping("/{id}/preview")
    public AjaxResult preview(@PathVariable("id") Long id,
                             @RequestBody(required = false) ReportTemplateRenderRequest request) {
        ReportTemplateRenderRequest safeRequest = request == null ? new ReportTemplateRenderRequest() : request;
        Map<String, Object> result = new HashMap<>();
        result.put("html", templateService.preview(id, safeRequest));
        return AjaxResult.success(result);
    }

    @PostMapping("/{id}/render/html")
    public ResponseEntity<byte[]> renderHtml(@PathVariable("id") Long id,
                                            @RequestBody(required = false) ReportTemplateRenderRequest request) {
        ReportTemplateRenderRequest safeRequest = request == null ? new ReportTemplateRenderRequest() : request;
        ReportDownloadData data = templateService.renderHtmlDownload(id, safeRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        UriUtils.encode(data.getFileName(), StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_TYPE, data.getContentType())
                .body(data.getBytes());
    }

    @PostMapping("/{id}/render/docx")
    public ResponseEntity<byte[]> renderDocx(@PathVariable("id") Long id,
                                            @RequestBody(required = false) ReportTemplateRenderRequest request) {
        ReportTemplateRenderRequest safeRequest = request == null ? new ReportTemplateRenderRequest() : request;
        ReportDownloadData data = templateService.renderDocxDownload(id, safeRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                        UriUtils.encode(data.getFileName(), StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_TYPE, data.getContentType())
                .body(data.getBytes());
    }

    @PostMapping("/validate")
    public AjaxResult validate(@Validated @RequestBody ReportTemplateValidateRequest request) {
        return AjaxResult.success(templateService.validateTemplate(request));
    }
}
