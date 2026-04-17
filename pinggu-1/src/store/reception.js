import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useReceptionStore = defineStore('reception', () => {
    // Mock Data for Requirements
    const requirements = ref([
        {
            id: 1,
            title: '某型号高空无人机图像识别效能评估',
            source: '外部系统',
            status: 'pending',
            tags: ['航侦', '图像识别'],
            receiveTime: '2026-05-10 10:00:00',
            description: '对某型号高空无人机在复杂气象环境下的图像识别能力进行评估。',
            customFields: {
                'priority': 'High',
                'location': '西北测试场'
            }
        },
        {
            id: 2,
            title: '航侦集群协同搜索任务评估',
            source: '人工上传',
            status: 'accepted',
            tags: ['协同', '搜索'],
            receiveTime: '2026-05-11 14:30:00',
            description: '评估航侦集群在特定海域场景下的协同搜索效率。',
            customFields: {}
        }
    ])

    // Mock Data for Available Tags
    const availableTags = ref(['航侦', '通信', '协同', '图像识别', '定位', '效能', '可靠性'])

    // Mock Data for Custom Fields Definition
    // Types: text, number, select, date, boolean
    const fieldDefinitions = ref([
        { id: 'f1', key: 'priority', label: '优先级', type: 'select', options: ['Low', 'Medium', 'High'], enabled: true },
        { id: 'f2', key: 'location', label: '测试地点', type: 'text', enabled: true },
        { id: 'f3', key: 'budget', label: '预算(万)', type: 'number', enabled: false }
    ])

    const addRequirement = (req) => {
        requirements.value.push({
            id: Date.now(),
            status: 'pending',
            receiveTime: new Date().toLocaleString(),
            ...req
        })
    }

    const addTag = (tag) => {
        if (!availableTags.value.includes(tag)) {
            availableTags.value.push(tag)
        }
    }

    const updateFieldDefinition = (field) => {
        const index = fieldDefinitions.value.findIndex(f => f.id === field.id)
        if (index !== -1) {
            fieldDefinitions.value[index] = field
        } else {
            fieldDefinitions.value.push(field)
        }
    }

    return {
        requirements,
        availableTags,
        fieldDefinitions,
        addRequirement,
        addTag,
        updateFieldDefinition
    }
})
