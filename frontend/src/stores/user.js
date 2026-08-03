import { defineStore } from 'pinia'
import { login as loginApi, getProfile } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null
  }),
  actions: {
    async login(form) {
      const res = await loginApi(form)
      this.token = res.data.token
      this.userInfo = res.data.user
      setToken(this.token)
      return res
    },
    async fetchProfile() {
      const res = await getProfile()
      this.userInfo = res.data
      return res.data
    },
    logout() {
      this.token = ''
      this.userInfo = null
      removeToken()
    }
  }
})
