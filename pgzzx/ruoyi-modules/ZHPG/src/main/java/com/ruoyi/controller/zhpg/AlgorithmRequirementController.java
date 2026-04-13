package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.AlgorithmRequirement;
import com.ruoyi.service.zhpg.IAlgorithmRequirementService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 算法需求管理Controller
 */
@Api(value = "算法需求管理")
@RestController
@RequestMapping("/zhpg/algorithmRequirement")
public class AlgorithmRequirementController extends BaseController {

    @Autowired
    private IAlgorithmRequirementService requirementService;

    @ApiOperation("分页查询算法需求列表")
    @GetMapping("/list")
    public TableDataInfo list(AlgorithmRequirement query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<AlgorithmRequirement> result = requirementService.selectRequirementPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询全部算法需求（不分页）")
    @GetMapping("/listAll")
    public AjaxResult listAll(AlgorithmRequirement query) {
        return AjaxResult.success(requirementService.selectRequirementList(query));
    }

    @ApiOperation("获取算法需求详情（含参数）")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(requirementService.selectRequirementDetail(id));
    }

    @ApiOperation("新增算法需求")
    @Log(title = "算法需求管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AlgorithmRequirement requirement) {
        requirement.setCreateBy(SecurityUtils.getUsername());
        requirement.setCreateTime(new Date());
        return toAjax(requirementService.insertRequirement(requirement));
    }

    @ApiOperation("修改算法需求")
    @Log(title = "算法需求管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AlgorithmRequirement requirement) {
        requirement.setUpdateBy(SecurityUtils.getUsername());
        requirement.setUpdateTime(new Date());
        return toAjax(requirementService.updateRequirement(requirement));
    }

    @ApiOperation("删除算法需求")
    @Log(title = "算法需求管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(requirementService.deleteRequirementByIds(ids));
    }

    @ApiOperation("批量删除算法需求")
    @Log(title = "算法需求管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch/{ids}")
    public AjaxResult batchRemove(@PathVariable Long[] ids) {
        return toAjax(requirementService.deleteRequirementByIds(ids));
    }

    @ApiOperation("更新构建状态")
    @Log(title = "算法需求管理", businessType = BusinessType.UPDATE)
    @PutMapping("/build/{id}")
    public AjaxResult updateBuildStatus(
            @PathVariable Long id,
            @RequestParam String algorithmName,
            @RequestParam(required = false) Long algorithmId) {
        return toAjax(requirementService.updateBuildStatus(id, algorithmName, algorithmId));
    }

    @ApiOperation("更新通知状态")
    @Log(title = "算法需求管理", businessType = BusinessType.UPDATE)
    @PutMapping("/notify/{id}")
    public AjaxResult updateNotifyStatus(
            @PathVariable Long id,
            @RequestParam String notifyStatus) {
        return toAjax(requirementService.updateNotifyStatus(id, notifyStatus));
    }

    @ApiOperation("导出算法需求列表")
    @Log(title = "算法需求管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AlgorithmRequirement query) {
        List<AlgorithmRequirement> list = requirementService.selectRequirementList(query);
        ExcelUtil<AlgorithmRequirement> util = new ExcelUtil<>(AlgorithmRequirement.class);
        util.exportExcel(response, list, "算法需求数据");
    }

    // ================== 分系统对接接口 ==================

    /**
     * 接收分系统发来的算法需求新增请求
     * 该接口供需求分系统调用，用于创建新的算法需求
     */
    @ApiOperation(value = "接收分系统算法需求", notes = "供需求分系统调用，创建新的算法需求")
    @Log(title = "算法需求-分系统对接", businessType = BusinessType.INSERT)
    @PostMapping(value = "/receive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AjaxResult receiveFromSubsystem(@RequestBody AlgorithmRequirement requirement) {
        try {
            int rows = requirementService.receiveFromSubsystem(requirement);
            if (rows > 0) {
                return AjaxResult.success("需求接收成功", requirement);
            }
            return AjaxResult.error("需求接收失败");
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 接收分系统发来的算法需求更新请求
     */
    @ApiOperation(value = "更新分系统算法需求", notes = "供需求分系统调用，更新已有的算法需求")
    @Log(title = "算法需求-分系统对接", businessType = BusinessType.UPDATE)
    @PutMapping(value = "/receive", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AjaxResult updateFromSubsystem(@RequestBody AlgorithmRequirement requirement) {
        try {
            requirement.setUpdateBy("subsystem");
            requirement.setUpdateTime(new Date());
            return toAjax(requirementService.updateRequirement(requirement));
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 分系统查询需求状态
     */
    @ApiOperation(value = "查询需求状态", notes = "供需求分系统查询算法需求的状态")
    @GetMapping("/status/{id}")
    public AjaxResult getStatus(@PathVariable Long id) {
        AlgorithmRequirement requirement = requirementService.selectRequirementDetail(id);
        if (requirement == null) {
            return AjaxResult.error("需求不存在");
        }
        return AjaxResult.success(requirement);
    }
}
