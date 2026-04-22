package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.CalcFlowExecution;
import com.ruoyi.domain.zhpg.CalcTask;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionInitRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionRunRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionSaveRequest;

public interface ICalcFlowExecutionService extends IService<CalcFlowExecution> {

    CalcFlowExecution initExecution(CalcFlowExecutionInitRequest request);

    CalcFlowExecution latest(Long templateId, Long indicatorSystemId);

    CalcFlowExecution saveRuntimeConfig(Long id, CalcFlowExecutionSaveRequest request);

    CalcTask runExecution(Long id, CalcFlowExecutionRunRequest request);
}
