import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useResourceStore = defineStore('resource', () => {
    // Categorized File Lists
    const categories = ref([
        { key: 'templates', label: 'Evaluation Templates' },
        { key: 'indicators', label: 'Indicator Systems' },
        { key: 'data_source', label: 'Data Sources' },
        { key: 'process_data', label: 'Process Data' },
        { key: 'schemes', label: 'Evaluation Schemes' },
        { key: 'reports', label: 'Evaluation Reports' },
        { key: 'results', label: 'Result Data' }
    ])

    const files = ref([
        { id: 1, name: 'Recon_UAV_Eval_Template_v1.json', category: 'templates', size: '25KB', type: 'JSON', updateTime: '2026-03-01', owner: 'admin' },
        { id: 2, name: 'SAR_Payload_Indicator_2026.xml', category: 'indicators', size: '120KB', type: 'XML', updateTime: '2026-03-02', owner: 'editor' },
        { id: 3, name: 'Mission_Test_Data_RX99.csv', category: 'data_source', size: '45MB', type: 'CSV', updateTime: '2026-03-03', owner: 'tester' },
        { id: 4, name: 'Intermediate_Calc_Step1.json', category: 'process_data', size: '2MB', type: 'JSON', updateTime: '2026-03-04', owner: 'system' },
        { id: 5, name: 'Eval_Scheme_Final_Draft.docx', category: 'schemes', size: '1.5MB', type: 'DOCX', updateTime: '2026-03-04', owner: 'admin' },
        { id: 6, name: 'Mission_Report_2026_Q1.pdf', category: 'reports', size: '5MB', type: 'PDF', updateTime: '2026-03-04', owner: 'admin' }
    ])

    const deleteFile = (id) => {
        const idx = files.value.findIndex(f => f.id === id)
        if (idx !== -1) files.value.splice(idx, 1)
    }

    const addFile = (file) => {
        files.value.unshift({
            id: Date.now(),
            updateTime: new Date().toISOString().split('T')[0],
            owner: 'admin',
            ...file
        })
    }

    return {
        categories,
        files,
        deleteFile,
        addFile
    }
})
