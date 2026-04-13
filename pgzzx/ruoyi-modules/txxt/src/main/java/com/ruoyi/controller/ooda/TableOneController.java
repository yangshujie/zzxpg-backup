package com.ruoyi.controller.ooda;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.domain.ooda.TableOne;
import com.ruoyi.service.ooda.TableOneService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(value = "测试表操作")
@RestController
@RequestMapping("tableOne")
public class TableOneController extends BaseController
{
    @Autowired
    private TableOneService tableOneService;

    @ApiOperation("新增测试表")
    @PostMapping("insertTableOne/{id}/{name}")
    public AjaxResult insertTableOne(@PathVariable String id, @PathVariable String name)
    {
        return AjaxResult.success(tableOneService.insertTableOne(id, name));
    }

    @ApiOperation("删除测试表")
    @DeleteMapping("deleteTableOne/{id}")
    public AjaxResult deleteTableOne(@PathVariable String id)
    {
        return AjaxResult.success(tableOneService.deleteTableOne(id));
    }

    @ApiOperation("更新测试表")
    @PutMapping("updateTableOne/{id}/{name}")
    public AjaxResult updateTableOne(@PathVariable String id, @PathVariable String name)
    {
        return AjaxResult.success(tableOneService.updateTableOne(id, name));
    }

    @ApiOperation("查询测试表")
    @GetMapping("selectTableOne/{id}")
    public AjaxResult selectTableOne(@PathVariable String id)
    {
        return AjaxResult.success(tableOneService.selectTableOne(id));
    }

    @ApiOperation("查询测试表名称列表")
    @GetMapping("selectNameList")
    public AjaxResult selectNameList(@RequestBody TableOne tableOne)
    {
        return AjaxResult.success(tableOneService.selectNameList(tableOne));
    }
}
