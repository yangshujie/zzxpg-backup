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
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import com.ruoyi.service.zhpg.IEvalIndicatorService.IndicatorTreeNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 评估指标管理 Controller
 *
 * 指标 与 指标模板共用同一张表，按 is_template 区分：
 *   - 列表/CRUD：通过 query 参数 isTemplate=0 / 1 切换 Tab
 *   - 从模板创建：POST /from-template，传入 templateId（is_template=1）
 *                  和 parentId，复制为 is_template=0 的新指标
 */
@Api(value = "评估指标管理")
@RestController
@RequestMapping("/zhpg/indicator")
public class EvalIndicatorController extends BaseController {

    @Autowired
    private IEvalIndicatorService indicatorService;

    @ApiOperation("分页查询指标列表（可按 isTemplate 区分指标 / 模板）")
    @GetMapping("/list")
    public TableDataInfo list(EvalIndicator query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<?> page = new Page<>(pageNum, pageSize);
        Page<EvalIndicator> result = indicatorService.selectIndicatorPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询全部指标（不分页）")
    @GetMapping("/listAll")
    public AjaxResult listAll(EvalIndicator query) {
        return AjaxResult.success(indicatorService.selectIndicatorList(query));
    }

    @ApiOperation("查询父节点候选")
    @GetMapping("/parentCandidates")
    public AjaxResult parentCandidates(@RequestParam String indicatorType,
                                       @RequestParam(required = false) Boolean isTemplate,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Long excludeId,
                                       @RequestParam(required = false) Integer limit) {
        return AjaxResult.success(
                indicatorService.selectParentCandidates(indicatorType, isTemplate, keyword, excludeId, limit));
    }

    @ApiOperation("以指定根节点构建指标子树（仅指标实例）")
    @GetMapping("/tree")
    public AjaxResult tree(@RequestParam Long rootId) {
        IndicatorTreeNode tree = indicatorService.buildIndicatorTree(rootId);
        if (tree == null) {
            return AjaxResult.error("根指标不存在或已删除");
        }
        return AjaxResult.success(tree);
    }

    @ApiOperation("获取指标 / 模板详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(indicatorService.getById(id));
    }

    @ApiOperation("从模板创建指标")
    @Log(title = "评估指标管理", businessType = BusinessType.INSERT)
    @PostMapping("/from-template")
    public AjaxResult fromTemplate(@RequestBody Map<String, Object> body) {
        Long templateId = body.get("templateId") == null ? null : Long.valueOf(body.get("templateId").toString());
        Long parentId = body.get("parentId") == null ? null : Long.valueOf(body.get("parentId").toString());
        if (templateId == null) {
            return AjaxResult.error("模板ID不能为空");
        }
        EvalIndicator created = indicatorService.instantiateFromTemplate(templateId, parentId, SecurityUtils.getUsername());
        return AjaxResult.success("生成成功", created);
    }

    @ApiOperation("新增指标 / 模板")
    @Log(title = "评估指标管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EvalIndicator indicator) {
        indicator.setCreateBy(SecurityUtils.getUsername());
        indicator.setCreateTime(new Date());
        return toAjax(indicatorService.insertIndicator(indicator));
    }

    @ApiOperation("修改指标 / 模板")
    @Log(title = "评估指标管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EvalIndicator indicator) {
        indicator.setUpdateBy(SecurityUtils.getUsername());
        indicator.setUpdateTime(new Date());
        return toAjax(indicatorService.updateIndicator(indicator));
    }

    @ApiOperation("删除指标 / 模板")
    @Log(title = "评估指标管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(indicatorService.deleteIndicatorByIds(ids));
    }

    @ApiOperation("导出指标")
    @Log(title = "评估指标管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvalIndicator query) {
        List<EvalIndicator> list = indicatorService.selectIndicatorList(query);
        ExcelUtil<EvalIndicator> util = new ExcelUtil<>(EvalIndicator.class);
        util.exportExcel(response, list, "评估指标数据");
    }
}
