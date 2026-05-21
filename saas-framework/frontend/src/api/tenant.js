import request from '@/utils/request'

/**
 * 租户管理 API
 * 仅超级账户可操作
 */
export const tenantApi = {
  /** 分页查询租户列表 */
  page(params) {
    return request.get('/tenant/page', { params })
  },
  /** 创建租户 */
  create(data) {
    return request.post('/tenant', data)
  },
  /** 修改租户状态 */
  updateStatus(id, status) {
    return request.put(`/tenant/${id}/status`, { status })
  }
}
