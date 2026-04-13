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
import com.ruoyi.domain.zhpg.EvalIndicatorTemplate;
import com.ruoyi.service.zhpg.IEvalIndicatorTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 评估指标模板Controller
 */
@Api(value = "评估指标模板管理")
@RestController
@RequestMapping("/zhpg/indicatorTemplate")
public class EvalIndicatorTemplateController extends BaseController {

    @Autowired
    private IEvalIndicatorTemplateService templateService;

    @ApiOperation("分页查询评估指标模板列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalIndicatorTemplate query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<?> page = new Page<>(pageNum, pageSize);
        Page<EvalIndicatorTemplate> result = templateService.selectTemplatePage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询全部评估指标模板")
    @GetMapping("/listAll")
    public AjaxResult listAll(EvalIndicatorTemplate query) {
        Page<EvalIndicatorTemplate> allPage = new Page<>(1, 1000);
        return AjaxResult.success(templateService.selectTemplatePage(allPage, query).getRecords());
    }

    @ApiOperation("获取评估指标模板详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(templateService.getById(id));
    }

    @ApiOperation("新增评估指标模板")
    @Log(title = "评估指标模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EvalIndicatorTemplate template) {
        if (!templateService.checkTemplateCodeUnique(template)) {
            return AjaxResult.error("新增模板'" + template.getTemplateName() + "'失败，模板编号已存在");
        }
        template.setCreateBy(SecurityUtils.getUsername());
        template.setCreateTime(new Date());
        return toAjax(templateService.insertTemplate(template));
    }

    @ApiOperation("修改评估指标模板")
    @Log(title = "评估指标模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EvalIndicatorTemplate template) {
        if (!templateService.checkTemplateCodeUnique(template)) {
            return AjaxResult.error("修改模板'" + template.getTemplateName() + "'失败，模板编号已存在");
        }
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return toAjax(templateService.updateTemplate(template));
    }

    @ApiOperation("删除评估指标模板")
    @Log(title = "评估指标模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(templateService.deleteTemplateByIds(ids));
    }

    @ApiOperation("导出评估指标模板")
    @Log(title = "评估指标模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvalIndicatorTemplate query) {
        Page<EvalIndicatorTemplate> exportPage = new Page<>(1, 10000);
        List<EvalIndicatorTemplate> list = templateService.selectTemplatePage(exportPage, query).getRecords();
        ExcelUtil<EvalIndicatorTemplate> util = new ExcelUtil<>(EvalIndicatorTemplate.class);
        util.exportExcel(response, list, "评估指标模板数据");
    }
}
