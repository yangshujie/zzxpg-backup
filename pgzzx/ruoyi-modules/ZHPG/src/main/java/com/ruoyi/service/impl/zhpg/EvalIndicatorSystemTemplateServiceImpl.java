package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystemTemplate;
import com.ruoyi.mapper.zhpg.EvalIndicatorSystemTemplateMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemTemplateService;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

/**
 * 指标体系模板Service实现
 */
@Service
public class EvalIndicatorSystemTemplateServiceImpl
        extends ServiceImpl<EvalIndicatorSystemTemplateMapper, EvalIndicatorSystemTemplate>
        implements IEvalIndicatorSystemTemplateService {

    private static final String DEFAULT_WORK_MODE = "内部流转";

    @Override
    public Page<EvalIndicatorSystemTemplate> selectTemplatePage(Page page, EvalIndicatorSystemTemplate query) {
        QueryWrapper<EvalIndicatorSystemTemplate> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTemplate(EvalIndicatorSystemTemplate template) {
        validateTemplateNameUnique(template, null);
        normalizeTemplateTypes(template);
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
    public int updateTemplate(EvalIndicatorSystemTemplate template) {
        validateTemplateNameUnique(template, template.getId());
        normalizeTemplateTypes(template);
        EvalIndicatorSystemTemplate existing = baseMapper.selectById(template.getId());
        if (existing != null) {
            template.setTemplateCode(existing.getTemplateCode());
        }
        return baseMapper.updateById(template);
    }

    @Override
    public int deleteTemplateByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public boolean checkTemplateCodeUnique(EvalIndicatorSystemTemplate template) {
        if (StringUtils.isEmpty(template.getTemplateCode())) {
            return true;
        }
        Long id = template.getId() == null ? -1L : template.getId();
        QueryWrapper<EvalIndicatorSystemTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_code", template.getTemplateCode());
        EvalIndicatorSystemTemplate existing = baseMapper.selectOne(wrapper);
        return existing == null || existing.getId().equals(id);
    }

    private void validateTemplateNameUnique(EvalIndicatorSystemTemplate template, Long excludeId) {
        if (template == null || StringUtils.isEmpty(template.getTemplateName())) {
            return;
        }
        String name = template.getTemplateName().trim();
        if (StringUtils.isEmpty(name)) {
            throw new ServiceException("模板名称不能为空");
        }
        template.setTemplateName(name);
        QueryWrapper<EvalIndicatorSystemTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("模板名称已存在，请使用其他名称");
        }
    }

    private QueryWrapper<EvalIndicatorSystemTemplate> buildQueryWrapper(EvalIndicatorSystemTemplate query) {
        QueryWrapper<EvalIndicatorSystemTemplate> wrapper = new QueryWrapper<>();
        if (query != null) {
            String systemType = normalizeSystemType(query.getSystemType());
            String equipmentType = normalizeEquipmentType(query.getEquipmentType());
            wrapper.like(StringUtils.isNotEmpty(query.getTemplateName()), "template_name", query.getTemplateName());
            wrapper.eq(StringUtils.isNotEmpty(systemType), "system_type", systemType);
            wrapper.eq(StringUtils.isNotEmpty(equipmentType), "equipment_type", equipmentType);
            wrapper.eq(StringUtils.isNotEmpty(query.getStatus()), "status", query.getStatus());
        }
        return wrapper;
    }

    private void normalizeTemplateTypes(EvalIndicatorSystemTemplate template) {
        if (template == null) {
            return;
        }
        template.setSystemType(normalizeSystemType(template.getSystemType()));
        template.setEquipmentType(normalizeEquipmentType(template.getEquipmentType()));
        if (StringUtils.isNotEmpty(template.getIndicatorTree())) {
            template.setIndicatorTree(ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTreeTypes(template.getIndicatorTree()));
        }
        String fallback = ZhpgIndicatorTreeJsonHelper.extractWorkMode(template.getIndicatorTree(), DEFAULT_WORK_MODE);
        template.setWorkMode(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(template.getWorkMode(), fallback));
    }

    private String normalizeSystemType(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return raw;
        }
        String v = raw.trim();
        switch (v) {
            case "EQUIPMENT_COMBAT_EFFECTIVENESS":
            case "COMBAT_EFFECTIVENESS":
                return "装备作战效能";
            case "COMBAT_APPLICABILITY":
                return "作战适用性";
            case "SYSTEM_APPLICABILITY":
            case "SYSTEM_CONTRIBUTION":
                return "体系适用性";
            case "SERVICE_APPLICABILITY":
            case "CAPABILITY_MATURITY":
                return "在役适用性";
            default:
                return v;
        }
    }

    private String normalizeEquipmentType(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return raw;
        }
        String v = raw.trim();
        switch (v) {
            case "SPACE_RECON":
                return "航天侦察";
            case "SPACE_SITUATIONAL_AWARENESS":
            case "SPACE_AWARENESS":
            case "SPACE_SA":
                return "太空态势感知";
            case "SPACE_OFFENSE_DEFENSE":
            case "SPACE_AD":
            case "SPACE_COMBAT":
                return "太空攻防";
            case "SPACE_TTC":
            case "COMM_COUNTER":
                return "航天测运控";
            case "SPACE_LAUNCH":
                return "航天发射";
            case "SEA_BASED_SPACE":
            case "SEA_BASED":
            case "NAV_POSITION":
                return "海基航天";
            case "SYSTEM_AGGREGATION":
                return "无";
            default:
                return v;
        }
    }
}
