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
import com.ruoyi.domain.zhpg.AlgorithmInfo;
import com.ruoyi.domain.zhpg.AlgorithmParam;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.service.zhpg.IAlgorithmParamService;
import com.ruoyi.service.impl.zhpg.AlgorithmCodeStorageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 算法管理Controller
 */
@Api(value = "算法模型配置管理")
@RestController
@RequestMapping("/zhpg/algorithm")
public class AlgorithmInfoController extends BaseController {

    @Autowired
    private IAlgorithmInfoService algorithmService;

    @Autowired
    private IAlgorithmParamService paramService;

    @Autowired
    private AlgorithmCodeStorageService algorithmCodeStorageService;

    @ApiOperation("分页查询算法列表")
    @GetMapping("/list")
    public TableDataInfo list(AlgorithmInfo query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<AlgorithmInfo> result = algorithmService.selectAlgorithmPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询全部算法（不分页）")
    @GetMapping("/listAll")
    public AjaxResult listAll(AlgorithmInfo query) {
        return AjaxResult.success(algorithmService.selectAlgorithmList(query));
    }

    @ApiOperation("获取算法详情（含参数）")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(algorithmService.selectAlgorithmDetail(id));
    }

    @ApiOperation("查询算法参数列表")
    @GetMapping("/{id}/params")
    public AjaxResult getParams(@PathVariable Long id,
                                @RequestParam(required = false) String paramCategory) {
        List<AlgorithmParam> params;
        if (paramCategory != null && !paramCategory.isEmpty()) {
            params = paramService.selectParamsByCategory(id, paramCategory);
        } else {
            params = paramService.selectParamsByAlgorithmId(id);
        }
        return AjaxResult.success(params);
    }

    @ApiOperation("新增算法")
    @Log(title = "算法管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AlgorithmInfo algorithm) {
        algorithm.setCreateBy(SecurityUtils.getUsername());
        algorithm.setCreateTime(new Date());
        return toAjax(algorithmService.insertAlgorithm(algorithm));
    }

    @ApiOperation("修改算法")
    @Log(title = "算法管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody AlgorithmInfo algorithm) {
        algorithm.setUpdateBy(SecurityUtils.getUsername());
        algorithm.setUpdateTime(new Date());
        return toAjax(algorithmService.updateAlgorithm(algorithm));
    }

    @ApiOperation("删除算法")
    @Log(title = "算法管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(algorithmService.deleteAlgorithmByIds(ids));
    }

    @ApiOperation("发布算法")
    @Log(title = "算法管理", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id) {
        return toAjax(algorithmService.publishAlgorithm(id));
    }

    @ApiOperation("撤回发布")
    @Log(title = "算法管理", businessType = BusinessType.UPDATE)
    @PutMapping("/unpublish/{id}")
    public AjaxResult unpublish(@PathVariable Long id) {
        return toAjax(algorithmService.unpublishAlgorithm(id));
    }

    @ApiOperation("导出算法列表")
    @Log(title = "算法管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, AlgorithmInfo query) {
        List<AlgorithmInfo> list = algorithmService.selectAlgorithmList(query);
        ExcelUtil<AlgorithmInfo> util = new ExcelUtil<>(AlgorithmInfo.class);
        util.exportExcel(response, list, "算法数据");
    }

    @ApiOperation("上传算法代码包（zip），路径按算法类型与算法名称自动生成")
    @Log(title = "算法管理", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/uploadCode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult uploadCode(@RequestPart("file") MultipartFile file,
                                 @RequestParam("algorithmType") String algorithmType,
                                 @RequestParam("algorithmName") String algorithmName) {
        try {
            String url = algorithmCodeStorageService.uploadAlgorithmZip(algorithmType, algorithmName, file);
            // 必须 success(msg, data)：单参 String 会命中 success(String msg)，data 为空，前端读 res.data 拿不到路径
            return AjaxResult.success("上传成功", url);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        } catch (IOException e) {
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }

    @ApiOperation("预览算法主 .py 源码（需已配置代码包路径且文件服务可用）")
    @GetMapping("/{id}/codePreview")
    public AjaxResult codePreview(@PathVariable Long id) {
        try {
            AlgorithmInfo info = algorithmService.selectAlgorithmDetail(id);
            if (info == null) {
                return AjaxResult.error("算法不存在");
            }
            String code = algorithmCodeStorageService.readMainPythonForPreview(info.getAlgorithmCodeUrl());
            return AjaxResult.success("预览成功", code);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
