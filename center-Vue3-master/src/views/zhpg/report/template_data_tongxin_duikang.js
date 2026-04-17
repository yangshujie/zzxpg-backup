/**
 * 通信对抗场景评估报告模板数据
 * 基于指标体系：干扰效果、响应速度与时序、跳频对抗能力、计划符合性、环境适应与一致性
 */

// 模板元数据
export const templateMeta = {
  templateName: '通信对抗试验评估报告模板',
  templateCode: 'TPL_TXDZ_001',
  templateDescription: '适用于通信对抗试验场景的综合评估报告模板，涵盖干扰效果、响应速度、跳频对抗能力、计划符合性及环境适应性等维度的评估指标',
  templateType: '对抗效能评估',
  evaluationType: '通信对抗试验',
  targetObject: '通信对抗系统',
  status: 'DRAFT',
  documentType: '综合评估报告',
}

// 模板HTML内容
export const templateHtmlContent = `<html><body>
<!-- 封面 -->
<div class="cover">
  <h1 style="text-align:center;">{{ReportTitle}}</h1>
  <h2 style="text-align:center;">{{SubTitle}}</h2>
  <div class="cover-info">
    <p>试验名称：{{ExperimentName}}</p>
    <p>试验时间：{{ExperimentTime}}</p>
    <p>评估对象：{{EvalTarget}}</p>
    <p>报告生成时间：{{ReportGenerateTime}}</p>
  </div>
</div>

<!-- 第一章：试验概述 -->
<h1>一、试验概述</h1>
<h2>1.1 试验目的</h2>
<p>{{TrialPurpose}}</p>

<h2>1.2 试验背景</h2>
<p>{{TrialBackground}}</p>

<h2>1.3 试验配置</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>配置项</th><th>参数值</th></tr>
  <tr><td>干扰装备型号</td><td>{{JammerModel}}</td></tr>
  <tr><td>靶星地面系统</td><td>{{TargetSystem}}</td></tr>
  <tr><td>试验频段</td><td>{{FrequencyBand}}</td></tr>
  <tr><td>试验时窗</td><td>{{TrialTimeWindow}}</td></tr>
</table>

<!-- 第二章：干扰效果评估 -->
<h1>二、干扰效果评估</h1>

<h2>2.1 通信中断评估</h2>
<h3>2.1.1 时间占空比</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>时间占空比</td><td>{{TimeOccupancyRatio_Value}}%</td><td>{{TimeOccupancyRatio_Threshold}}%</td><td>{{TimeOccupancyRatio_Result}}</td></tr>
</table>
<p>{{TimeOccupancyRatio_Analysis}}</p>

<h3>2.1.2 最大连续中断时长</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>最大连续中断时长</td><td>{{MaxInterruptDuration_Value}}s</td><td>{{MaxInterruptDuration_Threshold}}s</td><td>{{MaxInterruptDuration_Result}}</td></tr>
</table>
<p>{{MaxInterruptDuration_Analysis}}</p>

<h2>2.2 链路质量劣化评估</h2>
<h3>2.2.1 C/N0劣化量</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>C/N0劣化量</td><td>{{CN0Degradation_Value}}dB-Hz</td><td>{{CN0Degradation_Threshold}}dB-Hz</td><td>{{CN0Degradation_Result}}</td></tr>
</table>
<p>{{CN0Degradation_Analysis}}</p>

<h3>2.2.2 BER恶化幅度</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>BER恶化幅度</td><td>{{BERDegradation_Value}}</td><td>{{BERDegradation_Threshold}}</td><td>{{BERDegradation_Result}}</td></tr>
</table>
<p>{{BERDegradation_Analysis}}</p>

<h3>2.2.3 帧同步丢失率</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>帧同步丢失率</td><td>{{FrameSyncLossRate_Value}}%</td><td>{{FrameSyncLossRate_Threshold}}%</td><td>{{FrameSyncLossRate_Result}}</td></tr>
</table>
<p>{{FrameSyncLossRate_Analysis}}</p>

<h2>2.3 干扰建立时延</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>干扰建立有效时延</td><td>{{JammingEstablishDelay_Value}}s</td><td>{{JammingEstablishDelay_Threshold}}s</td><td>{{JammingEstablishDelay_Result}}</td></tr>
</table>
<p>{{JammingEstablishDelay_Analysis}}</p>

<h2>2.4 功率效能评估</h2>
<h3>2.4.1 干信比</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>干信比</td><td>{{JammingSignalRatio_Value}}dB</td><td>{{JammingSignalRatio_Threshold}}dB</td><td>{{JammingSignalRatio_Result}}</td></tr>
</table>

<h3>2.4.2 等效全向辐射功率</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>等效全向辐射功率</td><td>{{EIRP_Value}}dBW</td><td>{{EIRP_Threshold}}dBW</td><td>{{EIRP_Result}}</td></tr>
</table>

<h2>2.5 干扰效果综合评估</h2>
<blockquote style="padding:8px 12px;border-left:4px solid #5b8def;background:#f8fbff;">
  <p>干扰效果综合得分：{{JammingEffect_Score}}</p>
  <p>干扰效果评级：{{JammingEffect_Level}}</p>
  <p>干扰效果小结：{{JammingEffect_Summary}}</p>
</blockquote>

<!-- 第三章：响应速度与时序评估 -->
<h1>三、响应速度与时序评估</h1>

<h2>3.1 目指引导能力</h2>
<h3>3.1.1 目指接收时延</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>目指接收时延</td><td>{{TargetReceiveDelay_Value}}s</td><td>{{TargetReceiveDelay_Threshold}}s</td><td>{{TargetReceiveDelay_Result}}</td></tr>
</table>

<h3>3.1.2 干扰响应时延</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>干扰响应时延</td><td>{{JammingResponseDelay_Value}}s</td><td>{{JammingResponseDelay_Threshold}}s</td><td>{{JammingResponseDelay_Result}}</td></tr>
</table>

<h3>3.1.3 全链路端到端响应时延</h3>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>全链路端到端响应时延</td><td>{{EndToEndDelay_Value}}s</td><td>{{EndToEndDelay_Threshold}}s</td><td>{{EndToEndDelay_Result}}</td></tr>
</table>

<h2>3.2 响应速度综合评估</h2>
<blockquote style="padding:8px 12px;border-left:4px solid #5b8def;background:#f8fbff;">
  <p>响应速度综合得分：{{ResponseSpeed_Score}}</p>
  <p>响应速度评级：{{ResponseSpeed_Level}}</p>
  <p>响应速度小结：{{ResponseSpeed_Summary}}</p>
</blockquote>

<!-- 第四章：跳频对抗能力评估 -->
<h1>四、跳频对抗能力评估</h1>

<h2>4.1 跟踪能力评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>跳频跟踪成功率</td><td>{{FreqHoppingTrackSuccess_Value}}%</td><td>{{FreqHoppingTrackSuccess_Threshold}}%</td><td>{{FreqHoppingTrackSuccess_Result}}</td></tr>
  <tr><td>首次跳频跟踪成功率</td><td>{{FirstTrackSuccess_Value}}%</td><td>{{FirstTrackSuccess_Threshold}}%</td><td>{{FirstTrackSuccess_Result}}</td></tr>
</table>

<h2>4.2 再侦收能力评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>再侦收时延</td><td>{{ReacquisitionDelay_Value}}s</td><td>{{ReacquisitionDelay_Threshold}}s</td><td>{{ReacquisitionDelay_Result}}</td></tr>
  <tr><td>频率捕获准确率</td><td>{{FreqCaptureAccuracy_Value}}%</td><td>{{FreqCaptureAccuracy_Threshold}}%</td><td>{{FreqCaptureAccuracy_Result}}</td></tr>
</table>

<h2>4.3 频率覆盖评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>频率覆盖匹配度</td><td>{{FreqCoverageMatch_Value}}%</td><td>{{FreqCoverageMatch_Threshold}}%</td><td>{{FreqCoverageMatch_Result}}</td></tr>
  <tr><td>频率切换速度</td><td>{{FreqSwitchSpeed_Value}}ms</td><td>{{FreqSwitchSpeed_Threshold}}ms</td><td>{{FreqSwitchSpeed_Result}}</td></tr>
</table>

<h2>4.4 跳频后压制能力评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>再干扰建立时延</td><td>{{RejammingDelay_Value}}s</td><td>{{RejammingDelay_Threshold}}s</td><td>{{RejammingDelay_Result}}</td></tr>
  <tr><td>跳频后通信恢复窗口期</td><td>{{CommRecoveryWindow_Value}}s</td><td>{{CommRecoveryWindow_Threshold}}s</td><td>{{CommRecoveryWindow_Result}}</td></tr>
</table>

<h2>4.5 多次跳频稳定性评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>连续跳频跟踪稳定性</td><td>{{MultiHopStability_Value}}</td><td>{{MultiHopStability_Threshold}}</td><td>{{MultiHopStability_Result}}</td></tr>
</table>

<h2>4.6 跳频对抗综合评估</h2>
<blockquote style="padding:8px 12px;border-left:4px solid #5b8def;background:#f8fbff;">
  <p>跳频对抗综合得分：{{FreqHopping_Score}}</p>
  <p>跳频对抗评级：{{FreqHopping_Level}}</p>
  <p>跳频对抗小结：{{FreqHopping_Summary}}</p>
</blockquote>

<!-- 第五章：计划符合性评估 -->
<h1>五、计划符合性评估</h1>

<h2>5.1 任务执行评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>任务按时开展率</td><td>{{TaskOnTimeRate_Value}}%</td><td>{{TaskOnTimeRate_Threshold}}%</td><td>{{TaskOnTimeRate_Result}}</td></tr>
  <tr><td>任务完成率</td><td>{{TaskCompletionRate_Value}}%</td><td>{{TaskCompletionRate_Threshold}}%</td><td>{{TaskCompletionRate_Result}}</td></tr>
</table>

<h2>5.2 时机偏差评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>干扰时机偏差</td><td>{{JammingTimingDeviation_Value}}s</td><td>{{JammingTimingDeviation_Threshold}}s</td><td>{{JammingTimingDeviation_Result}}</td></tr>
</table>

<h2>5.3 效果达标评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>效果达标率</td><td>{{EffectComplianceRate_Value}}%</td><td>{{EffectComplianceRate_Threshold}}%</td><td>{{EffectComplianceRate_Result}}</td></tr>
  <tr><td>指标达标率</td><td>{{IndicatorComplianceRate_Value}}%</td><td>{{IndicatorComplianceRate_Threshold}}%</td><td>{{IndicatorComplianceRate_Result}}</td></tr>
  <tr><td>综合试验任务完成评级</td><td>{{OverallRating_Value}}</td><td>{{OverallRating_Threshold}}</td><td>{{OverallRating_Result}}</td></tr>
</table>

<h2>5.4 计划符合性综合评估</h2>
<blockquote style="padding:8px 12px;border-left:4px solid #5b8def;background:#f8fbff;">
  <p>计划符合性综合得分：{{PlanCompliance_Score}}</p>
  <p>计划符合性评级：{{PlanCompliance_Level}}</p>
  <p>计划符合性小结：{{PlanCompliance_Summary}}</p>
</blockquote>

<!-- 第六章：环境适应与一致性评估 -->
<h1>六、环境适应与一致性评估</h1>

<h2>6.1 环境适应评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>不同电磁环境中断率方差</td><td>{{EnvInterruptVariance_Value}}</td><td>{{EnvInterruptVariance_Threshold}}</td><td>{{EnvInterruptVariance_Result}}</td></tr>
  <tr><td>干扰效能-环境相关系数</td><td>{{InterferenceEnvCorrelation_Value}}</td><td>{{InterferenceEnvCorrelation_Threshold}}</td><td>{{InterferenceEnvCorrelation_Result}}</td></tr>
</table>

<h2>6.2 模式对比评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>普通模式vs抗干扰模式干扰成功率差异</td><td>{{ModeDifference_Value}}%</td><td>{{ModeDifference_Threshold}}%</td><td>{{ModeDifference_Result}}</td></tr>
</table>

<h2>6.3 一致性评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>多轮次效能均值与极差</td><td>{{MultiRunEfficiencyRange_Value}}</td><td>{{MultiRunEfficiencyRange_Threshold}}</td><td>{{MultiRunEfficiencyRange_Result}}</td></tr>
  <tr><td>效能标准差</td><td>{{EfficiencyStdDev_Value}}</td><td>{{EfficiencyStdDev_Threshold}}</td><td>{{EfficiencyStdDev_Result}}</td></tr>
</table>

<h2>6.4 环境影响因素评估</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>指标名称</th><th>实测值</th><th>达标阈值</th><th>评估结果</th></tr>
  <tr><td>引导精度-时延相关系数</td><td>{{GuidanceDelayCorrelation_Value}}</td><td>{{GuidanceDelayCorrelation_Threshold}}</td><td>{{GuidanceDelayCorrelation_Result}}</td></tr>
</table>

<h2>6.5 环境适应综合评估</h2>
<blockquote style="padding:8px 12px;border-left:4px solid #5b8def;background:#f8fbff;">
  <p>环境适应综合得分：{{EnvAdaptability_Score}}</p>
  <p>环境适应评级：{{EnvAdaptability_Level}}</p>
  <p>环境适应小结：{{EnvAdaptability_Summary}}</p>
</blockquote>

<!-- 第七章：综合评估结论 -->
<h1>七、综合评估结论</h1>

<h2>7.1 评估指标总览</h2>
<table border="1" cellspacing="0" cellpadding="6" style="border-collapse:collapse;width:100%;">
  <tr><th>评估维度</th><th>综合得分</th><th>评级</th><th>达标情况</th></tr>
  <tr><td>干扰效果</td><td>{{JammingEffect_Score}}</td><td>{{JammingEffect_Level}}</td><td>{{JammingEffect_Compliance}}</td></tr>
  <tr><td>响应速度与时序</td><td>{{ResponseSpeed_Score}}</td><td>{{ResponseSpeed_Level}}</td><td>{{ResponseSpeed_Compliance}}</td></tr>
  <tr><td>跳频对抗能力</td><td>{{FreqHopping_Score}}</td><td>{{FreqHopping_Level}}</td><td>{{FreqHopping_Compliance}}</td></tr>
  <tr><td>计划符合性</td><td>{{PlanCompliance_Score}}</td><td>{{PlanCompliance_Level}}</td><td>{{PlanCompliance_Compliance}}</td></tr>
  <tr><td>环境适应与一致性</td><td>{{EnvAdaptability_Score}}</td><td>{{EnvAdaptability_Level}}</td><td>{{EnvAdaptability_Compliance}}</td></tr>
</table>

<h2>7.2 综合评级</h2>
<blockquote style="padding:12px 16px;border-left:6px solid #52c41a;background:#f6ffed;font-size:16px;">
  <p style="font-size:18px;font-weight:bold;">综合评级：{{OverallGrade}}</p>
  <p>综合得分：{{OverallScore}}</p>
  <p>达标指标数：{{ComplianceCount}} / {{TotalIndicatorCount}}</p>
  <p>达标率：{{ComplianceRate}}%</p>
</blockquote>

<h2>7.3 评估结论</h2>
<p>{{OverallConclusion}}</p>

<h2>7.4 问题与建议</h2>
<h3>7.4.1 发现的问题</h3>
<p>{{DiscoveredProblems}}</p>

<h3>7.4.2 改进建议</h3>
<p>{{ImprovementSuggestions}}</p>

<h2>7.5 附件</h2>
<ul>
  <li>附件1：原始试验数据</li>
  <li>附件2：指标计算详情</li>
  <li>附件3：现场照片</li>
</ul>

<div class="footer">
  <p style="text-align:center;">报告编制：{{ReportAuthor}}</p>
  <p style="text-align:center;">审核：{{Reviewer}}</p>
  <p style="text-align:center;">批准：{{Approver}}</p>
</div>
</body></html>`;

// 模板数据结构（用于API调用）
export const templateData = {
  ...templateMeta,
  htmlContent: templateHtmlContent,
  dslJson: '',
  changeLog: '初始化通信对抗评估报告模板',
};

// 指标清单（用于参考）
export const indicatorList = {
  一级指标: [
    {
      名称: '干扰效果',
      二级指标: [
        {
          名称: '通信中断',
          底层指标: [
            { 名称: '时间占空比', 单位: '%', 类型: '效益型', 范围: [0, 100] },
            { 名称: '最大连续中断时长', 单位: 's', 类型: '效益型', 范围: [0, 300] },
          ],
        },
        {
          名称: '链路质量劣化程度',
          底层指标: [
            { 名称: 'C/N0劣化量', 单位: 'dB-Hz', 类型: '效益型', 范围: [0, 40] },
            { 名称: 'BER恶化幅度', 单位: '', 类型: '效益型', 范围: [0, 0.1] },
            { 名称: '帧同步丢失率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
          ],
        },
        {
          名称: '干扰建立有效时延',
          底层指标: [
            { 名称: '干扰建立有效时延', 单位: 's', 类型: '成本型', 范围: [0, 30] },
          ],
        },
        {
          名称: '功率效能',
          底层指标: [
            { 名称: '干信比', 单位: 'dB', 类型: '效益型', 范围: [0, 50] },
            { 名称: '等效全向辐射功率', 单位: 'dBW', 类型: '效益型', 范围: [30, 100] },
          ],
        },
      ],
    },
    {
      名称: '响应速度与时序',
      二级指标: [
        {
          名称: '干扰响应能力',
          底层指标: [
            { 名称: '干扰响应时延', 单位: 's', 类型: '成本型', 范围: [0, 15] },
            { 名称: '全链路端到端响应时延', 单位: 's', 类型: '成本型', 范围: [0, 30] },
          ],
        },
        {
          名称: '对抗反应能力',
          底层指标: [
            { 名称: '干扰空窗期', 单位: 's', 类型: '成本型', 范围: [0, 30] },
            { 名称: '跳频响应时延', 单位: 's', 类型: '成本型', 范围: [0, 30] },
          ],
        },
      ],
    },
    {
      名称: '跳频对抗能力',
      二级指标: [
        {
          名称: '跟踪能力',
          底层指标: [
            { 名称: '跳频跟踪成功率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
            { 名称: '首次跳频跟踪成功率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
          ],
        },
        {
          名称: '再侦收能力',
          底层指标: [
            { 名称: '再侦收时延', 单位: 's', 类型: '成本型', 范围: [0, 15] },
            { 名称: '频率捕获准确率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
          ],
        },
        {
          名称: '频率覆盖',
          底层指标: [
            { 名称: '频率覆盖匹配度', 单位: '%', 类型: '效益型', 范围: [0, 100] },
            { 名称: '频率切换速度', 单位: 'ms', 类型: '成本型', 范围: [0, 5000] },
          ],
        },
        {
          名称: '跳频后压制能力',
          底层指标: [
            { 名称: '再干扰建立时延', 单位: 's', 类型: '成本型', 范围: [0, 30] },
            { 名称: '跳频后通信恢复窗口期', 单位: 's', 类型: '成本型', 范围: [0, 60] },
          ],
        },
        {
          名称: '多次跳频',
          底层指标: [
            { 名称: '连续跳频跟踪稳定性', 单位: '', 类型: '成本型', 范围: [0, 1] },
          ],
        },
      ],
    },
    {
      名称: '计划符合性',
      二级指标: [
        {
          名称: '任务执行',
          底层指标: [
            { 名称: '任务按时开展率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
            { 名称: '任务完成率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
          ],
        },
        {
          名称: '时机偏差',
          底层指标: [
            { 名称: '干扰时机偏差', 单位: 's', 类型: '成本型', 范围: [0, 30] },
          ],
        },
        {
          名称: '效果达标',
          底层指标: [
            { 名称: '效果达标率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
            { 名称: '指标达标率', 单位: '%', 类型: '效益型', 范围: [0, 100] },
            { 名称: '综合试验任务完成评级', 单位: '', 类型: '效益型', 范围: [0, 5] },
          ],
        },
      ],
    },
    {
      名称: '环境适应与一致性',
      二级指标: [
        {
          名称: '环境适应',
          底层指标: [
            { 名称: '不同电磁环境中断率方差', 单位: '', 类型: '成本型', 范围: [0, 1] },
            { 名称: '干扰效能-环境相关系数', 单位: '', 类型: '成本型', 范围: [-1, 1] },
          ],
        },
        {
          名称: '模式对比',
          底层指标: [
            { 名称: '普通模式vs抗干扰模式干扰成功率差异', 单位: '%', 类型: '效益型', 范围: [0, 100] },
          ],
        },
        {
          名称: '一致性',
          底层指标: [
            { 名称: '多轮次效能均值与极差', 单位: '', 类型: '成本型', 范围: [0, 100] },
            { 名称: '效能标准差', 单位: '', 类型: '成本型', 范围: [0, 50] },
          ],
        },
        {
          名称: '影响因素',
          底层指标: [
            { 名称: '引导精度-时延相关系数', 单位: '', 类型: '成本型', 范围: [-1, 1] },
          ],
        },
      ],
    },
  ],
};

export default {
  templateMeta,
  templateHtmlContent,
  templateData,
  indicatorList,
};
