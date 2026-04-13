package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.DictUtils;
import com.ruoyi.system.api.domain.SysDictData;
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.domain.zhpg.EvalIndicatorTemplate;
import com.ruoyi.mapper.zhpg.EvalIndicatorMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 评估指标Service实现
 */
@Service
public class EvalIndicatorServiceImpl extends ServiceImpl<EvalIndicatorMapper, EvalIndicator>
        implements IEvalIndicatorService {

    /** 装备类型（标准指标类型，下拉、校验） */
    private static final String DICT_EQUIPMENT_TYPE = "zhpg_equipment_type";
    private static final String DEFAULT_WORK_MODE = "内部流转";

    @Override
    public Page<EvalIndicator> selectIndicatorPage(Page page, EvalIndicator query) {
        QueryWrapper<EvalIndicator> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc("order_num");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<EvalIndicator> selectIndicatorList(EvalIndicator query) {
        QueryWrapper<EvalIndicator> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc("order_num");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public int insertIndicator(EvalIndicator indicator) {
        ensureIndicatorIdCode(indicator);
        normalizeAndValidate(indicator, null);
        normalizeWorkMode(indicator);
        return baseMapper.insert(indicator);
    }

    @Override
    public int updateIndicator(EvalIndicator indicator) {
        if (indicator.getId() == null) {
            throw new ServiceException("指标ID不能为空");
        }
        EvalIndicator existing = baseMapper.selectById(indicator.getId());
        if (existing == null) {
            throw new ServiceException("指标不存在或已删除");
        }

        EvalIndicator merged = buildMerged(existing, indicator);
        normalizeAndValidate(merged, indicator.getId());

        indicator.setParentId(merged.getParentId());
        indicator.setIsBottomNode(merged.getIsBottomNode());
        indicator.setLevel(merged.getLevel());
        indicator.setWorkMode(merged.getWorkMode());
        indicator.setIdCode(merged.getIdCode());
        if (!Boolean.TRUE.equals(merged.getIsBottomNode())) {
            indicator.setCalcMethod(null);
            indicator.setAlgorithmId(null);
            indicator.setValueMin(null);
            indicator.setValueMax(null);
            indicator.setValueCategory(null);
        }
        normalizeWorkMode(indicator);
        return baseMapper.updateById(indicator);
    }

    @Override
    public int deleteIndicatorByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public EvalIndicator instantiateFromTemplate(EvalIndicatorTemplate template, Long parentId, String operator) {
        if (template == null || template.getId() == null) {
            throw new ServiceException("模板不存在");
        }
        EvalIndicator indicator = new EvalIndicator();
        long pid = parentId == null ? 0L : parentId;
        indicator.setParentId(pid);
        indicator.setIndicatorName(template.getTemplateName());
        indicator.setIndicatorType(resolveIndicatorType(template));
        indicator.setSystemType(template.getSystemType());
        indicator.setDescription(template.getDescription());
        indicator.setIsBottomNode(false);
        indicator.setStatus("DRAFT");
        indicator.setCreateBy(operator);
        indicator.setOrderNum(0);
        indicator.setCalcMethod(null);
        indicator.setAlgorithmId(null);

        applyTemplateConfigJson(indicator, template, pid);

        insertIndicator(indicator);
        return baseMapper.selectById(indicator.getId());
    }

    /**
     * 解析模板 configJson（结构化指标默认配置），覆盖实例化字段。
     * 约定：{ "schemaVersion": 1, "indicator": { "indicatorName", "indicatorType", "nodeRole", "unit", "valueCategory", "valueMin", "valueMax", "workMode", "calcMethod", "computeRule", "description" } }
     * nodeRole：LEAF=底层；NON_LEAF=非底层（父级由实例化请求 parentId 决定）。兼容旧值 ROOT/MIDDLE（ROOT 仍强制 parentId=0）。
     */
    private void applyTemplateConfigJson(EvalIndicator indicator, EvalIndicatorTemplate template, Long requestParentId) {
        if (StringUtils.isEmpty(template.getConfigJson())) {
            return;
        }
        try {
            JSONObject root = JSON.parseObject(template.getConfigJson());
            if (root == null) {
                return;
            }
            JSONObject ind = root.getJSONObject("indicator");
            if (ind == null) {
                return;
            }
            String name = ind.getString("indicatorName");
            if (StringUtils.isNotEmpty(name)) {
                indicator.setIndicatorName(name);
            }
            String type = ind.getString("indicatorType");
            if (StringUtils.isNotEmpty(type)) {
                indicator.setIndicatorType(type);
            }
            String systemType = ind.getString("systemType");
            if (StringUtils.isNotEmpty(systemType)) {
                indicator.setSystemType(systemType);
            }
            String nodeRole = ind.getString("nodeRole");
            if ("LEAF".equals(nodeRole)) {
                indicator.setIsBottomNode(true);
            } else if ("NON_LEAF".equals(nodeRole) || "MIDDLE".equals(nodeRole) || "ROOT".equals(nodeRole)) {
                indicator.setIsBottomNode(false);
            }
            if ("ROOT".equals(nodeRole)) {
                indicator.setParentId(0L);
            } else if (!"ROOT".equals(nodeRole) && requestParentId != null && requestParentId != 0L) {
                indicator.setParentId(requestParentId);
            }
            String unit = ind.getString("unit");
            if (StringUtils.isNotEmpty(unit)) {
                indicator.setUnit(unit);
            }
            String valueCategory = ind.getString("valueCategory");
            if (StringUtils.isNotEmpty(valueCategory)) {
                indicator.setValueCategory(valueCategory);
            }
            BigDecimal vmin = ind.getBigDecimal("valueMin");
            if (vmin != null) {
                indicator.setValueMin(vmin);
            }
            BigDecimal vmax = ind.getBigDecimal("valueMax");
            if (vmax != null) {
                indicator.setValueMax(vmax);
            }
            String indWorkMode = ind.getString("workMode");
            if (StringUtils.isNotEmpty(indWorkMode)) {
                indicator.setWorkMode(indWorkMode);
            }
            JSONObject computeRule = ind.getJSONObject("computeRule");
            if (computeRule != null && !computeRule.isEmpty()) {
                indicator.setCalcMethod(computeRule.toJSONString());
            } else {
                String calc = ind.getString("calcMethod");
                if (StringUtils.isNotEmpty(calc)) {
                    indicator.setCalcMethod(calc);
                    if (StringUtils.isEmpty(indicator.getWorkMode())) {
                        indicator.setWorkMode(extractWorkMode(calc));
                    }
                }
            }
            String desc = ind.getString("description");
            if (StringUtils.isNotEmpty(desc)) {
                indicator.setDescription(desc);
            }
        } catch (Exception ignored) {
            // 保留模板元数据默认值
        }
    }

    @Override
    public List<EvalIndicator> selectParentCandidates(String indicatorType, String keyword, Long excludeId, Integer limit) {
        if (StringUtils.isEmpty(indicatorType)) {
            return new ArrayList<>();
        }
        final String normalizedIndicatorType = legacyResolveIndicatorTypeFromEquipment(indicatorType);
        int safeLimit = limit == null ? 50 : Math.max(10, Math.min(limit, 200));

        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        // 分类规则调整：允许选择同类型节点 或 “无”类型的节点作为父级
        wrapper.and(w -> w.eq("indicator_type", normalizedIndicatorType).or().eq("indicator_type", "无"))
                .eq("is_bottom_node", 0)
                .like(StringUtils.isNotEmpty(keyword), "indicator_name", keyword)
                .orderByAsc("indicator_level")
                .orderByAsc("order_num")
                .orderByAsc("id");

        List<EvalIndicator> candidates = baseMapper.selectList(wrapper);
        if (excludeId != null) {
            Set<Long> blockedIds = collectDescendantIds(excludeId, normalizedIndicatorType);
            blockedIds.add(excludeId);
            List<EvalIndicator> filtered = new ArrayList<>();
            for (EvalIndicator item : candidates) {
                if (!blockedIds.contains(item.getId())) {
                    filtered.add(item);
                }
            }
            candidates = filtered;
        }
        if (candidates.size() <= safeLimit) {
            return candidates;
        }
        return candidates.subList(0, safeLimit);
    }

    /**
     * 指标名称全局唯一（去首尾空格后比较），便于列表区分与业务引用。
     */
    private void validateIndicatorNameUnique(EvalIndicator indicator, Long currentId) {
        if (indicator == null || StringUtils.isEmpty(indicator.getIndicatorName())) {
            return;
        }
        String name = indicator.getIndicatorName().trim();
        if (StringUtils.isEmpty(name)) {
            throw new ServiceException("指标名称不能为空");
        }
        indicator.setIndicatorName(name);
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("indicator_name", name);
        if (currentId != null) {
            wrapper.ne("id", currentId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("指标名称已存在，请使用其他名称");
        }
    }

    private QueryWrapper<EvalIndicator> buildQueryWrapper(EvalIndicator query) {
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        if (query != null) {
            String indicatorType = query.getIndicatorType();
            if (StringUtils.isNotEmpty(indicatorType)) {
                indicatorType = legacyResolveIndicatorTypeFromEquipment(indicatorType);
            }
            String workMode = ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(query.getWorkMode(), query.getWorkMode());
            wrapper.like(StringUtils.isNotEmpty(query.getIndicatorName()), "indicator_name", query.getIndicatorName());
            wrapper.eq(StringUtils.isNotEmpty(indicatorType), "indicator_type", indicatorType);
            wrapper.eq(StringUtils.isNotEmpty(query.getSystemType()), "system_type", query.getSystemType());
            wrapper.eq(StringUtils.isNotEmpty(workMode), "work_mode", workMode);
            wrapper.eq(StringUtils.isNotEmpty(query.getStatus()), "status", query.getStatus());
            wrapper.eq(query.getParentId() != null, "parent_id", query.getParentId());
            wrapper.eq(query.getAlgorithmId() != null, "algorithm_id", query.getAlgorithmId());
            wrapper.eq(query.getIsBottomNode() != null, "is_bottom_node", query.getIsBottomNode());
        }
        return wrapper;
    }

    private EvalIndicator buildMerged(EvalIndicator existing, EvalIndicator incoming) {
        EvalIndicator merged = new EvalIndicator();
        merged.setId(existing.getId());
        merged.setIndicatorName(StringUtils.isNotEmpty(incoming.getIndicatorName()) ? incoming.getIndicatorName() : existing.getIndicatorName());
        merged.setIndicatorType(StringUtils.isNotEmpty(incoming.getIndicatorType()) ? incoming.getIndicatorType() : existing.getIndicatorType());
        merged.setSystemType(StringUtils.isNotEmpty(incoming.getSystemType()) ? incoming.getSystemType() : existing.getSystemType());
        merged.setParentId(incoming.getParentId() != null ? incoming.getParentId() : existing.getParentId());
        merged.setIsBottomNode(incoming.getIsBottomNode() != null ? incoming.getIsBottomNode() : existing.getIsBottomNode());
        merged.setUnit(StringUtils.isNotEmpty(incoming.getUnit()) ? incoming.getUnit() : existing.getUnit());
        merged.setWeight(incoming.getWeight() != null ? incoming.getWeight() : existing.getWeight());
        merged.setValueCategory(StringUtils.isNotEmpty(incoming.getValueCategory()) ? incoming.getValueCategory() : existing.getValueCategory());
        merged.setCalcMethod(StringUtils.isNotEmpty(incoming.getCalcMethod()) ? incoming.getCalcMethod() : existing.getCalcMethod());
        merged.setWorkMode(StringUtils.isNotEmpty(incoming.getWorkMode()) ? incoming.getWorkMode() : existing.getWorkMode());
        merged.setAlgorithmId(incoming.getAlgorithmId() != null ? incoming.getAlgorithmId() : existing.getAlgorithmId());
        merged.setValueMin(incoming.getValueMin() != null ? incoming.getValueMin() : existing.getValueMin());
        merged.setValueMax(incoming.getValueMax() != null ? incoming.getValueMax() : existing.getValueMax());
        merged.setOrderNum(incoming.getOrderNum() != null ? incoming.getOrderNum() : existing.getOrderNum());
        merged.setDescription(StringUtils.isNotEmpty(incoming.getDescription()) ? incoming.getDescription() : existing.getDescription());
        merged.setStatus(StringUtils.isNotEmpty(incoming.getStatus()) ? incoming.getStatus() : existing.getStatus());
        merged.setLevel(existing.getLevel());
        merged.setIdCode(StringUtils.isNotEmpty(incoming.getIdCode()) ? incoming.getIdCode().trim() : existing.getIdCode());
        return merged;
    }

    private void ensureIndicatorIdCode(EvalIndicator indicator) {
        if (indicator == null) {
            return;
        }
        if (StringUtils.isEmpty(indicator.getIdCode())) {
            indicator.setIdCode("ind_" + UUID.randomUUID().toString().replace("-", ""));
        } else {
            indicator.setIdCode(indicator.getIdCode().trim());
        }
    }

    private void validateIndicatorIdCodeUnique(EvalIndicator indicator, Long excludeId) {
        if (indicator == null || StringUtils.isEmpty(indicator.getIdCode())) {
            return;
        }
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("id_code", indicator.getIdCode());
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("指标ID编码已存在，请使用其他编码");
        }
    }

    private void normalizeAndValidate(EvalIndicator indicator, Long currentId) {
        if (indicator.getParentId() == null) {
            indicator.setParentId(0L);
        }
        validateIndicatorNameUnique(indicator, currentId);
        validateIndicatorIdCodeUnique(indicator, currentId);
        if (StringUtils.isEmpty(indicator.getIndicatorType())) {
            throw new ServiceException("指标类型不能为空");
        }
        indicator.setIndicatorType(legacyResolveIndicatorTypeFromEquipment(indicator.getIndicatorType()));
        validateIndicatorTypeDict(indicator.getIndicatorType());
        indicator.setWorkMode(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(indicator.getWorkMode(), DEFAULT_WORK_MODE));
        indicator.setValueCategory(normalizeValueCategory(indicator.getValueCategory()));
        if (indicator.getIsBottomNode() == null) {
            indicator.setIsBottomNode(true);
        }

        validateParent(indicator, currentId);
        validateChildrenTypeConsistency(currentId, indicator.getIndicatorType());
        if (Boolean.TRUE.equals(indicator.getIsBottomNode()) && currentId != null && existsChildren(currentId)) {
            throw new ServiceException("当前指标存在子指标，不能设置为底层指标");
        }

        if (!Boolean.TRUE.equals(indicator.getIsBottomNode())) {
            // 非底层节点仅维护通用属性，不维护具体计算属性。
            indicator.setCalcMethod(null);
            indicator.setAlgorithmId(null);
            indicator.setValueMin(null);
            indicator.setValueMax(null);
            indicator.setValueCategory(null);
        }

        if (StringUtils.isEmpty(indicator.getWorkMode())) {
            indicator.setWorkMode(extractWorkMode(indicator.getCalcMethod()));
        }
        if (StringUtils.isEmpty(indicator.getWorkMode())) {
            indicator.setWorkMode(DEFAULT_WORK_MODE);
        }

        indicator.setLevel(calcLevel(indicator.getParentId()));
    }

    private void normalizeWorkMode(EvalIndicator indicator) {
        if (indicator == null) {
            return;
        }
        indicator.setWorkMode(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(indicator.getWorkMode(), DEFAULT_WORK_MODE));
        if (StringUtils.isEmpty(indicator.getWorkMode())) {
            indicator.setWorkMode(extractWorkMode(indicator.getCalcMethod()));
        }
        if (StringUtils.isEmpty(indicator.getWorkMode())) {
            indicator.setWorkMode(DEFAULT_WORK_MODE);
        }
    }

    private String extractWorkMode(String calcMethod) {
        if (StringUtils.isEmpty(calcMethod)) {
            return DEFAULT_WORK_MODE;
        }
        try {
            JSONObject calc = JSON.parseObject(calcMethod);
            String workMode = calc.getString("workMode");
            return StringUtils.isNotEmpty(workMode)
                    ? ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(workMode, DEFAULT_WORK_MODE)
                    : DEFAULT_WORK_MODE;
        } catch (Exception ignored) {
            return DEFAULT_WORK_MODE;
        }
    }

    private String normalizeValueCategory(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return raw;
        }
        String v = raw.trim();
        if ("COST".equals(v) || "成本型".equals(v)) {
            return "成本型";
        }
        if ("BENEFIT".equals(v) || "效益型".equals(v)) {
            return "效益型";
        }
        if ("INTERVAL_BENEFIT".equals(v) || "区间效益型".equals(v)) {
            return "区间效益型";
        }
        return v;
    }

    private void validateParent(EvalIndicator indicator, Long currentId) {
        Long parentId = indicator.getParentId();
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (currentId != null && Objects.equals(currentId, parentId)) {
            throw new ServiceException("父级指标不能选择当前指标自身");
        }
        EvalIndicator parent = baseMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("父级指标不存在");
        }
        if (resolveBottomNode(parent)) {
            throw new ServiceException("父级指标为底层指标，不能挂载子指标，请先将其改为非底层指标");
        }
        // 分类规则调整：如果父节点是“无”类型，允许挂载任何类型的子指标；否则保持同类型约束。
        if (!"无".equals(parent.getIndicatorType())
                && StringUtils.isNotEmpty(parent.getIndicatorType())
                && StringUtils.isNotEmpty(indicator.getIndicatorType())
                && !Objects.equals(parent.getIndicatorType(), indicator.getIndicatorType())) {
            throw new ServiceException("父子节点指标类型不匹配（非无节点不能跨类型关联）");
        }
        if (currentId != null && createsCycle(currentId, parentId)) {
            throw new ServiceException("父级指标设置不合法，会导致循环层级关系");
        }
    }

    private Integer calcLevel(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return 1;
        }
        EvalIndicator parent = baseMapper.selectById(parentId);
        if (parent == null || parent.getLevel() == null) {
            return 2;
        }
        return parent.getLevel() + 1;
    }

    private boolean resolveBottomNode(EvalIndicator indicator) {
        if (indicator.getIsBottomNode() != null) {
            return indicator.getIsBottomNode();
        }
        return !existsChildren(indicator.getId());
    }

    private boolean existsChildren(Long indicatorId) {
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", indicatorId);
        return baseMapper.selectCount(wrapper) > 0;
    }

    private void validateChildrenTypeConsistency(Long currentId, String indicatorType) {
        if (currentId == null || StringUtils.isEmpty(indicatorType)) {
            return;
        }
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", currentId)
                .ne("indicator_type", indicatorType);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("当前节点存在不同类型的子节点，不能修改为该指标类型");
        }
    }

    private boolean createsCycle(Long currentId, Long parentId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (Objects.equals(cursor, currentId)) {
                return true;
            }
            if (!visited.add(cursor)) {
                return true;
            }
            EvalIndicator node = baseMapper.selectById(cursor);
            if (node == null) {
                break;
            }
            cursor = node.getParentId();
        }
        return false;
    }

    private Set<Long> collectDescendantIds(Long rootId, String indicatorType) {
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.select("id", "parent_id")
                .eq(StringUtils.isNotEmpty(indicatorType), "indicator_type", indicatorType);
        List<EvalIndicator> nodes = baseMapper.selectList(wrapper);
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (EvalIndicator node : nodes) {
            Long pid = node.getParentId() == null ? 0L : node.getParentId();
            List<Long> children = childrenMap.computeIfAbsent(pid, k -> new ArrayList<>());
            children.add(node.getId());
        }

        Set<Long> descendants = new HashSet<>();
        List<Long> queue = new ArrayList<>();
        queue.add(rootId);
        for (int i = 0; i < queue.size(); i++) {
            Long current = queue.get(i);
            List<Long> children = childrenMap.get(current);
            if (children == null) {
                continue;
            }
            for (Long childId : children) {
                if (descendants.add(childId)) {
                    queue.add(childId);
                }
            }
        }
        return descendants;
    }

    private String resolveIndicatorType(EvalIndicatorTemplate template) {
        if (StringUtils.isEmpty(template.getEquipmentType())) {
            throw new ServiceException("模板缺少可映射的指标类型（equipment_type），无法生成指标");
        }
        String eq = legacyResolveIndicatorTypeFromEquipment(template.getEquipmentType());
        List<SysDictData> mapList = DictUtils.getDictCache(DICT_EQUIPMENT_TYPE);
        if (mapList != null && !mapList.isEmpty()) {
            for (SysDictData row : mapList) {
                if (!"0".equals(row.getStatus())) {
                    continue;
                }
                if (eq.equals(legacyResolveIndicatorTypeFromEquipment(row.getDictValue()))) {
                    return eq;
                }
            }
            validateIndicatorTypeDict(eq);
            return eq;
        }
        return eq;
    }

    /**
     * 模板装备类型兼容：标准码原样返回；历史别名在代码中收敛（不要求字典必配旧码行）。
     */
    private String legacyResolveIndicatorTypeFromEquipment(String eq) {
        if ("航天侦察".equals(eq)
                || "太空态势感知".equals(eq)
                || "太空攻防".equals(eq)
                || "航天测运控".equals(eq)
                || "航天发射".equals(eq)
                || "海基航天".equals(eq)
                || "无".equals(eq)) {
            return eq;
        }
        switch (eq) {
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
                break;
        }
        throw new ServiceException("模板缺少可映射的指标类型（equipment_type），无法生成指标");
    }

    /** 字典 zhpg_equipment_type 有缓存时，校验指标类型必须在允许列表中 */
    private void validateIndicatorTypeDict(String indicatorType) {
        List<SysDictData> types = DictUtils.getDictCache(DICT_EQUIPMENT_TYPE);
        if (types == null || types.isEmpty()) {
            return;
        }
        String normalizedType = legacyResolveIndicatorTypeFromEquipment(indicatorType);
        for (SysDictData row : types) {
            if (!"0".equals(row.getStatus())) {
                continue;
            }
            if (normalizedType.equals(legacyResolveIndicatorTypeFromEquipment(row.getDictValue()))) {
                return;
            }
        }
        throw new ServiceException("指标类型不在字典「" + DICT_EQUIPMENT_TYPE + "」允许范围内，请从选项中选择或联系管理员配置");
    }
}
