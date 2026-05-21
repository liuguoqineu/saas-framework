import request from '@/utils/request'

export const repairApi = {
  page(params) {
    return request.get('/repair/page', { params })
  },
  detail(id) {
    return request.get(`/repair/${id}`)
  },
  create(data) {
    return request.post('/repair', data)
  },
  update(id, data) {
    return request.put(`/repair/${id}`, data)
  },
  delete(id) {
    return request.delete(`/repair/${id}`)
  },
  assign(id, data) {
    return request.put(`/repair/${id}/assign`, data)
  },
  process(id, data) {
    return request.put(`/repair/${id}/process`, data)
  },
  confirm(id) {
    return request.put(`/repair/${id}/confirm`)
  },
  markException(id, data) {
    return request.put(`/repair/${id}/exception`, data)
  },
  listAttachments(id) {
    return request.get(`/repair/${id}/attachments`)
  },
  uploadAttachment(id, formData) {
    return request.post(`/repair/${id}/attachment`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  deleteAttachment(attachmentId) {
    return request.delete(`/repair/attachment/${attachmentId}`)
  },
  listProcessLogs(id) {
    return request.get(`/repair/${id}/process-logs`)
  },
  stats() {
    return request.get('/repair/stats')
  },
  getUnconfirmed() {
    return request.get('/repair/unconfirmed')
  },
  getExportUrl(params) {
    const query = new URLSearchParams()
    if (params.customerName) query.append('customerName', params.customerName)
    if (params.repairTimeStart) query.append('repairTimeStart', params.repairTimeStart)
    if (params.repairTimeEnd) query.append('repairTimeEnd', params.repairTimeEnd)
    if (params.status) query.append('status', params.status)
    if (params.assigneeName) query.append('assigneeName', params.assigneeName)
    if (params.urgency) query.append('urgency', params.urgency)
    if (params.repairType) query.append('repairType', params.repairType)
    return `/api/repair/export?${query.toString()}`
  }
}

export const repairTypeOptions = [
  { value: '智慧燃气系统故障', label: '智慧燃气系统故障' },
  { value: '设备问题', label: '设备问题' },
  { value: '管道泄漏', label: '管道泄漏' },
  { value: '仪表故障', label: '仪表故障' },
  { value: '其他', label: '其他' }
]

export const repairStatusOptions = [
  { value: '未处理', label: '未处理' },
  { value: '处理中', label: '处理中' },
  { value: '已解决', label: '已解决' },
  { value: '无法解决', label: '无法解决' }
]

export const urgencyOptions = [
  { value: '普通', label: '普通' },
  { value: '紧急', label: '紧急' }
]

export const repairStatusTagType = {
  '未处理': 'danger',
  '处理中': 'warning',
  '已解决': 'success',
  '无法解决': 'info'
}
