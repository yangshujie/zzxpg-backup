package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicatorTemplate;

/**
 * 评估指标模板Service接口
 */
public interface IEvalIndicatorTemplateService extends IService<EvalIndicatorTemplate> {

    /**
     * 分页查询评估指标模板列表
     */
    Page<EvalIndicatorTemplate> selectTemplatePage(Page page, EvalIndicatorTemplate query);

    /**
     * 新增评估指标模板
     */
    int insertTemplate(EvalIndicatorTemplate template);

    /**
     * 修改评估指标模板
     */
    int updateTemplate(EvalIndicatorTemplate template);

    /**
     * 删除评估指标模板
     */
    int deleteTemplateByIds(Long[] ids);

    /**
     * 校验模板编号是否唯一
     */
    boolean checkTemplateCodeUnique(EvalIndicatorTemplate template);
}
