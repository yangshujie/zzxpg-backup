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
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.domain.zhpg.EvalIndicatorTemplate;
import com.ruoyi.domain.zhpg.dto.IndicatorFromTemplateDTO;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import com.ruoyi.service.zhpg.IEvalIndicatorTemplateService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;

/**
 * 评估指标构建与管理Controller
 */
@Api(value = "评估指标构建与管理")
@RestController
@RequestMapping("/zhpg/indicator")
public class EvalIndicatorController extends BaseController {

    @Autowired
    private IEvalIndicatorService indicatorService;
    @Autowired
    private IEvalIndicatorTemplateService templateService;

    @ApiOperation("分页查询评估指标列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalIndicator query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<?> page = new Page<>(pageNum, pageSize);
        Page<EvalIndicator> result = indicatorService.selectIndicatorPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询全部评估指标（不分页）")
    @GetMapping("/listAll")
    public AjaxResult listAll(EvalIndicator query) {
        return AjaxResult.success(indicatorService.selectIndicatorList(query));
    }

    @ApiOperation("查询父节点候选（按类型与关键字）")
    @GetMapping("/parentCandidates")
    public AjaxResult parentCandidates(@RequestParam String indicatorType,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long excludeId,
                                       @RequestParam(required = false) Integer limit) {
        return AjaxResult.success(indicatorService.selectParentCandidates(indicatorType, keyword, excludeId, limit));
    }

    @ApiOperation("获取评估指标详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(indicatorService.getById(id));
    }

    @ApiOperation("从指标模板生成指标草稿")
    @Log(title = "评估指标管理", businessType = BusinessType.INSERT)
    @PostMapping("/from-template")
    public AjaxResult fromTemplate(@RequestBody IndicatorFromTemplateDTO dto) {
        if (dto == null || dto.getTemplateId() == null) {
            return AjaxResult.error("模板ID不能为空");
        }
        EvalIndicatorTemplate template = templateService.getById(dto.getTemplateId());
        if (template == null) {
            return AjaxResult.error("模板不存在或已删除");
        }
        EvalIndicator indicator = indicatorService.instantiateFromTemplate(
                template,
                dto.getParentId(),
                SecurityUtils.getUsername()
        );
        return AjaxResult.success("生成成功", indicator);
    }

    @ApiOperation("新增评估指标")
    @Log(title = "评估指标管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EvalIndicator indicator) {
        indicator.setCreateBy(SecurityUtils.getUsername());
        indicator.setCreateTime(new Date());
        return toAjax(indicatorService.insertIndicator(indicator));
    }

    @ApiOperation("修改评估指标")
    @Log(title = "评估指标管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EvalIndicator indicator) {
        indicator.setUpdateBy(SecurityUtils.getUsername());
        indicator.setUpdateTime(new Date());
        return toAjax(indicatorService.updateIndicator(indicator));
    }

    @ApiOperation("删除评估指标")
    @Log(title = "评估指标管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(indicatorService.deleteIndicatorByIds(ids));
    }

    @ApiOperation("导出评估指标")
    @Log(title = "评估指标管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvalIndicator query) {
        List<EvalIndicator> list = indicatorService.selectIndicatorList(query);
        ExcelUtil<EvalIndicator> util = new ExcelUtil<>(EvalIndicator.class);
        util.exportExcel(response, list, "评估指标数据");
    }
}
