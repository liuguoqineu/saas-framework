/**
 * 按钮级权限指令
 * 使用方式: <el-button v-permission="'student:add'">新增</el-button>
 *
 * 原理：从 localStorage 中读取用户权限列表，
 * 如果当前用户没有该权限，则移除该 DOM 元素
 */
const permissionDirective = {
  mounted(el, binding) {
    const requiredPermission = binding.value
    if (!requiredPermission) return

    // 获取用户权限列表
    const userInfoStr = localStorage.getItem('userInfo')
    if (!userInfoStr) {
      el.parentNode?.removeChild(el)
      return
    }

    try {
      const userInfo = JSON.parse(userInfoStr)
      const permissions = userInfo.permissions || []

      // 超级管理员（tenantId=0）拥有所有权限
      if (userInfo.tenantId === 0) return

      // 检查是否有该权限
      if (!permissions.includes(requiredPermission)) {
        el.parentNode?.removeChild(el)
      }
    } catch {
      el.parentNode?.removeChild(el)
    }
  }
}

export default permissionDirective
