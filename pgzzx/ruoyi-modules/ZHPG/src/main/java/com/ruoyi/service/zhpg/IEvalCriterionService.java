package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalCriterion;

import java.util.List;

/**
 * 评估准则明细Service接口
 */
public interface IEvalCriterionService extends IService<EvalCriterion> {

    /** 根据准则集ID查询准则明细列表 */
    List<EvalCriterion> selectCriterionListBySetId(Long setId);

    /** 根据任务ID加载绑定的准则列表（通过任务关联的准则集） */
    List<EvalCriterion> selectCriterionListByTaskId(Long taskId);

    /**
     * 批量保存准则项（事务一致性覆盖）
     * @param setId 准则集ID
     * @param criterionList 准则明细列表
     * @return 成功保存的数量
     */
    int batchSaveCriteria(Long setId, List<EvalCriterion> criterionList);
}
