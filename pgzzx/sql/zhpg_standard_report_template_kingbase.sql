-- 内置标准模板：通信对抗试验评估报告（标准版）
-- 对齐当前 ReportTemplate.java：template_name/template_description/evaluation_type/html_content
ALTER TABLE pgzc_report_template ADD COLUMN IF NOT EXISTS html_content TEXT;

WITH standard_template AS (
    SELECT
        '通信对抗试验评估报告（标准版）' AS template_name,
        '内置标准模板，采用标题、段落、图片、表格等基础块自由组合，仅保留指标章节循环块作为自动块。' AS template_description,
        'EQUIP_EFFECTIVENESS' AS evaluation_type,
        $$
<div data-component-type="title" data-component-label="主标题"><h1 style="text-align:center;">{{ReportTitle}}</h1></div>
<div data-component-type="subtitle" data-component-label="副标题"><h2 style="text-align:center;">{{SubTitle}}</h2></div>
<div data-component-type="h1" data-component-label="一级标题"><h2>一、试验概况</h2></div>
<div data-component-type="paragraph" data-component-label="正文"><p>{{ExperimentOverview}}</p></div>
<div data-component-type="h1" data-component-label="一级标题"><h2>二、综合评估</h2></div>
<div data-component-type="paragraph" data-component-label="正文"><p>本次评估综合得分为 {{score}}，评估等级为 {{grade}}。</p></div>
<div data-component-type="imageRadar" data-component-label="能力雷达图" data-image-type="capability_radar_chart" data-image-width="80" data-image-height="160" data-image-align="center"><p style="text-align:center;"><img src="{{CapabilityRadarChart}}" style="width:80%;max-width:100%;height:160px;object-fit:contain;" alt="能力雷达图" /></p></div>
<div data-component-type="dataTable" data-component-label="数据表格" data-table-type="indicator_summary_table"><div class="report-data-table-placeholder">{{CapabilityScoreTable}}</div></div>
<div data-component-type="paragraph" data-component-label="正文"><p>{{OverallConclusionParagraph}}</p></div>
<div data-component-type="h1" data-component-label="一级标题"><h2>三、指标分项评估</h2></div>
<div data-component-type="indicatorSections" data-component-label="指标章节循环块">{{#each IndicatorSections}}<section><h{{=item.level}}>{{item.numbering}} {{item.title}}</h{{=item.level}}>{{#if item.summary}}<p>{{item.summary}}</p>{{/if}}{{#if item.evalTable}}<div>{{item.evalTable}}</div>{{/if}}{{#if item.chartImg}}<p style="text-align:center;"><img src="{{=item.chartImg}}" style="max-width:80%;" alt="{{item.title}}图表" /></p>{{/if}}</section>{{/each}}</div>
<div data-component-type="h1" data-component-label="一级标题"><h2>四、结论与建议</h2></div>
<div data-component-type="h2" data-component-label="二级标题"><h3>关键发现</h3></div>
<div data-component-type="paragraph" data-component-label="正文"><p>{{KeyFindings}}</p></div>
<div data-component-type="h2" data-component-label="二级标题"><h3>改进建议</h3></div>
<div data-component-type="paragraph" data-component-label="正文"><p>{{ImprovementSuggestions}}</p></div>
<div data-component-type="h2" data-component-label="二级标题"><h3>最终结论</h3></div>
<div data-component-type="paragraph" data-component-label="正文"><p>{{FinalConclusion}}</p></div>
$$ AS html_content
),
updated AS (
    UPDATE pgzc_report_template t
    SET template_description = s.template_description,
        evaluation_type = s.evaluation_type,
        html_content = s.html_content,
        update_by = 'system',
        update_time = CURRENT_TIMESTAMP
    FROM standard_template s
    WHERE t.template_name = s.template_name
    RETURNING t.id
)
INSERT INTO pgzc_report_template (
    template_name,
    template_description,
    evaluation_type,
    html_content,
    create_by,
    update_by,
    create_time,
    update_time
)
SELECT
    template_name,
    template_description,
    evaluation_type,
    html_content,
    'system',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM standard_template
WHERE NOT EXISTS (SELECT 1 FROM updated);
