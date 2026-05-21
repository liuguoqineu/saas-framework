import request from '@/utils/request'

/**
 * 角色管理 API
 */
export const roleApi = {
  /** 获取角色详情（含权限ID） */
  getById(id) {
    return request.get(`/role/${id}`)
  },
  /** 分页查询角色 */
  page(params) {
    return request.get('/role/page', { params })
  },
  /** 新增角色 */
  create(data) {
    return request.post('/role', data)
  },
  /** 修改角色 */
  update(id, data) {
    return request.put(`/role/${id}`, data)
  },
  /** 删除角色 */
  delete(id) {
    return request.delete(`/role/${id}`)
  },
  /** 获取权限树 */
  getPermissionTree() {
    return request.get('/permission/tree')
  }
}
