package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.domain.zhpg.CalcTask;
import com.ruoyi.domain.zhpg.dto.CalcExecutionRequest;
import com.ruoyi.service.zhpg.ICalcTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "综合分析计算-执行任务")
@RestController
@RequestMapping("/zhpg/calc")
public class CalcTaskController extends BaseController {

    private final ICalcTaskService calcTaskService;

    public CalcTaskController(ICalcTaskService calcTaskService) {
        this.calcTaskService = calcTaskService;
    }

    @ApiOperation("分页查询计算任务")
    @GetMapping("/task/list")
    public TableDataInfo list(CalcTask query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<CalcTask> result = calcTaskService.selectTaskPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询计算任务详情")
    @GetMapping("/task/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(calcTaskService.selectTaskDetail(id));
    }

    @ApiOperation("发起一次综合分析计算（异步）")
    @Log(title = "综合分析计算", businessType = BusinessType.INSERT)
    @PostMapping("/run")
    public AjaxResult run(@Valid @RequestBody CalcExecutionRequest request) {
        return AjaxResult.success(calcTaskService.run(request));
    }

    @ApiOperation("查询任务执行状态（供前端轮询）")
    @GetMapping("/task/{id}/status")
    public AjaxResult getStatus(@PathVariable Long id) {
        return AjaxResult.success(calcTaskService.getTaskStatus(id));
    }
}
