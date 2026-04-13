package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.AlgorithmRequirement;

import java.util.List;

/**
 * 算法需求Service接口
 */
public interface IAlgorithmRequirementService extends IService<AlgorithmRequirement> {

    /**
     * 分页查询算法需求列表
     */
    Page<AlgorithmRequirement> selectRequirementPage(Page<?> page, AlgorithmRequirement query);

    /**
     * 查询算法需求列表（不分页）
     */
    List<AlgorithmRequirement> selectRequirementList(AlgorithmRequirement query);

    /**
     * 查询算法需求详情（含参数）
     */
    AlgorithmRequirement selectRequirementDetail(Long id);

    /**
     * 新增算法需求（含参数）
     */
    int insertRequirement(AlgorithmRequirement requirement);

    /**
     * 修改算法需求（含参数）
     */
    int updateRequirement(AlgorithmRequirement requirement);

    /**
     * 批量删除算法需求
     */
    int deleteRequirementByIds(Long[] ids);

    /**
     * 更新构建状态
     */
    int updateBuildStatus(Long id, String algorithmName, Long algorithmId);

    /**
     * 更新通知状态
     */
    int updateNotifyStatus(Long id, String notifyStatus);

    /**
     * 接收分系统算法需求新增请求
     */
    int receiveFromSubsystem(AlgorithmRequirement requirement);
}
