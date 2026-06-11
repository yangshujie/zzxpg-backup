package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.mapper.zhpg.EvalIndicatorMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 评估指标 Service 实现（指标 + 指标模板共表）
 *
 * 字段一致性约定：
 *   - 表内 is_template = 0/1 区分指标与模板，所有 CRUD 都在同一张表上完成
 *   - is_applied = 0/1 表示是否启用（替代旧的 status DRAFT/PUBLISHED）
 *   - 父子关系仅靠 parent_id 维护；同 indicator_type 或「无」类型节点之间可挂载
 */
@Service
public class EvalIndicatorServiceImpl extends ServiceImpl<EvalIndicatorMapper, EvalIndicator>
        implements IEvalIndicatorService {

    private static final Set<String> EQUIPMENT_TYPE_VALUES = new HashSet<>(Arrays.asList(
            "space_recon",
            "space_domain_awareness",
            "space_defense",
            "space_track_control",
            "space_launch",
            "sea_based_space",
            "无"
    ));

    @Override
    public Page<EvalIndicator> selectIndicatorPage(Page page, EvalIndicator query) {
        QueryWrapper<EvalIndicator> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc("indicator_level").orderByAsc("order_num").orderByAsc("id");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<EvalIndicator> selectIndicatorList(EvalIndicator query) {
        QueryWrapper<EvalIndicator> wrapper = buildQueryWrapper(query);
        wrapper.orderByAsc("indicator_level").orderByAsc("order_num").orderByAsc("id");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public int insertIndicator(EvalIndicator indicator) {
        ensureIndicatorIdCode(indicator);
        normalizeAndValidate(indicator, null);
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

        if (!isBottom(merged.getIsBottomNode())) {
            merged.setCalcMethod(null);
            merged.setAlgorithmId(null);
            merged.setValueMin(null);
            merged.setValueMax(null);
            merged.setValueCategory(null);
        }
        merged.setUpdateBy(indicator.getUpdateBy());
        merged.setUpdateTime(indicator.getUpdateTime());
        return baseMapper.updateById(merged);
    }

    @Override
    public int deleteIndicatorByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        for (Long id : ids) {
            List<EvalIndicator> children = baseMapper.selectList(new QueryWrapper<EvalIndicator>().eq("parent_id", id));
            if (children != null && !children.isEmpty()) {
                EvalIndicator indicator = baseMapper.selectById(id);
                String name = indicator != null ? indicator.getIndicatorName() : String.valueOf(id);
                
                StringBuilder childNames = new StringBuilder();
                for (int i = 0; i < Math.min(children.size(), 3); i++) {
                    if (i > 0) childNames.append("、");
                    childNames.append("「").append(children.get(i).getIndicatorName()).append("」");
                }
                if (children.size() > 3) {
                    childNames.append(" 等共 ").append(children.size()).append(" 个");
                }
                
                throw new ServiceException("指标「" + name + "」下存在子指标" + childNames.toString() + "，请先删除或迁移子指标后再操作");
            }
        }
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public List<EvalIndicator> selectParentCandidates(String indicatorType, Boolean isTemplate, String keyword,
                                                      Long excludeId, Integer limit) {
        if (StringUtils.isEmpty(indicatorType)) {
            return new ArrayList<>();
        }
        final String normalizedType = legacyResolveIndicatorTypeFromEquipment(indicatorType);
        int safeLimit = limit == null ? 50 : Math.max(10, Math.min(limit, 200));
        Integer flag = isTemplate == null ? null : (isTemplate ? 1 : 0);

        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("indicator_type", normalizedType).or().eq("indicator_type", "无"))
                .eq("is_bottom_node", 0)
                .eq(flag != null, "is_template", flag)
                .like(StringUtils.isNotEmpty(keyword), "indicator_name", keyword)
                .orderByAsc("indicator_level")
                .orderByAsc("order_num")
                .orderByAsc("id");

        List<EvalIndicator> candidates = baseMapper.selectList(wrapper);
        if (excludeId != null) {
            Set<Long> blocked = collectDescendantIds(excludeId, normalizedType);
            blocked.add(excludeId);
            List<EvalIndicator> filtered = new ArrayList<>(candidates.size());
            for (EvalIndicator item : candidates) {
                if (!blocked.contains(item.getId())) {
                    filtered.add(item);
                }
            }
            candidates = filtered;
        }
        return candidates.size() <= safeLimit ? candidates : candidates.subList(0, safeLimit);
    }

    @Override
    public IndicatorTreeNode buildIndicatorTree(Long rootId) {
        if (rootId == null) {
            return null;
        }
        EvalIndicator root = baseMapper.selectById(rootId);
        if (root == null) {
            return null;
        }
        // 仅在指标实例（is_template=0）中拉取
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.and(w -> w.eq("indicator_type", root.getIndicatorType()).or().eq("indicator_type", "无"))
                .eq("is_template", 0)
                .orderByAsc("indicator_level")
                .orderByAsc("order_num")
                .orderByAsc("id");
        List<EvalIndicator> all = baseMapper.selectList(wrapper);
        Map<Long, IndicatorTreeNode> index = new HashMap<>();
        for (EvalIndicator ind : all) {
            index.put(ind.getId(), IndicatorTreeNode.from(ind));
        }
        IndicatorTreeNode rootNode = index.get(rootId);
        if (rootNode == null) {
            return null;
        }
        for (EvalIndicator ind : all) {
            if (Objects.equals(ind.getId(), rootId)) {
                continue;
            }
            IndicatorTreeNode self = index.get(ind.getId());
            IndicatorTreeNode parent = ind.getParentId() == null ? null : index.get(ind.getParentId());
            if (parent != null) {
                parent.getChildren().add(self);
            }
        }
        return rootNode;
    }

    @Override
    public EvalIndicator instantiateFromTemplate(Long templateId, Long parentId, String operator) {
        if (templateId == null) {
            throw new ServiceException("模板ID不能为空");
        }
        EvalIndicator template = baseMapper.selectById(templateId);
        if (template == null || !Integer.valueOf(1).equals(template.getIsTemplate())) {
            throw new ServiceException("指标模板不存在或已删除");
        }
        EvalIndicator copy = new EvalIndicator()
                .setIndicatorName(uniqueIndicatorName(template.getIndicatorName(), false))
                .setIndicatorType(template.getIndicatorType())
                .setUnit(template.getUnit())
                .setValueMin(template.getValueMin())
                .setValueMax(template.getValueMax())
                .setValueCategory(template.getValueCategory())
                .setCalcMethod(template.getCalcMethod())
                .setAlgorithmId(template.getAlgorithmId())
                .setOrderNum(template.getOrderNum() == null ? 0 : template.getOrderNum())
                .setDescription(template.getDescription())
                .setIsBottomNode(template.getIsBottomNode())
                .setIsTemplate(0)
                .setIsApplied(0)
                .setParentId(parentId == null ? 0L : parentId);
        copy.setCreateBy(operator);
        copy.setCreateTime(new Date());
        insertIndicator(copy);
        return baseMapper.selectById(copy.getId());
    }

    // ==================== 内部工具 ====================

    private QueryWrapper<EvalIndicator> buildQueryWrapper(EvalIndicator query) {
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        String indicatorType = query.getIndicatorType();
        if (StringUtils.isNotEmpty(indicatorType)) {
            indicatorType = legacyResolveIndicatorTypeFromEquipment(indicatorType);
        }
        wrapper.like(StringUtils.isNotEmpty(query.getIndicatorName()), "indicator_name", query.getIndicatorName());
        wrapper.eq(StringUtils.isNotEmpty(indicatorType), "indicator_type", indicatorType);
        wrapper.eq(query.getParentId() != null, "parent_id", query.getParentId());
        wrapper.eq(query.getAlgorithmId() != null, "algorithm_id", query.getAlgorithmId());
        wrapper.eq(query.getIsBottomNode() != null, "is_bottom_node", query.getIsBottomNode());
        wrapper.eq(query.getIsApplied() != null, "is_applied", query.getIsApplied());
        wrapper.eq(query.getIsTemplate() != null, "is_template", query.getIsTemplate());
        return wrapper;
    }

    private EvalIndicator buildMerged(EvalIndicator existing, EvalIndicator incoming) {
        EvalIndicator merged = new EvalIndicator()
                .setId(existing.getId())
                .setIndicatorName(coalesce(incoming.getIndicatorName(), existing.getIndicatorName()))
                .setIndicatorType(coalesce(incoming.getIndicatorType(), existing.getIndicatorType()))
                .setParentId(incoming.getParentId() != null ? incoming.getParentId() : existing.getParentId())
                .setIsBottomNode(incoming.getIsBottomNode() != null ? incoming.getIsBottomNode() : existing.getIsBottomNode())
                .setUnit(coalesce(incoming.getUnit(), existing.getUnit()))
                .setValueCategory(coalesce(incoming.getValueCategory(), existing.getValueCategory()))
                .setCalcMethod(coalesce(incoming.getCalcMethod(), existing.getCalcMethod()))
                .setAlgorithmId(incoming.getAlgorithmId() != null ? incoming.getAlgorithmId() : existing.getAlgorithmId())
                .setValueMin(incoming.getValueMin() != null ? incoming.getValueMin() : existing.getValueMin())
                .setValueMax(incoming.getValueMax() != null ? incoming.getValueMax() : existing.getValueMax())
                .setOrderNum(incoming.getOrderNum() != null ? incoming.getOrderNum() : existing.getOrderNum())
                .setDescription(coalesce(incoming.getDescription(), existing.getDescription()))
                .setIsApplied(incoming.getIsApplied() != null ? incoming.getIsApplied() : existing.getIsApplied())
                .setIsTemplate(incoming.getIsTemplate() != null ? incoming.getIsTemplate() : existing.getIsTemplate())
                .setLevel(existing.getLevel())
                .setIdCode(StringUtils.isNotEmpty(incoming.getIdCode()) ? incoming.getIdCode().trim() : existing.getIdCode());
        return merged;
    }

    private static String coalesce(String a, String b) {
        return StringUtils.isNotEmpty(a) ? a : b;
    }

    private void ensureIndicatorIdCode(EvalIndicator indicator) {
        if (indicator == null) return;
        if (StringUtils.isEmpty(indicator.getIdCode())) {
            indicator.setIdCode("ind_" + UUID.randomUUID().toString().replace("-", ""));
        } else {
            indicator.setIdCode(indicator.getIdCode().trim());
        }
    }

    private void normalizeAndValidate(EvalIndicator indicator, Long currentId) {
        if (indicator.getParentId() == null) {
            indicator.setParentId(0L);
        }
        if (indicator.getIsTemplate() == null) {
            indicator.setIsTemplate(0);
        }
        if (indicator.getIsApplied() == null) {
            indicator.setIsApplied(0);
        }
        validateIndicatorNameUnique(indicator, currentId);
        validateIndicatorIdCodeUnique(indicator, currentId);
        if (StringUtils.isEmpty(indicator.getIndicatorType())) {
            throw new ServiceException("装备类型不能为空");
        }
        indicator.setIndicatorType(legacyResolveIndicatorTypeFromEquipment(indicator.getIndicatorType()));
        validateIndicatorType(indicator.getIndicatorType());
        indicator.setValueCategory(normalizeValueCategory(indicator.getValueCategory()));
        if (indicator.getIsBottomNode() == null) {
            indicator.setIsBottomNode(1);
        }

        validateParent(indicator, currentId);
        validateChildrenTypeConsistency(currentId, indicator.getIndicatorType());
        if (isBottom(indicator.getIsBottomNode()) && currentId != null && existsChildren(currentId)) {
            throw new ServiceException("当前指标存在子指标，不能设置为底层指标");
        }

        if (!isBottom(indicator.getIsBottomNode())) {
            indicator.setCalcMethod(null);
            indicator.setAlgorithmId(null);
            indicator.setValueMin(null);
            indicator.setValueMax(null);
            indicator.setValueCategory(null);
        }
        indicator.setLevel(calcLevel(indicator.getParentId()));
    }

    private void validateIndicatorNameUnique(EvalIndicator indicator, Long currentId) {
        // 完全跳过所有重名校验
        return;
    }

    private String uniqueIndicatorName(String base, boolean isTemplate) {
        String name = base == null ? "" : base.trim();
        if (StringUtils.isEmpty(name)) {
            return name;
        }
        int flag = isTemplate ? 1 : 0;
        String candidate = name;
        int suffix = 1;
        while (true) {
            QueryWrapper<EvalIndicator> w = new QueryWrapper<>();
            w.eq("indicator_name", candidate).eq("is_template", flag);
            if (baseMapper.selectCount(w) == 0) {
                return candidate;
            }
            suffix++;
            candidate = name + " (" + suffix + ")";
            if (suffix > 999) {
                return candidate;
            }
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
            throw new ServiceException("指标ID编码已存在");
        }
    }

    private String normalizeValueCategory(String raw) {
        if (StringUtils.isEmpty(raw)) return raw;
        String v = raw.trim();
        if ("COST".equals(v) || "成本型".equals(v)) return "成本型";
        if ("BENEFIT".equals(v) || "效益型".equals(v)) return "效益型";
        if ("INTERVAL_BENEFIT".equals(v) || "区间效益型".equals(v)) return "区间效益型";
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
        if (!Objects.equals(parent.getIsTemplate(), indicator.getIsTemplate())) {
            throw new ServiceException("父级与当前指标必须同属指标或模板");
        }
        if (resolveBottomNode(parent)) {
            throw new ServiceException("父级指标为底层指标，不能挂载子指标");
        }
        if (!"无".equals(parent.getIndicatorType())
                && StringUtils.isNotEmpty(parent.getIndicatorType())
                && StringUtils.isNotEmpty(indicator.getIndicatorType())
                && !Objects.equals(parent.getIndicatorType(), indicator.getIndicatorType())) {
            throw new ServiceException("父子节点装备类型不匹配（非「无」节点不能跨类型关联）");
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
            return isBottom(indicator.getIsBottomNode());
        }
        return !existsChildren(indicator.getId());
    }

    /** 底层节点标志归一：smallint 1=底层，其余（含 null/0）视为非底层。 */
    private static boolean isBottom(Integer flag) {
        return flag != null && flag == 1;
    }

    private boolean existsChildren(Long indicatorId) {
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", indicatorId);
        return baseMapper.selectCount(wrapper) > 0;
    }

    private void validateChildrenTypeConsistency(Long currentId, String indicatorType) {
        if (currentId == null || StringUtils.isEmpty(indicatorType)) return;
        QueryWrapper<EvalIndicator> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", currentId).ne("indicator_type", indicatorType);
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("当前节点存在不同类型的子节点，不能修改为该装备类型");
        }
    }

    private boolean createsCycle(Long currentId, Long parentId) {
        Set<Long> visited = new HashSet<>();
        Long cursor = parentId;
        while (cursor != null && cursor != 0L) {
            if (Objects.equals(cursor, currentId)) return true;
            if (!visited.add(cursor)) return true;
            EvalIndicator node = baseMapper.selectById(cursor);
            if (node == null) break;
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
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(node.getId());
        }
        Set<Long> descendants = new HashSet<>();
        List<Long> queue = new ArrayList<>();
        queue.add(rootId);
        for (int i = 0; i < queue.size(); i++) {
            List<Long> children = childrenMap.get(queue.get(i));
            if (children == null) continue;
            for (Long childId : children) {
                if (descendants.add(childId)) queue.add(childId);
            }
        }
        return descendants;
    }

    /** 装备类型兼容：标准码原样返回；历史别名收敛 */
    private String legacyResolveIndicatorTypeFromEquipment(String eq) {
        return ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTypeCode(eq);
    }

    private void validateIndicatorType(String indicatorType) {
        String normalized = legacyResolveIndicatorTypeFromEquipment(indicatorType);
        if (!EQUIPMENT_TYPE_VALUES.contains(normalized)) {
            throw new ServiceException("指标类型不在 ZHPG 装备类型允许范围内");
        }
    }
}
