package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.EvalIndicatorSystemTemplate;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import com.ruoyi.mapper.zhpg.EvalIndicatorSystemMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemTemplateService;
import com.ruoyi.zhpg.util.ZhpgIndicatorSystemTreeHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.ruoyi.zhpg.util.ZhpgRequirementRefinedPayloadHelper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评估指标体系Service实现
 */
@Service
public class EvalIndicatorSystemServiceImpl
        extends ServiceImpl<EvalIndicatorSystemMapper, EvalIndicatorSystem>
        implements IEvalIndicatorSystemService {

    private static final String DEFAULT_WORK_MODE = "内部流转";

    @Autowired
    private IEvalIndicatorSystemTemplateService systemTemplateService;

    @Override
    public Page<EvalIndicatorSystem> selectSystemPage(Page page, EvalIndicatorSystem query) {
        QueryWrapper<EvalIndicatorSystem> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public List<EvalIndicatorSystem> selectSystemList(EvalIndicatorSystem query) {
        QueryWrapper<EvalIndicatorSystem> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public int insertSystem(EvalIndicatorSystem system) {
        validateSystemNameUnique(system, null);
        if (StringUtils.isEmpty(system.getStatus())) {
            system.setStatus("DRAFT");
        }
        if (system.getIsApplied() == null) {
            system.setIsApplied(0);
        }
        normalizeWorkMode(system);
        syncIndicatorSystemIdCodeFromTree(system);
        validateSystemIdCodeUnique(system, null);
        return baseMapper.insert(system);
    }

    @Override
    public int updateSystem(EvalIndicatorSystem system) {
        if (system.getId() == null) {
            throw new ServiceException("指标体系ID不能为空");
        }
        validateSystemNameUnique(system, system.getId());
        normalizeWorkMode(system);
        syncIndicatorSystemIdCodeFromTree(system);
        validateSystemIdCodeUnique(system, system.getId());
        return baseMapper.updateById(system);
    }

    @Override
    public int deleteSystemByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public EvalIndicatorSystem createFromTemplate(Long templateId, String systemName, String operator) {
        EvalIndicatorSystemTemplate template = systemTemplateService.getById(templateId);
        if (template == null) {
            throw new ServiceException("指标体系模板不存在或已删除");
        }
        EvalIndicatorSystem system = new EvalIndicatorSystem();
        system.setSystemName(StringUtils.isNotEmpty(systemName) ? systemName : template.getTemplateName());
        // 指标体系行级不再维护指标集类型 / 装备类型（由指标树 JSON 等承载；与前端「暂不设置」一致）
        system.setIndicatorTree(template.getIndicatorTree());
            system.setWorkMode(ZhpgIndicatorTreeJsonHelper.extractWorkMode(template.getIndicatorTree(), DEFAULT_WORK_MODE));
        system.setDescription(template.getDescription());
        system.setTemplateId(templateId);
        system.setStatus("DRAFT");
        system.setIsApplied(0);
        system.setCreateBy(operator);
        system.setCreateTime(new Date());

        // 从模板配置JSON中提取权重分配算法等配置
        if (StringUtils.isNotEmpty(template.getConfigJson())) {
            try {
                com.alibaba.fastjson2.JSONObject config = com.alibaba.fastjson2.JSON.parseObject(template.getConfigJson());
                if (config != null) {
                    String weightAlg = config.getString("weightAssignAlgorithm");
                    if (StringUtils.isNotEmpty(weightAlg)) {
                        system.setWeightAssignAlgorithm(weightAlg);
                    }
                    String weightParams = config.getString("weightAssignParams");
                    if (StringUtils.isNotEmpty(weightParams)) {
                        system.setWeightAssignParams(weightParams);
                    }
                    String conductionAlg = config.getString("conductionAlgorithm");
                    if (StringUtils.isNotEmpty(conductionAlg)) {
                        JSONObject one = new JSONObject();
                        one.put("name", conductionAlg);
                        system.setConductionConfig(one.toJSONString());
                    } else {
                        JSONObject globalConduction = config.getJSONObject("globalConduction");
                        if (globalConduction != null && !globalConduction.isEmpty()) {
                            system.setConductionConfig(globalConduction.toJSONString());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        insertSystem(system);
        return baseMapper.selectById(system.getId());
    }

    private void syncIndicatorSystemIdCodeFromTree(EvalIndicatorSystem system) {
        if (system == null) {
            return;
        }
        String fromTree = ZhpgIndicatorTreeJsonHelper.extractRootIdCode(
                ZhpgIndicatorSystemTreeHelper.jsonForRootIdCodeSync(system));
        if (StringUtils.isNotEmpty(fromTree)) {
            system.setIdCode(fromTree);
        }
    }

    private void validateSystemIdCodeUnique(EvalIndicatorSystem system, Long excludeId) {
        if (system == null || StringUtils.isEmpty(system.getIdCode())) {
            return;
        }
        QueryWrapper<EvalIndicatorSystem> wrapper = new QueryWrapper<>();
        wrapper.eq("id_code", system.getIdCode().trim());
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("指标体系ID编码已被占用，请调整指标树根节点 id 或删除冲突记录");
        }
    }

    private void validateSystemNameUnique(EvalIndicatorSystem system, Long excludeId) {
        if (system == null || StringUtils.isEmpty(system.getSystemName())) {
            return;
        }
        String name = system.getSystemName().trim();
        if (StringUtils.isEmpty(name)) {
            throw new ServiceException("指标体系名称不能为空");
        }
        system.setSystemName(name);
        QueryWrapper<EvalIndicatorSystem> wrapper = new QueryWrapper<>();
        wrapper.eq("system_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("指标体系名称已存在，请使用其他名称");
        }
    }

    private QueryWrapper<EvalIndicatorSystem> buildQueryWrapper(EvalIndicatorSystem query) {
        QueryWrapper<EvalIndicatorSystem> wrapper = new QueryWrapper<>();
        if (query != null) {
            String workMode = ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(query.getWorkMode(), query.getWorkMode());
            wrapper.like(StringUtils.isNotEmpty(query.getSystemName()), "system_name", query.getSystemName());
            wrapper.eq(StringUtils.isNotEmpty(workMode), "work_mode", workMode);
            wrapper.eq(StringUtils.isNotEmpty(query.getStatus()), "status", query.getStatus());
            wrapper.eq(query.getIsApplied() != null, "is_applied", query.getIsApplied());
            wrapper.eq(query.getRequirementId() != null, "requirement_id", query.getRequirementId());
            if (StringUtils.isNotEmpty(query.getBuildPhase())) {
                String bp = query.getBuildPhase().trim();
                if ("INITIAL_DRAFT".equals(bp)) {
                    wrapper.in("build_phase", Arrays.asList("INITIAL_DRAFT", "SUBMITTED"));
                } else if ("REFINED".equals(bp)) {
                    wrapper.in("build_phase", Arrays.asList("REFINED", "FINALIZED"));
                } else {
                    wrapper.eq("build_phase", bp);
                }
            }
        }
        return wrapper;
    }

    private void normalizeWorkMode(EvalIndicatorSystem system) {
        if (system == null) {
            return;
        }
        if (StringUtils.isNotEmpty(system.getIndicatorTree())) {
            system.setIndicatorTree(ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTreeTypes(system.getIndicatorTree()));
        }
        if (StringUtils.isNotEmpty(system.getRefinedIndicatorTree())) {
            system.setRefinedIndicatorTree(
                    ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTreeTypes(system.getRefinedIndicatorTree()));
        }
        String fallback = ZhpgIndicatorTreeJsonHelper.extractWorkMode(
                ZhpgIndicatorSystemTreeHelper.jsonForWorkModeExtraction(system), DEFAULT_WORK_MODE);
        system.setWorkMode(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(system.getWorkMode(), fallback));
        if (StringUtils.isEmpty(system.getWorkMode())) {
            system.setWorkMode(DEFAULT_WORK_MODE);
        }
        // 不在非主分协同时强制清空 build_phase：前端可能暂时切到「内部流转」再保存，应保留原细化阶段供切回主分协同时一致。
    }

    @Override
    public List<EvalIndicatorSystemSelectVO> selectIndicatorSystemListForSelect(String keyword, Long requirementId) {
        List<EvalIndicatorSystemSelectVO> list = baseMapper.selectIndicatorSystemListForSelect(keyword, requirementId);
        for (EvalIndicatorSystemSelectVO vo : list) {
            Object raw = vo.getTreeData();
            if (raw instanceof String && StringUtils.isNotEmpty((String) raw)) {
                try {
                    vo.setTreeData(unwrapTreeDataPayload(JSON.parse((String) raw)));
                } catch (Exception ignored) {
                    // 解析失败时保留原字符串
                }
            } else if (raw instanceof JSONObject) {
                vo.setTreeData(unwrapTreeDataPayload(raw));
            }
        }
        return list.stream()
                .filter(vo -> vo.getRequirementId() != null)
                .filter(vo -> requirementId == null || Objects.equals(requirementId, vo.getRequirementId()))
                .collect(Collectors.toList());
    }

    @Override
    public EvalIndicatorSystem getByRequirementId(Long requirementId) {
        if (requirementId == null) {
            return null;
        }
        QueryWrapper<EvalIndicatorSystem> wrapper = new QueryWrapper<>();
        wrapper.eq("requirement_id", requirementId)
                .orderByDesc("update_time", "create_time", "id")
                .last("limit 1");
        return baseMapper.selectOne(wrapper);
    }

    /**
     * 库中多为 {"treeData": 根节点 }；返回给前端时直接给出根节点（或顶层数组等），不再多包一层 indicatorTree。
     */
    private static Object unwrapTreeDataPayload(Object parsed) {
        if (parsed instanceof JSONObject) {
            JSONObject obj = (JSONObject) parsed;
            if (obj.containsKey("treeData")) {
                return obj.get("treeData");
            }
        }
        return parsed;
    }

    @Override
    public EvalIndicatorSystem receiveRefinedFromRequirementPayload(JSONObject payload, String operator) {
        if (payload == null || payload.isEmpty()) {
            throw new ServiceException("请求体不能为空");
        }
        Object treeRoot = ZhpgRequirementRefinedPayloadHelper.resolveTreeRoot(payload);
        if (treeRoot == null) {
            throw new ServiceException("报文中缺少细化指标树（请提供 treeData，或 indicatorTree / indicatorSystem.treeData）");
        }
        JSONObject wrapped = new JSONObject();
        wrapped.put("treeData", treeRoot);
        String indicatorTreeJson = ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTreeTypes(wrapped.toJSONString());

        Long targetId = payload.getLong("targetIndicatorSystemId");
        if (targetId == null || targetId <= 0) {
            targetId = payload.getLong("indicatorSystemId");
        }
        if (targetId != null && targetId > 0) {
            return mergeRequirementRefinedIntoExisting(targetId, payload, indicatorTreeJson, operator);
        }
        EvalIndicatorSystem byRoot = findIndicatorSystemByTreeRootId(
                ZhpgRequirementRefinedPayloadHelper.extractRootNodeIdForMatch(treeRoot));
        if (byRoot != null) {
            return mergeRequirementRefinedIntoExisting(byRoot.getId(), payload, indicatorTreeJson, operator);
        }
        return createIndicatorSystemFromRequirementPayload(payload, treeRoot, indicatorTreeJson, operator);
    }

    /**
     * 按指标树根节点 id（与库 id_code 一致）定位体系；若无匹配且根 id 为纯数字则再按主键 id 尝试。
     */
    private EvalIndicatorSystem findIndicatorSystemByTreeRootId(String rootNodeId) {
        if (StringUtils.isEmpty(rootNodeId)) {
            return null;
        }
        String trimmed = rootNodeId.trim();
        QueryWrapper<EvalIndicatorSystem> byCode = new QueryWrapper<>();
        byCode.eq("id_code", trimmed);
        List<EvalIndicatorSystem> byCodeList = baseMapper.selectList(byCode);
        if (byCodeList != null && !byCodeList.isEmpty()) {
            if (byCodeList.size() > 1) {
                throw new ServiceException(
                        "treeData 根节点 id 与多条指标体系的 id_code 相同，请指定 targetIndicatorSystemId 或 indicatorSystemId");
            }
            return byCodeList.get(0);
        }
        try {
            long pk = Long.parseLong(trimmed);
            if (pk > 0) {
                EvalIndicatorSystem s = getById(pk);
                if (s != null) {
                    return s;
                }
            }
        } catch (NumberFormatException ignored) {
            // 非数字则仅依赖 id_code
        }
        return null;
    }

    /**
     * 从报文顶层或嵌套 indicatorSystem 中解析 requirementId；有效时写入实体。
     */
    private static void applyRequirementIdFromPayload(JSONObject payload, EvalIndicatorSystem system) {
        if (payload == null || system == null) {
            return;
        }
        Long rid = payload.getLong("requirementId");
        if (rid == null || rid <= 0) {
            JSONObject nested = payload.getJSONObject("indicatorSystem");
            if (nested != null) {
                rid = nested.getLong("requirementId");
            }
        }
        if (rid != null && rid > 0) {
            system.setRequirementId(rid);
        }
    }

    private EvalIndicatorSystem mergeRequirementRefinedIntoExisting(
            Long targetId, JSONObject payload, String indicatorTreeJson, String operator) {
        EvalIndicatorSystem system = getById(targetId);
        if (system == null) {
            throw new ServiceException("指标体系不存在");
        }
        applyRequirementIdFromPayload(payload, system);
        system.setRefinedIndicatorTree(indicatorTreeJson);

        String block = ZhpgRequirementRefinedPayloadHelper.buildDescription(payload);
        if (StringUtils.isNotEmpty(block)) {
            if (StringUtils.isNotEmpty(system.getDescription())) {
                system.setDescription(system.getDescription() + "\n\n--- 需求侧回传 ---\n" + block);
            } else {
                system.setDescription(block);
            }
        }

        String src = payload.getString("sourceSubsystem");
        system.setSourceSubsystem(StringUtils.isNotEmpty(src) ? src.trim() : "需求分析分系统");
        if ("主分协同".equals(system.getWorkMode())) {
            system.setBuildPhase("REFINED");
            system.setRefinedTime(new Date());
        }
        system.setUpdateBy(operator);
        system.setUpdateTime(new Date());
        updateSystem(system);
        return getById(targetId);
    }

    private EvalIndicatorSystem createIndicatorSystemFromRequirementPayload(
            JSONObject payload, Object treeRoot, String indicatorTreeJson, String operator) {
        String rootLabel = ZhpgRequirementRefinedPayloadHelper.extractRootLabel(treeRoot);
        EvalIndicatorSystem system = new EvalIndicatorSystem();
        applyRequirementIdFromPayload(payload, system);
        system.setSystemName(ZhpgRequirementRefinedPayloadHelper.resolveSystemName(payload, rootLabel));
        system.setDescription(ZhpgRequirementRefinedPayloadHelper.buildDescription(payload));
        system.setIndicatorTree(indicatorTreeJson);
        String extractedMode = ZhpgIndicatorTreeJsonHelper.extractWorkMode(indicatorTreeJson, DEFAULT_WORK_MODE);
        String normalizedMode = ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(extractedMode, DEFAULT_WORK_MODE);
        if ("主分协同".equals(normalizedMode)) {
            system.setRefinedIndicatorTree(indicatorTreeJson);
        }
        String src = payload.getString("sourceSubsystem");
        system.setSourceSubsystem(StringUtils.isNotEmpty(src) ? src.trim() : "需求分析分系统");
        system.setStatus("DRAFT");
        system.setIsApplied(0);
        system.setCreateBy(operator);
        system.setCreateTime(new Date());
        if ("主分协同".equals(normalizedMode)) {
            system.setBuildPhase("REFINED");
            system.setRefinedTime(new Date());
        }
        insertSystem(system);
        return getById(system.getId());
    }

}
