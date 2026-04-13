package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.domain.zhpg.EvalIndicatorTemplate;

import java.util.List;

/**
 * 评估指标Service接口
 */
public interface IEvalIndicatorService extends IService<EvalIndicator> {

    /**
     * 分页查询评估指标列表
     */
    Page<EvalIndicator> selectIndicatorPage(Page page, EvalIndicator query);

    /**
     * 查询评估指标列表（不分页）
     */
    List<EvalIndicator> selectIndicatorList(EvalIndicator query);

    /**
     * 新增评估指标
     */
    int insertIndicator(EvalIndicator indicator);

    /**
     * 修改评估指标
     */
    int updateIndicator(EvalIndicator indicator);

    /**
     * 删除评估指标
     */
    int deleteIndicatorByIds(Long[] ids);

    /**
     * 基于模板实例化指标（用于后续二次编辑）
     */
    EvalIndicator instantiateFromTemplate(EvalIndicatorTemplate template, Long parentId, String operator);

    /**
     * 查询可选父节点（按类型/关键字/排除节点过滤）
     */
    List<EvalIndicator> selectParentCandidates(String indicatorType, String keyword, Long excludeId, Integer limit);
}
