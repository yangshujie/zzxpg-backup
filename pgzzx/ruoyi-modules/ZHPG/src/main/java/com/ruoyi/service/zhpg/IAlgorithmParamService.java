package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.AlgorithmParam;

import java.util.List;

/**
 * 算法参数Service接口
 */
public interface IAlgorithmParamService extends IService<AlgorithmParam> {

    /**
     * 根据算法ID查询参数列表
     */
    List<AlgorithmParam> selectParamsByAlgorithmId(Long algorithmId);

    /**
     * 根据算法ID和参数类别查询参数列表
     */
    List<AlgorithmParam> selectParamsByCategory(Long algorithmId, String paramCategory);

    /**
     * 批量保存算法参数（先删后增）
     */
    void saveParams(Long algorithmId, List<AlgorithmParam> params, String username);

    /**
     * 根据算法ID删除所有参数
     */
    int deleteParamsByAlgorithmId(Long algorithmId);
}
