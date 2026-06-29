import request from '@/utils/request'

export const stockOutApi = {
  page(params) {
    return request.get('/stock-out/page', { params })
  },
  detail(id) {
    return request.get(`/stock-out/${id}`)
  },
  stockOut(data) {
    return request.post('/stock-out', data)
  }
}

export const usageTypeOptions = [
  { value: 1, label: '新装设备' },
  { value: 2, label: '维修更换' },
  { value: 3, label: '抢修备用' }
]

export const usageTypeLabel = {
  1: '新装设备',
  2: '维修更换',
  3: '抢修备用'
}

export const usageTypeTagType = {
  1: '',
  2: 'warning',
  3: 'danger'
}

export const itemTypeOptions = [
  { value: 1, label: '设备' },
  { value: 2, label: '配件' }
]

export const itemTypeLabel = {
  1: '设备',
  2: '配件'
}
