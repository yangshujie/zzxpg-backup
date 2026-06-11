package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import com.ruoyi.domain.zhpg.dto.WeightApplyResult;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper.ParsedTree;
import com.ruoyi.mapper.zhpg.EvalIndicatorSystemMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.zhpg.util.ZhpgIndicatorLibrarySyncHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorSystemTreeHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.ruoyi.zhpg.util.ZhpgRequirementRefinedPayloadHelper;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import com.ruoyi.service.zhpg.IObjectiveWeightService;
import com.ruoyi.service.zhpg.ISubjectiveWeightService;
import com.ruoyi.service.zhpg.IIndicatorTreeWeightService;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.mapper.zhpg.EvalCriterionSetMapper;
import com.ruoyi.mapper.zhpg.EvalTaskTemplateMapper;
import com.ruoyi.mapper.zhpg.CalcFlowTemplateMapper;
import com.ruoyi.domain.zhpg.EvalCriterionSet;
import com.ruoyi.domain.zhpg.EvalTaskTemplate;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    @Autowired
    @org.springframework.context.annotation.Lazy
    private IObjectiveWeightService objectiveWeightService;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private ISubjectiveWeightService subjectiveWeightService;

    @Autowired
    private IIndicatorTreeWeightService indicatorTreeWeightService;

    @Autowired
    private IAlgorithmInfoService algorithmInfoService;

    @Autowired
    private EvalCriterionSetMapper evalCriterionSetMapper;

    @Autowired
    private EvalTaskTemplateMapper evalTaskTemplateMapper;

    @Autowired
    private CalcFlowTemplateMapper calcFlowTemplateMapper;

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
        if (StringUtils.isNotEmpty(system.getIndicatorTree()) && StringUtils.isEmpty(system.getIndicatorTreeWeight())) {
            clearIndicatorTreeWeight(system.getId());
        }
        submitIndicatorSystemLineage(system);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteSystemByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        for (Long id : ids) {
            EvalIndicatorSystem system = baseMapper.selectById(id);
            if (system == null) {
                continue;
            }
            // 1. 校验评估准则集关联
            QueryWrapper<EvalCriterionSet> criterionQuery = new QueryWrapper<>();
            criterionQuery.eq("indicator_system_id", id);
            List<EvalCriterionSet> criterionSets = evalCriterionSetMapper.selectList(criterionQuery);
            if (criterionSets != null && !criterionSets.isEmpty()) {
                String names = criterionSets.stream().map(EvalCriterionSet::getSetName).collect(Collectors.joining("，"));
                throw new ServiceException(String.format("指标体系「%s」无法删除：已被评估准则集【%s】引用，请先在评估准则管理中删除对应准则集。", system.getSystemName(), names));
            }

            // 2. 校验评估任务模板关联
            QueryWrapper<EvalTaskTemplate> taskQuery = new QueryWrapper<>();
            taskQuery.eq("indicator_system_id", id);
            List<EvalTaskTemplate> taskTemplates = evalTaskTemplateMapper.selectList(taskQuery);
            if (taskTemplates != null && !taskTemplates.isEmpty()) {
                String names = taskTemplates.stream().map(EvalTaskTemplate::getTemplateName).collect(Collectors.joining("，"));
                throw new ServiceException(String.format("指标体系「%s」无法删除：已被评估任务模板【%s】引用，请先解除引用关系。", system.getSystemName(), names));
            }

            // 3. 校验计算流程模板关联
            QueryWrapper<CalcFlowTemplate> flowQuery = new QueryWrapper<>();
            flowQuery.eq("indicator_system_id", id);
            List<CalcFlowTemplate> flowTemplates = calcFlowTemplateMapper.selectList(flowQuery);
            if (flowTemplates != null && !flowTemplates.isEmpty()) {
                String names = flowTemplates.stream().map(CalcFlowTemplate::getTemplateName).collect(Collectors.joining("，"));
                throw new ServiceException(String.format("指标体系「%s」无法删除：已被计算流程模板【%s】引用，请先解除引用关系。", system.getSystemName(), names));
            }
        }
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public Map<Long, List<String>> checkSystemReferences(Long[] ids) {
        Map<Long, List<String>> result = new HashMap<>();
        if (ids == null || ids.length == 0) {
            return result;
        }
        for (Long id : ids) {
            List<String> refs = new ArrayList<>();
            
            // 1. 评估准则集
            QueryWrapper<EvalCriterionSet> criterionQuery = new QueryWrapper<>();
            criterionQuery.eq("indicator_system_id", id);
            List<EvalCriterionSet> criterionSets = evalCriterionSetMapper.selectList(criterionQuery);
            if (criterionSets != null && !criterionSets.isEmpty()) {
                refs.add("评估准则集：" + criterionSets.stream().map(EvalCriterionSet::getSetName).collect(Collectors.joining("，")));
            }

            // 2. 评估任务模板
            QueryWrapper<EvalTaskTemplate> taskQuery = new QueryWrapper<>();
            taskQuery.eq("indicator_system_id", id);
            List<EvalTaskTemplate> taskTemplates = evalTaskTemplateMapper.selectList(taskQuery);
            if (taskTemplates != null && !taskTemplates.isEmpty()) {
                refs.add("评估任务模板：" + taskTemplates.stream().map(EvalTaskTemplate::getTemplateName).collect(Collectors.joining("，")));
            }

            // 3. 计算流程模板
            QueryWrapper<CalcFlowTemplate> flowQuery = new QueryWrapper<>();
            flowQuery.eq("indicator_system_id", id);
            List<CalcFlowTemplate> flowTemplates = calcFlowTemplateMapper.selectList(flowQuery);
            if (flowTemplates != null && !flowTemplates.isEmpty()) {
                refs.add("计算流程模板：" + flowTemplates.stream().map(CalcFlowTemplate::getTemplateName).collect(Collectors.joining("，")));
            }

            if (!refs.isEmpty()) {
                result.put(id, refs);
            }
        }
        return result;
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
        system.setIndicatorTree(indicatorTreeJson);
        system.setIndicatorTreeWeight(null);

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

    private void clearIndicatorTreeWeight(Long targetId) {
        if (targetId == null) {
            return;
        }
        UpdateWrapper<EvalIndicatorSystem> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", targetId).set("indicator_tree_weight", null);
        baseMapper.update(null, wrapper);
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

        // 同步权重快照树（结构变化后通常为空，仅权重计算/调优后存在）
        if (StringUtils.isNotEmpty(system.getIndicatorTreeWeight())) {
            system.setIndicatorTreeWeight(librarySyncHelper.syncTreeToLibrary(
                    system.getIndicatorTreeWeight(), system.getIsTemplate(), systemId, systemName, indicatorService, operator));
        }
    }

    private void submitIndicatorSystemLineage(EvalIndicatorSystem system) {
        try {
            Object tree = null;
            // 有权重快照时优先使用；否则使用当前结构树。
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object computeWeightsSmart(Long systemId, JSONObject options, String operator) {
        if (systemId == null || systemId <= 0) {
            throw new ServiceException("指标体系ID无效");
        }
        EvalIndicatorSystem system = getById(systemId);
        if (system == null) {
            throw new ServiceException("指标体系不存在");
        }
        String sourceJson = ZhpgIndicatorSystemTreeHelper.jsonForWeightCalculation(system);
        if (StringUtils.isEmpty(sourceJson)) {
            throw new ServiceException("指标树为空，无法计算权重");
        }

        ParsedTree meta = ZhpgIndicatorTreeJsonHelper.parseForWeight(sourceJson);
        JSONArray roots = JSON.parseArray(meta.getRootsForWeight().toJSONString());

        SmartWalkStat stat = new SmartWalkStat();
        // 客观赋权所需的参数
        int sampleRows = options != null ? options.getIntValue("mockSampleRows", 8) : 8;
        Long seed = options != null && options.containsKey("mockSeed") ? options.getLong("mockSeed") : null;
        java.util.Random rnd = seed != null ? new java.util.Random(seed) : new java.util.Random();

        walkSmart(roots, stat, sampleRows, rnd);

        String merged = ZhpgIndicatorTreeJsonHelper.serializeAfterWeight(meta, roots);
        WeightApplyResult renorm = indicatorTreeWeightService.applyWeights(merged, "RENORMALIZE");
        String finalTree = renorm.getIndicatorTree();

        StringBuilder hint = new StringBuilder();
        hint.append("智能赋权完成：共处理 ").append(stat.totalParents).append(" 个节点。");
        if (stat.objectiveCalls > 0) {
            hint.append(" 其中客观算法调用 ").append(stat.objectiveCalls).append(" 次。");
        }
        if (stat.subjectiveCount > 0) {
            hint.append(" 主观赋权节点 ").append(stat.subjectiveCount).append(" 个。");
        }
        if (stat.ahpFailCount > 0) {
            hint.append(" 警告：").append(stat.ahpFailCount).append(" 个 AHP 节点一致性校验未通过。");
        }
        if (StringUtils.isNotEmpty(renorm.getHint())) {
            hint.append(" ").append(renorm.getHint());
        }

        boolean persist = true;
        if (options != null && options.containsKey("persist")) {
            Boolean p = options.getBoolean("persist");
            persist = p == null || p;
        }

        if (persist) {
            system.setIndicatorTreeWeight(finalTree);
            system.setUpdateTime(new java.util.Date());
            if (StringUtils.isNotEmpty(operator)) {
                system.setUpdateBy(operator);
            }
            updateById(system);
        }

        JSONObject result = new JSONObject();
        result.put("indicatorTreeWeight", finalTree);
        result.put("hint", hint.toString());
        result.put("objectiveCalls", stat.objectiveCalls);
        result.put("subjectiveCount", stat.subjectiveCount);
        result.put("ahpFailCount", stat.ahpFailCount);
        return result;
    }

    private static class SmartWalkStat {
        int totalParents = 0;
        int objectiveCalls = 0;
        int subjectiveCount = 0;
        int ahpFailCount = 0;
    }

    private void walkSmart(JSONArray nodes, SmartWalkStat stat, int sampleRows, java.util.Random rnd) {
        if (nodes == null || nodes.isEmpty()) return;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch == null || ch.isEmpty()) continue;

            stat.totalParents++;
            if (ch.size() == 1) {
                ch.getJSONObject(0).put("weight", 1.0);
            } else {
                Object algRef = n.get("weightAssignAlgorithm");
                boolean isObjective = false;
                if (algRef instanceof Number) {
                    isObjective = true;
                } else if (algRef instanceof String) {
                    String s = ((String) algRef).trim();
                    if (s.matches("\\d+")) isObjective = true;
                }

                if (isObjective) {
                    // 调用客观赋权单节点逻辑（通过反射或将 ObjectiveWeightServiceImpl 的核心逻辑抽离，
                    // 这里为了简单，我们直接模拟 ObjectiveWeightServiceImpl 的 walkAssignWeights 核心逻辑）
                    stat.objectiveCalls++;
                    // 由于目前 ObjectiveWeightServiceImpl 逻辑较为闭塞，我们通过 options 参数
                    // 调用其 computeForSystem 可能导致整棵树重算。
                    // 理想做法是重构 ObjectiveWeightServiceImpl，暴露单节点计算方法。
                    // 暂时这里手动处理客观单节点分发：
                    try {
                        dispatchObjectiveNode(n, ch, sampleRows, rnd);
                    } catch (Exception e) {
                        log.error("节点[{}]客观赋权失败: {}", n.getString("label"), e.getMessage());
                    }
                } else {
                    // 调用主观赋权单节点逻辑
                    stat.subjectiveCount++;
                    try {
                        dispatchSubjectiveNode(n, ch, stat);
                    } catch (Exception e) {
                        log.error("节点[{}]主观赋权失败: {}", n.getString("label"), e.getMessage());
                    }
                }
            }
            walkSmart(ch, stat, sampleRows, rnd);
        }
    }

    private void dispatchObjectiveNode(JSONObject parent, JSONArray children, int sampleRows, java.util.Random rnd) {
        objectiveWeightService.computeSingleNode(parent, children, sampleRows, rnd);
    }

    private void dispatchSubjectiveNode(JSONObject parent, JSONArray children, SmartWalkStat stat) {
        JSONObject s = new JSONObject();
        s.put("ahpFailCount", 0);
        subjectiveWeightService.computeSingleNode(parent, children, s);
        stat.ahpFailCount += s.getIntValue("ahpFailCount", 0);
    }
}
