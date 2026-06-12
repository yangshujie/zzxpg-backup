-- ============================================================
-- 评估报告实例进度字段迁移
-- 方向四：报告生成进度可见 + 异步化
-- ============================================================

-- render_progress: 生成进度阶段
--   PENDING           - 等待生成
--   HTML_RENDERING    - 正在渲染模板
--   CHART_GENERATING  - 正在生成图表
--   DOCX_CONVERTING   - 正在转换为 DOCX
--   PDF_CONVERTING    - 正在转换为 PDF
--   UPLOADING         - 正在上传文件
--   DONE              - 生成完成
--   FAILED            - 生成失败（与 render_status=FAILED 一致）
ALTER TABLE pgzc_eval_report_instance ADD COLUMN IF NOT EXISTS render_progress VARCHAR(50)
    DEFAULT 'PENDING' NOT NULL
    COMMENT '生成进度: PENDING/HTML_RENDERING/CHART_GENERATING/DOCX_CONVERTING/PDF_CONVERTING/UPLOADING/DONE/FAILED';

-- render_progress_detail: 进度详情文本（给前端展示）
ALTER TABLE pgzc_eval_report_instance ADD COLUMN IF NOT EXISTS render_progress_detail VARCHAR(500)
    COMMENT '进度详情，如"正在生成雷达图(3/5)"';

-- render_progress_detail 的默认值由代码维护，不设 DDL 默认值
