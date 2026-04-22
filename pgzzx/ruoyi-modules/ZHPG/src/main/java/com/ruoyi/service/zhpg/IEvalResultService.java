package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalResult;

import java.util.List;
import java.util.Map;

/**
 * 评估结果Service接口
 */
public interface IEvalResultService extends IService<EvalResult> {

    /**
     * 分页查询评估结果列表
     */
    Page<EvalResult> selectEvalResultPage(Page page, EvalResult query);

    /**
     * 查询评估结果列表（不分页）
     */
    List<EvalResult> selectEvalResultList(EvalResult query);

    /**
     * 获取评估结果统计
     */
    Map<String, Object> selectEvalResultStats();

    /**
     * 新增评估结果
     */
    int insertEvalResult(EvalResult evalResult);

    /**
     * 修改评估结果
     */
    int updateEvalResult(EvalResult evalResult);

    /**
     * 删除评估结果
     */
    int deleteEvalResultByIds(Long[] ids);

    /**
     * 发布评估结果
     */
    int publishEvalResult(Long id);

    /**
     * 详情（含维度列表解析）
     */
    EvalResult getDetail(Long id);

    /**
     * 根据计算任务获取评估结果详情
     */
    EvalResult getDetailByTaskId(Long taskId);
}
