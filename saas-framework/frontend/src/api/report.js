import request from '@/utils/request'

export const reportApi = {
  // Templates
  getTemplates() {
    return request.get('/report/templates')
  },
  getTemplate(id) {
    return request.get('/report/templates/' + id)
  },

  // Reports CRUD
  page(params) {
    return request.get('/report/reports', { params })
  },
  getReport(id) {
    return request.get('/report/reports/' + id)
  },
  create(data) {
    return request.post('/report/reports', data)
  },
  update(id, data) {
    return request.put('/report/reports/' + id, data)
  },
  deleteDraft(id) {
    return request.delete('/report/reports/' + id)
  },

  // Submit & Approval
  submit(id) {
    return request.post('/report/reports/' + id + '/submit')
  },
  resubmit(id, data) {
    return request.post('/report/reports/' + id + '/resubmit', data)
  },
  getPendingApprovals() {
    return request.get('/report/approvals/pending')
  },
  approve(id) {
    return request.post('/report/approvals/' + id + '/approve')
  },
  reject(id, data) {
    return request.post('/report/approvals/' + id + '/reject', data)
  },
  getApprovalChain(reportId) {
    return request.get('/report/reports/' + reportId + '/approval-chain')
  },

  // Revisions
  getRevisions(reportId) {
    return request.get('/report/reports/' + reportId + '/revisions')
  },

  // Export
  getExportExcelUrl(params) {
    const token = localStorage.getItem('token')
    const query = new URLSearchParams()
    if (params.userId) query.append('userId', params.userId)
    if (params.reportType) query.append('reportType', params.reportType)
    if (params.reportPeriod) query.append('reportPeriod', params.reportPeriod)
    if (params.status) query.append('status', params.status)
    query.append('token', token)
    return '/api/report/reports/export/excel?' + query.toString()
  },
  getExportPdfUrl(reportId) {
    const token = localStorage.getItem('token')
    return '/api/report/reports/' + reportId + '/export/pdf?token=' + token
  },

  // Dashboard
  getDashboardOverview() {
    return request.get('/report/dashboard/overview')
  },
  getDashboardByPost(postType) {
    return request.get('/report/dashboard/' + postType)
  },

  // Overdue
  getOverdueList() {
    return request.get('/report/overdue/list')
  },
  getOverdueExportUrl() {
    const token = localStorage.getItem('token')
    return '/api/report/overdue/export?token=' + token
  }
}
