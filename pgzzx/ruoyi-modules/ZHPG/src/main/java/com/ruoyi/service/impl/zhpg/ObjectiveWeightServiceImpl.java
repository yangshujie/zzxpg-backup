package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.AlgorithmInfo;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.ObjectiveWeightComputeResult;
import com.ruoyi.domain.zhpg.dto.WeightApplyResult;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.service.zhpg.IIndicatorTreeWeightService;
import com.ruoyi.service.zhpg.IObjectiveWeightService;
import com.ruoyi.zhpg.algs.ZhpgZgpgAlgsClient;
import com.ruoyi.zhpg.util.ZhpgIndicatorSystemTreeHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper.ParsedTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;

/**
 * 客观赋权：各父节点子层使用模拟样本矩阵调用 zgpg_algs（熵权 cal_weight / 层次分析 ahp），再层内归一。
 */
@Service
public class ObjectiveWeightServiceImpl implements IObjectiveWeightService {

    @Autowired
    private IEvalIndicatorSystemService indicatorSystemService;

    @Autowired
    private ZhpgZgpgAlgsClient zgpgAlgsClient;

    @Autowired
    private IIndicatorTreeWeightService indicatorTreeWeightService;

    @Autowired
    private IAlgorithmInfoService algorithmInfoService;

    @Override
    public ObjectiveWeightComputeResult computeForSystem(Long systemId, JSONObject options, String operator) {
        if (systemId == null || systemId <= 0) {
            throw new ServiceException("指标体系ID无效");
        }
        EvalIndicatorSystem system = indicatorSystemService.getById(systemId);
        if (system == null) {
            throw new ServiceException("指标体系不存在");
        }
        String sourceJson = ZhpgIndicatorSystemTreeHelper.jsonForWeightCalculation(system);
        if (StringUtils.isEmpty(sourceJson)) {
            throw new ServiceException("指标树为空，无法计算权重");
        }

        boolean persist = true;
        if (options != null && options.containsKey("persist")) {
            Boolean p = options.getBoolean("persist");
            persist = p == null || p;
        }
        int sampleRows = options != null ? options.getIntValue("mockSampleRows", 8) : 8;
        sampleRows = Math.max(2, Math.min(sampleRows, 64));
        Long seed = options != null && options.containsKey("mockSeed") ? options.getLong("mockSeed") : null;
        Random rnd = seed != null ? new Random(seed) : new Random();
        String overrideModule = resolveOverrideWeightModule(options);
        String missingWeightPolicy = options != null ? options.getString("missingWeightPolicy") : null;

        ParsedTree meta = ZhpgIndicatorTreeJsonHelper.parseForWeight(sourceJson);
        JSONArray roots = JSON.parseArray(meta.getRootsForWeight().toJSONString());
        int calls = walkAssignWeights(roots, system, sampleRows, rnd, overrideModule);

        String merged = ZhpgIndicatorTreeJsonHelper.serializeAfterWeight(meta, roots);
        String strategy = "RENORMALIZE";
        if ("EQUAL_DISTRIBUTE".equalsIgnoreCase(missingWeightPolicy)) {
            strategy = "EQUAL";
        }
        if ("TERMINATE".equalsIgnoreCase(missingWeightPolicy)) {
            ensureWeightPresent(roots);
        }
        WeightApplyResult renorm = indicatorTreeWeightService.applyWeights(merged, strategy);
        String finalTree = renorm.getIndicatorTree();

        StringBuilder hint = new StringBuilder();
        hint.append("已按层级调用算法服务赋权，共 ").append(calls).append(" 次。");
        if (StringUtils.isNotEmpty(renorm.getHint())) {
            hint.append(" ").append(renorm.getHint());
        }

        String mockNote = "mockSampleRows=" + sampleRows + (seed != null ? ", mockSeed=" + seed : ", mockSeed=random");

        if (persist) {
            system.setIndicatorTreeWeight(finalTree);
            system.setUpdateTime(new Date());
            if (StringUtils.isNotEmpty(operator)) {
                system.setUpdateBy(operator);
            }
            indicatorSystemService.updateById(system);
        }

        return new ObjectiveWeightComputeResult(finalTree, hint.toString(), mockNote, calls);
    }

    private int walkAssignWeights(JSONArray nodes, EvalIndicatorSystem system, int sampleRows, Random rnd,
                                  String overrideModule) {
        int calls = 0;
        if (nodes == null || nodes.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch == null || ch.isEmpty()) {
                continue;
            }
            if (ch.size() == 1) {
                ch.getJSONObject(0).put("weight", round8(1.0));
                JSONArray one = new JSONArray();
                one.add(ch.getJSONObject(0));
                calls += walkAssignWeights(one, system, sampleRows, rnd, overrideModule);
                continue;
            }
            Object nodeAlgObj = n.get("weightAssignAlgorithm");
            String nodeAlg = null;
            String nodeParams = "{}";
            if (nodeAlgObj instanceof Number) {
                AlgorithmInfo alg = algorithmInfoService.selectAlgorithmDetail(((Number) nodeAlgObj).longValue());
                if (alg != null) {
                    nodeAlg = alg.getAlgorithmName();
                    JSONObject params = n.getJSONObject("weightAssignAlgorithmParams");
                    if (params != null) {
                        nodeParams = params.toJSONString();
                    }
                }
            } else if (nodeAlgObj instanceof String) {
                nodeAlg = (String) nodeAlgObj;
            }

            String module = StringUtils.isNotEmpty(overrideModule) ? overrideModule : mapToPythonModule(nodeAlg);
            JSONArray weights = runModuleForChildren(module, ch, sampleRows, rnd, nodeParams);
            applySiblingWeights(ch, weights);
            calls++;
            for (int j = 0; j < ch.size(); j++) {
                JSONArray sub = new JSONArray();
                sub.add(ch.getJSONObject(j));
                calls += walkAssignWeights(sub, system, sampleRows, rnd, overrideModule);
            }
        }
        return calls;
    }

    private String resolveOverrideWeightModule(JSONObject options) {
        if (options == null) {
            return null;
        }
        Long algId = options.getLong("overrideAlgorithmId");
        if (algId == null || algId <= 0) {
            return null;
        }
        AlgorithmInfo alg = algorithmInfoService.selectAlgorithmDetail(algId);
        if (alg == null) {
            throw new ServiceException("覆盖权重算法不存在: " + algId);
        }
        String codeUrl = alg.getAlgorithmCodeUrl();
        if (StringUtils.isNotEmpty(codeUrl)) {
            String path = codeUrl.replace('\\', '/');
            int idx = path.lastIndexOf('/');
            String file = idx >= 0 ? path.substring(idx + 1) : path;
            if (file.endsWith(".zip")) {
                file = file.substring(0, file.length() - 4);
            }
            if (StringUtils.isNotEmpty(file)) {
                return file;
            }
        }
        return mapToPythonModule(alg.getAlgorithmName());
    }

    private void ensureWeightPresent(JSONArray nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch == null || ch.isEmpty()) {
                continue;
            }
            for (int j = 0; j < ch.size(); j++) {
                if (ch.getJSONObject(j).get("weight") == null) {
                    throw new ServiceException("存在缺失权重，按 missingWeightPolicy=TERMINATE 终止计算");
                }
            }
            ensureWeightPresent(ch);
        }
    }

    private static String mapToPythonModule(String uiCode) {
        if (StringUtils.isEmpty(uiCode)) {
            return "cal_weight";
        }
        switch (uiCode.trim()) {
            case "AHP":
            case "FAHP":
                return "ahp";
            default:
                return "cal_weight";
        }
    }

    private JSONArray runModuleForChildren(String module, JSONArray children, int sampleRows, Random rnd, String configJson) {
        int n = children.size();
        if (n <= 0) {
            return new JSONArray();
        }
        if ("ahp".equals(module)) {
            String literal = buildMockAhpMatrixJson(n, rnd);
            return zgpgAlgsClient.runWeightAlgorithmSync("ahp", literal, configJson);
        }
        String literal = buildMockEntropySampleMatrixJson(sampleRows, n, rnd);
        return zgpgAlgsClient.runWeightAlgorithmSync("cal_weight", literal, configJson);
    }

    /**
     * 熵权法：行为样本、列为同级子指标。
     */
    private static String buildMockEntropySampleMatrixJson(int rows, int cols, Random rnd) {
        JSONArray outer = new JSONArray();
        for (int r = 0; r < rows; r++) {
            JSONArray row = new JSONArray();
            for (int c = 0; c < cols; c++) {
                row.add(round8(0.08 + rnd.nextDouble() * 0.92));
            }
            outer.add(row);
        }
        return outer.toJSONString();
    }

    /**
     * AHP：构造 n×n 互反矩阵（主对角线为 1，上三角为 1～9 随机整数）。
     */
    private static String buildMockAhpMatrixJson(int n, Random rnd) {
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) {
            m[i][i] = 1.0;
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int scale = 1 + rnd.nextInt(9);
                m[i][j] = scale;
                m[j][i] = 1.0 / scale;
            }
        }
        JSONArray outer = new JSONArray();
        for (double[] row : m) {
            JSONArray jr = new JSONArray();
            for (double v : row) {
                jr.add(round8(v));
            }
            outer.add(jr);
        }
        return outer.toJSONString();
    }

    private static void applySiblingWeights(JSONArray children, JSONArray weights) {
        if (children == null || weights == null || children.size() != weights.size()) {
            throw new ServiceException("算法返回权重维数与当前父节点子节点数不一致（children=" + sizeOf(children)
                    + ", weights=" + sizeOf(weights) + "）");
        }
        for (int i = 0; i < children.size(); i++) {
            double w = weights.getDoubleValue(i);
            children.getJSONObject(i).put("weight", round8(w));
        }
    }

    private static int sizeOf(JSONArray a) {
        return a == null ? -1 : a.size();
    }

    private static double round8(double v) {
        return Math.round(v * 1.0e8) / 1.0e8;
    }
}
