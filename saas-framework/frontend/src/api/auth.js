import request from '@/utils/request'

/**
 * 认证 API
 */
export const authApi = {
  /** 登录 */
  login(data) {
    return request.post('/auth/login', data)
  },
  /** 获取当前用户信息 */
  getUserInfo() {
    return request.get('/auth/info')
  }
}
