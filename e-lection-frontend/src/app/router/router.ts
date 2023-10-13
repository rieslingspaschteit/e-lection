import axios from 'axios'
import { createRouter, createWebHashHistory } from 'vue-router'
import { ConfigProvider } from '../utils/utils'
import { ElectionStore } from '../view/view'

const routes = [
  { path: '/', name: 'Home', component: async () => await import('@views/StartView.vue') },
  { path: '/login', name: 'Login', component: async () => await import('@views/LoginView.vue') },
  { path: '/verify', name: 'Verify', component: async () => await import('@views/VerifyView.vue') },
  { path: '/authority', name: 'Authority', component: async () => await import('@views/AuthorityView.vue') },
  { path: '/trustee', name: 'Trustee', component: async () => await import('@views/TrusteeView.vue') },
  { path: '/trustee/:id/key-ceremony', name: 'Key-Ceremony', component: async () => await import('@views/KeyCerView.vue') },
  { path: '/dashboard/:id/:slug', name: 'Dashboard', component: async () => await import('@views/DashboardView.vue') },
  { path: '/:id/:slug/vote', name: 'Vote', component: async () => await import('@views/VoteView.vue') },
  { path: '/create', name: 'Create', component: async () => await import('@views/ConfigView.vue') },
  { path: '/trustee/:id/decryption', name: 'Decryption', component: async () => await import('@views/DecryptView.vue') }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async to => {
  if (to.name !== 'Login') {
    const authenticated = await checkForAuthenticated()
    ElectionStore.instance.authenticated = authenticated
    if (!authenticated) {
      return { name: 'Login' }
    }
  }
})

const checkForAuthenticated = async (): Promise<boolean> => {
  return await axios.get(
    (await ConfigProvider.getInstance().config).backendServer + '/api/am-i-logged-in',
    { withCredentials: true }
  )
    .then(response => response.status === 200)
    .catch(async error => {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        return await Promise.resolve(false)
      }
      return await Promise.reject(error)
    })
}

export default router
