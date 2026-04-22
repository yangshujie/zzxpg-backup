package com.ruoyi.controller.zhpg;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionInitRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionRunRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionSaveRequest;
import com.ruoyi.service.zhpg.ICalcFlowExecutionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "Calc flow execution")
@RestController
@RequestMapping("/zhpg/calcFlowExecution")
public class CalcFlowExecutionController extends BaseController {

    private final ICalcFlowExecutionService executionService;

    public CalcFlowExecutionController(ICalcFlowExecutionService executionService) {
        this.executionService = executionService;
    }

    @ApiOperation("Init or restore execution")
    @PostMapping("/init")
    public AjaxResult init(@Valid @RequestBody CalcFlowExecutionInitRequest request) {
        return AjaxResult.success(executionService.initExecution(request));
    }

    @ApiOperation("Get execution detail")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(executionService.getById(id));
    }

    @ApiOperation("Get latest execution")
    @GetMapping("/latest")
    public AjaxResult latest(@RequestParam Long templateId, @RequestParam Long indicatorSystemId) {
        return AjaxResult.success(executionService.latest(templateId, indicatorSystemId));
    }

    @ApiOperation("Save runtime config")
    @Log(title = "calc flow execution", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/runtime-config")
    public AjaxResult saveRuntimeConfig(@PathVariable Long id,
                                        @RequestBody CalcFlowExecutionSaveRequest request) {
        return AjaxResult.success(executionService.saveRuntimeConfig(id, request));
    }

    @ApiOperation("Run execution")
    @Log(title = "calc flow execution", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/run")
    public AjaxResult run(@PathVariable Long id, @RequestBody CalcFlowExecutionRunRequest request) {
        return AjaxResult.success(executionService.runExecution(id, request));
    }
}
