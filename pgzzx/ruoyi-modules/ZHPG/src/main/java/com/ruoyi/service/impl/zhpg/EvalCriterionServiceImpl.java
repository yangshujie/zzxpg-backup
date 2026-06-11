package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.domain.zhpg.EvalCriterion;
import com.ruoyi.mapper.zhpg.EvalCriterionMapper;
import com.ruoyi.service.zhpg.IEvalCriterionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评估准则明细Service实现
 */
@Slf4j
@Service
public class EvalCriterionServiceImpl extends ServiceImpl<EvalCriterionMapper, EvalCriterion>
        implements IEvalCriterionService {

    private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<EvalCriterion> selectCriterionListBySetId(Long setId) {
        if (setId == null) {
            return new ArrayList<>();
        }
        QueryWrapper<EvalCriterion> wrapper = new QueryWrapper<>();
        wrapper.eq("set_id", setId);
        wrapper.orderByAsc("priority", "id");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<EvalCriterion> selectCriterionListByTaskId(Long taskId) {
        if (taskId == null) {
            return new ArrayList<>();
        }
        // 动态查评估任务表获取准则集ID
        String sql = "SELECT criterion_set_id FROM pgzc_eval_task WHERE id = ? LIMIT 1";
        List<Long> setIds = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("criterion_set_id"), taskId);
        if (setIds == null || setIds.isEmpty() || setIds.get(0) == null) {
            return new ArrayList<>();
        }
        return selectCriterionListBySetId(setIds.get(0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchSaveCriteria(Long setId, List<EvalCriterion> criterionList) {
        if (setId == null) {
            return 0;
        }
        // 1. 物理清空旧准则明细，防止由于逻辑删除导致唯一性冲突
        jdbcTemplate.update("DELETE FROM pgzc_eval_criterion WHERE set_id = ?", setId);

        if (criterionList == null || criterionList.isEmpty()) {
            return 0;
        }

        // 2. 物理清除已逻辑删除（deleted = 1）且 criterionCode 相同的历史脏数据，避免唯一性索引冲突
        List<String> codes = new ArrayList<>();
        for (EvalCriterion item : criterionList) {
            if (item.getCriterionCode() != null && !item.getCriterionCode().isEmpty()) {
                codes.add(item.getCriterionCode());
            }
        }
        if (!codes.isEmpty()) {
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < codes.size(); i++) {
                if (i > 0) {
                    placeholders.append(",");
                }
                placeholders.append("?");
            }
            String sql = "DELETE FROM pgzc_eval_criterion WHERE deleted = 1 AND criterion_code IN (" + placeholders.toString() + ")";
            jdbcTemplate.update(sql, codes.toArray());
        }

        // 3. 批量插入新准则明细，并包含批次内去重、脏数据物理清理与冲突自愈逻辑
        int count = 0;
        java.util.Set<String> usedCodesInBatch = new java.util.HashSet<>();

        for (EvalCriterion item : criterionList) {
            item.setId(null); // 强制自增
            item.setSetId(setId);

            String code = item.getCriterionCode();

            // 如果为空，先初始化一个
            if (code == null || code.trim().isEmpty()) {
                code = "EC" + String.format("%06d", CODE_SEQ.incrementAndGet());
            }

            // 批次（列表）内去重防冲突
            if (usedCodesInBatch.contains(code)) {
                String oldCode = code;
                code = "EC" + System.currentTimeMillis() + "_" + String.format("%04d", CODE_SEQ.incrementAndGet() % 10000);
                log.info("Detected duplicate criterionCode [{}] within the save batch, re-generated to [{}]", oldCode, code);
            }

            // 数据库索引冲突自愈检测
            boolean needCheck = true;
            int maxAttempts = 5; // 防御性循环上限
            while (needCheck && maxAttempts > 0) {
                maxAttempts--;
                needCheck = false;

                String checkSql = "SELECT id, set_id, deleted FROM pgzc_eval_criterion WHERE criterion_code = ?";
                List<java.util.Map<String, Object>> existList = jdbcTemplate.queryForList(checkSql, code);

                if (existList != null && !existList.isEmpty()) {
                    for (java.util.Map<String, Object> exist : existList) {
                        Long existId = ((Number) exist.get("id")).longValue();
                        Number existSetIdNum = (Number) exist.get("set_id");
                        Long existSetId = existSetIdNum != null ? existSetIdNum.longValue() : null;
                        Number deletedNum = (Number) exist.get("deleted");
                        int deletedVal = deletedNum != null ? deletedNum.intValue() : 0;

                        if (deletedVal == 1) {
                            // (1) 如果是逻辑删除状态的脏数据残留，立即执行物理清除，扫清冲突源
                            jdbcTemplate.update("DELETE FROM pgzc_eval_criterion WHERE id = ?", existId);
                            log.info("Physically deleted logical residual criterion record: id={}, code={}", existId, code);
                        } else {
                            // (2) 如果是正常活动的有效数据，无论是属于本准则集（刚才新插入的重复项）还是别的准则集（跨集冲突）
                            // 均进行自动重命名以保证保存成功
                            String oldCode = code;
                            code = "EC" + System.currentTimeMillis() + "_" + String.format("%04d", CODE_SEQ.incrementAndGet() % 10000);
                            log.warn("Criterion code [{}] conflicts with existing active record (id={}, set_id={}). Re-generated to [{}]", 
                                    oldCode, existId, existSetId, code);
                            needCheck = true; // 针对重生成的 code 进行安全重检
                            break;
                        }
                    }
                }
            }

            item.setCriterionCode(code);
            usedCodesInBatch.add(code);

            if (item.getStatus() == null || item.getStatus().isEmpty()) {
                item.setStatus("PUBLISHED");
            }
            item.setCreateTime(new Date());

            try {
                baseMapper.insert(item);
            } catch (Exception e) {
                // 再次发生任何意外索引冲突时打印详细关联数据信息，以便极致排错
                log.error("Failed to insert criterion item. id={}, code={}, set_id={}. Database state lookup: {}", 
                        item.getId(), item.getCriterionCode(), item.getSetId(), 
                        jdbcTemplate.queryForList("SELECT id, set_id, deleted FROM pgzc_eval_criterion WHERE criterion_code = ?", item.getCriterionCode()), e);
                throw e;
            }
            count++;
        }
        return count;
    }
}
