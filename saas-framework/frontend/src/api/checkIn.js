import request from '@/utils/request'

export const checkInApi = {
  // 分页查询打卡记录
  page(params) {
    return request.get('/check-in/page', { params })
  },

  // 查看打卡详情
  detail(id) {
    return request.get(`/check-in/${id}`)
  },

  // 删除打卡记录
  delete(id) {
    return request.delete(`/check-in/${id}`)
  }
}
