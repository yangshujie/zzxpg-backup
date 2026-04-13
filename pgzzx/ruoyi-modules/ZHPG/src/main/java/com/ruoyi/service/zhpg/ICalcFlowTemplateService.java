package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;

import java.util.List;

/**
 * 评估计算流程模板Service接口
 */
public interface ICalcFlowTemplateService extends IService<CalcFlowTemplate> {

    /**
     * 分页查询流程模板列表
     */
    Page<CalcFlowTemplate> selectTemplatePage(Page<?> page, CalcFlowTemplate query);

    /**
     * 查询流程模板列表（不分页）
     */
    List<CalcFlowTemplate> selectTemplateList(CalcFlowTemplate query);

    /**
     * 查询流程模板详情
     */
    CalcFlowTemplate selectTemplateDetail(Long id);

    /**
     * 新增流程模板
     */
    int insertTemplate(CalcFlowTemplate template);

    /**
     * 修改流程模板
     */
    int updateTemplate(CalcFlowTemplate template);

    /**
     * 批量删除流程模板
     */
    int deleteTemplateByIds(Long[] ids);

    /**
     * 提交测试（DRAFT -> TESTING）
     */
    int submitTest(Long id);

    /**
     * 发布模板（TESTING -> PUBLISHED）
     */
    int publishTemplate(Long id);

    /**
     * 停用模板（PUBLISHED -> DISABLED）
     */
    int disableTemplate(Long id);

    /**
     * 启用模板（DISABLED/TESTING -> PUBLISHED）
     */
    int enableTemplate(Long id);

    /**
     * 复制新版本
     */
    CalcFlowTemplate copyVersion(Long id);

    /**
     * 确保模板在 xxl-job-admin 中已注册，返回 jobId。
     * 若已注册则同步更新路由策略等配置。
     */
    int ensureXxlJobRegistered(CalcFlowTemplate template);

    /**
     * 同步 XXL-JOB 展示信息，不改变稳定 taskName，只刷新展示名称等元数据。
     */
    void refreshXxlJobRuntimeMeta(CalcFlowTemplate template, String runtimeTaskName);
}
