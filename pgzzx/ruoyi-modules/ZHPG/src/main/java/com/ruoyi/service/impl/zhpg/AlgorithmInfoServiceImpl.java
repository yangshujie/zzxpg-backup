package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.AlgorithmInfo;
import com.ruoyi.domain.zhpg.AlgorithmParam;
import com.ruoyi.mapper.zhpg.AlgorithmInfoMapper;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.service.zhpg.IAlgorithmParamService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 算法信息Service实现
 */
@Service
public class AlgorithmInfoServiceImpl extends ServiceImpl<AlgorithmInfoMapper, AlgorithmInfo>
        implements IAlgorithmInfoService {

    @Autowired
    private IAlgorithmParamService paramService;

    @Override
    public Page<AlgorithmInfo> selectAlgorithmPage(Page<?> page, AlgorithmInfo query) {
        QueryWrapper<AlgorithmInfo> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage((Page<AlgorithmInfo>) page, wrapper);
    }

    @Override
    public List<AlgorithmInfo> selectAlgorithmList(AlgorithmInfo query) {
        QueryWrapper<AlgorithmInfo> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public AlgorithmInfo selectAlgorithmDetail(Long id) {
        AlgorithmInfo info = baseMapper.selectById(id);
        if (info != null) {
            info.setAlgorithmParams(paramService.selectParamsByAlgorithmId(id));
        }
        return info;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAlgorithm(AlgorithmInfo algorithm) {
        validateAlgorithmName(algorithm.getAlgorithmName(), null);
        if (StringUtils.isBlank(algorithm.getPublishStatus())) {
            algorithm.setPublishStatus("DRAFT");
        }
        if (StringUtils.isBlank(algorithm.getStatus())) {
            algorithm.setStatus("ENABLED");
        }
        if (StringUtils.isBlank(algorithm.getAlgorithmVersion())) {
            algorithm.setAlgorithmVersion(null);
        }
        int rows = baseMapper.insert(algorithm);
        // 保存参数
        if (algorithm.getAlgorithmParams() != null && !algorithm.getAlgorithmParams().isEmpty()) {
            paramService.saveParams(algorithm.getId(), algorithm.getAlgorithmParams(), algorithm.getCreateBy());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateAlgorithm(AlgorithmInfo algorithm) {
        AlgorithmInfo existing = baseMapper.selectById(algorithm.getId());
        if (existing == null) {
            throw new ServiceException("算法不存在");
        }
        if ("PUBLISHED".equals(existing.getPublishStatus())) {
            throw new ServiceException("已发布的算法不能修改，请先撤回发布");
        }
        validateAlgorithmName(algorithm.getAlgorithmName(), algorithm.getId());
        if (StringUtils.isBlank(algorithm.getAlgorithmVersion())) {
            algorithm.setAlgorithmVersion(null);
        }
        int rows = baseMapper.updateById(algorithm);
        // 更新参数
        if (algorithm.getAlgorithmParams() != null) {
            paramService.saveParams(algorithm.getId(), algorithm.getAlgorithmParams(), algorithm.getUpdateBy());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAlgorithmByIds(Long[] ids) {
        for (Long id : ids) {
            AlgorithmInfo existing = baseMapper.selectById(id);
            if (existing != null && "PUBLISHED".equals(existing.getPublishStatus())) {
                throw new ServiceException("已发布的算法[" + existing.getAlgorithmName() + "]不能删除");
            }
            paramService.deleteParamsByAlgorithmId(id);
        }
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public int publishAlgorithm(Long id) {
        AlgorithmInfo info = baseMapper.selectById(id);
        if (info == null) {
            throw new ServiceException("算法不存在");
        }
        if ("PUBLISHED".equals(info.getPublishStatus())) {
            throw new ServiceException("算法已处于发布状态");
        }
        info.setPublishStatus("PUBLISHED");
        return baseMapper.updateById(info);
    }

    @Override
    public int unpublishAlgorithm(Long id) {
        AlgorithmInfo info = baseMapper.selectById(id);
        if (info == null) {
            throw new ServiceException("算法不存在");
        }
        if ("DRAFT".equals(info.getPublishStatus())) {
            throw new ServiceException("算法已处于草稿状态");
        }
        info.setPublishStatus("DRAFT");
        return baseMapper.updateById(info);
    }

    private QueryWrapper<AlgorithmInfo> buildQueryWrapper(AlgorithmInfo query) {
        QueryWrapper<AlgorithmInfo> wrapper = new QueryWrapper<>();
        if (query != null) {
            if (StringUtils.isNotBlank(query.getAlgorithmName())) {
                wrapper.like("algorithm_name", query.getAlgorithmName());
            }
            if (StringUtils.isNotBlank(query.getAlgorithmType())) {
                wrapper.eq("algorithm_type", query.getAlgorithmType());
            }
            if (StringUtils.isNotBlank(query.getEquipmentType())) {
                wrapper.eq("equipment_type", query.getEquipmentType());
            }
            if (StringUtils.isNotBlank(query.getPublishStatus())) {
                wrapper.eq("publish_status", query.getPublishStatus());
            }
            if (StringUtils.isNotBlank(query.getStatus())) {
                wrapper.eq("status", query.getStatus());
            }
        }
        return wrapper;
    }

    private void validateAlgorithmName(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        QueryWrapper<AlgorithmInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("algorithm_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("算法名称'" + name + "'已存在");
        }
    }
}
