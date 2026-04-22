package com.ruoyi.service.zhpg;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;

import java.util.List;

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
}
