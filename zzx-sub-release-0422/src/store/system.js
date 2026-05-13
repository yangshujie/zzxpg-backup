import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSystemStore = defineStore('system', () => {
    // 1. User Management
    const deptTree = ref([
        {
            id: 1, label: 'Evaluate Tech', children: [
                { id: 101, label: 'Headquarters' },
                { id: 102, label: 'R&D Center' },
                { id: 103, label: 'Testing Center' }
            ]
        }
    ])

    const users = ref([
        { id: 1, username: 'admin', nickName: 'System Admin', dept: 'Headquarters', role: 'Super Admin', status: 'Active', createTime: '2023-01-01' },
        { id: 2, username: 'editor', nickName: 'Content Editor', dept: 'R&D Center', role: 'Editor', status: 'Active', createTime: '2023-02-15' },
        { id: 3, username: 'test_user', nickName: 'Test Engineer', dept: 'Testing Center', role: 'Tester', status: 'Disabled', createTime: '2023-03-10' }
    ])

    // 2. Role Management
    const roles = ref([
        { id: 1, name: 'Super Admin', key: 'admin', sort: 1, status: 'Active', createTime: '2023-01-01' },
        { id: 2, name: 'Editor', key: 'editor', sort: 2, status: 'Active', createTime: '2023-02-01' },
        { id: 3, name: 'Tester', key: 'tester', sort: 3, status: 'Active', createTime: '2023-02-01' }
    ])

    // 3. Logs
    const operLogs = ref([
        { id: 1001, module: 'System', type: 'Login', desc: 'User login success', status: 'Success', operator: 'admin', time: '2023-11-20 10:00:00', ip: '127.0.0.1' },
        { id: 1002, module: 'User', type: 'Update', desc: 'Updated user details', status: 'Success', operator: 'admin', time: '2023-11-20 10:05:00', ip: '127.0.0.1' }
    ])

    const loginLogs = ref([
        { id: 5001, user: 'admin', ip: '127.0.0.1', location: 'Localhost', browser: 'Chrome', os: 'Windows', status: 'Success', msg: 'Login successful', time: '2023-11-20 10:00:00' }
    ])

    // 4. Dicts
    const dicts = ref([
        { id: 1, name: 'User Status', type: 'sys_user_status', status: 'Active' },
        { id: 2, name: 'Gender', type: 'sys_user_sex', status: 'Active' }
    ])

    return {
        deptTree,
        users,
        roles,
        operLogs,
        loginLogs,
        dicts
    }
})
