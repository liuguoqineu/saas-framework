import request from '@/utils/request'

export const inventoryApi = {
  page(params) {
    return request.get('/inventory/page', { params })
  },
  detail(id) {
    return request.get(`/inventory/${id}`)
  },
  stockOut(data) {
    return request.post('/inventory/stock-out', data)
  },
  listStockOutOrders(inventoryId) {
    return request.get(`/inventory/${inventoryId}/stock-out-orders`)
  },
  updateMinStockQty(id, minStockQty) {
    return request.put(`/inventory/${id}/min-stock-qty`, { minStockQty })
  }
}

export const itemTypeOptions = [
  { value: 1, label: '设备' },
  { value: 2, label: '配件' }
]

export const itemTypeLabel = {
  1: '设备',
  2: '配件'
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
