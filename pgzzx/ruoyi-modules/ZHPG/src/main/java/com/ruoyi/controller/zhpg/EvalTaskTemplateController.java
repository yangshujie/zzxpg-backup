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
import com.ruoyi.domain.zhpg.EvalTaskTemplate;
import com.ruoyi.service.zhpg.IEvalTaskTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;

/**
 * 评估任务模板Controller（评估任务构建分系统 RWGJ）
 */
@Api(value = "评估任务构建-任务模板管理")
@RestController
@RequestMapping("/zhpg/evalTaskTemplate")
public class EvalTaskTemplateController extends BaseController {

    @Autowired
    private IEvalTaskTemplateService templateService;

    @ApiOperation("分页查询任务模板列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalTaskTemplate query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<EvalTaskTemplate> result = templateService.selectTemplatePage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询任务模板详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(templateService.selectTemplateDetail(id));
    }

    @ApiOperation("新增任务模板")
    @Log(title = "任务模板管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody EvalTaskTemplate template) {
        template.setCreateBy(SecurityUtils.getUsername());
        template.setCreateTime(new Date());
        templateService.insertTemplate(template);
        return AjaxResult.success(template);
    }

    @ApiOperation("修改任务模板")
    @Log(title = "任务模板管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody EvalTaskTemplate template) {
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return toAjax(templateService.updateTemplate(template));
    }

    @ApiOperation("删除任务模板")
    @Log(title = "任务模板管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(templateService.deleteTemplateByIds(ids));
    }

    @ApiOperation("停用模板（PUBLISHED -> DISABLED）")
    @Log(title = "任务模板管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/disable")
    public AjaxResult disable(@PathVariable Long id) {
        return toAjax(templateService.disableTemplate(id));
    }

    @ApiOperation("启用模板（DISABLED/TESTING -> PUBLISHED）")
    @Log(title = "任务模板管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/enable")
    public AjaxResult enable(@PathVariable Long id) {
        return toAjax(templateService.enableTemplate(id));
    }
}
