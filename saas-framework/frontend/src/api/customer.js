import request from '@/utils/request'

/**
 * 客户管理 API
 */
export const customerApi = {
  /** 分页查询客户列表 */
  page(params) {
    return request.get('/customer/page', { params })
  },
  /** 查看客户详情 */
  detail(id) {
    return request.get(`/customer/${id}`)
  },
  /** 新增客户 */
  create(data) {
    return request.post('/customer', data)
  },
  /** 修改客户 */
  update(id, data) {
    return request.put(`/customer/${id}`, data)
  },
  /** 标记客户为无效 */
  markInvalid(id) {
    return request.put(`/customer/${id}/invalid`)
  },
  /** 彻底删除客户 */
  delete(id) {
    return request.delete(`/customer/${id}`)
  },
  /** 查询客户附件列表 */
  listAttachments(id) {
    return request.get(`/customer/${id}/attachments`)
  },
  /** 上传客户附件 */
  uploadAttachment(id, formData) {
    return request.post(`/customer/${id}/attachment`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  /** 删除客户附件 */
  deleteAttachment(attachmentId) {
    return request.delete(`/customer/attachment/${attachmentId}`)
  },
  /** 下载客户附件 */
  getDownloadUrl(attachmentId) {
    const token = localStorage.getItem('token')
    return `/api/customer/attachment/${attachmentId}/download?token=${token}`
  },
  /** 查询客户修改记录 */
  listModifyLogs(id) {
    return request.get(`/customer/${id}/modify-logs`)
  },
  /** Excel批量导入客户 */
  importCustomers(formData) {
    return request.post('/customer/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  /** 获取导出URL（用于下载） */
  getExportUrl(params) {
    const token = localStorage.getItem('token')
    const query = new URLSearchParams()
    if (params.name) query.append('name', params.name)
    if (params.businessCategory) query.append('businessCategory', params.businessCategory)
    if (params.businessType) query.append('businessType', params.businessType)
    if (params.cooperationCategory) query.append('cooperationCategory', params.cooperationCategory)
    if (params.cooperationStatus) query.append('cooperationStatus', params.cooperationStatus)
    if (params.maintenanceCategory) query.append('maintenanceCategory', params.maintenanceCategory)
    query.append('token', token)
    return `/api/customer/export?${query.toString()}`
  }
}

/**
 * 业务类型分类数据（两级联动）
 * 核心分类，关联智慧燃气系统适配
 *
 * 一级分类:
 *   1. 加气站类客户 - 关联对应智慧燃气设备运维需求
 *   2. 商业用气客户 - 餐饮/团餐/其他商业
 *   3. 工业用气客户 - 按用气规模辅助分类，适配不同的运维和拜访频次
 */
export const businessCategoryMap = {
  '加气站类': ['CNG加气站', 'LPG加气站'],
  '商业用气': [
    { value: '餐饮类', label: '餐饮类（饭店、餐馆）' },
    { value: '团餐类', label: '团餐类（大企业食堂、高校食堂）' },
    { value: '其他商业类', label: '其他商业类（酒店、商超后厨等）' }
  ],
  '工业用气': ['大型', '中型', '小型']
}

/**
 * 合作状态分类数据（两级联动）
 * 核心分类，关联合同、跟进、拜访
 *
 * 一级分类:
 *   1. 已合作客户 - 已签合同，关联合同管理和系统使用状态
 *   2. 潜在客户 - 已对接，有关注和跟进需求
 *   3. 意向客户 - 刚对接，表达过兴趣，未深入沟通
 *   4. 无效客户 - 多次对接无回应或不符合服务范围
 */
export const cooperationCategoryMap = {
  '已合作': [
    { value: '正常履约', label: '正常履约（已签合同，正在使用系统，无逾期）' },
    { value: '终止合作', label: '终止合作（不再合作，留存历史数据）' }
  ],
  '潜在': [
    { value: '高潜力', label: '高潜力（明确需求，短期内可签约）' },
    { value: '中潜力', label: '中潜力（有需求但时间不明确）' },
    { value: '低潜力', label: '低潜力（需求不明确，需长期跟进）' }
  ],
  '无效': [
    { value: '无效客户', label: '无效客户（多次无回应或不符合服务范围）' }
  ]
}

/**
 * 区域分类数据（辅助分类，可选）
 * 按客户所在城市、区县分类，方便区域拜访统筹
 *
 * 使用说明：
 *   - 使用 element-china-area-data 组件的省市区级联选择器
 *   - 存储格式：完整地址字符串（如"北京市朝阳区"）
 */
export const regionOptions = []

/**
 * 运维需求分类数据（辅助分类，可选）
 * 关联报修模块，精准安排运维
 *
 * 分类说明：
 *   1. 高频报修客户 - 报修次数多，需重点关注和优先响应
 *   2. 常规运维客户 - 正常报修频率，按计划定期巡检
 *   3. 无报修客户 - 系统运行稳定，暂无报修记录
 */
export const maintenanceCategoryOptions = [
  { value: '高频报修', label: '高频报修客户（需重点关注）' },
  { value: '常规运维', label: '常规运维客户（按计划巡检）' },
  { value: '无报修', label: '无报修客户（系统稳定）' }
]
