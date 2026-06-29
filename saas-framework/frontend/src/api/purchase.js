import request from '@/utils/request'

export const purchaseApi = {
  page(params) {
    return request.get('/purchase/page', { params })
  },
  detail(id) {
    return request.get(`/purchase/${id}`)
  },
  create(data) {
    return request.post('/purchase', data)
  },
  update(id, data) {
    return request.put(`/purchase/${id}`, data)
  },
  delete(id) {
    return request.delete(`/purchase/${id}`)
  },
  stockIn(data) {
    return request.post('/purchase/stock-in', data)
  },
  listStockInOrders(purchaseOrderId) {
    return request.get(`/purchase/${purchaseOrderId}/stock-in-orders`)
  },
  uploadFile(formData) {
    return request.post('/purchase/upload-file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  getFileDownloadUrl(filePath) {
    const token = localStorage.getItem('token')
    return `/api/purchase/file/download?path=${encodeURIComponent(filePath)}&token=${token}`
  }
}

export const purchaseStatusOptions = [
  { value: 0, label: '待入库' },
  { value: 1, label: '已入库' }
]

export const itemTypeOptions = [
  { value: 1, label: '设备' },
  { value: 2, label: '配件' }
]

export const purchaseStatusTagType = {
  0: 'warning',
  1: 'success'
}

export const purchaseStatusLabel = {
  0: '待入库',
  1: '已入库'
}

export const itemTypeLabel = {
  1: '设备',
  2: '配件'
}
