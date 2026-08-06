import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 检查字符串中是否包含 < > 字符（XSS 防护）
function containsAngleBrackets(val) {
  if (typeof val === 'string') return val.includes('<') || val.includes('>')
  if (Array.isArray(val)) return val.some(containsAngleBrackets)
  if (val && typeof val === 'object') return Object.values(val).some(containsAngleBrackets)
  return false
}

// 请求拦截器：自动携带 Token + XSS 校验
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // 拦截 JSON 请求体中的 < > 字符（文件上传除外）
    if (config.data && !(config.data instanceof FormData) && !(config.data instanceof Blob)) {
      if (containsAngleBrackets(config.data)) {
        ElMessage.error('输入内容不能包含 < > 字符')
        return Promise.reject(new Error('输入内容不能包含 < > 字符'))
      }
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误码
request.interceptors.response.use(
  response => {
    const res = response.data

    if (response.config.responseType === 'blob') {
      return response
    }

    if (res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 401:
          // Token 过期或未登录，清除信息并跳转登录页
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
          break
        case 403:
          ElMessage.error(data.msg || '权限不足')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(data.msg || '请求失败')
      }
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
