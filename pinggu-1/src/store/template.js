import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useTemplateStore = defineStore('template', () => {
    const templates = ref([
        {
            id: 1,
            name: '高空无人机图像识别效能评估模板',
            description: '适用于各类可见光/红外侦察载荷的图像识别能力评估。',
            defaultFields: ['taskName', 'time', 'uavType'],
            collectionDef: [{ name: '识别置信度', type: 'float' }]
        },
        {
            id: 2,
            name: '航侦协同搜索广域布势模板',
            description: '针对多机协同搜索任务的布势合理性与覆盖率评估。',
            defaultFields: ['taskName', 'searchArea'],
            collectionDef: []
        }
    ])

    const saveTemplate = (template) => {
        if (!template.id) {
            template.id = Date.now()
            templates.value.push(template)
        } else {
            const idx = templates.value.findIndex(t => t.id === template.id)
            if (idx !== -1) templates.value[idx] = template
        }
    }

    return {
        templates,
        saveTemplate
    }
})
