import request from '@/utils/request'

export const deviceReplacementApi = {
  // 分页查询更换记录
  page(params) {
    return request.get('/device-replacement/page', { params })
  },
  // 更换记录详情
  detail(id) {
    return request.get(`/device-replacement/${id}`)
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

export const replacementItemTypeLabel = {
  1: '配件',
  2: '设备'
}

export const oldItemStatusLabel = {
  1: '报废',
  2: '返修',
  3: '留用'
}
