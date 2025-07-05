import axios from 'axios'
import { ElMessage } from 'element-plus'

const baseURL = '/api';
// 创建 axios 实例
const request = axios.create({
  // 使用 Apifox 的本地 Mock 服务地址
  baseURL, // 替换为您的 Apifox 项目 ID
  timeout: 5000,
  withCredentials: true
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 添加 Mock 请求头
    config.headers['X-Apifox-Mock'] = true
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    ElMessage.error(error.message || '请求失败')
    return Promise.reject(error)
  }
)

export default request
export {baseURL}
