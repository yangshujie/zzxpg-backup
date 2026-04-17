import request from './request'

// 小羊那边的指标列表
export function getIndicatorSystem() {
    return request({
        url: '/zhpg/indicatorSystem/select',
        method: 'get',
    })
}

// 算法列表
//  "data": [
//         {
//             "searchValue": null,
//             "createBy": "admin",
//             "createTime": "2024-01-25 03:43:46",
//             "updateBy": "admin",
//             "updateTime": "2024-01-25 03:43:46",
//             "remark": null,
//             "params": null,
//             "id": 324,
//             "algorithmName": "比值量化输出函数",
//             "algorithmType": "指标量化算法",
//             "equipmentType": "",
//             "algorithmDesc": "k*x+b",
//             "algorithmVersion": "V1",
//             "algorithmCodeUrl": "algs/norm/directFunc.zip",
//             "baseFlag": 1,
//             "publishStatus": "PUBLISHED",
//             "status": "ENABLED",
//             "delFlag": "0"
//         },
//     ]
export function getAlgorithmList() {
    return request({
        url: '/zhpg/algorithm/listAll?publishStatus=PUBLISHED',
        method: 'get',
    })
}

// 算法详情
export function getAlgorithmDetail(id) {
    return request({
        url: `/zhpg/algorithm/${id}`,
        method: 'get',
    })
}