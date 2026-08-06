import request from '@/utils/request'

/**
 * 数据库备份管理 API
 * 仅超级管理员可操作
 */
export const backupApi = {
  /** 手动备份数据库 */
  manualBackup() {
    return request.post('/backup/manual')
  },
  /** 分页查询备份记录 */
  page(params) {
    return request.get('/backup/page', { params })
  },
  /** 下载备份文件 */
  download(id) {
    return request.get(`/backup/download/${id}`, { responseType: 'blob' })
  },
  /** 删除备份记录 */
  delete(id) {
    return request.delete(`/backup/${id}`)
  }
}
