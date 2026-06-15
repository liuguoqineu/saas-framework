import request from '@/utils/request'

export const customerApi = {
  page(params) {
    return request.get('/customer/page', { params })
  },
  detail(id) {
    return request.get(`/customer/${id}`)
  },
  create(data) {
    return request.post('/customer', data)
  },
  update(id, data) {
    return request.put(`/customer/${id}`, data)
  },
  markInvalid(id) {
    return request.put(`/customer/${id}/invalid`)
  },
  restoreInvalid(id) {
    return request.put(`/customer/${id}/restore`)
  },
  delete(id) {
    return request.delete(`/customer/${id}`)
  },
  listAttachments(id) {
    return request.get(`/customer/${id}/attachments`)
  },
  uploadAttachment(id, formData) {
    return request.post(`/customer/${id}/attachment`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAttachment(attachmentId) {
    return request.delete(`/customer/attachment/${attachmentId}`)
  },
  getDownloadUrl(attachmentId) {
    const token = localStorage.getItem('token')
    return `/api/customer/attachment/${attachmentId}/download?token=${token}`
  },
  listModifyLogs(id) {
    return request.get(`/customer/${id}/modify-logs`)
  },
  importCustomers(formData) {
    return request.post('/customer/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getExportUrl(params) {
    const token = localStorage.getItem('token')
    const query = new URLSearchParams()
    if (params.name) query.append('name', params.name)
    if (params.businessCategory) query.append('businessCategory', params.businessCategory)
    if (params.businessType) query.append('businessType', params.businessType)
    if (params.cooperationStatus) query.append('cooperationStatus', params.cooperationStatus)
    if (params.maintenanceCategory) query.append('maintenanceCategory', params.maintenanceCategory)
    query.append('token', token)
    return `/api/customer/export?${query.toString()}`
  },
  getDicts() {
    return request.get('/customer/dicts')
  },
  // 查询公共客户池（未分配跟进人的客户）
  publicPool(params) {
    return request.get('/customer/public-pool', { params })
  },
  // 分配客户给销售人员
  assignCustomer(id, userId, username) {
    return request.post(`/customer/${id}/assign`, null, { params: { userId, username } })
  },
  // 转移客户给另一个销售人员
  transferCustomer(id, userId, username) {
    return request.post(`/customer/${id}/transfer`, null, { params: { userId, username } })
  },
  // 回收客户到公共池
  reclaimCustomer(id) {
    return request.post(`/customer/${id}/reclaim`)
  }
}

export const businessCategoryMap = {
  '加气站类': ['CNG加气站', 'LPG加气站'],
  '商业用气': [
    { value: '餐饮类', label: '餐饮类（饭店、餐馆）' },
    { value: '团餐类', label: '团餐类（大企业食堂、高校食堂）' },
    { value: '其他商业类', label: '其他商业类（酒店、商超后厨等）' }
  ],
  '民业用气': ['大型', '中型', '小型']
}

export const cooperationStatusOptions = [
  { value: '正常履约', label: '正常履约（已签合同，正在使用系统，无逾期）' },
  { value: '终止合作', label: '终止合作（不再合作，留存历史数据）' },
  { value: '高潜力', label: '高潜力（明确需求，短期内可签约）' },
  { value: '中潜力', label: '中潜力（有需求但时间不明确）' },
  { value: '低潜力', label: '低潜力（需求不明确，需长期跟进）' },
  { value: '无效客户', label: '无效客户' }
]

export const regionOptions = []

export const maintenanceCategoryOptions = [
  { value: '高频报修', label: '高频报修客户（需重点关注）' },
  { value: '常规运维', label: '常规运维客户（按计划巡检）' },
  { value: '无报修', label: '无报修客户（系统稳定）' }
]
