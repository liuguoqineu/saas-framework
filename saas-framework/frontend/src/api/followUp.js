import request from '@/utils/request'

export const followUpApi = {
  pageRecords(params) {
    return request.get('/follow-up/records', { params })
  },
  getRecordDetail(id) {
    return request.get(`/follow-up/records/${id}`)
  },
  createRecord(data) {
    return request.post('/follow-up/records', data)
  },
  updateRecord(id, data) {
    return request.put(`/follow-up/records/${id}`, data)
  },
  deleteRecord(id) {
    return request.delete(`/follow-up/records/${id}`)
  },
  listRecordsByCustomerId(customerId) {
    return request.get(`/follow-up/records/customer/${customerId}`)
  },
  getExportUrl(params) {
    const query = new URLSearchParams()
    if (params.customerId) query.append('customerId', params.customerId)
    if (params.customerName) query.append('customerName', params.customerName)
    if (params.followUpPerson) query.append('followUpPerson', params.followUpPerson)
    if (params.followUpStatus) query.append('followUpStatus', params.followUpStatus)
    if (params.followUpMethod) query.append('followUpMethod', params.followUpMethod)
    if (params.startTime) query.append('startTime', params.startTime)
    if (params.endTime) query.append('endTime', params.endTime)
    return `/api/follow-up/records/export?${query.toString()}`
  },
  changeCustomerStatus(customerId, data) {
    return request.put(`/follow-up/customers/${customerId}/status`, data)
  },
  listStatusLogs(customerId) {
    return request.get(`/follow-up/customers/${customerId}/status-logs`)
  }
}

export const followUpMethodMap = {
  1: '电话',
  2: '微信',
  3: '邮件',
  4: '上门拜访',
  5: '其他'
}

export const followUpStatusMap = {
  1: '待跟进',
  2: '已跟进',
  3: '已达成意向'
}

export const followUpMethodOptions = [
  { value: 1, label: '电话' },
  { value: 2, label: '微信' },
  { value: 3, label: '邮件' },
  { value: 4, label: '上门拜访' },
  { value: 5, label: '其他' }
]

export const followUpStatusOptions = [
  { value: 1, label: '待跟进' },
  { value: 2, label: '已跟进' },
  { value: 3, label: '已达成意向' }
]
