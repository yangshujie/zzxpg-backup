package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import com.ruoyi.service.zhpg.ICalcFlowTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

/**
 * 评估计算流程模板Controller
 */
@Api(value = "综合分析计算-流程模板管理")
@RestController
@RequestMapping("/zhpg/calcFlow")
public class CalcFlowTemplateController extends BaseController {

    @Autowired
    private ICalcFlowTemplateService templateService;

    @ApiOperation("分页查询流程模板列表")
    @GetMapping("/list")
    public TableDataInfo list(CalcFlowTemplate query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<CalcFlowTemplate> result = templateService.selectTemplatePage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询流程模板详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(templateService.selectTemplateDetail(id));
    }

    @ApiOperation("新增流程模板")
    @Log(title = "流程模板管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody CalcFlowTemplate template) {
        template.setCreateBy(SecurityUtils.getUsername());
        template.setCreateTime(new Date());
        templateService.insertTemplate(template);
        return AjaxResult.success(template);
    }

    @ApiOperation("修改流程模板")
    @Log(title = "流程模板管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody CalcFlowTemplate template) {
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return toAjax(templateService.updateTemplate(template));
    }

    @ApiOperation("删除流程模板")
    @Log(title = "流程模板管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(templateService.deleteTemplateByIds(ids));
    }

    @ApiOperation("提交测试（DRAFT -> TESTING）")
    @Log(title = "流程模板管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/test")
    public AjaxResult submitTest(@PathVariable Long id) {
        return toAjax(templateService.submitTest(id));
    }

    @ApiOperation("发布模板（TESTING -> PUBLISHED）")
    @Log(title = "流程模板管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        return toAjax(templateService.publishTemplate(id));
    }

    @ApiOperation("停用模板（PUBLISHED -> DISABLED）")
    @Log(title = "流程模板管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/disable")
    public AjaxResult disable(@PathVariable Long id) {
        return toAjax(templateService.disableTemplate(id));
    }

    @ApiOperation("启用模板（DISABLED/TESTING -> PUBLISHED）")
    @Log(title = "流程模板管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/enable")
    public AjaxResult enable(@PathVariable Long id) {
        return toAjax(templateService.enableTemplate(id));
    }

    @ApiOperation("复制新版本")
    @Log(title = "流程模板管理", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/copyVersion")
    public AjaxResult copyVersion(@PathVariable Long id) {
        CalcFlowTemplate copy = templateService.copyVersion(id);
        return AjaxResult.success(copy);
    }
}
