import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 后端 Result.success 返回 code=200，失败为业务 code；统一以 0/200 视为成功
    if (body && typeof body.code === 'number' && body.code !== 0 && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || 'Error'))
    }
    return body ? body.data : resp.data
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
