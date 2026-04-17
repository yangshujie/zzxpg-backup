import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMonitorStore = defineStore('monitor', () => {
    // 1. Task Status & Resources (Existing + Enhanced)
    const activeTasks = ref([
        {
            id: 'T-20260301-01',
            name: '某型高空无人侦察机图像识别效能评估',
            status: 'running',
            progress: 45,
            priority: 'High',
            type: 'AR-UAV-01',
            startTime: '2026-03-01 09:00:00',
            indicators: '航侦图像目标识别指标体系(v1.2)',
            algorithm: '深度学习识别, 模糊综合评判',
            criteria: '百分制情报质量等级',
            operator: 'ReconCommand'
        },
        {
            id: 'T-20260302-03',
            name: '航侦协同搜索集群任务效能评估',
            status: 'pending',
            progress: 0,
            priority: 'Medium',
            type: 'Recon-Swarm',
            startTime: '-',
            indicators: '协同搜索效能指标体系',
            algorithm: '层次分析法(AHP)',
            criteria: '任务达成度判定',
            operator: 'TaskGroup-Beta'
        }
    ])

    const resources = ref([
        { id: 'C01', name: '航侦数据处理中心-01', type: 'compute', status: 'busy', usage: 85, task: 'T-20260301-01' },
        { id: 'C02', name: '航侦数据处理中心-02', type: 'compute', status: 'idle', usage: 12, task: '-' },
        { id: 'S01', name: '遥感影像存储阵列', type: 'storage', status: 'running', usage: 45, task: 'Shared' },
        { id: 'D01', name: '合成孔径雷达(SAR)载荷', type: 'device', status: 'maintenance', usage: 0, task: '-' }
    ])

    // 2. Process Flows
    const processFlows = ref([
        {
            id: 'PF01', name: '标准效能评估流程', steps: [
                { id: 1, name: '任务启动', status: 'done' },
                { id: 2, name: '数据接入', status: 'done' },
                { id: 3, name: '预处理', status: 'running' },
                { id: 4, name: '模型计算', status: 'pending' },
                { id: 5, name: '结果生成', status: 'pending' }
            ]
        }
    ])

    // 3. Anomaly Warning Config
    const alertConfigs = ref([
        { id: 1, type: 'Email', target: 'admin@evaluate.com', level: 'High', status: 'Active' },
        { id: 2, type: 'SMS', target: '13800138000', level: 'Critical', status: 'Active' }
    ])

    // 4. Intervention Logs
    const interventionLogs = ref([
        { id: 101, time: '2026-03-01 10:30', operator: 'Admin', action: 'Pause', reason: '数据源异常中断', result: 'Success' }
    ])

    // 5. Data Sharing Links
    const sharingLinks = ref([
        { id: 1, center: '战区数据中心', status: 'Connected', lastSync: '10:00:00', uploadRate: '50MB/s' },
        { id: 2, center: '装备研究院', status: 'Disconnected', lastSync: 'Yesterday', uploadRate: '0KB/s' }
    ])

    // Actions
    const updateTaskStatus = (id, status) => {
        const task = activeTasks.value.find(t => t.id === id)
        if (task) task.status = status
    }

    const addLog = (log) => {
        interventionLogs.value.unshift({
            id: Date.now(),
            time: new Date().toLocaleString(),
            ...log
        })
    }

    return {
        activeTasks,
        resources,
        processFlows,
        alertConfigs,
        interventionLogs,
        sharingLinks,
        updateTaskStatus,
        addLog
    }
})
