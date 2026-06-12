package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;

import java.util.List;
import java.util.Map;

public interface IEvalReportInstanceService extends IService<EvalReportInstance> {

    /** 异步生成报告（返回 PENDING 实例，实际生成在后台执行） */
    EvalReportInstance generateForResult(Long evalResultId, EvalReportGenerateRequest request);

    /** 快速渲染 HTML 预览（不生成 DOCX/PDF，<3s 返回） */
    EvalReportInstance previewHtml(Long evalResultId, EvalReportGenerateRequest request);

    /** 查询某评估结果的报告生成历史 */
    List<EvalReportInstance> listByResult(Long evalResultId);

    /** 分页查询报告实例 */
    Page<EvalReportInstance> selectReportPage(Page<EvalReportInstance> page, EvalReportInstance query);

    /** 查询某一版报告的进度（含生成完成后的链接） */
    Map<String, Object> getProgress(Long reportId);

    /** 查询某一版报告的预览/下载链接 */
    Map<String, Object> getReportLinks(Long reportId);

    /** 删除报告实例 */
    int deleteReportByIds(Long[] ids);
}
