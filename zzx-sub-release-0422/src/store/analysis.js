import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAnalysisStore = defineStore('analysis', () => {
    // Mock analysis results for requirements
    const analysisResults = ref({})

    // Mock available data types for collection
    const dataTypes = ref([
        { label: '整型 (Integer)', value: 'int' },
        { label: '浮点型 (Float)', value: 'float' },
        { label: '布尔型 (Boolean)', value: 'bool' },
        { label: '字符串 (String)', value: 'string' },
        { label: '时间戳 (Timestamp)', value: 'timestamp' },
        { label: '图像 (Image)', value: 'image' }
    ])

    const getAnalysis = (reqId) => {
        if (!analysisResults.value[reqId]) {
            // Initialize empty analysis if not exists
            analysisResults.value[reqId] = {
                semantic: {
                    taskName: '',
                    content: '',
                    time: '',
                    object: '',
                    evaluationItems: [], // 评估项目
                    evaluationPoints: [] // 评估要点
                },
                dataCollection: [], // List of { name, type, unit, source }
                paramRules: [] // List of { param, formula, threshold, logic, exception }
            }
        }
        return analysisResults.value[reqId]
    }

    const saveAnalysis = (reqId, data) => {
        analysisResults.value[reqId] = data
    }

    // Mock Auto Analysis
    const autoAnalyzeSemantic = (reqId, text) => {
        // Return mock promise
        return new Promise((resolve) => {
            setTimeout(() => {
                const result = {
                    taskName: text.substring(0, 10) + '...',
                    content: '自动提取的任务内容摘要...',
                    time: '2025-Q3',
                    object: '某型高空无人侦察系统',
                    evaluationItems: ['地理定位精度', '目标识别率', '生存能力'],
                    evaluationPoints: ['像素偏差', '边缘锐度', '假目标剔除率']
                }
                // Update store
                const current = getAnalysis(reqId)
                current.semantic = result
                saveAnalysis(reqId, current)
                resolve(result)
            }, 1500)
        })
    }

    // Mock Auto Generate Data Collection
    const autoGenerateCollection = (reqId) => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const list = [
                    { name: '像素偏差', type: 'float', unit: 'px', source: '影像元数据' },
                    { name: '目标经纬度', type: 'float', unit: 'deg', source: '机载定位仪' },
                    { name: '云量占比', type: 'float', unit: '%', source: '气象传感器' },
                    { name: '识别置信度', type: 'float', unit: '-', source: 'AI推理载荷' }
                ]
                const current = getAnalysis(reqId)
                current.dataCollection = list
                saveAnalysis(reqId, current)
                resolve(list)
            }, 1500)
        })
    }

    // Mock Auto Generate Rules
    const autoGenerateRules = (reqId) => {
        return new Promise((resolve) => {
            setTimeout(() => {
                const list = [
                    { param: '识别置信度', formula: 'TargetConfidence / AvgNoise', threshold: '> 0.85', logic: 'And', exception: 'Null' },
                    { param: '定位误差', formula: 'Abs(CalculatedPos - TruthPos)', threshold: '< 5m', logic: 'Or', exception: 'ExtremeOutlier' }
                ]
                const current = getAnalysis(reqId)
                current.paramRules = list
                saveAnalysis(reqId, current)
                resolve(list)
            }, 1500)
        })
    }

    return {
        analysisResults,
        dataTypes,
        getAnalysis,
        saveAnalysis,
        autoAnalyzeSemantic,
        autoGenerateCollection,
        autoGenerateRules
    }
})
