import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import App from './App.vue'
import router from './router'
import permissionDirective from './directives/permission'

const app = createApp(App)

// Element Plus
app.use(ElementPlus, { locale: zhCn })

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 状态管理
app.use(createPinia())

// 路由
app.use(router)

// 自定义权限指令 v-permission
app.directive('permission', permissionDirective)

app.mount('#app')
