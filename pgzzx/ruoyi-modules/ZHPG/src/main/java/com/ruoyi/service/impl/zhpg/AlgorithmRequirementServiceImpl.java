package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.AlgorithmRequirement;
import com.ruoyi.domain.zhpg.AlgorithmRequirementParam;
import com.ruoyi.mapper.zhpg.AlgorithmRequirementMapper;
import com.ruoyi.mapper.zhpg.AlgorithmRequirementParamMapper;
import com.ruoyi.service.zhpg.IAlgorithmRequirementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 算法需求Service实现
 */
@Service
public class AlgorithmRequirementServiceImpl extends ServiceImpl<AlgorithmRequirementMapper, AlgorithmRequirement>
        implements IAlgorithmRequirementService {

    @Autowired
    private AlgorithmRequirementParamMapper paramMapper;

    @Override
    public Page<AlgorithmRequirement> selectRequirementPage(Page<?> page, AlgorithmRequirement query) {
        QueryWrapper<AlgorithmRequirement> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage((Page<AlgorithmRequirement>) page, wrapper);
    }

    @Override
    public List<AlgorithmRequirement> selectRequirementList(AlgorithmRequirement query) {
        QueryWrapper<AlgorithmRequirement> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public AlgorithmRequirement selectRequirementDetail(Long id) {
        AlgorithmRequirement requirement = baseMapper.selectById(id);
        if (requirement != null) {
            requirement.setRequirementParams(paramMapper.selectParamsByRequirementId(id));
        }
        return requirement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertRequirement(AlgorithmRequirement requirement) {
        // 生成需求编号
        if (StringUtils.isBlank(requirement.getCode())) {
            requirement.setCode(generateCode());
        }
        // 设置默认状态
        if (StringUtils.isBlank(requirement.getStatus())) {
            requirement.setStatus("待构建");
        }
        if (StringUtils.isBlank(requirement.getDataStatus())) {
            requirement.setDataStatus("ENABLED");
        }
        if (StringUtils.isBlank(requirement.getNotifyStatus())) {
            requirement.setNotifyStatus("待通知");
        }
        int rows = baseMapper.insert(requirement);
        // 保存参数
        if (requirement.getRequirementParams() != null && !requirement.getRequirementParams().isEmpty()) {
            saveParams(requirement.getId(), requirement.getRequirementParams(), requirement.getCreateBy());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRequirement(AlgorithmRequirement requirement) {
        AlgorithmRequirement existing = baseMapper.selectById(requirement.getId());
        if (existing == null) {
            throw new ServiceException("需求不存在");
        }
        int rows = baseMapper.updateById(requirement);
        // 更新参数
        if (requirement.getRequirementParams() != null) {
            // 删除旧参数
            paramMapper.deleteParamsByRequirementIds(Arrays.asList(requirement.getId()));
            // 保存新参数
            if (!requirement.getRequirementParams().isEmpty()) {
                saveParams(requirement.getId(), requirement.getRequirementParams(), requirement.getUpdateBy());
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteRequirementByIds(Long[] ids) {
        for (Long id : ids) {
            paramMapper.deleteParamsByRequirementIds(Arrays.asList(id));
        }
        // 逻辑删除
        for (Long id : ids) {
            AlgorithmRequirement requirement = new AlgorithmRequirement();
            requirement.setId(id);
            requirement.setDelFlag("2");
            baseMapper.updateById(requirement);
        }
        return ids.length;
    }

    @Override
    public int updateBuildStatus(Long id, String algorithmName, Long algorithmId) {
        AlgorithmRequirement requirement = new AlgorithmRequirement();
        requirement.setId(id);
        requirement.setStatus("已构建");
        requirement.setAlgorithmName(algorithmName);
        requirement.setAlgorithmId(algorithmId);
        return baseMapper.updateById(requirement);
    }

    @Override
    public int updateNotifyStatus(Long id, String notifyStatus) {
        AlgorithmRequirement requirement = new AlgorithmRequirement();
        requirement.setId(id);
        requirement.setNotifyStatus(notifyStatus);
        if ("已通知".equals(notifyStatus)) {
            requirement.setNotifyTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        return baseMapper.updateById(requirement);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int receiveFromSubsystem(AlgorithmRequirement requirement) {
        // 设置来源标记
        if (StringUtils.isBlank(requirement.getSource())) {
            requirement.setSource("分系统对接");
        }
        // 设置创建信息
        if (StringUtils.isBlank(requirement.getCreateBy())) {
            requirement.setCreateBy("subsystem");
        }
        requirement.setCreateTime(new Date());
        // 生成需求编号
        if (StringUtils.isBlank(requirement.getCode())) {
            requirement.setCode(generateCode());
        }
        // 设置默认状态
        if (StringUtils.isBlank(requirement.getStatus())) {
            requirement.setStatus("待构建");
        }
        if (StringUtils.isBlank(requirement.getDataStatus())) {
            requirement.setDataStatus("ENABLED");
        }
        if (StringUtils.isBlank(requirement.getNotifyStatus())) {
            requirement.setNotifyStatus("待通知");
        }
        int rows = baseMapper.insert(requirement);
        // 保存参数
        if (requirement.getRequirementParams() != null && !requirement.getRequirementParams().isEmpty()) {
            saveParams(requirement.getId(), requirement.getRequirementParams(), requirement.getCreateBy());
        }
        return rows;
    }

    /**
     * 保存参数列表
     */
    private void saveParams(Long requirementId, List<AlgorithmRequirementParam> params, String createBy) {
        for (int i = 0; i < params.size(); i++) {
            AlgorithmRequirementParam param = params.get(i);
            param.setRequirementId(requirementId);
            param.setSortOrder(i + 1);
            param.setCreateBy(createBy);
            param.setCreateTime(new Date());
            paramMapper.insert(param);
        }
    }

    /**
     * 生成需求编号
     */
    private String generateCode() {
        String prefix = "ALG-REQ-";
        // 查询最新的编号
        QueryWrapper<AlgorithmRequirement> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time").last("LIMIT 1");
        AlgorithmRequirement latest = baseMapper.selectOne(wrapper);
        if (latest != null && latest.getCode() != null && latest.getCode().startsWith(prefix)) {
            String numStr = latest.getCode().substring(prefix.length());
            try {
                int num = Integer.parseInt(numStr);
                return prefix + String.format("%03d", num + 1);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return prefix + "001";
    }

    private QueryWrapper<AlgorithmRequirement> buildQueryWrapper(AlgorithmRequirement query) {
        QueryWrapper<AlgorithmRequirement> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", "0");
        if (query != null) {
            if (StringUtils.isNotBlank(query.getCode())) {
                wrapper.like("code", query.getCode());
            }
            if (StringUtils.isNotBlank(query.getTitle())) {
                wrapper.like("title", query.getTitle());
            }
            if (StringUtils.isNotBlank(query.getType())) {
                wrapper.eq("type", query.getType());
            }
            if (StringUtils.isNotBlank(query.getStatus())) {
                wrapper.eq("status", query.getStatus());
            }
            if (StringUtils.isNotBlank(query.getNotifyStatus())) {
                wrapper.eq("notify_status", query.getNotifyStatus());
            }
            if (StringUtils.isNotBlank(query.getSource())) {
                wrapper.eq("source", query.getSource());
            }
            if (query.getAlgorithmId() != null) {
                wrapper.eq("algorithm_id", query.getAlgorithmId());
            }
        }
        return wrapper;
    }
}
