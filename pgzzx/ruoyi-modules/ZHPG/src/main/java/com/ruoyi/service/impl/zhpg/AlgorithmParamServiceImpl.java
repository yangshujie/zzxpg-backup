package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.domain.zhpg.AlgorithmParam;
import com.ruoyi.mapper.zhpg.AlgorithmParamMapper;
import com.ruoyi.service.zhpg.IAlgorithmParamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 算法参数Service实现
 */
@Service
public class AlgorithmParamServiceImpl extends ServiceImpl<AlgorithmParamMapper, AlgorithmParam>
        implements IAlgorithmParamService {

    @Override
    public List<AlgorithmParam> selectParamsByAlgorithmId(Long algorithmId) {
        QueryWrapper<AlgorithmParam> wrapper = new QueryWrapper<>();
        wrapper.eq("algorithm_id", algorithmId)
               .orderByAsc("param_category", "sort_order");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<AlgorithmParam> selectParamsByCategory(Long algorithmId, String paramCategory) {
        QueryWrapper<AlgorithmParam> wrapper = new QueryWrapper<>();
        wrapper.eq("algorithm_id", algorithmId)
               .eq("param_category", paramCategory)
               .orderByAsc("sort_order");
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveParams(Long algorithmId, List<AlgorithmParam> params, String username) {
        // 先删除原有参数
        deleteParamsByAlgorithmId(algorithmId);
        // 批量插入新参数
        if (params != null && !params.isEmpty()) {
            Date now = new Date();
            for (int i = 0; i < params.size(); i++) {
                AlgorithmParam param = params.get(i);
                param.setId(null);
                param.setAlgorithmId(algorithmId);
                param.setCreateBy(username);
                param.setCreateTime(now);
                if (param.getSortOrder() == null) {
                    param.setSortOrder(i + 1);
                }
                baseMapper.insert(param);
            }
        }
    }

    @Override
    public int deleteParamsByAlgorithmId(Long algorithmId) {
        QueryWrapper<AlgorithmParam> wrapper = new QueryWrapper<>();
        wrapper.eq("algorithm_id", algorithmId);
        return baseMapper.delete(wrapper);
    }
}
