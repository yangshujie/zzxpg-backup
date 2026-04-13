package com.ruoyi.controller.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalDataSource;
import com.ruoyi.service.impl.zhpg.EvalDataSourceFileStorageService;
import com.ruoyi.service.zhpg.IEvalDataSourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Date;

/**
 * 评估数据源管理 Controller。
 */
@Api(value = "评估数据源管理")
@RestController
@RequestMapping("/zhpg/dataSource")
public class EvalDataSourceController extends BaseController {

    @Autowired
    private IEvalDataSourceService dataSourceService;

    @Autowired
    private EvalDataSourceFileStorageService fileStorageService;

    @ApiOperation("分页查询评估数据源列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalDataSource query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Page<?> page = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());
        Page<EvalDataSource> result = dataSourceService.selectDataSourcePage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("查询全部已启用评估数据源")
    @GetMapping("/listAll")
    public AjaxResult listAll(EvalDataSource query) {
        return AjaxResult.success(dataSourceService.selectEnabledList(query));
    }

    @ApiOperation("查询评估数据源详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(dataSourceService.selectDataSourceDetail(id));
    }

    @ApiOperation("新增评估数据源")
    @Log(title = "评估数据源管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Valid @RequestBody EvalDataSource dataSource) {
        dataSource.setCreateBy(SecurityUtils.getUsername());
        dataSource.setCreateTime(new Date());
        return toAjax(dataSourceService.insertDataSource(dataSource));
    }

    @ApiOperation("修改评估数据源")
    @Log(title = "评估数据源管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Valid @RequestBody EvalDataSource dataSource) {
        dataSource.setUpdateBy(SecurityUtils.getUsername());
        dataSource.setUpdateTime(new Date());
        return toAjax(dataSourceService.updateDataSource(dataSource));
    }

    @ApiOperation("删除评估数据源")
    @Log(title = "评估数据源管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(dataSourceService.deleteDataSourceByIds(ids));
    }

    @ApiOperation("测试评估数据源连接")
    @Log(title = "评估数据源管理", businessType = BusinessType.OTHER)
    @PostMapping("/test")
    public AjaxResult test(@RequestBody EvalDataSource dataSource) {
        return AjaxResult.success(dataSourceService.testConnection(dataSource));
    }

    @ApiOperation("上传文件类数据源文件到文件服务")
    @Log(title = "评估数据源管理", businessType = BusinessType.OTHER)
    @PostMapping(value = "/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult uploadFile(@RequestPart("file") MultipartFile file,
                                 @RequestParam("sourceType") String sourceType,
                                 @RequestParam(value = "sourceName", required = false) String sourceName) {
        try {
            String filePath = fileStorageService.uploadDataSourceFile(sourceType, sourceName, file);
            return AjaxResult.success("上传成功")
                    .put("fileName", filePath)
                    .put("data", filePath);
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        } catch (IOException e) {
            return AjaxResult.error("上传失败: " + e.getMessage());
        }
    }
}
