import './init'
import { createApp } from 'vue'
import App from './App.vue'

import router from '@/app/router/router'

createApp(App)
  .use(router)
  .mount('#app')
