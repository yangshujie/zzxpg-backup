package com.ruoyi.service.zhpg;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;

import java.util.List;
import java.util.Map;

public interface IEvalIndicatorSystemService extends IService<EvalIndicatorSystem> {

    Page<EvalIndicatorSystem> selectSystemPage(Page page, EvalIndicatorSystem query);

    List<EvalIndicatorSystem> selectSystemList(EvalIndicatorSystem query);

    int insertSystem(EvalIndicatorSystem system);

    int updateSystem(EvalIndicatorSystem system);

    int deleteSystemByIds(Long[] ids);

    EvalIndicatorSystem createFromTemplate(Long templateId, String systemName, String operator);

    List<EvalIndicatorSystemSelectVO> selectIndicatorSystemListForSelect(String keyword, Long requirementId);

    EvalIndicatorSystem getByRequirementId(Long requirementId);

    EvalIndicatorSystem receiveRefinedFromRequirementPayload(JSONObject payload, String operator);

    /**
     * 智能综合赋权：根据指标体系中各父节点的算法配置（主观 vs 客观），自动分发计算逻辑并汇总结果。
     */
    Object computeWeightsSmart(Long systemId, JSONObject options, String operator);

    /**
     * 批量检测指标体系是否被其他模块引用（如评估准则、计算流程、任务模板等）
     */
    Map<Long, List<String>> checkSystemReferences(Long[] ids);
}
