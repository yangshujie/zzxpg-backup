package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * 评估指标体系Service接口
 */
public interface IEvalIndicatorSystemService extends IService<EvalIndicatorSystem> {

    Page<EvalIndicatorSystem> selectSystemPage(Page page, EvalIndicatorSystem query);

    List<EvalIndicatorSystem> selectSystemList(EvalIndicatorSystem query);

    int insertSystem(EvalIndicatorSystem system);

    int updateSystem(EvalIndicatorSystem system);

    int deleteSystemByIds(Long[] ids);

    EvalIndicatorSystem createFromTemplate(Long templateId, String systemName, String operator);

    /**
     * 外部系统选择 - 查询所有指标体系（支持下拉框，支持关键字搜索）
     * @param keyword 关键字
     * @return 指标体系列表
     */
    List<EvalIndicatorSystemSelectVO> selectIndicatorSystemListForSelect(String keyword);

    /**
     * 接收需求分析等系统下发的「需求 + 细化 treeData」报文，写入指标体系管理。
     * 报文字段兼容《下发至评估系统需求及对应指标数据》：顶层 treeData、需求元数据等。
     * 若 body 中含 targetIndicatorSystemId（或 indicatorSystemId），则合并更新已有记录；否则新增一条草稿。
     *
     * @param payload  解析后的 JSON 对象
     * @param operator 当前操作人
     * @return 保存后的指标体系（含 id）
     */
    EvalIndicatorSystem receiveRefinedFromRequirementPayload(JSONObject payload, String operator);
}
