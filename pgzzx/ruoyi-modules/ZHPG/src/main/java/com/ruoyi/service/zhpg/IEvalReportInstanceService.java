package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;

import java.util.List;
import java.util.Map;

public interface IEvalReportInstanceService extends IService<EvalReportInstance> {
    EvalReportInstance generateForResult(Long evalResultId, EvalReportGenerateRequest request);

    List<EvalReportInstance> listByResult(Long evalResultId);

    Map<String, Object> getReportLinks(Long reportId);
}
