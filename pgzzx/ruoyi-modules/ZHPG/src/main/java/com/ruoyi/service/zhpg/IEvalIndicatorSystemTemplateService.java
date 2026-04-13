package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicatorSystemTemplate;

/**
 * 指标体系模板Service接口
 */
public interface IEvalIndicatorSystemTemplateService extends IService<EvalIndicatorSystemTemplate> {

    Page<EvalIndicatorSystemTemplate> selectTemplatePage(Page page, EvalIndicatorSystemTemplate query);

    int insertTemplate(EvalIndicatorSystemTemplate template);

    int updateTemplate(EvalIndicatorSystemTemplate template);

    int deleteTemplateByIds(Long[] ids);

    boolean checkTemplateCodeUnique(EvalIndicatorSystemTemplate template);
}
