import { createApp } from 'vue'
import './style.css'
import './components/mobbin/workspace-layouts.css'
import App from './App.vue'
import { createAppRouter } from './router'

createApp(App).use(createAppRouter()).mount('#app')
