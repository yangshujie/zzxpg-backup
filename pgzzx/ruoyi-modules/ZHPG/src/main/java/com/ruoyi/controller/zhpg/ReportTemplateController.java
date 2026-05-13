package com.ruoyi.controller.zhpg;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.domain.zhpg.dto.*;
import com.ruoyi.service.zhpg.IReportTemplateService;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Autowired
    private RemoteFileService remoteFileService;

    @Value("${custom-config.minio.bucketName}")
    private String bucketName;

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

    /**
     * 上传报告自定义图片
     */
    @PostMapping("/uploadImage")
    public AjaxResult uploadImage(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }
        // 存储路径：zhpg/reportImages/yyyyMM/
        String dirPath = "zhpg/reportImages/" + new SimpleDateFormat("yyyyMM").format(new Date()) + "/";
        
        R<SysFile> result = remoteFileService.upload(bucketName, dirPath, file);
        if (result != null && result.isSuccess() && result.getData() != null) {
            AjaxResult ajax = AjaxResult.success("上传成功");
            ajax.put("fileName", result.getData().getUrl());
            ajax.put("url", result.getData().getUrl());
            return ajax;
        }
        return AjaxResult.error("上传失败: " + (result != null ? result.getMsg() : "文件服务无响应"));
    }
}
