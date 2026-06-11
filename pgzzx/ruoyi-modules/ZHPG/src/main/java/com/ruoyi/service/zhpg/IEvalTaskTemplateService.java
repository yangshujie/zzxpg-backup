package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalTaskTemplate;

import java.util.List;

/**
 * 评估任务模板Service接口
 */
public interface IEvalTaskTemplateService extends IService<EvalTaskTemplate> {

    /** 分页查询任务模板列表 */
    Page<EvalTaskTemplate> selectTemplatePage(Page<?> page, EvalTaskTemplate query);

    /** 查询任务模板列表（不分页） */
    List<EvalTaskTemplate> selectTemplateList(EvalTaskTemplate query);

    /** 查询任务模板详情 */
    EvalTaskTemplate selectTemplateDetail(Long id);

    /** 新增任务模板 */
    int insertTemplate(EvalTaskTemplate template);

    /** 修改任务模板 */
    int updateTemplate(EvalTaskTemplate template);

    /** 批量删除任务模板 */
    int deleteTemplateByIds(Long[] ids);

    /** 停用模板（PUBLISHED -> DISABLED） */
    int disableTemplate(Long id);

    /** 启用模板（DISABLED -> PUBLISHED） */
    int enableTemplate(Long id);
}
