package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;
import com.ruoyi.service.zhpg.IEvalReportInstanceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Api(value = "评估报告生成实例")
@RestController
@RequestMapping("/zhpg/evalReport")
public class EvalReportInstanceController extends BaseController {

    @Autowired
    private IEvalReportInstanceService reportInstanceService;

    @ApiOperation("分页查询评估报告生成实例")
    @GetMapping("/list")
    public TableDataInfo list(EvalReportInstance query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if (pageNum == null) pageNum = 1;
        if (pageSize == null) pageSize = 10;
        Page<EvalReportInstance> page = new Page<>(pageNum, pageSize);
        Page<EvalReportInstance> result = reportInstanceService.selectReportPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询某个评估结果的报告生成历史")
    @GetMapping("/result/{evalResultId}/list")
    public AjaxResult listByResult(@PathVariable Long evalResultId) {
        return AjaxResult.success(reportInstanceService.listByResult(evalResultId));
    }

    @ApiOperation("基于评估结果生成一版报告（异步，返回 PENDING 实例）")
    @PostMapping("/result/{evalResultId}")
    public AjaxResult generate(@PathVariable Long evalResultId,
                               @Validated @RequestBody EvalReportGenerateRequest request) {
        return AjaxResult.success(reportInstanceService.generateForResult(evalResultId, request));
    }

    @ApiOperation("快速渲染 HTML 预览（不生成 DOCX/PDF）")
    @PostMapping("/result/{evalResultId}/preview")
    public AjaxResult preview(@PathVariable Long evalResultId,
                              @Validated @RequestBody EvalReportGenerateRequest request) {
        return AjaxResult.success(reportInstanceService.previewHtml(evalResultId, request));
    }

    @ApiOperation("查询某一版报告的生成进度")
    @GetMapping("/{reportId}/progress")
    public AjaxResult progress(@PathVariable Long reportId) {
        return AjaxResult.success(reportInstanceService.getProgress(reportId));
    }

    @ApiOperation("获取某一版报告的预览和下载链接")
    @GetMapping("/{reportId}/links")
    public AjaxResult links(@PathVariable Long reportId) {
        return AjaxResult.success(reportInstanceService.getReportLinks(reportId));
    }

    @ApiOperation("删除评估报告生成实例")
    @Log(title = "评估报告管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(reportInstanceService.deleteReportByIds(ids));
    }
}
