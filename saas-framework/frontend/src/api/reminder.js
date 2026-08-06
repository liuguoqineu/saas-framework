import request from '@/utils/request'

export const reminderApi = {
  getLoginReminders() {
    return request.get('/reminder/login')
  }
}
