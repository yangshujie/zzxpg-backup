import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDesignStore = defineStore('design', () => {
    // 1. Task Info Module Data
    const taskTemplates = ref([
        { type: 'recon_uav', name: '高空无人侦察机效能评估模板', fields: ['巡航高度', '传感器分辨率', '下行链路速率'] },
        { type: 'recon_swarm', name: '航侦协同搜索集群模板', fields: ['节点数量', '协同覆盖率', '情报时效性'] }
    ])

    // 2. Indicator Module Data (Existing + New)
    const indicatorTree = ref([
        {
            id: 1,
            label: '航侦任务综合效能评估体系',
            children: [
                {
                    id: 11,
                    label: '侦察监视能力',
                    weight: 0.4,
                    children: [
                        { id: 111, label: '最大可见光分辨率', weight: 0.5, desc: '传感器在标准高度下的地理分辨率' },
                        { id: 112, label: '目标获取时延', weight: 0.3, desc: '从发现目标到完成识别的时间间隔' }
                    ]
                },
                {
                    id: 12,
                    label: '生存与抗对抗能力',
                    weight: 0.3,
                    children: [
                        { id: 121, label: '电子干扰防护增益', weight: 1.0, desc: '在复杂电磁环境下的数据链稳定性' }
                    ]
                }
            ]
        }
    ])

    // 3. Model Design Module Data
    const evaluationModels = ref([
        { id: 'm1', name: '模糊综合评判法', type: 'Fuzzy', category: 'Universal', fields: ['隶属度函数', '算子类型'] },
        { id: 'm2', name: 'ADC模型', type: 'ADC', category: 'System', fields: ['可用性(A)', '可靠性(D)', '能力(C)'] },
        { id: 'm3', name: '聚类分析法', type: 'Cluster', category: 'Statistical', fields: ['距离测度', '聚类中心数'] },
        { id: 'm4', name: '层次分析法(AHP)', type: 'AHP', category: 'Weighting', fields: ['判断矩阵', '一致性检验'] },
        { id: 'm5', name: '灰色关联分析', type: 'Grey', category: 'Analysis', fields: [] },
        { id: 'm6', name: '神经网络评估', type: 'NN', category: 'AI', fields: [] },
        { id: 'm7', name: '专家打分法', type: 'Expert', category: 'Subjective', fields: [] },
        { id: 'm8', name: '主成分分析', type: 'PCA', category: 'Statistical', fields: [] }
    ])

    // 4. Criteria Design Module Data
    const criteriaList = ref([
        { id: 'c1', name: '百分制计分规则', type: 'Scoring', definition: { logic: 'linear', min: 0, max: 100 } },
        { id: 'c2', name: '二进制合格评定', type: 'Judgment', definition: { threshold: 60, trueVal: 'Pass', falseVal: 'Fail' } }
    ])

    // 5. Algorithm Module Data
    const algorithms = ref([
        { id: 'a1', name: '数据归一化插件', type: 'Preprocessing', status: 'Loaded' },
        { id: 'a2', name: '异常检测算法包', type: 'Detection', status: 'Draft' }
    ])

    // 6. Data Collection Design Data
    const collectionLists = ref([
        { id: 'cl1', task: '航侦测试-01', items: [{ param: 'SNR', freq: '10Hz', source: 'Logger' }] }
    ])

    // 7. Scheme Generation Data (Existing Schemes)
    const schemes = ref([
        { id: 1, name: '某型高空无人侦察机定型试验评估方案', type: '定型试验', status: '已归档' }
    ])

    // Actions
    const updateNode = (id, data) => {
        const findAndUpdate = (nodes) => {
            for (let node of nodes) {
                if (node.id === id) { Object.assign(node, data); return true }
                if (node.children && findAndUpdate(node.children)) return true
            }
            return false
        }
        findAndUpdate(indicatorTree.value)
    }

    const addNode = (parentId, newNode) => {
        const findAndAdd = (nodes) => {
            for (let node of nodes) {
                if (node.id === parentId) {
                    if (!node.children) node.children = []
                    node.children.push(newNode)
                    return true
                }
                if (node.children && findAndAdd(node.children)) return true
            }
            return false
        }
        findAndAdd(indicatorTree.value)
    }

    const removeNode = (id) => {
        const findAndRemove = (nodes) => {
            const idx = nodes.findIndex(n => n.id === id)
            if (idx !== -1) { nodes.splice(idx, 1); return true }
            for (let node of nodes) {
                if (node.children && findAndRemove(node.children)) return true
            }
            return false
        }
        findAndRemove(indicatorTree.value)
    }

    return {
        taskTemplates,
        indicatorTree,
        evaluationModels,
        criteriaList,
        algorithms,
        collectionLists,
        schemes,
        updateNode,
        addNode,
        removeNode
    }
})
