import request from '@/utils/request'

/**
 * 员工申请 API
 */
export const employeeRequestApi = {
  /** 提交员工申请（租户管理员） */
  submit(data) {
    return request.post('/employee-request', data)
  },
  /** 上传资质证书图片 */
  uploadZhizhiImage(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/employee-request/upload-zhizhi-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  /** 审核列表（超级管理员） */
  page(params) {
    return request.get('/employee-request/page', { params })
  },
  /** 查询员工申请详情 */
  getById(id) {
    return request.get(`/employee-request/${id}`)
  },
  /** 本租户申请列表（租户管理员） */
  myRequests(params) {
    return request.get('/employee-request/my', { params })
  },
  /** 审核员工申请（超级管理员） */
  review(id, data) {
    return request.put(`/employee-request/${id}/review`, data)
  },
  /** 批量审核员工申请（超级管理员） */
  batchReview(data) {
    return request.put('/employee-request/batch-review', data)
  },
  /** 撤销员工申请（租户管理员） */
  cancel(id) {
    return request.put(`/employee-request/${id}/cancel`)
  }
}
