package com.ruoyi.zhpgcalc.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 将 XXL-JOB 综合计算结果写回 pgzc_calc_task，供 ZHPG 轮询。
 * 需与 ZHPG 使用同一业务库（Nacos 中数据源指向同一库）。
 */
@Slf4j
@Component
public class CalcTaskAsyncResultWriter {

    @Autowired(required = false)
    private DataSource dataSource;

    public boolean isAvailable() {
        return dataSource != null;
    }

    public void write(Long taskId, String state, String payload) {
        if (taskId == null) {
            return;
        }
        if (dataSource == null) {
            log.warn("DataSource 未配置，无法回写综合计算异步结果, taskId={}", taskId);
            return;
        }
        JdbcTemplate jt = new JdbcTemplate(dataSource);
        int n = jt.update(
                "UPDATE pgzc_calc_task SET comprehensive_async_state = ?, comprehensive_async_json = ? WHERE id = ? AND del_flag = '0'",
                state, payload, taskId);
        if (n == 0) {
            log.warn("回写综合计算结果未更新任何行, taskId={}, state={}", taskId, state);
        }
    }
}
