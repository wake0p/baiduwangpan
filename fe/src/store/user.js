import { defineStore } from 'pinia'
import { baseURL } from '@/utils/index'


export const useUserStore = defineStore('user', {
  state: () => ({
    user: null,
    status: 0,
  }),
  getters: {
    isAdmin: (state) => state.user?.isAdmin ?? null,
    nickname: (state) => state.user?.nickname ?? '',
    avatar: (state) => state.user?.avatar ?? '',
    userId: (state) => state.user?.id ?? null,
    username: (state) => state.user?.username ?? ''
  },
  actions: {
    setUser(userData) {
      // 处理 avatar 字段，保证为完整URL
      let avatar = userData.avatar || ''
      if (avatar && !/^https?:\/\//.test(avatar)) {
        const idx = avatar.indexOf('uploads/')
        const purePath = idx !== -1 ? avatar.substring(idx) : avatar
        avatar = baseURL.replace(/\/$/, '') + '/' + purePath.replace(/^\//, '')
      }
      this.user = { ...userData, avatar }
    },
    setStatus(status) {
      this.status = status
    },
    logout() {
      this.user = null
      this.status = 0
    }
  },
  persist: true
})