package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.AlgorithmInfo;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.SubjectiveWeightComputeResult;
import com.ruoyi.domain.zhpg.dto.WeightApplyResult;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.service.zhpg.IIndicatorTreeWeightService;
import com.ruoyi.service.zhpg.ISubjectiveWeightService;
import com.ruoyi.zhpg.util.ZhpgIndicatorSystemTreeHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper.ParsedTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 主观赋权：6 种方法全部用本地 Java 实现，与原参考项目 weightAssign2 中通过 Python 算法
 * 服务的实现一一对应（5 种本地化、1 种保持本地一致性校验）。
 *
 * 每个父节点在 indicator_tree_weight JSON 上保存：
 * <pre>
 *   weightAssignAlgorithm: "不校验" | "相似度法" | "校验" | "理论证据法" | "连环比率法" | "层次分析"
 *   expertList: [userName, ...]                          // 校验用
 * </pre>
 * 每个子节点根据父节点选择保存以下任意字段：
 * <pre>
 *   importance: number                                   // 不校验 / 相似度法
 *   weightTemp: number                                   // 相似度法 / 理论证据法
 *   weightTemp1: number                                  // 理论证据法
 *   expertWeight: [number, ...]                          // 校验（与父 expertList 对齐）
 *   bz: "a" 或 "a/b" 字符串                                // 连环比率法
 * </pre>
 * 父节点附加：
 * <pre>
 *   jz: number[n][n]                                     // 层次分析
 * </pre>
 */
@Service
public class SubjectiveWeightServiceImpl implements ISubjectiveWeightService {

    private static final Logger log = LoggerFactory.getLogger(SubjectiveWeightServiceImpl.class);

    /** AHP RI 表（n=1..12）。与参考实现保持一致；n>12 时取 n=12 的值。 */
    private static final double[] AHP_RI = new double[]{
            0d, 0d, 0.58d, 0.90d, 1.12d, 1.24d, 1.32d, 1.41d, 1.45d, 1.49d, 1.513d, 1.527d
    };

    /** AHP CR 阈值（与参考实现保持一致）。 */
    private static final double AHP_CR_THRESHOLD = 0.1d;

    private static final String ALG_NONE = "不校验";
    private static final String ALG_SIM = "相似度法";
    private static final String ALG_EXPERT = "校验";
    private static final String ALG_EVIDENCE = "理论证据法";
    private static final String ALG_CHAIN = "连环比率法";
    private static final String ALG_AHP = "层次分析";

    @Autowired
    private IEvalIndicatorSystemService indicatorSystemService;

    @Autowired
    private IIndicatorTreeWeightService indicatorTreeWeightService;

    @Autowired
    private IAlgorithmInfoService algorithmInfoService;

    // ============================== 对外接口 ==============================

    @Override
    public double calcAhpCr(JSONArray matrix) {
        if (matrix == null) {
            throw new ServiceException("AHP 判断矩阵为空");
        }
        JSONArray normalized = normalizeMatrixToNumbers(matrix);
        Double cr = computeAhpCr(normalized);
        if (cr == null) {
            throw new ServiceException("AHP 判断矩阵无效，无法计算 CR");
        }
        return cr;
    }

    @Override
    public boolean checkAHP(JSONArray matrix) {
        try {
            return calcAhpCr(matrix) <= AHP_CR_THRESHOLD;
        } catch (ServiceException e) {
            return false;
        }
    }

    @Override
    public SubjectiveWeightComputeResult computeForSystem(Long systemId, JSONObject options, String operator) {
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

        SubjectiveWeightComputeResult r = computeForTreeJson(sourceJson);

        if (persist) {
            system.setIndicatorTreeWeight(r.getIndicatorTreeWeight());
            system.setUpdateTime(new Date());
            if (StringUtils.isNotEmpty(operator)) {
                system.setUpdateBy(operator);
            }
            indicatorSystemService.updateById(system);
        }
        return r;
    }

    @Override
    public SubjectiveWeightComputeResult computeForTreeJson(String indicatorTreeJson) {
        if (StringUtils.isEmpty(indicatorTreeJson)) {
            throw new ServiceException("指标树为空，无法计算权重");
        }
        ParsedTree meta = ZhpgIndicatorTreeJsonHelper.parseForWeight(indicatorTreeJson);
        JSONArray roots = JSON.parseArray(meta.getRootsForWeight().toJSONString());

        WalkStat stat = new WalkStat();
        walkAssignSubjective(roots, stat);

        String merged = ZhpgIndicatorTreeJsonHelper.serializeAfterWeight(meta, roots);
        WeightApplyResult renorm = indicatorTreeWeightService.applyWeights(merged, "RENORMALIZE");
        String finalTree = renorm.getIndicatorTree();

        StringBuilder hint = new StringBuilder();
        hint.append("已对 ").append(stat.parentCount).append(" 个父节点完成主观赋权");
        if (stat.ahpFailCount > 0) {
            hint.append("；其中 ").append(stat.ahpFailCount).append(" 个 AHP 节点一致性 CR > 0.1，权重已计算但建议复核判断矩阵");
        }
        if (StringUtils.isNotEmpty(renorm.getHint())) {
            hint.append("。").append(renorm.getHint());
        }
        return new SubjectiveWeightComputeResult(finalTree, hint.toString(), stat.parentCount, stat.ahpFailCount);
    }

    @Override
    public WeightApplyResult applyUserEditedTreeWeights(Long systemId, String userEditedTreeJson, String operator) {
        if (systemId == null || systemId <= 0) {
            throw new ServiceException("指标体系ID无效");
        }
        EvalIndicatorSystem system = indicatorSystemService.getById(systemId);
        if (system == null) {
            throw new ServiceException("指标体系不存在");
        }
        if (StringUtils.isEmpty(userEditedTreeJson)) {
            throw new ServiceException("指标树为空");
        }
        WeightApplyResult renorm = indicatorTreeWeightService.applyWeights(userEditedTreeJson, "RENORMALIZE");
        system.setIndicatorTreeWeight(renorm.getIndicatorTree());
        system.setUpdateTime(new Date());
        if (StringUtils.isNotEmpty(operator)) {
            system.setUpdateBy(operator);
        }
        indicatorSystemService.updateById(system);
        return renorm;
    }

    @Override
    public void computeSingleNode(JSONObject parent, JSONArray children, JSONObject stat) {
        if (parent == null || children == null || children.isEmpty()) {
            return;
        }
        String alg = resolveSubjectiveAlgorithm(parent);
        WalkStat internalStat = new WalkStat();
        double[] weights = dispatch(parent, children, alg, internalStat);
        applySiblingWeights(children, weights);
        if (internalStat.ahpFailCount > 0) {
            int current = stat == null ? 0 : stat.getIntValue("ahpFailCount", 0);
            if (stat != null) {
                stat.put("ahpFailCount", current + internalStat.ahpFailCount);
            }
        }
    }

    // ============================== 核心遍历 ==============================

    private static class WalkStat {
        int parentCount = 0;
        int ahpFailCount = 0;
    }

    private void walkAssignSubjective(JSONArray nodes, WalkStat stat) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch == null || ch.isEmpty()) {
                continue;
            }
            if (ch.size() == 1) {
                ch.getJSONObject(0).put("weight", round8(1.0));
                walkAssignSubjective(ch, stat);
                continue;
            }

            String alg = resolveSubjectiveAlgorithm(n);
            try {
                double[] weights = dispatch(n, ch, alg, stat);
                applySiblingWeights(ch, weights);
            } catch (ServiceException sx) {
                throw sx;
            } catch (Exception ex) {
                throw new ServiceException("父节点[" + safeLabel(n) + "] 主观赋权失败（算法=" + alg + "）: "
                        + ex.getMessage());
            }
            stat.parentCount++;
            walkAssignSubjective(ch, stat);
        }
    }

    private double[] dispatch(JSONObject parent, JSONArray children, String alg, WalkStat stat) {
        switch (alg) {
            case ALG_AHP:
                return computeAhp(parent, children.size(), stat);
            case ALG_EXPERT:
                return computeExpert(children);
            case ALG_EVIDENCE:
                return computeEvidence(children);
            case ALG_CHAIN:
                return computeChain(children);
            case ALG_SIM:
                return computeSimilarity(children);
            case ALG_NONE:
            default:
                return computeNoCheck(children);
        }
    }

    /**
     * 解析父节点上的主观赋权算法。
     *
     * 规则：
     *   1. weightAssignAlgorithm 为算法管理表 ID 时，查算法表并按算法名推断 6 种主观算法；
     *   2. weightAssignAlgorithm 为 6 种中文字符串/英文代号时，直接解析；
     *   3. 仅当缺少 weightAssignAlgorithm 时，才兼容读取旧树里的 subtype。
     */
    private String resolveSubjectiveAlgorithm(JSONObject parent) {
        String algorithmRef = parent == null ? null : parent.getString("weightAssignAlgorithm");
        if (StringUtils.isNotEmpty(algorithmRef) && algorithmRef.trim().matches("\\d+")) {
            String inferred = resolveSubjectiveAlgorithmById(Long.valueOf(algorithmRef.trim()));
            if (StringUtils.isNotEmpty(inferred)) {
                return inferred;
            }
        }

        String raw = firstNonEmpty(algorithmRef, parent == null ? null : parent.getString("subtype"));
        return normalizeSubjectiveAlgorithm(raw);
    }

    private String resolveSubjectiveAlgorithmById(Long algorithmId) {
        if (algorithmId == null || algorithmInfoService == null) {
            return null;
        }
        try {
            AlgorithmInfo algorithm = algorithmInfoService.selectAlgorithmDetail(algorithmId);
            if (algorithm == null) {
                return null;
            }
            return inferSubjectiveAlgorithm(algorithm.getAlgorithmName());
        } catch (Exception ex) {
            log.warn("解析主观赋权算法ID[{}]失败，将按默认主观算法处理：{}", algorithmId, ex.getMessage());
            return null;
        }
    }

    private static String normalizeSubjectiveAlgorithm(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return ALG_NONE;
        }
        String v = raw.trim();
        // 数字 ID（算法管理表主键）不在主观分发范围 —— 让 ObjectiveWeightServiceImpl 处理
        if (v.matches("\\d+")) {
            return ALG_NONE;
        }
        // 直接命中 6 种中文名
        switch (v) {
            case ALG_NONE:
            case ALG_SIM:
            case ALG_EXPERT:
            case ALG_EVIDENCE:
            case ALG_CHAIN:
            case ALG_AHP:
                return v;
        }
        // 常见英文代号映射（与前端 ZHPG_WEIGHT_ASSIGN_OPTIONS 的 value 兼容）
        switch (v.toUpperCase()) {
            case "AHP":
                return ALG_AHP;
            case "FAHP":
                return ALG_SIM;
            case "RELATIVE":
                return ALG_NONE;
            case "FUZZY_ENTROPY":
                return ALG_EVIDENCE;
            case "MIN_SQUARE":
                return ALG_CHAIN;
            case "DELPHI":
            case "SCORING":
            case "EXPERT":
                return ALG_EXPERT;
            default:
                return ALG_NONE;
        }
    }

    private static String inferSubjectiveAlgorithm(String algorithmName) {
        if (StringUtils.isEmpty(algorithmName)) {
            return null;
        }
        String normalized = normalizeSubjectiveAlgorithm(algorithmName);
        if (!ALG_NONE.equals(normalized) || ALG_NONE.equals(algorithmName.trim())) {
            return normalized;
        }
        String name = algorithmName.trim();
        if (name.contains("连环比率")) {
            return ALG_CHAIN;
        }
        if (name.contains("理论证据")) {
            return ALG_EVIDENCE;
        }
        if (name.contains("相似度") || name.matches(".*模糊层次.*相似.*")) {
            return ALG_SIM;
        }
        if (name.matches(".*(德尔菲|Delphi|专家调查|专家.*打分).*")) {
            return ALG_EXPERT;
        }
        if (name.matches(".*对数.*最小二乘.*层次.*")) {
            return ALG_AHP;
        }
        if (name.matches(".*多维度.*层次.*")) {
            return ALG_EVIDENCE;
        }
        if (name.contains("相对比较")) {
            return ALG_NONE;
        }
        if (name.matches(".*(层次分析|AHP).*")) {
            return ALG_AHP;
        }
        return null;
    }

    // ============================== 6 种主观算法 ==============================

    /** 不校验：直接按 importance 归一化。 */
    private static double[] computeNoCheck(JSONArray children) {
        int n = children.size();
        double[] imp = new double[n];
        double sum = 0d;
        for (int i = 0; i < n; i++) {
            double v = getDouble(children.getJSONObject(i), "importance", 0d);
            if (v < 0d) {
                v = 0d;
            }
            imp[i] = v;
            sum += v;
        }
        return sum > 0d ? scale(imp, 1d / sum) : equalShare(n);
    }

    /**
     * 相似度法：把 importance（粗排序）和 weightTemp（精分布）按余弦相似度做加权融合，
     * 与参考项目 ahpScore2.vue 两步交互（先 importance 再 weightTemp）一致。
     * α = cos(imp_norm, weightTemp)；weight = α·imp_norm + (1-α)·weightTemp，再归一。
     */
    private static double[] computeSimilarity(JSONArray children) {
        int n = children.size();
        double[] imp = new double[n];
        double[] wt = new double[n];
        double sumImp = 0d;
        double sumWt = 0d;
        for (int i = 0; i < n; i++) {
            JSONObject c = children.getJSONObject(i);
            imp[i] = Math.max(0d, getDouble(c, "importance", 0d));
            wt[i] = Math.max(0d, getDouble(c, "weightTemp", 0d));
            sumImp += imp[i];
            sumWt += wt[i];
        }
        double[] impN = sumImp > 0d ? scale(imp, 1d / sumImp) : equalShare(n);
        double[] wtN = sumWt > 0d ? scale(wt, 1d / sumWt) : equalShare(n);
        double sim = cosineSim(impN, wtN);
        if (Double.isNaN(sim)) {
            sim = 0.5d;
        }
        double alpha = Math.max(0d, Math.min(1d, (sim + 1d) / 2d));
        double[] out = new double[n];
        double s = 0d;
        for (int i = 0; i < n; i++) {
            out[i] = alpha * impN[i] + (1d - alpha) * wtN[i];
            s += out[i];
        }
        return s > 0d ? scale(out, 1d / s) : equalShare(n);
    }

    /** 校验（多专家平均）：对每个子节点 expertWeight[] 取均值，再归一。 */
    private static double[] computeExpert(JSONArray children) {
        int n = children.size();
        double[] out = new double[n];
        double sum = 0d;
        for (int i = 0; i < n; i++) {
            JSONArray arr = children.getJSONObject(i).getJSONArray("expertWeight");
            double mean;
            if (arr == null || arr.isEmpty()) {
                mean = 0d;
            } else {
                double s = 0d;
                int cnt = 0;
                for (int k = 0; k < arr.size(); k++) {
                    Double v = arr.getDouble(k);
                    if (v != null && !Double.isNaN(v) && !Double.isInfinite(v)) {
                        s += v;
                        cnt++;
                    }
                }
                mean = cnt > 0 ? s / cnt : 0d;
            }
            if (mean < 0d) {
                mean = 0d;
            }
            out[i] = mean;
            sum += mean;
        }
        return sum > 0d ? scale(out, 1d / sum) : equalShare(n);
    }

    /**
     * 理论证据法（Dempster 合成简化版）：把两位专家给的两组分布 weightTemp1 / weightTemp
     * 按乘积归一，与参考项目 ahpScore4.vue 的"专家1/专家2"两步输入对应。
     */
    private static double[] computeEvidence(JSONArray children) {
        int n = children.size();
        double[] e1 = new double[n];
        double[] e2 = new double[n];
        for (int i = 0; i < n; i++) {
            JSONObject c = children.getJSONObject(i);
            e1[i] = Math.max(0d, getDouble(c, "weightTemp1", 0d));
            e2[i] = Math.max(0d, getDouble(c, "weightTemp", 0d));
        }
        double[] prod = new double[n];
        double sum = 0d;
        for (int i = 0; i < n; i++) {
            prod[i] = e1[i] * e2[i];
            sum += prod[i];
        }
        if (sum > 0d) {
            return scale(prod, 1d / sum);
        }
        // 若两组分布交叉为 0，退化为两组平均后归一
        double[] avg = new double[n];
        double sa = 0d;
        for (int i = 0; i < n; i++) {
            avg[i] = (e1[i] + e2[i]) / 2d;
            sa += avg[i];
        }
        return sa > 0d ? scale(avg, 1d / sa) : equalShare(n);
    }

    /**
     * 连环比率法：children[i].bz = w_i / w_{i+1}（字符串 "a" 或 "a/b"），最后一个 bz 不参与。
     * 反向回溯：w_{n-1}=1，w_i = bz_i · w_{i+1}，再归一。
     */
    private static double[] computeChain(JSONArray children) {
        int n = children.size();
        double[] ratio = new double[Math.max(0, n - 1)];
        for (int i = 0; i < n - 1; i++) {
            double r = parseRatio(children.getJSONObject(i).getString("bz"));
            if (!(r > 0d) || Double.isInfinite(r) || Double.isNaN(r)) {
                r = 1d;
            }
            ratio[i] = r;
        }
        double[] raw = new double[n];
        raw[n - 1] = 1d;
        for (int i = n - 2; i >= 0; i--) {
            raw[i] = ratio[i] * raw[i + 1];
        }
        double sum = 0d;
        for (double v : raw) {
            sum += v;
        }
        return sum > 0d ? scale(raw, 1d / sum) : equalShare(n);
    }

    /** 层次分析：对父节点 jz 做幂迭代，取主特征向量为权重；同时计算 CR 并统计失败数。 */
    private double[] computeAhp(JSONObject parent, int n, WalkStat stat) {
        JSONArray jz = parent.getJSONArray("jz");
        if (jz == null || jz.isEmpty()) {
            log.warn("AHP 节点[{}] 无 jz 判断矩阵，退化为均分", safeLabel(parent));
            return equalShare(n);
        }
        JSONArray normalized = normalizeMatrixToNumbers(jz);
        if (normalized.size() != n) {
            log.warn("AHP 节点[{}] jz 维度({})与子节点数({})不一致，退化为均分", safeLabel(parent), normalized.size(), n);
            return equalShare(n);
        }
        AhpPowerResult res = powerIterAhp(normalized);
        if (res == null || res.eigenvector == null) {
            log.warn("AHP 节点[{}] 幂迭代失败，退化为均分", safeLabel(parent));
            return equalShare(n);
        }
        Double cr = res.cr;
        if (cr == null || cr > AHP_CR_THRESHOLD) {
            stat.ahpFailCount++;
        }
        return res.eigenvector;
    }

    // ============================== AHP 实现 ==============================

    private static Double computeAhpCr(JSONArray dataArray) {
        AhpPowerResult r = powerIterAhp(dataArray);
        return r == null ? null : r.cr;
    }

    private static class AhpPowerResult {
        double[] eigenvector;
        Double cr;
    }

    private static AhpPowerResult powerIterAhp(JSONArray dataArray) {
        if (dataArray == null) {
            return null;
        }
        final int n = dataArray.size();
        if (n == 0) {
            return null;
        }
        if (n <= 2) {
            AhpPowerResult r = new AhpPowerResult();
            r.eigenvector = equalShare(n);
            r.cr = 0d;
            return r;
        }
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) {
            JSONArray row = dataArray.getJSONArray(i);
            if (row == null || row.size() != n) {
                return null;
            }
            for (int j = 0; j < n; j++) {
                Object cell = row.get(j);
                double v;
                if (cell instanceof Number) {
                    v = ((Number) cell).doubleValue();
                } else {
                    v = parseRatio(String.valueOf(cell));
                }
                if (!(v > 0d) || Double.isInfinite(v) || Double.isNaN(v)) {
                    return null;
                }
                m[i][j] = v;
            }
        }
        double[] v = new double[n];
        double init = 1.0d / n;
        for (int i = 0; i < n; i++) {
            v[i] = init;
        }
        double lambda = 0d;
        final int maxIter = 2000;
        final double tol = 1e-12;
        for (int iter = 0; iter < maxIter; iter++) {
            double[] w = matVec(m, v);
            double sum = 0d;
            for (double x : w) {
                sum += x;
            }
            if (sum == 0d || Double.isNaN(sum) || Double.isInfinite(sum)) {
                return null;
            }
            for (int i = 0; i < n; i++) {
                v[i] = w[i] / sum;
            }
            double[] Av = matVec(m, v);
            double lambdaNext = 0d;
            int cnt = 0;
            for (int i = 0; i < n; i++) {
                if (v[i] > 0d) {
                    lambdaNext += (Av[i] / v[i]);
                    cnt++;
                }
            }
            if (cnt == 0) {
                return null;
            }
            lambdaNext /= cnt;
            if (iter > 0 && Math.abs(lambdaNext - lambda) < tol) {
                lambda = lambdaNext;
                break;
            }
            lambda = lambdaNext;
        }
        double ri = (n > 12) ? AHP_RI[11] : AHP_RI[n - 1];
        double ci = (lambda - n) / (n - 1);
        AhpPowerResult r = new AhpPowerResult();
        r.eigenvector = v;
        r.cr = ri > 0d ? ci / ri : 0d;
        return r;
    }

    private static double[] matVec(double[][] A, double[] v) {
        int n = A.length;
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double s = 0d;
            for (int j = 0; j < n; j++) {
                s += A[i][j] * v[j];
            }
            out[i] = s;
        }
        return out;
    }

    /** 把 JSONArray 矩阵中可能存在的 "a/b" 字符串就地展开为 Double。 */
    private static JSONArray normalizeMatrixToNumbers(JSONArray data) {
        JSONArray out = new JSONArray(data.size());
        for (int i = 0; i < data.size(); i++) {
            JSONArray src = data.getJSONArray(i);
            if (src == null) {
                out.add(new JSONArray());
                continue;
            }
            JSONArray row = new JSONArray(src.size());
            for (int j = 0; j < src.size(); j++) {
                Object cell = src.get(j);
                if (cell instanceof Number) {
                    row.add(((Number) cell).doubleValue());
                } else {
                    row.add(parseRatio(String.valueOf(cell)));
                }
            }
            out.add(row);
        }
        return out;
    }

    // ============================== 通用工具 ==============================

    private static double parseRatio(String s) {
        if (StringUtils.isEmpty(s)) {
            return 1d;
        }
        String[] parts = s.trim().split("/");
        try {
            if (parts.length == 1) {
                return Double.parseDouble(parts[0]);
            }
            double a = Double.parseDouble(parts[0]);
            double b = Double.parseDouble(parts[1]);
            if (b == 0d) {
                return 1d;
            }
            return a / b;
        } catch (NumberFormatException e) {
            return 1d;
        }
    }

    private static void applySiblingWeights(JSONArray children, double[] weights) {
        if (children == null || weights == null || children.size() != weights.length) {
            throw new ServiceException("主观赋权返回维数与子节点数不一致（children="
                    + (children == null ? -1 : children.size()) + ", weights="
                    + (weights == null ? -1 : weights.length) + "）");
        }
        for (int i = 0; i < weights.length; i++) {
            children.getJSONObject(i).put("weight", round8(weights[i]));
        }
    }

    private static double[] equalShare(int n) {
        double[] o = new double[n];
        if (n <= 0) {
            return o;
        }
        double w = 1d / n;
        for (int i = 0; i < n; i++) {
            o[i] = w;
        }
        return o;
    }

    private static double[] scale(double[] v, double k) {
        double[] o = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            o[i] = v[i] * k;
        }
        return o;
    }

    private static double cosineSim(double[] a, double[] b) {
        double dot = 0d;
        double na = 0d;
        double nb = 0d;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na <= 0d || nb <= 0d) {
            return Double.NaN;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static double getDouble(JSONObject o, String key, double dft) {
        if (o == null) {
            return dft;
        }
        Double v = o.getDouble(key);
        return v == null || Double.isNaN(v) || Double.isInfinite(v) ? dft : v;
    }

    private static String firstNonEmpty(String... values) {
        for (String s : values) {
            if (StringUtils.isNotEmpty(s)) {
                return s;
            }
        }
        return null;
    }

    private static String safeLabel(JSONObject n) {
        if (n == null) {
            return "?";
        }
        String l = n.getString("label");
        return StringUtils.isNotEmpty(l) ? l : String.valueOf(n.get("uid"));
    }

    private static double round8(double v) {
        return Math.round(v * 1.0e8) / 1.0e8;
    }
}
