package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.dto.WeightApplyResult;
import com.ruoyi.service.zhpg.IIndicatorTreeWeightService;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper;
import com.ruoyi.zhpg.util.ZhpgIndicatorTreeJsonHelper.ParsedTree;
import org.springframework.stereotype.Service;

/**
 * 不依赖旧版 Python 算法包：层内均分或按现有正值比例归一到 1
 */
@Service
public class IndicatorTreeWeightServiceImpl implements IIndicatorTreeWeightService {

    @Override
    public WeightApplyResult applyWeights(String indicatorTreeJson, String strategy) {
        String eff = StringUtils.isEmpty(strategy) ? "AUTO" : strategy.trim().toUpperCase();
        String hint = "";

        if ("AUTO".equals(eff)) {
            eff = "RENORMALIZE";
        } else if (!"EQUAL".equals(eff) && !"RENORMALIZE".equals(eff)) {
            throw new ServiceException("不支持的 strategy，应为 AUTO、EQUAL 或 RENORMALIZE");
        }

        ParsedTree meta = ZhpgIndicatorTreeJsonHelper.parseForWeight(indicatorTreeJson);
        JSONArray roots = meta.getRootsForWeight();
        if ("EQUAL".equals(eff)) {
            applyEqualDepthFirst(roots);
        } else {
            applyRenormDepthFirst(roots);
        }
        return new WeightApplyResult(ZhpgIndicatorTreeJsonHelper.serializeAfterWeight(meta, roots), hint);
    }

    private void applyEqualDepthFirst(JSONArray nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch != null && !ch.isEmpty()) {
                double w = 1.0 / ch.size();
                for (int j = 0; j < ch.size(); j++) {
                    ch.getJSONObject(j).put("weight", round8(w));
                }
                applyEqualDepthFirst(ch);
            }
        }
    }

    private void applyRenormDepthFirst(JSONArray nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch != null && !ch.isEmpty()) {
                double sum = 0;
                int positive = 0;
                for (int j = 0; j < ch.size(); j++) {
                    JSONObject c = ch.getJSONObject(j);
                    Double w = c.getDouble("weight");
                    if (w != null && w > 0) {
                        sum += w;
                        positive++;
                    }
                }
                if (sum <= 0) {
                    double w = 1.0 / ch.size();
                    for (int j = 0; j < ch.size(); j++) {
                        ch.getJSONObject(j).put("weight", round8(w));
                    }
                } else {
                    for (int j = 0; j < ch.size(); j++) {
                        JSONObject c = ch.getJSONObject(j);
                        Double w = c.getDouble("weight");
                        if (w != null && w > 0) {
                            c.put("weight", round8(w / sum));
                        } else {
                            c.put("weight", 0.0);
                        }
                    }
                }
                applyRenormDepthFirst(ch);
            }
        }
    }

    private static double round8(double v) {
        return Math.round(v * 1.0e8) / 1.0e8;
    }
}
