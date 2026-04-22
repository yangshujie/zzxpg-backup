package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import com.ruoyi.domain.zhpg.CalcTask;
import com.ruoyi.domain.zhpg.EvalResult;
import com.ruoyi.mapper.zhpg.CalcFlowTemplateMapper;
import com.ruoyi.mapper.zhpg.CalcTaskMapper;
import com.ruoyi.mapper.zhpg.EvalResultMapper;
import com.ruoyi.service.zhpg.IEvalResultService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评估结果Service实现
 */
@Service
public class EvalResultServiceImpl extends ServiceImpl<EvalResultMapper, EvalResult>
        implements IEvalResultService {

    @Autowired
    private CalcTaskMapper calcTaskMapper;

    @Autowired
    private CalcFlowTemplateMapper calcFlowTemplateMapper;

    @Override
    public Page<EvalResult> selectEvalResultPage(Page page, EvalResult query) {
        QueryWrapper<EvalResult> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        Page<EvalResult> result = baseMapper.selectPage(page, wrapper);
        result.getRecords().forEach(this::parseDimensions);
        result.getRecords().forEach(this::fillTaskInfo);
        return result;
    }

    @Override
    public List<EvalResult> selectEvalResultList(EvalResult query) {
        QueryWrapper<EvalResult> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("create_time");
        List<EvalResult> list = baseMapper.selectList(wrapper);
        list.forEach(this::parseDimensions);
        list.forEach(this::fillTaskInfo);
        return list;
    }

    @Override
    public Map<String, Object> selectEvalResultStats() {
        Map<String, Object> stats = new HashMap<>();

        QueryWrapper<EvalResult> allWrapper = new QueryWrapper<>();
        allWrapper.eq("del_flag", 0);
        long total = baseMapper.selectCount(allWrapper);
        stats.put("total", total);

        QueryWrapper<EvalResult> publishedWrapper = new QueryWrapper<>();
        publishedWrapper.eq("del_flag", 0).eq("workflow_status", "PUBLISHED");
        long published = baseMapper.selectCount(publishedWrapper);
        stats.put("published", published);

        QueryWrapper<EvalResult> gradeAWrapper = new QueryWrapper<>();
        gradeAWrapper.eq("del_flag", 0).eq("grade", "A");
        long gradeA = baseMapper.selectCount(gradeAWrapper);
        stats.put("gradeA", gradeA);

        QueryWrapper<EvalResult> pendingWrapper = new QueryWrapper<>();
        pendingWrapper.eq("del_flag", 0).ne("workflow_status", "ARCHIVED");
        long pendingRectify = baseMapper.selectCount(pendingWrapper);
        stats.put("pendingRectify", pendingRectify);

        List<EvalResult> allResults = baseMapper.selectList(allWrapper);
        Map<String, Long> gradeDist = allResults.stream()
                .filter(r -> r.getGrade() != null)
                .collect(Collectors.groupingBy(EvalResult::getGrade, Collectors.counting()));
        stats.put("gradeDistribution", gradeDist);

        return stats;
    }

    @Override
    public int insertEvalResult(EvalResult evalResult) {
        if (StringUtils.isEmpty(evalResult.getResultCode())) {
            evalResult.setResultCode(generateResultCode());
        }
        if (evalResult.getDelFlag() == null) {
            evalResult.setDelFlag(0);
        }
        parseDimensionsToJson(evalResult);
        return baseMapper.insert(evalResult);
    }

    @Override
    public int updateEvalResult(EvalResult evalResult) {
        if (evalResult.getId() == null) {
            throw new ServiceException("结果ID不能为空");
        }
        EvalResult existing = baseMapper.selectById(evalResult.getId());
        if (existing == null || existing.getDelFlag() == 1) {
            throw new ServiceException("结果不存在或已删除");
        }
        parseDimensionsToJson(evalResult);
        evalResult.setUpdateTime(new Date());
        return baseMapper.updateById(evalResult);
    }

    @Override
    public int deleteEvalResultByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public int publishEvalResult(Long id) {
        EvalResult result = baseMapper.selectById(id);
        if (result == null || result.getDelFlag() == 1) {
            throw new ServiceException("结果不存在或已删除");
        }
        EvalResult update = new EvalResult();
        update.setId(id);
        update.setWorkflowStatus("PUBLISHED");
        update.setPublishTime(new Date());
        return baseMapper.updateById(update);
    }

    @Override
    public EvalResult getDetail(Long id) {
        EvalResult result = baseMapper.selectById(id);
        if (result == null) {
            return null;
        }
        parseDimensions(result);
        fillTaskInfo(result);
        return result;
    }

    @Override
    public EvalResult getDetailByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        QueryWrapper<EvalResult> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId).eq("del_flag", 0).orderByDesc("create_time").last("LIMIT 1");
        EvalResult result = baseMapper.selectOne(wrapper);
        if (result == null) {
            return null;
        }
        parseDimensions(result);
        fillTaskInfo(result);
        return result;
    }

    /**
     * 填充关联计算任务信息
     */
    private void fillTaskInfo(EvalResult result) {
        if (StringUtils.isBlank(result.getTemplateName()) && result.getTemplateId() != null) {
            try {
                CalcFlowTemplate template = calcFlowTemplateMapper.selectById(result.getTemplateId());
                if (template != null) {
                    result.setTemplateName(template.getTemplateName());
                }
            } catch (Exception e) {
                // ignore template fallback errors
            }
        }
        if (result.getTaskId() == null) {
            return;
        }
        try {
            CalcTask task = calcTaskMapper.selectById(result.getTaskId());
            if (task != null) {
                result.setTaskRunStatus(task.getRunStatus());
                result.setTaskProgressPercent(task.getProgressPercent());
                result.setTaskCurrentStage(task.getCurrentStage());
                // 从任务快照中提取评分等级配置，供前端动态渲染评分标准
                if (task.getTemplateSnapshotJson() != null) {
                    try {
                        com.alibaba.fastjson2.JSONObject snapshot = JSON.parseObject(task.getTemplateSnapshotJson());
                        com.alibaba.fastjson2.JSONObject stages = snapshot.getJSONObject("stages");
                        if (stages != null) {
                            com.alibaba.fastjson2.JSONObject compCalc = stages.getJSONObject("comprehensiveCalc");
                            if (compCalc != null) {
                                com.alibaba.fastjson2.JSONObject config = compCalc.getJSONObject("config");
                                if (config != null) {
                                    com.alibaba.fastjson2.JSONArray levels = config.getJSONArray("scoreLevels");
                                    if (levels != null && !levels.isEmpty()) {
                                        List<Map<String, Object>> scoreLevels = JSON.parseObject(
                                                levels.toJSONString(),
                                                new TypeReference<List<Map<String, Object>>>() {}
                                        );
                                        result.setScoreLevels(scoreLevels);
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // 快照解析失败不影响主流程
                    }
                }
            }
        } catch (Exception e) {
            // 忽略查询异常，不影响主数据
        }
    }

    private QueryWrapper<EvalResult> buildQueryWrapper(EvalResult query) {
        QueryWrapper<EvalResult> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0);

        if (StringUtils.isNotEmpty(query.getTaskName())) {
            wrapper.like("task_name", query.getTaskName());
        }
        if (StringUtils.isNotEmpty(query.getGrade())) {
            wrapper.eq("grade", query.getGrade());
        }
        if (StringUtils.isNotEmpty(query.getResultCode())) {
            wrapper.like("result_code", query.getResultCode());
        }
        if (StringUtils.isNotEmpty(query.getWorkflowStatus())) {
            wrapper.eq("workflow_status", query.getWorkflowStatus());
        }
        if (query.getTaskId() != null) {
            wrapper.eq("task_id", query.getTaskId());
        }
        if (query.getTemplateId() != null) {
            wrapper.eq("template_id", query.getTemplateId());
        }
        if (query.getIndicatorSystemId() != null) {
            wrapper.eq("indicator_system_id", query.getIndicatorSystemId());
        }
        if (query.getTemplateName() != null) {
            wrapper.like("template_name", query.getTemplateName());
        }

        return wrapper;
    }

    private String generateResultCode() {
        return "RES-" + System.currentTimeMillis();
    }

    private void parseDimensions(EvalResult result) {
        if (result.getDimensions() != null && !result.getDimensions().isEmpty()) {
            try {
                List<EvalResult.DimensionScore> dims = JSON.parseArray(
                        result.getDimensions(),
                        EvalResult.DimensionScore.class
                );
                result.setDimensionList(dims);
            } catch (Exception e) {
                // ignore parse errors
            }
        }
    }

    private void parseDimensionsToJson(EvalResult result) {
        if (result.getDimensionList() != null && !result.getDimensionList().isEmpty()) {
            result.setDimensions(JSON.toJSONString(result.getDimensionList()));
        }
    }
}
