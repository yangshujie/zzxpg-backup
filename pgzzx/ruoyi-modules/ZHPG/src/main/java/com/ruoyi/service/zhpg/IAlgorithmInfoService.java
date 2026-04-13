package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.AlgorithmInfo;

import java.util.List;

/**
 * 算法信息Service接口
 */
public interface IAlgorithmInfoService extends IService<AlgorithmInfo> {

    /**
     * 分页查询算法列表
     */
    Page<AlgorithmInfo> selectAlgorithmPage(Page<?> page, AlgorithmInfo query);

    /**
     * 查询算法列表（不分页）
     */
    List<AlgorithmInfo> selectAlgorithmList(AlgorithmInfo query);

    /**
     * 查询算法详情（含参数列表）
     */
    AlgorithmInfo selectAlgorithmDetail(Long id);

    /**
     * 新增算法（含参数）
     */
    int insertAlgorithm(AlgorithmInfo algorithm);

    /**
     * 修改算法（含参数）
     */
    int updateAlgorithm(AlgorithmInfo algorithm);

    /**
     * 批量删除算法
     */
    int deleteAlgorithmByIds(Long[] ids);

    /**
     * 发布算法
     */
    int publishAlgorithm(Long id);

    /**
     * 撤回发布
     */
    int unpublishAlgorithm(Long id);
}
