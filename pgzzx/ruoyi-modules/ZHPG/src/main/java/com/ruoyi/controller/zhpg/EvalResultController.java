package com.ruoyi.controller.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.PageDomain;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.core.web.page.TableSupport;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalResult;
import com.ruoyi.service.zhpg.IEvalResultService;
import com.ruoyi.system.api.RemoteFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估结果管理 Controller
 */
@Api(value = "评估结果管理")
@RestController
@RequestMapping("/zhpg/evalResult")
public class EvalResultController extends BaseController {

    @Autowired
    private IEvalResultService evalResultService;

    @Autowired
    private RemoteFileService remoteFileService;

    @ApiOperation("分页查询评估结果列表")
    @GetMapping("/list")
    public TableDataInfo list(EvalResult query) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        Page<?> page = new Page<>(pageNum, pageSize);
        Page<EvalResult> result = evalResultService.selectEvalResultPage(page, query);
        return getDataTable(result);
    }

    @ApiOperation("获取评估结果统计")
    @GetMapping("/stats")
    public AjaxResult getStats() {
        return AjaxResult.success(evalResultService.selectEvalResultStats());
    }

    @ApiOperation("获取评估结果详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(evalResultService.getDetail(id));
    }

    @ApiOperation("根据计算任务获取评估结果详情")
    @GetMapping("/byTask/{taskId}")
    public AjaxResult getInfoByTask(@PathVariable Long taskId) {
        EvalResult result = evalResultService.getDetailByTaskId(taskId);
        return result == null ? AjaxResult.error("暂无评估结果") : AjaxResult.success(result);
    }

    @ApiOperation("获取评估报告预览地址")
    @GetMapping("/{id}/reportPreviewUrl")
    public AjaxResult reportPreviewUrl(@PathVariable Long id) {
        EvalResult result = evalResultService.getById(id);
        if (result == null) {
            return AjaxResult.error("结果不存在");
        }
        if (StringUtils.isBlank(result.getReportUrl())) {
            return AjaxResult.error("暂无已生成的报告文件");
        }

        AjaxResult fileResult = remoteFileService.getPreviewUrl(result.getReportUrl());
        if (!isFileServiceAjaxSuccess(fileResult)) {
            String msg = fileResult != null && fileResult.get(AjaxResult.MSG_TAG) != null
                    ? String.valueOf(fileResult.get(AjaxResult.MSG_TAG))
                    : "文件服务不可用";
            return AjaxResult.error(msg);
        }
        return AjaxResult.success(fileResult.get(AjaxResult.DATA_TAG));
    }

    @ApiOperation("获取评估报告链接信息")
    @GetMapping("/{id}/reportLinks")
    public AjaxResult reportLinks(@PathVariable Long id) {
        EvalResult result = evalResultService.getById(id);
        if (result == null) {
            return AjaxResult.error("结果不存在");
        }

        String pdfUrl = null;
        String wordUrl = null;
        if (StringUtils.isNotBlank(result.getReportPayloadJson())) {
            try {
                JSONObject payload = JSON.parseObject(result.getReportPayloadJson());
                pdfUrl = payload.getString("reportUrl");
                wordUrl = payload.getString("wordUrl");
            } catch (Exception e) {
                return AjaxResult.error("报告链接解析失败: " + e.getMessage());
            }
        }

        if (StringUtils.isBlank(pdfUrl)) {
            pdfUrl = result.getReportUrl();
        }
        if (StringUtils.isBlank(wordUrl)
                && StringUtils.isNotBlank(result.getReportUrl())
                && result.getReportUrl().toLowerCase().endsWith(".docx")) {
            wordUrl = result.getReportUrl();
        }

        Map<String, Object> links = new HashMap<>();
        links.put("reportUrl", result.getReportUrl());
        links.put("pdfUrl", pdfUrl);
        links.put("wordUrl", wordUrl);

        if (StringUtils.isNotBlank(pdfUrl)) {
            AjaxResult fileResult = remoteFileService.getPreviewUrl(pdfUrl);
            if (isFileServiceAjaxSuccess(fileResult)) {
                links.put("previewUrl", fileResult.get(AjaxResult.DATA_TAG));
            }
        }

        return AjaxResult.success(links);
    }

    private static boolean isFileServiceAjaxSuccess(AjaxResult result) {
        if (result == null) {
            return false;
        }
        Object code = result.get(AjaxResult.CODE_TAG);
        return code instanceof Number && ((Number) code).intValue() == HttpStatus.SUCCESS;
    }

    @ApiOperation("新增评估结果")
    @Log(title = "评估结果管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody EvalResult evalResult) {
        evalResult.setCreateBy(SecurityUtils.getUsername());
        evalResult.setCreateTime(new Date());
        return toAjax(evalResultService.insertEvalResult(evalResult));
    }

    @ApiOperation("修改评估结果")
    @Log(title = "评估结果管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody EvalResult evalResult) {
        evalResult.setUpdateBy(SecurityUtils.getUsername());
        evalResult.setUpdateTime(new Date());
        return toAjax(evalResultService.updateEvalResult(evalResult));
    }

    @ApiOperation("删除评估结果")
    @Log(title = "评估结果管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(evalResultService.deleteEvalResultByIds(ids));
    }

    @ApiOperation("发布评估结果")
    @Log(title = "评估结果管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        return toAjax(evalResultService.publishEvalResult(id));
    }

    @ApiOperation("导出评估结果")
    @Log(title = "评估结果管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, EvalResult query) {
        List<EvalResult> list = evalResultService.selectEvalResultList(query);
        ExcelUtil<EvalResult> util = new ExcelUtil<>(EvalResult.class);
        util.exportExcel(response, list, "评估结果数据");
    }
}
