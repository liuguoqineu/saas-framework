import request from '@/utils/request'

export const operationLogApi = {
  page(params) {
    return request.get('/operation-log/page', { params })
  },
  export(params) {
    return request.get('/operation-log/export', { params, responseType: 'blob' })
  }
}
