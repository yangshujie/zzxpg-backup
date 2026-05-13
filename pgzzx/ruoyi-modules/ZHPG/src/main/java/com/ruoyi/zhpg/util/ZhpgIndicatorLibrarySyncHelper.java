package com.ruoyi.zhpg.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 指标库同步助手类
 * 负责将指标体系中的 JSON 树节点同步到 pgzc_indicator 表中，确保 ID 唯一且关联。
 */
@Component
public class ZhpgIndicatorLibrarySyncHelper {

    /**
     * 同步指标树到指标库
     *
     * @param indicatorTreeJson 原始 JSON 字符串
     * @param isTemplate        是否为模板模式（同步到指标模板还是指标实例）
     * @param systemId          当前所属指标体系ID
     * @param systemName        当前所属指标体系名称
     * @param service           指标服务接口
     * @param operator          操作人
     * @return 同步后更新了 id (idCode) 的 JSON 字符串
     */
    public String syncTreeToLibrary(String indicatorTreeJson, Integer isTemplate, Long systemId, String systemName, IEvalIndicatorService service, String operator) {
        System.out.println(">>> [ZhpgSync] Starting sync to library. systemId=" + systemId + ", systemName=" + systemName + ", operator=" + operator + ", isTemplate=" + (isTemplate != null ? isTemplate : 0));
        if (StringUtils.isEmpty(indicatorTreeJson)) {
            System.out.println(">>> [ZhpgSync] indicatorTreeJson is empty, skipping.");
            return indicatorTreeJson;
        }

        try {
            Object parsed = JSON.parse(indicatorTreeJson);
            
            // --- 第一步：预校验 (Pre-check) ---
            List<String> errors = new ArrayList<>();
            if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                Object td = obj.containsKey("treeData") ? obj.get("treeData") : obj;
                if (td instanceof JSONObject) {
                    validateNodeBeforeSync((JSONObject) td, 1, isTemplate, systemId, service, errors);
                } else if (td instanceof JSONArray) {
                    validateArrayBeforeSync((JSONArray) td, 1, isTemplate, systemId, service, errors);
                }
            } else if (parsed instanceof JSONArray) {
                validateArrayBeforeSync((JSONArray) parsed, 1, isTemplate, systemId, service, errors);
            }

            if (!errors.isEmpty()) {
                throw new ServiceException("指标库同步预校验失败，请修正以下冲突后重试：\n" + String.join("\n", errors));
            }
            // --- 校验结束 ---

            if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                if (obj.containsKey("treeData")) {
                    Object td = obj.get("treeData");
                    if (td instanceof JSONObject) {
                        processNode((JSONObject) td, 0L, 1, isTemplate, systemId, systemName, service, operator);
                    } else if (td instanceof JSONArray) {
                        processArray((JSONArray) td, 0L, 1, isTemplate, systemId, systemName, service, operator);
                    }
                    return obj.toJSONString();
                }
                processNode(obj, 0L, 1, isTemplate, systemId, systemName, service, operator);
                return obj.toJSONString();
            } else if (parsed instanceof JSONArray) {
                JSONArray arr = (JSONArray) parsed;
                processArray(arr, 0L, 1, isTemplate, systemId, systemName, service, operator);
                return arr.toJSONString();
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            System.err.println(">>> [ZhpgSync] ERROR during sync: " + e.getMessage());
            e.printStackTrace();
            throw new ServiceException("同步指标库失败：" + e.getMessage());
        }

        return indicatorTreeJson;
    }

    private void processArray(JSONArray arr, Long parentId, int level, Integer isTemplate, Long systemId, String systemName, IEvalIndicatorService service, String operator) {
        if (arr == null || arr.isEmpty()) {
            return;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject node = arr.getJSONObject(i);
            processNode(node, parentId, level, isTemplate, systemId, systemName, service, operator);
        }
    }

    private void processNode(JSONObject node, Long parentId, int level, Integer isTemplate, Long systemId, String systemName, IEvalIndicatorService service, String operator) {
        if (node == null) {
            return;
        }

        // 排除虚拟根节点
        String uid = node.getString("uid");
        String id = node.getString("id");
        if (ZhpgIndicatorTreeJsonHelper.FOREST_ROOT_UID.equals(uid) || ZhpgIndicatorTreeJsonHelper.FOREST_ROOT_UID.equals(id)) {
            JSONArray children = node.getJSONArray("children");
            processArray(children, parentId, level, isTemplate, systemId, systemName, service, operator);
            return;
        }

        // 1. 获取基础属性
        String name = node.getString("indicatorName");
        if (StringUtils.isEmpty(name)) {
            name = node.getString("label");
        }
        if (StringUtils.isEmpty(name)) {
            name = "未命名节点";
        }
        
        String type = ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTypeCode(node.getString("indicatorType"));
        if (StringUtils.isEmpty(type)) {
            type = "无";
        }

        String idCode = StringUtils.isNotEmpty(id) ? id : uid;
        // 如果没有 ID，或者是旧的前端临时 UID，则肯定是新节点
        boolean isNew = StringUtils.isEmpty(idCode) || idCode.startsWith("node_");

        // 2. 无需名称重名检查，允许体系内灵活命名。

        // 3. 构建实体
        EvalIndicator indicator = new EvalIndicator();
        indicator.setIndicatorName(name.trim());
        indicator.setIndicatorType(type);
        indicator.setSourceSystemId(systemId);
        indicator.setSourceSystemName(systemName);
        indicator.setParentId(parentId);
        indicator.setLevel(level);
        indicator.setIsTemplate(isTemplate != null ? isTemplate : 0);
        indicator.setUnit(node.getString("unit"));
        indicator.setDescription(node.getString("description"));
        indicator.setValueMin(node.getBigDecimal("valueMin"));
        indicator.setValueMax(node.getBigDecimal("valueMax"));
        
        String valCat = node.getString("valueCategory");
        if (StringUtils.isEmpty(valCat)) {
            valCat = node.getString("type");
        }
        indicator.setValueCategory(valCat);

        // 计算方法处理
        String expectedCalcMethod = null;
        Object calcMethodObj = node.get("computeRule");
        if (calcMethodObj == null) {
            calcMethodObj = node.get("calcMethod");
        }
        if (calcMethodObj != null) {
            expectedCalcMethod = calcMethodObj instanceof String ? (String) calcMethodObj : JSON.toJSONString(calcMethodObj);
            indicator.setCalcMethod(expectedCalcMethod);
        }

        JSONArray children = node.getJSONArray("children");
        boolean hasChildren = children != null && !children.isEmpty();
        indicator.setIsBottomNode(!hasChildren);

        // 4. 执行持久化
        if (isNew) {
            System.out.println(">>> [ZhpgSync] Inserting new node: " + name + " (level " + level + ")");
            indicator.setCreateBy(operator);
            service.insertIndicator(indicator);
            assertCalcMethodPersisted(indicator, expectedCalcMethod, name, service);
            // 回填生成的 idCode 到 JSON
            node.put("id", indicator.getIdCode());
            node.put("uid", indicator.getIdCode());
        } else {
            // 按 idCode 查找
            EvalIndicator existing = service.getOne(new QueryWrapper<EvalIndicator>().eq("id_code", idCode).eq("is_template", isTemplate != null ? isTemplate : 0), false);
            if (existing != null) {
                System.out.println(">>> [ZhpgSync] Updating node: " + name + " (idCode=" + idCode + ")");
                indicator.setId(existing.getId());
                indicator.setUpdateBy(operator);
                service.updateIndicator(indicator);
                assertCalcMethodPersisted(indicator, expectedCalcMethod, name, service);
                // 确保 JSON 中的 ID 是最新的 idCode
                node.put("id", idCode);
                node.put("uid", idCode);
            } else {
                System.out.println(">>> [ZhpgSync] Node idCode " + idCode + " not found, inserting as new: " + name);
                // 如果 idCode 不存在（可能是脏数据或跨库导入），视为新增
                indicator.setIdCode(idCode);
                indicator.setCreateBy(operator);
                service.insertIndicator(indicator);
                assertCalcMethodPersisted(indicator, expectedCalcMethod, name, service);
                node.put("id", idCode);
                node.put("uid", idCode);
            }
        }

        // 获取当前节点的 Long ID 用于子节点挂载
        Long currentId = indicator.getId();

        // 5. 递归处理子节点
        if (hasChildren) {
            processArray(children, currentId, level + 1, isTemplate, systemId, systemName, service, operator);
        }
    }

    private void assertCalcMethodPersisted(EvalIndicator indicator, String expectedCalcMethod, String indicatorName, IEvalIndicatorService service) {
        if (indicator == null || StringUtils.isEmpty(expectedCalcMethod) || indicator.getId() == null) {
            return;
        }
        EvalIndicator persisted = service.getById(indicator.getId());
        String actual = persisted == null ? null : persisted.getCalcMethod();
        if (!Objects.equals(expectedCalcMethod, actual)) {
            int expectedLen = expectedCalcMethod.length();
            int actualLen = actual == null ? 0 : actual.length();
            throw new ServiceException("指标「" + indicatorName + "」计算规则写入后不完整：pgzc_indicator.calc_method 长度 "
                    + actualLen + "/" + expectedLen
                    + "。请确认数据库列 pgzc_indicator.calc_method 已升级为 TEXT，并重新保存指标体系。");
        }
    }

    // =========================================================================
    // 预校验逻辑 (Pre-check Pass)
    // =========================================================================

    private void validateArrayBeforeSync(JSONArray arr, int level, Integer isTemplate, Long systemId, IEvalIndicatorService service, List<String> errors) {
        if (arr == null || arr.isEmpty()) return;
        for (int i = 0; i < arr.size(); i++) {
            validateNodeBeforeSync(arr.getJSONObject(i), level, isTemplate, systemId, service, errors);
        }
    }

    private void validateNodeBeforeSync(JSONObject node, int level, Integer isTemplate, Long systemId, IEvalIndicatorService service, List<String> errors) {
        if (node == null) return;

        String uid = node.getString("uid");
        String id = node.getString("id");
        if (ZhpgIndicatorTreeJsonHelper.FOREST_ROOT_UID.equals(uid) || ZhpgIndicatorTreeJsonHelper.FOREST_ROOT_UID.equals(id)) {
            validateArrayBeforeSync(node.getJSONArray("children"), level, isTemplate, systemId, service, errors);
            return;
        }

        String name = node.getString("indicatorName");
        if (StringUtils.isEmpty(name)) name = node.getString("label");
        if (StringUtils.isEmpty(name)) name = "未命名节点";
        name = name.trim();

        String type = ZhpgIndicatorTreeJsonHelper.normalizeIndicatorTypeCode(node.getString("indicatorType"));
        if (StringUtils.isEmpty(type)) type = "无";

        JSONArray children = node.getJSONArray("children");
        boolean hasChildren = children != null && !children.isEmpty();

        // 检查装备类型是否合规 (叶子节点不能为无)
        if (!hasChildren && "无".equals(type)) {
            errors.add(String.format("- 第 %d 层: 底层指标「%s」不能为无装备类型，请选择具体的装备。", level, name));
        }

        // 递归校验子节点
        if (hasChildren) {
            validateArrayBeforeSync(children, level + 1, isTemplate, systemId, service, errors);
        }
    }
}
