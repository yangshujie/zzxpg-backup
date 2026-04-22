package com.ruoyi.controller.zhpg;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
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

    @ApiOperation("查询某个评估结果的报告生成历史")
    @GetMapping("/result/{evalResultId}/list")
    public AjaxResult listByResult(@PathVariable Long evalResultId) {
        return AjaxResult.success(reportInstanceService.listByResult(evalResultId));
    }

    @ApiOperation("基于评估结果生成一版报告")
    @PostMapping("/result/{evalResultId}")
    public AjaxResult generate(@PathVariable Long evalResultId,
                               @Validated @RequestBody EvalReportGenerateRequest request) {
        return AjaxResult.success(reportInstanceService.generateForResult(evalResultId, request));
    }

    @ApiOperation("获取某一版报告的预览和下载链接")
    @GetMapping("/{reportId}/links")
    public AjaxResult links(@PathVariable Long reportId) {
        return AjaxResult.success(reportInstanceService.getReportLinks(reportId));
    }
}
