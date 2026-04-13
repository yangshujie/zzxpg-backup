package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorTemplate;
import com.ruoyi.mapper.zhpg.EvalIndicatorTemplateMapper;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.service.zhpg.IEvalIndicatorTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

/**
 * 评估指标模板Service实现
 */
@Service
public class EvalIndicatorTemplateServiceImpl extends ServiceImpl<EvalIndicatorTemplateMapper, EvalIndicatorTemplate>
        implements IEvalIndicatorTemplateService {

    @Override
    public Page<EvalIndicatorTemplate> selectTemplatePage(Page page, EvalIndicatorTemplate query) {
        QueryWrapper<EvalIndicatorTemplate> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTemplate(EvalIndicatorTemplate template) {
        validateTemplateNameUnique(template, null);
        // 模板编号与主键一致（字符串形式）；插入前用临时占位满足 NOT NULL，插入后回写为 id
        template.setTemplateCode("TMP_" + UUID.randomUUID().toString().replace("-", ""));
        int inserted = baseMapper.insert(template);
        if (inserted == 0 || template.getId() == null) {
            return inserted;
        }
        template.setTemplateCode(String.valueOf(template.getId()));
        int updated = baseMapper.updateById(template);
        if (updated < 1) {
            throw new ServiceException("模板编号写入失败");
        }
        return inserted;
    }

    @Override
    public int updateTemplate(EvalIndicatorTemplate template) {
        validateTemplateNameUnique(template, template.getId());
        EvalIndicatorTemplate existing = baseMapper.selectById(template.getId());
        if (existing != null) {
            // 编号由系统管理，禁止前端篡改
            template.setTemplateCode(existing.getTemplateCode());
        }
        return baseMapper.updateById(template);
    }

    @Override
    public int deleteTemplateByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public boolean checkTemplateCodeUnique(EvalIndicatorTemplate template) {
        if (StringUtils.isEmpty(template.getTemplateCode())) {
            return true;
        }
        Long id = template.getId() == null ? -1L : template.getId();
        QueryWrapper<EvalIndicatorTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_code", template.getTemplateCode());
        EvalIndicatorTemplate existing = baseMapper.selectOne(wrapper);
        return existing == null || existing.getId().equals(id);
    }

    /** 模板名称全局唯一（去首尾空格），与列表「模板编号/主键」区分展示。 */
    private void validateTemplateNameUnique(EvalIndicatorTemplate template, Long excludeId) {
        if (template == null || StringUtils.isEmpty(template.getTemplateName())) {
            return;
        }
        String name = template.getTemplateName().trim();
        if (StringUtils.isEmpty(name)) {
            throw new ServiceException("模板名称不能为空");
        }
        template.setTemplateName(name);
        QueryWrapper<EvalIndicatorTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("模板名称已存在，请使用其他名称");
        }
    }

    private QueryWrapper<EvalIndicatorTemplate> buildQueryWrapper(EvalIndicatorTemplate query) {
        QueryWrapper<EvalIndicatorTemplate> wrapper = new QueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.isNotEmpty(query.getTemplateName()), "template_name", query.getTemplateName());
            wrapper.eq(StringUtils.isNotEmpty(query.getEquipmentType()), "equipment_type", query.getEquipmentType());
            wrapper.eq(StringUtils.isNotEmpty(query.getSystemType()), "system_type", query.getSystemType());
            wrapper.eq(StringUtils.isNotEmpty(query.getStatus()), "status", query.getStatus());
        }
        return wrapper;
    }
}
