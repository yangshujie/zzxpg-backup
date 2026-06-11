package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.EvalCriterionSet;
import com.ruoyi.mapper.zhpg.EvalCriterionSetMapper;
import com.ruoyi.service.zhpg.IEvalCriterionSetService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评估准则集Service实现
 */
@Slf4j
@Service
public class EvalCriterionSetServiceImpl extends ServiceImpl<EvalCriterionSetMapper, EvalCriterionSet>
        implements IEvalCriterionSetService {

    private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Page<EvalCriterionSet> selectCriterionSetPage(Page<?> page, EvalCriterionSet query) {
        QueryWrapper<EvalCriterionSet> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time", "create_time");
        return baseMapper.selectPage((Page<EvalCriterionSet>) page, wrapper);
    }

    @Override
    public List<EvalCriterionSet> selectCriterionSetList(EvalCriterionSet query) {
        QueryWrapper<EvalCriterionSet> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time", "create_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public EvalCriterionSet selectCriterionSetDetail(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCriterionSet(EvalCriterionSet set) {
        validateSetName(set.getSetName(), null);
        if (StringUtils.isBlank(set.getSetCode())) {
            set.setSetCode("ECS" + String.format("%06d", CODE_SEQ.incrementAndGet()));
        }
        if (StringUtils.isBlank(set.getStatus())) {
            set.setStatus("DRAFT");
        }
        set.setCreateTime(new Date());
        return baseMapper.insert(set);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCriterionSet(EvalCriterionSet set) {
        EvalCriterionSet existing = baseMapper.selectById(set.getId());
        if (existing == null) {
            throw new ServiceException("评估准则集不存在");
        }
        validateSetName(set.getSetName(), set.getId());
        set.setUpdateTime(new Date());
        return baseMapper.updateById(set);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteCriterionSetByIds(Long[] ids) {
        // TODO: 校验是否被下游任务模板引用
        if (ids != null && ids.length > 0) {
            // 物理清空这些准则集底下的明细数据，防止删除准则集后明细数据残留占用唯一约束键
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) {
                    placeholders.append(",");
                }
                placeholders.append("?");
            }
            String sql = "DELETE FROM pgzc_eval_criterion WHERE set_id IN (" + placeholders.toString() + ")";
            jdbcTemplate.update(sql, (Object[]) ids);
        }
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    // ==================== 私有方法 ====================

    private QueryWrapper<EvalCriterionSet> buildQueryWrapper(EvalCriterionSet query) {
        QueryWrapper<EvalCriterionSet> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        if (StringUtils.isNotBlank(query.getSetName())) {
            wrapper.like("set_name", query.getSetName());
        }
        if (query.getIndicatorSystemId() != null) {
            wrapper.eq("indicator_system_id", query.getIndicatorSystemId());
        }
        if (StringUtils.isNotBlank(query.getEquipmentType())) {
            wrapper.eq("equipment_type", query.getEquipmentType());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus());
        }
        if (query.getParams() != null && query.getParams().get("systemName") != null) {
            String systemName = query.getParams().get("systemName").toString();
            if (StringUtils.isNotBlank(systemName)) {
                wrapper.inSql("indicator_system_id", "SELECT id FROM pgzc_indicator_system WHERE system_name LIKE '%" + systemName.replace("'", "''") + "%'");
            }
        }
        return wrapper;
    }

    private void validateSetName(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("准则集名称不能为空");
        }
        QueryWrapper<EvalCriterionSet> wrapper = new QueryWrapper<>();
        wrapper.eq("set_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("准则集名称已存在：" + name);
        }
    }
}
