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
import com.ruoyi.domain.zhpg.EvalCriterion;
import com.ruoyi.domain.zhpg.EvalCriterionSet;
import com.ruoyi.service.zhpg.IEvalCriterionService;
import com.ruoyi.service.zhpg.IEvalCriterionSetService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import java.util.List;

/**
 * 评估准则及准则集管理Controller（评估任务构建分系统 RWGJ）
 */
@Api(value = "评估任务构建-评估准则与准则集管理")
@RestController
@RequestMapping("/zhpg")
public class EvalCriterionController extends BaseController {

    @Autowired
    private IEvalCriterionSetService criterionSetService;

    @Autowired
    private IEvalCriterionService evalCriterionService;

    // ==================== 1. 准则集 (CriterionSet) CRUD ====================

    @ApiOperation("分页查询准则集列表")
    @GetMapping("/criterionSet/list")
    public TableDataInfo listSets(EvalCriterionSet query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<EvalCriterionSet> result = criterionSetService.selectCriterionSetPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询准则集详情")
    @GetMapping("/criterionSet/{id}")
    public AjaxResult getSetInfo(@PathVariable Long id) {
        return AjaxResult.success(criterionSetService.selectCriterionSetDetail(id));
    }

    @ApiOperation("新增准则集")
    @Log(title = "准则集管理", businessType = BusinessType.INSERT)
    @PostMapping("/criterionSet")
    public AjaxResult addSet(@Valid @RequestBody EvalCriterionSet set) {
        set.setCreateBy(SecurityUtils.getUsername());
        set.setCreateTime(new Date());
        criterionSetService.insertCriterionSet(set);
        return AjaxResult.success(set);
    }

    @ApiOperation("修改准则集")
    @Log(title = "准则集管理", businessType = BusinessType.UPDATE)
    @PutMapping("/criterionSet")
    public AjaxResult editSet(@Valid @RequestBody EvalCriterionSet set) {
        set.setUpdateBy(SecurityUtils.getUsername());
        set.setUpdateTime(new Date());
        return toAjax(criterionSetService.updateCriterionSet(set));
    }

    @ApiOperation("删除准则集")
    @Log(title = "准则集管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/criterionSet/{ids}")
    public AjaxResult removeSets(@PathVariable Long[] ids) {
        return toAjax(criterionSetService.deleteCriterionSetByIds(ids));
    }

    // ==================== 2. 准则明细 (EvalCriterion) CRUD ====================

    @ApiOperation("根据准则集ID查询准则明细")
    @GetMapping("/evalCriterion/bySet/{setId}")
    public AjaxResult getCriteriaBySet(@PathVariable Long setId) {
        return AjaxResult.success(evalCriterionService.selectCriterionListBySetId(setId));
    }

    @ApiOperation("根据评估任务ID查询准则明细")
    @GetMapping("/evalCriterion/byTask/{taskId}")
    public AjaxResult getCriteriaByTask(@PathVariable Long taskId) {
        return AjaxResult.success(evalCriterionService.selectCriterionListByTaskId(taskId));
    }

    @ApiOperation("批量覆盖保存指标准则")
    @Log(title = "评估准则配置", businessType = BusinessType.UPDATE)
    @PostMapping("/evalCriterion/batchSave/{setId}")
    public AjaxResult batchSave(@PathVariable Long setId, @RequestBody List<EvalCriterion> criterionList) {
        // 设置操作人属性
        String username = SecurityUtils.getUsername();
        if (criterionList != null) {
            for (EvalCriterion item : criterionList) {
                if (item.getId() == null) {
                    item.setCreateBy(username);
                } else {
                    item.setUpdateBy(username);
                }
                item.setUpdateTime(new Date());
            }
        }
        int count = evalCriterionService.batchSaveCriteria(setId, criterionList);
        return AjaxResult.success("批量保存成功，共保存 " + count + " 条规则", count);
    }
}
