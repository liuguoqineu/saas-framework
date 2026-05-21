import request from '@/utils/request'

export const contractApi = {
  page(params) {
    return request.get('/contract/page', { params })
  },
  detail(id) {
    return request.get(`/contract/${id}`)
  },
  create(data) {
    return request.post('/contract', data)
  },
  update(id, data) {
    return request.put(`/contract/${id}`, data)
  },
  delete(id) {
    return request.delete(`/contract/${id}`)
  },
  changeStatus(id, data) {
    return request.put(`/contract/${id}/status`, data)
  },
  listAttachments(id) {
    return request.get(`/contract/${id}/attachments`)
  },
  uploadAttachment(id, formData) {
    return request.post(`/contract/${id}/attachment`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAttachment(attachmentId) {
    return request.delete(`/contract/attachment/${attachmentId}`)
  },
  listModifyLogs(id) {
    return request.get(`/contract/${id}/modify-logs`)
  },
  listReminders(id) {
    return request.get(`/contract/${id}/reminders`)
  },
  getPendingReminders() {
    return request.get('/contract/reminders/pending')
  },
  markReminderRead(reminderId) {
    return request.put(`/contract/reminders/${reminderId}/read`)
  },
  markReminderHandled(reminderId) {
    return request.put(`/contract/reminders/${reminderId}/handled`)
  },
  getExportUrl(params) {
    const query = new URLSearchParams()
    if (params.contractNo) query.append('contractNo', params.contractNo)
    if (params.customerName) query.append('customerName', params.customerName)
    if (params.signDateStart) query.append('signDateStart', params.signDateStart)
    if (params.signDateEnd) query.append('signDateEnd', params.signDateEnd)
    if (params.expireDateStart) query.append('expireDateStart', params.expireDateStart)
    if (params.expireDateEnd) query.append('expireDateEnd', params.expireDateEnd)
    if (params.contractStatus) query.append('contractStatus', params.contractStatus)
    return `/api/contract/export?${query.toString()}`
  }
}

export const contractStatusOptions = [
  { value: '已生效', label: '已生效' },
  { value: '已终止', label: '已终止' }
]

export const paymentMethodOptions = [
  { value: '一次性付款', label: '一次性付款' },
  { value: '分期付款', label: '分期付款' },
  { value: '按季度付款', label: '按季度付款' },
  { value: '按年度付款', label: '按年度付款' },
  { value: '其他', label: '其他' }
]

export const contractStatusTagType = {
  '已生效': 'success',
  '已终止': 'danger'
}
