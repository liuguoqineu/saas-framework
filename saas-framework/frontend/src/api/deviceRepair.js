import request from '@/utils/request'

export const deviceRepairApi = {
  // 设备故障报修
  deviceRepair(data) {
    return request.post('/repair/device-repair', data)
  },
  // 设备维修处理（含更换记录）
  deviceProcess(id, data) {
    return request.put(`/repair/${id}/device-process`, data)
  },
  // 查询更换记录
  replacementPage(params) {
    return request.get('/device-replacement/page', { params })
  },
  // 更换记录详情
  replacementDetail(id) {
    return request.get(`/device-replacement/${id}`)
  },
  // 搜索设备（用于下拉选择）
  searchDevice(params) {
    return request.get('/device/search', { params })
  }
}

export const replacementTypeOptions = [
  { value: 1, label: '配件更换' },
  { value: 2, label: '整机更换' }
]

export const replacementTypeLabel = {
  1: '配件更换',
  2: '整机更换'
}

export const oldItemStatusOptions = [
  { value: 1, label: '报废' },
  { value: 2, label: '返修' },
  { value: 3, label: '留用' }
]

export const oldItemStatusLabel = {
  1: '报废',
  2: '返修',
  3: '留用'
}

export const replacementItemTypeOptions = [
  { value: 1, label: '配件' },
  { value: 2, label: '设备' }
]
