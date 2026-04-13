# 综合计算任务执行重构说明

## 重构目标

将 `/calc/run` 接口从**同步阻塞执行**改为**异步非阻塞执行**，并使用标准的 XXL-JOB 回调机制。

## 主要改动

### 1. ZHPG 模块（调用方）

#### 1.1 接口变更

**原接口（同步）：**
```java
POST /zhpg/calc/run
// 返回：CalcTask（执行完成后的完整结果）
// 耗时：等待全部4个阶段执行完成
```

**新接口（异步）：**
```java
POST /zhpg/calc/run
// 返回：CalcTask（仅包含taskId和PENDING状态）
// 耗时：立即返回（约100ms）

GET /zhpg/calc/task/{id}/status
// 返回：CalcTaskAsyncResult（执行状态，供前端轮询）
```

**新增回调接口（内部使用）：**
```java
POST /zhpg/calc/callback/success/{taskId}  // XXL-JOB执行成功回调
POST /zhpg/calc/callback/fail/{taskId}     // XXL-JOB执行失败回调
```

#### 1.2 执行流程变更

**原流程：**
1. /calc/run 接收请求
2. 执行4个阶段（SCHEDULE_CONFIG → WEIGHT_CALC → COMPREHENSIVE_CALC → REPORT_OUTPUT）
3. 同步等待XXL-JOB执行完成（轮询数据库）
4. 返回完整结果

**新流程：**
1. /calc/run 接收请求
2. 创建任务记录（状态 PENDING）
3. 预处理权重阶段（在提交前完成）
4. 触发 XXL-JOB（状态变为 DISPATCHED）
5. 立即返回 taskId
6. XXL-JOB 执行器执行综合计算
7. 执行器回调 ZHPG 更新任务状态（SUCCESS/FAILED）
8. 前端轮询 /calc/task/{id}/status 查询状态

### 2. ZhpgCalcExecutor 模块（执行方）

#### 2.1 回调机制变更

**原方式：**
- 执行器直接写业务数据库（`pgzc_calc_task`表）
- ZHPG 轮询数据库等待结果

**新方式：**
- 执行器通过 HTTP 回调 ZHPG 接口
- ZHPG 接收回调后更新任务状态

#### 2.2 新增组件

- `ZhpgCallbackClient`：回调 ZHPG 服务的客户端

#### 2.3 保留组件（兼容）

- `CalcTaskAsyncResultWriter`：原数据库回写方式（保留但不使用）

## Nacos 配置

### ZHPG 模块（已有的配置）

```yaml
# XXL-JOB Admin 地址
zhpg:
  calc:
    xxl-job:
      admin-base-url: http://127.0.0.1:9900
```

### ZhpgCalcExecutor 模块（新增配置）

```yaml
# ZHPG 服务回调地址（用于执行完成后通知ZHPG）
zhpg:
  callback:
    url: http://localhost:9303  # 或 http://zhpg（Nacos服务名）
```

**配置建议：**
- 开发环境：`http://localhost:9303`
- 测试/生产环境：`http://zhpg`（使用 Nacos 服务发现）

## 任务状态流转

```
PENDING（待执行）
    ↓
DISPATCHED（已分发到XXL-JOB）
    ↓
RUNNING（执行器正在执行）← 可选，取决于执行器是否回调中间状态
    ↓
SUCCESS / FAILED（执行完成）
```

## API 使用示例

### 1. 发起计算任务

```bash
POST /dev-api/zhpg/calc/run
Content-Type: application/json

{
  "calcFlowTemplateId": 1,
  "indicatorSystemId": 1,
  "taskName": "测试任务"
}

# 响应：
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 123,
    "taskName": "测试任务-1699999999999",
    "runStatus": "PENDING",
    "currentStage": "SCHEDULE_CONFIG",
    "progressPercent": 0,
    "createTime": "2024-01-01T10:00:00"
  }
}
```

### 2. 查询任务状态（轮询）

```bash
GET /dev-api/zhpg/calc/task/123/status

# 响应（执行中）：
{
  "code": 200,
  "data": {
    "taskId": 123,
    "status": "DISPATCHED",
    "currentStage": "SCHEDULE_CONFIG",
    "progressPercent": 0,
    "createTime": "2024-01-01T10:00:00",
    "startTime": "2024-01-01T10:00:01"
  }
}

# 响应（执行完成）：
{
  "code": 200,
  "data": {
    "taskId": 123,
    "status": "SUCCESS",
    "currentStage": "FINISHED",
    "progressPercent": 100,
    "result": {
      "score": 85.5,
      "grade": "良好",
      "conclusion": "综合评价结论..."
    },
    "createTime": "2024-01-01T10:00:00",
    "startTime": "2024-01-01T10:00:01",
    "endTime": "2024-01-01T10:00:05"
  }
}
```

## 前端适配建议

1. **调用 /calc/run 后：**
   - 显示"任务已提交，正在执行..."
   - 保存返回的 taskId

2. **轮询查询状态：**
   - 每 2-3 秒调用一次 /calc/task/{id}/status
   - 根据 status 更新进度条和状态文字

3. **状态映射：**
   - PENDING → "等待执行"
   - DISPATCHED → "已分发"
   - RUNNING → "执行中"
   - SUCCESS → "执行成功"
   - FAILED → "执行失败"

4. **执行完成后：**
   - 可调用 /calc/task/{id} 获取完整详情（包含阶段日志）

## 测试步骤

1. **确保服务启动：**
   - Nacos
   - XXL-JOB Admin（port 9900）
   - ZHPG 模块（port 9303）
   - ZhpgCalcExecutor（port 9305）

2. **配置检查：**
   - ZHPG 的 `zhpg.calc.xxl-job.admin-base-url` 指向 XXL-JOB Admin
   - ZhpgCalcExecutor 的 `zhpg.callback.url` 指向 ZHPG 服务

3. **发起测试请求：**
   ```bash
   curl -X POST http://localhost:8080/zhpg/calc/run \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer ${TOKEN}" \
     -d '{
       "calcFlowTemplateId": 1,
       "indicatorSystemId": 1,
       "taskName": "异步测试任务"
     }'
   ```

4. **验证：**
   - 立即返回 taskId
   - XXL-JOB Admin 有调度日志
   - 执行日志中有详细执行过程
   - 回调后任务状态更新为 SUCCESS/FAILED

## 回滚方案

如需回滚到同步模式：
1. 恢复 `CalcTaskServiceImpl.java` 为原版本（备份文件在 `.bak`）
2. 恢复 `ZhpgCalcJobService.java` 为原版本

## 注意事项

1. **回调地址必须可访问：** 执行器需要能通过网络访问 ZHPG 的回调接口
2. **权重计算前置：** 现在权重计算在 /calc/run 阶段完成，不占用 XXL-JOB 执行时间
3. **报告生成简化：** 当前版本在回调时生成评估结果记录，如需完整报告生成，可在后续迭代中扩展
