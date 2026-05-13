import axios from 'axios'
import { ElMessage } from 'element-plus'
const service = axios.create({
    baseURL: '/dev-api',
    timeout: 150000
})

service.interceptors.response.use(
    response => {
        const res = response.data
        if (res.code != 200) {
            ElMessage.error(res.msg || 'Error')
            return Promise.reject(new Error(res.msg || 'Error'))
        } else {
            return res
        }
    },
    error => {
        console.log('err' + error) // for debug
        ElMessage.error(error.message || 'Error')
        return Promise.reject(error)
    }
)
export default service