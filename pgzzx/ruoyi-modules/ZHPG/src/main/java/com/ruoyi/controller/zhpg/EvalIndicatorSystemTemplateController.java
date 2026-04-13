package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystemTemplate;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 指标体系模板Controller
 */
@Api(value = "指标体系模板管理")
@RestController
@RequestMapping("/zhpg/indicatorSystemTemplate")
public class EvalIndicatorSystemTemplateController extends BaseController {

    @Autowired
    private IEvalIndicatorSystemTemplateService templateService;

    @ApiOperation("分页查询指标体系模板列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalIndicatorSystemTemplate query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<?> page = new Page<>(pageNum, pageSize);
        Page<EvalIndicatorSystemTemplate> result = templateService.selectTemplatePage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("获取指标体系模板详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(templateService.getById(id));
    }

    @ApiOperation("新增指标体系模板")
    @Log(title = "指标体系模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EvalIndicatorSystemTemplate template) {
        if (!templateService.checkTemplateCodeUnique(template)) {
            return AjaxResult.error("新增模板'" + template.getTemplateName() + "'失败，模板编号已存在");
        }
        template.setCreateBy(SecurityUtils.getUsername());
        template.setCreateTime(new Date());
        return toAjax(templateService.insertTemplate(template));
    }

    @ApiOperation("修改指标体系模板")
    @Log(title = "指标体系模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EvalIndicatorSystemTemplate template) {
        if (!templateService.checkTemplateCodeUnique(template)) {
            return AjaxResult.error("修改模板'" + template.getTemplateName() + "'失败，模板编号已存在");
        }
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return toAjax(templateService.updateTemplate(template));
    }

    @ApiOperation("删除指标体系模板")
    @Log(title = "指标体系模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(templateService.deleteTemplateByIds(ids));
    }

    @ApiOperation("发布指标体系模板")
    @Log(title = "指标体系模板", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        EvalIndicatorSystemTemplate template = templateService.getById(id);
        if (template == null) {
            return AjaxResult.error("模板不存在");
        }
        template.setStatus("PUBLISHED");
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return toAjax(templateService.updateById(template));
    }

    @ApiOperation("导出指标体系模板JSON")
    @GetMapping("/{id}/exportJson")
    public AjaxResult exportJson(@PathVariable Long id) {
        EvalIndicatorSystemTemplate template = templateService.getById(id);
        if (template == null) {
            return AjaxResult.error("模板不存在");
        }
        return AjaxResult.success(template);
    }

    @ApiOperation("导入指标体系模板JSON")
    @Log(title = "指标体系模板", businessType = BusinessType.IMPORT)
    @PostMapping("/importJson")
    public AjaxResult importJson(@RequestBody EvalIndicatorSystemTemplate template) {
        template.setId(null);
        template.setStatus("DRAFT");
        template.setCreateBy(SecurityUtils.getUsername());
        template.setCreateTime(new Date());
        return toAjax(templateService.insertTemplate(template));
    }

    @ApiOperation("导出指标体系模板Excel")
    @Log(title = "指标体系模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvalIndicatorSystemTemplate query) {
        Page<EvalIndicatorSystemTemplate> exportPage = new Page<>(1, 10000);
        List<EvalIndicatorSystemTemplate> list = templateService.selectTemplatePage(exportPage, query).getRecords();
        ExcelUtil<EvalIndicatorSystemTemplate> util = new ExcelUtil<>(EvalIndicatorSystemTemplate.class);
        util.exportExcel(response, list, "指标体系模板数据");
    }
}
