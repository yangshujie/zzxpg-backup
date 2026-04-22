package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.CalcTask;
import com.ruoyi.domain.zhpg.dto.CalcExecutionRequest;
import com.ruoyi.domain.zhpg.dto.CalcTaskAsyncResult;

public interface ICalcTaskService extends IService<CalcTask> {

    Page<CalcTask> selectTaskPage(Page<?> page, CalcTask query);

    CalcTask selectTaskDetail(Long id);

    /**
     * 异步发起计算任务
     * 创建任务记录后立即返回，实际执行由XXL-JOB触发
     */
    CalcTask run(CalcExecutionRequest request);

    /**
     * 处理XXL-JOB执行完成的回调
     * @param taskId 任务ID
     * @param success 是否成功
     * @param resultJson 结果JSON（成功时为CalcExecuteResponse，失败时为错误信息）
     */
    void handleXxlJobCallback(Long taskId, boolean success, String resultJson);

    /**
     * 处理XXL-JOB执行完成的回调（带报告URL）
     * @param taskId 任务ID
     * @param success 是否成功
     * @param resultJson 结果JSON
     * @param reportUrl 报告URL（PDF）
     * @param wordUrl Word报告URL
     * @param wpsUrl WPS报告URL
     */
    void handleXxlJobCallbackWithReport(Long taskId, boolean success, String resultJson,
                                        String reportUrl, String wordUrl, String wpsUrl);

    /**
     * 获取任务执行状态（供前端轮询）
     */
    CalcTaskAsyncResult getTaskStatus(Long taskId);

    /**
     * 更新任务执行进度和详细阶段日志
     * @param taskId 任务ID
     * @param progress 进度百分比 (0-100)
     * @param message 阶段描述信息
     */
    void updateTaskProgress(Long taskId, int progress, String message);
}
