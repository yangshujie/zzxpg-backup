package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalTaskTemplate;
import com.ruoyi.mapper.zhpg.EvalTaskTemplateMapper;
import com.ruoyi.service.zhpg.IEvalTaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评估任务模板Service实现
 */
@Slf4j
@Service
public class EvalTaskTemplateServiceImpl extends ServiceImpl<EvalTaskTemplateMapper, EvalTaskTemplate>
        implements IEvalTaskTemplateService {

    private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Override
    public Page<EvalTaskTemplate> selectTemplatePage(Page<?> page, EvalTaskTemplate query) {
        QueryWrapper<EvalTaskTemplate> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time", "create_time");
        return baseMapper.selectPage((Page<EvalTaskTemplate>) page, wrapper);
    }

    @Override
    public List<EvalTaskTemplate> selectTemplateList(EvalTaskTemplate query) {
        QueryWrapper<EvalTaskTemplate> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time", "create_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public EvalTaskTemplate selectTemplateDetail(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTemplate(EvalTaskTemplate template) {
        validateTemplateName(template.getTemplateName(), null);
        if (StringUtils.isBlank(template.getTemplateCode())) {
            template.setTemplateCode("ETT" + String.format("%06d", CODE_SEQ.incrementAndGet()));
        }
        if (StringUtils.isBlank(template.getClassification())) {
            template.setClassification("SPECIFIC");
        }
        if (StringUtils.isBlank(template.getStatus())) {
            template.setStatus("DISABLED");
        }
        return baseMapper.insert(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTemplate(EvalTaskTemplate template) {
        EvalTaskTemplate existing = baseMapper.selectById(template.getId());
        if (existing == null) {
            throw new ServiceException("评估任务模板不存在");
        }
        validateTemplateName(template.getTemplateName(), template.getId());
        return baseMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTemplateByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public int disableTemplate(Long id) {
        EvalTaskTemplate template = requireTemplate(id);
        if (!"PUBLISHED".equals(template.getStatus())) {
            throw new ServiceException("仅已启用状态可停用");
        }
        return changeStatus(template, "DISABLED");
    }

    @Override
    public int enableTemplate(Long id) {
        EvalTaskTemplate template = requireTemplate(id);
        if ("PUBLISHED".equals(template.getStatus())) {
            throw new ServiceException("模板已处于启用状态");
        }
        return changeStatus(template, "PUBLISHED");
    }

    // ==================== 私有方法 ====================

    private QueryWrapper<EvalTaskTemplate> buildQueryWrapper(EvalTaskTemplate query) {
        QueryWrapper<EvalTaskTemplate> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        if (StringUtils.isNotBlank(query.getTemplateName())) {
            wrapper.like("template_name", query.getTemplateName());
        }
        if (StringUtils.isNotBlank(query.getTemplateType())) {
            wrapper.eq("template_type", query.getTemplateType());
        }
        if (StringUtils.isNotBlank(query.getClassification())) {
            wrapper.eq("classification", query.getClassification());
        }
        if (StringUtils.isNotBlank(query.getEquipmentType())) {
            wrapper.eq("equipment_type", query.getEquipmentType());
        }
        if (StringUtils.isNotBlank(query.getCalcGranularity())) {
            wrapper.eq("calc_granularity", query.getCalcGranularity());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus());
        }
        return wrapper;
    }

    private EvalTaskTemplate requireTemplate(Long id) {
        EvalTaskTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("评估任务模板不存在");
        }
        return template;
    }

    private int changeStatus(EvalTaskTemplate template, String status) {
        template.setStatus(status);
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return baseMapper.updateById(template);
    }

    private void validateTemplateName(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("模板名称不能为空");
        }
        QueryWrapper<EvalTaskTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("模板名称已存在：" + name);
        }
    }

}
