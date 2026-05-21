import request from '@/utils/request'

export const statisticsApi = {
  customerStats(params) {
    return request.get('/statistics/customer', { params })
  },
  repairStats(params) {
    return request.get('/statistics/repair', { params })
  },
  visitStats(params) {
    return request.get('/statistics/visit', { params })
  },
  contractStats(params) {
    return request.get('/statistics/contract', { params })
  },
  getCustomerExportUrl(params) {
    const query = new URLSearchParams()
    if (params.startDate) query.append('startDate', params.startDate)
    if (params.endDate) query.append('endDate', params.endDate)
    return `/api/statistics/customer/export?${query.toString()}`
  },
  getRepairExportUrl(params) {
    const query = new URLSearchParams()
    if (params.startDate) query.append('startDate', params.startDate)
    if (params.endDate) query.append('endDate', params.endDate)
    if (params.period) query.append('period', params.period)
    return `/api/statistics/repair/export?${query.toString()}`
  },
  getVisitExportUrl(params) {
    const query = new URLSearchParams()
    if (params.startDate) query.append('startDate', params.startDate)
    if (params.endDate) query.append('endDate', params.endDate)
    return `/api/statistics/visit/export?${query.toString()}`
  },
  getContractExportUrl(params) {
    const query = new URLSearchParams()
    if (params.startDate) query.append('startDate', params.startDate)
    if (params.endDate) query.append('endDate', params.endDate)
    return `/api/statistics/contract/export?${query.toString()}`
  }
}
