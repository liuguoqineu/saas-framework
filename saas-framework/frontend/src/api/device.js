import request from '@/utils/request'

export const deviceApi = {
  page(params) {
    return request.get('/device/page', { params })
  },
  detail(id) {
    return request.get(`/device/${id}`)
  },
  install(id, data) {
    return request.put(`/device/${id}/install`, data)
  },
  getTimeline(id) {
    return request.get(`/device/${id}/timeline`)
  },
  searchByCode(params) {
    return request.get('/device/search', { params })
  }
}

export const deviceStatusOptions = [
  { value: 0, label: '待入库' },
  { value: 1, label: '待安装' },
  { value: 2, label: '在用' },
  { value: 3, label: '维修中' },
  { value: 4, label: '停用' },
  { value: 5, label: '报废' }
]

export const deviceStatusLabel = {
  0: '待入库',
  1: '待安装',
  2: '在用',
  3: '维修中',
  4: '停用',
  5: '报废'
}

export const deviceStatusTagType = {
  0: 'info',
  1: 'warning',
  2: 'success',
  3: 'danger',
  4: 'info',
  5: 'danger'
}

export const timelineEventTypeLabel = {
  1: '采购',
  2: '入库',
  3: '出库',
  4: '安装',
  5: '报修',
  6: '维修',
  7: '配件更换',
  8: '整机更换',
  9: '报废'
}

export const timelineEventTypeTagType = {
  1: '',
  2: 'success',
  3: 'warning',
  4: '',
  5: 'danger',
  6: 'success',
  7: 'warning',
  8: 'danger',
  9: 'info'
}
