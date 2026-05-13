package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import com.ruoyi.mapper.zhpg.EvalIndicatorSystemMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.zhpg.util.ZhpgIndicatorLibrarySyncHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorSystemTreeHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.ruoyi.zhpg.util.ZhpgRequirementRefinedPayloadHelper;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 评估指标体系Service实现
 */
@Slf4j
@Service
public class EvalIndicatorSystemServiceImpl
        extends ServiceImpl<EvalIndicatorSystemMapper, EvalIndicatorSystem>
        implements IEvalIndicatorSystemService {

    private static final String DEFAULT_WORK_MODE = "内部流转";

    @Autowired
    private IEvalIndicatorService indicatorService;

    @Autowired
    private ZhpgIndicatorLibrarySyncHelper librarySyncHelper;

    @Autowired
    private EvaluationResultLineageClient evaluationResultLineageClient;

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
    @Transactional(rollbackFor = Exception.class)
    public int insertSystem(EvalIndicatorSystem system) {
        if (system.getIsTemplate() == null) {
            system.setIsTemplate(0);
        }
        validateSystemNameUnique(system, null);
        if (StringUtils.isEmpty(system.getStatus())) {
            system.setStatus("DRAFT");
        }
        if (system.getIsApplied() == null) {
            system.setIsApplied(0);
        }
        normalizeWorkMode(system);
        
        // 1. 先插入以获取体系 ID
        int rows = baseMapper.insert(system);
        
        // 2. 同步指标树到库 (此时 system.getId() 已经有值，用于隔离重名冲突)
        syncTreeToIndicatorLibrary(system);
        
        // 3. 将同步后回填了 ID 的 JSON 树和 idCode 更新回体系记录
        syncIndicatorSystemIdCodeFromTree(system);
        validateSystemIdCodeUnique(system, system.getId());
        baseMapper.updateById(system);
        submitIndicatorSystemLineage(system);
        
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSystem(EvalIndicatorSystem system) {
        if (system.getId() == null) {
            throw new ServiceException("指标体系ID不能为空");
        }
        validateSystemNameUnique(system, system.getId());
        normalizeWorkMode(system);
        syncTreeToIndicatorLibrary(system);
        syncIndicatorSystemIdCodeFromTree(system);
        validateSystemIdCodeUnique(system, system.getId());
        int rows = baseMapper.updateById(system);
        submitIndicatorSystemLineage(system);
        return rows;
    }

    @Override
    public int deleteSystemByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public EvalIndicatorSystem createFromTemplate(Long templateId, String systemName, String operator) {
        if (templateId == null) {
            throw new ServiceException("模板ID不能为空");
        }
        EvalIndicatorSystem template = baseMapper.selectById(templateId);
        if (template == null || template.getIsTemplate() == null || template.getIsTemplate() != 1) {
            throw new ServiceException("指标体系模板不存在或已删除");
        }
        EvalIndicatorSystem system = new EvalIndicatorSystem();
        system.setSystemName(StringUtils.isNotEmpty(systemName) ? systemName : template.getSystemName());
        system.setIndicatorTree(template.getIndicatorTree());
        system.setWorkMode(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(
                template.getWorkMode(),
                ZhpgIndicatorTreeJsonHelper.extractWorkMode(template.getIndicatorTree(), DEFAULT_WORK_MODE)));
        system.setConductionConfig(template.getConductionConfig());
        system.setWeightAssignConfig(template.getWeightAssignConfig());
        system.setDescription(template.getDescription());
        system.setTemplateId(templateId);
        system.setIsTemplate(0);
        system.setStatus("DRAFT");
        system.setIsApplied(0);
        system.setCreateBy(operator);
        system.setCreateTime(new Date());

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
        // 需求变更：允许重复的 ID 编码以支持多次回传细化生成多条记录
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
        Integer scopeIsTemplate = system.getIsTemplate();
        if (scopeIsTemplate == null && excludeId != null) {
            EvalIndicatorSystem persisted = baseMapper.selectById(excludeId);
            if (persisted != null) {
                scopeIsTemplate = persisted.getIsTemplate();
            }
        }
        if (scopeIsTemplate == null) {
            scopeIsTemplate = 0;
        }
        QueryWrapper<EvalIndicatorSystem> wrapper = new QueryWrapper<>();
        wrapper.eq("system_name", name);
        wrapper.eq("is_template", scopeIsTemplate);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            String label = scopeIsTemplate != null && scopeIsTemplate == 1 ? "指标体系模板" : "指标体系";
            throw new ServiceException(label + "名称「" + name + "」已存在，请修改名称。");
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
            wrapper.eq(query.getIsTemplate() != null, "is_template", query.getIsTemplate());
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
        if (StringUtils.isNotEmpty(system.getIndicatorTreeWeight())) {
            system.setIndicatorTreeWeight(
                    ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTreeTypes(system.getIndicatorTreeWeight()));
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
    @Transactional(rollbackFor = Exception.class)
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

        // 需求变更：同一个需求 ID 只保留一条记录。尝试按 requirementId 匹配并更新（Upsert）。
        Long rid = payload.getLong("requirementId");
        if (rid == null || rid <= 0) {
            JSONObject nested = payload.getJSONObject("indicatorSystem");
            if (nested != null) {
                rid = nested.getLong("requirementId");
            }
        }

        if (rid != null && rid > 0) {
            EvalIndicatorSystem existing = getByRequirementId(rid);
            if (existing != null) {
                return mergeRequirementRefinedIntoExisting(existing.getId(), payload, indicatorTreeJson, operator);
            }
        }

        return createIndicatorSystemFromRequirementPayload(payload, treeRoot, indicatorTreeJson, operator);
    }

    private EvalIndicatorSystem mergeRequirementRefinedIntoExisting(
            Long targetId, JSONObject payload, String indicatorTreeJson, String operator) {
        EvalIndicatorSystem system = getById(targetId);
        if (system == null) {
            throw new ServiceException("指标体系不存在");
        }
        applyRequirementIdFromPayload(payload, system);
        system.setIndicatorTreeWeight(indicatorTreeJson);
        // 同步主树（如果主树为空或根据业务需要覆盖）
        if (StringUtils.isEmpty(system.getIndicatorTree())) {
            system.setIndicatorTree(indicatorTreeJson);
        }

        String block = ZhpgRequirementRefinedPayloadHelper.buildDescription(payload);
        if (StringUtils.isNotEmpty(block)) {
            if (StringUtils.isNotEmpty(system.getDescription())) {
                system.setDescription(system.getDescription() + "\n\n--- 需求侧回传更新 ---\n" + block);
            } else {
                system.setDescription(block);
            }
        }

        String src = payload.getString("sourceSubsystem");
        system.setSourceSubsystem(StringUtils.isNotEmpty(src) ? src.trim() : "需求分析分系统");

        String extractedMode = ZhpgIndicatorTreeJsonHelper.extractWorkMode(indicatorTreeJson, system.getWorkMode());
        system.setWorkMode(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(extractedMode, system.getWorkMode()));

        if ("主分协同".equals(system.getWorkMode())) {
            system.setBuildPhase("REFINED");
            system.setRefinedTime(new Date());
        }
        system.setUpdateBy(operator);
        system.setUpdateTime(new Date());
        updateSystem(system);
        return getById(targetId);
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
            system.setIndicatorTreeWeight(indicatorTreeJson);
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

    private void syncTreeToIndicatorLibrary(EvalIndicatorSystem system) {
        if (system == null) return;
        String operator = StringUtils.isNotEmpty(system.getUpdateBy()) ? system.getUpdateBy() : system.getCreateBy();
        if (StringUtils.isEmpty(operator)) operator = "admin";

        Long systemId = system.getId();
        // 如果 systemName 为空（可能由 updateSystem 触发且未传全量字段），尝试补齐
        if (systemId != null && StringUtils.isEmpty(system.getSystemName())) {
            EvalIndicatorSystem existing = baseMapper.selectById(systemId);
            if (existing != null) {
                system.setSystemName(existing.getSystemName());
            }
        }
        String systemName = system.getSystemName();

        // 同步主指标树
        if (StringUtils.isNotEmpty(system.getIndicatorTree())) {
            system.setIndicatorTree(librarySyncHelper.syncTreeToLibrary(
                    system.getIndicatorTree(), system.getIsTemplate(), systemId, systemName, indicatorService, operator));
        }

        // 同步结果/权重树（包含回传细化或权重结果）
        if (StringUtils.isNotEmpty(system.getIndicatorTreeWeight())) {
            system.setIndicatorTreeWeight(librarySyncHelper.syncTreeToLibrary(
                    system.getIndicatorTreeWeight(), system.getIsTemplate(), systemId, systemName, indicatorService, operator));
        }
    }

    private void submitIndicatorSystemLineage(EvalIndicatorSystem system) {
        try {
            Object tree = null;
            // 回传细化场景主要写入 indicatorTreeWeight，需优先使用该树写血缘。
            if (StringUtils.isNotEmpty(system.getIndicatorTreeWeight())) {
                tree = JSON.parse(system.getIndicatorTreeWeight());
            } else if (StringUtils.isNotEmpty(system.getIndicatorTree())) {
                tree = JSON.parse(system.getIndicatorTree());
            }
            evaluationResultLineageClient.submitIndicatorSystemLineage(tree, system.getRequirementId());
        } catch (Exception e) {
            log.warn("指标体系血缘写入入队失败: systemId={}, requirementId={}, error={}",
                    system != null ? system.getId() : null,
                    system != null ? system.getRequirementId() : null,
                    e.getMessage(), e);
        }
    }

}
