package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;

import java.util.List;
import java.util.Map;

public interface IEvalReportInstanceService extends IService<EvalReportInstance> {
    EvalReportInstance generateForResult(Long evalResultId, EvalReportGenerateRequest request);

    List<EvalReportInstance> listByResult(Long evalResultId);

    Page<EvalReportInstance> selectReportPage(Page<EvalReportInstance> page, EvalReportInstance query);

    Map<String, Object> getReportLinks(Long reportId);

    int deleteReportByIds(Long[] ids);
}
