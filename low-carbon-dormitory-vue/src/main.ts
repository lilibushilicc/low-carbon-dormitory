import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/dist/index.css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import App from './App.vue'
import router from './router/router'

const pinia = createPinia()

createApp(App)
  .use(pinia)
  .use(router)
  .mount('#app')
