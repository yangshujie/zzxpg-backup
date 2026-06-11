package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalCriterionSet;

import java.util.List;

/**
 * 评估准则集Service接口
 */
public interface IEvalCriterionSetService extends IService<EvalCriterionSet> {

    /** 分页查询准则集列表 */
    Page<EvalCriterionSet> selectCriterionSetPage(Page<?> page, EvalCriterionSet query);

    /** 查询准则集列表 */
    List<EvalCriterionSet> selectCriterionSetList(EvalCriterionSet query);

    /** 查询准则集详情 */
    EvalCriterionSet selectCriterionSetDetail(Long id);

    /** 新增准则集 */
    int insertCriterionSet(EvalCriterionSet set);

    /** 修改准则集 */
    int updateCriterionSet(EvalCriterionSet set);

    /** 删除准则集 */
    int deleteCriterionSetByIds(Long[] ids);
}
