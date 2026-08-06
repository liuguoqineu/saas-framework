import request from '@/utils/request'

export const stockInApi = {
  page(params) {
    return request.get('/stock-in/page', { params })
  },
  detail(id) {
    return request.get(`/stock-in/${id}`)
  },
  independentStockIn(data) {
    return request.post('/stock-in/independent', data)
  },
  check(id, data) {
    return request.put(`/stock-in/${id}/check`, data)
  }
}

export const checkStatusOptions = [
  { value: 0, label: '待验收' },
  { value: 1, label: '验收通过' },
  { value: 2, label: '验收不通过' }
]

export const checkStatusLabel = {
  0: '待验收',
  1: '验收通过',
  2: '验收不通过'
}

export const checkStatusTagType = {
  0: 'warning',
  1: 'success',
  2: 'danger'
}

export const itemTypeOptions = [
  { value: 1, label: '设备' },
  { value: 2, label: '配件' }
]

export const itemTypeLabel = {
  1: '设备',
  2: '配件'
}
