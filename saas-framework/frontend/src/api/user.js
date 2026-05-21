import request from '@/utils/request'

/**
 * 员工管理 API
 */
export const userApi = {
  page(params) {
    return request.get('/user/page', { params })
  },
  list(roleName) {
    const params = roleName ? { roleName } : {}
    return request.get('/user/list', { params })
  },
  create(data) {
    return request.post('/user', data)
  },
  /** 修改员工 */
  update(id, data) {
    return request.put(`/user/${id}`, data)
  },
  /** 重置密码 */
  resetPassword(id) {
    return request.put(`/user/${id}/reset-password`)
  },
  /** 删除员工 */
  delete(id) {
    return request.delete(`/user/${id}`)
  }
}
