package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.service.zhpg.IObjectiveWeightService;
import com.ruoyi.domain.zhpg.dto.ObjectiveWeightComputeResult;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.alibaba.fastjson2.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估指标体系构建与管理Controller
 */
@Api(value = "评估指标体系构建与管理")
@RestController
@RequestMapping("/zhpg/indicatorSystem")
public class EvalIndicatorSystemController extends BaseController {

    @Autowired
    private IEvalIndicatorSystemService systemService;

    @Autowired
    private IObjectiveWeightService objectiveWeightService;

    @ApiOperation("分页查询指标体系列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalIndicatorSystem query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        Page<?> page = new Page<>(pageNum, pageSize);
        Page<EvalIndicatorSystem> result = systemService.selectSystemPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("获取指标体系详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(systemService.getById(id));
    }

    @ApiOperation("新增指标体系")
    @Log(title = "评估指标体系", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EvalIndicatorSystem system) {
        system.setCreateBy(SecurityUtils.getUsername());
        system.setCreateTime(new Date());
        return toAjax(systemService.insertSystem(system));
    }

    @ApiOperation("修改指标体系")
    @Log(title = "评估指标体系", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EvalIndicatorSystem system) {
        system.setUpdateBy(SecurityUtils.getUsername());
        system.setUpdateTime(new Date());
        return toAjax(systemService.updateSystem(system));
    }

    @ApiOperation("删除指标体系")
    @Log(title = "评估指标体系", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(systemService.deleteSystemByIds(ids));
    }

    @ApiOperation("基于模板创建指标体系")
    @Log(title = "评估指标体系", businessType = BusinessType.INSERT)
    @PostMapping("/createFromTemplate")
    public AjaxResult createFromTemplate(@RequestBody Map<String, Object> params) {
        Long templateId = params.get("templateId") != null ? Long.valueOf(params.get("templateId").toString()) : null;
        String systemName = params.get("systemName") != null ? params.get("systemName").toString() : null;
        if (templateId == null) {
            return AjaxResult.error("模板ID不能为空");
        }
        EvalIndicatorSystem system = systemService.createFromTemplate(templateId, systemName, SecurityUtils.getUsername());
        return AjaxResult.success("创建成功", system);
    }

    @ApiOperation("下发指标体系")
    @Log(title = "评估指标体系", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/distribute")
    public AjaxResult distribute(@PathVariable Long id) {
        EvalIndicatorSystem system = systemService.getById(id);
        if (system == null) {
            return AjaxResult.error("指标体系不存在");
        }
        if (!"PUBLISHED".equals(system.getStatus())) {
            return AjaxResult.error("仅已发布的指标体系可以下发");
        }
        system.setIsApplied(1);
        system.setUpdateBy(SecurityUtils.getUsername());
        system.setUpdateTime(new Date());
        return toAjax(systemService.updateById(system));
    }

    @ApiOperation("停用指标体系（取消启用状态，不删除数据）")
    @Log(title = "评估指标体系", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/disableApplied")
    public AjaxResult disableApplied(@PathVariable Long id) {
        EvalIndicatorSystem system = systemService.getById(id);
        if (system == null) {
            return AjaxResult.error("指标体系不存在");
        }
        system.setIsApplied(0);
        system.setUpdateBy(SecurityUtils.getUsername());
        system.setUpdateTime(new Date());
        return toAjax(systemService.updateById(system));
    }

    @ApiOperation("发布指标体系")
    @Log(title = "评估指标体系", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        EvalIndicatorSystem system = systemService.getById(id);
        if (system == null) {
            return AjaxResult.error("指标体系不存在");
        }
        system.setStatus("PUBLISHED");
        system.setUpdateBy(SecurityUtils.getUsername());
        system.setUpdateTime(new Date());
        return toAjax(systemService.updateById(system));
    }

    @ApiOperation("导出指标体系JSON")
    @GetMapping("/{id}/exportJson")
    public AjaxResult exportJson(@PathVariable Long id) {
        EvalIndicatorSystem system = systemService.getById(id);
        if (system == null) {
            return AjaxResult.error("指标体系不存在");
        }
        return AjaxResult.success(system);
    }

    @ApiOperation("导入指标体系JSON")
    @Log(title = "评估指标体系", businessType = BusinessType.IMPORT)
    @PostMapping("/importJson")
    public AjaxResult importJson(@RequestBody EvalIndicatorSystem system) {
        system.setId(null);
        system.setStatus("DRAFT");
        system.setIsApplied(0);
        system.setCreateBy(SecurityUtils.getUsername());
        system.setCreateTime(new Date());
        return toAjax(systemService.insertSystem(system));
    }

    @ApiOperation("导出指标体系Excel")
    @Log(title = "评估指标体系", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvalIndicatorSystem query) {
        List<EvalIndicatorSystem> list = systemService.selectSystemList(query);
        ExcelUtil<EvalIndicatorSystem> util = new ExcelUtil<>(EvalIndicatorSystem.class);
        util.exportExcel(response, list, "评估指标体系数据");
    }

    // ==================== 主分协同：构建阶段管理 ====================

    @ApiOperation("接收需求分析等外部系统下发的细化指标体系（需求元数据 + treeData）。"
            + "合并已有记录时优先 targetIndicatorSystemId / indicatorSystemId；"
            + "均未传时按 treeData 根节点 id（与 pgzc_indicator_system.id_code 一致，纯数字时亦可匹配主键 id）定位并写入 indicator_tree_weight；"
            + "可传 requirementId（或嵌套 indicatorSystem.requirementId）写入 pgzc_indicator_system.requirement_id。")
    @Log(title = "评估指标体系", businessType = BusinessType.IMPORT)
    @PostMapping("/receiveRefinedFromRequirement")
    public AjaxResult receiveRefinedFromRequirement(@RequestBody Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return AjaxResult.error("请求体不能为空");
        }
        JSONObject payload = new JSONObject(body);
        EvalIndicatorSystem saved = systemService.receiveRefinedFromRequirementPayload(payload, SecurityUtils.getUsername());
        return AjaxResult.success(saved);
    }


    @ApiOperation("外部系统选择 - 查询所有指标体系（支持下拉框，支持关键字搜索）")
    @GetMapping("/select")
    public AjaxResult selectForSelect(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long requirementId) {
        return AjaxResult.success(systemService.selectIndicatorSystemListForSelect(keyword, requirementId));
    }

    @ApiOperation("客观赋权：模拟样本矩阵调用 zgpg_algs（熵权 cal_weight / AHP ahp），写回 indicator_tree_weight；"
            + "请求体可选 persist（默认 true）、mockSampleRows（默认 8）、mockSeed")
    @Log(title = "评估指标体系", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/objectiveWeight")
    public AjaxResult objectiveWeight(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        JSONObject opts = body == null || body.isEmpty() ? new JSONObject() : new JSONObject(body);
        ObjectiveWeightComputeResult r = objectiveWeightService.computeForSystem(id, opts, SecurityUtils.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("indicatorTreeWeight", r.getIndicatorTreeWeight());
        data.put("hint", r.getHint());
        data.put("mockNote", r.getMockNote());
        data.put("algorithmCallCount", r.getAlgorithmCallCount());
        return AjaxResult.success(data);
    }
}
